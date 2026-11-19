// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.devexperts.qd.qtp.socket.ServerSocketTestHelper;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.event.market.Trade;
import com.dxfeed.promise.Promise;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * NetTestApi as a process started through QD's {@code Tools}: the lines of the specification for both sides, the exit
 * codes, the stop, the summary file and the metrics. The output is checked for structure, never for rates. A
 * consumer process runs against a publisher in the test JVM, which listens on a port that the system picks.
 */
public class NetTestApiProcessTest {
    private static final Map<String, Pattern> REGEXES = NetTestKit.regexes();
    private static final Pattern PRODUCER_LINE = Pattern.compile("[{][*]NetTestProducerSide-NetTest[*][}]");
    private static final Pattern CONSUMER_LINE = Pattern.compile("[{][*]NetTestConsumerSide-NetTest[*][}]");
    private static final long DEADLINE_SECONDS = 30;

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void durationEndsTheRunWithZeroAfterPrintingStatisticsAndTheSummary() throws Exception {
        Path summary = folder.getRoot().toPath().resolve("producer.json");
        // The first tick only starts the count and the last block needs a second: 5 s leave a tick to spare.
        ToolProcess process = ToolProcess.start("p", ":0", "-S", "100", "-r", "*/100k", "--duration", "5s",
            "-s", "1s", "--summary", summary.toString(), "--metrics-port", "0");
        String url = process.awaitMetricsUrl();
        process.awaitOutput(PRODUCER_LINE);
        NetTestApiChecks.checkScrape(NetTestApiChecks.scrape(url), "producer", "NetTest");

        assertEquals(process.describe(), 0, process.waitForExit());
        assertBlocks(process, "Producer");
        assertTrue("the lines go to stdout\n" + process.describe(), PRODUCER_LINE.matcher(process.stdout()).find());
        assertTrue(process.describe(), process.output().contains("Closed 1 endpoint(s)"));
        JsonNode json = NetTestApiChecks.readSummary(summary);
        assertEquals("ok", json.get("status").asText());
        assertEquals(0, json.get("exit_code").asInt());
        assertEquals("producer", json.get("side").asText());
        assertTrue(json.toString(), json.get("records").asLong() > 0);
        assertTrue(json.toString(), json.get("schedule_slip").isObject());
        assertTrue(json.toString(), json.get("e2e_lag").isNull());
        assertTrue("the temporary file is renamed", !Files.exists(Paths.get(summary + ".tmp")));
    }

    @Test
    public void theConsumerPrintsItsLinesAndEndsWithZeroAfterTheSummary() throws Exception {
        Path summary = folder.getRoot().toPath().resolve("consumer.json");
        StartedProducer producer = startProducer("-c", "stream");
        try {
            ToolProcess process = ToolProcess.start("c", producer.address, "-S", "100", "-c", "stream", "-C", "2",
                "-s", "1s", "--duration", "5s", "--warmup", "1s", "--summary", summary.toString(),
                "--metrics-port", "0");
            String url = process.awaitMetricsUrl();
            process.awaitOutput(CONSUMER_LINE);
            String scrape = NetTestApiChecks.scrape(url);
            NetTestApiChecks.checkScrape(scrape, "consumer", "NetTest");
            assertTrue(scrape, scrape.contains(
                "nettest_records_total{tool=\"java-api\",side=\"consumer\",name=\"NetTest\",connection=\"2\"}"));

            assertEquals(process.describe(), 0, process.waitForExit());
            assertBlocks(process, "Consumer");
            assertTrue(process.describe(), process.output().contains("Closed 2 endpoint(s)"));
            JsonNode json = NetTestApiChecks.readSummary(summary);
            assertEquals("ok", json.get("status").asText());
            assertEquals(100, json.get("symbols_seen").asInt());
            assertEquals(1_000, json.get("config").get("warmup_ms").asInt());
            assertEquals("warmup", json.get("intervals").get(0).get("phase").asText());
            JsonNode last = json.get("intervals").get(json.get("intervals").size() - 1);
            assertEquals(json.toString(), "measure", last.get("phase").asText());
            assertTrue(json.toString(), json.get("e2e_lag").get("count").asLong() > 0);
            assertTrue(json.toString(), json.get("publish_call").isNull());
        } finally {
            producer.side.finish();
        }
    }

    @Test
    public void aBusyMetricsPortExitsWithOneBeforeTheFirstEndpoint() throws Exception {
        try (ServerSocket occupant = new ServerSocket(0, 50, java.net.InetAddress.getLoopbackAddress())) {
            ToolProcess process = ToolProcess.start("p", ":0", "-S", "1", "--metrics-port",
                "127.0.0.1:" + occupant.getLocalPort());

            assertEquals(process.describe(), 1, process.waitForExit());
            assertTrue(process.describe(), process.stderr().contains("NetTestApi: --metrics-port cannot bind"));
            assertTrue(process.describe(), process.output().contains("Closed 0 endpoint(s)"));
        }
    }

    @Test
    public void anUnwritableSummaryPathExitsWithTwo() throws Exception {
        String path = folder.getRoot().toPath().resolve("absent").resolve("summary.json").toString();
        ToolProcess process = ToolProcess.start("p", ":0", "-S", "1", "--summary", path);

        assertEquals(process.describe(), 2, process.waitForExit());
        assertTrue(process.describe(), process.stderr().contains("NetTestApi: --summary cannot write to the " +
            "directory of '" + path + "'"));
    }

    @Test
    public void aConsumerWithoutDataExitsWithOne() throws Exception {
        String connectorName = "NetTestApiProcessTest-" + UUID.randomUUID();
        @SuppressWarnings("deprecation")
        Promise<Integer> port = ServerSocketTestHelper.createPortPromise(connectorName);
        // A publisher with nothing to publish.
        DXEndpoint publisher = DXEndpoint.create(DXEndpoint.Role.PUBLISHER);
        try {
            publisher.connect(":0[name=" + connectorName + "]");
            Path summary = folder.getRoot().toPath().resolve("no-data.json");
            ToolProcess process = ToolProcess.start("-D" + NetTestApiConsumerSide.NO_DATA_BEFORE_RECORD_PROPERTY +
                "=2000", "c", "127.0.0.1:" + port.await(DEADLINE_SECONDS, TimeUnit.SECONDS), "-S", "100",
                "--summary", summary.toString());

            assertEquals(process.describe(), 1, process.waitForExit());
            assertTrue(process.describe(), process.stderr().contains("NetTestApi: no_data: no records for 2000 ms " +
                "after the connection"));
            JsonNode json = NetTestApiChecks.readSummary(summary);
            assertEquals("no_data", json.get("status").asText());
            assertEquals(1, json.get("exit_code").asInt());
            assertTrue(json.toString(), json.get("error").asText().startsWith("no_data: "));
        } finally {
            publisher.close();
        }
    }

    @Test
    public void aConsumerWhoseRecordsStopExitsWithOne() throws Exception {
        String connectorName = "NetTestApiProcessTest-" + UUID.randomUUID();
        @SuppressWarnings("deprecation")
        Promise<Integer> port = ServerSocketTestHelper.createPortPromise(connectorName);
        String symbol = new NetTestApiConsumerSide(NetTestApi.parseArguments("c", "127.0.0.1:1", "-S", "100"),
            new NetTestApi.Stop()).getSymbols()[0];
        // A publisher that publishes for 3 s and then keeps the connection quiet.
        DXEndpoint publisher = DXEndpoint.newBuilder().withRole(DXEndpoint.Role.PUBLISHER)
            .withProperty(NetTestApiSide.NANO_TIME_PROPERTY, "true").build();
        try {
            publisher.connect(":0[name=" + connectorName + "]");
            ToolProcess process = ToolProcess.start("-D" + NetTestApiConsumerSide.NO_DATA_AFTER_RECORD_PROPERTY +
                "=1500", "c", "127.0.0.1:" + port.await(DEADLINE_SECONDS, TimeUnit.SECONDS), "-S", "100");
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (System.nanoTime() - end < 0) {
                Trade trade = new Trade(symbol);
                trade.setTimeNanos(System.currentTimeMillis() * 1_000_000 + 1);
                publisher.getPublisher().publishEvents(List.of(trade));
                Thread.sleep(10);
            }

            assertEquals(process.describe(), 1, process.waitForExit());
            assertTrue(process.describe(), process.stderr().contains("NetTestApi: no_data: no records for 1500 ms " +
                "after the last record"));
        } finally {
            publisher.close();
        }
    }

    @Test
    public void aConsumerOfEventsWithoutTheNanosecondPartExitsWithOne() throws Exception {
        StartedProducer producer = startProducer("-D" + NetTestApiSide.NANO_TIME_PROPERTY + "=false");
        try {
            ToolProcess process = ToolProcess.start("c", producer.address, "-S", "100");

            assertEquals(process.describe(), 1, process.waitForExit());
            assertTrue(process.describe(), process.stderr().contains("NetTestApi: nano_missing: the first 1000 " +
                "events have no nanosecond part"));

            ToolProcess unchecked = ToolProcess.start("c", producer.address, "-S", "100", "--duration", "3s",
                "-D" + NetTestApiSide.NANO_TIME_PROPERTY + "=false");

            assertEquals(unchecked.describe(), 0, unchecked.waitForExit());
        } finally {
            producer.side.finish();
        }
    }

    @Test
    public void invalidOptionExitsWithTwo() throws Exception {
        ToolProcess process = ToolProcess.start("c", "127.0.0.1:1", "-S", "100", "--batch", "10");

        assertEquals(process.describe(), 2, process.waitForExit());
        assertTrue(process.describe(), process.stderr().contains("NetTestApi: --batch is an option of the mode p"));
    }

    @Test
    public void helpPrintsTheSpecificationVersionAndExitsWithZero() throws Exception {
        ToolProcess process = ToolProcess.start("p", ":1", "--help");

        assertEquals(process.describe(), 0, process.waitForExit());
        assertTrue(process.describe(), process.stdout().contains("spec_version nettest/1"));
    }

    @Test
    public void noArgumentsPrintTheUsageAndExitWithTwo() throws Exception {
        ToolProcess process = ToolProcess.start();

        assertEquals(process.describe(), 2, process.waitForExit());
        assertTrue(process.describe(), process.stderr().contains("p|c <address>"));
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
    public void interruptAfterTheFirstStatisticsExitsWithZeroAndTheSummaryOfTheSignal() throws Exception {
        assumeFalse("SIGINT cannot be sent to a process on Windows; checked by hand with Ctrl+C",
            System.getProperty("os.name").startsWith("Windows"));
        Path summary = folder.getRoot().toPath().resolve("signal.json");
        ToolProcess process = ToolProcess.start("p", ":0", "-S", "100", "-r", "*/100k", "-s", "1s", "--summary",
            summary.toString());
        process.awaitOutput(PRODUCER_LINE);

        // The kill of the shell: a slim image may have no /usr/bin/kill.
        Process kill = new ProcessBuilder("/bin/sh", "-c", "kill -INT " + process.process.pid()).inheritIO().start();
        assertEquals(0, kill.waitFor());

        assertEquals(process.describe(), 0, process.waitForExit());
        assertTrue(process.describe(), process.output().contains("Closed 1 endpoint(s)"));
        JsonNode json = NetTestApiChecks.readSummary(summary);
        assertEquals("stopped_by_signal", json.get("status").asText());
        assertEquals(0, json.get("exit_code").asInt());
    }

    /**
     * Checks the statistics blocks of the side: each summary line with the label is followed by a line per instance
     * and the runtime line, all matching the expressions of the kit, and the values of the lines add up.
     */
    private static void assertBlocks(ToolProcess process, String side) {
        String[] lines = process.output().split("\n");
        int blocks = 0;
        for (int i = 0; i < lines.length; i++) {
            Matcher summary = REGEXES.get("summary").matcher(lines[i]);
            if (!summary.find()) {
                continue;
            }
            assertEquals(lines[i], side, summary.group(1));
            assertEquals(lines[i], "NetTest", summary.group(2));
            int instances = Integer.parseInt(summary.group(5));
            for (int k = 1; k <= instances; k++) {
                Matcher instance = REGEXES.get("instance").matcher(lines[i + k]);
                assertTrue(process.describe(), instance.find());
                assertEquals("NetTest", instance.group(1));
                assertEquals(String.valueOf(k), instance.group(2));
            }
            assertTrue(process.describe(), NetTestKit.matches(REGEXES.get("runtime"), lines[i + instances + 1]));
            blocks++;
        }
        assertTrue("blocks " + blocks + "\n" + process.describe(), blocks >= 2);
    }

    /** A producer in the test JVM on a port that the system picks; the -D options are restored after its start. */
    private static StartedProducer startProducer(String... options) throws Exception {
        String connectorName = "NetTestApiProcessTest-" + UUID.randomUUID();
        @SuppressWarnings("deprecation")
        Promise<Integer> port = ServerSocketTestHelper.createPortPromise(connectorName);
        List<String> args = new ArrayList<>(Arrays.asList(options));
        args.addAll(Arrays.asList("p", ":0[name=" + connectorName + "]", "-S", "100", "-r", "*/20k"));
        String before = System.getProperty(NetTestApiSide.NANO_TIME_PROPERTY);
        NetTestApiProducerSide side;
        try {
            side = new NetTestApiProducerSide(NetTestApi.parseArguments(args.toArray(new String[0])),
                new NetTestApi.Stop());
            side.start();
        } finally {
            if (before == null) {
                System.clearProperty(NetTestApiSide.NANO_TIME_PROPERTY);
            } else {
                System.setProperty(NetTestApiSide.NANO_TIME_PROPERTY, before);
            }
        }
        return new StartedProducer(side, "127.0.0.1:" + port.await(DEADLINE_SECONDS, TimeUnit.SECONDS));
    }

    private static final class StartedProducer {
        final NetTestApiProducerSide side;
        final String address;

        StartedProducer(NetTestApiProducerSide side, String address) {
            this.side = side;
            this.address = address;
        }
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

        /** Waits for the metrics line and returns its URL. */
        String awaitMetricsUrl() throws InterruptedException {
            Pattern metrics = REGEXES.get("metrics");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS);
            while (System.nanoTime() - deadline < 0 && process.isAlive()) {
                for (String line : stdout().split("\n")) {
                    Matcher matcher = metrics.matcher(line);
                    if (matcher.find()) {
                        assertEquals("NetTest", matcher.group(1));
                        assertTrue(line, Integer.parseInt(matcher.group(3)) > 0);
                        return "http://" + matcher.group(2) + ":" + matcher.group(3) + "/metrics";
                    }
                }
                Thread.sleep(50);
            }
            process.destroyForcibly();
            throw new AssertionError("no metrics line on stdout\n" + describe());
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

        String stdout() {
            return stdout.toString();
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
