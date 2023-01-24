package life.qbic.model;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Global configuration container
 */
public class Configuration {

    public static long MAX_DOWNLOAD_ATTEMPTS = 3;
    public static Path LOG_PATH = Paths.get(System.getProperty("user.dir"),"logs");
}
