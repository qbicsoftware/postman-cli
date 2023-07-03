package life.qbic.qpostman.download;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import life.qbic.qpostman.common.AuthenticationException;
import life.qbic.qpostman.common.FileSizeFormatter;
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
import life.qbic.qpostman.common.structures.FileSize;
import life.qbic.qpostman.download.WriteFileToDisk.DownloadReport;
import life.qbic.qpostman.openbis.ConnectionException;
import life.qbic.qpostman.openbis.OpenBisSessionProvider;
import life.qbic.qpostman.openbis.ServerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.remoting.RemoteAccessException;

@Command(name = "download",
        description = "Download data from QBiC.")
public class DownloadCommand implements Runnable {
    private static final Logger log = LogManager.getLogger(DownloadCommand.class);
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
    DownloadOptions downloadOptions;

    @Override
    public void run() {
        try {
            Functions functions = functions();

            Collection<DataFile> dataSetFiles = functions.searchDataSets()
                .andThen(functions.searchFiles())
                .apply(sampleIdentifierOptions.getIds());

            List<DataFile> sortedFiles = dataSetFiles.stream()
                .filter(functions.fileFilter())
                .sorted(functions.sortFiles().comparator())
                .toList();

            log.info(
                "Downloading %s files (%s)".formatted(sortedFiles.size(), FileSizeFormatter.format(
                    FileSize.of(sortedFiles.stream().mapToLong(file -> file.fileSize().bytes()
                    ).sum())), 6));
            int counter = 0;
            List<DownloadReport> downloadReports = sortedFiles.stream()
                .map(functions.writeFileToDisk())
                .peek(downloadReport -> {
                    if (downloadReport.isSuccess()) {
                        log.info("Download successful for " + downloadReport.outputPath());
                    } else {
                        log.warn("Failed to download " + downloadReport.outputPath());
                    }
                })
                .toList();


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
        }  catch (ConnectionException e) {
            log.error("Could not connect to QBiC's data source. Have you requested access to the "
                + "server? If not please write to support@qbic.zendesk.com");
            log.debug(e.getMessage(), e);
            System.exit(1);
        } catch (RuntimeException e) {
            log.error("Something went wrong. For more detailed output see " + Path.of(LOG_PATH, "postman.log").toAbsolutePath());
            log.debug(e.getMessage(), e);
        }
    }

    private Functions functions() {
        IApplicationServerApi applicationServerApi = ServerFactory.applicationServer(serverOptions.as_url, serverOptions.timeoutInMillis);
        OpenBisSessionProvider.init(applicationServerApi, authenticationOptions.user, new String(authenticationOptions.getPassword()));
        SearchDataSets searchDataSets = new SearchDataSets(applicationServerApi);
        Collection<IDataStoreServerApi> dataStoreServerApis = ServerFactory.dataStoreServers(serverOptions.dss_urls, serverOptions.timeoutInMillis);
        SearchFiles searchFiles = new SearchFiles(applicationServerApi, dataStoreServerApis);
        FileFilter myAwesomeFileFilter = FileFilter.create().withSuffixes(filterOptions.suffixes);
        WriteFileToDisk writeFileToDisk = new WriteFileToDisk(dataStoreServerApis.toArray(IDataStoreServerApi[]::new)[0],
            downloadOptions.bufferSize, Path.of(downloadOptions.outputPath), downloadOptions.successiveDownloadAttempts);
        FindSourceSample findSourceSample = new FindSourceSample("Q_TEST_SAMPLE");
        SortFiles sortFiles = new SortFiles();
        DataSetWrapper.setFindSourceFunction(findSourceSample);

        Functions functions = new Functions(searchDataSets, searchFiles, writeFileToDisk, sortFiles,
            myAwesomeFileFilter);
        return functions;
    }

    private record Functions(SearchDataSets searchDataSets, SearchFiles searchFiles, WriteFileToDisk writeFileToDisk, SortFiles sortFiles, FileFilter fileFilter) {

    }
}
