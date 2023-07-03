package life.qbic.qpostman.list;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import life.qbic.qpostman.common.AuthenticationException;
import life.qbic.qpostman.common.functions.FileFilter;
import life.qbic.qpostman.common.functions.FindSourceSample;
import life.qbic.qpostman.common.functions.SearchDataSets;
import life.qbic.qpostman.common.functions.SearchFiles;
import life.qbic.qpostman.common.functions.SortFiles;
import life.qbic.qpostman.common.options.AuthenticationOptions;
import life.qbic.qpostman.common.options.FilterOptions;
import life.qbic.qpostman.common.options.SampleIdentifierOptions;
import life.qbic.qpostman.common.options.ServerOptions;
import life.qbic.qpostman.common.structures.DataFile;
import life.qbic.qpostman.common.structures.DataSetWrapper;
import life.qbic.qpostman.openbis.ConnectionException;
import life.qbic.qpostman.openbis.OpenBisSessionProvider;
import life.qbic.qpostman.openbis.ServerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.remoting.RemoteAccessException;

@Command(name = "list",
        description = "lists all the datasets found for the given identifiers")
public class ListCommand implements Runnable {
    private static final Logger log = LogManager.getLogger(ListCommand.class);
    private static final String LOG_PATH = Optional.ofNullable(System.getProperty("log.path"))
        .orElse("logs");
    @Mixin
    AuthenticationOptions authenticationOptions;
    @Mixin
    SampleIdentifierOptions sampleIdentifierOptions;
    @Mixin
    FilterOptions filterOptions;
    @Mixin
    ServerOptions serverOptions;
    @Mixin
    ListOptions listOptions;

    @Override
    public void run() {
        try {
            Functions functions = setupFunctions();

            Collection<DataFile> dataSetFiles = functions.searchDataSets()
                .andThen(functions.searchFiles())
                .apply(sampleIdentifierOptions.getIds());

            List<DataFile> processedFiles = dataSetFiles.stream()
                .filter(functions.fileFilter())
                .sorted(functions.sortFiles().comparator())
                .toList();

            boolean withHeader = !listOptions.withoutHeader;
            String tsvContent = functions.dataFileFormatter()
                .formatAsTable(processedFiles, "\t", withHeader);
            log.info("Output:\n" + tsvContent);
        } catch (RemoteAccessException remoteAccessException) {
            log.error(
                "Failed to connect to OpenBis: " + remoteAccessException.getCause().getMessage());
            log.debug(remoteAccessException.getMessage(), remoteAccessException);
            System.exit(1);
        } catch (AuthenticationException authenticationException) {
            log.error(
                "Could not authenticate user %s. Please make sure to provide the correct password.".formatted(
                    authenticationException.username()));
            log.debug(authenticationException.getMessage(), authenticationException);
            System.exit(1);
        } catch (ConnectionException e) {
          log.error("Could not connect to QBiC's data source. Have you requested access to the "
              + "server? If not please write to support@qbic.zendesk.com");
            log.debug(e.getMessage(), e);
            System.exit(1);
        } catch (RuntimeException e) {
            log.error("Something went wrong. For more detailed output see " + Path.of(LOG_PATH,
                "postman.log").toAbsolutePath());
            log.debug(e.getMessage(), e);
        }
    }

    private Functions setupFunctions() {
        IApplicationServerApi applicationServerApi = ServerFactory.applicationServer(
            serverOptions.as_url, serverOptions.timeoutInMillis);
        OpenBisSessionProvider.init(applicationServerApi, authenticationOptions.user,
            new String(authenticationOptions.getPassword()));
        Collection<IDataStoreServerApi> dataStoreServerApis = ServerFactory.dataStoreServers(
            serverOptions.dss_urls, serverOptions.timeoutInMillis);
        SearchDataSets searchDataSets = new SearchDataSets(applicationServerApi);
        FileFilter myAwesomeFileFilter = FileFilter.create()
            .withSuffixes(filterOptions.suffixes);
        SearchFiles searchFiles = new SearchFiles(applicationServerApi, dataStoreServerApis);
        FindSourceSample findSourceSample = new FindSourceSample(serverOptions.sourceSampleType);
        DataFileFormatter dataFileFormatter = new DataFileFormatter(listOptions.exactFilesize);
        SortFiles sortFiles = new SortFiles();

        DataSetWrapper.setFindSourceFunction(findSourceSample);
      return new Functions(searchDataSets, myAwesomeFileFilter, searchFiles,
            dataFileFormatter, sortFiles);
    }

    private record Functions(SearchDataSets searchDataSets, FileFilter fileFilter, SearchFiles searchFiles, DataFileFormatter dataFileFormatter, SortFiles sortFiles) {

    }
}
