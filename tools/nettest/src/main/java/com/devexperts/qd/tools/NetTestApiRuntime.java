// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The runtime line of the JVM after each statistics block, in the format of the C-API NetTest:
 * {@code {*NetTestRuntime-<name>*} heap U MB of C MB, gc N (T ms), cpu P%}, where each value may be {@code n/a}.
 */
final class NetTestApiRuntime {
    static final long ABSENT = -1;

    private static final String ABSENT_TEXT = "n/a";
    private static final long BYTES_PER_MB = 1024 * 1024;

    private NetTestApiRuntime() {}

    /** One reading of the JVM; {@link #ABSENT} marks a value the JVM does not give. */
    static final class Sample {
        final long heapUsedBytes;
        final long heapCommittedBytes;
        final long gcCount; // the pauses of all counted collectors since the start
        final long gcTimeMillis;
        final long processCpuNanos;

        Sample(long heapUsedBytes, long heapCommittedBytes, long gcCount, long gcTimeMillis, long processCpuNanos) {
            this.heapUsedBytes = heapUsedBytes;
            this.heapCommittedBytes = heapCommittedBytes;
            this.gcCount = gcCount;
            this.gcTimeMillis = gcTimeMillis;
            this.processCpuNanos = processCpuNanos;
        }
    }

    /**
     * Whether the collections of a {@link GarbageCollectorMXBean} are counted: the pauses are, the concurrent cycles
     * of G1 ({@code G1 Concurrent GC}), ZGC and Shenandoah ({@code ... Cycles}) are not, so that {@code gc} is the
     * stop-the-world collections and their time, as the young and full collections of the Serial GC in the isolate.
     */
    static boolean countsPauses(String collectorName) {
        return !collectorName.endsWith(" Cycles") && !collectorName.equals("G1 Concurrent GC");
    }

    /** Reads the heap, the counted collectors and the CPU time of the process. */
    static Sample read() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long gcCount = 0;
        long gcTimeMillis = 0;
        boolean counted = false;
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (!countsPauses(collector.getName())) {
                continue;
            }
            long count = collector.getCollectionCount();
            long time = collector.getCollectionTime();
            if (count < 0 || time < 0) {
                gcCount = ABSENT;
                gcTimeMillis = ABSENT;
                break;
            }
            gcCount += count;
            gcTimeMillis += time;
            counted = true;
        }
        if (!counted) {
            gcCount = ABSENT;
            gcTimeMillis = ABSENT;
        }
        long cpuNanos = ProcessHandle.current().info().totalCpuDuration().map(Duration::toNanos).orElse(ABSENT);
        return new Sample(heap.getUsed(), heap.getCommitted(), gcCount, gcTimeMillis, cpuNanos);
    }

    /**
     * Formats the line of a period: the heap now, the counted collections of the period and their total time, the
     * CPU time of the process in the period as a share of its length (above 100% on several cores). A value absent
     * in either sample, or a counter that went back, is {@code n/a}.
     */
    static String format(String name, Sample previous, Sample current, long periodNanos) {
        long gcCount = growth(previous.gcCount, current.gcCount);
        long gcTimeMillis = growth(previous.gcTimeMillis, current.gcTimeMillis);
        long cpuNanos = growth(previous.processCpuNanos, current.processCpuNanos);
        StringBuilder text = new StringBuilder("{*NetTestRuntime-").append(name).append("*} heap ")
            .append(megabytes(current.heapUsedBytes)).append(" of ").append(megabytes(current.heapCommittedBytes))
            .append(", gc ");
        if (present(gcCount) && present(gcTimeMillis)) {
            text.append(groupDigits(gcCount)).append(" (").append(groupDigits(gcTimeMillis)).append(" ms)");
        } else {
            text.append(ABSENT_TEXT);
        }
        text.append(", cpu ");
        // The CPU nanoseconds of a period are far below the long range divided by 100.
        if (present(cpuNanos) && periodNanos > 0) {
            text.append(groupDigits(cpuNanos * 100 / periodNanos)).append('%');
        } else {
            text.append(ABSENT_TEXT);
        }
        return text.toString();
    }

    /**
     * Prints lines to stdout in one write: the lines of {@code text} are separated by {@code \n}, which becomes the
     * platform's line break.
     */
    static void print(String text) {
        String lines = text.replace("\n", System.lineSeparator()) + System.lineSeparator();
        synchronized (System.out) {
            System.out.print(lines);
            System.out.flush();
        }
    }

    /** The statistics blocks of the intervals, see {@link NetTestApiStatistics#tick}. */
    interface Blocks {
        /**
         * Ends an interval, the {@code last} one at the stop, and returns its block, or {@code null} when nothing is
         * printed for it.
         */
        String tick(boolean last);
    }

    /**
     * Logs the statistics block of each interval and the runtime line after it in one write, so that no other output
     * comes between them. The first run only starts the count. {@link #finish} ends the last interval, whose block
     * the {@link Blocks} may print even without a run before it; after it nothing is logged. The runtime line covers
     * the time since the previous block, or since the creation of the reporter before the first one.
     */
    static final class Reporter implements Runnable {
        private final Blocks blocks;
        private final String name;
        private final Supplier<Sample> source;
        private final Consumer<String> output;
        private Sample previous; // guarded by this
        private long previousNanos; // guarded by this
        private boolean finished; // guarded by this

        Reporter(Blocks blocks, String name) {
            this(blocks, name, NetTestApiRuntime::read, NetTestApiRuntime::print);
        }

        Reporter(Blocks blocks, String name, Supplier<Sample> source, Consumer<String> output) {
            this.blocks = blocks;
            this.name = name;
            this.source = source;
            this.output = output;
            this.previous = source.get();
            this.previousNanos = System.nanoTime();
        }

        @Override
        public synchronized void run() {
            report(false);
        }

        /** Logs the last block once and stops the reporting. */
        synchronized void finish() {
            report(true);
            finished = true;
        }

        private void report(boolean last) {
            if (finished) {
                return;
            }
            String block = blocks.tick(last);
            if (block == null) {
                // The next block starts here: the first tick or a short last interval.
                previous = source.get();
                previousNanos = System.nanoTime();
                return;
            }
            Sample current = source.get();
            long currentNanos = System.nanoTime();
            output.accept(block + "\n" + format(name, previous, current, currentNanos - previousNanos));
            previous = current;
            previousNanos = currentNanos;
        }
    }

    private static boolean present(long value) {
        return value >= 0;
    }

    private static long growth(long previous, long current) {
        return present(previous) && present(current) && current >= previous ? current - previous : ABSENT;
    }

    private static String megabytes(long bytes) {
        return present(bytes) ? groupDigits(bytes / BYTES_PER_MB) + " MB" : ABSENT_TEXT;
    }

    private static String groupDigits(long value) {
        return NetTestApiStatistics.groupDigits(value);
    }
}
