package life.qbic.io.commandline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OpenBISPasswordParser {

  private static final Logger LOG = LogManager.getLogger(OpenBISPasswordParser.class);

  /**
   * Retrieve the password from input stream
   *
   * @return the password read from the input stream
   */
  public static String readPasswordFromInputStream(InputStream inputStream) {
    String password = "";
    try(BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream))){
      password = inputReader.readLine();
    } catch (IOException e){
      LOG.error(e);
    }
    return password;
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
