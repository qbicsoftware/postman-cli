package life.qbic.model.files;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Formats a file size to human-readable form.
 */
public class FileSizeFormatter {

  private enum Unit {
    BYTE(1000, 0, "B"),
    KILO_BYTE(1000, 1, "KB"),
    MEGA_BYTE(1000, 2, "MB"),
    GIGA_BYTE(1000, 3, "GB"),
    TERA_BYTE(1000, 4, "TB");

    private final BigDecimal divisor;

    private final String siSymbol;

    Unit(int base, int power, String siSymbol) {
      this.divisor = BigDecimal.valueOf(base).pow(power);
      this.siSymbol = siSymbol;
    }

    public String symbol() {
      return siSymbol;
    }

    static Unit bestFor(FileSize fileSize) {
      return Arrays.stream(Unit.values())
          .filter(it -> fileSize.bytes() >= it.divisor.longValueExact())
          .max(Comparator.comparing((Unit it) ->
              it.divisor))
          .orElse(BYTE);
    }

    BigDecimal scaledValue(long bytes) {
      return BigDecimal.valueOf(bytes).divide(divisor, 32, RoundingMode.HALF_UP);
    }
  }

  /**
   * Formats a file size to the respective human-readable unit.
   * <ul>
   *  <li> formats 999 bytes as 999 B
   *  <li> formats 1000 bytes as 1.00 KB
   *  <li> formats 1000000 bytes as  1.00 MB
   *
   * @param fileSize the file size to format
   * @param minWidth the minimal width of the returned String
   * @return a string representing the file size in human-readable format
   */
  public static String format(FileSize fileSize, int minWidth) {
    minWidth = Math.max(minWidth, 1);
    Unit bestUnit = Unit.bestFor(fileSize);
    DecimalFormat decimalFormat = new DecimalFormat("#0.00");
    decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
    return String.format("%" + minWidth + "s %2s",
        decimalFormat.format(bestUnit.scaledValue(fileSize.bytes())), bestUnit.symbol());
  }

  /**
   /**
   * Formats a file size to the respective human-readable unit.
   * <ul>
   *  <li> formats 999 bytes as 999 B
   *  <li> formats 1000 bytes as 1.00 KB
   *  <li> formats 1000000 bytes as 1.00 MB
   *
   * @param fileSize the file size to format
   * @return a formatted output with a minimal length of 10
   * @see #format(FileSize, int)
   */
  public static String format(FileSize fileSize) {
    return format(fileSize, 1);
  }



}
