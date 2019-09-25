/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.distributed;

import com.powsybl.computation.Partition;
import com.powsybl.contingency.ContingenciesProvider;

/**
 * A contingencies provider which provides a subset of another provider,
 * defined by a {@link Partition}.
 *
 * For example, if the other provider defines 10 contingencies,
 * an instance of this provider will return the 5 first contingencies for the partition 1/2,
 * or the 5 next for the partition 2/2.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 *
 * @deprecated Use {@link com.powsybl.contingency.SubContingenciesProvider} instead.
 */
@Deprecated
public class SubContingenciesProvider extends com.powsybl.contingency.SubContingenciesProvider {

    public SubContingenciesProvider(ContingenciesProvider delegate, Partition partition) {
        super(delegate, partition);
    }
}
