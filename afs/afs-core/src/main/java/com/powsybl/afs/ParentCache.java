/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs;

import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.afs.storage.events.AppStorageListener;
import com.powsybl.afs.storage.events.NodeEvent;
import com.powsybl.afs.storage.events.NodeEventList;
import com.powsybl.afs.storage.events.NodeEventType;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ParentCache {

    private final NodeInfo info;

    private final ListenableAppStorage storage;

    private final Project project;

    private final AppStorageListener l = new AppStorageListener() {
        @Override
        public void onEvents(NodeEventList eventList) {
            for (NodeEvent event : eventList.getEvents()) {
                if (event.getId().equals(info.getId()) && event.getType() == NodeEventType.PARENT_CHANGED) {
                    invalidate();
                }
            }
        }
    };

    private ProjectFolder cache;

    private boolean cached = false;

    private final Lock lock = new ReentrantLock();

    public ParentCache(NodeInfo info, ListenableAppStorage storage, Project project) {
        this.info = Objects.requireNonNull(info);
        this.storage = Objects.requireNonNull(storage);
        this.project = Objects.requireNonNull(project);
        this.storage.addListener(l);
    }

    public void invalidate() {
        lock.lock();
        try {
            if (cached) {
                cache = null;
                cached = false;
            }
        } finally {
            lock.unlock();
        }
    }

    public void set(ProjectFolder projectFolder) {
        lock.lock();
        try {
            cache = projectFolder;
            cached = true;
        } finally {
            lock.unlock();
        }
    }

    public Optional<ProjectFolder> get() {
        lock.lock();
        try {
            if (!cached) {
                cache = storage.getParentNode(info.getId())
                        .filter(parentInfo -> ProjectFolder.PSEUDO_CLASS.equals(parentInfo.getPseudoClass()))
                        .map(parentInfo -> new ProjectFolder(new ProjectFileCreationContext(parentInfo, storage, project)))
                        .orElse(null);
                cached = true;
            }
            return Optional.ofNullable(cache);
        } finally {
            lock.unlock();
        }
    }
}
