// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dxfeed.api.DXEndpoint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;

/** The command line of NetTestApi, with the C-API NetTest as the reference for the ranges. */
public class NetTestApiOptionsTest {
    @Test
    public void defaultsAreThoseOfTheCTool() {
        NetTestApi.Settings settings = NetTestApi.parseArguments("p", ":7000", "-S", "100");

        assertEquals(":7000", settings.address);
        assertEquals(NetTestApi.EventKind.TRADE, settings.event);
        assertEquals(DXEndpoint.Role.PUBLISHER, settings.role);
        assertEquals(100, settings.batch);
        assertFalse(settings.reuse);
        assertEquals(0, settings.durationMillis);
        assertEquals(10_000, settings.statPeriod.getTime());
        assertEquals("NetTest", settings.config.name);
        assertEquals(1, settings.config.instanceCount);
        assertEquals(100, settings.config.totalSymbols);
        assertNull(settings.config.ipfPath);
        assertNull(settings.rate);
        assertEquals(0, settings.warmupMillis);
        assertNull(settings.summaryPath);
        assertEquals(-1, settings.metricsPort);
    }

    @Test
    public void everyOptionIsParsed() {
        NetTestApi.Settings settings = NetTestApi.parseArguments("p", "127.0.0.1:7000", "-S", "250@symbols.ipf",
            "-C", "3", "-r", "*/500k", "-s", "2s", "-n", "Bench", "--batch", "1000", "--event", "TimeAndSale",
            "--reuse", "--role", "STREAM_PUBLISHER", "--duration", "7m");

        assertEquals(250, settings.config.totalSymbols);
        assertEquals("symbols.ipf", settings.config.ipfPath);
        assertEquals(3, settings.config.instanceCount);
        assertEquals(3, settings.config.addresses.length);
        assertEquals("127.0.0.1:7000", settings.config.addresses[2]);
        assertEquals(500_000, settings.rate, 0);
        assertEquals(2_000, settings.statPeriod.getTime());
        assertEquals("Bench", settings.config.name);
        assertEquals(1000, settings.batch);
        assertEquals(NetTestApi.EventKind.TIME_AND_SALE, settings.event);
        assertTrue(settings.reuse);
        assertEquals(DXEndpoint.Role.STREAM_PUBLISHER, settings.role);
        assertEquals(7 * 60_000, settings.durationMillis);
    }

    @Test
    public void longOptionNamesAreAccepted() {
        NetTestApi.Settings settings = NetTestApi.parseArguments("p", "localhost:7000", "--symbols", "10",
            "--connections", "2", "--rate-limit", "*/1M", "--stat", "500ms", "--name", "X");

        assertEquals(10, settings.config.totalSymbols);
        assertEquals(2, settings.config.instanceCount);
        assertEquals(500, settings.statPeriod.getTime());
        assertEquals("X", settings.config.name);
    }

    @Test
    public void rateAcceptsTheFormsOfTheCTool() {
        assertEquals(1, rate("*/1"), 0);
        assertEquals(1500, rate("*/1.5k"), 0);
        assertEquals(2e6, rate("*/2M"), 0);
        assertEquals(3e9, rate("*/3G"), 0);
        assertEquals(1e12, rate("*/1000000000000"), 0);
        assertEquals(1e12, rate("*/1000G"), 0);
    }

    private static double rate(String value) {
        return NetTestApi.parseArguments("p", ":1", "-S", "1", "-r", value).rate;
    }

    @Test
    public void theConsumerTakesTheCommonOptions() {
        NetTestApi.Settings settings = NetTestApi.parseArguments("c", "127.0.0.1:7000", "-S", "250@symbols.ipf",
            "-C", "3", "-s", "2s", "-n", "Bench", "--event", "TimeAndSale", "--role", "FEED", "--duration", "7m");

        assertEquals(NetTestApi.Mode.CONSUMER, settings.mode);
        assertEquals(3, settings.config.instanceCount);
        assertEquals(NetTestApi.EventKind.TIME_AND_SALE, settings.event);
        assertEquals(DXEndpoint.Role.FEED, settings.role);
        assertFalse(settings.isConflated());
        assertNull(settings.rate);
        assertNull(settings.describe().get("batch"));
        assertNull(settings.describe().get("reuse"));
    }

    @Test
    public void longOptionsTakeTheirValueAfterAnEqualsSign() {
        NetTestApi.Settings settings = NetTestApi.parseArguments("c", "h:1", "--symbols=10", "--collector=ticker",
            "--event=Trade", "--duration=1s");

        assertEquals(10, settings.config.totalSymbols);
        assertEquals("ticker", settings.collector);
        assertEquals(DXEndpoint.Role.FEED, settings.role);
        assertTrue(settings.isConflated());
        assertEquals(1_000, settings.durationMillis);
    }

    @Test
    public void theLastValueOfARepeatedOptionCounts() {
        NetTestApi.Settings settings = NetTestApi.parseArguments("p", ":1", "-S", "1", "-r", "*/1k", "-r", "*/2k",
            "-c", "ticker", "--collector", "stream");

        assertEquals(2_000, settings.rate, 0);
        assertEquals("stream", settings.collector);
        assertEquals(DXEndpoint.Role.STREAM_PUBLISHER, settings.role);
    }

    @Test
    public void helpIsFoundOutsideTheValuesOfOptions() {
        assertTrue(NetTestApiCommandLine.isHelp("--help"));
        assertTrue(NetTestApiCommandLine.isHelp("c", "h:1", "-S", "1", "--help"));
        assertFalse(NetTestApiCommandLine.isHelp("p", ":1", "-S", "1", "-n", "--help"));
        // -h and -help are short forms of --help.
        assertTrue(NetTestApiCommandLine.isHelp("-h"));
        assertTrue(NetTestApiCommandLine.isHelp("-help"));
        assertTrue(NetTestApiCommandLine.isHelp("p", ":1", "-S", "1", "-h"));
        assertFalse(NetTestApiCommandLine.isHelp("p", ":1", "-S", "1", "-n", "-h"));
        assertTrue(new NetTestApi().usage().endsWith("spec_version nettest/1"));
    }

    @Test
    public void durationAndPeriodTakeTimeUnits() {
        assertEquals(1500, NetTestApi.parseArguments("p", ":1", "-S", "1", "--duration", "1500ms").durationMillis);
        assertEquals(30_000, NetTestApi.parseArguments("p", ":1", "-S", "1", "--duration", "30s").durationMillis);
        assertEquals(60_000, NetTestApi.parseArguments("p", ":1", "-S", "1", "-s", "1m").statPeriod.getTime());
    }

    @Test
    public void theWarmupTheSummaryAndTheMetricsAreParsed() throws Exception {
        Path directory = Files.createTempDirectory("nettest-options");
        try {
            String file = directory.resolve("summary.json").toString();
            NetTestApi.Settings settings = NetTestApi.parseArguments("c", "h:1", "-S", "1", "--warmup", "30s",
                "--summary", file, "--metrics-port", "0");

            assertEquals(30_000, settings.warmupMillis);
            assertEquals(30_000L, settings.describe().get("warmup_ms"));
            assertEquals(Paths.get(file).toAbsolutePath(), settings.summaryPath);
            assertEquals("127.0.0.1", settings.metricsHost);
            assertEquals(0, settings.metricsPort);
            assertEquals(List.of("c", "h:1", "-S", "1", "--warmup", "30s", "--summary", file, "--metrics-port", "0"),
                settings.args);
        } finally {
            Files.delete(directory);
        }
        NetTestApi.Settings settings = NetTestApi.parseArguments("p", ":1", "-S", "1", "--metrics-port=0.0.0.0:9100");
        assertEquals("0.0.0.0", settings.metricsHost);
        assertEquals(9100, settings.metricsPort);
        settings = NetTestApi.parseArguments("p", ":1", "-S", "1", "--metrics-port", "[::1]:9100");
        assertEquals("::1", settings.metricsHost);
        assertEquals(65_535, NetTestApi.parseArguments("p", ":1", "-S", "1", "--metrics-port", ":65535").metricsPort);
    }

    @Test
    public void aSummaryPathThatIsADirectoryIsRejected() throws Exception {
        Path directory = Files.createTempDirectory("nettest-options");
        try {
            assertRejected("--summary cannot write to the directory of", "p", ":1", "-S", "1", "--summary",
                directory.toString());
        } finally {
            Files.delete(directory);
        }
        assertRejected("--metrics-port needs [<host>:]<port>", "p", ":1", "-S", "1", "--metrics-port", "h:");
        assertRejected("--metrics-port needs [<host>:]<port>", "p", ":1", "-S", "1", "--metrics-port", "-1");
    }

    @Test
    public void clientAddressAllowsManyInstances() {
        assertEquals(2, NetTestApi.parseArguments("p", "127.0.0.1:7000", "-S", "1", "-C", "2").config.instanceCount);
        assertEquals(1000, NetTestApi.parseArguments("p", "h:1", "-S", "1", "-C", "1000").config.instanceCount);
    }

    @Test
    public void batchLimitIsTwoToTheTwentySecond() {
        assertEquals(1 << 22, NetTestApi.parseArguments("p", ":1", "-S", "1", "--batch", "4194304").batch);
        assertEquals(1, NetTestApi.parseArguments("p", ":1", "-S", "1", "--batch", "1").batch);
    }

    @Test
    public void definedPropertiesAreSetWhileParsing() {
        String key = "com.devexperts.qd.tools.NetTestApiOptionsTest.property";
        try {
            NetTestApi.parseArguments("-D" + key + "=bar", "p", ":7000", "-S", "1");

            assertEquals("bar", System.getProperty(key));
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    public void modeAndAddressAreChecked() {
        assertRejected("no mode", "-S", "1");
        assertRejected("unknown mode 'x'; the mode is p or c", "x", ":7000", "-S", "1");
        assertRejected("--rate-limit is an option of the mode p", "c", "h:1", "-S", "1", "--rate-limit=*/1k");
        assertRejected("address is missing", "p", "-S", "1");
        assertRejected("unexpected argument 'extra'", "p", ":7000", "extra", "-S", "1");
        assertRejected("-S is missing", "p", ":7000");
    }

    @Test
    public void qdSymbolSyntaxBeyondTheCToolIsRejected() {
        assertRejected("'{len,max}'", "p", ":7000", "-S", "10{3,5}");
        assertRejected("'{len,max}'", "p", ":7000", "-S", "10{3}");
        assertRejected("'/r' or '/s'", "p", ":7000", "-S", "1000/r10");
        assertRejected("'/r' or '/s'", "p", ":7000", "-S", "1000/s10");
        assertRejected("no file name after '@'", "p", ":7000", "-S", "10@");
        assertRejected("-S needs <total>", "p", ":7000", "-S", "0");
        assertRejected("-S needs <total>", "p", ":7000", "-S", "100000001");
        assertRejected("-S needs <total>", "p", ":7000", "-S", "abc");
    }

    @Test
    public void foreignOptionsAreRejected() {
        assertRejected("unsupported QD flag: -w", "p", ":7000", "-S", "1", "-w");
        assertRejected("unsupported option: --legacy", "p", ":7000", "-S", "1", "--legacy");
        assertRejected("unsupported option: -Xmx1g", "-Xmx1g", "p", ":7000", "-S", "1");
        assertRejected("unsupported option: -XX:+UseG1GC", "p", ":7000", "-S", "1", "-XX:+UseG1GC");
        assertRejected("unknown flag --no-such-option", "p", ":7000", "-S", "1", "--no-such-option=1");
        assertRejected("--reuse takes no value", "p", ":7000", "-S", "1", "--reuse=yes");
        assertRejected("-n cannot contain '*'", "p", ":7000", "-S", "1", "-n", "a*b");
    }

    @Test
    public void valuesOutsideTheRangesOfTheCToolAreRejected() {
        assertRejected("--batch needs a number from 1 to 4194304", "p", ":1", "-S", "1", "--batch", "0");
        assertRejected("--batch needs a number from 1 to 4194304", "p", ":1", "-S", "1", "--batch", "4194305");
        assertRejected("-C needs a number from 1 to 1000", "p", "h:1", "-S", "1", "-C", "0");
        assertRejected("-C needs a number from 1 to 1000", "p", "h:1", "-S", "1", "-C", "1001");
        assertRejected("-s needs a positive time with ms, s or m", "p", ":1", "-S", "1", "-s", "0s");
        assertRejected("-s needs a positive time with ms, s or m", "p", ":1", "-S", "1", "-s", "10");
        assertRejected("-s needs a positive time with ms, s or m", "p", ":1", "-S", "1", "-s", "1.5s");
        assertRejected("-s needs a positive time with ms, s or m", "p", ":1", "-S", "1", "-s", "1h");
        assertRejected("--duration needs a positive time with ms, s or m", "p", ":1", "-S", "1", "--duration", "0s");
        assertRejected("--duration needs a positive time with ms, s or m", "p", ":1", "-S", "1", "--duration", "ms");
        assertRejected("-n needs a non-empty name", "p", ":1", "-S", "1", "-n", "");
        assertRejected("--event is Trade or TimeAndSale", "p", ":1", "-S", "1", "--event", "Quote");
        assertRejected("--role is PUBLISHER or STREAM_PUBLISHER", "p", ":1", "-S", "1", "--role", "FEED");
    }

    @Test
    public void ratesOutsideTheCToolFormsAreRejected() {
        assertRejected("-r has a bad rate", "p", ":1", "-S", "1", "-r", "*/1001G");
        assertRejected("-r has a bad rate", "p", ":1", "-S", "1", "-r", "*/0.5");
        assertRejected("-r has a bad rate", "p", ":1", "-S", "1", "-r", "*/2e3");
        assertRejected("-r has a bad rate", "p", ":1", "-S", "1", "-r", "*/1.");
        assertRejected("-r has a bad rate", "p", ":1", "-S", "1", "-r", "*/k");
        assertRejected("-r needs */<rate>", "p", ":1", "-S", "1", "-r", "1000");
        assertRejected("-r needs */<rate>", "p", ":1", "-S", "1", "-r", "1/1000");
        assertRejected("bucket ';<time>'", "p", ":1", "-S", "1", "-r", "*/1000;2s");
    }

    @Test
    public void manyInstancesCannotShareOneListeningPort() {
        assertRejected("-C 2 cannot be used with the server address ':7000'", "p", ":7000", "-S", "1", "-C", "2");
    }

    private static void assertRejected(String expectedMessagePart, String... args) {
        try {
            NetTestApi.parseArguments(args);
            fail("accepted: " + String.join(" ", args));
        } catch (BadToolParametersException e) {
            assertTrue("'" + e.getMessage() + "' must contain '" + expectedMessagePart + "'",
                e.getMessage().contains(expectedMessagePart));
        }
    }
}
