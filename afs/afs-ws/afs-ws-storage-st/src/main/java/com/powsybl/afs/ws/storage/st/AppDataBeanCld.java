package com.powsybl.afs.ws.storage.st;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.client.ServiceInstance;
//import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Profile;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
//import com.powsybl.afs.AfsException;
//import com.powsybl.afs.AppData;
import com.powsybl.afs.AppFileSystem;
//import com.powsybl.afs.AppFileSystemProvider;
//import com.powsybl.afs.AppFileSystemProviderContext;
//import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.SecurityTokenProvider;
//import com.powsybl.afs.storage.ListenableAppStorage;
//import com.powsybl.afs.ws.client.utils.RemoteServiceConfig;
import com.powsybl.afs.ws.server.utils.AppDataBean;
//import com.powsybl.afs.ws.storage.RemoteAppStorage;
//import com.powsybl.afs.ws.storage.RemoteListenableAppStorage;
//import com.powsybl.afs.ws.storage.RemoteTaskMonitor;
import com.powsybl.computation.DefaultComputationManagerConfig;

@Profile("default")
@Component
public class AppDataBeanCld extends AppDataBean {
	
	private Map<String, AppFileSystem> fileSystems;
	
	@Autowired
	private EurekaClient eurekaClient;
	
	private URI uri;
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
        
	    InstanceInfo instInf = eurekaClient.getNextServerFromEureka("IMAGRID-SERVER", false);
	    try {
			this.uri=new URI(instInf.getHomePageUrl());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	    appDataCld.setUri(uri);
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
            throw new WebApplicationException("App file system '" + name + "' not found", Response.Status.NOT_FOUND);
        }
        return fileSystem;
    }
}
