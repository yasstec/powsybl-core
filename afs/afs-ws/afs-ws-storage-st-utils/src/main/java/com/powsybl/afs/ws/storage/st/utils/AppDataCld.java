package com.powsybl.afs.ws.storage.st.utils;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.powsybl.afs.AppData;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.ws.storage.st.RemoteAppStorageSt;
import com.powsybl.afs.ws.storage.st.RemoteListenableAppStorageSt;
import com.powsybl.afs.ws.storage.st.RemoteTaskMonitorSt;
import com.powsybl.computation.ComputationManager;

public class AppDataCld extends AppData {
    public AppDataCld(ComputationManager shortTimeExecutionComputationManager,
            ComputationManager longTimeExecutionComputationManager) {
        super(shortTimeExecutionComputationManager, longTimeExecutionComputationManager);
    }
    private URI uri;
    public void setUri(URI uri) {
        this.uri = uri;
    }
    private void loadFileSystems() {
        if (fileSystems == null) {
            fileSystems = new HashMap<>();
            for (AppFileSystem fileSystem : getFileSystems()) {
                fileSystem.setData(this);
                fileSystems.put(fileSystem.getName(), fileSystem);
            }
        }
    }
    public List<AppFileSystem> getFileSystems() {
        try {
            return getFileSystemNames("").stream()
                        .map(fileSystemName -> {
                            RemoteAppStorageSt storage = new RemoteAppStorageSt(fileSystemName, uri, "");
                            RemoteListenableAppStorageSt listenableStorage = new RemoteListenableAppStorageSt(storage, uri);
                            RemoteTaskMonitorSt taskMonitor = new RemoteTaskMonitorSt(fileSystemName, uri, "");
                            return new AppFileSystem(fileSystemName, true, listenableStorage, taskMonitor);
                        })
                        .collect(Collectors.toList());
        } catch (ProcessingException e) {
            return Collections.emptyList();
        }
    }
    public List<String> getFileSystemNames(String token) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<String>> response = restTemplate.exchange(
                        uri + "rest/afs/v1/fileSystems",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<String>>() { });
        List<String> lstr = response.getBody();
        return lstr;
    }
    @Override
    public AppFileSystem getFileSystem(String name) {
        Objects.requireNonNull(name);
        loadFileSystems();
        return fileSystems.get(name);
    }
}
