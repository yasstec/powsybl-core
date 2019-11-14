package com.powsybl.cgmes.conversion.test.update;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;

public final class NetworkChanges {
    private NetworkChanges() {
    }

    public static void scaleLoadGeneration(Network network, int maxChanges) {
        int count;
        count = 0;
        for (Generator g : network.getGenerators()) {
            double newP = g.getTargetP() * 1.1;
            double newQ = g.getTargetQ() * 1.1;
            g.setTargetP(newP).setTargetQ(newQ).getTerminal().setP(-newP).setQ(-newQ);
            count++;
            if (count > maxChanges) {
                break;
            }
        }

        count = 0;
        for (Load g : network.getLoads()) {
            double newP = g.getP0() * 1.1;
            double newQ = g.getQ0() * 1.1;
            g.setP0(newP).setQ0(newQ).getTerminal().setP(newP).setQ(newQ);
            count++;
            if (count > maxChanges) {
                break;
            }
        }
    }
}