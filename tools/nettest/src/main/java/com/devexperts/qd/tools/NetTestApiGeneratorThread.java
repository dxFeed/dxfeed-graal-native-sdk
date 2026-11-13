// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.qd.qtp.QDEndpoint;
import com.devexperts.qd.util.RateLimiter;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * The generator of one {@link NetTestApi} instance: publishes batches of Trade or TimeAndSale events and counts them
 * for QD's statistics collector.
 *
 * <p>All events of a batch carry the time of the batch and consecutive sequences, so that a QD NetTest consumer
 * computes the business lag. The lag of this side is the time from building a batch to the return of
 * {@link DXPublisher#publishEvents}, weighted by the events of the batch; the wait in the rate limiter is not part of
 * it.
 */
class NetTestApiGeneratorThread extends NetTestWorkingThread {
    /** The largest sequence of Trade and TimeAndSale (22 bits). */
    static final int MAX_SEQUENCE = (1 << 22) - 1;

    private static final int VALUE_TABLE_SIZE = 1 << 12; // a power of two
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final NetTestApiProducerSide side;
    private final DXPublisher publisher;
    private final boolean timeAndSale;
    private final boolean reuse;
    private final int batch;
    private final String[] symbols;
    private final MarketEvent[] pool; // one event per symbol, republished in place; only with --reuse
    private final double[] prices = new double[VALUE_TABLE_SIZE];
    private final long[] sizes = new long[VALUE_TABLE_SIZE];
    private final SplittableRandom random;
    private final List<MarketEvent> events;
    private int sequence;
    private int valueIndex;

    private long weightedNanos; // the sum of batch time * batch events, guarded by this

    NetTestApiGeneratorThread(int index, NetTestApiProducerSide side, QDEndpoint endpoint, DXPublisher publisher) {
        super("NetTestApi", index, side, endpoint);
        this.side = side;
        this.publisher = publisher;
        this.timeAndSale = side.settings.event == NetTestApi.EventKind.TIME_AND_SALE;
        this.reuse = side.settings.reuse;
        this.batch = side.settings.batch;
        this.symbols = side.getSymbols();
        this.random = new SplittableRandom(index);
        this.pool = reuse ? new MarketEvent[symbols.length] : null;
        if (reuse) {
            for (int i = 0; i < symbols.length; i++) {
                pool[i] = newEvent(symbols[i]);
            }
        }
        for (int i = 0; i < VALUE_TABLE_SIZE; i++) {
            prices[i] = (10_000 + random.nextInt(990_000)) / 100.0; // 100.00 .. 9999.99
            sizes[i] = 1 + random.nextInt(1_000);
        }
        this.events = new ArrayList<>(batch);
    }

    @Override
    public void run() {
        try {
            while (!side.stop.isRequested() && !isInterrupted()) {
                int count = acquire();
                long start = System.nanoTime();
                publisher.publishEvents(fill(count, epochNanos()));
                record(count, System.nanoTime() - start);
            }
        } catch (InterruptedException e) {
            // stopped
        } catch (Throwable t) {
            side.fail(t);
        }
    }

    /** Takes up to one batch of events from the shared rate limit, waiting for at least one. */
    private int acquire() throws InterruptedException {
        RateLimiter limiter = side.rateLimiter;
        if (limiter == null) {
            return batch;
        }
        synchronized (limiter) {
            int count = (int) Math.min(batch, limiter.availableOrWait());
            limiter.reportConsumed(count);
            return count;
        }
    }

    /**
     * Fills the next batch: {@code count} events of random symbols, all with the time {@code timeNanos} and
     * consecutive sequences.
     */
    List<MarketEvent> fill(int count, long timeNanos) {
        events.clear();
        for (int i = 0; i < count; i++) {
            int symbol = symbols.length == 1 ? 0 : random.nextInt(symbols.length);
            MarketEvent event = reuse ? pool[symbol] : newEvent(symbols[symbol]);
            int value = valueIndex++ & (VALUE_TABLE_SIZE - 1);
            if (timeAndSale) {
                TimeAndSale timeAndSaleEvent = (TimeAndSale) event;
                timeAndSaleEvent.setTimeNanos(timeNanos);
                timeAndSaleEvent.setSequence(sequence);
                timeAndSaleEvent.setPrice(prices[value]);
                timeAndSaleEvent.setSize(sizes[value]);
            } else {
                Trade trade = (Trade) event;
                trade.setTimeNanos(timeNanos);
                trade.setSequence(sequence);
                trade.setPrice(prices[value]);
                trade.setSize(sizes[value]);
            }
            sequence = (sequence + 1) & MAX_SEQUENCE;
            events.add(event);
        }
        return events;
    }

    /** Sets the sequence of the next event, for tests. */
    void setNextSequence(int sequence) {
        this.sequence = sequence & MAX_SEQUENCE;
    }

    /** Counts a published batch of {@code count} events that took {@code nanos}. */
    synchronized void record(int count, long nanos) {
        weightedNanos += nanos * count;
        processedRecords += count;
    }

    // The collector computes the lag in microseconds as (delta of sumLatency) * 1000 / (delta of records), so
    // sumLatency is the record-weighted time in milliseconds. It is truncated from the cumulative nanoseconds, so that
    // batches faster than 1 ms still add up.
    @Override
    synchronized void getStats(Stats target) {
        target.sumLatency = weightedNanos / 1_000_000;
        target.processedRecords = processedRecords;
    }

    private MarketEvent newEvent(String symbol) {
        return timeAndSale ? new TimeAndSale(symbol) : new Trade(symbol);
    }

    private static long epochNanos() {
        Instant now = Instant.now();
        return now.getEpochSecond() * NANOS_PER_SECOND + now.getNano();
    }
}
