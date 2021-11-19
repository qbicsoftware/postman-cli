package life.qbic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import life.qbic.io.commandline.CommandLineParser;
import life.qbic.io.commandline.OpenBISPasswordParser;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import life.qbic.model.download.AuthenticationException;
import life.qbic.model.download.ConnectionException;
import life.qbic.model.download.QbicDataDownloader;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** postman for staging data from openBIS */
public class App {

  private static final Logger LOG = LogManager.getLogger(App.class);

  public static void main(String[] args) throws IOException {
    // parse and verify all commandline parameters
    PostmanCommandLineOptions commandLineParameters =
        CommandLineParser.parseAndVerifyCommandLineParameters(args);

    // login to OpenBIS
    QbicDataDownloader qbicDataDownloader = loginToOpenBIS(commandLineParameters);

    // download all requested files by the user
    qbicDataDownloader.downloadRequestedFilesOfDatasets(commandLineParameters, qbicDataDownloader);
  }

  /**
   * Logs into OpenBIS asks for and verifies password.
   *
   * @param commandLineParameters The command line parameters.
   * @return An instance of a QbicDataDownloader.
   */
  private static QbicDataDownloader loginToOpenBIS(
      PostmanCommandLineOptions commandLineParameters) {

    String password = "";

    //Password read in from environment Variable
    if ((!(commandLineParameters.environmentVariableName.isEmpty()))) {

      if (System.getenv(commandLineParameters.environmentVariableName) == null) {

        LOG.info(String.format("Unfortunately, the given environment variable does not exist. " +
                "Please provide password for user '%s':", commandLineParameters.user));

        password = OpenBISPasswordParser.readPasswordFromConsole();
      }
      else {
        password = System.getenv(commandLineParameters.environmentVariableName);
      }
    }
    
    //Password read in from Console
    else {

      LOG.info(String.format("Please provide password for user '%s':", commandLineParameters.user));

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
