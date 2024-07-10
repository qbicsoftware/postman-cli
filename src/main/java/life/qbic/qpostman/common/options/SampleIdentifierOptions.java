package life.qbic.qpostman.common.options;

import static picocli.CommandLine.ArgGroup;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SampleIdentifierOptions {

  private static final Logger log = LogManager.getLogger(SampleIdentifierOptions.class);

  @ArgGroup(multiplicity = "1")
  public SampleInput sampleInput;

  public List<String> getIds() {
    return sampleInput.getIds();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SampleIdentifierOptions.class.getSimpleName() + "[", "]")
        .add("ids=" + getIds())
        .toString();
  }

  static class IdentityFileParser {

    public static List<String> parseIdentityFile(Path path) {
      File file = path.toFile();
      if (!file.exists()) {
        log.error("File not found: " + file);
        System.exit(2);
      }
      if (!file.canRead()) {
        log.error("Not allowed to read file " + file);
        System.exit(2);
      }
      try {
        return Files.readAllLines(path).stream()
            .filter(it -> !it.isBlank())
            .filter(it -> !it.startsWith("#"))
            .map(String::strip)
            .collect(Collectors.toList());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class SampleInput {

    @Option(
        names = {"-f", "--file"},
        description = "a file with line-separated list of QBiC sample ids",
        required = true)
    public Path filePath;

    @Parameters(arity = "1..", paramLabel = "SAMPLE_IDENTIFIER", description = "one or more QBiC sample identifiers")
    public List<String> ids = new ArrayList<>();

    public List<String> getIds() {
      var identifiers = ids.isEmpty()
          ? parseIdentifiers() :
          ids;
      // we want to prevent matching to something shorter than a project code.
      List<String> toShortSampleIds = identifiers.stream()
          .filter(it -> !it.matches("^\\w{5,}"))
          .toList();
      if (toShortSampleIds.size() > 0) {
        log.error("Please provide at least 5 letters for your sample identifiers. The following sample identifiers are to short: " + toShortSampleIds);
        System.exit(2);
      }
      return identifiers;
    }

    private List<String> parseIdentifiers() {
      List<String> identities = IdentityFileParser.parseIdentityFile(filePath).stream()
          .filter(it -> !it.isBlank()).toList();

      if (identities.isEmpty()) {
        log.error(filePath.toString() + " does not contain any identifiers.");
        System.exit(2);
      }
      return identities;
    }


    @Override
    public String toString() {
      return new StringJoiner(", ", SampleInput.class.getSimpleName() + "[", "]")
          .add("filePath=" + filePath)
          .add("ids=" + ids)
          .toString();
    }
  }


}
