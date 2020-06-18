package life.qbic

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile

/**
 * <add class description here>
 *
 * @author: Sven Fillinger
 */
class DownloadRequest {

    final private Map<String, DataSetFile> dataSetByPermId

    DownloadRequest() {
        this.dataSetByPermId = new HashMap<>()
    }

    DownloadRequest(List<DataSetFile> dataSetFiles) {
        this()
        dataSetFiles.each { dsFile ->
            addDataSet(dsFile.getPermId().toString(), dsFile)
        }
    }

    def addDataSet(String permId, DataSetFile dataSetFile) {
        if (dataSetByPermId.containsKey(permId)) {
            def list = dataSetByPermId[permId]
            list << dataSetFile
        } else {
            dataSetByPermId[permId] = dataSetFile
        }
    }

    int getCRC32sum(String permId) {
        if (! dataSetByPermId[permId].checksumCRC32) {
            new IllegalArgumentException("")
        }
       return dataSetByPermId[permId].checksumCRC32
    }

    List<DataSetFile> getDataSets() {
        dataSetByPermId.values().collect()
    }

}
