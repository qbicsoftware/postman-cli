package life.qbic.model.download;

/**
 * Exception to indicate failed authentication against openBIS.
 *
 * This exception shall be thrown, when the returned session token of
 * openBIS is empty, after the client tried to authenticate against
 * the openBIS application server via its Java API.
 */
public class AuthenticationException extends RuntimeException{

  AuthenticationException() {
    super();
  }

  AuthenticationException(String msg) {
    super(msg);
  }

  AuthenticationException(String msg, Throwable t) {
    super(msg, t);
  }

}
