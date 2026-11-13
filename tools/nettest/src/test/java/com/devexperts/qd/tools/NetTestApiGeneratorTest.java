// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/** The batches and the statistics of the generator (KTD6 of the plan), without endpoints. */
public class NetTestApiGeneratorTest {
    private static final long TIME_NANOS = 1_700_000_000_123_456_789L;

    @Test
    public void tradeBatchSharesTheTimeAndHasConsecutiveSequences() {
        NetTestApiGeneratorThread generator = generator("p", ":1", "-S", "100");

        List<MarketEvent> batch = generator.fill(5, TIME_NANOS);

        assertEquals(5, batch.size());
        for (int i = 0; i < batch.size(); i++) {
            Trade trade = (Trade) batch.get(i);
            assertEquals(TIME_NANOS, trade.getTimeNanos());
            assertEquals(TIME_NANOS / 1_000_000, trade.getTime());
            assertEquals(i, trade.getSequence());
            assertTrue(trade.getPrice() > 0);
            assertTrue(trade.getSize() > 0);
        }
    }

    @Test
    public void timeAndSaleBatchSharesTheTimeAndHasConsecutiveSequences() {
        NetTestApiGeneratorThread generator = generator("p", ":1", "-S", "100", "--event", "TimeAndSale");

        List<MarketEvent> batch = generator.fill(5, TIME_NANOS);

        for (int i = 0; i < batch.size(); i++) {
            TimeAndSale timeAndSale = (TimeAndSale) batch.get(i);
            assertEquals(TIME_NANOS, timeAndSale.getTimeNanos());
            assertEquals(i, timeAndSale.getSequence());
            assertTrue(timeAndSale.getPrice() > 0);
        }
    }

    @Test
    public void sequenceWrapsAtTwentyTwoBits() {
        NetTestApiGeneratorThread generator = generator("p", ":1", "-S", "100");
        generator.setNextSequence(NetTestApiGeneratorThread.MAX_SEQUENCE - 1);

        List<MarketEvent> batch = generator.fill(3, TIME_NANOS);

        assertEquals(NetTestApiGeneratorThread.MAX_SEQUENCE - 1, ((Trade) batch.get(0)).getSequence());
        assertEquals(NetTestApiGeneratorThread.MAX_SEQUENCE, ((Trade) batch.get(1)).getSequence());
        assertEquals(0, ((Trade) batch.get(2)).getSequence());
        assertEquals(TIME_NANOS, ((Trade) batch.get(2)).getTimeNanos());
    }

    @Test
    public void cipherCodedSymbolsArePublishedAsStrings() {
        NetTestApiProducerSide side = side("p", ":1", "-S", "1000");
        boolean anyCipher = false;
        for (int i = 0; i < side.symbols.size(); i++) {
            anyCipher |= side.symbols.getSymbol(i) == null;
        }
        assertTrue("QD's generated symbols include short cipher-coded ones", anyCipher);
        Set<String> symbols = new HashSet<>(Arrays.asList(side.getSymbols()));
        assertEquals(1000, symbols.size());

        List<MarketEvent> batch = new NetTestApiGeneratorThread(1, side, null, null).fill(2000, TIME_NANOS);

        for (MarketEvent event : batch) {
            assertNotNull(event.getEventSymbol());
            assertTrue(event.getEventSymbol(), symbols.contains(event.getEventSymbol()));
        }
    }

    @Test
    public void reusedEventsAreTheSameObjects() {
        NetTestApiGeneratorThread generator = generator("p", ":1", "-S", "1", "--reuse");

        MarketEvent first = generator.fill(1, TIME_NANOS).get(0);
        MarketEvent second = generator.fill(1, TIME_NANOS + 1).get(0);

        assertSame(first, second);
        assertEquals(TIME_NANOS + 1, ((Trade) second).getTimeNanos());
    }

    @Test
    public void newEventsAreNewObjects() {
        NetTestApiGeneratorThread generator = generator("p", ":1", "-S", "1");

        MarketEvent first = generator.fill(1, TIME_NANOS).get(0);
        MarketEvent second = generator.fill(1, TIME_NANOS + 1).get(0);

        assertNotSame(first, second);
        assertEquals(TIME_NANOS, ((Trade) first).getTimeNanos());
    }

    @Test
    public void statisticsAreRecordWeightedAndKeepSubMillisecondBatches() {
        NetTestApiGeneratorThread generator = generator("p", ":1", "-S", "1");
        NetTestWorkingThread.Stats stats = new NetTestWorkingThread.Stats();

        generator.record(10, 300_000); // 0.3 ms
        generator.getStats(stats);
        assertEquals(10, stats.processedRecords);
        assertEquals(3, stats.sumLatency); // 10 * 0.3 ms

        generator.record(10, 700_000); // 0.7 ms
        generator.getStats(stats);
        assertEquals(20, stats.processedRecords);
        assertEquals(10, stats.sumLatency); // the collector shows 10 * 1000 / 20 = 500 us

        for (int i = 0; i < 1000; i++) {
            generator.record(1, 50_000); // 0.05 ms, each far below 1 ms
        }
        generator.getStats(stats);
        assertEquals(1020, stats.processedRecords);
        assertEquals(60, stats.sumLatency); // 10 + 1000 * 0.05
    }

    private static NetTestApiGeneratorThread generator(String... args) {
        return new NetTestApiGeneratorThread(1, side(args), null, null);
    }

    private static NetTestApiProducerSide side(String... args) {
        return new NetTestApiProducerSide(NetTestApi.parseArguments(args), new NetTestApi.Stop());
    }
}
