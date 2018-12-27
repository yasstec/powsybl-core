package com.powsybl.afs.ws.storage.st.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.SecurityTokenProvider;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.computation.DefaultComputationManagerConfig;

@Profile("default")
@Component
public class AppDataBeanCld extends AppDataBean {
    private Map<String, AppFileSystem> fileSystems;

    @Autowired
    private EurekaClient eurekaClient;

    //private URI uri;
    private SecurityTokenProvider tokenProvider = () -> null;
    private AppDataCld appDataCld;

    public List<String> getLstFileSystem(String token) {
        return getAppData().getFileSystemNames("");
    }
    @Override
    @PostConstruct
    public void init() {
        DefaultComputationManagerConfig config = DefaultComputationManagerConfig.load();
        shortTimeExecutionComputationManager = config.createShortTimeExecutionComputationManager();
        longTimeExecutionComputationManager = config.createLongTimeExecutionComputationManager();
        appDataCld = new AppDataCld(shortTimeExecutionComputationManager, longTimeExecutionComputationManager);

        /*InstanceInfo instInf = eurekaClient.getNextServerFromEureka("IMAGRID-SERVER", false);
        try {
            this.uri = new URI(instInf.getHomePageUrl());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        appDataCld.setUri(uri);*/
        appDataCld.setUri(getHomePageUrl());
    }
    public URI getHomePageUrl() {
        InstanceInfo instInf = eurekaClient.getNextServerFromEureka("IMAGRID-SERVER", false);
        URI uriEur = null;
        try {
            uriEur = new URI(instInf.getHomePageUrl());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return uriEur;
    }
    @Override
    public AppDataCld getAppData() {
        return this.appDataCld;
    }

    @Override
    public AppFileSystem getFileSystem(String name) {
        Objects.requireNonNull(appDataCld);
        Objects.requireNonNull(name);
        System.out.println("------------------------------>>> : getAppData() : " + getAppData());
        AppFileSystem fileSystem = getAppData().getFileSystem(name);
        if (fileSystem == null) {
            //throw new WebApplicationException("App file system '" + name + "' not found", HttpStatus.NOT_FOUND.value());
            throw new WebApplicationException(HttpStatus.NOT_FOUND.value());
        }
        return fileSystem;
    }
}
