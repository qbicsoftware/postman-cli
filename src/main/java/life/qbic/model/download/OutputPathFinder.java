package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Methods to determine the final path for the output directory.
 * The requested data will be downloaded into this directory.
 */
public class OutputPathFinder {

    private static final Logger LOG = LogManager.getLogger(OutputPathFinder.class);

    /**
     * @param path to be shortened
     * @return path that has no parents (top directory)
     */
    private static Path getTopDirectory(Path path) {
      Path currentPath = Paths.get(path.toString());
      Path parentPath;
      while (currentPath.getParent() != null) {
        parentPath = currentPath.getParent();
        currentPath = parentPath;
      }
      return currentPath;
    }

    /**
     * @param possiblePath: string that could be an existing Path to a directory
     * @return true if path exists, false otherwise
     */
    private static boolean isPathValid(String possiblePath){
        Path path = Paths.get(possiblePath);
        return Files.isDirectory(path);
    }

    /**
     * @param file to download
     * @param conservePaths if true, directory structure will be conserved
     * @return final path to file itself
     */
    private static Path determineFinalPathFromDataset(DataSetFile file, Boolean conservePaths ) {
        Path finalPath;
        if (conservePaths) {
            finalPath = Paths.get(file.getPath());
            // drop top parent directory name in the openBIS DSS (usually "/origin")
            Path topDirectory = getTopDirectory(finalPath);
            finalPath = topDirectory.relativize(finalPath);
        } else {
            finalPath = Paths.get(file.getPath()).getFileName();
        }
        return finalPath;
    }

    /**
     * @param outputPath provided by user
     * @param prefix sample code
     * @param file to download
     * @param conservePaths provided by user
     * @return output directory path
     */
    public static Path determineOutputDirectory(String outputPath, Path prefix, DataSetFile file, boolean conservePaths){
        Path filePath = determineFinalPathFromFile(file, conservePaths);
        String path = File.separator + prefix.toString() + File.separator + filePath.toString();
        Path finalPath = Paths.get("");
        if (outputPath != null && !outputPath.isEmpty()) {
            if(isPathValid(outputPath)) {
                finalPath = Paths.get(outputPath + path);
            } else{
                LOG.error("The path you provided does not exist.");
                System.exit(1);
            }
        } else {
            finalPath = Paths.get(System.getProperty("user.dir") + path);
        }
        return finalPath;
    }
}
