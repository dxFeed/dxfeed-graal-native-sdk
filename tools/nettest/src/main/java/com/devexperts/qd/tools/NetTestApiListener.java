// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import java.time.Instant;
import java.util.List;

/**
 * The listener of one consumer instance: counts the events and sums their end-to-end lag in nanoseconds, the time of
 * the call minus the time of the event with its nanosecond part. The clock is read once per call. A negative lag (a
 * clock step or skew) enters the sum with its sign.
 */
final class NetTestApiListener implements DXFeedEventListener<MarketEvent> {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final NetTestApiConsumerSide side;
    private final NetTestApiStatistics.Counter counter;

    NetTestApiListener(NetTestApiConsumerSide side, NetTestApiStatistics.Counter counter) {
        this.side = side;
        this.counter = counter;
    }

    @Override
    public void eventsReceived(List<MarketEvent> events) {
        Instant now = Instant.now();
        long nowNanos = now.getEpochSecond() * NANOS_PER_SECOND + now.getNano();
        long lagSum = 0;
        for (MarketEvent event : events) {
            lagSum += nowNanos - timeNanos(event);
        }
        counter.add(events.size(), lagSum);
        side.received(System.nanoTime());
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

    /** The time of the event in nanoseconds since the epoch: its milliseconds and its nanosecond part. */
    static long timeNanos(MarketEvent event) {
        return event instanceof Trade ? ((Trade) event).getTimeNanos() : ((TimeAndSale) event).getTimeNanos();
    }

    private static int nanoPart(MarketEvent event) {
        return event instanceof Trade ? ((Trade) event).getTimeNanoPart() : ((TimeAndSale) event).getTimeNanoPart();
    }
}
