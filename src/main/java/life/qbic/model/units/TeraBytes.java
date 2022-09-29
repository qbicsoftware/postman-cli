package life.qbic.model.units;

class TeraBytes implements UnitDisplay {

  private static final double DIVISOR = Math.pow(1000, 4);

  @Override
  public double convertBytesToUnit(long bytes) {
    return (double) bytes / DIVISOR;
  }

  @Override
  public String getUnitType() {
    return "TB";
  }
}
