/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.contingency;

import com.powsybl.iidm.network.Identifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mathieu Bague <mathieu.bague@rte-france.com>
 */
final class ContingencyListHelper {

    private ContingencyListHelper() {
    }

    /**
     * Create a contingency list from a list of ids and a mapper function
     */
    static ContingencyList newContingencyList(Function<String, Contingency> mapper, String... ids) {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(ids);

        List<Contingency> contingencies = Arrays.stream(ids)
                .map(mapper)
                .collect(Collectors.toList());

        return new DefaultContingencyList(contingencies);
    }

    static <I extends Identifiable<I>> ContingencyList newContingencyList(Stream<I> identifiables, Predicate<I> predicate, Function<String, Contingency> mapper) {
        Objects.requireNonNull(identifiables);
        Objects.requireNonNull(predicate);

        List<Contingency> contingencies = identifiables
                .filter(predicate)
                .map(Identifiable::getId)
                .map(mapper)
                .collect(Collectors.toList());

        return new DefaultContingencyList(contingencies);
    }
}
