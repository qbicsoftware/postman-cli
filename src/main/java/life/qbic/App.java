package life.qbic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

import java.util.Map;



/**
 * qPostMan for staging data from openBIS
 *
 */
public class App {

    static String AS_URL = "https://qbis.qbic.uni-tuebingen.de/openbis/openbis";
    static String DSS_URL = "https://qbis.qbic.uni-tuebingen.de:444/datastore_server";
    static Logger log = LogManager.getLogger(App.class);

  public static void main(String[] args) throws IOException{

      Map<Argparser.Attribute, String> cmdValues = Argparser.parseCmdArguments(args);

      String user = cmdValues.get(Argparser.Attribute.USERNAME);

      if (cmdValues.containsKey(Argparser.Attribute.HELP)){
          Argparser.printHelp();
          System.exit(0);
      }

      if (user == null){
          Argparser.printHelp();
          System.exit(1);
      }


      System.out.format("Provide password for user \'%s\':\n", user);

      String password = Argparser.readPasswordFromInputStream();

      if (password.isEmpty()){
          System.out.println("You need to provide a password.");
          System.exit(1);
      }

      QbicDataLoader qbicDataLoader = new QbicDataLoader(AS_URL, DSS_URL, user, password);
      log.info(String.format("Connection test returned: %s", qbicDataLoader.testConnection()));

      }
    }


