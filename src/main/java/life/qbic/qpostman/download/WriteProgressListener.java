package life.qbic.qpostman.download;

/**
 * The WriteProgressListener interface provides methods for receiving updates on the progress
 * of a write operation.
 */
public interface WriteProgressListener {

  /**
   * Updates the number of bytes written.
   *
   * @param bytesWritten the number of bytes written to be updated
   */
  void update(long bytesWritten);

  /**
   * Finishes the operation.
   * <p>
   * This method restores the original state of the writer.
   * </p>
   */
  void finish();
}
