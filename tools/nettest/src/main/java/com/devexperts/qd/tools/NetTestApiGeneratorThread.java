// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.dxfeed.api.DXPublisher;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import java.util.ArrayList;
import java.util.List;

/**
 * The generator of one {@link NetTestApi} instance: publishes batches of Trade or TimeAndSale events on the schedule of
 * the side and counts them in the statistics counter of its instance.
 *
 * <p>Each batch, as the NetTest specification orders it (section "Rate limit and schedule"): wait for the batch's time
 * on the schedule, read the clock once (the time of every event of the batch and the reference of the slip), fill the
 * batch from the pool (section "Events and pools"), publish it through {@link DXPublisher#publishEvents} and read the
 * elapsed time. The lag of this side and the publishing time cover the fill and the call only.
 */
class NetTestApiGeneratorThread extends Thread {
    /** The largest sequence of Trade and TimeAndSale (22 bits). */
    static final int MAX_SEQUENCE = (1 << 22) - 1;

    final int index;
    private final NetTestApiProducerSide side;
    private final DXPublisher publisher;
    private final boolean timeAndSale;
    private final boolean reuse;
    private final int batch;
    private final String[] symbols;
    private final MarketEvent[] pool; // one event per symbol, stamped in place; only with --reuse
    private final NetTestApiPool random;
    private final List<MarketEvent> events;
    private final NetTestApiStatistics.Counter counter;
    private int sequence;

    NetTestApiGeneratorThread(int index, NetTestApiProducerSide side, DXPublisher publisher) {
        super("NetTestApi-" + side.config.name + "." + index);
        setDaemon(true);
        this.index = index;
        this.side = side;
        this.publisher = publisher;
        this.timeAndSale = side.settings.event == NetTestApi.EventKind.TIME_AND_SALE;
        this.reuse = side.settings.reuse;
        this.batch = side.settings.batch;
        this.symbols = side.getSymbols();
        this.random = new NetTestApiPool(index, symbols.length);
        this.pool = reuse ? new MarketEvent[symbols.length] : null;
        if (reuse) {
            for (int i = 0; i < symbols.length; i++) {
                pool[i] = newEvent(symbols[i]);
            }
        }
        this.events = new ArrayList<>(batch);
        this.counter = side.counters[index - 1];
    }

    @Override
    public void run() {
        NetTestApiSchedule schedule = side.schedule;
        try {
            while (!side.stop.isRequested() && !isInterrupted()) {
                long due = schedule.grant();
                schedule.awaitDue(due);
                long start = System.nanoTime();
                long time = epochNanos();
                schedule.recordSlip(due, start);
                publish(fill(batch, time));
                record(batch, System.nanoTime() - start);
            }
        } catch (InterruptedException e) {
            // stopped
        } catch (Throwable t) {
            side.fail(t);
        }
    }

    /** Publishes a batch; a test may slow it down. */
    void publish(List<MarketEvent> batchEvents) {
        publisher.publishEvents(batchEvents);
    }

    /**
     * Fills the next batch: {@code count} events, each with one draw of the pool for its symbol and its row, all with
     * the time {@code timeNanos} and consecutive sequences.
     */
    List<MarketEvent> fill(int count, long timeNanos) {
        events.clear();
        for (int i = 0; i < count; i++) {
            long r = random.next();
            int symbol = random.symbolIndex(r);
            int row = NetTestApiPool.row(r);
            MarketEvent event = reuse ? pool[symbol] : newEvent(symbols[symbol]);
            if (timeAndSale) {
                TimeAndSale timeAndSaleEvent = (TimeAndSale) event;
                timeAndSaleEvent.setTimeNanos(timeNanos);
                timeAndSaleEvent.setSequence(sequence);
                timeAndSaleEvent.setPrice(random.price(row));
                timeAndSaleEvent.setSizeAsDouble(random.size(row));
            } else {
                Trade trade = (Trade) event;
                trade.setTimeNanos(timeNanos);
                trade.setSequence(sequence);
                trade.setPrice(random.price(row));
                trade.setSizeAsDouble(random.size(row));
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

    /** Counts a published batch of {@code count} events whose fill and call took {@code nanos}. */
    void record(int count, long nanos) {
        counter.addBatch(count, nanos);
    }

    private MarketEvent newEvent(String symbol) {
        return timeAndSale ? new TimeAndSale(symbol) : new Trade(symbol);
    }

    /** The time of the next batch in nanoseconds since the epoch; a test may shift it. */
    long epochNanos() {
        return NetTestApiClock.epochNanos();
    }
}
