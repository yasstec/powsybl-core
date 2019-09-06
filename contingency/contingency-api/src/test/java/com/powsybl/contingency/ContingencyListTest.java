/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.contingency;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Mathieu Bague <mathieu.bague@rte-france.com>
 */
public class ContingencyListTest {

    @Test
    public void testManual() {
        Network network = EurostagTutorialExample1Factory.create();

        Contingency c1 = Contingency.line("NHV1_NHV2_1");
        Contingency c2 = Contingency.line("NHV1_NHV2_2");
        ContingencyList list = ContingencyList.of(c1, c2);
        ContingencyList list2 = ContingencyList.of(Arrays.asList(c1, c2));

        List<Contingency> contingencies = list.getContingencies(network);
        assertEquals(2, contingencies.size());
        assertSame(c1, contingencies.get(0));
        assertSame(c2, contingencies.get(1));

        contingencies = list2.getContingencies(network);
        assertEquals(2, contingencies.size());
        assertSame(c1, contingencies.get(0));
        assertSame(c2, contingencies.get(1));
    }

    @Test
    public void testEmpty() {
        ContingencyList list = ContingencyList.empty();
        assertTrue(list.getContingencies(null).isEmpty());
    }

    @Test
    public void testAutomatic() {
        Network network = EurostagTutorialExample1Factory.create();
        testAutomatic(ContingencyList.generators("UNKNOWN", "GEN", "UNKNOWN2"), network, 1, GeneratorContingency.class);
        testAutomatic(ContingencyList.generators(network), network, 1, GeneratorContingency.class);
        testAutomatic(ContingencyList.lines("NHV1_NHV2_1", "UNKNOWN", "NHV1_NHV2_2"), network, 2, BranchContingency.class);
        testAutomatic(ContingencyList.lines(network), network, 2, BranchContingency.class);
        testAutomatic(ContingencyList.twoWindingsTransformers("NGEN_NHV1", "NHV2_NLOAD", "UNKNOWN"), network, 2, BranchContingency.class);
        testAutomatic(ContingencyList.twoWindingsTransformers(network), network, 2, BranchContingency.class);

        network = HvdcTestNetwork.createLcc();
        testAutomatic(ContingencyList.busbarSections("BBS1", "UNKNOWN"), network, 1, BusbarSectionContingency.class);
        testAutomatic(ContingencyList.busbarSections(network), network, 1, BusbarSectionContingency.class);
        testAutomatic(ContingencyList.shuntCompensators("C1_Filter1", "C1_Filter2", "UNKNOWN"), network, 2, ShuntCompensatorContingency.class);
        testAutomatic(ContingencyList.shuntCompensators(network), network, 4, ShuntCompensatorContingency.class);
        testAutomatic(ContingencyList.hvdcLines("L", "UNKNOWN"), network, 1, HvdcLineContingency.class);
        testAutomatic(ContingencyList.hvdcLines(network), network, 1, HvdcLineContingency.class);

        network = SvcTestCaseFactory.create();
        testAutomatic(ContingencyList.staticVarCompensators("UNKNOWN", "SVC2"), network, 1, StaticVarCompensatorContingency.class);
        testAutomatic(ContingencyList.staticVarCompensators(network), network, 1, StaticVarCompensatorContingency.class);
    }

    private static void testAutomatic(ContingencyList list, Network network, int expectedSize, Class<?> clazz) {
        List<Contingency> contingencies = list.getContingencies(network);
        assertEquals(expectedSize, contingencies.size());
        for (Contingency contingency : contingencies) {
            assertEquals(1, contingency.getElements().size());
            assertTrue(clazz.isInstance(contingency.getElements().iterator().next()));
        }
    }
}
