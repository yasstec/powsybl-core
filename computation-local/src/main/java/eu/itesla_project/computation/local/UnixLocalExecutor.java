/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.computation.local;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class UnixLocalExecutor implements LocalExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnixLocalExecutor.class);

    @Override
    public int execute(String program, List<String> args, Path outFile, Path errFile, Path workingDir, Map<String, String> env) throws IOException, InterruptedException {
        // set TMPDIR to working dir to avoid issue with /tmp
        Map<String, String> env2 = ImmutableMap.<String, String>builder()
                .putAll(env)
                .put("TMPDIR", workingDir.toAbsolutePath().toString())
                .build();

        StringBuilder internalCmd = new StringBuilder();
        for (Map.Entry<String, String> entry : env2.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            internalCmd.append("export ").append(name).append("=").append(value);
            if (name.endsWith("PATH")) {
                internalCmd.append(File.pathSeparator).append("$").append(name);
            }
            internalCmd.append("; ");
        }
        internalCmd.append(program);
        for (String arg : args) {
            internalCmd.append(" \"").append(arg).append("\"");
        }

        List<String> cmdLs = ImmutableList.<String>builder()
                .add("bash")
                .add("-c")
                .add(internalCmd.toString())
                .build();
        ProcessBuilder.Redirect outRedirect = ProcessBuilder.Redirect.appendTo(outFile.toFile());
        ProcessBuilder.Redirect errRedirect = ProcessBuilder.Redirect.appendTo(errFile.toFile());
        Process process = new ProcessBuilder(cmdLs)
                .directory(workingDir.toFile())
                .redirectOutput(outRedirect)
                .redirectError(errRedirect)
                .start();
        int exitValue = process.waitFor();

        // to avoid 'two many open files' exception
        process.getInputStream().close();
        process.getOutputStream().close();
        process.getErrorStream().close();

        if (exitValue != 0) {
            LOGGER.debug("Command '{}' has failed (exitValue={})", cmdLs, exitValue);
        }
        return exitValue;
    }
}
