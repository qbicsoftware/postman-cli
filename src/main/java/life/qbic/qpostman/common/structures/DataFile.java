package life.qbic.qpostman.common.structures;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public final class DataFile {

    private final DataSetFile file;
    private final DataSetWrapper dataSet;

    public DataFile(DataSetFile dataSetFile, DataSetWrapper dataSet) {
        this.file = dataSetFile;
        this.dataSet = dataSet;
    }

    public DataSetFilePermId dataSetFilePermId() {
        return file.getPermId();
    }

    public FileSize fileSize() {
        return FileSize.of(file.getFileLength());
    }

    public String filePath() {
        return file.getPath().replaceFirst("original/", "");
    }

    public DataSetFilePermId fileId() {
        return file.getPermId();
    }

    public String fileName() {
        return filePath().substring(filePath().lastIndexOf("/") + 1);
    }

    public DataSetWrapper dataSet() {
        return dataSet;
    }

    public long crc32() {
        return Integer.toUnsignedLong(file.getChecksumCRC32());
    }
}
