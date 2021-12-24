package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import jline.internal.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OutputPathFinder {

    public static Path getTopDirectory(Path path) {
      Path currentPath = Paths.get(path.toString());
      Path parentPath;
      while (currentPath.getParent() != null) {
        parentPath = currentPath.getParent();
        currentPath = parentPath;
      }
      return currentPath;
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
    private static boolean isPathValid(String possiblePath){
        Path path = Paths.get(possiblePath);
        return Files.isDirectory(path);
    }

    public static File determineOutputDirectoryForData(String outputPath, Path prefix, Path filePath){
        File newFile;
        String newPath = File.separator + prefix.toString() + File.separator + filePath.toString();

        if(life.qbic.App.isNotNullOrEmpty(outputPath) && isPathValid(outputPath)){
            Log.info("Output directory: " + outputPath + File.separator + prefix.toString());
            newFile = new File(outputPath + newPath);
        } else {
            Log.info("Output directory: " + System.getProperty("user.dir") + File.separator + prefix.toString());
            newFile = new File(System.getProperty("user.dir") + newPath);
        }
        newFile.getParentFile().mkdirs();
        return newFile;
    }
    public static Path determineOutputDirectoryForChecksum(String outputPath, Path prefix, Path filePath){
        Path path;
        if (life.qbic.App.isNotNullOrEmpty(outputPath) && isPathValid(outputPath)) {
            path = Paths.get(outputPath,File.separator, prefix.toString(), File.separator, filePath.toString());
        } else {
            path = Paths.get(prefix.toString(), File.separator, filePath.toString());
        }
        return path;
    }
}
