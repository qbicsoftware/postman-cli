package life.qbic;

import life.qbic.io.commandline.CommandLineParser;
import life.qbic.io.commandline.OpenBISPasswordParser;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import life.qbic.model.download.AuthenticationException;
import life.qbic.model.download.ConnectionException;
import life.qbic.model.download.QbicDataDownloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

/** postman for staging data from openBIS */
public class App {

  private static final Logger LOG = LogManager.getLogger(App.class);

  public static void main(String[] args) throws IOException {
    // parse and verify all commandline parameters
    PostmanCommandLineOptions commandLineParameters =
        CommandLineParser.parseAndVerifyCommandLineParameters(args);

    // login to OpenBIS
    QbicDataDownloader qbicDataDownloader = loginToOpenBIS(commandLineParameters);

    // download all requested files by the user or print available datasets
    if (!commandLineParameters.printDatasets) {
      qbicDataDownloader.downloadRequestedFilesOfDatasets(commandLineParameters, qbicDataDownloader);
    } else{
      qbicDataDownloader.checkAvailableDatasets(commandLineParameters.ids);
    }
  }

  /**
   * checks if the commandline parameter for reading out the password from the environment variable
   * is correctly provided
   *
   * @param envVariableCommandLineParameter
   * @return
   */
  private static Boolean isNotNullOrEmpty(String envVariableCommandLineParameter) {
    Boolean NotNullOrEmpty = false;
    if (envVariableCommandLineParameter != null && !envVariableCommandLineParameter.isEmpty()) {
      NotNullOrEmpty = true;
    }
    return NotNullOrEmpty;
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
      Optional<String> envPassword = OpenBISPasswordParser.readPasswordFromEnvVariable(commandLineParameters.passwordEnvVariable);

      if (!envPassword.isPresent()) {
        System.out.println("No environment variable named " + commandLineParameters.passwordEnvVariable + " was found");
        LOG.info(String.format("Please provide a password for user '%s':", commandLineParameters.user));
      }
      password = envPassword.orElseGet(OpenBISPasswordParser::readPasswordFromConsole);
    } else {
      LOG.info(String.format("Please provide a password for user '%s':", commandLineParameters.user));
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
            Paths.get(System.getProperty("user.dir") + File.separator + "logs/summary_valid_files.txt"),
            Paths.get(
                System.getProperty("user.dir") + File.separator + "logs/summary_invalid_files.txt"));

    QbicDataDownloader qbicDataDownloader =
        new QbicDataDownloader(
            commandLineParameters.as_url,
            commandLineParameters.dss_url,
            commandLineParameters.user,
            password,
            commandLineParameters.bufferMultiplier * 1024,
            commandLineParameters.datasetType,
            commandLineParameters.conservePath,
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
