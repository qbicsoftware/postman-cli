package life.qbic.qpostman.list;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import life.qbic.qpostman.common.FileSizeFormatter;
import life.qbic.qpostman.common.structures.DataFile;

/**
 * The DataFileTableFormatter class is responsible for formatting a list of DataFiles
 * into a table format using specified delimiter and column settings.
 */
public class DataFileTableFormatter {

  private final List<Column<String>> columns;

  public DataFileTableFormatter(boolean exactFileSize, boolean withChecksum) {
    columns = new ArrayList<>();

    columns.add(Column.create("Dataset",
            file -> file.dataSet().sampleCode() + " (" + file.dataSet().dataSetPermId().getPermId()
                + ")"));
    columns.add(Column.create("Source", file -> file.dataSet().sourceSample().getCode()));
    columns.add(Column.create("Registration", file -> file.dataSet().registrationTime().toString()));
    columns.add(Column.create("Size", file -> exactFileSize
            ? String.valueOf(file.fileSize().bytes())
            : FileSizeFormatter.format(file.fileSize(), 6)));
    if (withChecksum) {
      columns.add(Column.create("CRC32", file -> Long.toHexString(file.crc32())));
    };
    columns.add(Column.create("File", DataFile::filePath));
  }

  public String formatAsTable(List<DataFile> files, String delimiter, boolean withHeader) {
    StringBuilder result = new StringBuilder();
    if (withHeader) {
      List<String> columnNames = columns.stream()
          .map(Column::name)
          .toList();
      String headerRow = String.join(delimiter, columnNames) + "\n";
      result.append(headerRow);
    }
    files.stream()
        .map(file -> toRow(file, delimiter))
        .forEach(result::append);
    return result.toString();
  }

  private String toRow(DataFile file, String delimiter) {
    List<String> values = columns.stream().map(c -> c.toValue(file)).toList();
    return String.join(delimiter, values) + "\n";
  }

  private record Column<T>(String name, Function<DataFile, T> valueProvider) {

    Column {
      requireNonNull(valueProvider, "valueProvider must not be null");
      if (isNull(name)) {
        name = "";
      }
    }

    public static Column<String> create(String name, Function<DataFile, String> valueProvider) {
      return new Column<>(name, valueProvider);
    }

    public T toValue(DataFile dataFile) {
      return valueProvider.apply(dataFile);
    }
  }
}
