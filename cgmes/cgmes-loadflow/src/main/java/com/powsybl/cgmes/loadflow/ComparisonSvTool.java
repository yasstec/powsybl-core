/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.loadflow;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.tools.ConversionToolUtils;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

import static com.powsybl.iidm.tools.ConversionToolUtils.readProperties;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
@AutoService(Tool.class)
public class ComparisonSvTool implements Tool {

    private static final String CASE_FILE = "case-file";
    private static final String SV_FOLDER = "sv-folder";
    private static final String BASE_NAME = "base-name";
    private static final String OUTPUT_FILE = "output-file";

    private static final String V = "v";
    private static final String ANGLE = "angle";
    private static final String TOPOLOGICAL_NODE = "TopologicalNode";

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "compare-sv";
            }

            @Override
            public String getTheme() {
                return "CGMES validation";
            }

            @Override
            public String getDescription() {
                return "Compare SV files";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(CASE_FILE)
                        .desc("the case path")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(SV_FOLDER)
                        .desc("the folder containing sv files to be compared")
                        .hasArg()
                        .argName("FOLDER")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(BASE_NAME)
                        .desc("the file name")
                        .hasArg()
                        .argName("FILE BASE NAME")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FILE)
                        .desc("the output file path")
                        .hasArg()
                        .argName("OUTPUT FILE")
                        .required()
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    static class Triplet {
        String voltageLevelId = "";
        String busId = "";
        String equipmentId = "";
        int connectedComponentNumber = 0;
        double v1 = Double.NaN;
        double v2 = Double.NaN;
        double v3 = Double.NaN;
        double angle1 = Double.NaN;
        double angle2 = Double.NaN;
        double angle3 = Double.NaN;
    }

    private static void addFile(Path folder, String file, Map<String, Triplet> results, BiConsumer<Triplet, PropertyBag> consumer) {
        ReadOnlyDataSource ds = new FileDataSource(folder, file);
        CgmesModelTripleStore cgmes = CgmesModelFactory.create(ds, "rdf4j");
        PropertyBags bags = cgmes.namedQuery("voltages");
        for (PropertyBag p : bags) {
            consumer.accept(results.computeIfAbsent(p.getId(TOPOLOGICAL_NODE), s -> new Triplet()), p);
        }
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws IOException {
        Map<String, Triplet> results = new HashMap<>();

        if (line.hasOption(CASE_FILE)) {
            Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE));
            context.getOutputStream().println("Loading network '" + caseFile + "'");
            Properties inputParams = readProperties(line, ConversionToolUtils.OptionType.IMPORT, context);
            Network network = Importers.loadNetwork(caseFile, context.getShortTimeExecutionComputationManager(), new ImportConfig(), inputParams);
            if (network == null) {
                throw new PowsyblException("Case '" + caseFile + "' not found");
            }
            CgmesModelExtension cgmesModelExtension = network.getExtension(CgmesModelExtension.class);
            for (PropertyBag p : cgmesModelExtension.getCgmesModel().topologicalNodes()) {
                Terminal t = cgmesModelExtension.getConversion().getContext().terminalMapping().findFromTopologicalNode(p.getId(TOPOLOGICAL_NODE));
                if (t != null) {
                    Bus bus = t.getBusBreakerView().getConnectableBus();
                    if (bus != null) {
                        results.computeIfAbsent(p.getId(TOPOLOGICAL_NODE), s -> {
                            Triplet triplet = new Triplet();
                            triplet.voltageLevelId = t.getVoltageLevel().getId();
                            triplet.busId = t.getBusBreakerView().getConnectableBus().getId();
                            triplet.equipmentId = t.getConnectable().getId();
                            triplet.connectedComponentNumber = bus.getConnectedComponent() != null ? bus.getConnectedComponent().getNum() : 0;
                            return triplet;
                        });
                    }
                }
            }
        }

        String folder = line.getOptionValue(SV_FOLDER);
        String baseName = line.getOptionValue(BASE_NAME);
        addFile(Paths.get(folder).resolve("1"), baseName, results, (triplet, p) -> {
            triplet.v1 = p.asDouble(V);
            triplet.angle1 = p.asDouble(ANGLE);
        });
        addFile(Paths.get(folder).resolve("2"), baseName, results, (triplet, p) -> {
            triplet.v2 = p.asDouble(V);
            triplet.angle2 = p.asDouble(ANGLE);
        });
        addFile(Paths.get(folder).resolve("3"), baseName, results, (triplet, p) -> {
            triplet.v3 = p.asDouble(V);
            triplet.angle3 = p.asDouble(ANGLE);
        });

        try (Writer writer = Files.newBufferedWriter(context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE)), StandardCharsets.UTF_8)) {
            try (TableFormatter formatter = new CsvTableFormatterFactory().create(writer,
                    "Results",
                    TableFormatterConfig.load(),
                    new Column("Topological Node"),
                    new Column("VL ID"),
                    new Column("IIDM bus ID"),
                    new Column("Equipment ID"),
                    new Column("numcnx"),
                    new Column(V),
                    new Column("v_after_import"),
                    new Column("v_after_lf"),
                    new Column(ANGLE),
                    new Column("angle_after_import"),
                    new Column("angle_after_lf"),
                    new Column("diff_v_after_import"),
                    new Column("diff_v_after_lf"),
                    new Column("diff_angle_after_import"),
                    new Column("diff_angle_after_lf"))) {
                results.forEach((id, triplet) -> {
                    try {
                        formatter.writeCell(id);
                        formatter.writeCell(triplet.voltageLevelId);
                        formatter.writeCell(triplet.busId);
                        formatter.writeCell(triplet.equipmentId);
                        formatter.writeCell(triplet.connectedComponentNumber);
                        formatter.writeCell(triplet.v1);
                        formatter.writeCell(triplet.v2);
                        formatter.writeCell(triplet.v3);
                        formatter.writeCell(triplet.angle1);
                        formatter.writeCell(triplet.angle2);
                        formatter.writeCell(triplet.angle3);
                        formatter.writeCell(triplet.v2 - triplet.v1);
                        formatter.writeCell(triplet.v3 - triplet.v1);
                        formatter.writeCell(triplet.angle2 - triplet.angle1);
                        formatter.writeCell(triplet.angle3 - triplet.angle1);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
    }
}
