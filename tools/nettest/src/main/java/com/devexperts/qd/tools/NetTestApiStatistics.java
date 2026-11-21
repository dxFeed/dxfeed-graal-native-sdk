// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

/**
 * The intervals of the NetTest specification (sections "Output lines" and "Summary file"): the statistics block
 * {@code {*NetTestProducerSide-<name>*} data <rps> rps, lag <us> us, instances <C>, connections <M>} or the same with
 * {@code NetTestConsumerSide}, followed by a line per instance, and the values of the interval for the summary. The lag
 * is the sum of the nanoseconds of the interval over its records: on {@code p} the fill and publishing time of each
 * batch weighted by its records, on {@code c} the end-to-end lag of each event, which may be negative.
 */
final class NetTestApiStatistics {
    static final String PRODUCER_LABEL = "NetTestProducerSide";
    static final String CONSUMER_LABEL = "NetTestConsumerSide";

    /** The e2e lag histogram of the specification: 1 ns to 3600 s, 3 significant digits. */
    static final long HISTOGRAM_LOWEST = 1;
    static final long HISTOGRAM_HIGHEST = 3_600_000_000_000L;
    static final int HISTOGRAM_DIGITS = 3;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final String label;
    private final String name;
    private final Counter[] counters;
    private final IntSupplier connections;
    private final Recorder lags; // the e2e lags of the consumer; null on the producer
    private final NetTestApiSchedule schedule; // the schedule of the producer; null on the consumer
    private final long warmupNanos; // 0 without --warmup
    private final LongSupplier warmupStart; // the epoch nanoseconds where the warm-up starts, 0 before it starts
    private long[][] last; // the counters at the last tick (zeros at the start), guarded by this
    private long lastNanos;
    private long lastEpochNanos;
    private boolean ticked; // whether a tick has ended an interval, guarded by this

    /** The cumulative values of one instance. */
    static final class Counter {
        static final int RECORDS = 0;
        static final int NANOS = 1; // the weighted publishing nanoseconds on p, the e2e lag sum on c
        static final int CALLS = 2; // the publishing calls on p
        static final int CALL_NANOS = 3; // the nanoseconds of the fills and the publishing calls on p
        static final int NEGATIVE = 4; // the negative lags on c
        static final int SIZE = 5;

        private final long[] values = new long[SIZE]; // guarded by this
        private long max = Long.MIN_VALUE; // the largest lag since the last tick, guarded by this

        synchronized void add(long records, long nanos) {
            values[RECORDS] += records;
            values[NANOS] += nanos;
        }

        /** Adds a published batch of {@code count} events whose fill and call took {@code batchNanos}. */
        synchronized void addBatch(int count, long batchNanos) {
            values[RECORDS] += count;
            values[NANOS] += batchNanos * count;
            values[CALLS]++;
            values[CALL_NANOS] += batchNanos;
        }

        /** Adds the lags of received events: their sum, the negative ones and the largest one. */
        synchronized void addLags(long records, long lagSum, long negative, long maxLag) {
            values[RECORDS] += records;
            values[NANOS] += lagSum;
            values[NEGATIVE] += negative;
            if (records > 0 && maxLag > max) {
                max = maxLag;
            }
        }

        /** Reads the records into {@code target[0]} and the nanoseconds into {@code target[1]} at once. */
        synchronized void read(long[] target) {
            System.arraycopy(values, 0, target, 0, Math.min(target.length, SIZE));
        }

        /** The largest lag since the previous call, {@link Long#MIN_VALUE} without one, and the start of a new count. */
        synchronized long takeMax() {
            long result = max;
            max = Long.MIN_VALUE;
            return result;
        }
    }

    /** One interval: its block and its values. */
    static final class Interval {
        final String text;
        final long startEpochMs;
        final long endEpochMs;
        final long lengthNanos;
        final boolean warmup;
        final long records;
        final long nanos;
        final long calls;
        final long callNanos;
        final long negative;
        final long maxLag; // Long.MIN_VALUE without records
        final Long lagP50; // the median lag of the interval on c, null on p and without records
        final Long lagP99;
        // The lags of the interval on c, null on p; the summary drops it once it has merged it.
        Histogram histogram;
        final Long slipMax; // null without a schedule
        final Long slipEnd;
        final int connections;

        Interval(String text, long startEpochMs, long endEpochMs, long lengthNanos, boolean warmup, long[] totals,
            long maxLag, Histogram histogram, Long slipMax, Long slipEnd, int connections)
        {
            this.text = text;
            this.startEpochMs = startEpochMs;
            this.endEpochMs = endEpochMs;
            this.lengthNanos = lengthNanos;
            this.warmup = warmup;
            this.records = totals[Counter.RECORDS];
            this.nanos = totals[Counter.NANOS];
            this.calls = totals[Counter.CALLS];
            this.callNanos = totals[Counter.CALL_NANOS];
            this.negative = totals[Counter.NEGATIVE];
            this.maxLag = maxLag;
            boolean lags = histogram != null && histogram.getTotalCount() > 0;
            this.lagP50 = lags ? histogram.getValueAtPercentile(50) : null;
            this.lagP99 = lags ? histogram.getValueAtPercentile(99) : null;
            this.histogram = histogram;
            this.slipMax = slipMax;
            this.slipEnd = slipEnd;
            this.connections = connections;
        }

        /** The records per second of the interval. */
        long rate() {
            return NetTestApiStatistics.rate(records, lengthNanos);
        }
    }

    NetTestApiStatistics(String label, String name, Counter[] counters, IntSupplier connections) {
        this(label, name, counters, connections, null, null, 0, () -> 0);
    }

    NetTestApiStatistics(String label, String name, Counter[] counters, IntSupplier connections, Recorder lags,
        NetTestApiSchedule schedule, long warmupNanos, LongSupplier warmupStart)
    {
        this.label = label;
        this.name = name;
        this.counters = counters;
        this.connections = connections;
        this.lags = lags;
        this.schedule = schedule;
        this.warmupNanos = warmupNanos;
        this.warmupStart = warmupStart;
        // The first interval begins here with zero counts, so the intervals cover the whole run.
        start(System.nanoTime(), NetTestApiClock.epochNanos(), new long[counters.length][Counter.SIZE]);
    }

    /** A recorder of e2e lags with the histogram of the specification. */
    static Recorder newLagRecorder() {
        return new Recorder(HISTOGRAM_LOWEST, HISTOGRAM_HIGHEST, HISTOGRAM_DIGITS);
    }

    /** A histogram of e2e lags of the specification. */
    static Histogram newLagHistogram() {
        return new Histogram(HISTOGRAM_LOWEST, HISTOGRAM_HIGHEST, HISTOGRAM_DIGITS);
    }

    /** The value that a lag enters the histogram with: 0 for a negative lag, the highest value above it. */
    static long histogramValue(long lagNanos) {
        return lagNanos < 0 ? 0 : Math.min(lagNanos, HISTOGRAM_HIGHEST);
    }

    /**
     * Ends an interval and returns it. The first interval begins when the statistics are created, each next one where
     * the previous one ends, so the intervals cover the whole run. The {@link Interval#text} of the interval is
     * {@code null} (no block is printed) for the first tick, which ends the part before the first multiple of the
     * period, and for an interval shorter than {@code minNanos}.
     */
    synchronized Interval tick(long minNanos) {
        long now = System.nanoTime();
        long epochNanos = NetTestApiClock.epochNanos();
        long[][] values = new long[counters.length][Counter.SIZE];
        for (int i = 0; i < counters.length; i++) {
            counters[i].read(values[i]);
        }
        long length = now - lastNanos;
        boolean printed = ticked && length >= minNanos;
        ticked = true;
        long[] totals = new long[Counter.SIZE];
        long[] records = new long[counters.length];
        long[] nanos = new long[counters.length];
        long maxLag = Long.MIN_VALUE;
        for (int i = 0; i < counters.length; i++) {
            for (int k = 0; k < Counter.SIZE; k++) {
                totals[k] += values[i][k] - last[i][k];
            }
            records[i] = values[i][Counter.RECORDS] - last[i][Counter.RECORDS];
            nanos[i] = values[i][Counter.NANOS] - last[i][Counter.NANOS];
            maxLag = Math.max(maxLag, counters[i].takeMax());
        }
        Histogram histogram = lags == null ? null : lags.getIntervalHistogram();
        Long slipMax = schedule != null && schedule.isLimited() ? schedule.takeSlipMax() : null;
        Long slipEnd = slipMax != null ? schedule.lastSlip() : null;
        int open = connections.getAsInt();
        long startEpochNanos = lastEpochNanos;
        start(now, epochNanos, values);
        return new Interval(printed ? format(label, name, length, open, records, nanos) : null,
            startEpochNanos / NANOS_PER_MILLI,
            epochNanos / NANOS_PER_MILLI, length, isWarmup(startEpochNanos, warmupNanos, warmupStart.getAsLong()),
            totals, maxLag, histogram, slipMax, slipEnd, open);
    }

    /**
     * Whether an interval that begins at {@code startEpochNanos} belongs to the warm-up of {@code warmupNanos} that
     * starts at {@code warmupStartNanos} (0 before it starts): all intervals until the warm-up starts, and those that
     * begin before it ends, so an interval across the end of the warm-up is warm-up.
     */
    static boolean isWarmup(long startEpochNanos, long warmupNanos, long warmupStartNanos) {
        if (warmupNanos <= 0) {
            return false;
        }
        return warmupStartNanos == 0 || startEpochNanos < warmupStartNanos + warmupNanos;
    }

    private void start(long now, long epochNanos, long[][] values) {
        lastNanos = now;
        lastEpochNanos = epochNanos;
        last = values;
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
