package life.qbic.io.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IdentifierParser {

  /**
   * Retrieve the identifiers from provided file
   *
   * @return Identifiers for which datasets will be retrieved
   * @throws IOException
   */
  public static List<String> readProvidedIdentifiers(File file) throws IOException {
    List<String> identifiers = new ArrayList<>();
    Scanner scanner = new Scanner(file);
    while (scanner.hasNext()) {
      identifiers.add(scanner.nextLine());
    }
    return identifiers;
  }
}
