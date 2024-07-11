package life.qbic.qpostman.list;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import life.qbic.qpostman.common.functions.FileFilter;
import life.qbic.qpostman.common.functions.FindSourceSample;
import life.qbic.qpostman.common.functions.SearchDataSets;
import life.qbic.qpostman.common.functions.SearchFiles;
import life.qbic.qpostman.common.functions.SortFiles;
import life.qbic.qpostman.common.options.AuthenticationOptions;
import life.qbic.qpostman.common.options.FilterOptions;
import life.qbic.qpostman.common.options.SampleIdentifierOptions;
import life.qbic.qpostman.common.options.ServerOptions;
import life.qbic.qpostman.common.structures.DataFile;
import life.qbic.qpostman.common.structures.DataSetWrapper;
import life.qbic.qpostman.list.LegacyOutputFormatter.DataSetSummary;
import life.qbic.qpostman.openbis.OpenBisSessionProvider;
import life.qbic.qpostman.openbis.ServerFactory;

@Command(name = "list",
        description = "lists all the datasets found for the given identifiers")
public class ListCommand implements Runnable {

  @Mixin
    AuthenticationOptions authenticationOptions;
    @Mixin
    SampleIdentifierOptions sampleIdentifierOptions;
    @Mixin
    FilterOptions filterOptions;
    @Mixin
    ServerOptions serverOptions;
    @Mixin
    ListOptions listOptions;

    @Override
    public void run() {
            Functions functions = setupFunctions();

            Collection<DataFile> dataSetFiles = functions.searchDataSets()
                .andThen(functions.searchFiles())
                .apply(sampleIdentifierOptions.getIds());

            List<DataFile> processedFiles = dataSetFiles.stream()
                .filter(functions.fileFilter())
                .sorted(functions.sortFiles().comparator())
                .toList();

            Consumer<List<DataFile>> output = switch (listOptions.outputFormat) {
                case LEGACY -> this::listAsLegacy;
                case TSV -> this::listAsTsv;
            };
            output.accept(processedFiles);
    }

    private void listAsTsv(List<DataFile> processedFiles) {
        boolean withHeader = !listOptions.withoutHeader;
        DataFileTableFormatter dataFileTableFormatter = new DataFileTableFormatter(listOptions.exactFilesize, listOptions.withChecksum);
        String tsvContent = dataFileTableFormatter
            .formatAsTable(processedFiles, "\t", withHeader);
        System.out.println(tsvContent);
    }

    private void listAsLegacy(List<DataFile> processedFiles) {
        LegacyOutputFormatter legacyOutputFormatter = new LegacyOutputFormatter();
        Map<DataSetWrapper, List<DataFile>> groupedFiles = processedFiles.stream()
            .collect(Collectors.groupingBy(DataFile::dataSet));
        for (Entry<DataSetWrapper, List<DataFile>> dataSetWrapperListEntry : groupedFiles.entrySet()) {
            String output = legacyOutputFormatter.format(
                new DataSetSummary(dataSetWrapperListEntry.getValue()),
                listOptions.exactFilesize, listOptions.withChecksum);
            System.out.println(output);
        }
    }

    private Functions setupFunctions() {
        IApplicationServerApi applicationServerApi = ServerFactory.applicationServer(
            serverOptions.as_url, serverOptions.timeoutInMillis);
        OpenBisSessionProvider.init(applicationServerApi, authenticationOptions.user,
            new String(authenticationOptions.getPassword()));
        Collection<IDataStoreServerApi> dataStoreServerApis = ServerFactory.dataStoreServers(
            serverOptions.dss_urls, serverOptions.timeoutInMillis);
        SearchDataSets searchDataSets = new SearchDataSets(applicationServerApi);
        FileFilter myAwesomeFileFilter = FileFilter.create()
            .withSuffixes(filterOptions.suffixes);
        SearchFiles searchFiles = new SearchFiles(dataStoreServerApis, number -> {});
        FindSourceSample findSourceSample = new FindSourceSample(serverOptions.sourceSampleType);

        SortFiles sortFiles = new SortFiles();

        DataSetWrapper.setFindSourceFunction(findSourceSample);
      return new Functions(searchDataSets, myAwesomeFileFilter, searchFiles, sortFiles);
    }

    private record Functions(SearchDataSets searchDataSets, FileFilter fileFilter, SearchFiles searchFiles, SortFiles sortFiles) {

    }
}
