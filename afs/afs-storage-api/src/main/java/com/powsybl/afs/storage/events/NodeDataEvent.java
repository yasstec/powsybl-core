/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NodeDataEvent extends NodeEvent {

    @JsonProperty("dataName")
    protected final String dataName;

    protected NodeDataEvent(String id, String dataName) {
        super(id);
        this.dataName = Objects.requireNonNull(dataName);
    }

    public String getDataName() {
        return dataName;
    }
}
