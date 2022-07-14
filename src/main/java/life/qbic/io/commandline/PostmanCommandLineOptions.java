package life.qbic.io.commandline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "Postman",
    footer =
        "Optional: specify a config file by running postman with '@/path/to/config.txt'. Details can be found in the README.",
    description =
        "A client software for dataset downloads from QBiC's data management system openBIS.")

public class PostmanCommandLineOptions {

  @Command(name = "download", description = "Download data from OpenBis")
    public String download() {
      return "download";
    }

  @Command(name = "status", description = "provides the status of the datasets of the given identifiers")
    public String status() {
      return "status";
    }

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

  // this consumes all parameters that are not labeled!
  @Parameters(paramLabel = "SAMPLE_ID", description = "one or more QBiC sample ids", scope = CommandLine.ScopeType.INHERIT)
  public List<String> ids;

  @Option(
      names = {"-f", "--file"},
      description = "a file with line-separated list of QBiC sample ids",
      scope = CommandLine.ScopeType.INHERIT)
  public Path filePath;

  @Option(
      names = {"-c", "--conserve"},
      description = "Conserve the file path structure during download",
      scope = CommandLine.ScopeType.INHERIT)
  public boolean conservePath;

  @Option(
      names = {"-b", "--buffer-size"},
      description =
          "dataset download performance can be improved by increasing this value with a multiple of 1024 (default)."
              + " Only change this if you know what you are doing.",
      scope = CommandLine.ScopeType.INHERIT)
  public int bufferMultiplier = 1;

  @Option(
      names = {"-s", "--suffix"},
      description = "returns all files of datasets containing the supplied suffix",
      scope = CommandLine.ScopeType.INHERIT)
  public List<String> suffixes = new ArrayList<>();

  @Option(
      names = {"-r", "--regex"},
      description = "returns all files of datasets using your supplied regular expression",
      scope = CommandLine.ScopeType.INHERIT)
  public List<String> regexPatterns = new ArrayList<>();

  @Option(
      names = {"-t", "--type"},
      description = "filter for a given openBIS dataset type",
      scope = CommandLine.ScopeType.INHERIT)
  public String datasetType = "";

  @Option(
      names = {"-dss", "--dss_url"},
      description = "DataStoreServer URL",
      scope = CommandLine.ScopeType.INHERIT)
  public String dss_url = "https://qbis.qbic.uni-tuebingen.de/datastore_server";

  @Option(
      names = {"-as", "-as_url"},
      description = "ApplicationServer URL",
      scope = CommandLine.ScopeType.INHERIT)
  public String as_url = "https://qbis.qbic.uni-tuebingen.de/openbis/openbis";

  @Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display a help message",
      scope = CommandLine.ScopeType.INHERIT)
  public boolean helpRequested = false;
}
