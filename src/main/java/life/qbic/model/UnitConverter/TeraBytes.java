package life.qbic.model.UnitConverter;

class TeraBytes implements UnitDisplay {

  private String unit = "Tb";

  private double divisor = Math.pow(1024, 4);

  @Override
  public double convertBytesToUnit(long bytes) {
    return (double) bytes / divisor;
  }

  @Override
  public String getUnitType() {
    return this.unit;
  }
}
