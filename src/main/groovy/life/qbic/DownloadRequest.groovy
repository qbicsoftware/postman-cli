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

    final private Map<String, DataSetFile> dataSetByPermId

    private String sampleCode

    private int retries

    DownloadRequest() {
        this.dataSetByPermId = new HashMap<>()
        this.sampleCode = ""
        this.retries = 1
    }

    /**
     * Download request constructor with a provided list of data set files.
     *
     * @param dataSetFiles
     */
    DownloadRequest(List<DataSetFile> dataSetFiles, String sampleCode) {
        this()
        this.sampleCode = sampleCode
        dataSetFiles.each { dsFile ->
            addDataSet(dsFile.getPermId().toString(), dsFile)
        }
        this.retries = 1
    }

    /**
     * Download request constructor with a provided list of data set files and a configured
     * number of retries on failure.
     *
     * @param dataSetFiles
     * @param sampleCode
     * @param numberRetries The number of retries. Must be >=1, else it will be set to 1
     */
    DownloadRequest(List<DataSetFile> dataSetFiles, String sampleCode, int numberRetries) {
        this()
        this.sampleCode = sampleCode
        dataSetFiles.each { dsFile ->
            addDataSet(dsFile.getPermId().toString(), dsFile)
        }
        this.retries = numberRetries >= 1 ? numberRetries : 1
    }

    /**
     * Adds a data set file to the download request.
     *
     * @param permId The openBIS permId.
     * @param dataSetFile A data set file that shall be downloaded.
     */
    void addDataSet(String permId, DataSetFile dataSetFile) {
        if (dataSetByPermId.containsKey(permId)) {
            def list = dataSetByPermId[permId]
            list << dataSetFile
        } else {
            dataSetByPermId[permId] = dataSetFile
        }
    }
    /**
     * Returns the CRC32 checksum for a data set with the provided permID.
     *
     * @param permId An openBIS data set file permId for which the checksum shall be returned.
     * @return The associated CRC32 checksum from the openBIS data base.
     */
    int getCRC32sum(String permId) {
        if (!dataSetByPermId[permId].checksumCRC32) {
            new IllegalArgumentException("The provided argument does not represent a known ID.")
        }
        return dataSetByPermId[permId].checksumCRC32
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
    List<DataSetFile> getDataSets() {
        dataSetByPermId.values().collect()
    }

    String getSampleCode() {
        sampleCode
    }

}
