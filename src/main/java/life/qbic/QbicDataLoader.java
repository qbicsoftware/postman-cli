package life.qbic;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;



public class QbicDataLoader {

    private String user;

    private String password;

    private IApplicationServerApi applicationServer;

    private IDataStoreServerApi dataStoreServer;

    private final static Logger LOG = LogManager.getLogger(QbicDataLoader.class);

    private String sessionToken;

    private String filterType;

    private final int defaultBufferSize;

    private final boolean conservePaths;


    /**
     * Constructor for a QBiCDataLoaderInstance
     * @param AppServerUri The openBIS application server URL (AS)
     * @param DataServerUri The openBIS datastore server URL (DSS)
     * @param user The openBIS user
     * @param password The openBis password
     * @param bufferSize The buffer size for the InputStream reader
     */
    public QbicDataLoader(String AppServerUri, String DataServerUri,
                            String user, String password,
                            int bufferSize, String filterType,
                            boolean conservePaths){
        this.conservePaths = conservePaths;
        this.defaultBufferSize = bufferSize;
        this.filterType = filterType;

        if (!AppServerUri.isEmpty()){
            this.applicationServer = HttpInvokerUtils.createServiceStub(
                    IApplicationServerApi.class,
                    AppServerUri + IApplicationServerApi.SERVICE_URL, 10000);
        } else {
            this.applicationServer = null;
        }
        if (!DataServerUri.isEmpty()){
            this.dataStoreServer = HttpInvokerUtils.createStreamSupportingServiceStub(
                    IDataStoreServerApi.class,
                    DataServerUri + IDataStoreServerApi.SERVICE_URL, 10000);
        } else {
            this.dataStoreServer = null;
        }

        this.setCredentials(user, password);
    }


    /**
     * Setter for user and password credentials
     * @param user The openBIS user
     * @param password The openBIS user's password
     * @return QBiCDataLoader instance
     */
    public QbicDataLoader setCredentials(String user, String password) {
        this.user = user;
        this.password = password;
        return this;
    }

    /**
     * Search method for a given openBIS identifier.
     *
     * LIKELY NOT USEFUL ANYMORE - RECURSIVE METHOD SHOULD WORK JUST AS WELL -> use findAllDatasetsRecursive
     *
     * @param sampleId An openBIS sample ID
     * @return A list of all data sets attached to the sample ID
     */
    @Deprecated
    public List<DataSet> findAllDatasets(String sampleId) {
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withCode().thatEquals(sampleId);

        // tell the API to fetch all descendents for each returned sample
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        DataSetFetchOptions dsFetchOptions = new DataSetFetchOptions();
        dsFetchOptions.withType();
        fetchOptions.withChildrenUsing(fetchOptions);
        fetchOptions.withDataSetsUsing(dsFetchOptions);
        SearchResult<Sample> result = applicationServer.searchSamples(sessionToken, criteria, fetchOptions);

        // get all datasets of sample with provided sample code and all descendants
        List<DataSet> foundDatasets = new ArrayList<>();
        for (Sample sample : result.getObjects()) {
            foundDatasets.addAll(sample.getDataSets());
            for (Sample desc : sample.getChildren()) {
                foundDatasets.addAll(desc.getDataSets());
            }
        }

        if (filterType.isEmpty())
            return foundDatasets;

        List<DataSet> filteredDatasets = new ArrayList<>();
        for (DataSet ds : foundDatasets){
            LOG.info(ds.getType().getCode() + " found.");
            if (this.filterType.equals(ds.getType().getCode())){

                filteredDatasets.add(ds);
            }
        }

        return filteredDatasets;
    }

}
    