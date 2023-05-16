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
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import life.qbic.model.files.FileSize;
import life.qbic.model.files.FileSizeFormatter;

/**
 * Lists information about requested datasets and their files
 */
public class QbicDataDisplay {

    final String sessionToken;

    private final static DateTimeFormatter utcDateTimeFormatterIso8601 = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'hh:mm:ss")
        .appendZoneId()
        .toFormatter()
        .withZone(ZoneOffset.UTC);

    private final QbicDataFinder qbicDataFinder;

    private final boolean printWithChecksums;

    /**
     * Constructor for a QbicDataDisplay instance
     *
     * @param AppServerUri   The openBIS application server URL (AS)
     * @param dataServerUris The openBIS datastore server URLs (DSS)
     * @param sessionToken   The session token for the datastore & application servers
     */
    public QbicDataDisplay(
        String AppServerUri,
        List<String> dataServerUris,
        String sessionToken,
        boolean printWithChecksums) {
        this.printWithChecksums = printWithChecksums;
        this.sessionToken = sessionToken;
        IApplicationServerApi applicationServer;
        if (!AppServerUri.isEmpty()) {
            applicationServer =
                HttpInvokerUtils.createServiceStub(
                    IApplicationServerApi.class, AppServerUri + IApplicationServerApi.SERVICE_URL,
                    10000);
        } else {
            applicationServer = null;
        }
        List<IDataStoreServerApi> dataStoreServerApis = dataServerUris.stream()
            .filter(dataStoreServerUri -> !dataStoreServerUri.isEmpty())
            .map(dataStoreServerUri ->
                HttpInvokerUtils.createStreamSupportingServiceStub(
                    IDataStoreServerApi.class, dataStoreServerUri + IDataStoreServerApi.SERVICE_URL,
                    10000))
            .collect(Collectors.toList());
        qbicDataFinder = new QbicDataFinder(applicationServer, dataStoreServerApis, sessionToken);
    }

    public void getInformation(List<String> ids, List<String> suffixes){
        for (String ident : ids) {
            Map<Sample, List<DataSet>> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);
            int datasetCount = foundDataSets
                .values().stream()
                .mapToInt(List::size)
                .sum();
            System.out.printf("Number of datasets found for identifier %s : %s %n", ident,
                datasetCount);
            if (foundDataSets.size() > 0) {
                printInformation(foundDataSets, suffixes);
            }
        }
    }

    private void printInformation(Map<Sample, List<DataSet>> sampleDataSets, List<String> suffixes) {
        for (Map.Entry<Sample, List<DataSet>> entry : sampleDataSets.entrySet()) {
            for (DataSet dataSet : entry.getValue()) {
                Predicate<DataSetFile> fileFilter = it -> true;
                if (Objects.nonNull(suffixes) && !suffixes.isEmpty()) {
                    Predicate<DataSetFile> suffixFilter = file -> {
                        String fileName = getFileName(file);
                        return suffixes.stream()
                            .map(String::trim).map(String::toLowerCase)
                            .anyMatch(fileName.toLowerCase()::endsWith);
                    };
                    fileFilter = fileFilter.and(suffixFilter);
                }
                List<DataSetFile> dataSetFiles = qbicDataFinder.getFiles(dataSet.getPermId(), fileFilter);

                //skip if no files found
                if (dataSetFiles.isEmpty()) {
                    continue;
                }

                long totalSize = dataSetFiles.stream().mapToLong(DataSetFile::getFileLength).sum();

                Sample analyte = qbicDataFinder.searchAnalyteParent(entry.getKey());
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
                    String name = getFileName(file);
                    String fileSize = FileSizeFormatter.format(FileSize.of(file.getFileLength()),6);
                    int crc32 = file.getChecksumCRC32();
                    if (printWithChecksums) {
                        System.out.printf("%s\t%08x\t%s%n", fileSize, crc32, name);
                    } else {
                        System.out.printf("%s\t%s%n", fileSize, name);
                    }
                }
                System.out.print("\n");
            }
        }
    }

    private static String getFileName(DataSetFile file) {
        String filePath = file.getPermId().getFilePath();
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }


}
