package life.qbic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import life.qbic.io.commandline.CommandLineParser;
import life.qbic.io.commandline.OpenBISPasswordParser;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import life.qbic.model.download.AuthenticationException;
import life.qbic.model.download.ConnectionException;
import life.qbic.model.download.QbicDataDownloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * postman for staging data from openBIS
 */
public class App {

  private static final Logger LOG = LogManager.getLogger(App.class);

  public static void main(String[] args) throws IOException {
    // parse and verify all commandline parameters
    PostmanCommandLineOptions commandLineParameters =
        CommandLineParser.parseAndVerifyCommandLineParameters(args);

    // login to OpenBIS
    QbicDataDownloader qbicDataDownloader = loginToOpenBIS(commandLineParameters);

    // download all requested files by the user or print available datasets
    qbicDataDownloader.downloadRequestedFilesOfDatasets(commandLineParameters, qbicDataDownloader);
  }

  /**
   * checks if the given commandline parameter for reading out the password from the environment variable
   * is correctly provided
   *
   * @param envVariableCommandLineParameter
   * @return true in case the provided string is
   */
  private static boolean isNotNullOrEmpty(String envVariableCommandLineParameter) {
    return envVariableCommandLineParameter != null && !envVariableCommandLineParameter.isEmpty();
  }

  /**
   * Logs into OpenBIS asks for and verifies password.
   *
   * @param commandLineParameters The command line parameters.
   * @return An instance of a QbicDataDownloader.
   */
  private static QbicDataDownloader loginToOpenBIS(
      PostmanCommandLineOptions commandLineParameters) {

    String password;
    if (isNotNullOrEmpty(commandLineParameters.passwordEnvVariable)) {
      Optional<String> envPassword = OpenBISPasswordParser.readPasswordFromEnvVariable(
          commandLineParameters.passwordEnvVariable);

      if (!envPassword.isPresent()) {
        System.out.println(
            "No environment variable named " + commandLineParameters.passwordEnvVariable
                + " was found");
        LOG.info(
            String.format("Please provide a password for user '%s':", commandLineParameters.user));
      }
      password = envPassword.orElseGet(OpenBISPasswordParser::readPasswordFromConsole);
    } else {
      LOG.info(
          String.format("Please provide a password for user '%s':", commandLineParameters.user));
      password = OpenBISPasswordParser.readPasswordFromConsole();
    }

    if (password.isEmpty()) {
      LOG.error("You need to provide a password.");
      System.exit(1);
    }

    // Ensure 'logs' folder is created
    new File(System.getProperty("user.dir") + File.separator + "logs").mkdirs();

    ChecksumReporter checksumWriter =
        new FileSystemWriter(
            Paths.get(
                System.getProperty("user.dir") + File.separator + "logs/summary_valid_files.txt"),
            Paths.get(
                System.getProperty("user.dir") + File.separator
                    + "logs/summary_invalid_files.txt"));

    QbicDataDownloader qbicDataDownloader =
        new QbicDataDownloader(
            commandLineParameters.as_url,
            commandLineParameters.dss_url,
            commandLineParameters.user,
            password,
            commandLineParameters.bufferMultiplier * 1024,
            commandLineParameters.datasetType,
            commandLineParameters.conservePath,
            commandLineParameters.outputPath,
            checksumWriter);

    try {
      qbicDataDownloader.login();
    } catch (ConnectionException e) {
      LOG.error("Could not connect to QBiC's data source. Have you requested access to the "
          + "server? If not please write to support@qbic.zendesk.com");
      System.exit(1);
    } catch (AuthenticationException e) {
      LOG.error(e.getMessage());
      System.exit(1);
    }
    return qbicDataDownloader;
  }
}
