package life.qbic.qpostman.common;

import static picocli.CommandLine.Command;

import life.qbic.qpostman.download.DownloadCommand;
import life.qbic.qpostman.list.ListCommand;
import picocli.CommandLine;

@Command(name = "postman-cli",
    descriptionHeading = "%nDescription:%n",
    description = "A software client for downloading data from QBiC.",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    commandListHeading = "%nCommands:%n",
    synopsisSubcommandLabel = "COMMAND",
    footer = "Optional: specify a config file by running postman with '@/path/to/config.txt'.\nA detailed documentation can be found at \nhttps://github.com/qbicsoftware/postman-cli#readme.",
    footerHeading = "%n",
    mixinStandardHelpOptions = true,
    scope = CommandLine.ScopeType.INHERIT,
    sortOptions = false,
    usageHelpAutoWidth = true,
    versionProvider = ManifestVersionProvider.class,
    subcommands = {DownloadCommand.class, ListCommand.class})
public class PostmanCommand {
}
