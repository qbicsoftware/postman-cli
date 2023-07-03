package life.qbic.qpostman.common.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import life.qbic.qpostman.common.structures.DataFile;

/**
 * A class implementing the {@link Predicate} interface and providing file filtering functionality based on suffixes.
 */
public class FileFilter implements Predicate<DataFile> {

    private final List<String> suffixes;
    private final boolean caseSensitive;

    public static FileFilter create() {
        return new FileFilter(new ArrayList<>(), false);
    }

    public FileFilter withSuffixes(List<String> suffixes) {
        var temp = new ArrayList<>(this.suffixes);
        temp.addAll(suffixes);
        return new FileFilter(temp, caseSensitive);
    }

    private FileFilter(List<String> suffixes, boolean caseSensitive) {
        this.suffixes = suffixes;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean test(DataFile dataFile) {
        boolean result = true;
        if (!suffixes.isEmpty()) {
            result &= suffixes.stream()
                    .anyMatch(suffix -> hasSuffix(dataFile.fileName(), suffix));
        }
        return result;
    }

    private static boolean hasSuffix(String input, String suffix) {
        return input.toLowerCase().endsWith(suffix.toLowerCase());
    }
}
