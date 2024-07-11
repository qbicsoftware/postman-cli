package life.qbic.qpostman.common.options;

import static java.util.Objects.requireNonNull;
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

public class SampleIdentifierOptions {

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

    private IdentityFileParser() {

    }
    public static List<String> parseIdentityFile(Path path) {
      File file = path.toFile();
      if (!file.exists()) {
        throw new IdentityFileNotFoundException(file);
      }
      if (!file.canRead()) {
        throw new IdentityFileNotReadableException(file);
      }
      try {
        List<String> readIdentifiers = Files.readAllLines(path).stream()
            .filter(it -> !it.isBlank())
            .filter(it -> !it.startsWith("#"))
            .map(String::strip)
            .collect(Collectors.toList());
        if (readIdentifiers.isEmpty()) {
          throw new IdentityFileEmptyException(file);
        }
        return readIdentifiers;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class IdentityFileParsingException extends RuntimeException {
    protected final File file;

    public IdentityFileParsingException(File file) {
      this.file = file;
    }

    public File getFile() {
      return file;
    }
  }

  public static class IdentityFileNotFoundException extends IdentityFileParsingException {

    public IdentityFileNotFoundException(File file) {
      super(file);
    }
  }

  public static class IdentityFileNotReadableException extends IdentityFileParsingException {


    public IdentityFileNotReadableException(File file) {
      super(file);
    }
  }

  public static class IdentityFileEmptyException extends IdentityFileParsingException {

    public IdentityFileEmptyException(File file) {
      super(file);
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
          ? parseIdentifiers()
          : ids;
      // we want to prevent matching to something shorter than a project code.
      List<String> toShortSampleIds = identifiers.stream()
          .filter(it -> !it.matches("^\\w{5,}"))
          .toList();
      if (!toShortSampleIds.isEmpty()) {
        throw new ToShortSampleIdsException(toShortSampleIds);
      }
      return identifiers;
    }

    public static class SampleIdParsingException extends RuntimeException {
    }

    public static class ToShortSampleIdsException extends SampleIdParsingException {
      protected final List<String> identifiers;

      public ToShortSampleIdsException(List<String> identifiers) {
        this.identifiers = requireNonNull(identifiers, "identifiers must not be null");
      }

      public List<String> getIdentifiers() {
        return identifiers;
      }
    }

    private List<String> parseIdentifiers() {
      return IdentityFileParser.parseIdentityFile(filePath).stream()
          .filter(it -> !it.isBlank())
          .toList();
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
