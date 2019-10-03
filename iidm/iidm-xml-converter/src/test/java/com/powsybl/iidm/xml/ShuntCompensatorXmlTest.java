/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.xml;

import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.test.ShuntTestCaseFactory;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public class ShuntCompensatorXmlTest extends AbstractConverterTest {

    @Test
    public void test() throws IOException {
        Network network = ShuntTestCaseFactory.create();
        ShuntCompensator sc = network.getShuntCompensator("SHUNT");
        sc.setProperty("test", "test");
        roundTripXmlTest(network, NetworkXml::writeAndValidate, NetworkXml::read, "/shuntRoundTripRef.xml");
    }
}
