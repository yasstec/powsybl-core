package com.powsybl.iidm.network.impl;

public interface NetworkDatastore {

    SubstationData loadSubstationData(String id);

    void markAsDirty(IdentifiableData data);
}
