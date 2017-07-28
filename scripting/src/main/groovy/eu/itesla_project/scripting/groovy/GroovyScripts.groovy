/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.scripting.groovy

import eu.itesla_project.afs.core.AfsException
import eu.itesla_project.afs.core.AppData
import eu.itesla_project.afs.core.ProjectFileExtension
import eu.itesla_project.afs.core.ProjectFolder
import org.codehaus.groovy.control.CompilerConfiguration

import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class GroovyScripts {

    static {
        ProjectFolder.metaClass.methodMissing = { String name, args ->
            if (name.endsWith("Builder") && name.size() > 7) {
                String projectFilePseudoClass = name.substring(0, name.length() - 7)
                ProjectFileExtension extension = delegate.getProject().getFileSystem().getData()
                        .getProjectFileExtensionByPseudoClass(projectFilePseudoClass)
                if (!extension) {
                    throw new AfsException("No extension found for project file pseudo class '"
                            + projectFilePseudoClass + "'");
                }
                delegate.fileBuilder(extension.getProjectFileBuilderClass())
            }
        }
    }

    static void run(Path file, AppData data) {
        run(file, data, null)
    }

    static void run(Path file, AppData data, Writer out) {
        run(file, data, new Binding(), out)
    }

    static void run(Reader codeReader, AppData data, Writer out) {
        run(codeReader, data, new Binding(), out)
    }

    static void run(Path file, AppData data, Binding binding, Writer out) {
        file.withReader(StandardCharsets.UTF_8.name(), { reader ->
            run(reader, data, binding, out)
        })
    }

    static void run(Reader codeReader, AppData data, Binding binding, Writer out) {
        run(codeReader, data, binding, ServiceLoader.load(GroovyScriptExtension.class), out)
    }

    static void run(Reader codeReader, AppData data, Iterable<GroovyScriptExtension> extensions, Writer out) {
        run(codeReader, data, new Binding(), extensions, out)
    }

    static void run(Reader codeReader, AppData data, Binding binding, Iterable<GroovyScriptExtension> extensions, Writer out) {
        assert codeReader
        assert data
        assert extensions != null

        CompilerConfiguration conf = new CompilerConfiguration()

        binding.afs = new AfsGroovyFacade(data)

        binding.computationManager = data.getComputationManager()

        if (out != null) {
            binding.out = out
        }

        // load extensions
        extensions.forEach { it.load(binding, data.getComputationManager()) }

        GroovyShell shell = new GroovyShell(binding, conf)
        shell.evaluate(codeReader)
    }
}
