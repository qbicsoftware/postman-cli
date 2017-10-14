package life.qbic;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
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
import com.sun.org.apache.xpath.internal.SourceTree;


/**
 * qPostMan for staging data from openBIS
 *
 */
public class App {
  public static void main(String[] args) throws IOException{

      Map<Argparser.Attribute, String> cmdValues = Argparser.parseCmdArguments(args);

      String user = cmdValues.get(Argparser.Attribute.USERNAME);

      String id = cmdValues.get(Argparser.Attribute.ID);

      if (cmdValues.containsKey(Argparser.Attribute.HELP)){
          Argparser.printHelp();
          System.exit(0);
      }
      
      if (user == null){
          Argparser.printHelp();
          System.exit(1);
      }

      if (id == null || id.isEmpty()){
          System.out.println("You have to provide an ID.");
          Argparser.printHelp();
          System.exit(1);
      }


      System.out.format("Provide password for user \'%s\':\n", user);

      String password = Argparser.readPasswordFromInputStream();

      if (password.isEmpty()){
          System.out.println("You need to provide a password.");
          System.exit(1);
      }

    String AS_URL = "https://qbis.qbic.uni-tuebingen.de/openbis/openbis";
    String DSS_URL = "https://qbis.qbic.uni-tuebingen.de:444/datastore_server";

    // Reference the DSS
    IDataStoreServerApi dss = HttpInvokerUtils.createStreamSupportingServiceStub(
        IDataStoreServerApi.class, DSS_URL + IDataStoreServerApi.SERVICE_URL, 10000);

    // Reference the AS and login & get a session token
    IApplicationServerApi as = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
        AS_URL + IApplicationServerApi.SERVICE_URL, 10000);

    String sessionToken = as.login(user, password);

    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withCode().thatEquals(id);

    // tell the API to fetch all descendents for each returned sample
    SampleFetchOptions fetchOptions = new SampleFetchOptions();
    DataSetFetchOptions dsFetchOptions = new DataSetFetchOptions();
    fetchOptions.withChildrenUsing(fetchOptions);
    fetchOptions.withDataSetsUsing(dsFetchOptions);
    SearchResult<Sample> result = as.searchSamples(sessionToken, criteria, fetchOptions);
    System.out.println(result.getTotalCount());

    // get all datasets of sample with provided sample code and all descendents
    List<DataSet> foundDatasets = new ArrayList<DataSet>();
    for (Sample sample : result.getObjects()) {
      foundDatasets.addAll(sample.getDataSets());
      System.out.println(sample.getDataSets());
      for (Sample desc : sample.getChildren()) {
        System.out.println(desc.getDataSets());
        foundDatasets.addAll(desc.getDataSets());
      }
    }

    // Download the files of found datasets
    System.out.println(foundDatasets.size());
    for (DataSet dataset : foundDatasets) {
      DataSetPermId permID = dataset.getPermId();
      System.out.println(permID.toString());

      DataSetFileDownloadOptions options = new DataSetFileDownloadOptions();
      IDataSetFileId fileId = new DataSetFilePermId(new DataSetPermId(permID.toString()));
      options.setRecursive(true);
      InputStream stream = dss.downloadFiles(sessionToken, Arrays.asList(fileId), options);
      DataSetFileDownloadReader reader = new DataSetFileDownloadReader(stream);
      DataSetFileDownload file = null;

      while ((file = reader.read()) != null) {
        InputStream initialStream = file.getInputStream();

        if(file.getDataSetFile().getFileLength() > 0) {
          String[] splitted = file.getDataSetFile().getPath().split("/");
          String lastOne = splitted[splitted.length - 1];
          OutputStream os = new FileOutputStream("/home/sven/Downloads/" + lastOne);

          byte[] buffer = new byte[1024];
          int bytesRead;
          //read from is to buffer
          while ((bytesRead = initialStream.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
          }
          initialStream.close();
          //flush OutputStream to write any buffered data to file
          os.flush();
          os.close();
        }

        //System.out.println("Downloaded " + file.getDataSetFile().getPath() + " "
          //  + file.getDataSetFile().getFileLength());
          //while ((outfile = initialStream.read()) != null){



/*
          String[] splitted = file.getDataSetFile().getPath().split("/");
          String lastOne = splitted[splitted.length-1];
          File targetFile = new File("/home/sven1103/Downloads/" +  lastOne);
          OutputStream outStream = new FileOutputStream(targetFile);
          outStream.write(buffer);
          */
        }
      }
      }
    }


