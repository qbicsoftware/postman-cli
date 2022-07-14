package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;

public class Authentication {

    private static String sessionToken;
    private String user;
    private String password;
    private final IApplicationServerApi applicationServer;


    public Authentication(
            String user,
            String password,
            String AppServerUri) {
        this.user = user;
        this.password = password;
        if (!AppServerUri.isEmpty()) {
            this.applicationServer =
                    HttpInvokerUtils.createServiceStub(
                            IApplicationServerApi.class, AppServerUri + IApplicationServerApi.SERVICE_URL, 10000);
        } else {
            this.applicationServer = null;
        }
        this.setCredentials(user, password);
    }

    /**
     * Login method for openBIS authentication
     *
     * returns 0 if successful, 1 else
     */
    public void login() throws ConnectionException, AuthenticationException {
        try {
            sessionToken = applicationServer.login(user, password);
        } catch (Exception e) {
            throw new ConnectionException("Connection to openBIS server failed.");
        }
        if (sessionToken == null || sessionToken.isEmpty()) {
            throw new AuthenticationException("Authentication failed. Are you using the correct "
                    + "credentials for http://qbic.life?");
        }
    }

    public String getSessionToken() {
        return sessionToken;
    }


    /**
     * Setter for user and password credentials
     *
     * @param user     The openBIS user
     * @param password The openBIS user's password
     */
    public void setCredentials(String user, String password) {
        this.user = user;
        this.password = password;
    }
}
