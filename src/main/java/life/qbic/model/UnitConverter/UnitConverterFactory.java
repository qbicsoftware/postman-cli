package life.qbic.model.UnitConverter;

public class UnitConverterFactory {

    static double kiloByteFactor = Math.pow(1024, 1);

    static double megaByteFactor = Math.pow(1024, 2);

    static double gigaByteFactor = Math.pow(1024, 3);

    static double teraByteFactor = Math.pow(1024, 4);

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
