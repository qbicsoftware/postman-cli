package life.qbic.qpostman.download;

import life.qbic.qpostman.common.ProgressBar;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class DownloadProgressListener implements WriteProgressListener {

  private final ProgressBar progressBar;

  public DownloadProgressListener(String fileName, long totalFileSize) {
    progressBar = new ProgressBar(fileName, totalFileSize);
  }

  @Override
  public void update(long bytesWritten) {
    progressBar.updateProgress(bytesWritten);
  }

  @Override
  public void finish() {
    progressBar.remove();
  }
}
