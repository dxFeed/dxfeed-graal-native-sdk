// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The listener of one consumer instance: counts the events and their end-to-end lag in nanoseconds, the time of the
 * call minus the time of the event with its nanosecond part. The clock is read once per call. A negative lag (a clock
 * step or skew) enters the sum and the maximum with its sign, is counted as negative and enters the histogram as 0.
 */
class NetTestApiListener implements DXFeedEventListener<MarketEvent> {
    private final NetTestApiConsumerSide side;
    private final NetTestApiStatistics.Counter counter;
    private final int sliceSize; // the symbols of the instance
    private final Set<String> seen = new HashSet<>(); // guarded by itself
    private volatile int seenCount;

    NetTestApiListener(NetTestApiConsumerSide side, NetTestApiStatistics.Counter counter, int sliceSize) {
        this.side = side;
        this.counter = counter;
        this.sliceSize = sliceSize;
    }

    @Override
    public void eventsReceived(List<MarketEvent> events) {
        long nowNanos = epochNanos();
        long lagSum = 0;
        long negative = 0;
        long maxLag = Long.MIN_VALUE;
        long[] buckets = new long[NetTestApiMetrics.LAG_BUCKET_NANOS.length + 1];
        boolean seeing = seenCount < sliceSize;
        for (MarketEvent event : events) {
            long lag = nowNanos - timeNanos(event);
            lagSum += lag;
            if (lag < 0) {
                negative++;
            }
            if (lag > maxLag) {
                maxLag = lag;
            }
            side.recordLag(lag);
            buckets[NetTestApiMetrics.lagBucket(lag)]++;
            if (seeing) {
                see(event.getEventSymbol());
            }
        }
        counter.addLags(events.size(), lagSum, negative, maxLag);
        side.addLagBuckets(buckets);
        side.received(System.nanoTime(), nowNanos);
        if (side.needsNanoCheck()) {
            // Only the events that are still among the first ones of the check count.
            int checked = Math.min(events.size(), side.nanoCheckRemaining());
            int withNanoPart = 0;
            for (int i = 0; i < checked; i++) {
                if (nanoPart(events.get(i)) != 0) {
                    withNanoPart++;
                }
            }
            side.checkNanoParts(checked, withNanoPart);
        }
    }

    /** The symbols of the instance received so far. */
    int symbolsSeen() {
        return seenCount;
    }

    private void see(String symbol) {
        synchronized (seen) {
            if (seen.add(symbol)) {
                seenCount = seen.size();
            }
        }
    }

    /** The time of the call in nanoseconds since the epoch; a test may shift it. */
    long epochNanos() {
        return NetTestApiClock.epochNanos();
    }

    /** The time of the event in nanoseconds since the epoch: its milliseconds and its nanosecond part. */
    static long timeNanos(MarketEvent event) {
        return event instanceof Trade ? ((Trade) event).getTimeNanos() : ((TimeAndSale) event).getTimeNanos();
    }

    private static int nanoPart(MarketEvent event) {
        return event instanceof Trade ? ((Trade) event).getTimeNanoPart() : ((TimeAndSale) event).getTimeNanoPart();
    }
}
