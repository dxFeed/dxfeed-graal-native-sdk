// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import static com.dxfeed.sdk.system.RuntimeStatistics.UNAVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

public class RuntimeStatisticsTest {

    @Test
    public void serialCollectorsMapToYoungAndFull() {
        final FakeSources sources = new FakeSources();
        sources.collectors = List.of(collector(RuntimeStatistics.YOUNG_COLLECTOR, 5, 12),
                collector(RuntimeStatistics.FULL_COLLECTOR, 1, 40));

        final RuntimeStatistics statistics = RuntimeStatistics.read(sources);

        assertEquals(5, statistics.getYoungGcCount());
        assertEquals(12, statistics.getYoungGcTimeMs());
        assertEquals(1, statistics.getFullGcCount());
        assertEquals(40, statistics.getFullGcTimeMs());
    }

    @Test
    public void noCollectorsLeaveTheGcUnavailable() {
        final FakeSources sources = new FakeSources();
        sources.collectors = List.of();

        assertGcUnavailable(RuntimeStatistics.read(sources));
    }

    @Test
    public void unknownCollectorNamesLeaveTheGcUnavailable() {
        final FakeSources sources = new FakeSources();
        sources.collectors = List.of(collector("G1 Young Generation", 5, 12), collector("G1 Old Generation", 1, 40));

        assertGcUnavailable(RuntimeStatistics.read(sources));
    }

    @Test
    public void failingCollectorLeavesOnlyItsPairUnavailable() {
        final FakeSources sources = new FakeSources();
        sources.collectors = List.of(bean(GarbageCollectorMXBean.class, Map.of(
                        "getName", RuntimeStatistics.YOUNG_COLLECTOR,
                        "getCollectionCount", new UnsupportedOperationException(),
                        "getCollectionTime", new UnsupportedOperationException())),
                collector(RuntimeStatistics.FULL_COLLECTOR, 1, 40));

        final RuntimeStatistics statistics = RuntimeStatistics.read(sources);

        assertEquals(UNAVAILABLE, statistics.getYoungGcCount());
        assertEquals(UNAVAILABLE, statistics.getYoungGcTimeMs());
        assertEquals(1, statistics.getFullGcCount());
        assertEquals(40, statistics.getFullGcTimeMs());
    }

    @Test
    public void undefinedCollectionTimeKeepsTheCount() {
        final FakeSources sources = new FakeSources();
        sources.collectors = List.of(collector(RuntimeStatistics.YOUNG_COLLECTOR, 5, -1));

        final RuntimeStatistics statistics = RuntimeStatistics.read(sources);

        assertEquals(5, statistics.getYoungGcCount());
        assertEquals(UNAVAILABLE, statistics.getYoungGcTimeMs());
    }

    @Test
    public void serialPoolsMapToThePoolFields() {
        final FakeSources sources = new FakeSources();
        sources.pools = List.of(pool(RuntimeStatistics.EDEN_POOL, 10),
                pool(RuntimeStatistics.SURVIVOR_POOL, 2),
                pool(RuntimeStatistics.OLD_POOL, 30));

        final RuntimeStatistics statistics = RuntimeStatistics.read(sources);

        assertEquals(10, statistics.getEdenUsedBytes());
        assertEquals(2, statistics.getSurvivorUsedBytes());
        assertEquals(30, statistics.getOldUsedBytes());
    }

    @Test
    public void unknownOrMissingPoolsLeaveThePoolFieldsUnavailable() {
        final FakeSources sources = new FakeSources();
        sources.pools = List.of(pool("G1 Eden Space", 10), pool("G1 Old Gen", 30));
        assertPoolsUnavailable(RuntimeStatistics.read(sources));

        sources.pools = List.of();
        assertPoolsUnavailable(RuntimeStatistics.read(sources));
    }

    @Test
    public void failingPoolLeavesOnlyItsFieldUnavailable() {
        final FakeSources sources = new FakeSources();
        sources.pools = List.of(pool(RuntimeStatistics.EDEN_POOL, 10),
                bean(MemoryPoolMXBean.class, Map.of("getName", RuntimeStatistics.OLD_POOL)));

        final RuntimeStatistics statistics = RuntimeStatistics.read(sources);

        assertEquals(10, statistics.getEdenUsedBytes());
        assertEquals(UNAVAILABLE, statistics.getOldUsedBytes());
    }

    @Test
    public void heapComesFromTheMemoryBean() {
        final FakeSources sources = new FakeSources();
        sources.memory = bean(MemoryMXBean.class, Map.of("getHeapMemoryUsage", new MemoryUsage(-1, 100, 200, 400)));

        final RuntimeStatistics statistics = RuntimeStatistics.read(sources);

        assertEquals(100, statistics.getHeapUsedBytes());
        assertEquals(200, statistics.getHeapCommittedBytes());
        assertEquals(400, statistics.getHeapMaxBytes());
    }

    @Test
    public void threadValuesComeFromTheThreadBean() {
        final FakeSources sources = new FakeSources();
        sources.threads = bean(ThreadMXBean.class, Map.of(
                "getThreadCount", 4,
                "getPeakThreadCount", 6,
                "getDaemonThreadCount", 3,
                "getTotalStartedThreadCount", 9L));

        final RuntimeStatistics statistics = RuntimeStatistics.read(sources);

        assertEquals(4, statistics.getThreadCount());
        assertEquals(6, statistics.getPeakThreadCount());
        assertEquals(3, statistics.getDaemonThreadCount());
        assertEquals(9, statistics.getTotalStartedThreadCount());
        // A plain java.lang.management.ThreadMXBean has no total of the allocated bytes.
        assertEquals(UNAVAILABLE, statistics.getAllocatedBytes());
    }

    @Test
    public void allocatedBytesComeFromTheExtendedThreadBean() {
        final FakeSources sources = new FakeSources();
        sources.threads = bean(com.sun.management.ThreadMXBean.class, Map.of("getTotalThreadAllocatedBytes", 1234L));
        assertEquals(1234, RuntimeStatistics.read(sources).getAllocatedBytes());

        // GraalVM 25.5 does not implement the total: the default of the JDK returns -1.
        sources.threads = bean(com.sun.management.ThreadMXBean.class, Map.of("getTotalThreadAllocatedBytes", -1L));
        assertEquals(UNAVAILABLE, RuntimeStatistics.read(sources).getAllocatedBytes());
    }

    @Test
    public void processCpuTimeComesFromTheProcessHandle() {
        final FakeSources sources = new FakeSources();
        sources.processInfo = info(Optional.of(Duration.ofMillis(1500)));

        assertEquals(1_500_000_000L, RuntimeStatistics.read(sources).getProcessCpuTimeNs());
    }

    @Test
    public void unreadableProcessCpuTimeLeavesTheOtherFieldsFilled() {
        // Where the platform does not give the CPU time (an empty duration or a failing source), only that field is
        // unavailable.
        final FakeSources sources = new FakeSources();
        sources.memory = bean(MemoryMXBean.class, Map.of("getHeapMemoryUsage", new MemoryUsage(-1, 100, 200, 400)));
        sources.collectors = List.of(collector(RuntimeStatistics.YOUNG_COLLECTOR, 5, 12));
        sources.runtime = bean(RuntimeMXBean.class, Map.of("getUptime", 77L));
        sources.availableProcessors = 8;

        sources.processInfo = info(Optional.empty());
        assertOnlyCpuUnavailable(RuntimeStatistics.read(sources));

        sources.processInfoFailure = new UnsupportedOperationException();
        assertOnlyCpuUnavailable(RuntimeStatistics.read(sources));

        sources.processInfoFailure = new UnsatisfiedLinkError();
        assertOnlyCpuUnavailable(RuntimeStatistics.read(sources));

        // An error of native-image about an unsupported feature is not a LinkageError.
        sources.processInfoFailure = new Error("unsupported in the image");
        assertOnlyCpuUnavailable(RuntimeStatistics.read(sources));
    }

    @Test
    public void virtualMachineErrorOfASourceIsThrownOn() {
        final FakeSources sources = new FakeSources();
        sources.processInfoFailure = new OutOfMemoryError();

        assertThrows(OutOfMemoryError.class, () -> RuntimeStatistics.read(sources));
    }

    @Test
    public void failingSourcesLeaveEveryFieldUnavailable() {
        final FakeSources sources = new FakeSources();
        sources.failAll = true;

        final RuntimeStatistics statistics = RuntimeStatistics.read(sources);

        assertEquals(UNAVAILABLE, statistics.getHeapUsedBytes());
        assertGcUnavailable(statistics);
        assertPoolsUnavailable(statistics);
        assertEquals(UNAVAILABLE, statistics.getThreadCount());
        assertEquals(UNAVAILABLE, statistics.getUptimeMs());
        assertEquals(UNAVAILABLE, statistics.getProcessCpuTimeNs());
        assertEquals(UNAVAILABLE, statistics.getAvailableProcessors());
    }

    @Test
    public void valuesOfTheTestJvmAreConsistent() {
        final RuntimeStatistics statistics = RuntimeStatistics.read();

        assertTrue(statistics.getHeapUsedBytes() >= 0);
        assertTrue(statistics.getHeapUsedBytes() <= statistics.getHeapCommittedBytes());
        assertTrue(statistics.getHeapCommittedBytes() <= statistics.getHeapMaxBytes());
        assertTrue(statistics.getThreadCount() >= 1);
        assertTrue(statistics.getPeakThreadCount() >= statistics.getThreadCount());
        assertTrue(statistics.getAvailableProcessors() >= 1);
        assertTrue(statistics.getUptimeMs() >= 0);
        assertTrue(statistics.getProcessCpuTimeNs() > 0);
        assertEquals(Runtime.getRuntime().availableProcessors(), statistics.getAvailableProcessors());
        assertTrue(statistics.getDaemonThreadCount() <= statistics.getThreadCount());
        assertTrue(statistics.getTotalStartedThreadCount() >= statistics.getThreadCount());
    }

    private static void assertGcUnavailable(final RuntimeStatistics statistics) {
        assertEquals(UNAVAILABLE, statistics.getYoungGcCount());
        assertEquals(UNAVAILABLE, statistics.getYoungGcTimeMs());
        assertEquals(UNAVAILABLE, statistics.getFullGcCount());
        assertEquals(UNAVAILABLE, statistics.getFullGcTimeMs());
    }

    private static void assertPoolsUnavailable(final RuntimeStatistics statistics) {
        assertEquals(UNAVAILABLE, statistics.getEdenUsedBytes());
        assertEquals(UNAVAILABLE, statistics.getSurvivorUsedBytes());
        assertEquals(UNAVAILABLE, statistics.getOldUsedBytes());
    }

    private static void assertOnlyCpuUnavailable(final RuntimeStatistics statistics) {
        assertEquals(UNAVAILABLE, statistics.getProcessCpuTimeNs());
        assertEquals(100, statistics.getHeapUsedBytes());
        assertEquals(5, statistics.getYoungGcCount());
        assertEquals(77, statistics.getUptimeMs());
        assertEquals(8, statistics.getAvailableProcessors());
    }

    private static GarbageCollectorMXBean collector(final String name, final long count, final long time) {
        return bean(GarbageCollectorMXBean.class,
                Map.of("getName", name, "getCollectionCount", count, "getCollectionTime", time));
    }

    private static MemoryPoolMXBean pool(final String name, final long used) {
        return bean(MemoryPoolMXBean.class,
                Map.of("getName", name, "getUsage", new MemoryUsage(-1, used, used, -1)));
    }

    private static ProcessHandle.Info info(final Optional<Duration> totalCpuDuration) {
        return new ProcessHandle.Info() {
            @Override
            public Optional<String> command() {
                return Optional.empty();
            }

            @Override
            public Optional<String> commandLine() {
                return Optional.empty();
            }

            @Override
            public Optional<String[]> arguments() {
                return Optional.empty();
            }

            @Override
            public Optional<Instant> startInstant() {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> totalCpuDuration() {
                return totalCpuDuration;
            }

            @Override
            public Optional<String> user() {
                return Optional.empty();
            }
        };
    }

    /**
     * A bean whose methods return the given values or throw the given exceptions; other methods return null (the
     * collector treats it as a missing value) or throw for primitive results.
     */
    private static <T> T bean(final Class<T> type, final Map<String, Object> results) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            final Object result = results.get(method.getName());

            if (result instanceof RuntimeException exception) {
                throw exception;
            }

            if (result == null && method.getReturnType().isPrimitive()) {
                throw new UnsupportedOperationException(method.getName());
            }

            return result;
        }));
    }

    private static final class FakeSources implements RuntimeStatistics.Sources {

        MemoryMXBean memory;
        List<GarbageCollectorMXBean> collectors;
        List<MemoryPoolMXBean> pools;
        ThreadMXBean threads;
        RuntimeMXBean runtime;
        ProcessHandle.Info processInfo;
        Throwable processInfoFailure;
        long availableProcessors = -1;
        boolean failAll;

        @Override
        public MemoryMXBean memory() {
            failIfAsked();
            return memory;
        }

        @Override
        public List<GarbageCollectorMXBean> collectors() {
            failIfAsked();
            return collectors;
        }

        @Override
        public List<MemoryPoolMXBean> pools() {
            failIfAsked();
            return pools;
        }

        @Override
        public ThreadMXBean threads() {
            failIfAsked();
            return threads;
        }

        @Override
        public RuntimeMXBean runtime() {
            failIfAsked();
            return runtime;
        }

        @Override
        public ProcessHandle.Info processInfo() {
            failIfAsked();

            if (processInfoFailure instanceof RuntimeException exception) {
                throw exception;
            }

            if (processInfoFailure instanceof Error error) {
                throw error;
            }

            return processInfo;
        }

        @Override
        public long availableProcessors() {
            failIfAsked();
            return availableProcessors;
        }

        private void failIfAsked() {
            if (failAll) {
                throw new UnsupportedOperationException("no management in this test");
            }
        }
    }
}
