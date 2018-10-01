package life.qbic.dataLoading;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import life.qbic.util.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
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
                                         int bufferSize, String filterType){
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
     * Login method for openBIS authentication
     * @return 0 if successful, 1 else
     */
    public int login() {
        try{
            this.sessionToken = this.applicationServer.login(this.user, this.password);
            this.applicationServer.getSessionInformation(this.sessionToken);
        } catch (AssertionError | Exception err) {
            LOG.debug(err);
            return 1;
        }
        return 0;
    }

    /**
     * downloads files that have been found after filtering for suffixes by a list of supplied IDs
     *
     * @param foundSuffixFilteredIDs
     * @return exitcode
     * @throws IOException
     */
    public int downloadFilesByID(List<IDataSetFileId> foundSuffixFilteredIDs) throws IOException{
        for (IDataSetFileId id : foundSuffixFilteredIDs) {
            DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
            options.setRecursive(true);
            InputStream stream = this.dataStoreServer.downloadFiles(sessionToken, Collections.singletonList(id), options);
            DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
            DataSetFileDownload file;

            while ((file = reader.read()) != null) {
                InputStream initialStream = file.getInputStream();

                if (file.getDataSetFile().getFileLength() > 0) {
                    String[] splitted = file.getDataSetFile().getPath().split("/");
                    String lastOne = splitted[splitted.length - 1];
                    OutputStream os = new FileOutputStream(System.getProperty("user.dir") + File.separator + lastOne);
                    ProgressBar progressBar = new ProgressBar(lastOne, file.getDataSetFile().getFileLength());
                    int bufferSize = (file.getDataSetFile().getFileLength() < defaultBufferSize) ? (int) file.getDataSetFile().getFileLength() : defaultBufferSize;
                    byte[] buffer = new byte[bufferSize];
                    int bytesRead;
                    //read from is to buffer
                    while ((bytesRead = initialStream.read(buffer)) != -1) {
                        progressBar.updateProgress(bufferSize);
                        os.write(buffer, 0, bytesRead);
                        os.flush();

                    }
                    System.out.print("\n");
                    initialStream.close();
                    //flush OutputStream to write any buffered data to file
                    os.flush();
                    os.close();
                }

            }
        }

        return 0;
    }

    /**
     * Download a given list of data sets
     *
     * @param dataSetList A list of data sets
     * @return 0 if successful, 1 else
     */
    public int downloadDataset(List<DataSet> dataSetList) throws IOException{
        for (DataSet dataset : dataSetList) {
            DataSetPermId permID = dataset.getPermId();
            DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
            IDataSetFileId fileId = new DataSetFilePermId(new DataSetPermId(permID.toString()));
            options.setRecursive(true);
            InputStream stream = this.dataStoreServer.downloadFiles(sessionToken, Arrays.asList(fileId), options);
            DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
            DataSetFileDownload file;

            while ((file = reader.read()) != null) {
                InputStream initialStream = file.getInputStream();

                if (file.getDataSetFile().getFileLength() > 0) {
                    String[] splitted = file.getDataSetFile().getPath().split("/");
                    String lastOne = splitted[splitted.length - 1];
                    OutputStream os = new FileOutputStream(System.getProperty("user.dir") + File.separator + lastOne);
                    ProgressBar progressBar = new ProgressBar(lastOne, file.getDataSetFile().getFileLength());
                    int bufferSize = (file.getDataSetFile().getFileLength() < defaultBufferSize) ? (int) file.getDataSetFile().getFileLength() : defaultBufferSize;
                    byte[] buffer = new byte[bufferSize];
                    int bytesRead;
                    //read from is to buffer
                    while ((bytesRead = initialStream.read(buffer)) != -1) {
                        progressBar.updateProgress(bufferSize);
                        os.write(buffer, 0, bytesRead);
                        os.flush();

                    }
                    System.out.print("\n");
                    initialStream.close();
                    //flush OutputStream to write any buffered data to file
                    os.flush();
                    os.close();
                }

            }
        }
        return 0;
    }


    public IApplicationServerApi getApplicationServer() {
        return applicationServer;
    }

    public IDataStoreServerApi getDataStoreServer() {
        return dataStoreServer;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getFilterType() {
        return filterType;
    }
}
    