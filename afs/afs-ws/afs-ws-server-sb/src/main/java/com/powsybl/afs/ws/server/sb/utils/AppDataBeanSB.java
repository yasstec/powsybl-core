package com.powsybl.afs.ws.server.sb.utils;

import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import com.powsybl.afs.AfsException;
import com.powsybl.afs.AppData;
//import com.powsybl.afs.AppData;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
@Configuration
@Profile("default")
@Component
//@Repository
@Singleton
public class AppDataBeanSB {
	
    protected AppData appData;

    protected ComputationManager shortTimeExecutionComputationManager;

    protected ComputationManager longTimeExecutionComputationManager;

    public AppData getAppDataSB() {
        return appData;
    }

    public ListenableAppStorage getStorage(String fileSystemName) {
        ListenableAppStorage storage = appData.getRemotelyAccessibleStorage(fileSystemName);
        if (storage == null) {
            //throw new WebApplicationException("App file system '" + fileSystemName + "' not found", Response.Status.NOT_FOUND);
        }
        return storage;
    }

    public AppFileSystem getFileSystem(String name) {
        Objects.requireNonNull(appData);
        Objects.requireNonNull(name);
        AppFileSystem fileSystem = appData.getFileSystem(name);
        if (fileSystem == null) {
            //throw new WebApplicationException("App file system '" + name + "' not found", Response.Status.NOT_FOUND);
        }
        return fileSystem;
    }

    public <T extends ProjectFile> T getProjectFile(String fileSystemName, String nodeId, Class<T> clazz) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(clazz);
        AppFileSystem fileSystem = getFileSystem(fileSystemName);
        return fileSystem.findProjectFile(nodeId, clazz);
    }

    public <T extends ProjectFile, U> U getProjectFile(String fileSystemName, String nodeId, Class<T> clazz, Class<U> clazz2) {
        T projectFile = getProjectFile(fileSystemName, nodeId, clazz);
        if (!(clazz2.isAssignableFrom(projectFile.getClass()))) {
            throw new AfsException("Project file '" + nodeId  + "' is not a " + clazz2.getName());
        }
        return (U) projectFile;
    }

    @PostConstruct
    public void init() {
        DefaultComputationManagerConfig config = DefaultComputationManagerConfig.load();
        shortTimeExecutionComputationManager = config.createShortTimeExecutionComputationManager();
        longTimeExecutionComputationManager = config.createLongTimeExecutionComputationManager();
        appData = new AppData(shortTimeExecutionComputationManager, longTimeExecutionComputationManager);
    }

    @PreDestroy
    public void clean() {
        if (appData != null) {
            appData.close();
            shortTimeExecutionComputationManager.close();
            if (longTimeExecutionComputationManager != null) {
                longTimeExecutionComputationManager.close();
            }
        }
    }
}
