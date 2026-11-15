// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The runtime values of the isolate at one moment: heap, GC, threads, process CPU time and uptime. Each value is read
 * on its own: a value the platform does not give, or whose source fails, is {@link #UNAVAILABLE} and leaves the others
 * as they are. The values are not one atomic snapshot.
 *
 * <p>The sources are the standard management beans, except the process CPU time, which comes from
 * {@link ProcessHandle.Info#totalCpuDuration()}: the {@code OperatingSystemMXBean} needs the {@code management_ext}
 * library of a JDK, which the SDK does not ship. The GC and the heap pools are mapped by the names of the beans of the
 * Serial GC of Native Image; another GC (G1) leaves those values unavailable.
 */
public final class RuntimeStatistics {

    /**
     * The value of a field that the platform does not give.
     */
    public static final long UNAVAILABLE = -1L;

    static final String YOUNG_COLLECTOR = "young generation scavenger";
    static final String FULL_COLLECTOR = "complete scavenger";
    static final String EDEN_POOL = "eden space";
    static final String SURVIVOR_POOL = "survivor space";
    static final String OLD_POOL = "old generation space";

    long heapUsedBytes = UNAVAILABLE;
    long heapCommittedBytes = UNAVAILABLE;
    long heapMaxBytes = UNAVAILABLE;
    long youngGcCount = UNAVAILABLE;
    long youngGcTimeMs = UNAVAILABLE;
    long fullGcCount = UNAVAILABLE;
    long fullGcTimeMs = UNAVAILABLE;
    long threadCount = UNAVAILABLE;
    long peakThreadCount = UNAVAILABLE;
    long processCpuTimeNs = UNAVAILABLE;
    long uptimeMs = UNAVAILABLE;
    long allocatedBytes = UNAVAILABLE;
    long edenUsedBytes = UNAVAILABLE;
    long survivorUsedBytes = UNAVAILABLE;
    long oldUsedBytes = UNAVAILABLE;
    long daemonThreadCount = UNAVAILABLE;
    long totalStartedThreadCount = UNAVAILABLE;
    long availableProcessors = UNAVAILABLE;

    private RuntimeStatistics() {
    }

    /**
     * Reads the values of the current isolate.
     *
     * @return the values; never null
     */
    public static RuntimeStatistics read() {
        return read(PlatformSources.INSTANCE);
    }

    static RuntimeStatistics read(final Sources sources) {
        final RuntimeStatistics statistics = new RuntimeStatistics();

        final MemoryMXBean memory = value(sources::memory);
        final MemoryUsage heap = memory == null ? null : value(memory::getHeapMemoryUsage);

        statistics.heapUsedBytes = heap == null ? UNAVAILABLE : guarded(heap::getUsed);
        statistics.heapCommittedBytes = heap == null ? UNAVAILABLE : guarded(heap::getCommitted);
        statistics.heapMaxBytes = heap == null ? UNAVAILABLE : guarded(heap::getMax);

        final List<GarbageCollectorMXBean> collectors = value(sources::collectors);
        final GarbageCollectorMXBean young = named(collectors, GarbageCollectorMXBean::getName, YOUNG_COLLECTOR);
        final GarbageCollectorMXBean full = named(collectors, GarbageCollectorMXBean::getName, FULL_COLLECTOR);

        statistics.youngGcCount = young == null ? UNAVAILABLE : guarded(young::getCollectionCount);
        statistics.youngGcTimeMs = young == null ? UNAVAILABLE : guarded(young::getCollectionTime);
        statistics.fullGcCount = full == null ? UNAVAILABLE : guarded(full::getCollectionCount);
        statistics.fullGcTimeMs = full == null ? UNAVAILABLE : guarded(full::getCollectionTime);

        final List<MemoryPoolMXBean> pools = value(sources::pools);

        statistics.edenUsedBytes = used(named(pools, MemoryPoolMXBean::getName, EDEN_POOL));
        statistics.survivorUsedBytes = used(named(pools, MemoryPoolMXBean::getName, SURVIVOR_POOL));
        statistics.oldUsedBytes = used(named(pools, MemoryPoolMXBean::getName, OLD_POOL));

        final ThreadMXBean threads = value(sources::threads);

        if (threads != null) {
            statistics.threadCount = guarded(threads::getThreadCount);
            statistics.peakThreadCount = guarded(threads::getPeakThreadCount);
            statistics.daemonThreadCount = guarded(threads::getDaemonThreadCount);
            statistics.totalStartedThreadCount = guarded(threads::getTotalStartedThreadCount);
            statistics.allocatedBytes = guarded(() -> threads instanceof com.sun.management.ThreadMXBean extended
                    ? extended.getTotalThreadAllocatedBytes()
                    : UNAVAILABLE);
        }

        final RuntimeMXBean runtime = value(sources::runtime);

        statistics.uptimeMs = runtime == null ? UNAVAILABLE : guarded(runtime::getUptime);
        statistics.processCpuTimeNs = guarded(() -> {
            final ProcessHandle.Info info = sources.processInfo();

            return info == null ? UNAVAILABLE : info.totalCpuDuration().map(Duration::toNanos).orElse(UNAVAILABLE);
        });
        statistics.availableProcessors = guarded(sources::availableProcessors);

        return statistics;
    }

    /**
     * Calls the source; a failure of the source makes the value {@link #UNAVAILABLE}. The failure may be an
     * {@link Error}: a {@link LinkageError} of a missing native library or an error of native-image about a feature
     * the image does not support. A {@link VirtualMachineError} (out of memory, stack overflow) is thrown on.
     */
    static long guarded(final LongSupplier source) {
        try {
            final long value = source.getAsLong();

            return value < 0 ? UNAVAILABLE : value;
        } catch (final VirtualMachineError error) {
            throw error;
        } catch (final RuntimeException | Error ignored) {
            return UNAVAILABLE;
        }
    }

    private static <T> T value(final Supplier<T> source) {
        try {
            return source.get();
        } catch (final VirtualMachineError error) {
            throw error;
        } catch (final RuntimeException | Error ignored) {
            return null;
        }
    }

    private static <T> T named(final List<T> beans, final Function<T, String> nameOf, final String name) {
        if (beans == null) {
            return null;
        }

        for (final T bean : beans) {
            if (name.equals(value(() -> nameOf.apply(bean)))) {
                return bean;
            }
        }

        return null;
    }

    private static long used(final MemoryPoolMXBean pool) {
        if (pool == null) {
            return UNAVAILABLE;
        }

        final MemoryUsage usage = value(pool::getUsage);

        return usage == null ? UNAVAILABLE : guarded(usage::getUsed);
    }

    /**
     * @return the used bytes of the heap, or {@link #UNAVAILABLE}
     */
    public long getHeapUsedBytes() {
        return heapUsedBytes;
    }

    /**
     * @return the committed bytes of the heap, or {@link #UNAVAILABLE}
     */
    public long getHeapCommittedBytes() {
        return heapCommittedBytes;
    }

    /**
     * @return the maximum bytes of the heap, or {@link #UNAVAILABLE}
     */
    public long getHeapMaxBytes() {
        return heapMaxBytes;
    }

    /**
     * @return the incremental (young) collections since the isolate started, or {@link #UNAVAILABLE}
     */
    public long getYoungGcCount() {
        return youngGcCount;
    }

    /**
     * @return the total time of the incremental (young) collections in milliseconds, or {@link #UNAVAILABLE}
     */
    public long getYoungGcTimeMs() {
        return youngGcTimeMs;
    }

    /**
     * @return the collections of the whole heap since the isolate started, or {@link #UNAVAILABLE}
     */
    public long getFullGcCount() {
        return fullGcCount;
    }

    /**
     * @return the total time of the collections of the whole heap in milliseconds, or {@link #UNAVAILABLE}
     */
    public long getFullGcTimeMs() {
        return fullGcTimeMs;
    }

    /**
     * @return the live Java threads of the isolate, attached threads included, or {@link #UNAVAILABLE}
     */
    public long getThreadCount() {
        return threadCount;
    }

    /**
     * @return the peak of the live Java threads of the isolate, or {@link #UNAVAILABLE}
     */
    public long getPeakThreadCount() {
        return peakThreadCount;
    }

    /**
     * @return the user plus system CPU time of the whole process in nanoseconds, or {@link #UNAVAILABLE}
     */
    public long getProcessCpuTimeNs() {
        return processCpuTimeNs;
    }

    /**
     * @return the time since the isolate started in milliseconds, or {@link #UNAVAILABLE}
     */
    public long getUptimeMs() {
        return uptimeMs;
    }

    /**
     * @return the bytes allocated by all threads of the isolate, or {@link #UNAVAILABLE}
     */
    public long getAllocatedBytes() {
        return allocatedBytes;
    }

    /**
     * @return the used bytes of the eden space, or {@link #UNAVAILABLE}
     */
    public long getEdenUsedBytes() {
        return edenUsedBytes;
    }

    /**
     * @return the used bytes of the survivor space, or {@link #UNAVAILABLE}
     */
    public long getSurvivorUsedBytes() {
        return survivorUsedBytes;
    }

    /**
     * @return the used bytes of the old generation, or {@link #UNAVAILABLE}
     */
    public long getOldUsedBytes() {
        return oldUsedBytes;
    }

    /**
     * @return the live daemon threads of the isolate, or {@link #UNAVAILABLE}
     */
    public long getDaemonThreadCount() {
        return daemonThreadCount;
    }

    /**
     * @return the threads started in the isolate since it started, or {@link #UNAVAILABLE}
     */
    public long getTotalStartedThreadCount() {
        return totalStartedThreadCount;
    }

    /**
     * @return the processors available to the process, or {@link #UNAVAILABLE}
     */
    public long getAvailableProcessors() {
        return availableProcessors;
    }

    /**
     * The sources of the values; tests replace them.
     */
    interface Sources {

        MemoryMXBean memory();

        List<GarbageCollectorMXBean> collectors();

        List<MemoryPoolMXBean> pools();

        ThreadMXBean threads();

        RuntimeMXBean runtime();

        ProcessHandle.Info processInfo();

        long availableProcessors();
    }

    /**
     * The beans of the platform, fetched per call: they are singletons of the image heap, no state is kept here.
     */
    private enum PlatformSources implements Sources {
        INSTANCE;

        @Override
        public MemoryMXBean memory() {
            return ManagementFactory.getMemoryMXBean();
        }

        @Override
        public List<GarbageCollectorMXBean> collectors() {
            return ManagementFactory.getGarbageCollectorMXBeans();
        }

        @Override
        public List<MemoryPoolMXBean> pools() {
            return ManagementFactory.getMemoryPoolMXBeans();
        }

        @Override
        public ThreadMXBean threads() {
            return ManagementFactory.getThreadMXBean();
        }

        @Override
        public RuntimeMXBean runtime() {
            return ManagementFactory.getRuntimeMXBean();
        }

        @Override
        public ProcessHandle.Info processInfo() {
            return ProcessHandle.current().info();
        }

        @Override
        public long availableProcessors() {
            return Runtime.getRuntime().availableProcessors();
        }
    }
}
