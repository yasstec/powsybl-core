/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.computation;

import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MaybeCompletableFuture<T> extends CompletableFuture<T> {

    private final Disposable disposable;

    public MaybeCompletableFuture(Maybe<T> maybe) {
        Objects.requireNonNull(maybe);
        disposable = maybe.subscribe(this::complete, this::completeExceptionally, () -> complete(null));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        disposable.dispose();
        return super.cancel(mayInterruptIfRunning);
    }
}
