/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.ws.server;

import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.events.AppStorageListener;
import com.powsybl.afs.storage.events.NodeEventFilter;
import com.powsybl.afs.ws.utils.AfsRestApi;
import com.powsybl.commons.exceptions.UncheckedClassNotFoundException;
import com.rte_france.imagrid.afs.ws.server.utils.AppDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@ServerEndpoint(value = "/messages/" + AfsRestApi.RESOURCE_ROOT + "/" + AfsRestApi.VERSION + "/node_events/{fileSystemName}/{nodeId}/{nodeClassName}",
                encoders = {NodeEventListEncoder.class})
public class NodeEventServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeEventServer.class);

    @Inject
    private AppDataBean appDataBean;

    @OnOpen
    public void onOpen(@PathParam("fileSystemName") String fileSystemName,
                       @PathParam("nodeId") String nodeId,
                       @PathParam("nodeClassName") String nodeClassName,
                       Session session) {
        LOGGER.debug("WebSocket session '{}' opened at {}", session.getId(), session.getRequestURI());

        ListenableAppStorage storage = appDataBean.getStorage(fileSystemName);

        AppStorageListener listener = eventList -> {
            if (session.isOpen()) {
                RemoteEndpoint.Async remote = session.getAsyncRemote();
                remote.setSendTimeout(1000);
                remote.sendObject(eventList, result -> {
                    if (!result.isOK()) {
                        LOGGER.error(result.getException().toString(), result.getException());
                    }
                });
            }
        };
        Class<?> nodeClass;
        try {
            nodeClass = Class.forName(nodeClassName);
        } catch (ClassNotFoundException e) {
            throw new UncheckedClassNotFoundException(e);
        }
        storage.addListener(listener, new NodeEventFilter(nodeId, nodeClass));
        session.getUserProperties().put("listener", listener); // to prevent weak listener from being garbage collected
    }

    private void removeSession(String fileSystemName, Session session) {
        ListenableAppStorage storage = appDataBean.getStorage(fileSystemName);
        AppStorageListener listener = (AppStorageListener) session.getUserProperties().get("listener");
        storage.removeListener(listener);
    }

    @OnClose
    public void onClose(@PathParam("fileSystemName") String fileSystemName, Session session, CloseReason closeReason) {
        LOGGER.debug("WebSocket session '{}' closed ({})",
                session.getId(), closeReason);

        removeSession(fileSystemName, session);
    }

    @OnError
    public void error(@PathParam("fileSystemName") String fileSystemName, Session session, Throwable t) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(t.toString(), t);
        }

        removeSession(fileSystemName, session);
    }
}
