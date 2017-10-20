/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.computation.mpi;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.io.FileUtil;
import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.computation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MpiComputationManager implements ComputationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpiComputationManager.class);

    private static final int LOG_DELAY = 10; // 10 s

    private static final int CHUNK_MAX_SIZE = (int) (100 * Math.pow(2, 20)); // 100 Mb

    private final Path localDir;

    private final MpiStatistics statistics;

    private final MpiExecutorContext executorContext;

    private final MpiJobScheduler scheduler;

    private Future<?> busyCoresPrintTask;

    public MpiComputationManager(Path localDir, MpiJobScheduler scheduler) {
        this(localDir, scheduler, new NoMpiStatistics(), new MpiExecutorContext());
    }

    public MpiComputationManager(Path localDir, MpiNativeServices nativeServices) throws IOException, InterruptedException {
        this(localDir, nativeServices, new NoMpiStatistics(), new MpiExecutorContext(), 1, false, null);
    }

    public MpiComputationManager(Path localDir, MpiStatistics statistics, MpiExecutorContext executorContext,
                                 int coresPerRank, boolean verbose, Path stdOutArchive) throws IOException, InterruptedException {
        this(localDir, new JniMpiNativeServices(), statistics, executorContext, coresPerRank, verbose, stdOutArchive);
    }

    public MpiComputationManager(Path localDir, MpiNativeServices nativeServices, MpiStatistics statistics,
                                 MpiExecutorContext executorContext, int coresPerRank, boolean verbose, Path stdOutArchive) throws IOException, InterruptedException {
        this(localDir, new MpiJobSchedulerImpl(nativeServices, statistics, coresPerRank, verbose, executorContext.getSchedulerExecutor(), stdOutArchive), statistics, executorContext);
    }

    public MpiComputationManager(Path localDir, MpiJobScheduler scheduler, MpiStatistics statistics, MpiExecutorContext executorContext) {
        this.localDir = Objects.requireNonNull(localDir);
        this.statistics = Objects.requireNonNull(statistics);
        this.executorContext = Objects.requireNonNull(executorContext);
        this.scheduler = scheduler;
        if (executorContext.getMonitorExecutor() != null) {
            busyCoresPrintTask = executorContext.getMonitorExecutor().scheduleAtFixedRate(
                () -> LOGGER.info("Busy cores {}/{}, {} tasks/s", scheduler.getResources().getBusyCores(),
                                                                  scheduler.getResources().getAvailableCores(),
                                                                  ((float) scheduler.getStartedTasksAndReset()) / LOG_DELAY),
                0, LOG_DELAY, TimeUnit.SECONDS);
        }
    }

    @Override
    public String getVersion() {
        return "MPI " + scheduler.getVersion();
    }

    @Override
    public Path getLocalDir() {
        return localDir;
    }

    @Override
    public OutputStream newCommonFile(final String fileName) throws IOException {
        // transfer the common file in fixed size chunks
        return new OutputStream() {

            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            private int chunk = 0;

            private void checkSize(boolean last) {
                if (last || buffer.size() > CHUNK_MAX_SIZE) {
                    scheduler.sendCommonFile(new CommonFile(fileName, buffer.toByteArray(), chunk++, last));
                    buffer.reset();
                }
            }

            @Override
            public void write(int b) throws IOException {
                buffer.write(b);
                checkSize(false);
            }

            @Override
            public void write(byte[] b) throws IOException {
                buffer.write(b);
                checkSize(false);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                buffer.write(b, off, len);
                checkSize(false);
            }

            @Override
            public void flush() throws IOException {
                buffer.flush();
            }

            @Override
            public void close() throws IOException {
                buffer.close();
                checkSize(true);
            }
        };
    }

    @Override
    public CommandExecutor newCommandExecutor(Map<String, String> env, String workingDirPrefix, boolean debug) throws Exception {
        Objects.requireNonNull(env);
        Objects.requireNonNull(workingDirPrefix);
        Path workingDir = Files.createTempDirectory(localDir, workingDirPrefix);
        return new CommandExecutor() {
            @Override
            public Path getWorkingDir() {
                return workingDir;
            }

            @Override
            public void start(CommandExecution execution, ExecutionListener listener) throws Exception {
                Objects.requireNonNull(execution);
                final int[] startCounter = new int[1];
                final Lock startLock = new ReentrantLock();
                final Condition zero = startLock.newCondition();
                scheduler.execute(execution, workingDir, env, new ExecutionListener() {

                    @Override
                    public void onExecutionStart(int fromExecutionIndex, int toExecutionIndex) {
                        if (listener != null) {
                            listener.onExecutionStart(fromExecutionIndex, toExecutionIndex);
                        }
                    }

                    @Override
                    public void onExecutionCompletion(int executionIndex) {
                        if (listener != null) {
                            listener.onExecutionCompletion(executionIndex);
                        }
                    }

                    @Override
                    public void onEnd(ExecutionReport report) {
                        startLock.lock();
                        try {
                            startCounter[0]--;
                            if (startCounter[0] == 0) {
                                zero.signal();
                            }
                        } finally {
                            startLock.unlock();
                        }
                        if (listener != null) {
                            listener.onEnd(report);
                        }
                    }

                });
                startLock.lock();
                try {
                    startCounter[0]++;
                } finally {
                    startLock.unlock();
                }
            }

            @Override
            public ExecutionReport start(CommandExecution execution) throws Exception {
                final CountDownLatch latch = new CountDownLatch(1);
                final ExecutionReport[] reports = new ExecutionReport[1];
                start(execution, new DefaultExecutionListener() {

                    @Override
                    public void onEnd(ExecutionReport report) {
                        reports[0] = report;
                        latch.countDown();
                    }
                });
                latch.await();
                return reports[0];
            }

            @Override
            public void close() throws Exception {
                if (!debug) {
                    FileUtil.removeDir(workingDir);
                }
            }
        };
    }

    @Override
    public <R> CompletableFuture<R> execute(final ExecutionEnvironment environment,
                                            final ExecutionHandler<R> handler) {
        Objects.requireNonNull(environment);
        Objects.requireNonNull(handler);

        class AsyncContext {

            WorkingDirectory workingDir;

            List<CommandExecution> parametersList;

            ExecutionReport report;

        }

        return CompletableFuture
                .completedFuture(new AsyncContext())
                .thenApplyAsync(ctxt -> {
                    try {
                        ctxt.workingDir = new WorkingDirectory(localDir, environment.getWorkingDirPrefix(), environment.isDebug());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    try {
                        ctxt.parametersList = handler.before(ctxt.workingDir.toPath());
                    } catch (Throwable t) {
                        try {
                            ctxt.workingDir.close();
                        } catch (IOException e2) {
                            LOGGER.error(e2.toString(), e2);
                        }
                        throw new PowsyblException(t);
                    }
                    return ctxt;
                }, executorContext.getComputationExecutor())
                .thenComposeAsync(ctxt -> {
                    if (ctxt.parametersList.isEmpty()) {
                        ctxt.report = new ExecutionReport(Collections.emptyList());
                        return CompletableFuture.completedFuture(ctxt);
                    } else {
                        CompletableFuture<ExecutionReport> last = null;
                        for (CommandExecution execution : ctxt.parametersList) {

                            ExecutionListener l = new DefaultExecutionListener() {

                                @Override
                                public void onExecutionStart(int fromExecutionIndex, int toExecutionIndex) {
                                    try {
                                        for (int executionIndex = fromExecutionIndex; executionIndex <= toExecutionIndex; executionIndex++) {
                                            handler.onExecutionStart(execution, executionIndex);
                                        }
                                    } catch (Exception e) {
                                        LOGGER.error(e.toString(), e);
                                    }
                                }

                                @Override
                                public void onExecutionCompletion(int executionIndex) {
                                    try {
                                        handler.onExecutionCompletion(execution, executionIndex);
                                    } catch (Exception e) {
                                        LOGGER.error(e.toString(), e);
                                    }
                                }
                            };

                            if (last == null) {
                                last = scheduler.execute(execution, ctxt.workingDir.toPath(), environment.getVariables(), l);
                            } else {
                                last = last.thenCompose(report -> {
                                    if (report.getErrors().isEmpty()) {
                                        return scheduler.execute(execution, ctxt.workingDir.toPath(), environment.getVariables(), l);
                                    } else {
                                        return CompletableFuture.completedFuture(report);
                                    }
                                });
                            }
                        }

                        return last.thenApply(report -> {
                            ctxt.report = report;
                            return ctxt;
                        });
                    }
                }, executorContext.getComputationExecutor())
                .thenApplyAsync(ctxt -> {
                    try {
                        return handler.after(ctxt.workingDir.toPath(), ctxt.report);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        try {
                            ctxt.workingDir.close();
                        } catch (IOException e2) {
                            LOGGER.error(e2.toString(), e2);
                        }
                    }
                }, executorContext.getComputationExecutor());
    }

    @Override
    public ComputationResourcesStatus getResourcesStatus() {
        return new MpiComputationResourcesStatus(scheduler.getResources());
    }

    @Override
    public Executor getExecutor() {
        return executorContext.getApplicationExecutor();
    }

    @Override
    public void close() {
        try {
            scheduler.shutdown();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        }
        try {
            statistics.close();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        }
        if (busyCoresPrintTask != null) {
            busyCoresPrintTask.cancel(true);
        }
        try {
            executorContext.shutdown();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        }
    }

}
