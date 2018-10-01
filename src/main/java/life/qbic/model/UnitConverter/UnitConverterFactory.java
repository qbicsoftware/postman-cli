package life.qbic.model.UnitConverter;

public class UnitConverterFactory {

    static double kiloByteFactor = Math.pow(10, 3);

    static double megaByteFactor = Math.pow(10, 6);

    static double gigaByteFactor = Math.pow(10, 9);

    static double teraByteFactor = Math.pow(10, 12);

    public static UnitDisplay determineBestUnitType(long bytes){

        if (bytes > teraByteFactor){
            return new TeraBytes();
        }

        if (bytes > gigaByteFactor){
            return new GigaBytes();
        }

        if (bytes > megaByteFactor){
            return new MegaBytes();
        }

        if (bytes > kiloByteFactor){
            return new KiloBytes();
        }

        return new Bytes();

    }

}
