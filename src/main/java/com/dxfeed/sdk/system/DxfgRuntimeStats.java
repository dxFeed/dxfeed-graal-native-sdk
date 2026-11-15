// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

/**
 * {@code dxfg_v4_runtime_stats_t}: the runtime statistics of the isolate, filled in the caller's structure.
 */
@CContext(SystemV4Directives.class)
@CStruct("dxfg_v4_runtime_stats_t")
public interface DxfgRuntimeStats extends PointerBase {

    @CField("heap_used_bytes")
    void setHeapUsedBytes(long value);

    @CField("heap_committed_bytes")
    void setHeapCommittedBytes(long value);

    @CField("heap_max_bytes")
    void setHeapMaxBytes(long value);

    @CField("young_gc_count")
    void setYoungGcCount(long value);

    @CField("young_gc_time_ms")
    void setYoungGcTimeMs(long value);

    @CField("full_gc_count")
    void setFullGcCount(long value);

    @CField("full_gc_time_ms")
    void setFullGcTimeMs(long value);

    @CField("thread_count")
    void setThreadCount(long value);

    @CField("peak_thread_count")
    void setPeakThreadCount(long value);

    @CField("process_cpu_time_ns")
    void setProcessCpuTimeNs(long value);

    @CField("uptime_ms")
    void setUptimeMs(long value);

    @CField("allocated_bytes")
    void setAllocatedBytes(long value);

    @CField("eden_used_bytes")
    void setEdenUsedBytes(long value);

    @CField("survivor_used_bytes")
    void setSurvivorUsedBytes(long value);

    @CField("old_used_bytes")
    void setOldUsedBytes(long value);

    @CField("daemon_thread_count")
    void setDaemonThreadCount(long value);

    @CField("total_started_thread_count")
    void setTotalStartedThreadCount(long value);

    @CField("available_processors")
    void setAvailableProcessors(long value);
}
