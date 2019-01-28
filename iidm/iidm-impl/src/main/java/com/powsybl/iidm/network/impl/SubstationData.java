package com.powsybl.iidm.network.impl;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.impl.util.Ref;

import java.util.Properties;
import java.util.Set;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SubstationData extends IdentifiableData {

    private Country country;

    private String tso;

    private Set<String> geographicalTags;

    private final Set<Ref<VoltageLevelExt>> voltageLevels;

    public SubstationData(String id, String name, Properties properties, NetworkDatastore datastore,
                          Country country, String tso, Set<String> geographicalTags, Set<Ref<VoltageLevelExt>> voltageLevels) {
        super(id, name, properties, datastore);
        this.country = country;
        this.tso = tso;
        this.geographicalTags = geographicalTags;
        this.voltageLevels = voltageLevels;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
        datastore.markAsDirty(this);
    }

    public String getTso() {
        return tso;
    }

    public void setTso(String tso) {
        this.tso = tso;
        datastore.markAsDirty(this);
    }

    public Set<String> getGeographicalTags() {
        return geographicalTags;
    }

    public void setGeographicalTags(Set<String> geographicalTags) {
        this.geographicalTags = geographicalTags;
        datastore.markAsDirty(this);
    }

    public Set<Ref<VoltageLevelExt>> getVoltageLevels() {
        return voltageLevels;
    }

    public void addVoltageLevel(Ref<VoltageLevelExt> voltageLevel) {
        voltageLevels.add(voltageLevel);
        datastore.markAsDirty(this);
    }

    public void removeVoltageLevel(VoltageLevelExt voltageLevel) {
        voltageLevels.removeIf(ref -> voltageLevel == ref.get());
        datastore.markAsDirty(this);
    }
}
