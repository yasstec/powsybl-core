package com.powsybl.afs.network.client.st;

import java.util.Objects;
import java.util.Optional;

import com.google.auto.service.AutoService;
import com.google.common.base.Supplier;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.afs.ext.base.NetworkCacheService;
import com.powsybl.afs.ws.client.utils.RemoteServiceConfig;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ServiceExtension.class)
public class RemoteNetworkCacheServiceExtensionSt implements ServiceExtension<NetworkCacheService> {

    private final Supplier<Optional<RemoteServiceConfig>> configSupplier;

    public RemoteNetworkCacheServiceExtensionSt() {
        this(RemoteServiceConfig::load);
    }

    public RemoteNetworkCacheServiceExtensionSt(Supplier<Optional<RemoteServiceConfig>> configSupplier) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
    }
    @Override
    public ServiceKey<NetworkCacheService> getServiceKey() {
        return new ServiceKey<>(NetworkCacheService.class, true);
    }

    @Override
    public NetworkCacheService createService(ServiceCreationContext context) {
        return new RemoteNetworkCacheServiceSt(configSupplier, context.getToken());
    }
}
