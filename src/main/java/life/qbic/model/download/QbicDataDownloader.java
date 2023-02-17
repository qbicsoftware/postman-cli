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
import life.qbic.ChecksumReporter;
import life.qbic.DownloadException;
import life.qbic.DownloadRequest;
import life.qbic.FileSystemWriter;
import life.qbic.model.Configuration;
import life.qbic.util.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class QbicDataDownloader {

  private static final Logger LOG = LogManager.getLogger(QbicDataDownloader.class);
  private final int defaultBufferSize;
  private final boolean conservePaths;
  private final List<IDataStoreServerApi> dataStoreServers;
  private final String sessionToken;
  private final String outputPath;

  private final ChecksumReporter checksumReporter = new FileSystemWriter(
      Paths.get(Configuration.LOG_PATH.toString(), "summary_valid_files.txt"),
      Paths.get(Configuration.LOG_PATH.toString(), "summary_invalid_files.txt"));
  private final QbicDataFinder qbicDataFinder;

  /**
   * Constructor for a QBiCDataDownloader instance
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
   * Filters the files as specified by the user and downloads them.
   */
  public DownloadResponse downloadForIds(List<String> ids, List<String> suffixes) {
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

    DownloadResponse downloadResponse = DownloadResponse.create();
    for (String ident : ids) {
      downloadResponse.merge(downloadForId(fileFilter, ident));
    }
    return downloadResponse;
  }

  private DownloadResponse downloadForId(Predicate<DataSetFile> fileFilter, String ident) {
    Map<Sample, List<DataSet>> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);

    if (foundDataSets.size() > 0) {
      Map<Sample, List<DataSet>> datasetsPerAnalyte = associateDatasetsWithAnalyte(foundDataSets);

      // ensures the order of download is the same always
      List<Entry<Sample, List<DataSet>>> sortedEntries = datasetsPerAnalyte
          .entrySet().stream()
          .sorted(Comparator.comparing(entry -> entry.getKey().getCode()))
          .collect(Collectors.toList());

      DownloadResponse response = DownloadResponse.create();
      for (Entry<Sample, List<DataSet>> analyteDatasets : sortedEntries) {
        response.merge(downloadForAnalyte(fileFilter, analyteDatasets));
      }
      return response;
    } else {
      LOG.info("Nothing to download for " + ident + ".");
      return DownloadResponse.create();
    }
  }

  private DownloadResponse downloadForAnalyte(Predicate<DataSetFile> fileFilter,
      Entry<Sample, List<DataSet>> analyteDatasets) {

    Sample analyte = analyteDatasets.getKey();
    List<DataSet> dataSets = analyteDatasets.getValue();
    return downloadDatasets(analyte, dataSets, fileFilter);
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

  private DownloadResponse downloadDatasets(Sample analyte, List<DataSet> dataSets,
      Predicate<DataSetFile> fileFilter) {

    if (dataSets.isEmpty()) {
      return DownloadResponse.create();
    }

    List<DataSet> sortedDatasets = dataSets.stream()
        .sorted(Comparator.comparing(it -> it.getSample().getCode()))
        .collect(Collectors.toList());

    LOG.info(String.format("Found " + sortedDatasets.size() + " dataset(s) for sample %s",
        analyte.getCode()));
    DownloadResponse downloadResponse = DownloadResponse.create();
    for (DataSet dataSet : sortedDatasets) {
      DataSetPermId permID = dataSet.getPermId();
      List<DataSetFile> files = qbicDataFinder.getFiles(permID, fileFilter);
      String datasetSample = dataSet.getSample().getCode();
      if (files.isEmpty()) {
        LOG.info("Nothing to download for dataset " + datasetSample + " (" + permID + ")" + ".");
        continue;
      }
      final DownloadRequest downloadRequest = new DownloadRequest(
          Paths.get(datasetSample + File.separator), files, Configuration.MAX_DOWNLOAD_ATTEMPTS);
      LOG.info(
          "Downloading " + files.size() + " file" + (files.size() != 1 ? "s" : "") + " for dataset "
              + datasetSample + " (" + permID + ")");
      downloadResponse.merge(downloadFiles(downloadRequest));
    }
    return downloadResponse;
  }

  private FileDownloadResponse downloadFile(DataSetFile dataSetFile, Path prefix) throws DownloadException {
    for (IDataStoreServerApi dataStoreServer : dataStoreServers) {
      try {
        return downloadFileFromDataStoreServer(dataSetFile, prefix, dataStoreServer);
      } catch (FileNotFoundException fileNotFoundException) {
        // log and try the next data store server
        LOG.debug("Download attempt failed on " + dataStoreServer, fileNotFoundException);
      }
    }
    return FileDownloadResponse.failure();
  }

  // note: DataSetFileDownloadReader closes the input stream after it finished reading it.
  private static class AutoClosableDataSetFileDownloadReader extends DataSetFileDownloadReader implements AutoCloseable {
    public AutoClosableDataSetFileDownloadReader(InputStream in) {
      super(in);
    }
  }

  public static class FileDownloadResponse {
    private final boolean success;

    private FileDownloadResponse(boolean success) {
      this.success = success;
    }

    public static FileDownloadResponse success() {
      return new FileDownloadResponse(true);
    }

    private static FileDownloadResponse failure() {
      return new FileDownloadResponse(false);
    }

    public boolean isSuccess() {
      return success;
    }

    public boolean isFailure() {
      return !success;
    }
  }


  /**
   * @throws FileNotFoundException if the file was not on the data store server.
   */
  private FileDownloadResponse downloadFileFromDataStoreServer(DataSetFile dataSetFile, Path prefix,
      IDataStoreServerApi dataStoreServerApi) throws FileNotFoundException {

    // skip if file is empty
    if (dataSetFile.getFileLength() < 1) {
      LOG.info("Skipped empty file " + dataSetFile.getPath());
      return FileDownloadResponse.success();
    }

    Path localFilePath = OutputPathFinder.determineOutputDirectory(outputPath, prefix, dataSetFile,
        conservePaths);
    File localFile = localFilePath.toFile();

    // skip if same content exists already
    if (localFile.exists()) {
      System.out.print("Found existing file. Checking content...");

      if (localFileWithSameContent(dataSetFile, localFile)) {
        System.out.println("\rFound existing file with identical content. Skipping "
            + localFile.getAbsolutePath());
        return FileDownloadResponse.success();
      } else {
        System.out.println("\rUpdating existing file " + localFile.getAbsolutePath());
      }
    }

    createParentDirectoryIfNotExists(localFile);

    long computedChecksum = writeFileToDisk(dataSetFile, dataStoreServerApi, localFilePath);
    // validate written file
    ChecksumValidationResult checksumValidationResult = validateChecksum(computedChecksum,
        dataSetFile);
    reportValidation(checksumValidationResult);
    if (checksumValidationResult.isValid()) {
      LOG.info("Download successful for " + localFile.getAbsolutePath());
      return FileDownloadResponse.success();
    } else {
      LOG.warn(String.format(
          "Checksum mismatches were detected for file %s.%nFor more Information check the logs/summary_invalid_files.txt log file.",
          localFile.getAbsolutePath()));
      return FileDownloadResponse.failure();
    }
  }

  private long writeFileToDisk(DataSetFile dataSetFile, IDataStoreServerApi dataStoreServerApi,
      Path localFilePath) throws FileNotFoundException {

    long computedChecksum;
    DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
    options.setRecursive(false);
    try (AutoClosableDataSetFileDownloadReader reader = new AutoClosableDataSetFileDownloadReader(
        dataStoreServerApi.downloadFiles(sessionToken,
            Collections.singletonList(dataSetFile.getPermId()), options))) {

      DataSetFileDownload fileDownload = reader.read();
      // there is no file in the input stream
      if (Objects.isNull(fileDownload)) {
        throw new FileNotFoundException("No file " + dataSetFile.getPermId() + " found");
      }

      // write the file
      try (
          InputStream initialStream = fileDownload.getInputStream();
          OutputStream os = Files.newOutputStream(localFilePath);
          CheckedInputStream checkedInputStream = new CheckedInputStream(initialStream,
              new CRC32())) {

        long fileLength = dataSetFile.getFileLength();
        String fileName = localFilePath.getFileName().toString();
        ProgressBar progressBar = new ProgressBar(fileName, fileLength);
        int bufferSize = adjustedBufferSize(fileLength);

        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = checkedInputStream.read(buffer)) != -1) {
          progressBar.updateProgress(bufferSize);
          progressBar.draw();
          os.write(buffer, 0, bytesRead);
          os.flush();
        }
        // flush OutputStream to write any buffered data to file
        os.flush();

        computedChecksum = checkedInputStream.getChecksum().getValue();
      } catch (IOException e) {
        throw new DownloadException(e);
      }
    }
    return computedChecksum;
  }

  private boolean localFileWithSameContent(DataSetFile dataSetFile, File localFile) {
    ChecksumValidationResult checksumValidationResult = checksumForFile(dataSetFile, localFile);
    if (checksumValidationResult.isValid()) {
      LOG.debug(checksumValidationResult.computedChecksum + " " + localFile.getName()
          + " exists locally.");
      return true;
    }
    return false;
  }

  private static void createParentDirectoryIfNotExists(File localFile) {
    if (!localFile.getParentFile().exists()) {
      boolean successfullyCreatedDirectory = localFile.getParentFile().mkdirs();
      if (!successfullyCreatedDirectory) {
        throw new DownloadException("Could not create directory " + localFile.getParentFile());
      }
    }
  }

  private ChecksumValidationResult checksumForFile(DataSetFile dataSetFile, File localFile) {
    try (CheckedInputStream existingFileReader = new CheckedInputStream(
        Files.newInputStream(localFile.toPath()), new CRC32())) {
      int bufferSize = adjustedBufferSize(dataSetFile.getFileLength());
      byte[] buffer = new byte[bufferSize];
      while (existingFileReader.read(buffer) != -1) {
        // reading
      }
      return validateChecksum(
          existingFileReader.getChecksum().getValue(), dataSetFile);
    } catch (IOException e) {
      throw new RuntimeException("Existing file could not be processed", e);
    }
  }

  private int adjustedBufferSize(long fileLength) {
    return (fileLength < defaultBufferSize) ? (int) fileLength : defaultBufferSize;
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

  public static class DownloadResponse {

    private final List<FileDownloadResponse> responses;

    private DownloadResponse() {
      responses = new ArrayList<>();
    }

    public static DownloadResponse create() {
      return new DownloadResponse();
    }

    public void add(FileDownloadResponse response) {
      responses.add(response);
    }

    public DownloadResponse merge(DownloadResponse response) {
      responses.addAll(response.responses);
      return this;
    }

    public boolean containsFailure() {
      return responses.stream().anyMatch(FileDownloadResponse::isFailure);
    }

    public long failureCount() {
      return responses.stream().filter(FileDownloadResponse::isFailure).count();
    }

    public long fileCount() {
      return responses.size();
    }

  }
  private DownloadResponse downloadFiles(DownloadRequest request) throws DownloadException {
    List<DataSetFile> dataSetFiles = request.getFiles().stream()
        .sorted(Comparator.comparing(this::getFileName))
        .collect(Collectors.toList());
    DownloadResponse downloadResponse = DownloadResponse.create();
    for (DataSetFile dataSetFile : dataSetFiles) {
      FileDownloadResponse fileDownloadResponse = attemptFileDownload(request.getPrefix(),
          dataSetFile, request.getMaxNumberOfAttempts());
      downloadResponse.add(fileDownloadResponse);
    }

    assert dataSetFiles.size() == downloadResponse.fileCount() : "each file gave a response";

    if (downloadResponse.containsFailure()) {
      LOG.warn(String.format("Failed to download %s out of %s files",
          downloadResponse.failureCount(), dataSetFiles.size()));
    }
    return downloadResponse;
  }

  private FileDownloadResponse attemptFileDownload(Path pathPrefix,
      DataSetFile dataSetFile, long maxNumberOfAttempts) throws DownloadException {
    int downloadAttempt = 1;
    assert maxNumberOfAttempts >= 1 : "max download attempts are at least 1";
    while (true) {
      if (downloadAttempt > maxNumberOfAttempts) {
        LOG.error("Maximum number of download attempts reached.");
        return FileDownloadResponse.failure();
      }
      try {
        FileDownloadResponse fileDownloadResponse = downloadFile(dataSetFile, pathPrefix);
        writeCRC32Checksum(dataSetFile, pathPrefix);
        if (fileDownloadResponse.isFailure()) {
          LOG.error("Download attempt " + downloadAttempt + " failed.");
          downloadAttempt++;
          continue;
        }
        return fileDownloadResponse;
      } catch (Exception e) {
        LOG.error(String.format("Download attempt %d failed.", downloadAttempt));
        LOG.error(String.format("Reason: %s", e.getMessage()));
        LOG.debug(e);
        downloadAttempt++;
      }
    }
  }

  private void writeCRC32Checksum(DataSetFile dataSetFile, Path pathPrefix) {

    Path path = OutputPathFinder.determineOutputDirectory(outputPath, pathPrefix ,dataSetFile, conservePaths);

    checksumReporter.storeChecksum(path, Integer.toHexString(dataSetFile.getChecksumCRC32()));
  }

}
