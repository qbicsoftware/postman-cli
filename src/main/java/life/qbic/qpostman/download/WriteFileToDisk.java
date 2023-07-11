package life.qbic.qpostman.download;

import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.function.Function;
import life.qbic.qpostman.common.structures.DataFile;
import life.qbic.qpostman.download.WriteFileToDisk.DownloadReport;
import life.qbic.qpostman.openbis.OpenBisSessionProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A function writing a DataFile to disk and returning the write report.
 */
public class WriteFileToDisk implements Function<DataFile, DownloadReport> {

    private static final String LOG_PATH = System.getProperty("log.path", "logs");

    private final IDataStoreServerApi dataStoreServerApi;
    private final int bufferSize;
    private final Path outputDirectory;
    private final int downloadAttempts;

    private static final Logger log = LogManager.getLogger(WriteFileToDisk.class);
    public WriteFileToDisk(IDataStoreServerApi dataStoreServerApi, int bufferSize, Path outputDirectory,
        int downloadAttempts) {
        this.dataStoreServerApi = dataStoreServerApi;
        this.bufferSize = bufferSize;
        this.outputDirectory = outputDirectory;
        this.downloadAttempts = downloadAttempts;
    }

    private static Path toOutputPath(DataFile dataFile, Path outputDirectory) {
        String originalFilePath = dataFile.filePath();
        Path outFilePath = outputDirectory.resolve(originalFilePath);
        return outFilePath;
    }

    private AutoClosableDataSetFileDownloadReader toReader(DataFile dataFile) {
        return new AutoClosableDataSetFileDownloadReader(
            dataStoreServerApi.downloadFiles(OpenBisSessionProvider.get().getToken(),
                Collections.singletonList(dataFile.fileId()),
                new DataSetFileDownloadOptions()));
    }

    private InputStream toInputStream(DataSetFileDownloadReader reader) {
        return reader.read().getInputStream();
    }

    /**
     * Download the specific data file.
     *
     * @param dataFile the data file to be applied
     * @return the download report
     */
    @Override
    public DownloadReport apply(DataFile dataFile) {
        int bufferSize =
            (dataFile.fileSize().bytes() < this.bufferSize) ? (int) dataFile.fileSize().bytes()
                : this.bufferSize;
        Path outputPath = toOutputPath(dataFile, outputDirectory);
        if (WriteUtils.doesExistWithCrc32(outputPath, dataFile.crc32(), bufferSize)) {
            log.info("File " + outputPath + " exists on your machine.");
            return new DownloadReport(dataFile.crc32(), dataFile.crc32(), outputPath.toAbsolutePath());
        }
        DownloadReport downloadReport = null;
        for (int attempt = 1; attempt <= downloadAttempts; attempt++) {
            downloadReport = writeToDisk(dataFile, new DownloadProgressListener(dataFile.fileName(), dataFile.fileSize().bytes()));

            if (downloadReport.isSuccess()) {
                return downloadReport;
            } else {
                log.warn("Download attempt %s / %s failed for %s".formatted(attempt, downloadAttempts, downloadReport.outputPath()));
                log.trace(downloadReport);
            }
        }
        try {
            Path file = Path.of(LOG_PATH, "checksum-mismatch.log");
            assert downloadReport != null : "download report is null";
            Files.writeString(file, downloadReport + "\n", StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return downloadReport;
    }

    public record DownloadReport(long expectedCrc32, long actualCrc32, Path outputPath) {
        public boolean isSuccess() {
            return expectedCrc32 == actualCrc32;
        }
        public boolean isFailure() {
            return !isSuccess();
        }

        @Override
        public String toString() {
            return "%s\t%s\t%s".formatted(Long.toHexString(expectedCrc32()),
                Long.toHexString(actualCrc32()),
                outputPath());
        }
    }

    private DownloadReport writeToDisk(DataFile dataFile, WriteProgressListener progressListener) {
        Path outFile = toOutputPath(dataFile, outputDirectory);

        Path crc32File = Path.of(outFile.toAbsolutePath() + ".crc32");
        try {
            Files.createDirectories(outFile.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (AutoClosableDataSetFileDownloadReader reader = toReader(dataFile); //we need to reader here, so it is closed correctly
            InputStream inputStream = toInputStream(reader);
            FileOutputStream outputStream = new FileOutputStream(outFile.toFile());
            BufferedWriter crc32FileWriter = new BufferedWriter(
                new FileWriter(crc32File.toFile()))) {
            int bufferSize =
                (dataFile.fileSize().bytes() < this.bufferSize) ? (int) dataFile.fileSize().bytes()
                    : this.bufferSize;
            long writtenCrc32 = WriteUtils.write(bufferSize, inputStream, outputStream,
                progressListener);
            if (writtenCrc32 != dataFile.crc32()) {
                return new DownloadReport(dataFile.crc32(), writtenCrc32, outFile.toAbsolutePath());
            }
            try {
                Files.createDirectories(crc32File.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            crc32FileWriter.write(Long.toHexString(writtenCrc32) + "\t" + dataFile.fileName());
            crc32FileWriter.flush();
            return new DownloadReport(dataFile.crc32(), writtenCrc32, outFile.toAbsolutePath());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new DownloadReport(dataFile.crc32(), 0, outFile.toAbsolutePath());
        }
    }
    // note: DataSetFileDownloadReader closes the input stream after it finished reading it.
    private static class AutoClosableDataSetFileDownloadReader extends DataSetFileDownloadReader implements AutoCloseable {
        public AutoClosableDataSetFileDownloadReader(InputStream in) {
            super(in);
        }


    }
}
