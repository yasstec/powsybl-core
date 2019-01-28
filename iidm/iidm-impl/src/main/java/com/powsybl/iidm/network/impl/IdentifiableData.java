package com.powsybl.iidm.network.impl;

import java.util.Properties;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IdentifiableData {

    private final String id;

    private String name;

    private Properties properties;

    protected NetworkDatastore datastore;

    protected IdentifiableData(String id, String name, Properties properties, NetworkDatastore datastore) {
        this.id = id;
        this.name = name;
        this.properties = properties;
        this.datastore = datastore;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        datastore.markAsDirty(this);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
        datastore.markAsDirty(this);
    }
}
