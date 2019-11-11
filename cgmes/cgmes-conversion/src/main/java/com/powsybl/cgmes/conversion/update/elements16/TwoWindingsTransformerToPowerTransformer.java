//package com.powsybl.cgmes.conversion.update.elements16;
//
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.UUID;
//
//import com.google.common.collect.ArrayListMultimap;
//import com.google.common.collect.Multimap;
//import com.powsybl.cgmes.conversion.update.CgmesPredicateDetails;
//import com.powsybl.cgmes.conversion.update.ConversionMapper;
//import com.powsybl.cgmes.conversion.update.IidmChange;
//import com.powsybl.cgmes.model.CgmesModel;
//import com.powsybl.iidm.network.TwoWindingsTransformer;
//import com.powsybl.triplestore.api.PropertyBag;
//import com.powsybl.triplestore.api.PropertyBags;
//
///**
// * For conversion onCreate of TwoWindingsTransformer we need to create
// * additional elements: End1 and End2, tapChangers, tables, table points if
// * required. All have distinct ID (Subject) and contain reference to the parents
// * id.
// */
//public class TwoWindingsTransformerToPowerTransformer implements ConversionMapper {
//
//    public TwoWindingsTransformerToPowerTransformer(IidmChange change, CgmesModel cgmes) {
//        this.change = change;
//        this.cgmes = cgmes;
//        this.currId = change.getIdentifiableId();
//        this.newTwoWindingsTransformer = (TwoWindingsTransformer) change.getIdentifiable();
//        this.name = newTwoWindingsTransformer.getName();
//        this.idEnd1 = (getEndsId().get(ID_END1) != null) ? getEndsId().get(ID_END1)
//            : currId.concat("_OR");
//        this.idEnd2 = (getEndsId().get(ID_END2) != null) ? getEndsId().get(ID_END2)
//            : currId.concat("_CL");
//        this.idPTC = getTapChangerId(PHASE_TAP_CHANGER);
//        this.idRTC = getTapChangerId(RATIO_TAP_CHANGER);
//        this.idPTCTable = getTapChangerTableId(idPTC, PHASE_TAP_CHANGER);
//        this.idRTCTable = getTapChangerTableId(idRTC, RATIO_TAP_CHANGER);
//        this.idPTCTablePoint = getTapChangerTablePointId(PHASE_TAP_CHANGER);
//        this.idRTCTablePoint = getTapChangerTablePointId(RATIO_TAP_CHANGER);
//    }
//
//    @Override
//    public Multimap<String, CgmesPredicateDetails> converter() {
//
//        final Multimap<String, CgmesPredicateDetails> map = ArrayListMultimap.create();
//
//        String ptId = newTwoWindingsTransformer.getId();
//
//        map.put("rdfType", new CgmesPredicateDetails("rdf:type", "_EQ", false, "cim:PowerTransformer"));
//
//        map.put("name", new CgmesPredicateDetails("cim:IdentifiedObject.name", "_EQ", false, name));
//
//        String substationId = newTwoWindingsTransformer.getSubstation().getId();
//        map.put("equipmentContainer", new CgmesPredicateDetails(
//            "cim:Equipment.EquipmentContainer", "_EQ", true, substationId));
//
//        /**
//         * PowerTransformerEnd1
//         */
//        map.put("rdfTypeEnd1", new CgmesPredicateDetails("rdf:type", "_EQ", false, "cim:PowerTransformerEnd", idEnd1));
//
//        map.put("powerTransformerEnd1", new CgmesPredicateDetails(
//            "cim:PowerTransformerEnd.PowerTransformer", "_EQ", true, ptId, idEnd1));
//
//        map.put("end1Type", new CgmesPredicateDetails(
//            "cim:TransformerEnd.endNumber", "_EQ", false, "1", idEnd1));
//
//        double r0 = newTwoWindingsTransformer.getR();
//        double x0 = newTwoWindingsTransformer.getX();
//        double b0 = newTwoWindingsTransformer.getB();
//        double g0 = newTwoWindingsTransformer.getG();
//        double r2 = 0;
//        double x2 = 0;
//        double b2 = 0;
//        double g2 = 0;
//        double ratedU1 = newTwoWindingsTransformer.getRatedU1();
//        double ratedU2 = newTwoWindingsTransformer.getRatedU2();
//        double rho0 = ratedU2 / ratedU1;
//        double rho0Square = rho0 * rho0;
//        double r1 = (r0 - r2) / rho0Square;
//        double x1 = (x0 - x2) / rho0Square;
//        double b1 = (b0 + b2) * rho0Square;
//        double g1 = (g0 + g2) * rho0Square;
//
//        if (!String.valueOf(b1).equals("NaN")) {
//            map.put("b", new CgmesPredicateDetails(
//                "cim:PowerTransformerEnd.b", "_EQ", false, String.valueOf(b1), idEnd1));
//        }
//
//        if (!String.valueOf(r1).equals("NaN")) {
//            map.put("r", new CgmesPredicateDetails(
//                "cim:PowerTransformerEnd.r", "_EQ", false, String.valueOf(r1), idEnd1));
//        }
//
//        if (!String.valueOf(x1).equals("NaN")) {
//            map.put("x", new CgmesPredicateDetails(
//                "cim:PowerTransformerEnd.x", "_EQ", false, String.valueOf(x1), idEnd1));
//        }
//
//        if (!String.valueOf(g1).equals("NaN")) {
//            map.put("g", new CgmesPredicateDetails(
//                "cim:PowerTransformerEnd.g", "_EQ", false, String.valueOf(g1), idEnd1));
//        }
//
////        ratedU1 = newTwoWindingsTransformer.getRatedU1();
//        if (!String.valueOf(ratedU1).equals("NaN")) {
//            map.put("ratedU1", new CgmesPredicateDetails(
//                "cim:PowerTransformerEnd.ratedU", "_EQ", false, String.valueOf(ratedU1), idEnd1));
//        }
//        /**
//         * PowerTransformerEnd2
//         */
//        map.put("rdfTypeEnd2", new CgmesPredicateDetails("rdf:type", "_EQ", false, "cim:PowerTransformerEnd", idEnd2));
//
//        map.put("powerTransformerEnd2", new CgmesPredicateDetails(
//            "cim:PowerTransformerEnd.PowerTransformer", "_EQ", true, ptId, idEnd2));
//
//        map.put("end2Type", new CgmesPredicateDetails(
//            "cim:TransformerEnd.endNumber", "_EQ", false, "2", idEnd2));
//
//        map.put("bEnd2", new CgmesPredicateDetails(
//            "cim:PowerTransformerEnd.b", "_EQ", false, String.valueOf(b2), idEnd2));
//
//        map.put("rEnd2", new CgmesPredicateDetails(
//            "cim:PowerTransformerEnd.r", "_EQ", false, String.valueOf(r2), idEnd2));
//
//        map.put("xEnd2", new CgmesPredicateDetails(
//            "cim:PowerTransformerEnd.x", "_EQ", false, String.valueOf(x2), idEnd2));
//
//        map.put("gEnd2", new CgmesPredicateDetails(
//            "cim:PowerTransformerEnd.g", "_EQ", false, String.valueOf(g2), idEnd2));
//
////        ratedU2 = newTwoWindingsTransformer.getRatedU2();
//        if (!String.valueOf(ratedU2).equals("NaN")) {
//            map.put("ratedU2", new CgmesPredicateDetails(
//                "cim:PowerTransformerEnd.ratedU", "_EQ", false, String.valueOf(ratedU2), idEnd2));
//        }
//        /**
//         * PhaseTapChanger
//         */
//        if (newTwoWindingsTransformer.getPhaseTapChanger() != null) {
//            PhaseTapChangerToPhaseTapChanger phtc = new PhaseTapChangerToPhaseTapChanger(change, cgmes);
//            map.putAll(phtc.converter());
//        }
//        /**
//         * RatioTapChanger
//         */
//        if (newTwoWindingsTransformer.getRatioTapChanger() != null) {
//            RatioTapChangerToRatioTapChanger rttc = new RatioTapChangerToRatioTapChanger(change, cgmes);
//            map.putAll(rttc.converter());
//        }
//        return map;
//    }
//
//    /**
//     * Check if TransformerEnd elements already exist in grid, if yes - returns the
//     * id.
//     *
//     */
//    private Map<String, String> getEndsId() {
//        PropertyBags transformerEnds = cgmes.transformerEnds();
//        Map<String, String> ids = new HashMap<>();
//        Iterator i = transformerEnds.iterator();
//
//        while (i.hasNext()) {
//            PropertyBag pb = (PropertyBag) i.next();
//            String windingType = pb.get("endNumber");
//            if (pb.getId(POWER_TRANSFORMER).equals(currId)
//                && windingType.equals("1")) {
//                idEnd1 = pb.getId(TRANSFORMER_END);
//                ids.put(ID_END1, idEnd1);
//
//            } else if (pb.getId(POWER_TRANSFORMER).equals(currId)
//                && windingType.equals("2")) {
//                idEnd2 = pb.getId(TRANSFORMER_END);
//                ids.put(ID_END2, idEnd2);
//            } else {
//                continue;
//            }
//        }
//        return ids;
//    }
//
//    /**
//     * Check if TapChanger elements already exist in grid, if yes - returns the id.
//     * TapChanger is on End1;
//     */
//    private String getTapChangerId(String tapChangerType) {
//        PropertyBags tapChangers = (tapChangerType.equals(RATIO_TAP_CHANGER)) ? cgmes.ratioTapChangers()
//            : cgmes.phaseTapChangers();
//        Iterator i = tapChangers.iterator();
//
//        while (i.hasNext()) {
//            PropertyBag pb = (PropertyBag) i.next();
//            if (pb.getId(TRANSFORMER_END).equals(idEnd1)) {
//                return pb.getId(tapChangerType);
//            } else {
//                continue;
//            }
//        }
//        return UUID.randomUUID().toString();
//    }
//
//    /**
//     * Check if TapChangerTable elements already exist in grid, if yes - returns the
//     * id. Otherwise, return concatenated string.
//     */
//    private String getTapChangerTableId(String tapChangerId, String tapChangerType) {
//
//        String propertyName = tapChangerType.concat("Table");
//        PropertyBags tapChangers = (tapChangerType.equals(RATIO_TAP_CHANGER)) ? cgmes.ratioTapChangers()
//            : cgmes.phaseTapChangers();
//        Iterator i = tapChangers.iterator();
//        while (i.hasNext()) {
//            PropertyBag pb = (PropertyBag) i.next();
//            if (pb.getId(tapChangerType).equals(tapChangerId) && pb.containsKey(propertyName)) {
//
//                return pb.getId(propertyName);
//            } else {
//                continue;
//            }
//        }
//        return UUID.randomUUID().toString();
//    }
//
//    private String getTapChangerTablePointId(String tapChangerType) {
//
//        String propertyName = tapChangerType.concat("TablePoint");
//        PropertyBags phaseTapChangerTable = (tapChangerType.equals(RATIO_TAP_CHANGER))
//            ? cgmes.ratioTapChangerTable(idRTCTable)
//            : cgmes.phaseTapChangerTable(idPTCTable);
//        Iterator i = phaseTapChangerTable.iterator();
//        while (i.hasNext()) {
//            PropertyBag pb = (PropertyBag) i.next();
//            if (pb.getId("step").equals("check")) {
//                return pb.getId(propertyName);
//            } else {
//                continue;
//            }
//        }
//        return UUID.randomUUID().toString();
//    }
//
//    private IidmChange change;
//    private CgmesModel cgmes;
//    TwoWindingsTransformer newTwoWindingsTransformer;
//    String currId;
//    String name;
//    String idEnd1;
//    String idEnd2;
//    String idRTC;
//    String idPTC;
//    String idPTCTable;
//    String idRTCTable;
//    String idPTCTablePoint;
//    String idRTCTablePoint;
//}