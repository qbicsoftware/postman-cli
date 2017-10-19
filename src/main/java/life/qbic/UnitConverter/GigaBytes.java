package life.qbic.UnitConverter;


class GigaBytes implements UnitDisplay{

    private String unit = "Gb";

    private double divisor = Math.pow(10, 9);


    @Override
    public double convertBytesToUnit(long bytes) {
        return (double) bytes/divisor;
    }

    @Override
    public String getUnitType() {
        return this.unit;
    }
}
