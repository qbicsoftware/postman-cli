package life.qbic.util;

public class StringUtil {

  public static boolean endsWithIgnoreCase(String input, String suffix) {
    int suffixLength = suffix.length();
    return input.regionMatches(true, input.length() - suffixLength, suffix, 0, suffixLength);
  }
}
