package life.qbic.qpostman.common.structures;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public final class FileSize {
  private final long bytes;

  private FileSize(long bytes) {
    this.bytes = bytes;
  }

  public static FileSize of(long bytes) {
    return new FileSize(bytes);
  }

  public long bytes() {
    return bytes;
  }

  public FileSize add(FileSize other) {
    return add(this, other);
  }

  public static FileSize add(FileSize size1, FileSize size2) {
    return FileSize.of(size1.bytes() + size2.bytes());
  }
}
