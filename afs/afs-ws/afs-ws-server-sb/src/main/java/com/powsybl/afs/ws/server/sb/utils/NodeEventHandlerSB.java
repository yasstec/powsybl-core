package com.powsybl.afs.ws.server.sb.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.ConvertingEncoderDecoderSupport;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.TaskListener;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.events.AppStorageListener;
import com.powsybl.afs.storage.events.NodeEventList;
import com.powsybl.afs.ws.utils.JacksonEncoder;




@Component
public class NodeEventHandlerSB extends TextWebSocketHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeEventServerSB.class);
	List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
	
	//@Autowired(required=true)
    private final AppDataBeanSB appDataBean;

    //@Autowired
    private final WebSocketContextSB webSocketContext;
    
	public NodeEventHandlerSB(AppDataBeanSB appDataBean, WebSocketContextSB webSocketContext) {
		this.appDataBean= appDataBean;
		this.webSocketContext = webSocketContext;
	}
	
	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws InterruptedException, IOException {
		System.out.println("----------------------WS --------------------------------------------- 2");
		//Map<String, String> value = new Gson().fromJson(message.getPayload(), Map.class);
		/*for(WebSocketSession webSocketSession : sessions) {
			webSocketSession.sendMessage(new TextMessage("Hello " + value.get("name") + " !"));
		}*/
		
		//System.out.println("----------------------WS --------------------------------------------- 2 " + value.get("fileSystemName") + " --------------------- " + value.get("projectId"));
		//session.sendMessage(new TextMessage("Hello " + value.get("name") + " !"));
	}
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		//the messages will be broadcasted to all users.
		String fileSystemName = session.getAttributes().get("fileSystemName").toString();
		System.out.println("----------------------WS --------------------------------------------- 00 02 :: " + session.getAttributes().get("fileSystemName") +" :: " + appDataBean);
        LOGGER.debug("WebSocket session '{}' opened for file system '{}'", session.getId(), fileSystemName);

        ListenableAppStorage storage = appDataBean.getStorage(fileSystemName);

        AppStorageListener listener = eventList -> {
            if (session.isOpen()) {
            	//((StandardWebSocketSession) session).getNativeSession().getContainer().
                RemoteEndpoint.Async remote = ((StandardWebSocketSession) session).getNativeSession().getAsyncRemote();
                remote.setSendTimeout(1000);
                String ss = "";
                //Object ss = "";
                try {
					//ss=((Encoder.Text)new JacksonEncoder()).encode(eventList);
                	ss = new NodeEventListEncoder().encode(eventList);
                	System.out.println("----------------------WS --------------------------------------------- 00 02 :: +++++ : " + ss);
                	System.out.println("----------------------WS --------------------------------------------- 00 02 :: +++++ : " + eventList.getEvents());
				} catch (EncodeException e) {
					// TODO Auto-generated catch block
					System.out.println("----------------------WS --------------------------------------------- 00 02 :: ---");
					e.printStackTrace();
				}
                remote.sendText(ss, result -> {
                    if (!result.isOK()) {
                        LOGGER.error(result.getException().toString(), result.getException());
                    }
                });
            } else {
                webSocketContext.removeSession(((StandardWebSocketSession) session).getNativeSession());
            }
        };
        storage.addListener(listener);
        //session.getUserProperties().put("listener", listener); // to prevent weak listener from being garbage collected
        ((StandardWebSocketSession) session).getNativeSession().getUserProperties().put("listener", listener); // to prevent weak listener from being garbage collected
        webSocketContext.addSession(((StandardWebSocketSession) session).getNativeSession());
		sessions.add(session);
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		System.out.println("----------------------WS --------------------------------------------- 00 03");
		String fileSystemName = (String) session.getAttributes().get("fileSystemName");
		removeSession(fileSystemName, ((StandardWebSocketSession) session).getNativeSession());
		
	}
	
    private void removeSession(String fileSystemName, Session session) {
        AppFileSystem fileSystem = appDataBean.getFileSystem(fileSystemName);
        webSocketContext.removeSession(session);
        Object oo = session.getUserProperties().get("listener");
//        try {
        System.out.println("---------------------------------------WS---------------------------------" + oo.getClass() + "---------------" + fileSystemName);		
        TaskListener listener = (TaskListener) session.getUserProperties().get("listener");
        fileSystem.getTaskMonitor().removeListener(listener);
//        } catch (Exception e) {}
        fileSystem.getTaskMonitor().removeListener((TaskListener)oo);
        
    }
}
