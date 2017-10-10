package life.qbic;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;

/**
 * qPostMan for staging data from openBIS
 *
 */
public class App {
  public static void main(String[] args) {
    String AS_URL = "https://qbis.qbic.uni-tuebingen.de:443/openbis/openbis";
    String DSS_URL = "https://qbis.qbic.uni-tuebingen.de:444/datastore_server";

    // Reference the DSS
    IDataStoreServerApi dss = HttpInvokerUtils.createStreamSupportingServiceStub(
        IDataStoreServerApi.class, DSS_URL + IDataStoreServerApi.SERVICE_URL, 10000);

    // Reference the AS and login & get a session token
    IApplicationServerApi as = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
        AS_URL + IApplicationServerApi.SERVICE_URL, 10000);

    String sessionToken = as.login(args[0], args[1]);

    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withCode().thatEquals(args[2]);

    // tell the API to fetch all descendents for each returned sample
    SampleFetchOptions fetchOptions = new SampleFetchOptions();
    fetchOptions.withChildrenUsing(fetchOptions);
    SearchResult<Sample> result = as.searchSamples(sessionToken, criteria, fetchOptions);

    // get all datasets of sample with provided sample code and all descendents
    List<DataSet> foundDatasets = new ArrayList<DataSet>();
    for (Sample sample : result.getObjects()) {
      foundDatasets.addAll(sample.getDataSets());
      for (Sample desc : sample.getChildren()) {
        foundDatasets.addAll(desc.getDataSets());
      }
    }

    // Download the files of found datasets
    for (DataSet dataset : foundDatasets) {
      DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
      IDataSetFileId fileId = new DataSetFilePermId(new DataSetPermId("20161205154857065-25"));
      options.setRecursive(true);
      InputStream stream = dss.downloadFiles(sessionToken, Arrays.asList(fileId), options);
      DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
      DataSetFileDownload file = null;

      while ((file = reader.read()) != null) {
        file.getInputStream();
        System.out.println("Downloaded " + file.getDataSetFile().getPath() + " "
            + file.getDataSetFile().getFileLength());
      }
    }

  }

}
