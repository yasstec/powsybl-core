package com.powsybl.afs.ws.server.utils.sb;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.powsybl.afs.ws.server.utils.AppDataBean;

@Profile("default")
@Component
public class AppDataBeanSB extends AppDataBean {
}
