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
}
