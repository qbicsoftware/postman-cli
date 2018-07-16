package life.qbic.io.commandline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class PostmanCommandLineOptions {

    @Option(names = {"-u", "--user"}, required = true, description = "openBIS user name")
    public String user;

    // this consumes all parameters that are not labeled!
    @Parameters(paramLabel = "SAMPLE_ID", description = "one or more QBiC sample ids")
    public List<String> ids;

    @Option(names = {"-f", "--file"}, description = "a file with line-separated list of QBiC sample ids")
    public Path filePath;

    @Option(names = {"-b", "--buffer-size"}, description = " Dataset download performance can be improved by increasing this value with a multiple of 1024 (default)."
            + " Only change this if you know what you are doing.")
    public int bufferMultiplier = 1;

    @Option(names = {"-s", "--suffix"}, description = "Returns all datasets containing the supplied suffix")
    public List<String> suffixes = new ArrayList<>();

    @Option(names = {"-t", "--type"}, description = "filter for a given openBIS dataset type")
    public String datasetType = "";

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    public boolean helpRequested = false;

}

