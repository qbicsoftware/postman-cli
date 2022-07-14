package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class QbicDataStatus {
    private final IApplicationServerApi applicationServer;
    private final IDataStoreServerApi dataStoreServer;
    String filterType;
    String sessionToken;

    public QbicDataStatus(
            String AppServerUri,
            String DataServerUri,
            String filterType,
            String sessionToken) {
        this.filterType = filterType;
        this.sessionToken = sessionToken;
        if (!AppServerUri.isEmpty()) {
            this.applicationServer =
                    HttpInvokerUtils.createServiceStub(
                            IApplicationServerApi.class, AppServerUri + IApplicationServerApi.SERVICE_URL, 10000);
        } else {
            this.applicationServer = null;
        }
        if (!DataServerUri.isEmpty()) {
            this.dataStoreServer =
                    HttpInvokerUtils.createStreamSupportingServiceStub(
                            IDataStoreServerApi.class, DataServerUri + IDataStoreServerApi.SERVICE_URL, 10000);
        } else {
            this.dataStoreServer = null;
        }
    }

    public void GetDataStatus(PostmanCommandLineOptions commandLineParameters){
        QbicDataFinder qbicDataFinder =
                new QbicDataFinder(applicationServer, dataStoreServer, sessionToken, filterType);

        for (String ident : commandLineParameters.ids) {
            Map<String, List<DataSet>> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);
            System.out.printf("Number of datasets found for identifier %s : %s%n", ident,
                    QbicDataDownloader.countDatasets(foundDataSets));
            if (foundDataSets.size() > 0) {
                printFileInformation(foundDataSets);
            }
        }
    }

    private void printFileInformation(Map<String, List<DataSet>> sampleDataSets) {

            for (Map.Entry<String, List<DataSet>> entry : sampleDataSets.entrySet()) {
                for (DataSet dataSet : entry.getValue()) {
                    DataSetPermId permID = dataSet.getPermId();
                    System.out.printf("\tDataset %s (%s)%n",permID,entry.getKey());
                    List<DataSetFile> dataSetFiles = getFiles(permID);
                    for (DataSetFile file : dataSetFiles) {
                        String filePath = file.getPermId().getFilePath();
                        String name = filePath.substring(filePath.lastIndexOf("/") + 1);
                        Long length = file.getFileLength();
                        String unit = "Bytes";
                        Date registrationDate = file.getDataStore().getRegistrationDate();
                        String iso_registrationDate = registrationDate.toInstant().atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"));

                        System.out.printf("\t\t%s\t%s\t%s %s%n",iso_registrationDate, name, length, unit);
                    }
                }
            }
    }

    private List<DataSetFile> getFiles(DataSetPermId permID) {
        DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
        criteria.withDataSet().withCode().thatEquals(permID.getPermId());
        SearchResult<DataSetFile> result =
                this.dataStoreServer.searchFiles(sessionToken, criteria,
                        new DataSetFileFetchOptions());
        return QbicDataDownloader.withoutDirectories(result.getObjects());
    }
}
