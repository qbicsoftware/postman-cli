package life.qbic;

import java.nio.file.Path;
import java.util.List;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class MyCommandLine {

    @Option(names = {"-u", "--user"}, required = true, description = "openBIS user name")
    protected String user;

    @Parameters(paramLabel = "SAMPLE_ID", description = "one or more QBiC sample ids")
    protected List<String> ids;

    @Option(names = {"-f", "--file"}, description = "a file with line-separated list of QBiC sample ids")
    protected Path filePath;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    protected boolean helpRequested = false;

    @Option(names = {"-b", "--buffer-size"}, description = "a integer muliple of 1024 bytes (default). Only change this if you know what you are doing.")
    protected int bufferMultiplier = 1;
}