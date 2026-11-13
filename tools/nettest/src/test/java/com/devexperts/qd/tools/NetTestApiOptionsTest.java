// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dxfeed.api.DXEndpoint;
import org.junit.Test;

/** The command line of NetTestApi: R1 to R3 of the plan, with the C-API NetTest as the reference for the ranges. */
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
        assertNull(settings.config.rateLimiters);
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
        assertTrue(settings.config.rateLimiters.containsKey("*"));
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
        assertEquals(1, NetTestApi.parseRateOption("*/1"), 0);
        assertEquals(1500, NetTestApi.parseRateOption("*/1.5k"), 0);
        assertEquals(2e6, NetTestApi.parseRateOption("*/2M"), 0);
        assertEquals(3e9, NetTestApi.parseRateOption("*/3G"), 0);
        assertEquals(1e12, NetTestApi.parseRateOption("*/1000000000000"), 0);
        assertEquals(1e12, NetTestApi.parseRateOption("*/1000G"), 0);
    }

    @Test
    public void durationAndPeriodTakeTimeUnits() {
        assertEquals(1500, NetTestApi.parseArguments("p", ":1", "-S", "1", "--duration", "1500ms").durationMillis);
        assertEquals(30_000, NetTestApi.parseArguments("p", ":1", "-S", "1", "--duration", "30s").durationMillis);
        assertEquals(60_000, NetTestApi.parseArguments("p", ":1", "-S", "1", "-s", "1m").statPeriod.getTime());
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
        assertRejected("mode 'c'", "c", "127.0.0.1:7000", "-S", "1");
        assertRejected("unknown mode 'x'", "x", ":7000", "-S", "1");
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
        assertRejected("-c", "p", ":7000", "-S", "1", "-c", "ticker");
        assertRejected("--collector", "p", ":7000", "-S", "1", "--collector", "ticker");
        assertRejected("-w", "p", ":7000", "-S", "1", "-w");
        assertRejected("--legacy", "p", ":7000", "-S", "1", "--legacy");
        assertRejected("-Xmx1g", "-Xmx1g", "p", ":7000", "-S", "1");
        assertRejected("-XX:+UseG1GC", "p", ":7000", "-S", "1", "-XX:+UseG1GC");
        assertRejected("Unrecognized option", "p", ":7000", "-S", "1", "--no-such-option");
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
