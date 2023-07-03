package life.qbic.qpostman.common.functions;

import static java.util.Objects.requireNonNull;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import life.qbic.qpostman.common.structures.DataFile;
import life.qbic.qpostman.common.structures.DataSetWrapper;
import life.qbic.qpostman.openbis.OpenBisSessionProvider;

/**
 * Searches for data files based on a collection of DataSetWrapper objects.
 * It utilizes a collection of IDataStoreServerApi instances to perform the search querying every datastore and aggregating the files.
 */
public class SearchFiles implements Function<Collection<DataSetWrapper>, Collection<DataFile>> {

    private final Collection<IDataStoreServerApi> dataStoreServerApis;

    public SearchFiles(IApplicationServerApi applicationServerApi, Collection<IDataStoreServerApi> dataStoreServerApis) {
        this.dataStoreServerApis = dataStoreServerApis;
    }

    @Override
    public Collection<DataFile> apply(Collection<DataSetWrapper> dataSetWrappers) {
        return searchFiles(dataSetWrappers);
    }

    public interface DataSetCounterUpdateListener {

        void dataSetQueried();
    }

    private static class DataSetCounterProgressDisplay implements DataSetCounterUpdateListener {

        final int maxCount;
        int currCount;

        private DataSetCounterProgressDisplay(int maxCount) {
            this.maxCount = maxCount;
            currCount = 0;
        }

        @Override
        public void dataSetQueried() {
            currCount++;
            System.out.printf("Indexing dataset %4s / %s\r", currCount, maxCount);
        }
    }



    private List<DataFile> searchFiles(Collection<DataSetWrapper> dataSets) {
        DataSetCounterUpdateListener updateListener = new DataSetCounterProgressDisplay(dataSets.size());
        Stream<DataSetFileQuery> dataSetFileQueries = dataSets.stream()
            .map(DataSetWrapper::dataSetPermId)
            .map(DataSetFileQuery::new);
        Stream<DataSetFile> dataSetFiles = dataSetFileQueries
            .peek(it -> updateListener.dataSetQueried())
            .map(this::queryDataStoresForFiles)
            .flatMap(it -> it);
        Stream<DataFile> dataFiles = dataSetFiles
            .map(dataSetFile -> {
                DataSetPermId dataSetPermId = dataSetFile.getDataSetPermId();
                DataSetWrapper dataSet = dataSets.stream()
                    .filter(ds -> ds.dataSetPermId().equals(dataSetPermId)).findFirst()
                    .orElseThrow();
                return new DataFile(dataSetFile, dataSet);
            });
        return dataFiles.toList();
    }

    private Stream<DataSetFile> queryDataStoresForFiles(DataSetFileQuery dataSetFileQuery) {
        return dataStoreServerApis.stream()
                .flatMap(dataStoreServerApi ->
                {
                    List<DataSetFile> files = dataStoreServerApi.searchFiles(OpenBisSessionProvider.get().getToken(),
                                    dataSetFileQuery.searchCriteria(),
                                    dataSetFileQuery.fetchOptions())
                            .getObjects();
                    return files.stream();
                })
                .filter(file -> !file.isDirectory()); // filter out all folders but keeps the files
    }

    private record DataSetFileQuery(DataSetPermId dataSetPermId) {
        private DataSetFileQuery {
            requireNonNull(dataSetPermId, "dataSetPermId must not be null");
        }

        public DataSetFileSearchCriteria searchCriteria() {
            DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
            criteria.withDataSet().withPermId().thatEquals(dataSetPermId.getPermId());
            return criteria;
        }

        public DataSetFileFetchOptions fetchOptions() {
            return new DataSetFileFetchOptions();
        }
    }

}
