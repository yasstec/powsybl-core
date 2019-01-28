/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.impl;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.iidm.network.Identifiable;

import java.util.*;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
abstract class AbstractIdentifiable<I extends Identifiable<I>, D extends IdentifiableData> extends AbstractExtendable<I> implements Identifiable<I>, Validable {

    protected D data;

    AbstractIdentifiable(D data) {
        this.data = data;
    }

    @Override
    public String getId() {
        return data.getId();
    }

    @Override
    public String getName() {
        return data.getName() != null ? data.getName() : data.getId();
    }

    protected abstract String getTypeDescription();

    @Override
    public String getMessageHeader() {
        return getTypeDescription() + " '" + data.getId() + "': ";
    }

    @Override
    public boolean hasProperty() {
        return data.getProperties() != null && !data.getProperties().isEmpty();
    }

    @Override
    public Properties getProperties() {
        if (data.getProperties() == null) {
            data.setProperties(new Properties());
        }
        return data.getProperties();
    }

    @Override
    public String toString() {
        return data.getId();
    }

}
