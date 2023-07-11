package life.qbic.qpostman.common.functions;

import static java.util.Objects.requireNonNull;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Searches for data files based on a collection of DataSetWrapper objects.
 * It utilizes a collection of IDataStoreServerApi instances to perform the search querying every datastore and aggregating the files.
 */
public class SearchFiles implements Function<Collection<DataSetWrapper>, Collection<DataFile>> {

    private static final Logger log = LogManager.getLogger(SearchFiles.class);
    private final Collection<IDataStoreServerApi> dataStoreServerApis;
    private DataSetCounterUpdateListener dataSetCounterUpdateListener;

    public SearchFiles(Collection<IDataStoreServerApi> dataStoreServerApis,
        DataSetCounterUpdateListener dataSetCounterUpdateListener) {
        this.dataStoreServerApis = dataStoreServerApis;
        this.dataSetCounterUpdateListener = dataSetCounterUpdateListener;

    }

    @Override
    public Collection<DataFile> apply(Collection<DataSetWrapper> dataSetWrappers) {
        return searchFiles(dataSetWrappers, dataSetCounterUpdateListener);
    }

    @FunctionalInterface
    public interface DataSetCounterUpdateListener {

        void updateCounter(int numberOfDatasets);
    }



    public static class DataSetCounterProgressDisplay implements DataSetCounterUpdateListener {

        final int maxCount;
        int currCount;

        public DataSetCounterProgressDisplay(int maxCount) {
            this.maxCount = maxCount;
            currCount = 0;
        }

        @Override
        public void updateCounter(int numberOfDatasets) {
            currCount += numberOfDatasets;
            System.out.printf("Indexing dataset %4s / %s\r", currCount, maxCount);
        }
    }



    private List<DataFile> searchFiles(Collection<DataSetWrapper> dataSets,
        DataSetCounterUpdateListener updateListener) {
        Stream<DataSetFileQuery> dataSetFileQueries = dataSets.stream()
            .map(DataSetWrapper::dataSetPermId)
            .map(DataSetFileQuery::new);
        Stream<DataSetFile> dataSetFiles = dataSetFileQueries
            .peek(it -> updateListener.updateCounter(1))
            .flatMap(this::queryDataStoresForFiles);
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
                    log.trace("Found " + files.size() + " files for "
                        + dataSetFileQuery.dataSetPermId.getPermId() + " on "
                        + dataStoreServerApi);
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
