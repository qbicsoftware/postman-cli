package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
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
import java.util.stream.Stream;
import life.qbic.QbicDataLoaderRegexUtil;
import life.qbic.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QbicDataFinder {

  private static final Logger LOG = LogManager.getLogger(QbicDataFinder.class);

  private IApplicationServerApi applicationServer;

  private IDataStoreServerApi dataStoreServer;

  private String sessionToken;

  private String filterType;

  public QbicDataFinder(
      IApplicationServerApi applicationServer,
      IDataStoreServerApi dataStoreServer,
      String sessionToken,
      String filterType) {
    this.applicationServer = applicationServer;
    this.dataStoreServer = dataStoreServer;
    this.sessionToken = sessionToken;
    this.filterType = filterType;
  }

  /**
   * Fetches all datasets, even those of children - recursively
   *
   * @param sample
   * @return all recursively found datasets
   */
  private static Map<String, List<DataSet>> fetchDescendantDatasets(Sample sample, List<String> visitedSamples) {
    List<Sample> children = sample.getChildren();
    //avoid duplicates
    if(visitedSamples.contains(sample.getCode())){
      return new HashMap<>();
    }
    // recursion end
    if (children.size() < 1) {
      HashMap<String, List<DataSet>> sampleDatasets = new HashMap<>();
      sampleDatasets.put(sample.getCode(), sample.getDataSets());
      return sampleDatasets;
    }
    Map<String, List<DataSet>> sampleDatasets = new HashMap<>();
    sampleDatasets.put(sample.getCode(), sample.getDataSets());
    for (Sample child : children) {
      Map<String, List<DataSet>> childDatasetMapping = fetchDescendantDatasets(child,visitedSamples);
      sampleDatasets = joinMaps(sampleDatasets, childDatasetMapping);
    }
    return sampleDatasets;
  }

  /**
   * Finds all datasets of a given sampleID, even those of its children - recursively
   *
   * @param sampleId
   * @return all found datasets for a given sampleID
   */
  public Map<String, List<DataSet>> findAllDatasetsRecursive(String sampleId) {
    List<String> searchedSamples = new ArrayList<>();
    Map<String, List<DataSet>> dataSetsBySampleId = new HashMap<>();

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
    List<Sample> samples = result.getObjects();

    for (Sample sample : samples) {
      if(searchedSamples.contains(sample.getCode())){
        continue;
      }
      Map<String, List<DataSet>> sampleDatasetMap = fetchDescendantDatasets(sample,searchedSamples);
      dataSetsBySampleId = joinMaps(dataSetsBySampleId, sampleDatasetMap);
      LOG.info("visited: "+searchedSamples.toString());
      LOG.info("Found samples: "+sampleDatasetMap.keySet());
      //remember what has been searched
      searchedSamples.addAll(sampleDatasetMap.keySet());
    }
    return dataSetsBySampleId;
  }

  // FIXME this is needed to preserve functionality. For some reason collisions appear and we have to merge the lists
  //  the collisions observed might be caused by the bug where every dataset is contained 3 times.
  private static <T, V> Map<T, List<V>> joinMaps(Map<T, List<V>> map1, Map<T, List<V>> map2) {
    Map<T,List<V>> joinedMap = new HashMap<>();
    joinedMap.putAll(map1);
    map2.forEach((key, value) -> joinedMap.merge(key, value, QbicDataFinder::joinLists));
    return joinedMap;
  }

  private static <T> List<T> joinLists(List<T> list1, List<T> list2) {
    List<T> joinedList = new ArrayList<>();
    Stream.of(list1, list2).forEach(joinedList::addAll);
    return joinedList;
  }

  /**
   * Calls groovy code Filters all IDs by provided regex patterns
   *
   * @param ident
   * @param regexPatterns
   * @return
   */
  public List<DataSetFile> findAllRegexFilteredIDs(String ident, List<String> regexPatterns) {
    // TODO adjust for datasets per sample
    //List<DataSet> allDatasets = findAllDatasetsRecursive(ident);

    // TODO replace empty list
    return QbicDataLoaderRegexUtil.findAllRegexFilteredIDsGroovy(
        regexPatterns, new ArrayList<>(), dataStoreServer, sessionToken);
  }

  /**
   * Finds all IDs of files filtered by a suffix
   *
   * @param ident
   * @param suffixes
   * @return
   */
  public List<Map<String, List<DataSetFile>>> findAllSuffixFilteredIDs(String ident,
      List<String> suffixes) {
    // TODO adjust type
    Map<String, List<DataSet>> allDatasets = findAllDatasetsRecursive(ident);
    List<Map<String, List<DataSetFile>>> filteredDatasets = new ArrayList<>();

    for (Entry<String, List<DataSet>> entry : allDatasets.entrySet()) {
      String sampleCode = entry.getKey();
      List<DataSet> sampleDataSets = entry.getValue();
      List<DataSetFile> filteredFiles =
          filterDataSetBySuffix(sampleDataSets, suffixes);

      Map<String, List<DataSetFile>> result = new HashMap<>();
      result.put(sampleCode, filteredFiles);
      filteredDatasets.add(result);
    }

    return filteredDatasets;
  }

  private List<DataSetFile> filterDataSetBySuffix(List<DataSet> datasets, List<String> suffixes){
    List<DataSetFile> filteredFiles = new ArrayList<>();
    for (DataSet ds : datasets) {
      // we cannot access the files directly of the datasets -> we need to query for the files first
      // using the datasetID
      DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();
      criteria.withDataSet().withCode().thatEquals(ds.getCode());
      SearchResult<DataSetFile> result =
          dataStoreServer.searchFiles(sessionToken, criteria, new DataSetFileFetchOptions());
      List<DataSetFile> files = result.getObjects();

      filteredFiles.addAll(filterDataSetFilesBySuffix(files, suffixes));
    }
    return filteredFiles;
  }

  private List<DataSetFile> filterDataSetFilesBySuffix(List<DataSetFile> files, List<String> suffixes){
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

    // tell the API to fetch all descendents for each returned sample
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

    if (filterType.isEmpty()) return foundDatasets;

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
