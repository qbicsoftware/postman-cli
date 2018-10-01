package life.qbic.model.UnitConverter;


class TeraBytes implements UnitDisplay{

    private String unit = "Tb";

    private double divisor = Math.pow(10, 12);


    @Override
    public double convertBytesToUnit(long bytes) {
        return (double) bytes/divisor;
    }

    @Override
    public String getUnitType() {
        return this.unit;
    }
}
