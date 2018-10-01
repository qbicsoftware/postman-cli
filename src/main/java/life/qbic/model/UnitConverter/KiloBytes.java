package life.qbic.model.UnitConverter;


class KiloBytes implements UnitDisplay{

    private String unit = "kb";

    private double divisor = Math.pow(10, 3);


    @Override
    public double convertBytesToUnit(long bytes) {
        return (double) bytes/divisor;
    }

    @Override
    public String getUnitType() {
        return this.unit;
    }
}
