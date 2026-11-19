// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.logging.Logging;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The consumer side of {@link NetTestApi}: one {@link DXEndpoint} per instance with a subscription to its slice of
 * the symbols. It stops the run with a failure when no records come (status {@code no_data}) or when the events lack
 * the nanosecond part of their time (status {@code nano_missing}).
 */
class NetTestApiConsumerSide extends NetTestApiSide {
    private static final Logging log = Logging.getLogging(NetTestApiConsumerSide.class);

    static final String STATUS_NO_DATA = "no_data";
    static final String STATUS_NANO_MISSING = "nano_missing";

    /** The events whose nanosecond part is checked: when all of them have 0, a hop lacks the property. */
    static final int NANO_CHECK_EVENTS = 1_000;

    /**
     * The quiet time with open connections after a record, then before the first one, that stops the consumer, as
     * the specification sets them. The properties shorten them for the tests of the tool; they are read when a side
     * is created, after the {@code -D} options of the command line.
     */
    static final long NO_DATA_AFTER_RECORD_MILLIS = 30_000;
    static final long NO_DATA_BEFORE_RECORD_MILLIS = 60_000;
    static final String NO_DATA_AFTER_RECORD_PROPERTY = "com.devexperts.qd.tools.NetTestApi.noDataAfterRecordMillis";
    static final String NO_DATA_BEFORE_RECORD_PROPERTY = "com.devexperts.qd.tools.NetTestApi.noDataBeforeRecordMillis";

    private static final long WATCHDOG_STEP_MILLIS = 200;

    private final Watchdog watchdog = new Watchdog(
        TimeUnit.MILLISECONDS.toNanos(Long.getLong(NO_DATA_AFTER_RECORD_PROPERTY, NO_DATA_AFTER_RECORD_MILLIS)),
        TimeUnit.MILLISECONDS.toNanos(Long.getLong(NO_DATA_BEFORE_RECORD_PROPERTY, NO_DATA_BEFORE_RECORD_MILLIS)));
    private final boolean nanoCheck = isNanoTime();
    private final List<DXFeedSubscription<MarketEvent>> subscriptions = new ArrayList<>();
    private volatile boolean nanoChecked; // the check is over: a nanosecond part came or the check failed
    private long checkedEvents; // guarded by this

    NetTestApiConsumerSide(NetTestApi.Settings settings, NetTestApi.Stop stop) {
        super(settings, stop);
    }

    @Override
    String statisticsLabel() {
        return NetTestApiStatistics.CONSUMER_LABEL;
    }

    /**
     * The symbols of the instance {@code index} (from 1): those with the index {@code j} where
     * {@code j % C == index - 1}, so that the instances together subscribe to each symbol once.
     */
    static List<String> slice(String[] symbols, int index, int instances) {
        List<String> slice = new ArrayList<>();
        for (int j = index - 1; j < symbols.length; j += instances) {
            slice.add(symbols[j]);
        }
        return slice;
    }

    /**
     * Creates the endpoints, adds the listener and then the symbols to the subscription of each and connects it last.
     * Throws when an endpoint fails.
     */
    @Override
    public void start() {
        log.info("Receiving " + settings.event.text + " events through " + settings.role + " endpoints" +
            (settings.isConflated() ? " (conflated: Trade through the ticker contract)" : "") +
            (nanoCheck ? "" : " without the nanosecond part"));
        Class<? extends MarketEvent> type =
            settings.event == NetTestApi.EventKind.TRADE ? Trade.class : TimeAndSale.class;
        for (int i = 1; i <= config.instanceCount; i++) {
            DXEndpoint endpoint = createEndpoint(i);
            DXFeedSubscription<MarketEvent> subscription = endpoint.getFeed().createSubscription(type);
            subscription.addEventListener(new NetTestApiListener(this, counters[i - 1]));
            subscription.addSymbols(slice(getSymbols(), i, config.instanceCount));
            subscriptions.add(subscription);
            connect(i, endpoint);
        }
        startStatistics();
    }

    /** Waits for the end of the run and checks for the starvation on the way. */
    @Override
    void awaitEnd() throws InterruptedException {
        long end = settings.durationMillis > 0 ?
            System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(settings.durationMillis) : 0;
        while (!stop.isRequested()) {
            long step = WATCHDOG_STEP_MILLIS;
            if (end != 0) {
                long left = TimeUnit.NANOSECONDS.toMillis(end - System.nanoTime());
                if (left <= 0) {
                    return;
                }
                step = Math.min(step, left);
            }
            stop.await(step);
            String starvation = watchdog.check(System.nanoTime(), connectionCount());
            if (starvation != null) {
                fail(STATUS_NO_DATA, STATUS_NO_DATA + ": " + starvation, null);
            }
        }
    }

    @Override
    void stopWork() {
        for (DXFeedSubscription<MarketEvent> subscription : subscriptions) {
            subscription.close();
        }
    }

    /** Takes the records of one listener call at {@code nowNanos} of {@link System#nanoTime}. */
    void received(long nowNanos) {
        watchdog.received(nowNanos);
    }

    /** Whether the nanosecond parts of the events still need a check. */
    boolean needsNanoCheck() {
        return nanoCheck && !nanoChecked;
    }

    /** The events that the check still takes; a listener checks only that many of the first events of its call. */
    synchronized int nanoCheckRemaining() {
        return (int) Math.max(0, NANO_CHECK_EVENTS - checkedEvents);
    }

    /**
     * Takes {@code events} more events of the check, of which {@code withNanoPart} have a nonzero nanosecond part,
     * and stops the run with {@code nano_missing} when the first {@link #NANO_CHECK_EVENTS} all have 0.
     */
    void checkNanoParts(int events, int withNanoPart) {
        synchronized (this) {
            if (nanoChecked) {
                return;
            }
            if (withNanoPart > 0) {
                nanoChecked = true;
                return;
            }
            checkedEvents += events;
            if (checkedEvents < NANO_CHECK_EVENTS) {
                return;
            }
            nanoChecked = true;
        }
        fail(STATUS_NANO_MISSING, STATUS_NANO_MISSING + ": the first " + NANO_CHECK_EVENTS + " events have no " +
            "nanosecond part: a hop of the path lacks " + NANO_TIME_PROPERTY + "=true (the publisher, a multiplexor " +
            "or this consumer); -D" + NANO_TIME_PROPERTY + "=false turns the check off", null);
    }

    /**
     * The starvation check: with open connections, no records for {@code afterRecordNanos} after a record, or for
     * {@code beforeRecordNanos} after the connection when no record came at all. Times are of
     * {@link System#nanoTime}.
     */
    static final class Watchdog {
        private final long afterRecordNanos;
        private final long beforeRecordNanos;
        private volatile boolean anyRecord;
        private volatile long lastRecordNanos;
        private boolean connected; // the main thread only
        private long connectedNanos;

        Watchdog(long afterRecordNanos, long beforeRecordNanos) {
            this.afterRecordNanos = afterRecordNanos;
            this.beforeRecordNanos = beforeRecordNanos;
        }

        void received(long nowNanos) {
            lastRecordNanos = nowNanos;
            anyRecord = true;
        }

        /** Returns the reason of the starvation at {@code nowNanos} with {@code connections} open, or null. */
        String check(long nowNanos, int connections) {
            if (connections <= 0) {
                connected = false;
                return null;
            }
            if (!connected) {
                connected = true;
                connectedNanos = nowNanos;
            }
            boolean any = anyRecord;
            long quietSince = connectedNanos;
            if (any && lastRecordNanos - quietSince > 0) {
                quietSince = lastRecordNanos;
            }
            long limit = any ? afterRecordNanos : beforeRecordNanos;
            if (nowNanos - quietSince < limit) {
                return null;
            }
            return "no records for " + TimeUnit.NANOSECONDS.toMillis(limit) + " ms " +
                (any ? "after the last record" : "after the connection") + " with " + connections +
                " open connection(s)";
        }
    }
}
