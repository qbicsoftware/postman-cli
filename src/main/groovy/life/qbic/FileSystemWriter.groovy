package life.qbic


import java.nio.file.Path
import java.nio.file.Paths

/**
 * <add class description here>
 *
 * @author: Sven Fillinger
 */
class FileSystemWriter implements ChecksumWriter {
    
    final private File matchingSummaryFile

    final private File failureSummaryFile

    FileSystemWriter(Path matchingSummarFile, Path failureSummaryFile) {
        this.matchingSummaryFile = new File(matchingSummarFile.toString())
        this.failureSummaryFile = new File(failureSummaryFile.toString())
    }

    @Override
    def writeMatchingChecksum(String expectedChecksum, String computedChecksum, URL fileLocation) {
        def content = "$expectedChecksum\t$computedChecksum\t${Paths.get(fileLocation.toURI())}\n"
        this.matchingSummaryFile.append(content, "UTF-8")
    }

    @Override
    def writeFailedChecksum(String expectedChecksum, String computedChecksum, URL fileLocation) {
        def content = "$expectedChecksum\t$computedChecksum\t${Paths.get(fileLocation.toURI())}\n"
        this.failureSummaryFile.append(content, "UTF-8")
    }
}
