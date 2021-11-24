package life.qbic.io.commandline;

import life.qbic.App;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Console;

public class OpenBISPasswordParser {

  private static final Logger LOG = LogManager.getLogger(App.class);


  /**
   * Retrieve the password from the system console
   *
   * @return the password read from the system console input
   */
  public static String readPasswordFromConsole() {
    Console console = System.console();
    char[] passwordChars = console.readPassword();
    return String.valueOf(passwordChars);
  }

  /**
   *
   * @param variableName Name of given environment variable
   * @param user
   *
   * @return the password read from the environment variable
   */
  public static String readPasswordFromEnvVariable(String variableName, String user){

    String password = System.getenv(variableName);
    if (password == null) {
      LOG.info(String.format("Unfortunately, the given environment variable does not exist. " +
              "Please provide password for user '%s':", user));
      password = readPasswordFromConsole();
    }
    return password;
  }

}


