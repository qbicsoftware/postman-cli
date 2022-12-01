package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QbicDataFinder {

  private static final Logger log = LogManager.getLogger(QbicDataFinder.class);

  private final IApplicationServerApi applicationServer;

  private final List<IDataStoreServerApi> dataStoreServers;

  private final String sessionToken;

  public QbicDataFinder(
      IApplicationServerApi applicationServer,
      List<IDataStoreServerApi> dataStoreServers,
      String sessionToken) {
    this.applicationServer = applicationServer;
    this.dataStoreServers = dataStoreServers;
    this.sessionToken = sessionToken;
  }

  /**
   * Fetches all datasets, even those of children - recursively
   *
   * @param sample         the sample for which descending data sets should be added
   * @param visitedSamples map with samples and datasets already visited.
   */
  private static void fillWithDescendantDatasets(Sample sample,
      Map<Sample, List<DataSet>> visitedSamples) {
    if (visitedSamples.containsKey(sample)) {
      return;
    }

    List<Sample> children = sample.getChildren();
    List<DataSet> foundDataSets = sample.getDataSets();
    visitedSamples.put(sample, foundDataSets);
    // recursion end
    if (children.size() > 0) {
      for (Sample child : children) {
        fillWithDescendantDatasets(child, visitedSamples);
      }
    }
  }

  public List<DataSetFile> getFiles(DataSetPermId permID, Predicate<DataSetFile> fileFilter) {
      DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
      criteria.withDataSet().withCode().thatEquals(permID.getPermId());
    List<DataSetFile> files = new ArrayList<>();
    // add files from all data store servers
    for (IDataStoreServerApi dataStoreServer : dataStoreServers) {
      List<DataSetFile> filesOnDataStoreServer = dataStoreServer
          .searchFiles(sessionToken, criteria, new DataSetFileFetchOptions())
          .getObjects();
      if (filesOnDataStoreServer.isEmpty()) {
        log.debug(
            String.format("No files found in dataset %s on dss %s", permID, dataStoreServer));
      } else {
        log.debug(String.format("%s files and directories found in dataset %s on dss %s",
            filesOnDataStoreServer.size(), permID, dataStoreServer));
      }
      files.addAll(filesOnDataStoreServer);
    }

    Predicate<DataSetFile> notADirectory = dataSetFile -> !dataSetFile.isDirectory();
    return files.stream().filter(notADirectory.and(fileFilter)).collect(Collectors.toList());
  }

  /**
   * Finds all datasets of a given sampleID, even those of its children - recursively
   *
   * @param sampleId provided by user
   * @return all found datasets for a given sampleID
   */
  public Map<Sample, List<DataSet>> findAllDatasetsRecursive(String sampleId) {
    Map<Sample, List<DataSet>> dataSetsBySample = new HashMap<>();

    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withCode().thatEquals(sampleId);

    // tell the API to fetch all descendants for each returned sample
    SampleFetchOptions fetchOptions = new SampleFetchOptions();
    DataSetFetchOptions dsFetchOptions = new DataSetFetchOptions();
    dsFetchOptions.withType();
    dsFetchOptions.withSample();
    fetchOptions.withType();
    fetchOptions.withChildrenUsing(fetchOptions);
    fetchOptions.withParentsUsing(fetchOptions);
    fetchOptions.withDataSetsUsing(dsFetchOptions);

    SearchResult<Sample> result =
        applicationServer.searchSamples(sessionToken, criteria, fetchOptions);
    List<Sample> samples = result.getObjects();

    for (Sample sample : samples) {
      fillWithDescendantDatasets(sample, dataSetsBySample);
    }
    return dataSetsBySample;
  }

  /**
   * Searches the parents for a Q_TEST_SAMPLE assuming at most one Q_TEST_SAMPLE exists in the parent
   * samples. If no Q_TEST_SAMPLE was found, the original sample is returned.
   *
   * @param sample the sample to which a dataset is attached to
   * @return the Q_TEST_SAMPLE parent if exists, the sample itself otherwise.
   */
  public Sample searchAnalyteParent(Sample sample) {
      Optional<Sample> firstTestSample = sample.getParents().stream()
          .filter(
              it -> it.getType().getCode().equals("Q_TEST_SAMPLE"))
          .findFirst();
      return firstTestSample.orElse(sample);
  }
}
