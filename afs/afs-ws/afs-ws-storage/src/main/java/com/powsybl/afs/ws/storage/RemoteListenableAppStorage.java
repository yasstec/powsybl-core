/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.ws.storage;

import com.powsybl.afs.storage.ForwardingAppStorage;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.events.AppStorageListener;
import com.powsybl.afs.storage.events.NodeEventFilter;
import com.powsybl.afs.storage.events.NodeEventList;
import com.powsybl.afs.ws.client.utils.UncheckedDeploymentException;
import com.powsybl.afs.ws.utils.AfsRestApi;
import com.powsybl.commons.exceptions.UncheckedUriSyntaxException;
import com.powsybl.commons.util.WeakListenerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class RemoteListenableAppStorage extends ForwardingAppStorage implements ListenableAppStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteListenableAppStorage.class);

    private final URI restUri;

    private final ClientEndpointConfig endpointConfig;

    private final WeakListenerList<AppStorageListener> listeners = new WeakListenerList<>();

    public RemoteListenableAppStorage(RemoteAppStorage storage, URI restUri) {
        super(storage);
        this.restUri = Objects.requireNonNull(restUri);
        endpointConfig = ClientEndpointConfig.Builder.create()
                .decoders(Collections.singletonList(NodeEventListDecoder.class))
                .build();
    }

    static URI getWebSocketUri(URI restUri) {
        try {
            return new URI("ws", restUri.getUserInfo(), restUri.getHost(), restUri.getPort(), restUri.getPath(), restUri.getQuery(), null);
        } catch (URISyntaxException e) {
            throw new UncheckedUriSyntaxException(e);
        }
    }

    @Override
    public void addListener(AppStorageListener l, NodeEventFilter filter) {
        if (listeners.contains(l)) {
            return;
        }

        URI wsUri = getWebSocketUri(restUri);
        URI endPointUri = URI.create(wsUri + "/messages/" + AfsRestApi.RESOURCE_ROOT + "/" +
                AfsRestApi.VERSION + "/node_events/" + storage.getFileSystemName() + "/" +
                filter.getNodeId() + "/" + filter.getNodeClass().getName());
        LOGGER.debug("Connecting to node event websocket at {}", endPointUri);

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        try {
            Session session = container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig endpointConfig) {
                    LOGGER.trace("Node event websocket session '{}' open at {}",
                            session.getId(), endPointUri.toString());
                    session.addMessageHandler((MessageHandler.Whole<NodeEventList>) nodeEventList -> {
                        LOGGER.trace("Node event websocket session '{}' received events: {}",
                                session.getId(), nodeEventList);
                        listeners.log();
                        listeners.notify(l -> l.onEvents(nodeEventList));
                    });
                }

                @Override
                public void onError(Session session, Throwable t) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error(t.toString(), t);
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    LOGGER.trace("Node event websocket session '{}' closed ", session.getId());
                }
            }, endpointConfig, wsUri);

            listeners.add(l, session);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DeploymentException e) {
            throw new UncheckedDeploymentException(e);
        }
    }

    @Override
    public void removeListener(AppStorageListener l) {
        Session session = (Session) listeners.remove(l);
        if (session != null) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, ""));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void removeListeners() {
        for (Object session : listeners.removeAll()) {
            try {
                ((Session) session).close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, ""));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
