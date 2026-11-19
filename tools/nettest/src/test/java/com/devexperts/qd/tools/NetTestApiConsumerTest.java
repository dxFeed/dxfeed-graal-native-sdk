// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.devexperts.qd.qtp.socket.ServerSocketTestHelper;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.Trade;
import com.dxfeed.promise.Promise;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/** The consumer mode of NetTestApi: the slices, the checks and a loopback run against the producer in one JVM. */
public class NetTestApiConsumerTest {
    private static final long SECOND = 1_000_000_000L;
    private static final long DEADLINE_SECONDS = 15;
    private static final long INJECTED_DELAY_NANOS = 5_000_000;
    private static final long MAX_TRANSPORT_NANOS = 1_000_000;
    private static final int WINDOWS = 10;
    private static final long WINDOW_MILLIS = 1_000;

    @Test
    public void theInstancesShareTheSymbolsOnce() {
        String[] symbols = {"A", "B", "C", "D", "E", "F", "G"};

        assertEquals(List.of("A", "D", "G"), NetTestApiConsumerSide.slice(symbols, 1, 3));
        assertEquals(List.of("B", "E"), NetTestApiConsumerSide.slice(symbols, 2, 3));
        assertEquals(List.of("C", "F"), NetTestApiConsumerSide.slice(symbols, 3, 3));
        assertEquals(Arrays.asList(symbols), NetTestApiConsumerSide.slice(symbols, 1, 1));
        assertEquals(List.of(), NetTestApiConsumerSide.slice(new String[] {"A"}, 2, 2));
    }

    @Test
    public void theWatchdogWaitsLongerForTheFirstRecord() {
        NetTestApiConsumerSide.Watchdog watchdog = new NetTestApiConsumerSide.Watchdog(30 * SECOND, 60 * SECOND);

        assertNull("no connection: no starvation", watchdog.check(100 * SECOND, 0));
        assertNull(watchdog.check(101 * SECOND, 1)); // connected at 101 s
        assertNull(watchdog.check(160 * SECOND, 1));
        String starvation = watchdog.check(161 * SECOND, 2);
        assertNotNull(starvation);
        assertTrue(starvation, starvation.contains("60000 ms after the connection"));
    }

    @Test
    public void theWatchdogCountsFromTheLastRecordOrTheReconnection() {
        NetTestApiConsumerSide.Watchdog watchdog = new NetTestApiConsumerSide.Watchdog(30 * SECOND, 60 * SECOND);
        assertNull(watchdog.check(0, 1));
        watchdog.received(10 * SECOND);

        assertNull(watchdog.check(39 * SECOND, 1));
        assertTrue(watchdog.check(40 * SECOND, 1).contains("30000 ms after the last record"));
        assertNull("disconnected", watchdog.check(50 * SECOND, 0));
        assertNull("reconnected at 60 s", watchdog.check(60 * SECOND, 1));
        assertNull(watchdog.check(89 * SECOND, 1));
        assertNotNull(watchdog.check(90 * SECOND, 1));
    }

    @Test
    public void theFirstThousandEventsWithoutANanosecondPartStopTheRun() {
        NetTestApi.Stop stop = new NetTestApi.Stop();
        NetTestApiConsumerSide side = new NetTestApiConsumerSide(
            NetTestApi.parseArguments("c", "127.0.0.1:1", "-S", "10"), stop);

        side.checkNanoParts(999, 0);
        assertNull(side.getFailure());
        side.checkNanoParts(1, 0);

        assertTrue(stop.isRequested());
        assertEquals(NetTestApiConsumerSide.STATUS_NANO_MISSING, side.getFailureStatus());
        assertTrue(side.getFailure(), side.getFailure().contains("dxscheme.nanoTime"));
    }

    @Test
    public void aNanosecondPartEndsTheCheck() {
        NetTestApi.Stop stop = new NetTestApi.Stop();
        NetTestApiConsumerSide side = new NetTestApiConsumerSide(
            NetTestApi.parseArguments("c", "127.0.0.1:1", "-S", "10"), stop);

        side.checkNanoParts(500, 0);
        side.checkNanoParts(10, 1);
        side.checkNanoParts(5_000, 0);

        assertTrue(side.needsNanoCheck() == false);
        assertNull(side.getFailure());
        assertTrue(!stop.isRequested());
    }

    @Test
    public void theListenerSumsTheSignedLagOfEachEvent() {
        NetTestApiConsumerSide side = new NetTestApiConsumerSide(
            NetTestApi.parseArguments("c", "127.0.0.1:1", "-S", "10"), new NetTestApi.Stop());
        NetTestApiListener listener = new NetTestApiListener(side, side.counters[0], 10);
        long now = System.currentTimeMillis() * 1_000_000;
        Trade past = trade(now - 3_000_000_123L); // 3 s ago, with a nanosecond part
        Trade future = trade(now + 60_000_000_000L); // a minute ahead: a negative lag

        long before = System.currentTimeMillis() * 1_000_000;
        listener.eventsReceived(List.of(past, future));
        long after = (System.currentTimeMillis() + 1) * 1_000_000;

        long[] value = new long[NetTestApiStatistics.Counter.SIZE];
        side.counters[0].read(value);
        assertEquals(2, value[NetTestApiStatistics.Counter.RECORDS]);
        // The clock is read once: the sum is 2 * now - (past + future) for one now between before and after.
        long lagSum = value[NetTestApiStatistics.Counter.NANOS];
        long sum = lagSum + past.getTimeNanos() + future.getTimeNanos();
        assertTrue(lagSum + " ns", sum >= 2 * before && sum <= 2 * after);
        assertTrue("a negative lag enters with its sign: " + lagSum, lagSum < 0);
        assertEquals(1, value[NetTestApiStatistics.Counter.NEGATIVE]);
        long maxLag = side.counters[0].takeMax();
        assertTrue("the largest lag is the one of the past event: " + maxLag,
            maxLag >= 3_000_000_123L && maxLag <= after - past.getTimeNanos());
        // The negative lag enters the histogram and the first bucket of the metrics as 0.
        assertEquals(1, side.lagBuckets()[0]);
        assertEquals(1, side.lagBuckets()[NetTestApiMetrics.LAG_BUCKET_NANOS.length]);
        assertEquals(2, side.lagRecorder().getIntervalHistogram().getTotalCount());
        assertEquals(1, listener.symbolsSeen());
        assertTrue("the first record starts the warm-up", side.warmupStartEpochNanos() >= before);
    }

    @Test
    public void theNanosecondCheckTakesOnlyTheFirstThousandEventsOfACall() {
        NetTestApi.Stop stop = new NetTestApi.Stop();
        NetTestApiConsumerSide side = new NetTestApiConsumerSide(
            NetTestApi.parseArguments("c", "127.0.0.1:1", "-S", "10"), stop);
        NetTestApiListener listener = new NetTestApiListener(side, side.counters[0], 10);
        List<MarketEvent> events = new ArrayList<>();
        for (int i = 0; i < 1_100; i++) {
            // Only the 1051st event has a nanosecond part: the first 1000 have none.
            events.add(trade(1_700_000_000_000_000_000L + (i == 1_050 ? 1 : 0)));
        }

        listener.eventsReceived(events.subList(0, 900));
        assertEquals(100, side.nanoCheckRemaining());
        listener.eventsReceived(events.subList(900, 1_100));

        assertTrue(stop.isRequested());
        assertEquals(NetTestApiConsumerSide.STATUS_NANO_MISSING, side.getFailureStatus());
    }

    private static Trade trade(long timeNanos) {
        Trade trade = new Trade("A");
        trade.setTimeNanos(timeNanos);
        return trade;
    }

    @Test
    public void theMeanLagIsTheDelayInjectedByThePublisher() throws Exception {
        String connectorName = "NetTestApiConsumerTest-" + UUID.randomUUID();
        @SuppressWarnings("deprecation")
        Promise<Integer> port = ServerSocketTestHelper.createPortPromise(connectorName);
        NetTestApiProducerSide producer = new NetTestApiProducerSide(NetTestApi.parseArguments("p",
            ":0[name=" + connectorName + "]", "-S", "100", "-r", "*/20k", "-c", "stream"), new NetTestApi.Stop())
        {
            @Override
            NetTestApiGeneratorThread createGenerator(int index, DXEndpoint endpoint) {
                return new NetTestApiGeneratorThread(index, this, endpoint.getPublisher()) {
                    @Override
                    long epochNanos() {
                        return super.epochNanos() - INJECTED_DELAY_NANOS;
                    }
                };
            }
        };
        producer.start();
        NetTestApiConsumerSide consumer = null;
        try {
            consumer = new NetTestApiConsumerSide(NetTestApi.parseArguments("c",
                "127.0.0.1:" + port.await(DEADLINE_SECONDS, TimeUnit.SECONDS), "-S", "100", "-c", "stream", "-C", "2"),
                new NetTestApi.Stop());
            assertEquals(new HashSet<>(Arrays.asList(producer.getSymbols())),
                new HashSet<>(Arrays.asList(consumer.getSymbols())));
            consumer.start();
            awaitRecords(consumer, 1);
            Thread.sleep(1_000); // past the first events of the JIT and the connection
            // Every window holds the injected delay; a stall of a loaded machine (GC, scheduling) may raise the mean
            // of a window by milliseconds, so the upper bound holds for the best window of up to 10 s.
            List<Long> means = new ArrayList<>();
            long best = Long.MAX_VALUE;
            long[] start = totals(consumer);
            for (int window = 0; window < WINDOWS && best > INJECTED_DELAY_NANOS + MAX_TRANSPORT_NANOS; window++) {
                Thread.sleep(WINDOW_MILLIS);
                long[] end = totals(consumer);
                long records = end[0] - start[0];
                assertTrue("records " + records + " in window " + window, records > 1_000);
                long meanLag = (end[1] - start[1]) / records;
                means.add(meanLag);
                assertTrue("mean lags " + means + " ns", meanLag >= INJECTED_DELAY_NANOS);
                best = Math.min(best, meanLag);
                start = end;
            }
            assertTrue("mean lags " + means + " ns", best <= INJECTED_DELAY_NANOS + MAX_TRANSPORT_NANOS);
            // Each instance receives its own slice into its own counter.
            long[] value = new long[2];
            for (int i = 0; i < consumer.counters.length; i++) {
                consumer.counters[i].read(value);
                assertTrue("records of instance " + (i + 1), value[0] > 0);
            }
            assertNull(consumer.getFailure());
        } finally {
            if (consumer != null) {
                assertNull(consumer.finish());
            }
            assertNull(producer.finish());
        }
    }

    private static long[] awaitRecords(NetTestApiConsumerSide consumer, long records) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS);
        long[] totals;
        while ((totals = totals(consumer))[0] < records) {
            assertTrue("no records in " + DEADLINE_SECONDS + " s", System.nanoTime() - deadline < 0);
            Thread.sleep(20);
        }
        return totals;
    }

    // The records and the lag sum of all instances.
    private static long[] totals(NetTestApiConsumerSide consumer) {
        long[] totals = new long[2];
        long[] value = new long[2];
        for (NetTestApiStatistics.Counter counter : consumer.counters) {
            counter.read(value);
            totals[0] += value[0];
            totals[1] += value[1];
        }
        return totals;
    }

    @Test
    public void theCounterOfEachInstanceGetsItsSlice() {
        Set<Integer> sizes = new HashSet<>();
        String[] symbols = new String[10];
        Arrays.fill(symbols, "X");
        for (int i = 1; i <= 4; i++) {
            sizes.add(NetTestApiConsumerSide.slice(symbols, i, 4).size());
        }
        assertEquals(Set.of(2, 3), sizes);
    }
}
