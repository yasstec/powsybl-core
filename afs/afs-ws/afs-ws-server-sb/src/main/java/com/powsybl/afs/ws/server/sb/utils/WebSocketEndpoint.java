package com.powsybl.afs.ws.server.sb.utils;

import javax.websocket.server.ServerEndpoint;

import org.springframework.web.socket.server.standard.SpringConfigurator;

@ServerEndpoint(value = "/echo", configurator = Toto.class)
public class WebSocketEndpoint {

}
class Toto extends SpringConfigurator{
	
}