package life.qbic.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    public static Path longestCommonPrefix(Path p1, Path p2) {
        Path shorterPath = p1.getNameCount() <= p2.getNameCount() ? p1 : p2;
        Path longerPath = shorterPath.equals(p1) ? p2 : p1;

        if (shorterPath == longerPath) {
            return shorterPath;
        }
        if (shorterPath.getParent().equals(longerPath.getParent())) {
            return shorterPath.getParent();
        }

        Path path = shorterPath.getRoot();
        for (int index = 0; index < shorterPath.getNameCount(); index++) {
            if (shorterPath.getName(index).equals(longerPath.getName(index))) {
                path = Paths.get(path.toString(), shorterPath.getName(index).toString());
            } else {
                break;
            }
        }
        return path;
    }

}
