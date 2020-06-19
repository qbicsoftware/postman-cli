package life.qbic.model.dataLoading;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import life.qbic.ChecksumWriter;
import life.qbic.DownloadException;
import life.qbic.DownloadRequest;
import life.qbic.FileSystemWriter;
import life.qbic.WriterHelper;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import life.qbic.util.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;


public class QbicDataDownloader {

    private String user;

    private String password;

    private IApplicationServerApi applicationServer;

    private IDataStoreServerApi dataStoreServer;

    private final static Logger LOG = LogManager.getLogger(QbicDataDownloader.class);

    private String sessionToken;

    private String filterType;

    private final int defaultBufferSize;

    private final boolean conservePaths;


    /**
     * Constructor for a QBiCDataLoaderInstance
     * @param AppServerUri The openBIS application server URL (AS)
     * @param DataServerUri The openBIS datastore server URL (DSS)
     * @param user The openBIS user
     * @param password The openBis password
     * @param bufferSize The buffer size for the InputStream reader
     */
    public QbicDataDownloader(String AppServerUri, String DataServerUri,
                              String user, String password,
                              int bufferSize, String filterType,
                                boolean conservePaths) {
        this.defaultBufferSize = bufferSize;
        this.filterType = filterType;
        this.conservePaths = conservePaths;

        if (!AppServerUri.isEmpty()) {
            this.applicationServer = HttpInvokerUtils.createServiceStub(
                    IApplicationServerApi.class,
                    AppServerUri + IApplicationServerApi.SERVICE_URL, 10000);
        } else {
            this.applicationServer = null;
        }
        if (!DataServerUri.isEmpty()) {
            this.dataStoreServer = HttpInvokerUtils.createStreamSupportingServiceStub(
                    IDataStoreServerApi.class,
                    DataServerUri + IDataStoreServerApi.SERVICE_URL, 10000);
        } else {
            this.dataStoreServer = null;
        }

        this.setCredentials(user, password);

    }

    /**
     * Setter for user and password credentials
     * @param user The openBIS user
     * @param password The openBIS user's password
     * @return QBiCDataLoader instance
     */
    public QbicDataDownloader setCredentials(String user, String password) {
        this.user = user;
        this.password = password;
        return this;
    }

    /**
     * Login method for openBIS authentication
     * @return 0 if successful, 1 else
     */
    public int login() {
        try {
            this.sessionToken = this.applicationServer.login(this.user, this.password);
            this.applicationServer.getSessionInformation(this.sessionToken);
        } catch (AssertionError | Exception err) {
            LOG.debug(err);
            return 1;
        }

        return 0;
    }

    /**
     * Downloads the files that the user requested
     * checks whether any filtering option (suffix or regex) has been passed and applies filtering if needed
     *
     * @param commandLineParameters
     * @param qbicDataDownloader
     * @throws IOException
     */
    public void downloadRequestedFilesOfDatasets(PostmanCommandLineOptions commandLineParameters, QbicDataDownloader qbicDataDownloader) throws IOException {
        QbicDataFinder qbicDataFinder = new QbicDataFinder(applicationServer,
                                                           dataStoreServer,
                                                           sessionToken,
                                                           filterType);

        LOG.info(String.format("%s provided openBIS identifiers have been found: %s",
                commandLineParameters.ids.size(), commandLineParameters.ids.toString()));

        // a suffix was provided -> only download files which contain the suffix string
        if (!commandLineParameters.suffixes.isEmpty()) {
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<DataSetFile> foundSuffixFilteredIDs = qbicDataFinder.findAllSuffixFilteredIDs(ident, commandLineParameters.suffixes);

                LOG.info(String.format("Number of files found: %s", foundSuffixFilteredIDs.size()));

                downloadFilesFilteredByIDs(ident, foundSuffixFilteredIDs);
            }
            // a regex pattern was provided -> only download files which contain the regex pattern
        } else if (!commandLineParameters.regexPatterns.isEmpty()) {
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<DataSetFile> foundRegexFilteredIDs = qbicDataFinder.findAllRegexFilteredIDs(ident, commandLineParameters.regexPatterns);

                LOG.info(String.format("Number of files found: %s", foundRegexFilteredIDs.size()));

                downloadFilesFilteredByIDs(ident, foundRegexFilteredIDs);
            }
        } else {
            // no suffix or regex was supplied -> download all datasets
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<DataSet> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);

                LOG.info(String.format("Number of datasets found: %s", foundDataSets.size()));

                if (foundDataSets.size() > 0) {
                    LOG.info("Initialize download ...");
                    int datasetDownloadReturnCode = -1;
                    try {
                        datasetDownloadReturnCode = qbicDataDownloader.downloadDataset(foundDataSets);
                    } catch (NullPointerException e) {
                        LOG.error("Datasets were found by the application server, but could not be found on the datastore server for "
                                + ident + "." + " Try to supply the correct datastore server using a config file!");
                    }

                    if (datasetDownloadReturnCode != 0) {
                        LOG.error("Error while downloading dataset: " + ident);
                    } else {
                        LOG.info("Download successfully finished.");
                    }

                } else {
                    LOG.info("Nothing to download.");
                }
            }
        }
    }


    /**
     * Downloads all IDs which were previously filtered by either suffixes or regexes
     *
     * @param ident
     * @param foundFilteredDatasets
     * @throws IOException
     */
    private void downloadFilesFilteredByIDs(String ident, List<DataSetFile> foundFilteredDatasets) throws IOException {
        if (foundFilteredDatasets.size() > 0) {
            LOG.info("Initialize download ...");
            int filesDownloadReturnCode = -1;
            try {
                List<DataSetFile> filteredDataSetFiles = removeDirectories(foundFilteredDatasets);
                final DownloadRequest downloadRequest = new DownloadRequest(filteredDataSetFiles);
                downloadFiles(downloadRequest);
            } catch (NullPointerException e) {
                LOG.error("Datasets were found by the application server, but could not be found on the datastore server for "
                        + ident + "." + " Try to supply the correct datastore server using a config file!");
            }
            if (filesDownloadReturnCode != 0) {
                LOG.error("Error while downloading dataset: " + ident);
            } else {
                LOG.info("Download successfully finished");
            }

        } else {
            LOG.info("Nothing to download.");
        }
    }


    /**
     * Download a given list of data sets
     * @param dataSetList A list of data sets
     * @return 0 if successful, 1 else
     */
    private int downloadDataset(List<DataSet> dataSetList) {
        for (DataSet dataset : dataSetList) {
            DataSetPermId permID = dataset.getPermId();
            DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
            criteria.withDataSet().withCode().thatEquals(permID.getPermId());
            SearchResult<DataSetFile> result = this.dataStoreServer.searchFiles(sessionToken, criteria, new DataSetFileFetchOptions());
            List<DataSetFile> filteredDataSetFiles = removeDirectories(result.getObjects());
            final DownloadRequest downloadRequest = new DownloadRequest(filteredDataSetFiles);
            downloadFiles(downloadRequest);
        }
        return 0;
    }


    private List<DataSetFile> removeDirectories(List<DataSetFile> dataSetFiles) {
        List<DataSetFile> filteredList = new ArrayList<>();
        dataSetFiles.forEach(item -> {
            if (!item.isDirectory()) {
                filteredList.add(item);
            }
        });
        return filteredList;
    }

    private void downloadFile(DataSetFile dataSetFile) throws IOException {
        DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
        options.setRecursive(false);
        IDataSetFileId fileId = dataSetFile.getPermId();
        InputStream stream = this.dataStoreServer.downloadFiles(sessionToken, Collections.singletonList(fileId), options);
        DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
        DataSetFileDownload file;

        ChecksumWriter checksumWriter = new FileSystemWriter(Paths.get(System.getProperty("user.dir") + File.separator + "summary_validated.txt"),
            Paths.get(System.getProperty("user.dir") + File.separator + "summary_failed.txt"));


        while ((file = reader.read()) != null) {
            InputStream initialStream = file.getInputStream();
            CheckedInputStream checkedInputStream = new CheckedInputStream(initialStream, new CRC32());
            if (file.getDataSetFile().getFileLength() > 0) {
                final Path filePath = determineFinalPathFromDataset(file.getDataSetFile());
                File newFile = new File(System.getProperty("user.dir") + File.separator + filePath.toString());
                newFile.getParentFile().mkdirs();
                OutputStream os = new FileOutputStream(newFile);
                ProgressBar progressBar = new ProgressBar(filePath.getFileName().toString(), file.getDataSetFile().getFileLength());
                int bufferSize = (file.getDataSetFile().getFileLength() < defaultBufferSize) ? (int) file.getDataSetFile().getFileLength() : defaultBufferSize;
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                CRC32 crcSum = new CRC32();
                //read from is to buffer
                while ((bytesRead = checkedInputStream.read(buffer)) != -1) {
                    progressBar.updateProgress(bufferSize);
                    os.write(buffer, 0, bytesRead);
                    os.flush();

                }
                System.out.print("\n");
                validateChecksum(Long.toHexString(checkedInputStream.getChecksum().getValue()), dataSetFile, checksumWriter);
                initialStream.close();
                //flush OutputStream to write any buffered data to file
                os.flush();
                os.close();
            }
        }
    }

    private void validateChecksum(String computedChecksumHex, DataSetFile dataSetFile, ChecksumWriter writer) {
        String expectedChecksum = Integer.toHexString(dataSetFile.getChecksumCRC32());
        try {
            if (computedChecksumHex.equals(expectedChecksum)) {
                writer.writeMatchingChecksum(expectedChecksum,
                    computedChecksumHex,
                    Paths.get(dataSetFile.getPath()).toUri().toURL());
            } else {
                writer.writeFailedChecksum(expectedChecksum,
                    computedChecksumHex,
                    Paths.get(dataSetFile.getPath()).toUri().toURL());
            }
        } catch (MalformedURLException e) {
            LOG.error(e);
        }
    }

    private static Path getTopDirectory(Path path) {
        Path newPath = Paths.get(path.toString());
        while (newPath.getParent() != null) {
            newPath = newPath.getParent();
        }
        return newPath;
    }

    private Path determineFinalPathFromDataset(DataSetFile file) {
        Path finalPath;
        if (conservePaths) {
            finalPath = Paths.get(file.getPath());
            // drop top parent directory name in the openBIS DSS (usually "/origin")
            Path topDirectory = getTopDirectory(finalPath);
            finalPath = topDirectory.relativize(finalPath);
        } else {
            finalPath = Paths.get(file.getPath()).getFileName();
        }
        return finalPath;
    }

    private void downloadFiles(DownloadRequest request) throws DownloadException {
        request.getDataSets().forEach( dataSetFile -> {
            try {
                downloadFile(dataSetFile);
            } catch (IOException e) {
                String fileName = Paths.get(dataSetFile.getPath()).getFileName().toString();
                LOG.error(e);
                throw new DownloadException("Dataset " + fileName + " could not have been downloaded.");
            }
            writeCRC32Checksum(dataSetFile);
        });
    }

    void writeCRC32Checksum(DataSetFile dataSetFile) {
        Path path = determineFinalPathFromDataset(dataSetFile);
        String content = Integer.toHexString(dataSetFile.getChecksumCRC32()) + "\t" + path.getFileName();
        WriterHelper.writeToFileSystem(Paths.get(path.toString() + ".crc32"), content);
    }


}
    