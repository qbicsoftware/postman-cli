package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.model.files.FileSize;
import life.qbic.model.files.FileSizeFormatter;

public class QbicDataDisplay {

    String sessionToken;

    DateTimeFormatter utcDateTimeFormatterIso8601 = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'hh:mm:ss")
        .appendZoneId()
        .toFormatter()
        .withZone(ZoneOffset.UTC);

    private final QbicDataFinder qbicDataFinder;

    public QbicDataDisplay(
            String AppServerUri,
            String DataServerUri,
            String sessionToken) {
        this.sessionToken = sessionToken;
        IApplicationServerApi applicationServer;
        if (!AppServerUri.isEmpty()) {
            applicationServer =
                    HttpInvokerUtils.createServiceStub(
                            IApplicationServerApi.class, AppServerUri + IApplicationServerApi.SERVICE_URL, 10000);
        } else {
            applicationServer = null;
        }
        IDataStoreServerApi dataStoreServer;
        if (!DataServerUri.isEmpty()) {
            dataStoreServer =
                    HttpInvokerUtils.createStreamSupportingServiceStub(
                            IDataStoreServerApi.class, DataServerUri + IDataStoreServerApi.SERVICE_URL, 10000);
        } else {
            dataStoreServer = null;
        }
        qbicDataFinder = new QbicDataFinder(applicationServer, dataStoreServer, sessionToken);
    }

    public void getInformation(List<String> ids){
        for (String ident : ids) {
            Map<Sample, List<DataSet>> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);
            int fileCount = foundDataSets
                .values().stream()
                .mapToInt(List::size)
                .sum();
            System.out.printf("Number of datasets found for identifier %s : %s %n", ident,
                fileCount);
            if (foundDataSets.size() > 0) {
                printInformation(foundDataSets);
            }
        }
    }

    private void printInformation(Map<Sample, List<DataSet>> sampleDataSets) {
        for (Map.Entry<Sample, List<DataSet>> entry : sampleDataSets.entrySet()) {
            for (DataSet dataSet : entry.getValue()) {
                List<DataSetFile> dataSetFiles = qbicDataFinder.getFiles(dataSet.getPermId());

                //skip if no files found
                if (dataSetFiles.isEmpty()) {
                    continue;
                }

                long totalSize = dataSetFiles.stream().mapToLong(DataSetFile::getFileLength).sum();

                Sample analyte = searchAnalyteParent(entry.getKey());
                Date registrationDate = dataSet.getRegistrationDate();
                String iso_registrationDate = utcDateTimeFormatterIso8601.format(registrationDate.toInstant());
                int columnWidth = 16;
                System.out.printf("# %-" + columnWidth + "s %s (%s)%n", "Dataset",
                    dataSet.getSample().getCode(),
                    dataSet.getPermId());
                System.out.printf("# %-" + columnWidth + "s %s%n", "Source", analyte.getCode());
                System.out.printf("# %-" + columnWidth + "s %s%n", "Registration",
                    iso_registrationDate);
                System.out.printf("# %-" + columnWidth + "s %s%n", "Size",
                    FileSizeFormatter.format(FileSize.of(totalSize)));

                List<DataSetFile> sortedFiles = dataSetFiles.stream()
                    .sorted(Comparator.comparing(QbicDataDisplay::getFileName))
                    .collect(Collectors.toList());

                for (DataSetFile file : sortedFiles) {
                    String filePath = file.getPermId().getFilePath();
                    String name = filePath.substring(filePath.lastIndexOf("/") + 1);
                    String fileSize = FileSizeFormatter.format(FileSize.of(file.getFileLength()),6);
                    System.out.printf("%s\t%s%n", fileSize, name);
                }
                System.out.print("\n");
            }
        }
    }

    private static String getFileName(DataSetFile file) {
        String filePath = file.getPermId().getFilePath();
        return filePath.substring(filePath.lastIndexOf("/") + 1);
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


}
