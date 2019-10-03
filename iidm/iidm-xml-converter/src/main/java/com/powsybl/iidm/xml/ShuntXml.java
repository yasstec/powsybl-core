/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.xml;

import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.ShuntCompensatorAdder;
import com.powsybl.iidm.network.VoltageLevel;

import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class ShuntXml extends AbstractConnectableXml<ShuntCompensator, ShuntCompensatorAdder, VoltageLevel> {

    static final ShuntXml INSTANCE = new ShuntXml();

    static final String ROOT_ELEMENT_NAME = "shunt";

    @Override
    protected String getRootElementName() {
        return ROOT_ELEMENT_NAME;
    }

    @Override
    protected boolean hasSubElements(ShuntCompensator sc) {
        return sc.getRegulatingTerminal() != sc.getTerminal();
    }

    @Override
    protected void writeRootElementAttributes(ShuntCompensator sc, VoltageLevel vl, NetworkXmlWriterContext context) throws XMLStreamException {
        XmlUtil.writeDouble("bPerSection", sc.getbPerSection(), context.getWriter());
        context.getWriter().writeAttribute("maximumSectionCount", Integer.toString(sc.getMaximumSectionCount()));
        context.getWriter().writeAttribute("currentSectionCount", Integer.toString(sc.getCurrentSectionCount()));
        context.getWriter().writeAttribute("regulating", Boolean.toString(sc.isRegulating()));
        XmlUtil.writeDouble("targetV", sc.getTargetV(), context.getWriter());
        XmlUtil.writeOptionalDouble("targetDeadband", sc.getTargetDeadband(), 0, context.getWriter());
        writeNodeOrBus(null, sc.getTerminal(), context);
        writePQ(null, sc.getTerminal(), context.getWriter());
    }

    @Override
    protected void writeSubElements(ShuntCompensator sc, VoltageLevel vl, NetworkXmlWriterContext context) throws XMLStreamException {
        if (sc.getRegulatingTerminal() != sc.getTerminal()) {
            TerminalRefXml.writeTerminalRef(sc.getRegulatingTerminal(), context, "regulatingTerminal");
        }
    }

    @Override
    protected ShuntCompensatorAdder createAdder(VoltageLevel vl) {
        return vl.newShuntCompensator();
    }

    @Override
    protected ShuntCompensator readRootElementAttributes(ShuntCompensatorAdder adder, NetworkXmlReaderContext context) {
        double bPerSection = XmlUtil.readDoubleAttribute(context.getReader(), "bPerSection");
        int maximumSectionCount = XmlUtil.readIntAttribute(context.getReader(), "maximumSectionCount");
        int currentSectionCount = XmlUtil.readIntAttribute(context.getReader(), "currentSectionCount");
        boolean regulating = XmlUtil.readBoolAttribute(context.getReader(), "regulating");
        double targetV = XmlUtil.readOptionalDoubleAttribute(context.getReader(), "targetV");
        double targetDeadband = XmlUtil.readOptionalDoubleAttribute(context.getReader(), "targetDeadband", 0);
        adder.setbPerSection(bPerSection)
                .setMaximumSectionCount(maximumSectionCount)
                .setCurrentSectionCount(currentSectionCount)
                .setRegulating(regulating)
                .setTargetV(targetV)
                .setTargetDeadband(targetDeadband);
        readNodeOrBus(adder, context);
        ShuntCompensator sc = adder.add();
        readPQ(null, sc.getTerminal(), context.getReader());
        return sc;
    }

    @Override
    protected void readSubElements(ShuntCompensator sc, NetworkXmlReaderContext context) throws XMLStreamException {
        readUntilEndRootElement(context.getReader(), () -> {
            if (context.getReader().getLocalName().equals("regulatingTerminal")) {
                String id = context.getAnonymizer().deanonymizeString(context.getReader().getAttributeValue(null, "id"));
                String side = context.getReader().getAttributeValue(null, "side");
                context.getEndTasks().add(() -> sc.setRegulatingTerminal(TerminalRefXml.readTerminalRef(sc.getTerminal().getVoltageLevel().getSubstation().getNetwork(), id, side)));
            } else {
                super.readSubElements(sc, context);
            }
        });
    }
}
