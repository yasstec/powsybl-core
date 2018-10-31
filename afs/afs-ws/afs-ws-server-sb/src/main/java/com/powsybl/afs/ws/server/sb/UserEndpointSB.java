package com.powsybl.afs.ws.server.sb;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.powsybl.afs.ws.server.utils.SecurityConfig;
import com.powsybl.afs.ws.server.utils.UserAuthenticator;
import com.powsybl.afs.ws.server.utils.sb.AppDataBeanSB;
import com.powsybl.afs.ws.server.utils.sb.jwt.JwtService;
import com.powsybl.afs.ws.utils.UserProfile;
import com.powsybl.commons.config.PlatformConfig;

@RestController
@RequestMapping(path = "/rest/users")
/*@ComponentScan({
	"com.powsybl.afs.ws.server.utils.sb.jwt", 
	"com.powsybl.afs.ws.server.utils.sb",
	})*/
@ComponentScan(basePackageClasses = {AppDataBeanSB.class})
public class UserEndpointSB {
	
	@Autowired
    private JwtService jwtService;
	
    @Autowired
    private UserAuthenticator authenticator;

    private long tokenValidity;
    
    public UserEndpointSB() {
        this(PlatformConfig.defaultConfig());
    }
    public UserEndpointSB(PlatformConfig platformConfig) {
        tokenValidity = SecurityConfig.load(platformConfig).getTokenValidity();
    }    
	
    @RequestMapping(path = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<UserProfile> login(@RequestParam("login") String login, @RequestParam("password") String password, HttpServletResponse response) {
    	try {
    		UserProfile profile = authenticator.check(login, password);
    		String token = jwtService.tokenFor(profile.getLastName(), tokenValidity);
			return ResponseEntity.ok().header("Authorization", "Bearer " + token).body(profile);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
    	return null;
    }
    //TODO: à supprimer
    @RequestMapping(path = "/hello", method = RequestMethod.GET)
    public ResponseEntity<String> sayHello() {
		return ResponseEntity.ok().body("Hello !");
    }
}
