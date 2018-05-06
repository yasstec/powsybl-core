/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.commons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WeakListenerList<L> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeakListenerList.class);

    private final WeakHashMap<L, Object> listeners = new WeakHashMap<>();

    private final Lock lock = new ReentrantLock();

    public int size() {
        return listeners.size();
    }

    public void add(L l) {
        add(l, new Object());
    }

    public void add(L l, Object info) {
        Objects.requireNonNull(l);
        Objects.requireNonNull(info);
        lock.lock();
        try {
            listeners.put(l, info);
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(L l) {
        Objects.requireNonNull(l);
        lock.lock();
        try {
            return listeners.containsKey(l);
        } finally {
            lock.unlock();
        }
    }

    public Object remove(L l) {
        Objects.requireNonNull(l);
        lock.lock();
        try {
            return listeners.remove(l);
        } finally {
            lock.unlock();
        }
    }

    public Collection<Object> removeAll() {
        lock.lock();
        try {
            Collection<Object> values = listeners.values();
            listeners.clear();
            return values;
        } finally {
            lock.unlock();
        }
    }

    public void notify(BiConsumer<L, Object> notifier) {
        Objects.requireNonNull(notifier);
        lock.lock();
        try {
            for (Map.Entry<L, Object> e : new HashSet<>(listeners.entrySet())) {
                notifier.accept(e.getKey(), e.getValue());
            }
        } finally {
            lock.unlock();
        }
    }

    public void notify(Consumer<L> notifier) {
        notify((l, o) -> notifier.accept(l));
    }

    public List<L> toList() {
        lock.lock();
        try {
            return new ArrayList<>(listeners.keySet());
        } finally {
            lock.unlock();
        }
    }

    public void log() {
        lock.lock();
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Weak listener list status:{}{}",
                        System.lineSeparator(),
                        new HashSet<>(listeners.keySet())
                                .stream()
                                .collect(Collectors.groupingBy(L::getClass))
                                .entrySet()
                                .stream()
                                .map(e -> e.getKey().getSimpleName() + ": " + e.getValue().size())
                                .collect(Collectors.joining(System.lineSeparator())));
            }
        } finally {
            lock.unlock();
        }
    }
}
