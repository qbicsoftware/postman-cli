package life.qbic.qpostman.download;

import life.qbic.qpostman.common.ProgressBar;

/**
 * The DownloadProgressListener class provides methods to track the progress of a file download.
 *
 * It uses a ProgressBar object to display the download progress.
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
