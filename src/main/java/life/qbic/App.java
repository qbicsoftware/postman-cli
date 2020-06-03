package life.qbic;

import java.io.IOException;
import java.util.List;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import life.qbic.io.commandline.Argparser;
import life.qbic.io.commandline.PostmanCommandLineOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import picocli.CommandLine;


/**
 * postman for staging data from openBIS
 */
public class App {

    static String AS_URL;
    static String DSS_URL;
    private final static Logger LOG = LogManager.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            CommandLine.usage(new PostmanCommandLineOptions(), System.out);
            System.exit(0);
        }

        PostmanCommandLineOptions commandLineParameters = new PostmanCommandLineOptions();
        new CommandLine(commandLineParameters).parse(args);

        if (commandLineParameters.helpRequested) {
            CommandLine.usage(new PostmanCommandLineOptions(), System.out);
            System.exit(0);
        }

        if ((commandLineParameters.ids == null || commandLineParameters.ids.isEmpty()) && commandLineParameters.filePath == null) {
            System.out.println("You have to provide one ID as command line argument or a file containing IDs.");
            System.exit(1);
        } else if ((commandLineParameters.ids != null) && (commandLineParameters.filePath != null)) {
            System.out.println("Arguments --identifier and --file are mutually exclusive, please provide only one.");
            System.exit(1);
        } else if (commandLineParameters.filePath != null) {
            commandLineParameters.ids = Argparser.readProvidedIdentifiers(commandLineParameters.filePath.toFile());
        }

        // Set the server URLS specified by a config file/CLI argument -> use default if none is provided
        AS_URL = commandLineParameters.as_url;
        DSS_URL = commandLineParameters.dss_url;

        System.out.format("Please provide password for user \'%s\':\n", commandLineParameters.user);

        String password = Argparser.readPasswordFromInputStream();

        if (password.isEmpty()) {
            System.out.println("You need to provide a password.");
            System.exit(1);
        }

        QbicDataLoader qbicDataLoader = new QbicDataLoader(AS_URL, DSS_URL, commandLineParameters.user, password,
                commandLineParameters.bufferMultiplier * 1024, commandLineParameters.datasetType,
            commandLineParameters.conservePath);
        int returnCode = qbicDataLoader.login();
        LOG.info(String.format("OpenBis login returned with %s", returnCode));
        if (returnCode != 0) {
            LOG.error("Connection to openBIS failed.");
            System.exit(1);
        }
        LOG.info("Connection to openBIS was successful.");

        LOG.info(String.format("%s provided openBIS identifiers have been found: %s",
                commandLineParameters.ids.size(), commandLineParameters.ids.toString()));

        // a suffix was provided -> only download files which contain the suffix string
        if (!commandLineParameters.suffixes.isEmpty()) {
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<IDataSetFileId> foundSuffixFilteredIDs = qbicDataLoader.findAllSuffixFilteredIDs(ident, commandLineParameters.suffixes);

                LOG.info(String.format("Number of files found: %s", foundSuffixFilteredIDs.size()));

                downloadFilteredIDs(qbicDataLoader, ident, foundSuffixFilteredIDs);
            }
            // a regex pattern was provided -> only download files which contain the regex pattern
        } else if (!commandLineParameters.regexPatterns.isEmpty()) {
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<IDataSetFileId> foundRegexFilteredIDs = qbicDataLoader.findAllRegexFilteredIDs(ident, commandLineParameters.regexPatterns);

                LOG.info(String.format("Number of files found: %s", foundRegexFilteredIDs.size()));

                downloadFilteredIDs(qbicDataLoader, ident, foundRegexFilteredIDs);
            }
        } else {
            // no suffix or regex was supplied -> download all datasets
            for (String ident : commandLineParameters.ids) {
                LOG.info(String.format("Downloading files for provided identifier %s", ident));
                List<DataSet> foundDataSets = qbicDataLoader.findAllDatasetsRecursive(ident);

                LOG.info(String.format("Number of data sets found: %s", foundDataSets.size()));

                if (foundDataSets.size() > 0) {
                    LOG.info("Initialize download ...");
                    int datasetDownloadReturnCode = -1;
                    try {
                        datasetDownloadReturnCode = qbicDataLoader.downloadDataset(foundDataSets);
                    } catch (NullPointerException e) {
                        LOG.error("Datasets were found by the application server, but could not be found on the datastore server for "
                                + ident + "." + " Try to supply the correct datastore server using a config file!");
                        e.printStackTrace();
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


