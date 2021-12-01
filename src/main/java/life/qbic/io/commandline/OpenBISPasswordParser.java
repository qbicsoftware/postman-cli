package life.qbic.io.commandline;

import java.io.Console;
import java.util.Optional;

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

  /**
   * checks if the commandline parameter for reading out the password from the environment variable
   * is correctly provided
   *
   * @param envVariableCommandLineParameter
   * @return
   */
  public static Boolean isNotNullOrEmpty(String envVariableCommandLineParameter) {
    Boolean NotNullOrEmpty = false;
    if (envVariableCommandLineParameter != null && !envVariableCommandLineParameter.isEmpty()) {
      NotNullOrEmpty = true;
    }
    return NotNullOrEmpty;
  }

  /**
   *
   * @param variableName Name of given environment variable
   *
   * @return the password read from the environment variable
   */
  public static Optional<String> readPasswordFromEnvVariable(String variableName){

    Optional<String> password = Optional.ofNullable(System.getenv(variableName));
    return password;

  }

}


