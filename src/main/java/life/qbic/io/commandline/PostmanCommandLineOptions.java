package life.qbic.io.commandline;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import life.qbic.App;
import life.qbic.io.parser.IdentifierParser;
import life.qbic.model.download.Authentication;
import life.qbic.model.download.QbicDataDisplay;
import life.qbic.model.download.QbicDataDownloader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// main command with format specifiers for the usage help message
@Command(name = "postman-cli",
        footer = "Optional: specify a config file by running postman with '@/path/to/config.txt'. Details can be found in the README.",
        description = "A client software for dataset downloads from QBiC's data management system openBIS.",
        usageHelpAutoWidth = true,
        sortOptions = false,
        descriptionHeading = "%nDescription:%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        commandListHeading = "%nCommands:%n",
        footerHeading = "%n",
        versionProvider = VersionProvider.class)

public class PostmanCommandLineOptions {
  //parameters to format the help message
  @Command(name = "download",
      description = "Download data from OpenBis",
      usageHelpAutoWidth = true,
      sortOptions = false,
      descriptionHeading = "%nDescription: ",
      parameterListHeading = "%nParameters:%n",
      optionListHeading = "%nOptions:%n",
      footerHeading = "%n")
  void download(
      @Option(names = {"-c", "--conserve"},
          description = "Conserve the file path structure during download") boolean conservePath,
      @Option(names = {"-b", "--buffer-size"}, defaultValue = "1",
          description =
              "dataset download performance can be improved by increasing this value with a multiple of 1024 (default)."
                  + " Only change this if you know what you are doing.") int bufferMultiplier,
      @Option(
          names = {"-o", "--output-dir"},
          description = "provide the path to an existing directory where you want to download your data to") String outputPath)
      throws IOException {
    Authentication authentication = App.loginToOpenBIS(passwordEnvVariable, user, as_url);

    QbicDataDownloader qbicDataDownloader =
        new QbicDataDownloader(
            as_url,
            dss_url,
            bufferMultiplier * 1024,
            conservePath,
            authentication.getSessionToken(),
            outputPath);
    ids = verifyProvidedIdentifiers();
    qbicDataDownloader.downloadRequestedFilesOfDatasets(ids, suffixes);
  }

  @Command(name = "list",
      description = "lists all the datasets found for the given identifiers",
      usageHelpAutoWidth = true,
      sortOptions = false,
      descriptionHeading = "%nDescription: ",
      parameterListHeading = "%nParameters:%n",
      optionListHeading = "%nOptions:%n",
      footerHeading = "%n")
  void listDatasets()
      throws IOException {
    Authentication authentication = App.loginToOpenBIS(passwordEnvVariable, user, as_url);
    QbicDataDisplay qbicDataDisplay = new QbicDataDisplay(as_url, dss_url,
        authentication.getSessionToken());
    ids = verifyProvidedIdentifiers();
    qbicDataDisplay.getInformation(ids, suffixes);
  }

  @Parameters(paramLabel = "SAMPLE_ID", description = "one or more QBiC sample ids", scope = CommandLine.ScopeType.INHERIT)
  public List<String> ids;

  @Option(
      names = {"-u", "--user"},
      required = true,
      description = "openBIS user name",
      scope = CommandLine.ScopeType.INHERIT)
  public String user;

  @Option(
      names = {"-p", "--env-password"},
      description = "provide the name of an environment variable to read in the password from",
      scope = CommandLine.ScopeType.INHERIT)
  public String passwordEnvVariable;

  @Option(
          names = {"-as", "-as_url"},
          description = "ApplicationServer URL",
          scope = CommandLine.ScopeType.INHERIT)
  public String as_url = "https://qbis.qbic.uni-tuebingen.de/openbis/openbis";

  @Option(
          names = {"-dss", "--dss_url"},
          description = "DataStoreServer URL",
          scope = CommandLine.ScopeType.INHERIT)
  public String dss_url = "https://qbis.qbic.uni-tuebingen.de/datastore_server";

  @Option(
          names = {"-f", "--file"},
          description = "a file with line-separated list of QBiC sample ids",
          scope = CommandLine.ScopeType.INHERIT)
  public Path filePath;

  @Option(names = {"-s", "--suffix"},
      split = ",",
      description= "only include files ending with one of these suffixes",
      paramLabel = "<suffix>",
      scope = CommandLine.ScopeType.INHERIT)
  public List<String> suffixes;


  @Option(
          names = {"-h", "--help"},
          usageHelp = true,
          description = "display a help message and exit",
          scope = CommandLine.ScopeType.INHERIT)
  public boolean helpRequested = false;

  @Option(names = {"-V", "--version"},
          versionHelp = true,
          description = "display version info")
  public boolean versionInfoRequested;

  private List<String> verifyProvidedIdentifiers() throws IOException {
    if ((isNull(ids) || ids.isEmpty()) && isNull(filePath)) {
      System.err.println(
          "You have to provide one ID as command line argument or a file containing IDs.");
      System.exit(1);
    } else if (nonNull(ids) && nonNull(filePath)) {
      System.err.println(
          "Arguments --identifier and --file are mutually exclusive, please provide only one.");
      System.exit(1);
    } else if (nonNull(filePath)){
      ids = IdentifierParser.readProvidedIdentifiers(filePath.toFile());
    }
    return ids;
  }

}
