package life.qbic.qpostman.download;

import static picocli.CommandLine.Help;
import static picocli.CommandLine.Option;

import java.util.Optional;
import java.util.StringJoiner;
import picocli.CommandLine.Help.Visibility;

public class DownloadOptions {
    @Option(names = {"--buffer-size"},
            defaultValue = "1024",
            showDefaultValue = Help.Visibility.ALWAYS,
            description =
                    "dataset download performance can be improved by increasing this value with a multiple of 1024 (default)."
                            + " Only change this if you know what you are doing.",
            hidden = true)
    public int bufferSize;

    @Option(names = {"-o", "--output-dir"},
        description = "specify where to write the downloaded data")
    public String outputPath = Optional.ofNullable(System.getenv("user.dir")).orElse(".");

    @Option(names = "--download-attempts",
        defaultValue = "3",
        description = "how often to attempt file download in case the download failed",
        hidden = true)
    public int successiveDownloadAttempts;

    @Option(names = "--ignore-subdirectories", defaultValue = "false",
        description = "put all files into one directory regardless of the directory structure on the server; conflicts with files with equal names are not addressed",
        showDefaultValue = Visibility.ON_DEMAND)
    public boolean ignoreSubDirectories;

    @Override
    public String toString() {
        return new StringJoiner(", ", DownloadOptions.class.getSimpleName() + "[", "]")
                .add("bufferSize=" + bufferSize)
                .add("outputPath='" + outputPath + "'")
                .toString();
    }
}
