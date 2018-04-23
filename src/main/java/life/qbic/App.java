package life.qbic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import picocli.CommandLine;



/**
 * qPostMan for staging data from openBIS
 *
 */
public class App {

  static String AS_URL = "https://qbis.qbic.uni-tuebingen.de/openbis/openbis";
  static String DSS_URL = "https://qbis.qbic.uni-tuebingen.de:444/datastore_server";
  static Logger log = LogManager.getLogger(App.class);


  public static void main(String[] args) throws IOException {

    MyCommandLine commandLine = new MyCommandLine();
    new CommandLine(commandLine).parse(args);

    if (commandLine.user == null) {
      Argparser.printHelp();
      System.exit(1);
    }

    if ((commandLine.ids == null || commandLine.ids.isEmpty()) && (commandLine.filePath == null || commandLine.filePath == null)) {
      System.out
          .println("You have to provide one ID as command line argument or a file containing IDs.");
      System.exit(1);
    } else if ((commandLine.ids != null) && (commandLine.filePath != null)) {
      System.out.println(
          "Arguments --identifier and --file are mutually exclusive, please provide only one.");
      System.exit(1);
    } else if (commandLine.filePath != null) {
      commandLine.ids = Argparser.readProvidedIndentifiers(commandLine.filePath.toFile());
    }

    System.out.format("Provide password for user \'%s\':\n", commandLine.user);

    String password = Argparser.readPasswordFromInputStream();

    if (password.isEmpty()) {
      System.out.println("You need to provide a password.");
      System.exit(1);
    }

    QbicDataLoader qbicDataLoader = new QbicDataLoader(AS_URL, DSS_URL, commandLine.user, password, commandLine.bufferMultiplier*1024);
    int returnCode = qbicDataLoader.login();
    log.info(String.format("OpenBis login returned with %s", returnCode));
    if (returnCode != 0) {
      log.error("Connection to openBIS failed.");
      System.exit(1);
    }
    log.info("Connection to openBIS was successful.");

    log.info(String.format("%s provided openBIS identifiers have been found: %s",
        commandLine.ids.size(), commandLine.ids.toString()));

    for (String ident : commandLine.ids) {
      log.info(String.format("Downloading files for provided identifier %s", ident));
      List<DataSet> foundDataSets = qbicDataLoader.findAllDatasets(ident);

      log.info(String.format("Number of data sets found: %s", foundDataSets.size()));

      if (foundDataSets.size() > 0) {
        log.info("Initialize download ...");
        qbicDataLoader.downloadDataset(foundDataSets);
        log.info("Download finished.");

      } else {
        log.info("Nothing to download.");
      }
    }
  }

}


