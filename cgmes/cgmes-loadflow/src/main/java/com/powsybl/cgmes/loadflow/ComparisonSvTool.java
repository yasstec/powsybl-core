/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.loadflow;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;
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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
@AutoService(Tool.class)
public class ComparisonSvTool implements Tool {

    private static final String SV_FOLDER = "sv-folder";
    private static final String BASE_NAME = "base-name";
    private static final String OUTPUT_FILE = "output-file";

    private static final String V = "v";
    private static final String ANGLE = "angle";

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
        double v1 = Double.NaN;
        double v2 = Double.NaN;
        double v3 = Double.NaN;
        double angle1 = Double.NaN;
        double angle2 = Double.NaN;
        double angle3 = Double.NaN;
    }

    private static void addFile(String folder, String file, Map<String, Triplet> results, BiConsumer<Triplet, PropertyBag> consumer) {
        ReadOnlyDataSource ds = new FileDataSource(Paths.get(folder), file);
        CgmesModelTripleStore cgmes = CgmesModelFactory.create(ds, "rdf4j");
        PropertyBags bags = cgmes.namedQuery("voltages");
        for (PropertyBag p : bags) {
            consumer.accept(results.computeIfAbsent(p.getId("TopologicalNode"), s -> new Triplet()), p);
        }
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws IOException {
        Map<String, Triplet> results = new HashMap<>();

        String folder = line.getOptionValue(SV_FOLDER);
        String baseName = line.getOptionValue(BASE_NAME);
        addFile(folder, baseName, results, (triplet, p) -> {
            triplet.v1 = p.asDouble(V);
            triplet.angle1 = p.asDouble(ANGLE);
        });
        addFile(folder, baseName + "_before_lf", results, (triplet, p) -> {
            triplet.v2 = p.asDouble(V);
            triplet.angle2 = p.asDouble(ANGLE);
        });
        addFile(folder, baseName + "_after_lf", results, (triplet, p) -> {
            triplet.v3 = p.asDouble(V);
            triplet.angle3 = p.asDouble(ANGLE);
        });

        try (Writer writer = Files.newBufferedWriter(context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE)), StandardCharsets.UTF_8)) {
            try (TableFormatter formatter = new CsvTableFormatterFactory().create(writer,
                    "Results",
                    TableFormatterConfig.load(),
                    new Column("Topological Node"),
                    new Column(V),
                    new Column("v_after_import"),
                    new Column("v_after_lf"),
                    new Column(ANGLE),
                    new Column("angle_after_import"),
                    new Column("angle_after_lf"))) {
                results.forEach((id, triplet) -> {
                    try {
                        formatter.writeCell(id);
                        formatter.writeCell(triplet.v1);
                        formatter.writeCell(triplet.v2);
                        formatter.writeCell(triplet.v3);
                        formatter.writeCell(triplet.angle1);
                        formatter.writeCell(triplet.angle2);
                        formatter.writeCell(triplet.angle3);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
    }
}
