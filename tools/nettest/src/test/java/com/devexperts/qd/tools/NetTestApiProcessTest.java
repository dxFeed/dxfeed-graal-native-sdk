// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * NetTestApi as a process started through QD's {@code Tools}: the statistics lines, the exit codes and the stop.
 * The output is checked for structure, never for rates.
 */
public class NetTestApiProcessTest {
    private static final Pattern TOTAL_LINE = Pattern.compile(
        "\\{\\*NetTestApiProducerSide-NetTest\\*} data [\\d,]+ rps, lag [\\d,]+ us, instances 1, connections \\d+");
    private static final Pattern INSTANCE_LINE = Pattern.compile("\\n\\s+NetTest\\.1 data [\\d,]+ rps, lag [\\d,]+ us");
    private static final long DEADLINE_SECONDS = 30;

    @Test
    public void durationEndsTheRunWithZeroAfterPrintingStatistics() throws Exception {
        ToolProcess process = ToolProcess.start("p", ":0", "-S", "100", "-r", "*/100k", "--duration", "3s",
            "-s", "1s");

        assertEquals(process.describe(), 0, process.waitForExit());
        assertTrue(process.describe(), TOTAL_LINE.matcher(process.output()).find());
        assertTrue(process.describe(), INSTANCE_LINE.matcher(process.output()).find());
        assertTrue(process.describe(), process.output().contains("Closed 1 endpoint(s)"));
    }

    @Test
    public void invalidOptionExitsWithTwo() throws Exception {
        ToolProcess process = ToolProcess.start("p", ":0", "-S", "100", "--legacy");

        assertEquals(process.describe(), 2, process.waitForExit());
        assertTrue(process.describe(), process.stderr().contains("NetTestApi: --legacy is an option of the C tool"));
    }

    @Test
    public void noArgumentsPrintTheUsageAndExitWithTwo() throws Exception {
        ToolProcess process = ToolProcess.start();

        assertEquals(process.describe(), 2, process.waitForExit());
        assertTrue(process.describe(), process.stderr().contains("p <address>"));
        assertTrue(process.describe(), process.stderr().contains("--duration"));
    }

    @Test
    public void busyPortExitsWithOneWithinSeconds() throws Exception {
        try (ServerSocket occupant = new ServerSocket(0)) {
            long start = System.nanoTime();
            ToolProcess process = ToolProcess.start("p", ":" + occupant.getLocalPort(), "-S", "1");

            assertEquals(process.describe(), 1, process.waitForExit());
            assertTrue(process.describe(), process.stderr().contains("did not start in 3000 ms"));
            assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 20);
        }
    }

    @Test
    public void interruptAfterTheFirstStatisticsExitsWithZero() throws Exception {
        assumeFalse("SIGINT cannot be sent to a process on Windows; checked by hand with Ctrl+C",
            System.getProperty("os.name").startsWith("Windows"));
        ToolProcess process = ToolProcess.start("p", ":0", "-S", "100", "-r", "*/100k", "-s", "1s");
        process.awaitOutput(TOTAL_LINE);

        // The kill of the shell: a slim image may have no /usr/bin/kill.
        Process kill = new ProcessBuilder("/bin/sh", "-c", "kill -INT " + process.process.pid()).inheritIO().start();
        assertEquals(0, kill.waitFor());

        assertEquals(process.describe(), 0, process.waitForExit());
        assertTrue(process.describe(), process.output().contains("Closed 1 endpoint(s)"));
    }

    /** The tool in a child JVM with the test classpath; stdout and stderr are drained in threads. */
    private static final class ToolProcess {
        final Process process;
        private final StringBuffer stdout = new StringBuffer();
        private final StringBuffer stderr = new StringBuffer();
        private final List<Thread> drains = new ArrayList<>();

        private ToolProcess(Process process) {
            this.process = process;
            drains.add(drain(process.getInputStream(), stdout));
            drains.add(drain(process.getErrorStream(), stderr));
        }

        static ToolProcess start(String... toolArgs) throws IOException {
            List<String> command = new ArrayList<>(Arrays.asList(
                Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                "com.devexperts.qd.tools.Tools", "NetTestApi"));
            command.addAll(Arrays.asList(toolArgs));
            ProcessBuilder builder = new ProcessBuilder(command).directory(new File("."));
            return new ToolProcess(builder.start());
        }

        int waitForExit() throws InterruptedException {
            if (!process.waitFor(DEADLINE_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError("no exit in " + DEADLINE_SECONDS + " s\n" + describe());
            }
            for (Thread drain : drains) {
                drain.join(5_000);
            }
            return process.exitValue();
        }

        void awaitOutput(Pattern pattern) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS);
            while (!pattern.matcher(output()).find()) {
                if (System.nanoTime() - deadline > 0 || !process.isAlive()) {
                    process.destroyForcibly();
                    throw new AssertionError("no " + pattern + "\n" + describe());
                }
                Thread.sleep(50);
            }
        }

        // QD logs to stderr when stdout is not a console.
        String output() {
            return stdout + "\n" + stderr;
        }

        String stderr() {
            return stderr.toString();
        }

        String describe() {
            return "stdout:\n" + stdout + "\nstderr:\n" + stderr;
        }

        private static Thread drain(InputStream stream, StringBuffer target) {
            Thread thread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)))
                {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        target.append(line).append('\n');
                    }
                } catch (IOException e) {
                    target.append("[drain failed: ").append(e).append("]\n");
                }
            }, "drain");
            thread.setDaemon(true);
            thread.start();
            return thread;
        }
    }
}
