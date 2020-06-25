package life.qbic.io.commandline;

import java.io.Console;

public class OpenBISPasswordParser {

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
}
