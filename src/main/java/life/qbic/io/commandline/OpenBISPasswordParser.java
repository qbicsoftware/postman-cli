package life.qbic.io.commandline;

import java.io.Console;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OpenBISPasswordParser {

  private static final Logger LOG = LogManager.getLogger(OpenBISPasswordParser.class);

  /**
   * Retrieve the password from input stream
   *
   * @return the password read from the input stream
   */
  public static String readPasswordFromInputStream() {
    char[] password;
    Console console = System.console();
    if (console == null) {
      LOG.error(
          "Could not get console instance!"
              + " Please make sure that you're running this from a normal console, a console supplied by an IDE will not suffice!");
      return "";
    }
    password = console.readPassword();

    return new String(password);
  }

  /** Definition of some useful enum types for the cmd attributes */
  public enum Attribute {
    HELP,
    USERNAME,
    ID,
    FILE,
    BUFFER_SIZE,
    CONSERVE_PATH
  }
}
