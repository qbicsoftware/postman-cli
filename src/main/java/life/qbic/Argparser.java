package life.qbic;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Argparser {

  private static final HelpFormatter help = new HelpFormatter();

  private static final Options options = new Options();

  /**
   * Public method for parsing the command line arguments
   * 
   * @param args The args String array from cmd
   * @return A hashmap with the parsed arguments
   */
  public static HashMap<Attribute, String> parseCmdArguments(String[] args) {

    /*
     * define the options
     */
    options.addOption("h", "help", false, "Print this help");
    options.addOption("u", "user-name", true, "openBIS user name");
    options.addOption("i", "identifiers", true,
        "File containing openBis sample IDs (one per line)");

    /*
     * container for parsed attributes
     */
    HashMap<Attribute, String> parsedArguments = new HashMap<>();

    /*
     * set parser type, default is enough here
     */
    CommandLineParser parser = new DefaultParser();

    /*
     * try to parse the command line arguments
     */
    try {
      CommandLine cmd = parser.parse(options, args);
      parsedArguments.put(Attribute.USERNAME, cmd.getOptionValue("u"));
      parsedArguments.put(Attribute.ID, cmd.getOptionValue("i"));
      if (cmd.hasOption("h")) {
        parsedArguments.put(Attribute.HELP, "");
      }
    } catch (ParseException exc) {
      System.err.println(exc);
      /*
       * return empty map on fail
       */
      return parsedArguments;
    }

    return parsedArguments;
  }

  public static void printHelp() {
    help.printHelp("qPostMan", options, true);
  }

  /**
   * Retrieve the password from input stream
   * 
   * @return The password
   * @throws IOException
   */
  public static String readPasswordFromInputStream() throws IOException {
    char[] password;
    Console console = System.console();
    if (console == null) {
      System.err.println("Could not get console instance!");
      return "";
    }
    password = console.readPassword();
    return new String(password);
  }

  /**
   * Retrieve the identifiers from provided file
   * 
   * @return Identifiers for which datasets will be retrieved
   * @throws IOException
   */
  public static List<String> readProvidedIndentifiers(File file) throws IOException {
    List<String> identifiers = new ArrayList<String>();
    Scanner scanner = new Scanner(file);
    while (scanner.hasNext()) {
      identifiers.add(scanner.nextLine());
    }
    return identifiers;
  }

  /**
   * Definition of some useful enum types for the cmd attributes
   */
  public enum Attribute {
    HELP, USERNAME, ID
  }

}

