package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import life.qbic.ChecksumReporter;
import life.qbic.DownloadException;
import life.qbic.DownloadRequest;
import life.qbic.FileSystemWriter;
import life.qbic.util.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QbicDataDownloader {

  private static final Logger LOG = LogManager.getLogger(QbicDataDownloader.class);
  private final int defaultBufferSize;
  private final boolean conservePaths;
  private final IDataStoreServerApi dataStoreServer;
  private final String sessionToken;
  private static final int DEFAULT_DOWNLOAD_ATTEMPTS = 3;
  private boolean invalidChecksumOccurred = false;
  private final String outputPath;

  private final ChecksumReporter checksumReporter =
          new FileSystemWriter(
                  Paths.get(System.getProperty("user.dir") + File.separator + "logs" + File.separator + "summary_valid_files.txt"),
                  Paths.get(System.getProperty("user.dir") + File.separator + "logs" + File.separator + "summary_invalid_files.txt"));
  private final QbicDataFinder qbicDataFinder;

  /**
   * Constructor for a QBiCDataLoaderInstance
   *
   * @param AppServerUri  The openBIS application server URL (AS)
   * @param DataServerUri The openBIS datastore server URL (DSS)
   * @param bufferSize    The buffer size for the InputStream reader
   * @param conservePaths Flag to conserve the file path structure during download
   * @param sessionToken The session token for the datastore & application servers
   */
  public QbicDataDownloader(
          String AppServerUri,
          String DataServerUri,
          int bufferSize,
          boolean conservePaths,
          String sessionToken,
          String outputPath) {
    this.defaultBufferSize = bufferSize;
    this.conservePaths = conservePaths;
    this.sessionToken = sessionToken;
    this.outputPath = outputPath;

    IApplicationServerApi applicationServer;
    if (!AppServerUri.isEmpty()) {
      applicationServer =
          HttpInvokerUtils.createServiceStub(
              IApplicationServerApi.class, AppServerUri + IApplicationServerApi.SERVICE_URL, 10000);
    } else {
      applicationServer = null;
    }
    if (!DataServerUri.isEmpty()) {
      this.dataStoreServer =
          HttpInvokerUtils.createStreamSupportingServiceStub(
              IDataStoreServerApi.class, DataServerUri + IDataStoreServerApi.SERVICE_URL, 10000);
    } else {
      this.dataStoreServer = null;
    }
    qbicDataFinder = new QbicDataFinder(applicationServer, dataStoreServer, sessionToken);
  }

  /**
   * Downloads the files that the user requested checks whether the filtering option suffix has been
   * passed and applies filtering if needed
   */
  public void downloadRequestedFilesOfDatasets(List<String> ids, List<String> suffixes) {
    LOG.info(
        String.format(
            "%s provided openBIS identifiers have been found: %s",
            ids.size(), ids));

    for (String ident : ids) {
      Map<Sample, List<DataSet>> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);
      if (foundDataSets.size() > 0) {
        LOG.info(String.format("Downloading files for identifier %s", ident));
        try {
          Predicate<DataSetFile> fileFilter = it -> true;
          if (Objects.nonNull(suffixes) && !suffixes.isEmpty()) {
            LOG.info("Applying suffix filter for suffixes: [" + String.join(" ", suffixes) + "] ...");
            Predicate<DataSetFile> suffixFilter = file -> {
              String fileName = getFileName(file);
              return suffixes.stream().anyMatch(fileName::endsWith);
            };
            fileFilter = fileFilter.and(suffixFilter);
          }
          int datasetDownloadReturnCode = downloadDataset(foundDataSets, fileFilter);
          if (datasetDownloadReturnCode != 0) {
            LOG.error("Error while downloading dataset: " + ident);
          } else if (!invalidChecksumOccurred) {
            LOG.info("Download successfully finished.");
          }
        } catch (NullPointerException e) {
          LOG.error(
              "Datasets were found by the application server, but could not be found on the datastore server for "
                  + ident
                  + "."
                  + " Try to supply the correct datastore server using a config file!");
        }
      } else {
        LOG.info("Nothing to download.");
      }
    }
  }

  private String getFileName(DataSetFile file) {
    String filePath = file.getPermId().getFilePath();
    return filePath.substring(filePath.lastIndexOf("/") + 1);
  }

  private int downloadDataset(Map<Sample, List<DataSet>> dataSetsPerSample, Predicate<DataSetFile> fileFilter) {
    int returnCode = 0;
    for (Entry<Sample, List<DataSet>> entry : dataSetsPerSample.entrySet()) {
      String sampleCode = entry.getKey().getCode();
      List<DataSet> sampleDatasets = entry.getValue();
      for (DataSet sampleDataset : sampleDatasets) {
        DataSetPermId permID = sampleDataset.getPermId();
        List<DataSetFile> files = qbicDataFinder.getFiles(permID, fileFilter);
        if (files.isEmpty()) {
          LOG.info("Nothing to download for dataset " + sampleDataset.getPermId() + ".");
          continue;
        }

        final DownloadRequest downloadRequest = new DownloadRequest(files, sampleCode,
            DEFAULT_DOWNLOAD_ATTEMPTS);
        int filesDownloadedSuccessfully = downloadFiles(downloadRequest);
        if (filesDownloadedSuccessfully != 0) {
          returnCode += 1;
        }
      }
    }
    return returnCode;
  }

  private void downloadFile(DataSetFile dataSetFile, Path prefix) throws IOException {
    DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
    options.setRecursive(false);
    IDataSetFileId fileId = dataSetFile.getPermId();
    InputStream stream =
            this.dataStoreServer.downloadFiles(
                    sessionToken, Collections.singletonList(fileId), options);
    DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
    DataSetFileDownload file;

    while ((file = reader.read()) != null) {
      InputStream initialStream = file.getInputStream();
      CheckedInputStream checkedInputStream = new CheckedInputStream(initialStream, new CRC32());
      if (file.getDataSetFile().getFileLength() > 0) {
        final Path filePath = OutputPathFinder.determineFinalPathFromDataset(file.getDataSetFile(), conservePaths);
        final Path finalPath = OutputPathFinder.determineOutputDirectory(outputPath, prefix, file.getDataSetFile(), conservePaths);
        LOG.info("Output directory: " + finalPath.toAbsolutePath().getParent().toString());
        File newFile = new File(finalPath.toString());
        if(!newFile.getParentFile().exists()) {
          boolean successfullyCreatedDirectory = newFile.getParentFile().mkdirs();
          if (!successfullyCreatedDirectory) {
            LOG.error("Could not create directory " + newFile.getParentFile());
          }
        }
        OutputStream os = Files.newOutputStream(newFile.toPath());
        String fileName = filePath.getFileName().toString();
        ProgressBar progressBar =
                new ProgressBar(
                        fileName, file.getDataSetFile().getFileLength());
        int bufferSize =
                (file.getDataSetFile().getFileLength() < defaultBufferSize)
                        ? (int) file.getDataSetFile().getFileLength()
                        : defaultBufferSize;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        LOG.info(String.format("Download of %s is starting", fileName));
        // read from is to buffer
        while ((bytesRead = checkedInputStream.read(buffer)) != -1) {
          progressBar.updateProgress(bufferSize);
          os.write(buffer, 0, bytesRead);
          os.flush();
        }
        initialStream.close();
        // flush OutputStream to write any buffered data to file
        os.flush();
        LOG.info(String.format("Download of %s has finished", fileName));
        validateChecksum(
                Long.toHexString(checkedInputStream.getChecksum().getValue()), dataSetFile);
        if(invalidChecksumOccurred) {
          notifyUserOfInvalidChecksum(dataSetFile);
        }
        os.close();
      }
    }
  }

  private void validateChecksum(String computedChecksumHex, DataSetFile dataSetFile) {
    String expectedChecksum = Integer.toHexString(dataSetFile.getChecksumCRC32());
    try {
      if (computedChecksumHex.equals(expectedChecksum)) {
        checksumReporter.reportMatchingChecksum(
            expectedChecksum,
            computedChecksumHex,
            Paths.get(dataSetFile.getPath()).toUri().toURL());
      } else {
        checksumReporter.reportMismatchingChecksum(
            expectedChecksum,
            computedChecksumHex,
            Paths.get(dataSetFile.getPath()).toUri().toURL());
        invalidChecksumOccurred = true;
      }

    } catch (MalformedURLException e) {
      LOG.error(e);
    }
  }

  private int downloadFiles(DownloadRequest request) throws DownloadException {
    String sampleCode = request.getSampleCode();
    LOG.info(String.format("Downloading file(s) for sample %s", sampleCode));
    Path pathPrefix = Paths.get(sampleCode + File.separator);
    List<DataSetFile> dataSetFiles = request.getDataSets();
    for (DataSetFile dataSetFile : dataSetFiles) {
      attemptFileDownload(request, pathPrefix, dataSetFile);
    }
    return 0;
  }

  private void attemptFileDownload(DownloadRequest request, Path pathPrefix, DataSetFile dataSetFile) {
    try {
      int downloadAttempt = 1;
      while (downloadAttempt <= request.getMaxNumberOfAttempts()) {
        try {
          if (dataSetFile.getFileLength() > 0) {
            downloadFile(dataSetFile, pathPrefix);
            writeCRC32Checksum(dataSetFile, pathPrefix);
          } else {
            LOG.warn("Skipped empty file " + dataSetFile.getPath()
                .substring(dataSetFile.getPath().lastIndexOf("original/") + 9));
          }

          return;
        } catch (Exception e) {
          LOG.error(String.format("Download attempt %d failed.", downloadAttempt));
          LOG.error(String.format("Reason: %s", e.getMessage()), e);
          downloadAttempt++;
          if (downloadAttempt > request.getMaxNumberOfAttempts()) {
            throw new IOException("Maximum number of download attempts reached.");
          }
        }
      }
    } catch (IOException e) {
      String fileName = Paths.get(dataSetFile.getPath()).getFileName().toString();
      LOG.error(e);
      throw new DownloadException(
          "File " + fileName + " download failed.");
    }
  }

  private void writeCRC32Checksum(DataSetFile dataSetFile, Path pathPrefix) {

    Path path = OutputPathFinder.determineOutputDirectory(outputPath, pathPrefix ,dataSetFile, conservePaths);

    checksumReporter.storeChecksum(path, Integer.toHexString(dataSetFile.getChecksumCRC32()));
  }

  public void notifyUserOfInvalidChecksum(DataSetFile file) {
    LOG.warn(String.format("Checksum mismatches were detected for file %s. For more Information check the logs/summary_invalid_files.txt log file." , getFileName(file)));
  }

}
