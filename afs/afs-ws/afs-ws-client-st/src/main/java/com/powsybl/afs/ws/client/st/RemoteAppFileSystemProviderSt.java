package com.powsybl.afs.ws.client.st;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;
import com.google.common.base.Supplier;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.AppFileSystemProvider;
import com.powsybl.afs.AppFileSystemProviderContext;
import com.powsybl.afs.ws.client.utils.RemoteServiceConfig;
import com.powsybl.afs.ws.storage.st.RemoteAppStorageSt;
import com.powsybl.afs.ws.storage.st.RemoteListenableAppStorageSt;
import com.powsybl.afs.ws.storage.st.RemoteTaskMonitorSt;

@AutoService(AppFileSystemProvider.class)
public class RemoteAppFileSystemProviderSt implements AppFileSystemProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAppFileSystemProviderSt.class);

    private final Supplier<Optional<RemoteServiceConfig>> configSupplier;

    public RemoteAppFileSystemProviderSt() {
        this(RemoteServiceConfig::load);
    }

    public RemoteAppFileSystemProviderSt(Supplier<Optional<RemoteServiceConfig>> configSupplier) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
    }

    @Override
    public List<AppFileSystem> getFileSystems(AppFileSystemProviderContext context) {
        Objects.requireNonNull(context);
        Optional<RemoteServiceConfig> config = configSupplier.get();
        if (config.isPresent()) {
            URI uri = config.get().getRestUri();
            try {
                return RemoteAppStorageSt.getFileSystemNames(uri, context.getToken()).stream()
                        .map(fileSystemName -> {
                            RemoteAppStorageSt storage = new RemoteAppStorageSt(fileSystemName, uri, context.getToken());
                            RemoteListenableAppStorageSt listenableStorage = new RemoteListenableAppStorageSt(storage, uri);
                            RemoteTaskMonitorSt taskMonitor = new RemoteTaskMonitorSt(fileSystemName, uri, context.getToken());
                            return new AppFileSystem(fileSystemName, true, listenableStorage, taskMonitor);
                        })
                        .collect(Collectors.toList());
            } catch (ProcessingException e) {
                LOGGER.warn(e.toString());
                return Collections.emptyList();
            }
        } else {
            LOGGER.warn("Remote service config is missing");
            return Collections.emptyList();
        }
    }
}
