/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.impl;

import com.powsybl.iidm.network.ShuntCompensatorAdder;
import com.powsybl.iidm.network.Terminal;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class ShuntCompensatorAdderImpl extends AbstractInjectionAdder<ShuntCompensatorAdderImpl> implements ShuntCompensatorAdder {

    private final VoltageLevelExt voltageLevel;

    private double bPerSection;

    private int maximumSectionCount;

    private int currentSectionCount;

    private double targetV = Double.NaN;

    private double targetDeadband = 0;

    private TerminalExt regulatingTerminal;

    private boolean regulating = false;

    ShuntCompensatorAdderImpl(VoltageLevelExt voltageLevel) {
        this.voltageLevel = voltageLevel;
    }

    @Override
    protected NetworkImpl getNetwork() {
        return voltageLevel.getNetwork();
    }

    @Override
    protected String getTypeDescription() {
        return "Shunt compensator";
    }

    @Override
    public ShuntCompensatorAdder setbPerSection(double bPerSection) {
        this.bPerSection = bPerSection;
        return this;
    }

    @Override
    public ShuntCompensatorAdder setMaximumSectionCount(int maximumSectionCount) {
        this.maximumSectionCount = maximumSectionCount;
        return this;
    }

    @Override
    public ShuntCompensatorAdder setCurrentSectionCount(int currentSectionCount) {
        this.currentSectionCount = currentSectionCount;
        return this;
    }

    @Override
    public ShuntCompensatorAdder setRegulatingTerminal(Terminal regulatingTerminal) {
        this.regulatingTerminal = (TerminalExt) regulatingTerminal;
        return this;
    }

    @Override
    public ShuntCompensatorAdder setRegulating(boolean regulating) {
        this.regulating = regulating;
        return this;
    }

    @Override
    public ShuntCompensatorAdder setTargetV(double targetV) {
        this.targetV = targetV;
        return this;
    }

    @Override
    public ShuntCompensatorAdder setTargetDeadband(double targetDeadband) {
        this.targetDeadband = targetDeadband;
        return this;
    }

    @Override
    public ShuntCompensatorImpl add() {
        String id = checkAndGetUniqueId();
        TerminalExt terminal = checkAndGetTerminal();
        ValidationUtil.checkbPerSection(this, bPerSection);
        ValidationUtil.checkSections(this, currentSectionCount, maximumSectionCount);
        ValidationUtil.checkRegulatingTerminal(this, regulatingTerminal, getNetwork());
        ValidationUtil.checkVoltageControl(this, regulating, targetV);
        ValidationUtil.checkTargetDeadband(this, targetDeadband);
        ShuntCompensatorImpl shunt
                = new ShuntCompensatorImpl(getNetwork().getRef(),
                id, getName(), bPerSection, maximumSectionCount,
                currentSectionCount, regulatingTerminal == null ? terminal : regulatingTerminal,
                regulating, targetV, targetDeadband);
        shunt.addTerminal(terminal);
        voltageLevel.attach(terminal, false);
        getNetwork().getIndex().checkAndAdd(shunt);
        getNetwork().getListeners().notifyCreation(shunt);
        return shunt;
    }

}
