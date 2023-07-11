package life.qbic.qpostman.openbis;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import java.util.Objects;
import life.qbic.qpostman.common.AuthenticationException;

public class OpenBisSession {
    private final String username;
    private final String password;
    private final IApplicationServerApi applicationServerApi;
    private String token;

    public OpenBisSession(IApplicationServerApi applicationServerApi, String username, String password) {
        this.applicationServerApi = applicationServerApi;
        this.username = username;
        this.password = password;
        token = login();
    }

    public void logout() {
        applicationServerApi.logout(token);
        token = null;
    }

    public boolean isLoggedIn() {
        return Objects.nonNull(token) && !token.isBlank() && applicationServerApi.isSessionActive(token);
    }

    public String getToken() {
        return isLoggedIn() ? token : login();
    }

    private String login() throws AuthenticationException {
        if (isLoggedIn()) {
            logout();
        }
        try {
            token = applicationServerApi.login(username, password);
        } catch (Exception e) {
            throw new ConnectionException("Connection to openBIS server failed.", e);
        }
        if (Objects.isNull(token)) {
            throw new AuthenticationException("openbis application server did not produce a session token for " + username, username);
        }
        return token;
    }

}
