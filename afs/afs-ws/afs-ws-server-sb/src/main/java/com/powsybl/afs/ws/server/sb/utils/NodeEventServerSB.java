package com.powsybl.afs.ws.server.sb.utils;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;


import com.powsybl.afs.ws.utils.AfsRestApi;

@Configuration
@EnableWebSocket
@ComponentScan(basePackageClasses = {NodeEventListEncoder.class})

public class NodeEventServerSB   implements WebSocketConfigurer  {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeEventServerSB.class);
	
    @Autowired(required=true)
    private AppDataBeanSB appDataBean;
	
    @Autowired(required=true)
    private WebSocketContextSB webSocketContext;

    
    
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		//registry.addHandler(new TaskEventHandlerSB(appDataBean, webSocketContext), "/messages/" + AfsRestApi.RESOURCE_ROOT + "/" + AfsRestApi.VERSION + "/node_events/{fileSystemName}")
		registry.addHandler(traiter(), "/messages/" + AfsRestApi.RESOURCE_ROOT + "/" + AfsRestApi.VERSION + "/node_events/{fileSystemName}")
		.setAllowedOrigins("*")
		.addInterceptors(new UriTemplateHandshakeInterceptor())
		;
	}
	
    @Bean
    public WebSocketHandler traiter() {
        return new NodeEventHandlerSB(appDataBean, webSocketContext);
    }
	
    /*@Bean
    public HandshakeInterceptor auctionInterceptor() {
        return new HandshakeInterceptor() {
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            	System.out.println("----------------------WS ---------------------------------------------:: 001 ");
                // Get the URI segment corresponding to the auction id during handshake
                String path = request.getURI().getPath();
                String fileSystemName = path.substring(path.lastIndexOf('/') + 1);

                // This will be added to the websocket session
                attributes.put("fileSystemName", fileSystemName);
                return true;
            }

            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
            	System.out.println("----------------------WS ---------------------------------------------:: 002");
                // Nothing to do after handshake
            }
        };
    }

    @Bean
    public WebSocketHandler auctionHandler() {
        return new TextWebSocketHandler() {
            public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
                // Retrieve the auction id from the websocket session (copied during the handshake)
                String fileSystemName = (String) session.getAttributes().get("fileSystemName");
                // Your business logic...
            }
        };
    }*/
    
    private class UriTemplateHandshakeInterceptor implements HandshakeInterceptor {
    	@Override
    	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
    		HttpServletRequest origRequest =((ServletServerHttpRequest) request).getServletRequest();
    		
    		/* Retrieve template variables */
    		Map<String, String> uriTemplateVars =(Map<String, String>) origRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    		/* Put template variables into WebSocket session attributes */
    		if (uriTemplateVars != null) {
    			attributes.putAll(uriTemplateVars);
    		}
    		return true;
    	}
    	@Override
    	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,Exception exception) {
    		System.out.println("----------------------WS --------------------------------------------- :: 3");
    	}
    }
}
