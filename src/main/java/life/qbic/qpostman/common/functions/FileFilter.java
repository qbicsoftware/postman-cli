package life.qbic.qpostman.common.functions;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import life.qbic.qpostman.common.structures.DataFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class implementing the {@link Predicate} interface and providing file filtering functionality based on suffixes.
 */
public class FileFilter implements Predicate<DataFile> {
    private static final Logger log = LogManager.getLogger(FileFilter.class);

    private final List<String> suffixes;
    private final boolean caseSensitive;
    private final Pattern pattern;

    public static FileFilter create() {
        return new FileFilter(new ArrayList<>(), false, null);
    }

    public FileFilter withSuffixes(List<String> suffixes) {
        var temp = new ArrayList<>(this.suffixes);
        temp.addAll(suffixes);
        return new FileFilter(temp, caseSensitive, pattern);
    }

    public FileFilter withPattern(String pattern) {
        if (isNull(pattern)) {
            return new FileFilter(suffixes, caseSensitive, null);
        }
        Pattern compiledPattern = null;
        try {
            compiledPattern = Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new MalformedPatternException(e, pattern, e.getMessage());
        }
        return new FileFilter(suffixes, caseSensitive, compiledPattern);
    }

    public static class MalformedPatternException extends RuntimeException {

        private final String patternString;
        private final String errorDescription;
        public MalformedPatternException(Throwable cause, String patternString,
            String errorDescription) {
            super(cause);
          this.patternString = patternString;
          this.errorDescription = errorDescription;
        }

        public String getPatternString() {
            return patternString;
        }

        public String getErrorDescription() {
            return errorDescription;
        }
    }

    private FileFilter(List<String> suffixes, boolean caseSensitive, Pattern pattern) {
        this.suffixes = suffixes;
        this.caseSensitive = caseSensitive;
        this.pattern = pattern;
    }

    @Override
    public boolean test(DataFile dataFile) {
        boolean result = true;
        if (!suffixes.isEmpty()) {
            result &= suffixes.stream()
                    .anyMatch(suffix -> hasSuffix(dataFile.fileName(), suffix));
        }
        if (nonNull(pattern)) {
            result &= pattern.matcher(dataFile.filePath()).matches();
        }
        return result;
    }

    private static boolean hasSuffix(String input, String suffix) {
        return input.toLowerCase().endsWith(suffix.toLowerCase());
    }
}
