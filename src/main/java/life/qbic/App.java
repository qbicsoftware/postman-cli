package life.qbic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;



/**
 * qPostMan for staging data from openBIS
 *
 */
public class App {

  static String AS_URL = "https://qbis.qbic.uni-tuebingen.de/openbis/openbis";
  static String DSS_URL = "https://qbis.qbic.uni-tuebingen.de:444/datastore_server";
  static Logger log = LogManager.getLogger(App.class);

  public static void main(String[] args) throws IOException {

    Map<Argparser.Attribute, String> cmdValues = Argparser.parseCmdArguments(args);

    String user = cmdValues.get(Argparser.Attribute.USERNAME);

    String idPath = cmdValues.get(Argparser.Attribute.ID);

    if (cmdValues.containsKey(Argparser.Attribute.HELP)) {
      Argparser.printHelp();
      System.exit(0);
    }

    if (user == null) {
      Argparser.printHelp();
      System.exit(1);
    }

    if (idPath == null || idPath.isEmpty()) {
      System.out.println("You have to provide a file containing IDs.");
      Argparser.printHelp();
      System.exit(1);
    }

    System.out.format("Provide password for user \'%s\':\n", user);

    String password = Argparser.readPasswordFromInputStream();

    if (password.isEmpty()) {
      System.out.println("You need to provide a password.");
      System.exit(1);
    }

    QbicDataLoader qbicDataLoader = new QbicDataLoader(AS_URL, DSS_URL, user, password);
    int returnCode = qbicDataLoader.login();
    log.info(String.format("OpenBis login returned with %s", returnCode));
    if (returnCode != 0) {
      log.error("Connection to openBIS failed.");
      System.exit(1);
    }
    log.info("Connection to openBIS was successful.");

    List<String> identifiers = Argparser.readProvidedIndentifiers(new File(idPath));
    log.info(String.format("%s provided openBIS identifiers have been found: %s",
        identifiers.size(), identifiers.toString()));

    // Download datasets for all provided identifiers
    for (String id : identifiers) {
      log.info(String.format("Downloading files for provided identifier %s", id));
      List<DataSet> foundDataSets = qbicDataLoader.findAllDatasets(id);

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


