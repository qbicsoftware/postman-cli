package life.qbic.qpostman.list;

import java.util.StringJoiner;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

public class ListOptions {
    @Option(names = "--with-checksum", defaultValue = "false",
        description = "list the crc32 checksum for each file")
    public boolean withChecksum;

    @Option(names = "--exact-filesize", defaultValue = "false",
        description = "use exact byte count instead of unit suffixes: Byte, Kilobyte, Megabyte, Gigabyte, Terabyte and Petabyte using base 2 for sizes.",
        showDefaultValue = Visibility.ON_DEMAND)
    public boolean exactFilesize;

    @Option(names = "--format", defaultValue = "LEGACY",
        converter = OutputFormat.OutputFormatConverter.class,
        completionCandidates = OutputFormat.CompletionCandidates.class,
        paramLabel = "<format>",
        description = "The format to list files in. Case-insensitive. Possible values: ${COMPLETION-CANDIDATES}",
        showDefaultValue = Visibility.ALWAYS,
        required = true)
    public OutputFormat outputFormat;

    @Option(names = "--without-header", defaultValue = "false",
        description = "remove the header line from the output. Only takes effect for tabular output formats.",
        showDefaultValue = Visibility.ON_DEMAND)
    public boolean withoutHeader;

    @Override
    public String toString(){
        return new StringJoiner(", ", ListOptions.class.getSimpleName() + "[", "]")
            .add("withChecksum=" + withChecksum)
            .add("exactFilesize=" + exactFilesize)
            .toString();
    }
}
