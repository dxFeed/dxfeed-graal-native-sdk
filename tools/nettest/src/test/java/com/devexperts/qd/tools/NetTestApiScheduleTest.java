// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/** The shared schedule of the publisher and the warm-up rule of the statistics. */
public class NetTestApiScheduleTest {
    private static final long T0 = 1_000_000_000_000L;

    @Test
    public void batchKIsDueAtT0PlusKBatchesOverTheRate() {
        NetTestApiSchedule schedule = new NetTestApiSchedule(100, 10_000.0);

        assertTrue(schedule.isLimited());
        assertEquals(T0, schedule.dueNanos(T0, 0));
        assertEquals(T0 + 10_000_000L, schedule.dueNanos(T0, 1));
        assertEquals(T0 + 12_340_000_000L, schedule.dueNanos(T0, 1234));
        // One event at 3 per second: 333,333,333.3 ns a batch, rounded at each k, not summed.
        NetTestApiSchedule third = new NetTestApiSchedule(1, 3.0);
        assertEquals(T0 + 333_333_333L, third.dueNanos(T0, 1));
        assertEquals(T0 + 1_000_000_000L, third.dueNanos(T0, 3));
    }

    @Test
    public void concurrentGrantsTakeEveryBatchOnce() throws Exception {
        int threads = 4;
        int grants = 250;
        NetTestApiSchedule schedule = new NetTestApiSchedule(1, 1_000.0);
        List<long[]> dues = new ArrayList<>();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            long[] due = new long[grants];
            dues.add(due);
            workers.add(new Thread(() -> {
                for (int k = 0; k < grants; k++) {
                    due[k] = schedule.grant();
                }
            }));
        }
        workers.forEach(Thread::start);
        for (Thread worker : workers) {
            worker.join(TimeUnit.SECONDS.toMillis(30));
        }

        long t0 = Long.MAX_VALUE;
        for (long[] due : dues) {
            for (long value : due) {
                t0 = Math.min(t0, value);
            }
        }
        TreeSet<Long> batches = new TreeSet<>();
        for (long[] due : dues) {
            for (long value : due) {
                assertEquals(0, (value - t0) % 1_000_000L);
                batches.add((value - t0) / 1_000_000L);
            }
        }
        assertEquals(threads * grants, batches.size());
        assertEquals(0L, (long) batches.first());
        assertEquals(threads * grants - 1L, (long) batches.last());
        assertTrue(schedule.firstGrantEpochNanos() > 0);
    }

    @Test
    public void theSlipIsTheLatenessOfABatchAndItsMaximumStartsAgainAtEachTake() {
        NetTestApiSchedule schedule = new NetTestApiSchedule(100, 10_000.0);

        assertEquals(0, schedule.recordSlip(100, 50));
        assertEquals(30, schedule.recordSlip(100, 130));
        assertEquals(10, schedule.recordSlip(100, 110));

        assertEquals(30, schedule.takeSlipMax());
        assertEquals(10, schedule.lastSlip());
        assertEquals(0, schedule.takeSlipMax());
    }

    @Test
    public void withoutARateEveryBatchIsDueAtOnceWithoutASlip() throws Exception {
        NetTestApiSchedule schedule = new NetTestApiSchedule(100, null);

        assertFalse(schedule.isLimited());
        assertEquals(T0, schedule.dueNanos(T0, 1234));
        assertEquals(0, schedule.recordSlip(100, 1_000_000));
        assertEquals(0, schedule.lastSlip());
        assertEquals(0, schedule.takeSlipMax());
        long before = System.nanoTime();
        schedule.awaitDue(before + TimeUnit.HOURS.toNanos(1));
        assertTrue(System.nanoTime() - before < TimeUnit.SECONDS.toNanos(10));
        schedule.grant();
        assertTrue("the first grant anchors the warm-up", schedule.firstGrantEpochNanos() > 0);
    }

    @Test
    public void anIntervalIsWarmupUntilTheWarmupStartsAndWhenItBeginsBeforeItEnds() {
        long warmup = TimeUnit.SECONDS.toNanos(60);
        long start = T0;

        assertFalse("no warm-up", NetTestApiStatistics.isWarmup(start, 0, 0));
        assertFalse("no warm-up", NetTestApiStatistics.isWarmup(start, 0, start));
        assertTrue("before the warm-up starts", NetTestApiStatistics.isWarmup(start + warmup * 2, warmup, 0));
        assertTrue("begins before the start", NetTestApiStatistics.isWarmup(start - 1, warmup, start));
        assertTrue("begins at the start", NetTestApiStatistics.isWarmup(start, warmup, start));
        assertTrue("begins 1 ns before the end", NetTestApiStatistics.isWarmup(start + warmup - 1, warmup, start));
        assertFalse("begins at the end", NetTestApiStatistics.isWarmup(start + warmup, warmup, start));
        assertFalse("begins after the end", NetTestApiStatistics.isWarmup(start + warmup + 1, warmup, start));
    }
}
