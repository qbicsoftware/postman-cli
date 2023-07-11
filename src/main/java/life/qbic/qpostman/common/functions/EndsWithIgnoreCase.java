package life.qbic.qpostman.common.functions;

import java.util.function.BiFunction;

public class EndsWithIgnoreCase implements BiFunction<String, String, Boolean> {

  public static boolean endsWithIgnoreCase(String input, String suffix) {
    int suffixLength = suffix.length();
    return input.regionMatches(true, input.length() - suffixLength, suffix, 0, suffixLength);
  }

  @Override
  public Boolean apply(String input, String suffix) {
    return endsWithIgnoreCase(input, suffix);
  }
}
