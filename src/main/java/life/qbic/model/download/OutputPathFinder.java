package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OutputPathFinder {

    private static Path getTopDirectory(Path path) {
      Path currentPath = Paths.get(path.toString());
      Path parentPath;
      while (currentPath.getParent() != null) {
        parentPath = currentPath.getParent();
        currentPath = parentPath;
      }
      return currentPath;
    }

    private static boolean isPathValid(String possiblePath){
        Path path = Paths.get(possiblePath);
        return Files.isDirectory(path);
    }

    public static Path determineFinalPathFromDataset(DataSetFile file, Boolean conservePaths ) {
        Path finalPath;
        if (conservePaths) {
            finalPath = Paths.get(file.getPath());
            // drop top parent directory name in the openBIS DSS (usually "/origin")
            Path topDirectory = OutputPathFinder.getTopDirectory(finalPath);
            finalPath = topDirectory.relativize(finalPath);
        } else {
            finalPath = Paths.get(file.getPath()).getFileName();
        }
        return finalPath;
    }

    public static Path determineOutputDirectory(String outputPath, Path prefix, Path filePath){
        Path path;
        String newPath = File.separator + prefix.toString() + File.separator + filePath.toString();
        if (life.qbic.App.isNotNullOrEmpty(outputPath) && isPathValid(outputPath)) {
            path = Paths.get(outputPath + newPath);
        } else {
            path = Paths.get(System.getProperty("user.dir") + newPath);
        }
        return path;
    }
}
