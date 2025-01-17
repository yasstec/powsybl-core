/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.xml;

import com.powsybl.iidm.network.test.HvdcTestNetwork;
import org.junit.Test;

import java.io.IOException;

import static com.powsybl.iidm.xml.IidmXmlConstants.CURRENT_IIDM_XML_VERSION;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class HvdcXmlTest extends AbstractXmlConverterTest {

    @Test
    public void roundTripLccTest() throws IOException {
        roundTripXmlTest(HvdcTestNetwork.createLcc(),
                NetworkXml::writeAndValidate,
                NetworkXml::read,
                getVersionDir(CURRENT_IIDM_XML_VERSION) + "LccRoundTripRef.xml");

        // backward compatibility 1.0
        roundTripVersionnedXmlTest("LccRoundTripRef.xml", IidmXmlVersion.V_1_0);
    }

    @Test
    public void roundTripVscTest() throws IOException {
        roundTripXmlTest(HvdcTestNetwork.createVsc(),
                NetworkXml::writeAndValidate,
                NetworkXml::read,
                getVersionDir(CURRENT_IIDM_XML_VERSION) + "VscRoundTripRef.xml");

        // backward compatibility 1.0
        roundTripVersionnedXmlTest("VscRoundTripRef.xml", IidmXmlVersion.V_1_0);
    }
}
