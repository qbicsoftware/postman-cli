package life.qbic.model.download;

/**
 * ConnectionException indicates issues when a client wants to connect
 * with the openBIS API.
 */
public class ConnectionException extends RuntimeException{

    ConnectionException() {
        super();
    }

    ConnectionException(String msg) {
        super(msg);
    }

    ConnectionException(String msg, Throwable t) {
        super(msg, t);
    }

}
