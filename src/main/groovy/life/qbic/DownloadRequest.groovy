package life.qbic

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile

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

  private final String datasetSampleCode;

  private final List<DataSetFile> dataSetFiles;

  private final int retries


  /**
   * Download request constructor with a provided list of data set files and a configured
   * number of retries on failure.
   *
   * @param datasetSampleCode the code of the sample to which the dataset was attached
   * @param dataSetFiles the files to download
   * @param numberRetries The number of retries. Must be >=1, else it will be set to 1
   */
  DownloadRequest(String datasetSampleCode, List<DataSetFile> dataSetFiles, int numberRetries) {
    Objects.requireNonNull(datasetSampleCode, "sample code of dataset must not be null")
    Objects.requireNonNull(dataSetFiles, "dataset files must not be null")

    this.dataSetFiles = new ArrayList<>()
    this.dataSetFiles.addAll(dataSetFiles)
    this.retries = numberRetries >= 1 ? numberRetries : 1
    this.datasetSampleCode = datasetSampleCode
  }

  /**
   * Returns the max number of attempts for the download request.
   * @return The number of attempts to perform, if the download fails
   */
  int getMaxNumberOfAttempts() {
    return retries
  }

  /**
   * Returns a shallow copy of the data set file list from the download request.
   * @return A list of all requested data set files.
   */
  List<DataSetFile> getFiles() {
    dataSetFiles.asUnmodifiable()
  }

  String getDatasetSampleCode() {
    return datasetSampleCode
  }
}
