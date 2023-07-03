package life.qbic.qpostman.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import life.qbic.model.files.FileSize;
import life.qbic.model.files.FileSizeFormatter;


public class ProgressBar {

  private static final int TERMINAL_WIDTH = 80;
  private final int BARSIZE = TERMINAL_WIDTH / 3;
  private final int MAXFILENAMESIZE = TERMINAL_WIDTH / 3;
  private static final long UPDATE_INTERVAL = 1000;
  private float nextProgressJump;
  private final float stepSize;
  private final String fileName;
  private final Long totalFileSize;
  private Long downloadedSize;
  private final long start;
  private long lastUpdated;

  public ProgressBar(String fileName, long totalFileSize) {
    this.fileName = shortenFileName(fileName);
    this.totalFileSize = totalFileSize;
    this.downloadedSize = 0L;
    this.stepSize = (float) totalFileSize / (float) BARSIZE;
    this.nextProgressJump = this.stepSize;
    this.start = System.currentTimeMillis();
    lastUpdated = 0;
  }

  public void updateProgress(long addDownloadedSize) {
    this.downloadedSize += addDownloadedSize;
    update();
  }

  /**
   * Updates the progress bar if an update is applicable.
   */
  public void update() {
    if (progressStepsChanged()) {
      this.nextProgressJump += this.stepSize;
      drawProgress();
    }
    // update periodically
    if (isLastUpdateOutdated()) {
      drawProgress();
    }
  }

  private boolean progressStepsChanged() {
    return this.downloadedSize > this.nextProgressJump;
  }

  private boolean isLastUpdateOutdated() {
    long timePassedSinceLastUpdate = System.currentTimeMillis() - lastUpdated;
    return timePassedSinceLastUpdate >= UPDATE_INTERVAL;
  }

  private String shortenFileName(String fullFileName) {
    String shortName;
    if (fullFileName.length() > MAXFILENAMESIZE) {
      shortName = fullFileName.substring(0, MAXFILENAMESIZE - 3) + "...";
    } else {
      shortName = fullFileName;
    }
    return shortName;
  }

  private void drawProgress() {
    System.out.printf("\r%-" + computeLeftPadding() + "s %s", this.fileName, buildProgressBar());
    lastUpdated = System.currentTimeMillis();
  }

  public void remove() {
    System.out.printf("\r%"+TERMINAL_WIDTH+"s\r", ""); //clear whole line
  }

  private int computeLeftPadding() {
    return MAXFILENAMESIZE + 5;
  }

  private String buildProgressBar() {
    StringBuilder progressBar = new StringBuilder("[");
    int numberProgressStrings = Math.min((int) (this.downloadedSize / this.stepSize), BARSIZE);

    double elapsedTimeSeconds = (System.currentTimeMillis() - this.start) / 1000.0;

    // Download Speed in Mbyte/s
    double downloadSpeed = this.downloadedSize / (1000000.0 * elapsedTimeSeconds);

    // Estimate remaining download time
    long remainingDownloadTime = estimateRemainingTime(downloadSpeed * 1000000.0 / 1000.0);
    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String remainingTime = dateFormat.format(new Date(remainingDownloadTime));

    for (int i = 0; i < numberProgressStrings; i++) {
      progressBar.append("#");
    }
    for (int i = numberProgressStrings; i < BARSIZE; i++) {
      progressBar.append(" ");
    }

    progressBar.append("]\t");
    FileSize downloadedSize = FileSize.of(this.downloadedSize);
    FileSize totalSize = FileSize.of(totalFileSize);
    progressBar.append(
        String.format(
            "%s / %s [%s] (%.02f Mb/s)",
            FileSizeFormatter.format(downloadedSize, 6),
            FileSizeFormatter.format(totalSize),
            remainingTime,
            downloadSpeed));
    return progressBar.toString();
  }

  /**
   * Estimates the remaining download time in milliseconds.
   *
   * @param downloadSpeed The current download speed in bytes per second
   * @return The estimated remaining download time in milliseconds
   */
  private long estimateRemainingTime(double downloadSpeed) {
    long remainingFileSize = this.totalFileSize - this.downloadedSize;
    return (long) (remainingFileSize / downloadSpeed);
  }
}
