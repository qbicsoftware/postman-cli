package life.qbic;

import java.io.IOException;
import java.util.List;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import life.qbic.dataLoading.QbicDataFinder;
import life.qbic.dataLoading.QbicDataLoader;
import life.qbic.io.commandline.CommandLineParser;
import life.qbic.io.commandline.CommandLineVerification;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;


/**
 * postman for staging data from openBIS
 */
public class App {

    static String AS_URL;
    static String DSS_URL;
    private final static Logger LOG = LogManager.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        PostmanCommandLineOptions commandLineParameters = CommandLineVerification.verifyCommandLineParameters(args);

        // Set the server URLS specified by a config file/CLI argument -> use default if none is provided
        AS_URL = commandLineParameters.as_url;
        DSS_URL = commandLineParameters.dss_url;

        QbicDataLoader qbicDataLoader = loginToOpenBIS(commandLineParameters);

        downloadRequestedFilesOfDatasets(commandLineParameters, qbicDataLoader);
    }

    /**
     * Logs into OpenBIS
     * asks for and verifies password
     *
     * @param commandLineParameters
     * @return
     */
    private static QbicDataLoader loginToOpenBIS(PostmanCommandLineOptions commandLineParameters) {
        System.out.format("Please provide password for user \'%s\':\n", commandLineParameters.user);

        String password = CommandLineParser.readPasswordFromInputStream();

        if (password.isEmpty()) {
            System.out.println("You need to provide a password.");
            System.exit(1);
        }

        QbicDataLoader qbicDataLoader = new QbicDataLoader(AS_URL, DSS_URL, commandLineParameters.user, password,
                commandLineParameters.bufferMultiplier * 1024, commandLineParameters.datasetType);
        int returnCode = qbicDataLoader.login();
        LOG.info(String.format("OpenBis login returned with %s", returnCode));
        if (returnCode != 0) {
            LOG.error("Connection to openBIS failed.");
            System.exit(1);
        }
        LOG.info("Connection to openBIS was successful.");
        return qbicDataLoader;
    }

    /**
     * Downloads the files that the user requested
     * checks whether any filtering option (suffix or regex) has been passed and applies filtering if needed
     *
     * @param commandLineParameters
     * @param qbicDataLoader
     * @throws IOException
     */
    private static void downloadRequestedFilesOfDatasets(PostmanCommandLineOptions commandLineParameters, QbicDataLoader qbicDataLoader) throws IOException {
        QbicDataFinder qbicDataFinder = new QbicDataFinder(qbicDataLoader.getApplicationServer(),
                                                           qbicDataLoader.getDataStoreServer(),
                                                           qbicDataLoader.getSessionToken(),
                                                           qbicDataLoader.getFilterType());

        LOG.info(String.format("%s provided openBIS identifiers have been found: %s",
                commandLineParameters.ids.size(), commandLineParameters.ids.toString()));

        // a suffix was provided -> only download files which contain the suffix string
        if (!commandLineParameters.suffixes.isEmpty()) {
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<IDataSetFileId> foundSuffixFilteredIDs = qbicDataFinder.findAllSuffixFilteredIDs(ident, commandLineParameters.suffixes);

                LOG.info(String.format("Number of files found: %s", foundSuffixFilteredIDs.size()));

                downloadFilteredIDs(qbicDataLoader, ident, foundSuffixFilteredIDs);
            }
            // a regex pattern was provided -> only download files which contain the regex pattern
        } else if (!commandLineParameters.regexPatterns.isEmpty()) {
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<IDataSetFileId> foundRegexFilteredIDs = qbicDataFinder.findAllRegexFilteredIDs(ident, commandLineParameters.regexPatterns);

                LOG.info(String.format("Number of files found: %s", foundRegexFilteredIDs.size()));

                downloadFilteredIDs(qbicDataLoader, ident, foundRegexFilteredIDs);
            }
        } else {
            // no suffix or regex was supplied -> download all datasets
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<DataSet> foundDataSets = qbicDataFinder.findAllDatasetsRecursive(ident);

                LOG.info(String.format("Number of data sets found: %s", foundDataSets.size()));

                if (foundDataSets.size() > 0) {
                    LOG.info("Initialize download ...");
                    int datasetDownloadReturnCode = -1;
                    try {
                        datasetDownloadReturnCode = qbicDataLoader.downloadDataset(foundDataSets);
                    } catch (NullPointerException e) {
                        LOG.error("Datasets were found by the application server, but could not be found on the datastore server for "
                                + ident + "." + " Try to supply the correct datastore server using a config file!");
                    }

                    if (datasetDownloadReturnCode != 0) {
                        LOG.error("Error while downloading dataset: " + ident);
                    } else {
                        LOG.info("Download successfully finished.");
                    }

                } else {
                    LOG.info("Nothing to download.");
                }
            }
        }
    }


    /**
     * downloads all IDs which were previously filtered by either suffixes or regexes
     *
     * @param qbicDataLoader
     * @param ident
     * @param foundFilteredIDs
     * @throws IOException
     */
    private static void downloadFilteredIDs(QbicDataLoader qbicDataLoader, String ident, List<IDataSetFileId> foundFilteredIDs) throws IOException {
        if (foundFilteredIDs.size() > 0) {
            LOG.info("Initialize download ...");
            int filesDownloadReturnCode = -1;
            try {
                filesDownloadReturnCode = qbicDataLoader.downloadFilesByID(foundFilteredIDs);
            } catch (NullPointerException e) {
                LOG.error("Datasets were found by the application server, but could not be found on the datastore server for "
                        + ident + "." + " Try to supply the correct datastore server using a config file!");
            }
            if (filesDownloadReturnCode != 0) {
                LOG.error("Error while downloading dataset: " + ident);
            } else {
                LOG.info("Download successfully finished");
            }

        } else {
            LOG.info("Nothing to download.");
        }
    }

}


