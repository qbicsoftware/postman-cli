package life.qbic;

import java.util.Arrays;
import life.qbic.qpostman.common.PostmanCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

/**
 * postman for staging data from openBIS
 */
public class App {

  private static final Logger LOG = LogManager.getLogger(App.class);

  public static void main(String[] args) {
    LOG.debug("command line arguments: " + Arrays.deepToString(args));
    CommandLine cmd = new CommandLine(new PostmanCommand());
    int exitCode = cmd.execute(args);
    System.exit(exitCode);
  }

}
