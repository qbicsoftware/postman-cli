package life.qbic;

import life.qbic.io.commandline.OpenBISPasswordParser;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import life.qbic.model.download.Authentication;
import life.qbic.model.download.AuthenticationException;
import life.qbic.model.download.ConnectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * postman for staging data from openBIS
 */
public class App {

  private static final Logger LOG = LogManager.getLogger(App.class);

  public static void main(String[] args) throws IOException {

    CommandLine cmd = new CommandLine(new PostmanCommandLineOptions());
    int exitCode = cmd.execute(args);
    System.exit(exitCode);

  }

  /**
   * checks if the commandline parameter for reading out the password from the environment variable
   * is correctly provided
   */
  private static Boolean isNotNullOrEmpty(String envVariableCommandLineParameter) {
    return envVariableCommandLineParameter != null && !envVariableCommandLineParameter.isEmpty();
  }

  /**
   * Logs into OpenBIS asks for and verifies password.
   *
   * @return An instance of the Authentication class.
   */
  public static Authentication loginToOpenBIS(
      String passwordEnvVariable, String user, String as_url) {

    String password;
    if (isNotNullOrEmpty(passwordEnvVariable)) {
      Optional<String> envPassword = OpenBISPasswordParser.readPasswordFromEnvVariable(
          passwordEnvVariable);
      if (!envPassword.isPresent()) {
        System.out.println(
            "No environment variable named " + passwordEnvVariable
                + " was found");
        LOG.info(
            String.format("Please provide a password for user '%s':", user));
      }
      password = envPassword.orElseGet(OpenBISPasswordParser::readPasswordFromConsole);
    } else {
      LOG.info(
          String.format("Please provide a password for user '%s':", user));
      password = OpenBISPasswordParser.readPasswordFromConsole();
    }

    if (password.isEmpty()) {
      LOG.error("You need to provide a password.");
      System.exit(1);
    }

    // Ensure 'logs' folder is created
    new File(System.getProperty("user.dir") + File.separator + "logs").mkdirs();
    Authentication authentication =
            new Authentication(
                    user,
                    password,
                    as_url);
    try {
      authentication.login();
    } catch (ConnectionException e) {
      LOG.error("Could not connect to QBiC's data source. Have you requested access to the "
          + "server? If not please write to support@qbic.zendesk.com");
      System.exit(1);
    } catch (AuthenticationException e) {
      LOG.error(e.getMessage());
      System.exit(1);
    }
    return authentication;
  }
}
