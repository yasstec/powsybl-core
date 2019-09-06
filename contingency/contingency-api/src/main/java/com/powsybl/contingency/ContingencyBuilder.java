/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.contingency;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Mathieu Bague <mathieu.bague@rte-france.com>
 */
public class ContingencyBuilder {

    private final String id;

    private final List<ContingencyElement> elements = new ArrayList<>();

    ContingencyBuilder(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public ContingencyBuilder busbarSection(String busbarSectionId) {
        elements.add(new BusbarSectionContingency(busbarSectionId));
        return this;
    }

    public ContingencyBuilder generator(String generatorId) {
        elements.add(new GeneratorContingency(generatorId));
        return this;
    }

    public ContingencyBuilder hvdcLine(String hvdcLineId) {
        elements.add(new HvdcLineContingency(hvdcLineId));
        return this;
    }

    public ContingencyBuilder line(String lineId) {
        elements.add(new BranchContingency(lineId));
        return this;
    }

    public ContingencyBuilder shuntCompensator(String shuntCompensatorId) {
        elements.add(new ShuntCompensatorContingency(shuntCompensatorId));
        return this;
    }

    public ContingencyBuilder staticVarCompensator(String svcId) {
        elements.add(new StaticVarCompensatorContingency(svcId));
        return this;
    }

    public ContingencyBuilder twoWindingsTransformer(String twtId) {
        elements.add(new BranchContingency(twtId));
        return this;
    }

    public Contingency build() {
        return new Contingency(id, elements);
    }
}
