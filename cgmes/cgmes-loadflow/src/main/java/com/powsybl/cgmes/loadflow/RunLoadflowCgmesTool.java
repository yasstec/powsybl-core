package com.powsybl.cgmes.loadflow; /**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.commons.io.table.*;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.util.Networks;
import com.powsybl.iidm.tools.ConversionToolUtils;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.powsybl.loadflow.json.LoadFlowResultSerializer;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import static com.powsybl.iidm.tools.ConversionToolUtils.*;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
@AutoService(Tool.class)
public class RunLoadflowCgmesTool implements Tool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunLoadflowCgmesTool.class);

    private static final String CASE_FILE = "case-file";
    private static final String PARAMETERS_FILE = "parameters-file";
    private static final String OUTPUT_FILE = "output-file";
    private static final String OUTPUT_FORMAT = "output-format";
    private static final String SKIP_POSTPROC = "skip-postproc";
    private static final String OUTPUT_CASE_FORMAT = "output-case-format";
    private static final String OUTPUT_CASE_FILE = "output-case-file";
    private static final String OUTPUT_SV_FILE = "output-sv-file";

    private static final String DEPENDING_MODEL = "    <md:Model.DependentOn rdf:resource=\"%s\"/>%n";
    private static final String FULL_MODEL = "FullModel";
    private static final String PROFILE = "profile";

    private enum Format {
        CSV,
        JSON
    }

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "cgmes-loadflow";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "Run loadflow";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(CASE_FILE)
                        .desc("the case path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(PARAMETERS_FILE)
                        .desc("loadflow parameters as JSON file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FILE)
                        .desc("loadflow results output path")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FORMAT)
                        .desc("loadflow results output format " + Arrays.toString(Format.values()))
                        .hasArg()
                        .argName("FORMAT")
                        .build());
                options.addOption(Option.builder().longOpt(SKIP_POSTPROC)
                        .desc("skip network importer post processors (when configured)")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_CASE_FORMAT)
                        .desc("modified network output format " + Exporters.getFormats())
                        .hasArg()
                        .argName("CASEFORMAT")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_CASE_FILE)
                        .desc("modified network base name")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_SV_FILE)
                        .desc("location for generated SV file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(createImportParametersFileOption());
                options.addOption(createImportParameterOption());
                options.addOption(createExportParametersFileOption());
                options.addOption(createExportParameterOption());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE));
        boolean skipPostProc = line.hasOption(SKIP_POSTPROC);
        Path outputFile = null;
        Format format = null;
        Path outputCaseFile = null;
        String outputSvFile = null;
        ComponentDefaultConfig defaultConfig = ComponentDefaultConfig.load();

        ImportConfig importConfig = (!skipPostProc) ? ImportConfig.load() : new ImportConfig();
        // process a single network: output-file/output-format options available
        if (line.hasOption(OUTPUT_FILE)) {
            outputFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE));
            if (!line.hasOption(OUTPUT_FORMAT)) {
                throw new ParseException("Missing required option: " + OUTPUT_FORMAT);
            }
            format = Format.valueOf(line.getOptionValue(OUTPUT_FORMAT));
        }

        if (line.hasOption(OUTPUT_CASE_FILE)) {
            outputCaseFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_CASE_FILE));
            if (!line.hasOption(OUTPUT_CASE_FORMAT)) {
                throw new ParseException("Missing required option: " + OUTPUT_CASE_FORMAT);
            }
        }

        if (line.hasOption(OUTPUT_SV_FILE)) {
            outputSvFile = line.getOptionValue(OUTPUT_SV_FILE);
        }

        context.getOutputStream().println("Loading network '" + caseFile + "'");
        Properties inputParams = readProperties(line, ConversionToolUtils.OptionType.IMPORT, context);
        Network network = Importers.loadNetwork(caseFile, context.getShortTimeExecutionComputationManager(), importConfig, inputParams);
        if (network == null) {
            throw new PowsyblException("Case '" + caseFile + "' not found");
        }

        if (outputSvFile != null) {
            try (Writer writer = Files.newBufferedWriter(context.getFileSystem().getPath(outputSvFile + "_before_lf.xml"), StandardCharsets.UTF_8)) {
                writeSvFile(network, "002", writer);
            }
        }

        LoadFlow loadFlow = defaultConfig.newFactoryImpl(LoadFlowFactory.class).create(network, context.getShortTimeExecutionComputationManager(), 0);

        LoadFlowParameters params = LoadFlowParameters.load();
        if (line.hasOption(PARAMETERS_FILE)) {
            Path parametersFile = context.getFileSystem().getPath(line.getOptionValue(PARAMETERS_FILE));
            JsonLoadFlowParameters.update(params, parametersFile);
        }

        LoadFlowResult result = loadFlow.run(network.getVariantManager().getWorkingVariantId(), params).join();

        if (outputFile != null) {
            exportResult(result, context, outputFile, format);
        } else {
            printResult(result, context);
        }

        // exports the modified network to the filesystem, if requested
        if (outputCaseFile != null) {
            String outputCaseFormat = line.getOptionValue(OUTPUT_CASE_FORMAT);
            Properties outputParams = readProperties(line, ConversionToolUtils.OptionType.EXPORT, context);
            Exporters.export(outputCaseFormat, network, outputParams, outputCaseFile);
        }

        if (outputSvFile != null && result.isOk()) {
            try (Writer writer = Files.newBufferedWriter(context.getFileSystem().getPath(outputSvFile + "_after_lf.xml"), StandardCharsets.UTF_8)) {
                writeSvFile(network, "003", writer);
            }
        }

        Networks.printBalanceSummary("", network, LOGGER);
    }

    private void writeSvFile(Network network, String version, Writer writer) throws IOException {
        CgmesModelExtension cgmesModelExtension = network.getExtension(CgmesModelExtension.class);

        if (cgmesModelExtension != null) {
            writerHeader(writer);
            writeFullModel(cgmesModelExtension, version, writer);
            writeAngleTension(network, writer);
            writer.write("</rdf:RDF>");
        }
    }

    private void writerHeader(Writer writer) throws IOException {
        writer.write("\uFEFF<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        writer.write("<rdf:RDF xmlns:cim=\"http://iec.ch/TC57/2013/CIM-schema-cim16#\"");
        writer.write(" xmlns:md=\"http://iec.ch/TC57/61970-552/ModelDescription/1#\" xmlns:entsoe=\"http://entsoe.eu/CIM/SchemaExtension/3/1#\"");
        writer.write(" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");
    }

    private void writeFullModel(CgmesModelExtension cgmesModelExtension, String version, Writer writer) throws IOException {
        CgmesModel cgmes = cgmesModelExtension.getCgmesModel();
        PropertyBags properties = cgmes.modelProfiles();

        String[] svProfile = new String[1];

        for (PropertyBag p : properties) {
            String tmp = p.get(PROFILE);
            if (tmp != null && tmp.contains("/StateVariables/")) {
                svProfile[0] = p.getId(FULL_MODEL);
            }
        }

        properties.removeIf(p -> p.getId(FULL_MODEL).equals(svProfile[0]));

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date date = new Date();

        writer.write("<md:FullModel rdf:about=\"" + svProfile[0] + "\">\n");
        writer.write("    <md:Model.created>" + dateFormat.format(date) + "</md:Model.created>\n");
        writer.write("    <neplan:Model.createdBy>powsybl</neplan:Model.createdBy>\n");
        writer.write("    <md:Model.scenarioTime>" + dateFormat.format(date) + "</md:Model.scenarioTime>\n");
        writer.write("    <md:Model.description>Generated by PowSyBl for tests</md:Model.description>\n");
        writer.write("    <md:Model.modelingAuthoritySet>powsybl</md:Model.modelingAuthoritySet>\n");
        writer.write("    <md:Model.profile>http://entsoe.eu/CIM/StateVariables/4/1</md:Model.profile>\n");
        writer.write("    <md:Model.version>" + version + "</md:Model.version>\n");
        writer.write(String.format(DEPENDING_MODEL, properties.stream().filter(p -> p.get(PROFILE) != null && p.get(PROFILE).contains("/EquipmentCore/")).findFirst().map(p -> p.getId(FULL_MODEL)).orElse("MISSING_EQ")));
        writer.write(String.format(DEPENDING_MODEL, properties.stream().filter(p -> p.get(PROFILE) != null && p.get(PROFILE).contains("/Topology/")).findFirst().map(p -> p.getId(FULL_MODEL)).orElse("MISSING_TP")));
        writer.write(String.format(DEPENDING_MODEL, properties.stream().filter(p -> p.get(PROFILE) != null && p.get(PROFILE).contains("/SteadyStateHypothesis/")).findFirst().map(p -> p.getId(FULL_MODEL)).orElse("MISSING_SSH")));
        writer.write("  </md:FullModel>\n");
    }

    private void writeAngleTension(Network network, Writer writer) throws IOException {
        int counter = 0;
        for (Bus bus : network.getBusBreakerView().getBuses()) {
            if (!Double.isNaN(bus.getV()) || !Double.isNaN(bus.getAngle())) {
                writer.write(String.format("  <cim:SvVoltage rdf:ID=\"%d\">%n", counter));
                writer.write(String.format("    <cim:SvVoltage.angle>%.2f</cim:SvVoltage.angle>%n", bus.getAngle()));
                writer.write(String.format("    <cim:SvVoltage.v>%.2f</cim:SvVoltage.v>%n", bus.getV()));
                writer.write(String.format("    <cim:SvVoltage.TopologicalNode rdf:resource=\"#%s\" />%n", bus.getId()));
                writer.write("  </cim:SvVoltage>\n");
                counter++;
            }
        }
    }

    private void printLoadFlowResult(LoadFlowResult result, Path outputFile, TableFormatterFactory formatterFactory,
                                     TableFormatterConfig formatterConfig) {
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            printLoadFlowResult(result, writer, formatterFactory, formatterConfig);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printLoadFlowResult(LoadFlowResult result, Writer writer, TableFormatterFactory formatterFactory,
                                     TableFormatterConfig formatterConfig) {
        try (TableFormatter formatter = formatterFactory.create(writer,
                "loadflow results",
                formatterConfig,
                new Column("Result"),
                new Column("Metrics"))) {
            formatter.writeCell(result.isOk());
            formatter.writeCell(result.getMetrics().toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printResult(LoadFlowResult result, ToolRunningContext context) {
        Writer writer = new OutputStreamWriter(context.getOutputStream());

        AsciiTableFormatterFactory asciiTableFormatterFactory = new AsciiTableFormatterFactory();
        printLoadFlowResult(result, writer, asciiTableFormatterFactory, TableFormatterConfig.load());
    }

    private void exportResult(LoadFlowResult result, ToolRunningContext context, Path outputFile, Format format) {
        context.getOutputStream().println("Writing results to '" + outputFile + "'");
        switch (format) {
            case CSV:
                CsvTableFormatterFactory csvTableFormatterFactory = new CsvTableFormatterFactory();
                printLoadFlowResult(result, outputFile, csvTableFormatterFactory, TableFormatterConfig.load());
                break;

            case JSON:
                LoadFlowResultSerializer.write(result, outputFile);
                break;
        }
    }

}
