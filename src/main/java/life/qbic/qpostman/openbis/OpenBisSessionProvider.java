package life.qbic.qpostman.openbis;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import java.util.Objects;

public class OpenBisSessionProvider {

    private static OpenBisSession openBisSession;

    public static OpenBisSession init(IApplicationServerApi applicationServerApi, String username, String password) {
        openBisSession = new OpenBisSession(applicationServerApi, username, password);
        return openBisSession;
    }

    public static OpenBisSession get() {
        if (Objects.isNull(openBisSession)) {
            throw new RuntimeException("Session not initialized");
        }
        return openBisSession;
    }
}
