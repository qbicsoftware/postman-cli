package life.qbic.model.units;

class GigaBytes implements UnitDisplay {

  private String unit = "Gb";

  private double divisor = Math.pow(1024, 3);

  @Override
  public double convertBytesToUnit(long bytes) {
    return (double) bytes / divisor;
  }

  @Override
  public String getUnitType() {
    return this.unit;
  }
}
