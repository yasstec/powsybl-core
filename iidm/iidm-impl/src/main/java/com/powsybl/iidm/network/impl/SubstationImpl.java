/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.impl.util.Ref;
import com.powsybl.iidm.network.impl.util.RefObj;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class SubstationImpl extends AbstractIdentifiable<Substation, SubstationData> implements Substation {

    private final Ref<NetworkImpl> networkRef;

    SubstationImpl(SubstationData data, Ref<NetworkImpl> networkRef) {
        super(data);
        this.networkRef = networkRef;
    }

    @Override
    public ContainerType getContainerType() {
        return ContainerType.SUBSTATION;
    }

    @Override
    public Country getCountry() {
        return data.getCountry();
    }

    @Override
    public SubstationImpl setCountry(Country country) {
        ValidationUtil.checkCountry(this, country);
        Country oldValue = data.getCountry();
        data.setCountry(country);
        getNetwork().getListeners().notifyUpdate(this, "country", oldValue.toString(), country.toString());
        return this;
    }

    @Override
    public String getTso() {
        return data.getTso();
    }

    @Override
    public SubstationImpl setTso(String tso) {
        String oldValue = data.getTso();
        data.setTso(tso);
        getNetwork().getListeners().notifyUpdate(this, "tso", oldValue, tso);
        return this;
    }

    @Override
    public NetworkImpl getNetwork() {
        return networkRef.get();
    }

    void addVoltageLevel(VoltageLevelExt voltageLevel) {
        data.addVoltageLevel(new RefObj<>(voltageLevel));
    }

    @Override
    public VoltageLevelAdderImpl newVoltageLevel() {
        return new VoltageLevelAdderImpl(this);
    }

    @Override
    public List<VoltageLevel> getVoltageLevels() {
        return getVoltageLevelStream().collect(Collectors.toList());
    }

    @Override
    public Stream<VoltageLevel> getVoltageLevelStream() {
        return data.getVoltageLevels().stream().map(Ref::get);
    }

    @Override
    public TwoWindingsTransformerAdderImpl newTwoWindingsTransformer() {
        return new TwoWindingsTransformerAdderImpl(this);
    }

    @Override
    public List<TwoWindingsTransformer> getTwoWindingsTransformers() {
        return getTwoWindingsTransformerStream().collect(Collectors.toList());
    }

    @Override
    public Stream<TwoWindingsTransformer> getTwoWindingsTransformerStream() {
        return data.getVoltageLevels().stream()
                .map(Ref::get)
                .flatMap(vl -> vl.getConnectableStream(TwoWindingsTransformer.class))
                .distinct();
    }

    @Override
    public int getTwoWindingsTransformerCount() {
        return data.getVoltageLevels().stream()
                .map(Ref::get)
                .mapToInt(vl -> vl.getConnectableCount(TwoWindingsTransformer.class))
                .sum();
    }

    @Override
    public ThreeWindingsTransformerAdderImpl newThreeWindingsTransformer() {
        return new ThreeWindingsTransformerAdderImpl(this);
    }

    @Override
    public List<ThreeWindingsTransformer> getThreeWindingsTransformers() {
        return getThreeWindingsTransformerStream().collect(Collectors.toList());
    }

    @Override
    public Stream<ThreeWindingsTransformer> getThreeWindingsTransformerStream() {
        return data.getVoltageLevels().stream()
                .map(Ref::get)
                .flatMap(vl -> vl.getConnectableStream(ThreeWindingsTransformer.class))
                .distinct();
    }

    @Override
    public int getThreeWindingsTransformerCount() {
        return data.getVoltageLevels().stream()
                .map(Ref::get)
                .mapToInt(vl -> vl.getConnectableCount(ThreeWindingsTransformer.class))
                .sum();
    }

    @Override
    public Set<String> getGeographicalTags() {
        return Collections.unmodifiableSet(data.getGeographicalTags());
    }

    @Override
    public Substation addGeographicalTag(String tag) {
        if (tag == null) {
            throw new ValidationException(this, "geographical tag is null");
        }
        data.setGeographicalTags(ImmutableSet.<String>builder()
                .addAll(data.getGeographicalTags())
                .add(tag)
                .build());
        return this;
    }

    @Override
    protected String getTypeDescription() {
        return "Substation";
    }

    @Override
    public void remove() {
        Substations.checkRemovability(this);

        Set<VoltageLevelExt> vls = data.getVoltageLevels().stream().map(Ref::get).collect(Collectors.toSet());
        for (VoltageLevelExt vl : vls) {
            // Remove all branches, transformers and HVDC lines
            List<Connectable> connectables = Lists.newArrayList(vl.getConnectables());
            for (Connectable connectable : connectables) {
                ConnectableType type = connectable.getType();
                if (VoltageLevels.MULTIPLE_TERMINALS_CONNECTABLE_TYPES.contains(type)) {
                    connectable.remove();
                } else if (type == ConnectableType.HVDC_CONVERTER_STATION) {
                    HvdcLine hvdcLine = getNetwork().getHvdcLine((HvdcConverterStation) connectable);
                    if (hvdcLine != null) {
                        hvdcLine.remove();
                    }
                }
            }

            // Then remove the voltage level (bus, switches and injections) from the network
            vl.remove();
        }

        // Remove this substation from the network
        getNetwork().getIndex().remove(this);

        getNetwork().getListeners().notifyRemoval(this);
    }

    void remove(VoltageLevelExt voltageLevelExt) {
        Objects.requireNonNull(voltageLevelExt);
        data.removeVoltageLevel(voltageLevelExt);
    }
}
