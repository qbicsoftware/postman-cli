package life.qbic.io.commandline;

import java.io.IOException;
import life.qbic.io.parser.IdentifierParser;
import picocli.CommandLine;

public class CommandLineParser {

  /**
   * Parses all passed CLI parameters Prints help menu if no commandline parameters were passed
   * verifies whether all mandatory commandline parameters have been passed (IDs and username)
   *
   * @param args
   * @return commandline parameters
   * @throws IOException
   */
  public static PostmanCommandLineOptions parseAndVerifyCommandLineParameters(String[] args)
      throws IOException {
    if (args.length == 0) {
      CommandLine.usage(new PostmanCommandLineOptions(), System.out);
      System.exit(0);
    }
    PostmanCommandLineOptions commandLineParameters = new PostmanCommandLineOptions();
    CommandLine.ParseResult parsed = new CommandLine(commandLineParameters).parseArgs(args);



    //System.out.println(parsed.subcommand().commandSpec().name().matches("status"));
    if (commandLineParameters.helpRequested) {
      CommandLine.usage(new PostmanCommandLineOptions(), System.out);
      System.exit(0);
    }
    if(parsed.subcommands().isEmpty()){
      System.out.println(
              "You have to provide one of the following subcommands: status, download");
      System.exit(1);
    }
    if ((commandLineParameters.ids == null || commandLineParameters.ids.isEmpty())
        && commandLineParameters.filePath == null) {
      System.out.println(
          "You have to provide one ID as command line argument or a file containing IDs.");
      System.exit(1);
    } else if ((commandLineParameters.ids != null) && (commandLineParameters.filePath != null)) {
      System.out.println(
          "Arguments --identifier and --file are mutually exclusive, please provide only one.");
      System.exit(1);
    } else if (commandLineParameters.filePath != null) {
      commandLineParameters.ids =
          IdentifierParser.readProvidedIdentifiers(commandLineParameters.filePath.toFile());
    }

    return commandLineParameters;
  }

  public static String getSubcommand(String[] args){
    PostmanCommandLineOptions commandLineParameters = new PostmanCommandLineOptions();
    CommandLine.ParseResult parsed = new CommandLine(commandLineParameters).parseArgs(args);
    return parsed.subcommand().commandSpec().name();
  }
}
