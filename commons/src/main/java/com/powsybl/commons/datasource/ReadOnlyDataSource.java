/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.commons.datasource;

import java.io.InputStream;
import java.util.Set;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public interface ReadOnlyDataSource {

    boolean isContainer();

    String getMainFileName();

    Set<String> getFileNames(String regex);

    boolean fileExists(String fileName);

    InputStream newInputStream(String fileName);
}
