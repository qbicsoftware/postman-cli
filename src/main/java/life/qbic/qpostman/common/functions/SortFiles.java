package life.qbic.qpostman.common.functions;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import life.qbic.qpostman.common.structures.DataFile;

public class SortFiles implements Function<Collection<DataFile>, List<DataFile>> {


  private final Comparator<DataFile> comparator = Comparator
      .comparing((DataFile dataFile) -> dataFile.dataSet().registrationTime())
      .reversed()
      .thenComparing(DataFile::filePath, String::compareToIgnoreCase);

  @Override
  public List<DataFile> apply(Collection<DataFile> dataFiles) {
    return dataFiles.stream()
        .sorted(comparator)
        .toList();
  }

  public Comparator<DataFile> comparator() {
    return comparator;
  }
}
