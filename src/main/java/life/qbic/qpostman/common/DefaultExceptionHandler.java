package life.qbic.qpostman.common;

import java.nio.file.Path;
import java.util.Optional;
import life.qbic.qpostman.common.options.AuthenticationOptions.NoPasswordException;
import life.qbic.qpostman.common.options.SampleIdentifierOptions.IdentityFileEmptyException;
import life.qbic.qpostman.common.options.SampleIdentifierOptions.IdentityFileNotFoundException;
import life.qbic.qpostman.common.options.SampleIdentifierOptions.IdentityFileNotReadableException;
import life.qbic.qpostman.common.options.SampleIdentifierOptions.SampleInput.ToShortSampleIdsException;
import life.qbic.qpostman.openbis.ConnectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.remoting.RemoteAccessException;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

/**
 * The exception handler to use for exceptions within commands.
 */
public class DefaultExceptionHandler implements IExecutionExceptionHandler {
  private static final Logger log = LogManager.getLogger(DefaultExceptionHandler.class);
  private static final String LOG_PATH = Optional.ofNullable(System.getProperty("log.path"))
      .orElse("logs");

  private void logError(RuntimeException e) {
    if (e instanceof RemoteAccessException remoteAccessException) {
      log.error(
          "Failed to connect to OpenBis: " + remoteAccessException.getCause().getMessage());
      log.debug(remoteAccessException.getMessage(), remoteAccessException);
    } else if (e instanceof AuthenticationException authenticationException) {
      log.error(
          "Could not authenticate user %s. Please make sure to provide the correct password.".formatted(
              authenticationException.username()));
      log.debug(authenticationException.getMessage(), authenticationException);
    } else if (e instanceof ConnectionException) {
      log.error("Could not connect to QBiC's data source. Have you requested access to the "
          + "server? If not please write to support@qbic.zendesk.com");
      log.debug(e.getMessage(), e);
    } else if (e instanceof NoPasswordException) {
      log.error("No password provided. Please provide your password.");
    } else if (e instanceof IdentityFileEmptyException identityFileException) {
      log.error(identityFileException.getFile().getAbsolutePath() + " does not contain any identifiers.");
    } else if (e instanceof IdentityFileNotFoundException identityFileNotFoundException) {
      log.error("File not found: " + identityFileNotFoundException.getFile());
    } else if (e instanceof IdentityFileNotReadableException identityFileNotReadableException) {
      log.error("Not allowed to read file " + identityFileNotReadableException.getFile());
    } else if (e instanceof ToShortSampleIdsException toShortSampleIdsException) {
      log.error(
          "Please provide at least 5 letters for your sample identifiers. The following sample identifiers are to short: "
              + toShortSampleIdsException.getIdentifiers());
    } else {
      log.error("Something went wrong. For more detailed output see " + Path.of(LOG_PATH, "postman.log").toAbsolutePath());
      log.debug(e.getMessage(), e);
    }
  }

  @Override
  public int handleExecutionException(Exception e, CommandLine commandLine, ParseResult parseResult)
      throws Exception {
    logError((RuntimeException) e);
    commandLine.usage(commandLine. getErr());
    return commandLine.getCommandSpec().exitCodeOnExecutionException();
  }
}
