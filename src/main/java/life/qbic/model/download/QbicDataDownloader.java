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
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
  private final List<IDataStoreServerApi> dataStoreServers;
  private final String sessionToken;
  private static final int DEFAULT_DOWNLOAD_ATTEMPTS = 3;
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
   * @param dataServerUris The openBIS datastore server URLs (DSS)
   * @param bufferSize    The buffer size for the InputStream reader
   * @param conservePaths Flag to conserve the file path structure during download
   * @param sessionToken The session token for the datastore & application servers
   */
  public QbicDataDownloader(
          String AppServerUri,
          List<String> dataServerUris,
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
    this.dataStoreServers = dataServerUris.stream()
        .filter(dataStoreServerUri -> !dataStoreServerUri.isEmpty())
        .map(dataStoreServerUri ->
            HttpInvokerUtils.createStreamSupportingServiceStub(
                IDataStoreServerApi.class, dataStoreServerUri + IDataStoreServerApi.SERVICE_URL,
                10000))
        .collect(Collectors.toList());
    qbicDataFinder = new QbicDataFinder(applicationServer, dataStoreServers, sessionToken);
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
    Predicate<DataSetFile> fileFilter = it -> true;
    if (Objects.nonNull(suffixes) && !suffixes.isEmpty()) {
      LOG.info(
          "Applying suffix filter for suffixes: [" + String.join(" ", suffixes) + "] ...");
      Predicate<DataSetFile> suffixFilter = file -> {
        String fileName = getFileName(file);
        return suffixes.stream().anyMatch(fileName::endsWith);
      };
      fileFilter = fileFilter.and(suffixFilter);
    }

    for (String ident : ids) {
      Map<Sample, List<DataSet>> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);

      if (foundDataSets.size() > 0) {
        Map<Sample, List<DataSet>> datasetsPerAnalyte = associateDatasetsWithAnalyte(foundDataSets);

        // ensures the order of download is the same always
        List<Entry<Sample, List<DataSet>>> sortedEntries = datasetsPerAnalyte
            .entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().getCode()))
            .collect(Collectors.toList());

        for (Entry<Sample, List<DataSet>> analyteDatasets : sortedEntries) {
          Sample analyte = analyteDatasets.getKey();
          List<DataSet> dataSets = analyteDatasets.getValue();
          try {
            downloadDatasets(analyte, dataSets, fileFilter);
          } catch (NullPointerException e) {
            LOG.error(
                "Datasets were found by the application server, but could not be found on the datastore server for "
                    + ident
                    + "."
                    + " Try to supply the correct datastore server using a config file!");
            e.printStackTrace();
          }
        }
      } else {
        LOG.info("Nothing to download for " + ident+".");
      }
    }
    LOG.info("Done");
  }

  private Map<Sample, List<DataSet>> associateDatasetsWithAnalyte(Map<Sample, List<DataSet>> foundDataSets) {
    Map<Sample, List<DataSet>> datasetsPerAnalyte = new HashMap<>();
    for (Entry<Sample, List<DataSet>> sampleListEntry : foundDataSets.entrySet()) {
      Sample analyte = qbicDataFinder.searchAnalyteParent(sampleListEntry.getKey());
      datasetsPerAnalyte.putIfAbsent(analyte, new ArrayList<>());
      datasetsPerAnalyte.get(analyte).addAll(sampleListEntry.getValue());
    }
    return datasetsPerAnalyte;
  }

  private String getFileName(DataSetFile file) {
    String filePath = file.getPermId().getFilePath();
    return filePath.substring(filePath.lastIndexOf("/") + 1);
  }

  private void downloadDatasets(Sample analyte, List<DataSet> dataSets,
      Predicate<DataSetFile> fileFilter) {
    List<DataSet> sortedDatasets = dataSets.stream()
        .sorted(Comparator.comparing(it -> it.getSample().getCode()))
        .collect(Collectors.toList());

    if (sortedDatasets.size() == 0) {
      return;
    }

    LOG.info(String.format("Found " + sortedDatasets.size() + " dataset(s) for sample %s",
        analyte.getCode()));
    for (DataSet dataSet : sortedDatasets) {
      DataSetPermId permID = dataSet.getPermId();
      List<DataSetFile> files = qbicDataFinder.getFiles(permID, fileFilter);
      String datasetSample = dataSet.getSample().getCode();
      if (files.isEmpty()) {
        LOG.info("Nothing to download for dataset " + datasetSample + " (" + permID + ")" + ".");
        continue;
      }
      final DownloadRequest downloadRequest = new DownloadRequest(
          Paths.get(datasetSample + File.separator), files, DEFAULT_DOWNLOAD_ATTEMPTS);
      LOG.info(
          "Downloading " + files.size() + " file" + (files.size() != 1 ? "s" : "") + " for dataset "
              + datasetSample + " (" + permID + ")");
      downloadFiles(downloadRequest);
    }
  }

  private void downloadFile(DataSetFile dataSetFile, Path prefix) throws DownloadException {
    for (IDataStoreServerApi dataStoreServer : dataStoreServers) {
      try {
        downloadFileFromDataStoreServer(dataSetFile, prefix, dataStoreServer);
        break;
      } catch (FileNotFoundException fileNotFoundException) {
        // log and try the next data store server
        LOG.debug("Download attempt failed on " + dataStoreServer, fileNotFoundException);
        continue;
      }
    }
  }

  /**
   * @throws FileNotFoundException if the file was not found on the data store server.
   */
  private void downloadFileFromDataStoreServer(DataSetFile dataSetFile, Path prefix,
      IDataStoreServerApi dataStoreServerApi) throws FileNotFoundException {

    DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
    options.setRecursive(false);

    // note: DataSetFileDownloadReader closes the input stream after it finished reading it
    DataSetFileDownloadReader reader = new DataSetFileDownloadReader(
        dataStoreServerApi.downloadFiles(sessionToken,
            Collections.singletonList(dataSetFile.getPermId()), options));
    try {
      // we expect exactly one file
      DataSetFileDownload file = reader.read();
      if (Objects.isNull(file)) {
        throw new FileNotFoundException("No file " + dataSetFile.getPermId() + " found");
      }
      if (file.getDataSetFile().getFileLength() > 0) {
        final Path finalPath = OutputPathFinder.determineOutputDirectory(outputPath, prefix,
            file.getDataSetFile(), conservePaths);
        File newFile = new File(finalPath.toString());
        if (!newFile.getParentFile().exists()) {
          boolean successfullyCreatedDirectory = newFile.getParentFile().mkdirs();
          if (!successfullyCreatedDirectory) {
            LOG.error("Could not create directory " + newFile.getParentFile());
          }
        }
        long computedChecksum;

        String fileName = finalPath.getFileName().toString();
        ProgressBar progressBar =
            new ProgressBar(
                fileName, file.getDataSetFile().getFileLength());
        int bufferSize =
            (file.getDataSetFile().getFileLength() < defaultBufferSize)
                ? (int) file.getDataSetFile().getFileLength()
                : defaultBufferSize;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        try (InputStream initialStream = file.getInputStream();
            OutputStream os = Files.newOutputStream(newFile.toPath());
            CheckedInputStream checkedInputStream = new CheckedInputStream(initialStream,
                new CRC32())) {
          // read from is to buffer
          while ((bytesRead = checkedInputStream.read(buffer)) != -1) {
            progressBar.updateProgress(bufferSize);
            os.write(buffer, 0, bytesRead);
            os.flush();
          }
          // flush OutputStream to write any buffered data to file
          os.flush();
          computedChecksum = checkedInputStream.getChecksum().getValue();
        } catch (IOException e) {
          throw new DownloadException(e);
        }
        ChecksumValidationResult checksumValidationResult = validateChecksum(computedChecksum,
            dataSetFile);
        if (checksumValidationResult.isValid()) {
          LOG.info("Download successful for " + finalPath.toAbsolutePath());
        } else if (checksumValidationResult.isInvalid()) {
          LOG.warn(String.format(
              "Checksum mismatches were detected for file %s.%nFor more Information check the logs/summary_invalid_files.txt log file.",
              finalPath.toAbsolutePath()));
        }
        reportValidation(checksumValidationResult);
      } else {
        LOG.info("Skipped empty file " + file.getDataSetFile().getPath());
      }

      // consume the rest of the stream
      while (Objects.nonNull(file = reader.read())) {
        // we only expected exactly one file
        LOG.debug("Ignored file " + file.getDataSetFile().getPath() + "; Expected one file exactly");
      }
    } finally {
      reader.close();
    }
  }

  private static class ChecksumValidationResult {
    private final String expectedChecksum;
    private final String computedChecksum;
    private final DataSetFile dataSetFile;


    private ChecksumValidationResult(String expectedChecksum, String computedChecksum,
        DataSetFile dataSetFile) {
      Objects.requireNonNull(expectedChecksum);
      Objects.requireNonNull(computedChecksum);
      Objects.requireNonNull(dataSetFile);

      this.expectedChecksum = expectedChecksum;
      this.computedChecksum = computedChecksum;
      this.dataSetFile = dataSetFile;
    }

    public String expectedChecksum() {
      return expectedChecksum;
    }

    public String computedChecksum() {
      return computedChecksum;
    }

    public DataSetFile dataSetFile() {
      return dataSetFile;
    }

    public boolean isValid() {
      return computedChecksum.equals(expectedChecksum);
    }

    public boolean isInvalid() {
      return !isValid();
    }
  }


  private void reportValidation(ChecksumValidationResult validation) {
    try {
      if (validation.isValid()) {
        checksumReporter.reportMatchingChecksum(
            validation.expectedChecksum(),
            validation.computedChecksum(),
            Paths.get(validation.dataSetFile().getPath()).toUri().toURL());
      } else {
        checksumReporter.reportMismatchingChecksum(
            validation.expectedChecksum(),
            validation.computedChecksum(),
            Paths.get(validation.dataSetFile().getPath()).toUri().toURL());
      }
    } catch (MalformedURLException e) {
      LOG.error(e);
    }
  }

  private ChecksumValidationResult validateChecksum(long computedChecksum, DataSetFile dataSetFile) {
    String expectedChecksumHex = Integer.toHexString(dataSetFile.getChecksumCRC32());
    String computedChecksumHex = Long.toHexString(computedChecksum);
    return new ChecksumValidationResult(
        expectedChecksumHex, computedChecksumHex, dataSetFile);
  }

  private void downloadFiles(DownloadRequest request) throws DownloadException {
    List<DataSetFile> dataSetFiles = request.getFiles().stream()
        .sorted(Comparator.comparing(this::getFileName))
        .collect(Collectors.toList());
    List<DataSetFile> filesNotDownloaded = new ArrayList<>();
    for (DataSetFile dataSetFile : dataSetFiles) {
      try {
        attemptFileDownload(request.getPrefix(),dataSetFile, request.getMaxNumberOfAttempts());
      } catch (DownloadException e) {
        filesNotDownloaded.add(dataSetFile);
      }
    }
    if (filesNotDownloaded.size() > 0) {
      LOG.warn(String.format("Failed to download %s out of %s files", filesNotDownloaded.size(),
          dataSetFiles.size()));
    }
  }

  private void attemptFileDownload(Path pathPrefix,
      DataSetFile dataSetFile, int maxNumberOfAttempts) throws DownloadException {
    int downloadAttempt = 1;
    while (downloadAttempt <= maxNumberOfAttempts) {
      try {
        if (dataSetFile.getFileLength() > 0) {
          downloadFile(dataSetFile, pathPrefix);
          writeCRC32Checksum(dataSetFile, pathPrefix);
        } else {
          String prefix = "original/";
          LOG.warn("Skipped empty file " + dataSetFile.getPath()
              .substring(dataSetFile.getPath().lastIndexOf(prefix) + prefix.length()));
        }
        return;
      } catch (Exception e) {
        LOG.error(String.format("Download attempt %d failed.", downloadAttempt));
        LOG.error(String.format("Reason: %s", e.getMessage()));
        LOG.debug(e);
        downloadAttempt++;
        if (downloadAttempt > maxNumberOfAttempts) {
          throw new DownloadException("Maximum number of download attempts reached.");
        }
      }
    }
  }

  private void writeCRC32Checksum(DataSetFile dataSetFile, Path pathPrefix) {

    Path path = OutputPathFinder.determineOutputDirectory(outputPath, pathPrefix ,dataSetFile, conservePaths);

    checksumReporter.storeChecksum(path, Integer.toHexString(dataSetFile.getChecksumCRC32()));
  }

}
