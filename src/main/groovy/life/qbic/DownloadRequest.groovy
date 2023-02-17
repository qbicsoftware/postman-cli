package life.qbic

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile

import java.nio.file.Path

/**
 * Contains information about a download request for openBIS
 * data set files.
 *
 * Allows to add and access data set files to and from a request
 * Allows for quicks access of CRC32 checksums of corresponding data set files.
 *
 * @author Sven Fillinger
 * @since 0.4.0
 */
class DownloadRequest {
  private final List<DataSetFile> dataSetFiles
  private final Path prefix
  private final long retries


  /**
   * Download request constructor with a provided list of data set files and a configured
   * number of retries on failure.
   *
   * @param datasetSampleCode the code of the sample to which the dataset was attached
   * @param dataSetFiles the files to download
   * @param numberRetries The number of retries. Must be >=1, else it will be set to 1
   */
  DownloadRequest(Path prefix, List<DataSetFile> dataSetFiles, long numberRetries) {
    Objects.requireNonNull(prefix, "prefix must not be null")
    Objects.requireNonNull(dataSetFiles, "files must not be null")

    this.dataSetFiles = new ArrayList<>()
    this.dataSetFiles.addAll(dataSetFiles)
    this.retries = numberRetries >= 1 ? numberRetries : 1
    this.prefix = prefix
  }

  /**
   * Returns the max number of attempts for the download request.
   * @return The number of attempts to perform, if the download fails
   */
  long getMaxNumberOfAttempts() {
    return retries
  }

  /**
   * Returns a shallow copy of the data set file list from the download request.
   * @return A list of all requested data set files.
   */
  List<DataSetFile> getFiles() {
    dataSetFiles.asUnmodifiable()
  }

  Path getPrefix() {
    return prefix
  }
}
