package com.powsybl.iidm.network.impl;

public class EmptyNetworkDatastore implements NetworkDatastore {

    @Override
    public SubstationData loadSubstationData(String id) {
        return null;
    }

    @Override
    public void markAsDirty(IdentifiableData data) {
    }
}
