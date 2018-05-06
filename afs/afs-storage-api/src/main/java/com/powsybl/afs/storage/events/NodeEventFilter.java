/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.events;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NodeEventFilter {

    private final String nodeId;

    private final Class<?> nodeClass;

    public NodeEventFilter(String nodeId, Class<?> nodeClass) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.nodeClass = Objects.requireNonNull(nodeClass);
    }

    public String getNodeId() {
        return nodeId;
    }

    public Class<?> getNodeClass() {
        return nodeClass;
    }
}
