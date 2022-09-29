package life.qbic.model.units;

class MegaBytes implements UnitDisplay {

  private final double divisor = Math.pow(1000, 2);

  @Override
  public double convertBytesToUnit(long bytes) {
    return (double) bytes / divisor;
  }

  @Override
  public String getUnitType() {
    return "MB";
  }
}
