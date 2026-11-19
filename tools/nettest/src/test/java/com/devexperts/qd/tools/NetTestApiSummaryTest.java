// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.event.market.MarketEvent;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Histogram;
import org.junit.Test;

/** The summary file and the schedule slip of NetTestApi in the test JVM. */
public class NetTestApiSummaryTest {
    /** One batch every 10 ms at 10k events per second with batches of 100. */
    private static final long BATCH_MILLIS = 10;
    /** A publishing call 5 ms longer than the time of a batch: the slip grows by half a second per second. */
    private static final long THROTTLED_CALL_MILLIS = 15;

    @Test
    public void aThrottledPublisherShowsAGrowingSlip() throws Exception {
        NetTestApiProducerSide side = new NetTestApiProducerSide(NetTestApi.parseArguments("p", ":0", "-S", "100",
            "-r", "*/10k", "--batch", "100", "-s", "1s", "--duration", "4500ms"), new NetTestApi.Stop())
        {
            @Override
            NetTestApiGeneratorThread createGenerator(int index, DXEndpoint endpoint) {
                return new NetTestApiGeneratorThread(index, this, endpoint.getPublisher()) {
                    @Override
                    void publish(List<MarketEvent> batchEvents) {
                        super.publish(batchEvents);
                        try {
                            Thread.sleep(THROTTLED_CALL_MILLIS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                };
            }
        };

        assertNull(side.run());

        String text = side.summary().toJson();
        JsonNode json = NetTestApiChecks.checkSummary(text);
        assertEquals("ok", json.get("status").asText());
        JsonNode slip = json.get("schedule_slip");
        long growth = slip.get("growth_ns_per_s").asLong();
        // (15 - 10) / 15 of each second: about 333 ms per second, far above any scheduling noise.
        assertTrue(text, growth > TimeUnit.MILLISECONDS.toNanos(100));
        assertTrue(text, slip.get("max_ns").asLong() >= slip.get("end_ns").asLong() - BATCH_MILLIS * 1_000_000);
        List<Long> ends = new ArrayList<>();
        for (JsonNode interval : json.get("intervals")) {
            ends.add(interval.get("slip_end_ns").asLong());
            assertTrue(text, interval.get("publish_ns_per_event").asLong() >=
                TimeUnit.MILLISECONDS.toNanos(THROTTLED_CALL_MILLIS) / 100);
        }
        assertTrue(text, ends.size() >= 2);
        for (int i = 1; i < ends.size(); i++) {
            assertTrue("the slip grows: " + ends, ends.get(i) > ends.get(i - 1));
        }
        JsonNode call = json.get("publish_call");
        assertEquals(text, call.get("events").asLong(), call.get("calls").asLong() * 100);
    }

    @Test
    public void withoutARateThereIsNoScheduleAndNoSlip() throws Exception {
        NetTestApiProducerSide side = new NetTestApiProducerSide(NetTestApi.parseArguments("p", ":0", "-S", "10",
            "-s", "1s", "--duration", "2500ms"), new NetTestApi.Stop());

        assertNull(side.run());

        String text = side.summary().toJson();
        JsonNode json = NetTestApiChecks.checkSummary(text);
        assertTrue(text, json.get("schedule_slip").isNull());
        for (JsonNode interval : json.get("intervals")) {
            assertTrue(text, interval.get("slip_max_ns").isNull());
            assertTrue(text, interval.get("slip_end_ns").isNull());
        }
        assertTrue(text, json.get("publish_call").get("calls").asLong() > 0);
        assertTrue(text, json.get("rate").asLong() > 0);
    }

    @Test
    public void theWarmupIntervalsAreLeftOutOfTheTotals() {
        NetTestApiProducerSide side = new NetTestApiProducerSide(NetTestApi.parseArguments("p", ":0", "-S", "10",
            "--warmup", "1s"), new NetTestApi.Stop());
        NetTestApiSummary summary = side.summary();
        summary.started();
        summary.add(interval(true, 100, 5));
        summary.add(interval(false, 300, 7));
        summary.add(interval(false, 600, 9));
        summary.ended();

        JsonNode json;
        try {
            json = NetTestApiChecks.checkSummary(summary.toJson());
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        assertEquals(1_000, json.get("records").asLong());
        assertEquals(900, json.get("publish_call").get("events").asLong());
        assertEquals(2, json.get("publish_call").get("calls").asLong());
        assertEquals("warmup", json.get("intervals").get(0).get("phase").asText());
        assertEquals("measure", json.get("intervals").get(1).get("phase").asText());
    }

    @Test
    public void theSummaryKeepsThePercentilesOfAnIntervalAndDropsItsHistogram() throws Exception {
        NetTestApiConsumerSide side = new NetTestApiConsumerSide(NetTestApi.parseArguments("c", "127.0.0.1:1", "-S",
            "10", "--warmup", "1s"), new NetTestApi.Stop());
        NetTestApiSummary summary = side.summary();
        summary.started();
        Histogram warmupLags = NetTestApiStatistics.newLagHistogram();
        warmupLags.recordValueWithCount(1_000_000, 100);
        Histogram lags = NetTestApiStatistics.newLagHistogram();
        lags.recordValueWithCount(1_000, 50);
        lags.recordValueWithCount(9_000, 49);
        lags.recordValue(500_000);
        NetTestApiStatistics.Interval warmup = interval(true, 100, 5, 1_000_000, warmupLags);
        // The largest lag lost to the swap of the interval: the records are there, the maximum is not.
        NetTestApiStatistics.Interval measured = interval(false, 100, 6, Long.MIN_VALUE, lags);
        summary.add(warmup);
        summary.add(measured);
        summary.ended();

        assertNull(warmup.histogram);
        assertNull(measured.histogram);
        assertEquals(Long.valueOf(1_000), measured.lagP50);
        assertNotNull(measured.lagP99);
        String text = summary.toJson();
        JsonNode json = NetTestApiChecks.checkSummary(text);
        JsonNode interval = json.get("intervals").get(1);
        assertEquals(text, 1_000, interval.get("lag_p50_ns").asLong());
        assertEquals(text, measured.lagP99.longValue(), interval.get("lag_p99_ns").asLong());
        assertTrue(text, interval.get("lag_max_ns").isNull());
        JsonNode e2e = json.get("e2e_lag");
        assertEquals(text, 100, e2e.get("count").asLong());
        // Only the measured histogram is merged: the warm-up lags of 1 ms are not in it.
        assertTrue(text, e2e.get("p99_ns").asLong() < 1_000_000);
        assertTrue(text, e2e.get("max_ns").isNull());
    }

    @Test
    public void theIntervalOfAnEmptyHistogramHasNoPercentiles() {
        NetTestApiStatistics.Interval interval = interval(false, 0, 1, Long.MIN_VALUE,
            NetTestApiStatistics.newLagHistogram());

        assertNull(interval.lagP50);
        assertNull(interval.lagP99);
    }

    @Test
    public void stringsAreEscaped() {
        NetTestApiSummary.Json json = new NetTestApiSummary.Json();
        json.begin().field("a", "q\"b\\n\n\u0001").field("n", null).key("l").value(List.of("x", 1L)).end();

        assertEquals("{\"a\":\"q\\\"b\\\\n\\n\\u0001\",\"n\":null,\"l\":[\"x\",1]}", json.toString());
    }

    // An interval of 1 s with one call of 1 ms.
    private static NetTestApiStatistics.Interval interval(boolean warmup, long records, long index) {
        return interval(warmup, records, index, Long.MIN_VALUE, null);
    }

    private static NetTestApiStatistics.Interval interval(boolean warmup, long records, long index, long maxLag,
        Histogram lags)
    {
        long[] totals = new long[NetTestApiStatistics.Counter.SIZE];
        totals[NetTestApiStatistics.Counter.RECORDS] = records;
        totals[NetTestApiStatistics.Counter.CALLS] = 1;
        totals[NetTestApiStatistics.Counter.CALL_NANOS] = 1_000_000;
        totals[NetTestApiStatistics.Counter.NANOS] = records * 1_000_000;
        long start = 1_763_380_800_000L + index * 1_000;
        return new NetTestApiStatistics.Interval("", start, start + 1_000, 1_000_000_000L, warmup, totals,
            maxLag, lags, null, null, 1);
    }
}
