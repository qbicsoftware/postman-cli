package life.qbic.qpostman.common.options;

import static picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ServerOptions {

    @Option(names = {"--application-server"},
        paramLabel = "url",
        description = "set the application server to find samples and datasets",
        hidden = true)
    public String as_url = "https://qbis.qbic.uni-tuebingen.de/openbis/openbis";

    @Option(names = {"--datastore-server"},
        paramLabel = "url",
        description = "add a data store server to find files on",
        hidden = true)
    public List<String> dss_urls = new ArrayList<>(List.of(
        "https://qbis.qbic.uni-tuebingen.de/datastore_server",
        "https://qbis.qbic.uni-tuebingen.de/datastore_server2",
        "https://qbis.qbic.uni-tuebingen.de/datastore_server_attachments"
    ));


    @Option(names = {"--source-sample-type"},
        paramLabel = "Q_SAMPLE_TYPE",
        description = "the sample type in openBis considered to be the source sample",
        hidden = true)
    public String sourceSampleType = "Q_TEST_SAMPLE";

    @Option(names = {"--server-timeout"},
        paramLabel = "milliseconds",
        description = "the server timeout in milliseconds",
        hidden = true)
    public long timeoutInMillis = 10_000;

    @Override
    public String toString() {

        return new StringJoiner(", ", ServerOptions.class.getSimpleName() + "[", "]")
            .add("as_url='" + as_url + "'")
            .add("dss_urls=" + dss_urls)
            .toString();
    }
}
