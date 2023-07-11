package life.qbic.qpostman.list;

import java.util.Arrays;
import java.util.Iterator;
import picocli.CommandLine.ITypeConverter;

public enum OutputFormat {
  LEGACY,
  TSV;

  public static class OutputFormatConverter implements ITypeConverter<OutputFormat> {

    @Override
    public OutputFormat convert(String input) {
      return Arrays.stream(OutputFormat.values())
          .map(Enum::name)
          .filter(name -> name.equalsIgnoreCase(input))
          .map(OutputFormat::valueOf)
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unknown format " + input));
    }
  }

  public static class CompletionCandidates implements Iterable<String> {

    @Override
    public Iterator<String> iterator() {
      return Arrays.stream(values()).map(Enum::name).iterator();
    }
  }

}
