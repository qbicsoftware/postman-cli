package life.qbic.qpostman.openbis;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import java.util.Collection;
import java.util.List;

/**
 * Creates server instances given urls
 */
public class ServerFactory {
    public static Collection<IDataStoreServerApi> dataStoreServers(List<String> dataStoreServerUrls, long serverTimeoutInMillis) {
        return dataStoreServerUrls.stream()
                .filter(dataStoreServerUrl -> !dataStoreServerUrl.isEmpty())
                .map(dataStoreServerUrl -> HttpInvokerUtils.createStreamSupportingServiceStub(IDataStoreServerApi.class,
                        dataStoreServerUrl + IDataStoreServerApi.SERVICE_URL, serverTimeoutInMillis))
                .toList();
    }

    public static IApplicationServerApi applicationServer(String applicationServerUrl, long serverTimeoutInMillis) {
        return HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
                applicationServerUrl + IApplicationServerApi.SERVICE_URL, serverTimeoutInMillis);
    }
}
