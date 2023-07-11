package life.qbic.qpostman.common;

/**
 * Exception to indicate failed authentication against openBIS.
 * <p>
 * This exception shall be thrown, when the returned session token of openBIS is empty, after the
 * client tried to authenticate against the openBIS application server via its Java API.
 */
public class AuthenticationException extends RuntimeException {

  private final String username;

  public AuthenticationException(String username) {
    this.username = username;
  }

  public AuthenticationException(String message, String username) {
    super(message);
    this.username = username;
  }

  public AuthenticationException(String message, Throwable cause, String username) {
    super(message, cause);
    this.username = username;
  }

  public AuthenticationException(Throwable cause, String username) {
    super(cause);
    this.username = username;
  }

  public AuthenticationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String username) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.username = username;
  }

  public String username() {
    return username;
  }
}
