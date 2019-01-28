package com.powsybl.iidm.network.impl;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.impl.util.Ref;

public class LazyRefObj<T extends Identifiable> implements Ref<T> {

    private final String id;

    private final Class<T> aClass;

    private final NetworkIndex index;

    private T cache;

    public LazyRefObj(String id, Class<T> aClass, NetworkIndex index) {
        this.id = id;
        this.aClass = aClass;
        this.index = index;
    }

    @Override
    public T get() {
        if (cache == null) {
            cache = index.get(id, aClass);
        }
        return cache;
    }
}
