// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * The statistics block of the NetTest specification (section "Output lines"):
 * {@code {*NetTestProducerSide-<name>*} data <rps> rps, lag <us> us, instances <C>, connections <M>} or the same with
 * {@code NetTestConsumerSide}, followed by a line per instance. The lag is the sum of the nanoseconds of the interval
 * over its records: on {@code p} the fill and publishing time of each batch weighted by its records, on {@code c} the
 * end-to-end lag of each event, which may be negative.
 */
final class NetTestApiStatistics {
    static final String PRODUCER_LABEL = "NetTestProducerSide";
    static final String CONSUMER_LABEL = "NetTestConsumerSide";

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final String label;
    private final String name;
    private final Counter[] counters;
    private final IntSupplier connections;
    private long[] lastRecords; // guarded by this; null before the first tick
    private long[] lastNanos;
    private long lastTime;

    /** The records and the nanoseconds of one instance, cumulative. */
    static final class Counter {
        private long records; // guarded by this
        private long nanos;

        synchronized void add(long records, long nanos) {
            this.records += records;
            this.nanos += nanos;
        }

        /** Reads the records into {@code target[0]} and the nanoseconds into {@code target[1]} at once. */
        synchronized void read(long[] target) {
            target[0] = records;
            target[1] = nanos;
        }
    }

    NetTestApiStatistics(String label, String name, Counter[] counters, IntSupplier connections) {
        this.label = label;
        this.name = name;
        this.counters = counters;
        this.connections = connections;
    }

    /**
     * Ends an interval: returns its block, or {@code null} for the first tick, which only starts the count, and for
     * an interval shorter than {@code minNanos}.
     */
    synchronized String tick(long minNanos) {
        long now = System.nanoTime();
        long[] records = new long[counters.length];
        long[] nanos = new long[counters.length];
        long[] value = new long[2];
        for (int i = 0; i < counters.length; i++) {
            counters[i].read(value);
            records[i] = value[0];
            nanos[i] = value[1];
        }
        if (lastRecords == null) {
            start(now, records, nanos);
            return null;
        }
        long interval = now - lastTime;
        if (interval < minNanos) {
            return null;
        }
        long[] intervalRecords = new long[counters.length];
        long[] intervalNanos = new long[counters.length];
        for (int i = 0; i < counters.length; i++) {
            intervalRecords[i] = records[i] - lastRecords[i];
            intervalNanos[i] = nanos[i] - lastNanos[i];
        }
        start(now, records, nanos);
        return format(label, name, interval, connections.getAsInt(), intervalRecords, intervalNanos);
    }

    private void start(long now, long[] records, long[] nanos) {
        lastTime = now;
        lastRecords = records;
        lastNanos = nanos;
    }

    /**
     * Formats a block from the records and the nanoseconds of each instance in an interval of {@code intervalNanos};
     * the lines are separated by {@code \n}, without the last line break.
     */
    static String format(String label, String name, long intervalNanos, int connections, long[] records,
        long[] nanos)
    {
        long totalRecords = 0;
        long totalNanos = 0;
        for (int i = 0; i < records.length; i++) {
            totalRecords += records[i];
            totalNanos += nanos[i];
        }
        StringBuilder text = new StringBuilder("{*").append(label).append('-').append(name).append("*} data ")
            .append(groupDigits(rate(totalRecords, intervalNanos))).append(" rps, lag ")
            .append(groupDigits(lagMicros(totalRecords, totalNanos))).append(" us, instances ").append(records.length)
            .append(", connections ").append(connections);
        for (int i = 0; i < records.length; i++) {
            text.append("\n    ").append(name).append('.').append(i + 1).append(" data ")
                .append(groupDigits(rate(records[i], intervalNanos))).append(" rps, lag ")
                .append(groupDigits(lagMicros(records[i], nanos[i]))).append(" us");
        }
        return text.toString();
    }

    /** {@code records * 1000000000 / intervalNanos} with integer division, 0 for an empty interval. */
    static long rate(long records, long intervalNanos) {
        if (records <= 0 || intervalNanos <= 0) {
            return 0;
        }
        try {
            return Math.multiplyExact(records, NANOS_PER_SECOND) / intervalNanos;
        } catch (ArithmeticException e) {
            return BigInteger.valueOf(records).multiply(BigInteger.valueOf(NANOS_PER_SECOND))
                .divide(BigInteger.valueOf(intervalNanos)).longValue();
        }
    }

    /** {@code nanos / records / 1000}, divided toward zero, 0 without records. */
    static long lagMicros(long records, long nanos) {
        return records <= 0 ? 0 : nanos / records / 1000;
    }

    static String groupDigits(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }
}
