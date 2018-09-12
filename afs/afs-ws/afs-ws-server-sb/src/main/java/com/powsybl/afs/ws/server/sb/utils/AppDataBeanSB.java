package com.powsybl.afs.ws.server.sb.utils;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.powsybl.afs.AppData;
import com.powsybl.afs.ws.server.utils.AppDataBean;

@Profile("default")
@Component
public class AppDataBeanSB extends AppDataBean {
    public AppData getAppDataSB() {
        return appData;
    }
}
