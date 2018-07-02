package life.qbic;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Argparser {

    /**
   * Retrieve the password from input stream
   * 
   * @return The password
   * @throws IOException
   */
  public static String readPasswordFromInputStream() throws IOException {
    char[] password;
    Console console = System.console();
    if (console == null) {
      System.err.println("Could not get console instance!");
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
    List<String> identifiers = new ArrayList<String>();
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

