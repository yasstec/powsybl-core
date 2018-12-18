package com.powsybl.afs.network.server.sb;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.ext.base.ScriptType;
import com.powsybl.afs.ws.storage.st.AppDataBeanCld;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping(value="/rest/networkCache")
@Api(value = "/rest/networkCache", tags = "networkCache")
public class NetworkCacheServerSB {
	@Autowired(required=true)
    private AppDataBeanCld appDataBeanCld;
    
    @RequestMapping(method = RequestMethod.GET, value = "hello", produces=MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sayHello() {
    	List<String> lst = appDataBeanCld.getLstFileSystem("String fileSystemName");
    	return ResponseEntity.ok().body(""+lst+"\n");
    }
	
    @RequestMapping(method = RequestMethod.GET, value = "fileSystems/{fileSystemName}/nodes/{nodeId}", produces=MediaType.APPLICATION_XML_VALUE)
    @ApiOperation (value = "Get Network", response = StreamingResponseBody.class)
    @ApiResponses (value = {@ApiResponse(code = 200, message = "The available network"), @ApiResponse(code = 404, message = "No network found.")})
    public ResponseEntity<StreamingResponseBody> getNetwork(@ApiParam(value = "File System Name") @PathVariable("fileSystemName") String fileSystemName,
    					@ApiParam(value = "Node ID") @PathVariable("nodeId") String nodeId) {
        Network network = appDataBeanCld.getProjectFile(fileSystemName, nodeId, ProjectFile.class, ProjectCase.class)
                .getNetwork();
        StreamingResponseBody streamingOutput = output -> NetworkXml.write(network, output);
        return ResponseEntity.ok().body(streamingOutput);
    }

    @RequestMapping(method = RequestMethod.POST, value = "fileSystems/{fileSystemName}/nodes/{nodeId}", consumes = MediaType.TEXT_PLAIN_VALUE, produces=MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation (value = "Question Network", response = String.class)
    @ApiResponses (value = {@ApiResponse(code = 200, message = "Response of query"), @ApiResponse(code = 500, message = "Error.")})
    public ResponseEntity<String> queryNetwork(@ApiParam(value = "File System Name") @PathVariable("fileSystemName") String fileSystemName,
    					@ApiParam(value = "Node ID") @PathVariable("nodeId") String nodeId,
    					@ApiParam(value = "Script Type") @PathVariable("scriptType") ScriptType scriptType,
    					@ApiParam(value = "Script Content") @RequestBody String scriptContent) {
        String resultJson = appDataBeanCld.getProjectFile(fileSystemName, nodeId, ProjectFile.class, ProjectCase.class)
                .queryNetwork(scriptType, scriptContent);
        return ResponseEntity.ok().body(resultJson);
    }
    
    @RequestMapping(method = RequestMethod.DELETE, value = "fileSystems/{fileSystemName}/nodes/{nodeId}")
    @ApiOperation (value = "Invalidate Cache", response = String.class)
    @ApiResponses (value = {@ApiResponse(code = 200, message = "Cache invalidated"), @ApiResponse(code = 500, message = "Error.")})
    public ResponseEntity<String> invalidateCache(@ApiParam(value = "File System Name") @PathVariable("fileSystemName") String fileSystemName,
    					@ApiParam(value = "Node ID") @PathVariable("nodeId") String nodeId) {
    	appDataBeanCld.getProjectFile(fileSystemName, nodeId, ProjectFile.class, ProjectCase.class)
                .invalidateNetworkCache();
        return ResponseEntity.ok().build();
    }
}
