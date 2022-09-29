package life.qbic.model.units;

public class UnitConverterFactory {

  private static final double KILO_BYTE_FACTOR = Math.pow(1000, 1);

  private static final double MEGA_BYTE_FACTOR = Math.pow(1000, 2);

  private static final double GIGA_BYTE_FACTOR = Math.pow(1000, 3);

  private static final double TERA_BYTE_FACTOR = Math.pow(1000, 4);

  public static UnitDisplay determineBestUnitType(long bytes) {

    if (bytes > TERA_BYTE_FACTOR) {
      return new TeraBytes();
    }

    if (bytes > GIGA_BYTE_FACTOR) {
      return new GigaBytes();
    }

    if (bytes > MEGA_BYTE_FACTOR) {
      return new MegaBytes();
    }

    if (bytes > KILO_BYTE_FACTOR) {
      return new KiloBytes();
    }

    return new Bytes();
  }
}
