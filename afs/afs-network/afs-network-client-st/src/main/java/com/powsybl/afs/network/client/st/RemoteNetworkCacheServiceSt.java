package com.powsybl.afs.network.client.st;



import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Supplier;
import com.powsybl.afs.AfsException;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ext.base.NetworkCacheService;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.ext.base.ProjectCaseListener;
import com.powsybl.afs.ext.base.ScriptType;
import com.powsybl.afs.ws.client.utils.RemoteServiceConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;

import static com.powsybl.afs.ws.client.utils.st.ClientUtilsSt.readEntityIfOk;
import static com.powsybl.afs.ws.client.utils.st.ClientUtilsSt.checkOk;;

public class RemoteNetworkCacheServiceSt implements NetworkCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteNetworkCacheServiceSt.class);
    private static final String FILE_SYSTEM_NAME = "fileSystemName";
    private static final String NODE_ID = "nodeId";
    private static final String NODE_PATH = "fileSystems/{fileSystemName}/nodes/{nodeId}";

    private final Supplier<Optional<RemoteServiceConfig>> configSupplier;

    private final String token;
    private final UriComponentsBuilder webTarget;

    RemoteNetworkCacheServiceSt(Supplier<Optional<RemoteServiceConfig>> configSupplier, String token) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
        this.token = token;
        this.webTarget = getWebTarget(getConfig().getRestUri());
    }
    private RemoteServiceConfig getConfig() {
        return Objects.requireNonNull(configSupplier.get()).orElseThrow(() -> new AfsException("Remote service config is missing"));
    }
    static RestTemplate createClient() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }
    static UriComponentsBuilder getWebTarget(URI baseUri) {
        UriComponentsBuilder ub = UriComponentsBuilder
                .fromUri(baseUri)
                .pathSegment("rest")
                .pathSegment("networkCache");
        return ub;
    }
    @Override
    public <T extends ProjectFile & ProjectCase> Network getNetwork(T projectCase) {
        LOGGER.info("getNetwork(fileSystemName={}, nodeId={})", projectCase.getFileSystem().getName(),
                projectCase.getId());

        RestTemplate client = createClient();
        UriComponentsBuilder webTargetTemp = webTarget.cloneBuilder();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        headers.add(HttpHeaders.AUTHORIZATION, token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map<String, String> params = new HashMap<String, String>();
        params.put(FILE_SYSTEM_NAME, projectCase.getFileSystem().getName());
        params.put(NODE_ID, projectCase.getId());

        URI uri = webTargetTemp
                .path(NODE_PATH)
                .buildAndExpand(params)
                .toUri();

        ResponseEntity<InputStream> response = client.exchange(
                uri,
                HttpMethod.GET,
                entity,
                InputStream.class
                );
        try (InputStream is = readEntityIfOk(response)) {
            return NetworkXml.read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            response = null;
        }
    }

    @Override
    public <T extends ProjectFile & ProjectCase> String queryNetwork(T projectCase, ScriptType scriptType, String scriptContent) {
        Objects.requireNonNull(projectCase);
        Objects.requireNonNull(scriptType);
        Objects.requireNonNull(scriptContent);

        LOGGER.info("queryNetwork(fileSystemName={}, nodeId={}, scriptType={}, scriptContent=...)",
                projectCase.getFileSystem().getName(), projectCase.getId(), scriptType);

        RestTemplate client = createClient();
        UriComponentsBuilder webTargetTemp = webTarget.cloneBuilder();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
        headers.add(HttpHeaders.AUTHORIZATION, token);

        HttpEntity<String> entity = new HttpEntity<>(scriptContent, headers);

        Map<String, String> params = new HashMap<String, String>();
        params.put(FILE_SYSTEM_NAME, projectCase.getFileSystem().getName());
        params.put(NODE_ID, projectCase.getId());

        URI uri = webTargetTemp
                .path(NODE_PATH)
                .queryParam("scriptType", scriptType.name())
                .buildAndExpand(params)
                .toUri();

        ResponseEntity<String> response = client.exchange(
                uri,
                HttpMethod.POST,
                entity,
                String.class
                );
        try {
            return readEntityIfOk(response);
        } finally {
            response = null;
        }
    }

    @Override
    public <T extends ProjectFile & ProjectCase> void invalidateCache(T projectCase) {
        Objects.requireNonNull(projectCase);

        LOGGER.info("invalidateCache(fileSystemName={}, nodeId={})",
                projectCase.getFileSystem().getName(), projectCase.getId());
        RestTemplate client = createClient();
        UriComponentsBuilder webTargetTemp = webTarget.cloneBuilder();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map<String, String> params = new HashMap<String, String>();
        params.put(FILE_SYSTEM_NAME, projectCase.getFileSystem().getName());
        params.put(NODE_ID, projectCase.getId());

        URI uri = webTargetTemp
                .path(NODE_PATH)
                .buildAndExpand(params)
                .toUri();

        ResponseEntity<String> response = client.exchange(
                uri,
                HttpMethod.DELETE,
                entity,
                String.class
                );
        try {
            checkOk(response);
        } finally {
            response = null;
        }
    }
    @Override
    public <T extends ProjectFile & ProjectCase> void addListener(T projectCase, ProjectCaseListener listener) {
        // TODO
    }
    @Override
    public <T extends ProjectFile & ProjectCase> void removeListener(T projectCase, ProjectCaseListener listener) {
        // TODO
    }
}
