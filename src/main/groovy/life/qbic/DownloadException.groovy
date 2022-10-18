package life.qbic

/**
 * Thrown, when a resource download attempt failed.
 *
 * This includes:
 *  - Stream interruption
 *  - Remote server exceptions
 *  - Local I/O exceptions
 *
 * @author Sven Fillinger
 * @since 0.4.0
 */
class DownloadException extends RuntimeException {

  DownloadException() {
    super()
  }

  DownloadException(String m) {
    super(m)
  }

}
