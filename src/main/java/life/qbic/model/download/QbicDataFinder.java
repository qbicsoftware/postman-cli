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
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import life.qbic.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QbicDataFinder {

  private static final Logger LOG = LogManager.getLogger(QbicDataFinder.class);

  private final IApplicationServerApi applicationServer;

  private final IDataStoreServerApi dataStoreServer;

  private final String sessionToken;

  public QbicDataFinder(
      IApplicationServerApi applicationServer,
      IDataStoreServerApi dataStoreServer,
      String sessionToken) {
    this.applicationServer = applicationServer;
    this.dataStoreServer = dataStoreServer;
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

  public static List<DataSetFile> withoutDirectories(List<DataSetFile> dataSetFiles) {
    Predicate<DataSetFile> notADirectory = dataSetFile -> !dataSetFile.isDirectory();
    return dataSetFiles.stream()
        .filter(notADirectory)
        .collect(Collectors.toList());
  }

  public List<DataSetFile> getFiles(DataSetPermId permID) {
      DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
      criteria.withDataSet().withCode().thatEquals(permID.getPermId());
      SearchResult<DataSetFile> result =
          dataStoreServer.searchFiles(sessionToken, criteria,
                      new DataSetFileFetchOptions());
      return withoutDirectories(result.getObjects());
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
   * Finds all IDs of files filtered by a suffix
   *
   * @param ident sample ID
   * @param suffixes the suffixes to filter for
   * @return a filtered list of sample, dataset file maps
   */
  public List<Map<Sample, List<DataSetFile>>> findAllSuffixFilteredIDs(String ident,
      List<String> suffixes) {
    Map<Sample, List<DataSet>> allDatasets = findAllDatasetsRecursive(ident);
    List<Map<Sample, List<DataSetFile>>> filteredDatasets = new ArrayList<>();

    for (Entry<Sample, List<DataSet>> entry : allDatasets.entrySet()) {
      List<DataSet> sampleDataSets = entry.getValue();
      List<DataSetFile> filteredFiles =
          filterDataSetBySuffix(sampleDataSets, suffixes);
      if (filteredFiles.isEmpty()) {
        continue;
      }
      Sample sample = entry.getKey();
      Map<Sample, List<DataSetFile>> result = new HashMap<>();
      result.put(sample, filteredFiles);
      filteredDatasets.add(result);
    }

    return filteredDatasets;
  }

  private List<DataSetFile> filterDataSetBySuffix(List<DataSet> datasets, List<String> suffixes) {
    List<DataSetFile> filteredFiles = new ArrayList<>();
    for (DataSet ds : datasets) {
      // we cannot access the files directly of the datasets -> we need to query for the files first
      // using the datasetID
      DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
      criteria.withDataSet().withCode().thatEquals(ds.getCode());
      criteria.withDataSet().withSample();
      SearchResult<DataSetFile> result =
          dataStoreServer.searchFiles(sessionToken, criteria, new DataSetFileFetchOptions());
      List<DataSetFile> files = result.getObjects();

      filteredFiles.addAll(filterDataSetFilesBySuffix(files, suffixes));
    }
    return filteredFiles;
  }

  private List<DataSetFile> filterDataSetFilesBySuffix(List<DataSetFile> files,
      List<String> suffixes) {
    List<DataSetFile> filesFiltered = new ArrayList<>();
    // remove everything that doesn't match the suffix -> only add if suffix matches
    for (DataSetFile file : files) {
      for (String suffix : suffixes) {
        // We omit directories and check files for suffix pattern
        if ((!file.isDirectory()) && StringUtil.endsWithIgnoreCase(file.getPath(), suffix)) {
          filesFiltered.add(file);
        }
      }
    }
    return filesFiltered;
  }

  /**
   * Search method for a given openBIS identifier.
   *
   * <p>LIKELY NOT USEFUL ANYMORE - RECURSIVE METHOD SHOULD WORK JUST AS WELL -> use
   * findAllDatasetsRecursive
   *
   * @param sampleId An openBIS sample ID
   * @return A list of all data sets attached to the sample ID
   */
  @Deprecated
  public List<DataSet> findAllDatasets(
      String sampleId,
      IApplicationServerApi applicationServer,
      String sessionToken,
      String filterType) {
    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withCode().thatEquals(sampleId);

    // tell the API to fetch all descendants for each returned sample
    SampleFetchOptions fetchOptions = new SampleFetchOptions();
    DataSetFetchOptions dsFetchOptions = new DataSetFetchOptions();
    dsFetchOptions.withType();
    fetchOptions.withChildrenUsing(fetchOptions);
    fetchOptions.withDataSetsUsing(dsFetchOptions);
    SearchResult<Sample> result =
        applicationServer.searchSamples(sessionToken, criteria, fetchOptions);

    // get all datasets of sample with provided sample code and all descendants
    List<DataSet> foundDatasets = new ArrayList<>();
    for (Sample sample : result.getObjects()) {
      foundDatasets.addAll(sample.getDataSets());
      for (Sample desc : sample.getChildren()) {
        foundDatasets.addAll(desc.getDataSets());
      }
    }

    if (filterType.isEmpty()) {
      return foundDatasets;
    }

    List<DataSet> filteredDatasets = new ArrayList<>();
    for (DataSet ds : foundDatasets) {
      LOG.info(ds.getType().getCode() + " found.");
      if (filterType.equals(ds.getType().getCode())) {

        filteredDatasets.add(ds);
      }
    }

    return filteredDatasets;
  }

}
