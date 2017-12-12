package life.qbic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    String id = cmdValues.get(Argparser.Attribute.ID);

    String filePath = cmdValues.get(Argparser.Attribute.FILE);

    List<String> identifiers = new ArrayList<String>();

    if (cmdValues.containsKey(Argparser.Attribute.HELP)) {
      Argparser.printHelp();
      System.exit(0);
    }

    if (user == null) {
      Argparser.printHelp();
      System.exit(1);
    }

    if ((id == null || id.isEmpty()) && (filePath == null || filePath.isEmpty())) {
      System.out
          .println("You have to provide one ID as command line argument or a file containing IDs.");
      Argparser.printHelp();
      System.exit(1);
    } else if ((id != null) && (filePath != null)) {
      System.out.println(
          "Arguments --identifier and --file are mutually exclusive, please provide only one.");
      Argparser.printHelp();
      System.exit(1);
    } else if ((id != null)) {
      identifiers.add(id);
    } else {
      identifiers.addAll(Argparser.readProvidedIndentifiers(new File(filePath)));
    }

    System.out.format("Provide password for user \'%s\':\n", user);

    String password = Argparser.readPasswordFromInputStream();

    if (password.isEmpty()) {
      System.out.println("You need to provide a password.");
      System.exit(1);
    }

    QbicDataLoader qbicDataLoader = new QbicDataLoader(AS_URL, DSS_URL, user, password, cmdValues.get(Argparser.Attribute.BUFFER_SIZE));
    int returnCode = qbicDataLoader.login();
    log.info(String.format("OpenBis login returned with %s", returnCode));
    if (returnCode != 0) {
      log.error("Connection to openBIS failed.");
      System.exit(1);
    }
    log.info("Connection to openBIS was successful.");

    log.info(String.format("%s provided openBIS identifiers have been found: %s",
        identifiers.size(), identifiers.toString()));

    for (String ident : identifiers) {
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


