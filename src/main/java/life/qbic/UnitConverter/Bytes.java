package life.qbic.UnitConverter;


class Bytes implements UnitDisplay{

    private String unit = "bytes";

    private double divisor = Math.pow(10, 1);


    @Override
    public double convertBytesToUnit(long bytes) {
        return (double) bytes/divisor;
    }

    @Override
    public String getUnitType() {
        return this.unit;
    }
}
