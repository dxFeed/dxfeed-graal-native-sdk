// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static com.devexperts.qd.tools.NetTestApiRuntime.ABSENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class NetTestApiRuntimeTest {
    private static final long MB = 1024 * 1024;
    private static final long SECOND = 1_000_000_000L;

    private static NetTestApiRuntime.Sample sample(long used, long committed, long gcCount, long gcMillis, long cpu) {
        return new NetTestApiRuntime.Sample(used, committed, gcCount, gcMillis, cpu);
    }

    @Test
    public void formatsTheLineOfTheCTool() {
        NetTestApiRuntime.Sample previous = sample(100 * MB, 256 * MB, 10, 40, 5 * SECOND);
        NetTestApiRuntime.Sample current = sample(120 * MB + MB - 1, 256 * MB, 13, 55, 5 * SECOND + 85 * SECOND / 10);

        assertEquals("{*NetTestRuntime-NetTest*} heap 120 MB of 256 MB, gc 3 (15 ms), cpu 85%",
            NetTestApiRuntime.format("NetTest", previous, current, 10 * SECOND));
    }

    @Test
    public void groupsDigitsAndGoesAbove100PercentOnSeveralCores() {
        NetTestApiRuntime.Sample previous = sample(0, 0, 0, 0, 0);
        NetTestApiRuntime.Sample current = sample(1234 * MB, 4096 * MB, 1000, 12345, 25 * SECOND);

        assertEquals("{*NetTestRuntime-x*} heap 1,234 MB of 4,096 MB, gc 1,000 (12,345 ms), cpu 250%",
            NetTestApiRuntime.format("x", previous, current, 10 * SECOND));
    }

    @Test
    public void anAbsentValueIsNotAvailable() {
        NetTestApiRuntime.Sample previous = sample(MB, MB, ABSENT, ABSENT, ABSENT);
        NetTestApiRuntime.Sample current = sample(ABSENT, MB, 5, 5, 5);

        assertEquals("{*NetTestRuntime-NetTest*} heap n/a of 1 MB, gc n/a, cpu n/a",
            NetTestApiRuntime.format("NetTest", previous, current, SECOND));
    }

    @Test
    public void aCounterThatWentBackIsNotAvailable() {
        NetTestApiRuntime.Sample previous = sample(MB, MB, 5, 50, 2 * SECOND);
        NetTestApiRuntime.Sample current = sample(MB, MB, 4, 60, SECOND);

        assertEquals("{*NetTestRuntime-NetTest*} heap 1 MB of 1 MB, gc n/a, cpu n/a",
            NetTestApiRuntime.format("NetTest", previous, current, SECOND));
    }

    @Test
    public void aZeroPeriodHasNoCpuShare() {
        NetTestApiRuntime.Sample sample = sample(MB, MB, 0, 0, SECOND);

        assertEquals("{*NetTestRuntime-NetTest*} heap 1 MB of 1 MB, gc 0 (0 ms), cpu n/a",
            NetTestApiRuntime.format("NetTest", sample, sample, 0));
    }

    @Test
    public void pausesAreCountedAndConcurrentCyclesAreNot() {
        for (String name : new String[] {"Copy", "MarkSweepCompact", "PS Scavenge", "PS MarkSweep",
            "G1 Young Generation", "G1 Old Generation", "ZGC Pauses", "ZGC Minor Pauses", "ZGC Major Pauses",
            "Shenandoah Pauses"}) {
            assertTrue(name, NetTestApiRuntime.countsPauses(name));
        }
        for (String name : new String[] {"G1 Concurrent GC", "ZGC Cycles", "ZGC Minor Cycles", "ZGC Major Cycles",
            "Shenandoah Cycles"}) {
            assertFalse(name, NetTestApiRuntime.countsPauses(name));
        }
    }

    @Test
    public void readsTheValuesOfThisJvm() {
        NetTestApiRuntime.Sample sample = NetTestApiRuntime.read();

        assertTrue(sample.heapUsedBytes > 0);
        assertTrue(sample.heapCommittedBytes >= sample.heapUsedBytes);
        // The test JVM runs a GC with pause beans (Serial, Parallel or G1 by default).
        assertTrue(sample.gcCount >= 0);
        assertTrue(sample.gcTimeMillis >= 0);
        assertTrue(sample.processCpuNanos > 0);
    }

    @Test
    public void theReporterWritesTheBlockAndTheRuntimeLineAtOnce() {
        List<String> calls = new ArrayList<>();
        List<String> written = new ArrayList<>();
        NetTestApiRuntime.Reporter reporter = new NetTestApiRuntime.Reporter(last -> {
            calls.add("block " + last);
            return calls.size() == 2 ? null : "{*NetTestProducerSide-NetTest*} block";
        }, "NetTest", () -> {
            calls.add("sample");
            return sample(MB, MB, 0, 0, 0);
        }, written::add);

        reporter.run(); // starts the count
        reporter.run();
        reporter.finish();
        reporter.run(); // nothing after the finish

        assertEquals(List.of("sample", "block false", "sample", "block false", "sample", "block true", "sample"),
            calls);
        assertEquals(2, written.size());
        String[] lines = written.get(0).split("\n");
        assertEquals("{*NetTestProducerSide-NetTest*} block", lines[0]);
        assertTrue(lines[1], lines[1].startsWith("{*NetTestRuntime-NetTest*} heap 1 MB of 1 MB, gc 0 (0 ms), cpu "));
    }

    @Test
    public void aShortLastIntervalPrintsNothing() {
        List<String> written = new ArrayList<>();
        NetTestApiRuntime.Reporter reporter = new NetTestApiRuntime.Reporter(last -> null, "NetTest",
            () -> sample(MB, MB, 0, 0, 0), written::add);

        reporter.run();
        reporter.finish();

        assertTrue(written.isEmpty());
    }

    @Test
    public void aLastBlockWithoutATickHasTheRuntimeLineSinceTheStart() {
        List<String> written = new ArrayList<>();
        long[] gcCount = {2};
        NetTestApiRuntime.Reporter reporter = new NetTestApiRuntime.Reporter(
            last -> last ? "{*NetTestProducerSide-NetTest*} block" : null, "NetTest",
            () -> sample(MB, MB, gcCount[0]++, 0, 0), written::add);

        reporter.finish();

        assertEquals(1, written.size());
        String[] lines = written.get(0).split("\n");
        assertEquals("{*NetTestProducerSide-NetTest*} block", lines[0]);
        // The sample of the creation (gc 2) is the start of the line, the one of the finish (gc 3) its end.
        assertTrue(lines[1], lines[1].startsWith("{*NetTestRuntime-NetTest*} heap 1 MB of 1 MB, gc 1 ("));
    }
}
