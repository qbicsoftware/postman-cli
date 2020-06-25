package life.qbic.model.units;

class KiloBytes implements UnitDisplay {

  private String unit = "kb";

  private double divisor = Math.pow(1024, 1);

  @Override
  public double convertBytesToUnit(long bytes) {
    return (double) bytes / divisor;
  }

  @Override
  public String getUnitType() {
    return this.unit;
  }
}
