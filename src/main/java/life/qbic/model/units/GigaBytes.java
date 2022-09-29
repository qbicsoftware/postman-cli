package life.qbic.model.units;

class GigaBytes implements UnitDisplay {

  private static final double DIVISOR = Math.pow(1000, 3);

  @Override
  public double convertBytesToUnit(long bytes) {
    return (double) bytes / DIVISOR;
  }

  @Override
  public String getUnitType() {
    return "GB";
  }
}
