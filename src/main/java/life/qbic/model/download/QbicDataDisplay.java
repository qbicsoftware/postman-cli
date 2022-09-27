package life.qbic.model.download;

import static life.qbic.model.units.UnitConverterFactory.determineBestUnitType;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.model.units.UnitDisplay;

public class QbicDataDisplay {
    private final IApplicationServerApi applicationServer;
    private final IDataStoreServerApi dataStoreServer;
    String sessionToken;

    DateTimeFormatter utcDateTimeFormatterIso8601 = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'hh:mm:ss")
        .appendZoneId()
        .toFormatter()
        .withZone(ZoneOffset.UTC);

    public QbicDataDisplay(
            String AppServerUri,
            String DataServerUri,
            String sessionToken) {
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

    public void GetInformation(List<String> ids){
        QbicDataFinder qbicDataFinder =
                new QbicDataFinder(applicationServer, dataStoreServer, sessionToken);
        for (String ident : ids) {
            Map<Sample, List<DataSet>> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);
            System.out.printf("Number of datasets found for identifier %s : %s %n", ident,
                    QbicDataDownloader.countFiles(foundDataSets));
            if (foundDataSets.size() > 0) {
                printInformation(foundDataSets);
            }
        }
    }

    private void printInformation(Map<Sample, List<DataSet>> sampleDataSets) {
        for (Map.Entry<Sample, List<DataSet>> entry : sampleDataSets.entrySet()) {
            for (DataSet dataSet : entry.getValue()) {
                List<DataSetFile> dataSetFiles = getFiles(dataSet.getPermId());

                //skip if no files found
                if (dataSetFiles.isEmpty()) {
                    continue;
                }
                Sample analyte = searchAnalyteParent(entry.getKey());
                Date registrationDate = dataSet.getRegistrationDate();
                String iso_registrationDate = utcDateTimeFormatterIso8601.format(registrationDate.toInstant());
                System.out.printf("# Dataset %s (%s)%n", dataSet.getSample().getCode(), dataSet.getPermId());
                System.out.printf("# Source %s %n", analyte.getCode());
                System.out.printf("# Registration %s %n", iso_registrationDate);

                for (DataSetFile file : dataSetFiles) {
                    String filePath = file.getPermId().getFilePath();
                    String name = filePath.substring(filePath.lastIndexOf("/") + 1);
                    long size = file.getFileLength();
                    // units used here have base 2
                    UnitDisplay bestUnit = determineBestUnitType(size);
                    Double finalSize = Math.round(10.0 * bestUnit.convertBytesToUnit(size)) / 10.0;

                    System.out.printf("%s\t%s %s%n", name, finalSize, bestUnit.getUnitType());
                }
                System.out.print("\n");
            }
        }
    }

    /**
     * Searches the parents for a Q_TEST_SAMPLE assuming at most one Q_TEST_SAMPLE exists in the parent samples.
     * If not Q_TEST_SAMPLE was found, the original sample is returned.
     * @param sample the sample to which a dataset is attached to
     * @return the Q_TEST_SAMPLE parent if exists, the sample itself otherwise.
     */
    private Sample searchAnalyteParent(Sample sample) {
        Optional<Sample> firstTestSample = sample.getParents().stream()
            .filter(
                it -> it.getType().getCode().equals("Q_TEST_SAMPLE"))
            .findFirst();
        return firstTestSample.orElse(sample);
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
