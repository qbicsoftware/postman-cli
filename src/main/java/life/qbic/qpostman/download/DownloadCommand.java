package life.qbic.qpostman.download;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import life.qbic.qpostman.common.FileSizeFormatter;
import life.qbic.qpostman.common.functions.FileFilter;
import life.qbic.qpostman.common.functions.FindSourceSample;
import life.qbic.qpostman.common.functions.SearchDataSets;
import life.qbic.qpostman.common.functions.SearchFiles;
import life.qbic.qpostman.common.functions.SearchFiles.DataSetCounterProgressDisplay;
import life.qbic.qpostman.common.functions.SortFiles;
import life.qbic.qpostman.common.options.AuthenticationOptions;
import life.qbic.qpostman.common.options.FilterOptions;
import life.qbic.qpostman.common.options.SampleIdentifierOptions;
import life.qbic.qpostman.common.options.ServerOptions;
import life.qbic.qpostman.common.structures.DataFile;
import life.qbic.qpostman.common.structures.DataSetWrapper;
import life.qbic.qpostman.common.structures.FileSize;
import life.qbic.qpostman.download.WriteFileToDisk.DownloadReport;
import life.qbic.qpostman.openbis.OpenBisSessionProvider;
import life.qbic.qpostman.openbis.ServerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Command(name = "download",
        description = "Download data from QBiC.")
public class DownloadCommand implements Runnable {
    private static final Logger log = LogManager.getLogger(DownloadCommand.class);
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

            Functions functions = functions();

            Collection<DataFile> dataSetFiles = functions.searchDataSets()
                .andThen(it -> searchFiles(it).apply(it))
                .apply(sampleIdentifierOptions.getIds());

            List<DataFile> sortedFiles = dataSetFiles.stream()
                .filter(functions.fileFilter())
                .sorted(functions.sortFiles().comparator())
                .toList();

            log.info(
                "Downloading %s files (%s)".formatted(sortedFiles.size(), FileSizeFormatter.format(
                    FileSize.of(sortedFiles.stream().mapToLong(file -> file.fileSize().bytes()
                    ).sum()), 6)));
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
            List<DownloadReport> successfulDownloads = downloadReports.stream()
                .filter(DownloadReport::isSuccess).toList();
            List<DownloadReport> failedDownloads = downloadReports.stream()
                .filter(DownloadReport::isFailure).toList();
            log.info("Successfully downloaded " + successfulDownloads.size() +" files.");
            if (!failedDownloads.isEmpty()) {
                log.warn("Failed to download %s / %s files.".formatted(failedDownloads.size(), downloadReports.size()));
            }
    }

    private SearchFiles searchFiles(Collection<DataSetWrapper> it) {
        return new SearchFiles(dataStoreServerApis(), new DataSetCounterProgressDisplay(it.size()));
    }

    private Collection<IDataStoreServerApi> dataStoreServerApis() {
        return ServerFactory.dataStoreServers(serverOptions.dss_urls,
            serverOptions.timeoutInMillis);
    }

    private Functions functions() {
        IApplicationServerApi applicationServerApi = ServerFactory.applicationServer(serverOptions.as_url, serverOptions.timeoutInMillis);
        OpenBisSessionProvider.init(applicationServerApi, authenticationOptions.user, new String(authenticationOptions.getPassword()));
        SearchDataSets searchDataSets = new SearchDataSets(applicationServerApi);
        FileFilter myAwesomeFileFilter = FileFilter.create().withSuffixes(filterOptions.suffixes)
            .withPattern(filterOptions.pattern);
        WriteFileToDisk writeFileToDisk = new WriteFileToDisk(dataStoreServerApis().toArray(IDataStoreServerApi[]::new)[0],
            downloadOptions.bufferSize, Path.of(downloadOptions.outputPath), downloadOptions.successiveDownloadAttempts,
            downloadOptions.ignoreSubDirectories);
        FindSourceSample findSourceSample = new FindSourceSample(serverOptions.sourceSampleType);
        SortFiles sortFiles = new SortFiles();
        DataSetWrapper.setFindSourceFunction(findSourceSample);

        return new Functions(searchDataSets, writeFileToDisk, sortFiles, myAwesomeFileFilter);
    }

    private record Functions(SearchDataSets searchDataSets, WriteFileToDisk writeFileToDisk, SortFiles sortFiles, FileFilter fileFilter) {

    }
}
