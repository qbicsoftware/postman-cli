package life.qbic.io.commandline;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Argparser {

  private final static Logger LOG = LogManager.getLogger(Argparser.class);

    /**
   * Retrieve the password from input stream
   * 
   * @return The password
     */
  public static String readPasswordFromInputStream() {
    char[] password;
    Console console = System.console();
    if (console == null) {
      LOG.error("Could not get console instance!" +
              " Please make sure that you're running this from a normal console, a console supplied by an IDE will not suffice!");
      return "";
    }
    password = console.readPassword();
    return new String(password);
  }

  /**
   * Retrieve the identifiers from provided file
   * 
   * @return Identifiers for which datasets will be retrieved
   * @throws IOException
   */
  public static List<String> readProvidedIndentifiers(File file) throws IOException {
    List<String> identifiers = new ArrayList<>();
    Scanner scanner = new Scanner(file);
    while (scanner.hasNext()) {
      identifiers.add(scanner.nextLine());
    }
    return identifiers;
  }

  /**
   * Definition of some useful enum types for the cmd attributes
   */
  public enum Attribute {
    HELP, USERNAME, ID, FILE, BUFFER_SIZE
  }

}

