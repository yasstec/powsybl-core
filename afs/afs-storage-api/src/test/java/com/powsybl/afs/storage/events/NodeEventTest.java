/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.testing.EqualsTester;
import com.powsybl.commons.json.JsonUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NodeEventTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper = JsonUtil.createObjectMapper();
    }

    @Test
    public void dependencyAddedTest() throws IOException {
        DependencyAdded added = new DependencyAdded("a", "b");
        assertEquals("a", added.getId());
        assertEquals("b", added.getDependencyName());

        DependencyAdded added2 = objectMapper.readValue(objectMapper.writeValueAsString(added), DependencyAdded.class);
        assertEquals(added, added2);

        new EqualsTester()
                .addEqualityGroup(new DependencyAdded("a", "b"), new DependencyAdded("a", "b"))
                .addEqualityGroup(new DependencyAdded("c", "d"), new DependencyAdded("c", "d"))
                .testEquals();
    }

    @Test
    public void backwardDependencyAddedTest() throws IOException {
        BackwardDependencyAdded added = new BackwardDependencyAdded("a", "b");
        assertEquals("a", added.getId());
        assertEquals("b", added.getDependencyName());

        BackwardDependencyAdded added2 = objectMapper.readValue(objectMapper.writeValueAsString(added), BackwardDependencyAdded.class);
        assertEquals(added, added2);

        new EqualsTester()
                .addEqualityGroup(new BackwardDependencyAdded("a", "b"), new BackwardDependencyAdded("a", "b"))
                .addEqualityGroup(new BackwardDependencyAdded("c", "d"), new BackwardDependencyAdded("c", "d"))
                .testEquals();
    }

    @Test
    public void dependencyRemovedTest() throws IOException {
        DependencyRemoved removed = new DependencyRemoved("a", "b");
        assertEquals("a", removed.getId());
        assertEquals("b", removed.getDependencyName());

        DependencyRemoved removed2 = objectMapper.readValue(objectMapper.writeValueAsString(removed), DependencyRemoved.class);
        assertEquals(removed, removed2);

        new EqualsTester()
                .addEqualityGroup(new DependencyRemoved("a", "b"), new DependencyRemoved("a", "b"))
                .addEqualityGroup(new DependencyRemoved("c", "d"), new DependencyRemoved("c", "d"))
                .testEquals();
    }

    @Test
    public void backwardDependencyRemovedTest() throws IOException {
        BackwardDependencyRemoved removed = new BackwardDependencyRemoved("a", "b");
        assertEquals("a", removed.getId());
        assertEquals("b", removed.getDependencyName());

        BackwardDependencyRemoved removed2 = objectMapper.readValue(objectMapper.writeValueAsString(removed), BackwardDependencyRemoved.class);
        assertEquals(removed, removed2);

        new EqualsTester()
                .addEqualityGroup(new BackwardDependencyRemoved("a", "b"), new BackwardDependencyRemoved("a", "b"))
                .addEqualityGroup(new BackwardDependencyRemoved("c", "d"), new BackwardDependencyRemoved("c", "d"))
                .testEquals();
    }

    @Test
    public void nodeCreatedTest() throws IOException {
        ChildAdded created = new ChildAdded("a", "b");
        assertEquals("a", created.getId());
        assertEquals("b", created.getParentId());

        ChildAdded created2 = objectMapper.readValue(objectMapper.writeValueAsString(created), ChildAdded.class);
        assertEquals(created, created2);

        new EqualsTester()
                .addEqualityGroup(new ChildAdded("a", "b"), new ChildAdded("a", "b"))
                .addEqualityGroup(new ChildAdded("b", null), new ChildAdded("b", null))
                .testEquals();
    }

    @Test
    public void nodeDataUpdatedTest() throws IOException {
        NodeDataUpdated updated = new NodeDataUpdated("a", "b");
        assertEquals("a", updated.getId());
        assertEquals("b", updated.getDataName());

        NodeDataUpdated updated2 = objectMapper.readValue(objectMapper.writeValueAsString(updated), NodeDataUpdated.class);
        assertEquals(updated, updated2);

        new EqualsTester()
                .addEqualityGroup(new NodeDataUpdated("a", "b"), new NodeDataUpdated("a", "b"))
                .addEqualityGroup(new NodeDataUpdated("c", "d"), new NodeDataUpdated("c", "d"))
                .testEquals();
    }

    @Test
    public void nodeDescriptionUpdatedTest() throws IOException {
        NodeDescriptionUpdated updated = new NodeDescriptionUpdated("a", "b");
        assertEquals("a", updated.getId());
        assertEquals("b", updated.getDescription());

        NodeDescriptionUpdated updated2 = objectMapper.readValue(objectMapper.writeValueAsString(updated), NodeDescriptionUpdated.class);
        assertEquals(updated, updated2);

        new EqualsTester()
                .addEqualityGroup(new NodeDescriptionUpdated("a", "b"), new NodeDescriptionUpdated("a", "b"))
                .addEqualityGroup(new NodeDescriptionUpdated("c", "d"), new NodeDescriptionUpdated("c", "d"))
                .testEquals();
    }

    @Test
    public void nodeRemovedTest() throws IOException {
        ChildRemoved removed = new ChildRemoved("a", "b");
        assertEquals("a", removed.getId());
        assertEquals("b", removed.getParentId());

        ChildRemoved removed2 = objectMapper.readValue(objectMapper.writeValueAsString(removed), ChildRemoved.class);
        assertEquals(removed, removed2);

        new EqualsTester()
                .addEqualityGroup(new ChildRemoved("a", "b"), new ChildRemoved("a", "b"))
                .addEqualityGroup(new ChildRemoved("b", "c"), new ChildRemoved("b", "c"))
                .testEquals();
    }

    @Test
    public void timeSeriesClearedTest() throws IOException {
        TimeSeriesCleared cleared = new TimeSeriesCleared("a");
        assertEquals("a", cleared.getId());

        TimeSeriesCleared cleared2 = objectMapper.readValue(objectMapper.writeValueAsString(cleared), TimeSeriesCleared.class);
        assertEquals(cleared, cleared2);

        new EqualsTester()
                .addEqualityGroup(new TimeSeriesCleared("a"), new TimeSeriesCleared("a"))
                .addEqualityGroup(new TimeSeriesCleared("b"), new TimeSeriesCleared("b"))
                .testEquals();
    }

    @Test
    public void timeSeriesCreatedTest() throws IOException {
        TimeSeriesCreated created = new TimeSeriesCreated("a", "b");
        assertEquals("a", created.getId());
        assertEquals("b", created.getTimeSeriesName());

        TimeSeriesCreated created2 = objectMapper.readValue(objectMapper.writeValueAsString(created), TimeSeriesCreated.class);
        assertEquals(created, created2);

        new EqualsTester()
                .addEqualityGroup(new TimeSeriesCreated("a", "b"), new TimeSeriesCreated("a", "b"))
                .addEqualityGroup(new TimeSeriesCreated("c", "d"), new TimeSeriesCreated("c", "d"))
                .testEquals();
    }

    @Test
    public void timeSeriesDataUpdatedTest() throws IOException {
        TimeSeriesDataAdded updated = new TimeSeriesDataAdded("a", "b");
        assertEquals("a", updated.getId());
        assertEquals("b", updated.getTimeSeriesName());

        TimeSeriesDataAdded updated2 = objectMapper.readValue(objectMapper.writeValueAsString(updated), TimeSeriesDataAdded.class);
        assertEquals(updated, updated2);

        new EqualsTester()
                .addEqualityGroup(new TimeSeriesDataAdded("a", "b"), new TimeSeriesDataAdded("a", "b"))
                .addEqualityGroup(new TimeSeriesDataAdded("c", "d"), new TimeSeriesDataAdded("c", "d"))
                .testEquals();
    }

    @Test
    public void eventListTest() throws IOException {
        NodeEventList eventList = new NodeEventList(new ChildAdded("a", "c"), new ChildRemoved("b", "d"));
        assertEquals("[ChildAdded(id=a, parentId=c), ChildRemoved(id=b, parentId=d)]", eventList.toString());

        NodeEventList eventList2 = objectMapper.readValue(objectMapper.writeValueAsString(eventList), NodeEventList.class);
        assertEquals(eventList, eventList2);
    }
}
