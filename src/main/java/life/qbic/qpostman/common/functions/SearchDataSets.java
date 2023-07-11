package life.qbic.qpostman.common.functions;

import static java.util.Objects.requireNonNull;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import life.qbic.qpostman.common.structures.DataSetWrapper;
import life.qbic.qpostman.openbis.OpenBisSessionProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchDataSets implements Function<Collection<String>, Collection<DataSetWrapper>> {
    private static final Logger log = LogManager.getLogger(SearchDataSets.class);
    private final IApplicationServerApi applicationServerApi;

    public SearchDataSets(IApplicationServerApi applicationServerApi) {
        this.applicationServerApi = applicationServerApi;
    }

    private Collection<DataSetWrapper> searchDataSets(Collection<String> userInput) {
        List<Sample> samples = userInput.stream()
                .map(SampleQuery::new)
                .flatMap(this::searchSamples)
                .toList();
        Set<String> processedSampleCodes = new HashSet<>();
        Set<DataSetWrapper> foundDataSets = new HashSet<>();
        for (Sample it : samples) {
            addAllDataSets(it, processedSampleCodes, foundDataSets);
        }
        return foundDataSets;
    }

    private Stream<Sample> searchSamples(SampleQuery sampleQuery) {
        return applicationServerApi.searchSamples(OpenBisSessionProvider.get().getToken(), sampleQuery.searchCriteria(), sampleQuery.fetchOptions()).getObjects().stream();
    }

    @Override
    public Collection<DataSetWrapper> apply(Collection<String> strings) {
        return searchDataSets(strings);
    }

    private record SampleQuery(String sampleCode) {
        SampleQuery {
            requireNonNull(sampleCode, "sampleCode must not be null");
        }
        public SampleSearchCriteria searchCriteria() {
            SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
            sampleSearchCriteria.withCode().thatEquals(sampleCode);
            return sampleSearchCriteria;
        }

        public SampleFetchOptions fetchOptions() {
            SampleFetchOptions parentFetchOptions = new SampleFetchOptions();
            parentFetchOptions.withType();
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withDataSets().withSample();
            sampleFetchOptions.withType();
            sampleFetchOptions.withParents();
            sampleFetchOptions.withParentsUsing(parentFetchOptions);
            sampleFetchOptions.withChildrenUsing(sampleFetchOptions);
            return sampleFetchOptions;
        }
    }

    private void addAllDataSets(Sample sample, Set<String> processedSampleCodes, Set<DataSetWrapper> dataSetAccumulator) {
        if (processedSampleCodes.contains(sample.getCode())) {
            log.trace("already visited " + sample.getCode());
            return;
        } else {
            log.trace("visiting " + sample.getCode());
        }
        List<DataSetWrapper> dataSetList = sample.getDataSets().stream()
                .map(DataSetWrapper::new)
                .toList();
        dataSetAccumulator.addAll(dataSetList);
        processedSampleCodes.add(sample.getCode());
        if (!dataSetList.isEmpty()) {
            log.trace("added " + dataSetList.size() + " datasets for " + sample.getCode());
        }
        sample.getChildren().forEach(it -> addAllDataSets(it, processedSampleCodes, dataSetAccumulator));

    }


}
