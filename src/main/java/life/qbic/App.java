package life.qbic;

import java.io.InputStream;
import java.util.Arrays;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;

/**
 * Hello world!
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

    String sessionToken = as.login("admin", "password");

    // Download the files and print the contents
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
