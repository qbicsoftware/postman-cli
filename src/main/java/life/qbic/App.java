package life.qbic;

import java.io.IOException;
import java.util.List;

import life.qbic.io.commandline.Argparser;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import picocli.CommandLine;


/**
 * postman for staging data from openBIS
 *
 */
public class App {

  final static String AS_URL = "https://qbis.qbic.uni-tuebingen.de/openbis/openbis";
  final static String DSS_URL = "https://qbis.qbic.uni-tuebingen.de:444/datastore_server";
  private final static Logger LOG = LogManager.getLogger(App.class);


  public static void main(String[] args) throws IOException {

    if (args.length == 0){
      CommandLine.usage(new PostmanCommandLineOptions(), System.out);
      System.exit(0);
    }

    PostmanCommandLineOptions commandLineParameters = new PostmanCommandLineOptions();
    new CommandLine(commandLineParameters).parse(args);

    if (commandLineParameters.helpRequested){
      CommandLine.usage(new PostmanCommandLineOptions(), System.out);
      System.exit(0);
    }

    if ((commandLineParameters.ids == null || commandLineParameters.ids.isEmpty()) && commandLineParameters.filePath == null) {
      System.out.println("You have to provide one ID as command line argument or a file containing IDs.");
      System.exit(1);
    } else if ((commandLineParameters.ids != null) && (commandLineParameters.filePath != null)) {
      System.out.println("Arguments --identifier and --file are mutually exclusive, please provide only one.");
      System.exit(1);
    } else if (commandLineParameters.filePath != null) {
      commandLineParameters.ids = Argparser.readProvidedIndentifiers(commandLineParameters.filePath.toFile());
    }

    System.out.format("Please provide password for user \'%s\':\n", commandLineParameters.user);

    String password = Argparser.readPasswordFromInputStream();

    if (password.isEmpty()) {
      System.out.println("You need to provide a password.");
      System.exit(1);
    }

    QbicDataLoader qbicDataLoader = new QbicDataLoader(AS_URL, DSS_URL, commandLineParameters.user, password,
        commandLineParameters.bufferMultiplier*1024, commandLineParameters.datasetType);
    int returnCode = qbicDataLoader.login();
    LOG.info(String.format("OpenBis login returned with %s", returnCode));
    if (returnCode != 0) {
      LOG.error("Connection to openBIS failed.");
      System.exit(1);
    }
    LOG.info("Connection to openBIS was successful.");

    LOG.info(String.format("%s provided openBIS identifiers have been found: %s",
        commandLineParameters.ids.size(), commandLineParameters.ids.toString()));

    for (String ident : commandLineParameters.ids) {
      LOG.info(String.format("Downloading files for provided identifier %s", ident));
      List<DataSet> foundDataSets = qbicDataLoader.findAllDatasetsRecursive(ident);

      LOG.info(String.format("Number of data sets found: %s", foundDataSets.size()));

      if (foundDataSets.size() > 0) {
        LOG.info("Initialize download ...");
        int datasetDownloadReturnCode = qbicDataLoader.downloadDataset(foundDataSets);
        if (datasetDownloadReturnCode != 0) {
          LOG.error("Error while downloading dataset: " + ident);
        }

        LOG.info("Download successfully finished.");

      } else {
        LOG.info("Nothing to download.");
      }
    }
  }

}


