package com.powsybl.afs.ws.server.sb.utils;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.*;
import com.powsybl.afs.storage.AbstractAppStorageTest;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.ws.client.utils.ClientUtils;
import com.powsybl.afs.ws.client.utils.UserSession;
import com.powsybl.afs.ws.storage.RemoteAppStorage;
import com.powsybl.afs.ws.storage.RemoteListenableAppStorage;
import com.powsybl.afs.ws.storage.st.RemoteAppStorageSt;
import com.powsybl.afs.ws.storage.st.RemoteListenableAppStorageSt;
import com.powsybl.commons.exceptions.UncheckedUriSyntaxException;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AppStorageServerSBTest extends AbstractAppStorageTest  {

	@LocalServerPort
    private int port;

	@Autowired
    private ServletContext servletContext; 
	
	//private UserSession userSession;
	
    @Override
    public void setUp() throws Exception {
        //userSession = ClientUtils.authenticate(getRestUri(), "", "");
        super.setUp();
    }
    private URI getRestUri() {
        try {
        	String sheme = "http";
            return new URI(sheme + "://localhost:"+port+servletContext.getContextPath().toString());
        } catch (URISyntaxException e) {
            throw new UncheckedUriSyntaxException(e);
		}
    }
    @Override
    protected ListenableAppStorage createStorage() {
    	URI restUri = getRestUri();
        //RemoteAppStorage storage = new RemoteAppStorage(AppDataBeanMockSB.TEST_FS_NAME, restUri, "");//userSession.getToken());
        //return new RemoteListenableAppStorage(storage, restUri);
    	RemoteAppStorageSt storage = new RemoteAppStorageSt(AppDataBeanMockSB.TEST_FS_NAME, restUri, "");//userSession.getToken());
        return new RemoteListenableAppStorageSt(storage, restUri);
    }
    @Test
    public void getFileSystemNamesTest() {
        List<String> fileSystemNames = RemoteAppStorage.getFileSystemNames(getRestUri(), "");//userSession.getToken());
        assertEquals(Collections.singletonList(AppDataBeanMockSB.TEST_FS_NAME), fileSystemNames);
    }
}
