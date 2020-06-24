package life.qbic.model.UnitConverter;

class MegaBytes implements UnitDisplay {

  private String unit = "Mb";

  private double divisor = Math.pow(1024, 2);

  @Override
  public double convertBytesToUnit(long bytes) {
    return (double) bytes / divisor;
  }

  @Override
  public String getUnitType() {
    return this.unit;
  }
}
