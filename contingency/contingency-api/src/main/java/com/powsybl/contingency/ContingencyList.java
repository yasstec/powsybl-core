/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.contingency;

import com.powsybl.iidm.network.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Mathieu Bague <mathieu.bague@rte-france.com>
 */
public interface ContingencyList {

    /**
     * Create a contingency list from an array of contingencies
     * @param contingencies The contingencies of the list to create
     * @return a new ContingencyList
     */
    static ContingencyList of(Contingency... contingencies) {
        Objects.requireNonNull(contingencies);

        return contingencies.length == 0 ? empty() : new DefaultContingencyList(contingencies);
    }

    /**
     * Create a contingency list from a list of contingencies
     * @param contingencies The contingencies of the list to create
     * @return a new ContingencyList
     */
    static ContingencyList of(List<Contingency> contingencies) {
        Objects.requireNonNull(contingencies);

        return contingencies.isEmpty() ? empty() : new DefaultContingencyList(contingencies);
    }

    /**
     * Create an empty contingency list
     */
    static ContingencyList empty() {
        return network -> Collections.emptyList();
    }

    /**
     * Create a contingency list with several {@link BusbarSectionContingency} elements.
     */
    static ContingencyList busbarSections(String... ids) {
        return ContingencyListHelper.newContingencyList(Contingency::busbarSection, ids);
    }

    /**
     * Create a contingency list that triggers all the busbar sections of the network
     */
    static ContingencyList busbarSections(Network network) {
        return busbarSections(network, l -> true);
    }

    /**
     * Create a contingency list that triggers all the busbar sections of the network which pass the predicate
     */
    static ContingencyList busbarSections(Network network, Predicate<BusbarSection> predicate) {
        Objects.requireNonNull(network);
        return ContingencyListHelper.newContingencyList(network.getBusbarSectionStream(), predicate, Contingency::busbarSection);
    }

    /**
     * Create a contingency list with several {@link GeneratorContingency} elements.
     */
    static ContingencyList generators(String... ids) {
        return ContingencyListHelper.newContingencyList(Contingency::generator, ids);
    }

    /**
     * Create a contingency list that triggers all the generators of the network
     */
    static ContingencyList generators(Network network) {
        return generators(network, l -> true);
    }

    /**
     * Create a contingency list that triggers all the generators of the network which pass the predicate
     */
    static ContingencyList generators(Network network, Predicate<Generator> predicate) {
        Objects.requireNonNull(network);
        return ContingencyListHelper.newContingencyList(network.getGeneratorStream(), predicate, Contingency::generator);
    }

    /**
     * Create a contingency list with several {@link HvdcLineContingency} elements.
     */
    static ContingencyList hvdcLines(String... ids) {
        return ContingencyListHelper.newContingencyList(Contingency::hvdcLine, ids);
    }

    /**
     * Create a contingency list that triggers all the HVDC lines of the network
     */
    static ContingencyList hvdcLines(Network network) {
        return hvdcLines(network, l -> true);
    }

    /**
     * Create a contingency list that triggers all the HVDC lines of the network which pass the predicate
     */
    static ContingencyList hvdcLines(Network network, Predicate<HvdcLine> predicate) {
        Objects.requireNonNull(network);
        return ContingencyListHelper.newContingencyList(network.getHvdcLineStream(), predicate, Contingency::hvdcLine);
    }

    /**
     * Create a contingency list with several {@link BranchContingency} elements.
     */
    static ContingencyList lines(String... ids) {
        return ContingencyListHelper.newContingencyList(Contingency::line, ids);
    }

    /**
     * Create a contingency list that triggers all the lines of the network
     */
    static ContingencyList lines(Network network) {
        return lines(network, l -> true);
    }

    /**
     * Create a contingency list that triggers all the lines of the network which pass the predicate
     */
    static ContingencyList lines(Network network, Predicate<Line> predicate) {
        Objects.requireNonNull(network);
        return ContingencyListHelper.newContingencyList(network.getLineStream(), predicate, Contingency::line);
    }

    /**
     * Create a contingency list with several {@link ShuntCompensatorContingency} elements.
     */
    static ContingencyList shuntCompensators(String... ids) {
        return ContingencyListHelper.newContingencyList(Contingency::shuntCompensator, ids);
    }

    /**
     * Create a contingency list that triggers all the shunt compensators of the network
     */
    static ContingencyList shuntCompensators(Network network) {
        return shuntCompensators(network, l -> true);
    }

    /**
     * Create a contingency list that triggers all the shunt compensators of the network which pass the predicate
     */
    static ContingencyList shuntCompensators(Network network, Predicate<ShuntCompensator> predicate) {
        Objects.requireNonNull(network);
        return ContingencyListHelper.newContingencyList(network.getShuntCompensatorStream(), predicate, Contingency::shuntCompensator);
    }

    /**
     * Create a contingency list with several {@link StaticVarCompensatorContingency} elements.
     */
    static ContingencyList staticVarCompensators(String... ids) {
        return ContingencyListHelper.newContingencyList(Contingency::staticVarCompensator, ids);
    }

    /**
     * Create a contingency list that triggers all the static VAR compensators of the network
     */
    static ContingencyList staticVarCompensators(Network network) {
        return staticVarCompensators(network, l -> true);
    }

    /**
     * Create a contingency list that triggers all the static VAR compensators of the network which pass the predicate
     */
    static ContingencyList staticVarCompensators(Network network, Predicate<StaticVarCompensator> predicate) {
        Objects.requireNonNull(network);
        return ContingencyListHelper.newContingencyList(network.getStaticVarCompensatorStream(), predicate, Contingency::staticVarCompensator);
    }

    /**
     * Create a contingency list with several {@link BranchContingency} elements.
     */
    static ContingencyList twoWindingsTransformers(String... ids) {
        return ContingencyListHelper.newContingencyList(Contingency::twoWindingsTransformer, ids);
    }

    /**
     * Create a contingency list that triggers all the two windings transformers of the network
     */
    static ContingencyList twoWindingsTransformers(Network network) {
        return twoWindingsTransformers(network, l -> true);
    }

    /**
     * Create a contingency list that triggers all the two windings transformers of the network which pass the predicate
     */
    static ContingencyList twoWindingsTransformers(Network network, Predicate<TwoWindingsTransformer> predicate) {
        Objects.requireNonNull(network);
        return ContingencyListHelper.newContingencyList(network.getTwoWindingsTransformerStream(), predicate, Contingency::twoWindingsTransformer);
    }

    List<Contingency> getContingencies(Network network);
}
