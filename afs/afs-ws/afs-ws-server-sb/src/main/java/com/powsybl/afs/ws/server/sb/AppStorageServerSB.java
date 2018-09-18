package com.powsybl.afs.ws.server.sb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.io.ByteStreams;
import com.powsybl.afs.AfsException;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.TaskMonitor;
import com.powsybl.afs.storage.AppStorage;
import com.powsybl.afs.storage.NodeDependency;
import com.powsybl.afs.storage.NodeGenericMetadata;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.afs.storage.buffer.DoubleTimeSeriesChunksAddition;
import com.powsybl.afs.storage.buffer.StorageChange;
import com.powsybl.afs.storage.buffer.StorageChangeSet;
import com.powsybl.afs.storage.buffer.StringTimeSeriesChunksAddition;
import com.powsybl.afs.storage.buffer.TimeSeriesCreation;
import com.powsybl.afs.ws.server.sb.utils.AppDataBeanSB;
import com.powsybl.timeseries.DoubleArrayChunk;
import com.powsybl.timeseries.StringArrayChunk;
import com.powsybl.timeseries.TimeSeriesMetadata;

@RestController
@RequestMapping(value="/rest/afs/v1")
public class AppStorageServerSB {
	@Autowired(required=true)
    private AppDataBeanSB appDataBean;

	@RequestMapping(method = RequestMethod.GET, value = "fileSystems")
    public List<String> getFileSystemNames() {
        return appDataBean.getAppDataSB().getRemotelyAccessibleFileSystemNames();
    }
    @RequestMapping(method = RequestMethod.PUT, value = "fileSystems/{fileSystemName}/rootNode", produces = "application/json")
    public ResponseEntity<NodeInfo> createRootNodeIfNotExists(@PathVariable("fileSystemName") String fileSystemName, @RequestParam("nodeName") String nodeName, @RequestParam("nodePseudoClass") String nodePseudoClass) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        NodeInfo rootNodeInfo = storage.createRootNodeIfNotExists(nodeName, nodePseudoClass);
        return ResponseEntity.ok()./*accepted().headers(headers).*/body(rootNodeInfo);
    }
    @RequestMapping(method = RequestMethod.POST, value = "fileSystems/{fileSystemName}/flush", consumes= "application/json")
    public ResponseEntity<String> flush(@PathVariable("fileSystemName") String fileSystemName, @RequestBody StorageChangeSet changeSet) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);

        for (StorageChange change : changeSet.getChanges()) {
            switch (change.getType()) {
                case TIME_SERIES_CREATION:
                    TimeSeriesCreation creation = (TimeSeriesCreation) change;
                    storage.createTimeSeries(creation.getNodeId(), creation.getMetadata());
                    break;
                case DOUBLE_TIME_SERIES_CHUNKS_ADDITION:
                    DoubleTimeSeriesChunksAddition doubleAddition = (DoubleTimeSeriesChunksAddition) change;
                    storage.addDoubleTimeSeriesData(doubleAddition.getNodeId(), doubleAddition.getVersion(),
                                                    doubleAddition.getTimeSeriesName(), doubleAddition.getChunks());
                    break;
                case STRING_TIME_SERIES_CHUNKS_ADDITION:
                    StringTimeSeriesChunksAddition stringAddition = (StringTimeSeriesChunksAddition) change;
                    storage.addStringTimeSeriesData(stringAddition.getNodeId(), stringAddition.getVersion(),
                                                    stringAddition.getTimeSeriesName(), stringAddition.getChunks());
                    break;
                default:
                    throw new AssertionError("Unknown change type " + change.getType());
            }
        }
        // propagate flush to underlying storage
        storage.flush();

        return ResponseEntity.ok().build();
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/writable", produces=MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> isWritable(@PathVariable("fileSystemName") String fileSystemName, @PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        boolean writable = storage.isWritable(nodeId);
        return ResponseEntity.ok().body(Boolean.toString(writable));
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/parent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NodeInfo> getParentNode(@PathVariable("fileSystemName") String fileSystemName, @PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Optional<NodeInfo> parentNodeInfo = storage.getParentNode(nodeId);
        if (parentNodeInfo.isPresent()) {
            return ResponseEntity.ok().body(parentNodeInfo.get());
        } else {
            return ResponseEntity.noContent().build();
        }
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}",  produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NodeInfo> getNodeInfo(@PathVariable("fileSystemName") String fileSystemName, @PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        NodeInfo nodeInfo = storage.getNodeInfo(nodeId);
        return ResponseEntity.ok().body(nodeInfo);
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/children",  produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NodeInfo>> getChildNodes(@PathVariable("fileSystemName") String fileSystemName, @PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        List<NodeInfo> childNodes = storage.getChildNodes(nodeId);
        return ResponseEntity.ok().body(childNodes);
    }
    @RequestMapping(method = RequestMethod.POST, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/children/{childName}", produces = MediaType.APPLICATION_JSON_VALUE, consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NodeInfo> createNode(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		@PathVariable("childName") String childName,
    		@RequestParam("description") String description,
    		@RequestParam("nodePseudoClass") String nodePseudoClass,
    		@RequestParam("version") int version,
    		@RequestBody NodeGenericMetadata nodeMetadata) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        NodeInfo newNodeInfo =  storage.createNode(nodeId, childName, nodePseudoClass, description, version, nodeMetadata);
        return ResponseEntity.ok().body(newNodeInfo);
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/children/{childName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NodeInfo> getChildNode(@PathVariable("fileSystemName") String fileSystemName,
    					@PathVariable("nodeId") String nodeId,
    					@PathVariable("childName") String childName) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Optional<NodeInfo> childNodeInfo = storage.getChildNode(nodeId, childName);
        if (childNodeInfo.isPresent()) {
            return ResponseEntity.ok().body(childNodeInfo.get());
        } else {
            return ResponseEntity.noContent().build();
        }
    }
    @RequestMapping(method = RequestMethod.PUT, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/description", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> setDescription(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		@RequestBody String description) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        storage.setDescription(nodeId, description);
        return ResponseEntity.ok().build();
    }
    @RequestMapping(method = RequestMethod.PUT, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/modificationTime")
    public ResponseEntity<String> updateModificationTime(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        storage.updateModificationTime(nodeId);
        return ResponseEntity.ok().build();
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/dependencies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<NodeDependency>> getDependencies(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Set<NodeDependency> dependencies = storage.getDependencies(nodeId);
        return ResponseEntity.ok().body(dependencies);
    }    
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/backwardDependencies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<NodeInfo>> getBackwardDependencies(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Set<NodeInfo> backwardDependencyNodes = storage.getBackwardDependencies(nodeId);
        return ResponseEntity.ok().body(backwardDependencyNodes);
    }
    @RequestMapping(method = RequestMethod.PUT, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/data/{name}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> writeBinaryData(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		@PathVariable("name") String name,
                                    InputStream is) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        try (OutputStream os = storage.writeBinaryData(nodeId, name)) {
            if (os == null) {
            	return ResponseEntity.ok().build();
            } else {
                ByteStreams.copy(is, os);
                return ResponseEntity.ok().build();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> getDataNames(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Set<String> dataNames = storage.getDataNames(nodeId);
        return ResponseEntity.ok().body(dataNames);
    }    
    @RequestMapping(method = RequestMethod.PUT, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/dependencies/{name}/{toNodeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addDependency(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		@PathVariable("name") String name,
    		@PathVariable("toNodeId") String toNodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        storage.addDependency(nodeId, name, toNodeId);
        return ResponseEntity.ok().build();
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/dependencies/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<NodeInfo>> getDependencies(@PathVariable("fileSystemName") String fileSystemName,
    								@PathVariable("nodeId") String nodeId,
    								@PathVariable("name") String name) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Set<NodeInfo> dependencies = storage.getDependencies(nodeId, name);
        return ResponseEntity.ok().body(dependencies);
    }
    @RequestMapping(method = RequestMethod.DELETE, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/dependencies/{name}/{toNodeId}")
    public ResponseEntity<String> removeDependency(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		@PathVariable("name") String name,
    		@PathVariable("toNodeId") String toNodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        storage.removeDependency(nodeId, name, toNodeId);
        return ResponseEntity.ok().build();
    }
    @RequestMapping(method = RequestMethod.DELETE, value = "fileSystems/{fileSystemName}/nodes/{nodeId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteNode(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        String parentNodeId = storage.deleteNode(nodeId);
        return ResponseEntity.ok().body(parentNodeId);
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/data/{name}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> readBinaryAttribute(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		@PathVariable("name") String name) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Optional<InputStream> is = storage.readBinaryData(nodeId, name);
        
        if (is.isPresent()) {
            return ResponseEntity.ok().body(new InputStreamResource(is.get()));
        }
        return ResponseEntity.noContent().build();
    }   
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/data/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> dataExists(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		@PathVariable("name") String name) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        boolean exists = storage.dataExists(nodeId, name);
        return ResponseEntity.ok().body(Boolean.toString(exists));
    }
    @RequestMapping(method = RequestMethod.DELETE, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/data/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> removeData(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		@PathVariable("name") String name) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        boolean removed = storage.removeData(nodeId, name);
        return ResponseEntity.ok().body(Boolean.toString(removed));
    }
    
    @RequestMapping(method = RequestMethod.POST, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String>  createTimeSeries(@PathVariable("fileSystemName") String fileSystemName,
    		@PathVariable("nodeId") String nodeId,
    		TimeSeriesMetadata metadata) {
    	
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        storage.createTimeSeries(nodeId, metadata);
        return ResponseEntity.ok().build();
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries/name", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> getTimeSeriesNames(@PathVariable("fileSystemName") String fileSystemName,
                                       @PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Set<String> timeSeriesNames = storage.getTimeSeriesNames(nodeId);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_ENCODING, "gzip").body(timeSeriesNames);
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries/{timeSeriesName}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> timeSeriesExists(@PathVariable("fileSystemName") String fileSystemName,
                                     @PathVariable("nodeId") String nodeId,
                                     @PathVariable("timeSeriesName") String timeSeriesName) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        boolean exists = storage.timeSeriesExists(nodeId, timeSeriesName);
        return  ResponseEntity.ok().body(Boolean.toString(exists));
    }
    @RequestMapping(method = RequestMethod.POST, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries/metadata", produces = MediaType.APPLICATION_JSON_VALUE, consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TimeSeriesMetadata>> getTimeSeriesMetadata(@PathVariable("fileSystemName") String fileSystemName,
                                          @PathVariable("nodeId") String nodeId,
                                          @RequestBody Set<String> timeSeriesNames) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        List<TimeSeriesMetadata> metadataList = storage.getTimeSeriesMetadata(nodeId, timeSeriesNames);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_ENCODING, "gzip").body(metadataList);
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<Integer>> getTimeSeriesDataVersions(@PathVariable("fileSystemName") String fileSystemName,
                                              @PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Set<Integer> versions = storage.getTimeSeriesDataVersions(nodeId);
        return ResponseEntity.ok().body(versions);
    }
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries/{timeSeriesName}/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<Integer>> getTimeSeriesDataVersions(@PathVariable("fileSystemName") String fileSystemName,
                                              @PathVariable("nodeId") String nodeId,
                                              @PathVariable("timeSeriesName") String timeSeriesName) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Set<Integer> versions = storage.getTimeSeriesDataVersions(nodeId, timeSeriesName);
        return ResponseEntity.ok().body(versions);
    }
    @RequestMapping(method = RequestMethod.POST, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries/double/{version}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<DoubleArrayChunk>>> getDoubleTimeSeriesData(@PathVariable("fileSystemName") String fileSystemName,
                                            @PathVariable("nodeId") String nodeId,
                                            @PathVariable("version") int version,
                                            @RequestBody Set<String> timeSeriesNames) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Map<String, List<DoubleArrayChunk>> timeSeriesData = storage.getDoubleTimeSeriesData(nodeId, timeSeriesNames, version);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                .body(timeSeriesData);
    }
    @RequestMapping(method = RequestMethod.POST, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries/string/{version}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<StringArrayChunk>>> getStringTimeSeriesData(@PathVariable("fileSystemName") String fileSystemName,
                                            @PathVariable("nodeId") String nodeId,
                                            @PathVariable("version") int version,
                                            @RequestBody Set<String> timeSeriesNames) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        Map<String, List<StringArrayChunk>> timeSeriesData = storage.getStringTimeSeriesData(nodeId, timeSeriesNames, version);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                .body(timeSeriesData);
    }
    @RequestMapping(method = RequestMethod.DELETE, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/timeSeries")
    public ResponseEntity<String> clearTimeSeries(@PathVariable("fileSystemName") String fileSystemName,
                                    @PathVariable("nodeId") String nodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        storage.clearTimeSeries(nodeId);
        return ResponseEntity.ok().build();
    }
    @RequestMapping(method = RequestMethod.PUT, value = "fileSystems/{fileSystemName}/nodes/{nodeId}/parent", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> setParentNode(@PathVariable("fileSystemName") String fileSystemName,
                                  @PathVariable("nodeId") String nodeId,
                                  @RequestBody String newParentNodeId) {
        AppStorage storage = appDataBean.getStorage(fileSystemName);
        storage.setParentNode(nodeId, newParentNodeId);
        return ResponseEntity.ok().build();
    }
    
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskMonitor.Snapshot> takeSnapshot(@PathVariable("fileSystemName") String fileSystemName,
    		@RequestParam("projectId") String projectId) {
        AppFileSystem fileSystem = appDataBean.getFileSystem(fileSystemName);
        TaskMonitor.Snapshot snapshot = fileSystem.getTaskMonitor().takeSnapshot(projectId);
        return ResponseEntity.ok().body(snapshot);
    } 
    @RequestMapping(method = RequestMethod.PUT, value = "fileSystems/{fileSystemName}/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskMonitor.Task> startTask(@PathVariable("fileSystemName") String fileSystemName,
    		@RequestParam("projectFileId") String projectFileId) {
        AppFileSystem fileSystem = appDataBean.getFileSystem(fileSystemName);
        ProjectFile projectFile = fileSystem.findProjectFile(projectFileId, ProjectFile.class);
        if (projectFile == null) {
            throw new AfsException("Project file '" + projectFileId + "' not found in file system '" + fileSystemName + "'");
        }
        TaskMonitor.Task task = fileSystem.getTaskMonitor().startTask(projectFile);
        return ResponseEntity.ok().body(task);
    }
    @RequestMapping(method = RequestMethod.POST, value = "fileSystems/{fileSystemName}/tasks/{taskId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> updateTaskMessage(@PathVariable("fileSystemName") String fileSystemName,
                                      @PathVariable("taskId") UUID taskId,
                                      @RequestBody String message) {
        AppFileSystem fileSystem = appDataBean.getFileSystem(fileSystemName);
        fileSystem.getTaskMonitor().updateTaskMessage(taskId, message);
        return ResponseEntity.ok().build();
    }
    @RequestMapping(method = RequestMethod.DELETE, value = "fileSystems/{fileSystemName}/tasks/{taskId}")
    public ResponseEntity<String> stopTask(@PathVariable("fileSystemName") String fileSystemName,
                             @PathVariable("taskId") UUID taskId) {
        AppFileSystem fileSystem = appDataBean.getFileSystem(fileSystemName);
        fileSystem.getTaskMonitor().stopTask(taskId);
        return ResponseEntity.ok().build();
    }
}
