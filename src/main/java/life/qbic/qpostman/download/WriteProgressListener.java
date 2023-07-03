package life.qbic.qpostman.download;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface WriteProgressListener {

  void update(long bytesWritten);

  void finish();
}
