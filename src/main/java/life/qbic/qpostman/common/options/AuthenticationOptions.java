package life.qbic.qpostman.common.options;

import static java.util.Objects.nonNull;
import static picocli.CommandLine.ArgGroup;

import java.util.StringJoiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine.Option;

public class AuthenticationOptions {
    private static final Logger log = LogManager.getLogger(AuthenticationOptions.class);

    @Option(
            names = {"-u", "--user"},
            required = true,
            description = "openBIS user name")
    public String user;
    @ArgGroup(multiplicity = "1") // ensures the password is provided once with at least one of the possible options.
    PasswordOptions passwordOptions;

    public char[] getPassword() {
        return passwordOptions.getPassword();
    }

    /**
     * <a href="https://picocli.info/#_optionally_interactive">official picocli documentation example</a>
     */
    static class PasswordOptions {
        @Option(names = "--password:env", arity = "1", paramLabel = "<environment-variable>", description = "provide the name of an environment variable to read in your password from")
        protected String passwordEnvironmentVariable = "";

        @Option(names = "--password:prop", arity = "1", paramLabel = "<system-property>", description = "provide the name of a system property to read in your password from")
        protected String passwordProperty = "";

        @Option(names = "--password", arity = "0", description = "please provide your password", interactive = true)
        protected char[] password = null;

        /**
         * Gets the password. If no password is provided, the program exits.
         * @return the password provided by the user.
         */
        char[] getPassword() {
            if (nonNull(password)) {
                return password;
            }
            // System.getProperty(String key) does not work for empty or blank keys.
            if (!passwordProperty.isBlank()) {
                String systemProperty = System.getProperty(passwordProperty);
                if (nonNull(systemProperty)) {
                    return systemProperty.toCharArray();
                }
            }
            String environmentVariable = System.getenv(passwordEnvironmentVariable);
            if (nonNull(environmentVariable) && !environmentVariable.isBlank()) {
                return environmentVariable.toCharArray();
            }
            log.error("No password provided. Please provide your password.");
            System.exit(2);
            return null; // not reachable due to System.exit in previous line
        }

    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AuthenticationOptions.class.getSimpleName() + "[", "]")
                .add("user='" + user + "'")
                .toString();
        //ATTENTION: do not expose the password here!
    }
}
