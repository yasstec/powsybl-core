package com.powsybl.afs.ws.server.sb.utils;




import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;






import static org.junit.Assert.*;

import com.powsybl.afs.AppData;
import com.powsybl.afs.storage.AbstractAppStorageTest;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.ws.server.NodeEventServer;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.afs.ws.storage.RemoteAppStorage;

@RunWith(SpringRunner.class)
//@SpringBootTest (webEnvironment=WebEnvironment.DEFINED_PORT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@WebAppConfiguration
@ComponentScan(basePackageClasses = {AppStorageServerSB.class, AppDataBeanMockSB.class, AppDataBeanSB.class, AppData.class})
//@ComponentScan(basePackageClasses = {AppStorageServerSB.class})


@ActiveProfiles("test")
public class AppStorageServerSBTest extends AbstractAppStorageTest  {
    @LocalServerPort
    private int port;

    @Override
    protected ListenableAppStorage createStorage() {
    	System.out.println("###################   TEST createStorage #####################################");
        URI restUri = null;
		try {
			restUri = new URI("http://localhost:"+port+"");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} 
        RemoteAppStorage storage = new RemoteAppStorage(AppDataBeanMockSB.TEST_FS_NAME, restUri);
    	System.out.println("###################   TEST createStorage Fin #####################################");
        return new RemoteListenableAppStorageSB(storage, restUri);
    }
    @Test
    public void getFileSystemNamesTest() {
    	System.out.println("###################   TEST getFileSystemNamesTest #####################################");
        List<String> fileSystemNames=null;
		try {
			fileSystemNames = RemoteAppStorage.getFileSystemNames(new URI("http://localhost:"+port));
			System.out.println("###################   TEST getFileSystemNamesTest #####################################" + fileSystemNames);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        assertEquals(Collections.singletonList(AppDataBeanMockSB.TEST_FS_NAME), fileSystemNames);
    	assertEquals("a", "a");
    }
}
