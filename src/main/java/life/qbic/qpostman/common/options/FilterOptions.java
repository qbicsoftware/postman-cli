package life.qbic.qpostman.common.options;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import picocli.CommandLine;

public class FilterOptions {
    @CommandLine.Option(names = {"-s", "--suffix"},
            split = ",",
            description= "only include files ending with one of these (case-insensitive) suffixes",
            paramLabel = "<suffix>")
    public List<String> suffixes = new ArrayList<>(0);

    @CommandLine.Option(names = {"--pattern"},
        description= "only include files with paths matching this pattern",
        paramLabel = "<regex_pattern>")
    public String pattern = null;

    @Override
    public String toString() {
        return new StringJoiner(", ", FilterOptions.class.getSimpleName() + "[", "]")
                .add("suffixes=" + suffixes)
                .toString();
    }
}
