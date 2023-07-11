package life.qbic.qpostman.list;

import static java.util.Objects.requireNonNull;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import java.util.List;
import life.qbic.qpostman.common.FileSizeFormatter;
import life.qbic.qpostman.common.structures.DataFile;
import life.qbic.qpostman.common.structures.DataSetWrapper;
import life.qbic.qpostman.common.structures.FileSize;

/**
 * Formats a list of data files in legacy format to support backwards compatibility.
 */
public class LegacyOutputFormatter {

  public String format(DataSetSummary dataSetSummary, boolean exactFileSize) {
    String summaryOutput = """
        # Dataset         %s
        # Source          %s
        # Registration    %s
        # Size            %s
        """.formatted(
        dataSetSummary.datasetName(),
        dataSetSummary.sourceSampleCode(),
        dataSetSummary.dataSet().registrationTime(),
        exactFileSize ? dataSetSummary.totalSize().bytes() : FileSizeFormatter.format(dataSetSummary.totalSize()
        ));
    StringBuilder result = new StringBuilder(summaryOutput);
    for (DataFile datafile : dataSetSummary.datafiles()) {
      String fileOutput = "%s\t%s\t%s".formatted(
          exactFileSize ? datafile.fileSize().bytes() : FileSizeFormatter.format(datafile.fileSize()),
          Long.toHexString(datafile.crc32()),
          datafile.fileName());
      result.append(fileOutput);
      result.append("\n");
    }
    return result.toString();
  }

  public record DataSetSummary(List<DataFile> datafiles) {

    public DataSetSummary {
      requireNonNull(datafiles, "dataSetFiles must not be null");
      if (datafiles.isEmpty()) {
        throw new IllegalArgumentException("No data files provided.");
      }
      DataSetPermId dataSetPermId = datafiles.get(0).dataSet().dataSetPermId();
      if (datafiles.stream().anyMatch(file -> !file.dataSet().dataSetPermId().equals(dataSetPermId))) {
        throw new IllegalArgumentException("Not all files are of the same dataset.");
      }
    }

    private DataSetWrapper dataSet() {
      return datafiles.get(0).dataSet();
    }

    public String sourceSampleCode() {
      return dataSet().sourceSample().getCode();
    }

    public String datasetName() {
      DataSetWrapper dataSet = dataSet();
      return "%s (%s)".formatted(dataSet.sampleCode(), dataSet.dataSetPermId().getPermId());
    }

    public FileSize totalSize() {
      return datafiles.stream()
          .map(DataFile::fileSize)
          .reduce((size, size2) -> FileSize.add(size, size2))
          .orElse(FileSize.of(0));
    }



  }

}
