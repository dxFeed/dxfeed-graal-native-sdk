// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;
import org.HdrHistogram.Histogram;

/**
 * The summary file of a run, schema {@code nettest-summary/1} (NetTest specification, section "Summary file"):
 * collected from the intervals of the run and written after the stop to {@code <file>.tmp}, then renamed to
 * {@code <file>}. All keys are always present; an absent value is {@code null}.
 */
final class NetTestApiSummary {
    static final String SCHEMA = "nettest-summary/1";
    static final String IMPLEMENTATION = "java-api";
    static final String TOOL_NAME = "NetTestApi";

    private static final long BYTES_PER_MB = 1024 * 1024;
    private static final double[] PERCENTILES = {50, 90, 99, 99.9, 99.99};

    private final NetTestApi.Settings settings;
    private final NetTestApiSide side;
    private final List<NetTestApiStatistics.Interval> intervals = new ArrayList<>(); // guarded by this
    private final Histogram measuredLags = NetTestApiStatistics.newLagHistogram(); // guarded by this
    private long startEpochNanos;
    private long endEpochNanos;
    private long startNanos;
    private long endNanos;
    private NetTestApiRuntime.Sample startSample;
    private NetTestApiRuntime.Sample endSample;

    NetTestApiSummary(NetTestApi.Settings settings, NetTestApiSide side) {
        this.settings = settings;
        this.side = side;
    }

    /** Marks the start of the run. */
    synchronized void started() {
        startEpochNanos = NetTestApiClock.epochNanos();
        startNanos = System.nanoTime();
        startSample = NetTestApiRuntime.read();
    }

    /** Marks the end of the run. */
    synchronized void ended() {
        endEpochNanos = NetTestApiClock.epochNanos();
        endNanos = System.nanoTime();
        endSample = NetTestApiRuntime.read();
    }

    /** Takes an interval of the run. */
    synchronized void add(NetTestApiStatistics.Interval interval) {
        intervals.add(interval);
        if (!interval.warmup && interval.histogram != null) {
            measuredLags.add(interval.histogram);
        }
        // The interval keeps its percentiles; its histogram would hold a few hundred KB for every period of the run.
        interval.histogram = null;
    }

    /** The intervals so far. */
    synchronized List<NetTestApiStatistics.Interval> intervals() {
        return new ArrayList<>(intervals);
    }

    /** Writes the summary to {@code <path>.tmp} and renames it to {@code path}. */
    void write(Path path) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, toJson(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The summary as JSON. */
    synchronized String toJson() {
        boolean consumer = !settings.isProducer();
        long records = 0;
        long lengthNanos = 0;
        for (NetTestApiStatistics.Interval interval : intervals) {
            records += interval.records;
            lengthNanos += interval.lengthNanos;
        }
        Map<String, Object> config = settings.describe();
        config.remove("side");
        config.remove("name");
        String failure = side.getFailure();
        Properties versions = versions();

        Json json = new Json();
        json.begin();
        json.field("schema", SCHEMA);
        json.field("spec_version", NetTestApi.SPEC_VERSION);
        json.key("tool").begin();
        json.field("implementation", IMPLEMENTATION);
        json.field("name", TOOL_NAME);
        json.field("version", versions.getProperty("version"));
        json.field("sdk_version", versions.getProperty("version"));
        json.field("qd_version", versions.getProperty("qd.version"));
        json.key("build").begin();
        json.field("vendor", vendor());
        json.field("march", null);
        json.field("gc", garbageCollectors());
        json.end();
        json.end();
        json.field("side", consumer ? "consumer" : "producer");
        json.field("name", settings.config.name);
        json.field("args", settings.args);
        json.key("config").begin();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            json.field(entry.getKey(), entry.getValue());
        }
        json.end();
        json.field("start_epoch_ns", startEpochNanos);
        json.field("end_epoch_ns", endEpochNanos);
        json.field("status", side.status());
        json.field("exit_code", failure == null ? 0 : 1);
        json.field("error", failure);
        json.field("records", records);
        json.field("rate", NetTestApiStatistics.rate(records, lengthNanos));
        json.field("time_source", "actual");
        json.field("time_resolution", NetTestApiSide.isNanoTime() ? "ns" : "ms");
        json.key("intervals").beginArray();
        for (NetTestApiStatistics.Interval interval : intervals) {
            writeInterval(json, interval, consumer);
        }
        json.endArray();
        json.key("e2e_lag");
        if (consumer) {
            writeLags(json);
        } else {
            json.value(null);
        }
        json.key("publish_call");
        if (consumer) {
            json.value(null);
        } else {
            writePublishCall(json);
        }
        json.key("schedule_slip");
        NetTestApiSchedule schedule = side.schedule();
        if (consumer || schedule == null || !schedule.isLimited()) {
            json.value(null);
        } else {
            writeSlip(json, schedule);
        }
        json.field("symbols_seen", side.symbolsSeen());
        writeRuntime(json);
        json.key("connectors").beginArray();
        for (NetTestApiSide.ConnectorStatistics connector : side.connectorStatistics()) {
            json.begin();
            json.field("address", connector.address);
            json.key("v1").begin();
            json.field("read_bytes", connector.readBytes);
            json.field("written_bytes", connector.writtenBytes);
            json.field("read_data_records", connector.readDataRecords);
            json.field("written_data_records", connector.writtenDataRecords);
            json.field("active_connections", connector.activeConnections);
            json.field("closed_connections", connector.closedConnections);
            json.end();
            json.key("v2").begin();
            json.field("read_sub_records", connector.readSubRecords);
            json.field("written_sub_records", connector.writtenSubRecords);
            json.field("data_read_lag_us", connector.readDataLagMicros);
            json.field("data_write_lag_us", connector.writtenDataLagMicros);
            json.field("read_rtt_us", connector.readRttMicros);
            json.field("write_rtt_us", connector.writtenRttMicros);
            json.end();
            json.end();
        }
        json.endArray();
        json.end();
        return json.toString();
    }

    private static void writeInterval(Json json, NetTestApiStatistics.Interval interval, boolean consumer) {
        boolean lags = consumer && interval.records > 0;
        json.begin();
        json.field("start_epoch_ms", interval.startEpochMs);
        json.field("end_epoch_ms", interval.endEpochMs);
        json.field("phase", interval.warmup ? "warmup" : "measure");
        json.field("records", interval.records);
        json.field("rate", interval.rate());
        json.field("lag_mean_ns", lags ? interval.nanos / interval.records : null);
        json.field("lag_p50_ns", lags ? interval.lagP50 : null);
        json.field("lag_p99_ns", lags ? interval.lagP99 : null);
        json.field("lag_max_ns", lags && interval.maxLag != Long.MIN_VALUE ? interval.maxLag : null);
        json.field("publish_ns_per_event", !consumer && interval.records > 0 ?
            interval.callNanos / interval.records : null);
        json.field("slip_max_ns", consumer ? null : interval.slipMax);
        json.field("slip_end_ns", consumer ? null : interval.slipEnd);
        json.field("connections", interval.connections);
        json.end();
    }

    private void writeLags(Json json) {
        long count = 0;
        long negative = 0;
        long sum = 0;
        long max = Long.MIN_VALUE;
        for (NetTestApiStatistics.Interval interval : intervals) {
            if (interval.warmup) {
                continue;
            }
            count += interval.records;
            negative += interval.negative;
            sum += interval.nanos;
            max = Math.max(max, interval.maxLag);
        }
        boolean any = count > 0;
        json.begin();
        json.field("count", count);
        json.field("negative", negative);
        String[] keys = {"p50_ns", "p90_ns", "p99_ns", "p999_ns", "p9999_ns"};
        for (int i = 0; i < keys.length; i++) {
            json.field(keys[i], any ? measuredLags.getValueAtPercentile(PERCENTILES[i]) : null);
        }
        json.field("max_ns", any && max != Long.MIN_VALUE ? max : null);
        json.field("mean_ns", any ? sum / count : null);
        json.key("histogram").begin();
        json.field("lowest", NetTestApiStatistics.HISTOGRAM_LOWEST);
        json.field("highest", NetTestApiStatistics.HISTOGRAM_HIGHEST);
        json.field("digits", NetTestApiStatistics.HISTOGRAM_DIGITS);
        json.end();
        json.end();
    }

    private void writePublishCall(Json json) {
        long calls = 0;
        long events = 0;
        long nanos = 0;
        for (NetTestApiStatistics.Interval interval : intervals) {
            if (!interval.warmup) {
                calls += interval.calls;
                events += interval.records;
                nanos += interval.callNanos;
            }
        }
        json.begin();
        json.field("calls", calls);
        json.field("events", events);
        json.field("ns_total", nanos);
        json.field("ns_per_event_mean", events > 0 ? nanos / events : null);
        json.end();
    }

    private void writeSlip(Json json, NetTestApiSchedule schedule) {
        long measuredMax = 0;
        long allMax = 0;
        NetTestApiStatistics.Interval first = null;
        NetTestApiStatistics.Interval last = null;
        for (NetTestApiStatistics.Interval interval : intervals) {
            if (interval.slipMax == null) {
                continue;
            }
            allMax = Math.max(allMax, interval.slipMax);
            if (interval.warmup) {
                continue;
            }
            measuredMax = Math.max(measuredMax, interval.slipMax);
            if (first == null) {
                first = interval;
            }
            last = interval;
        }
        // The intervals cover the whole run, so the slip of the last batch is in the last one already.
        json.begin();
        json.field("max_ns", first != null ? measuredMax : allMax);
        json.field("end_ns", schedule.lastSlip());
        json.field("growth_ns_per_s", growth(first, last));
        json.end();
    }

    /**
     * The growth of the slip in nanoseconds per second: the slip at the end of the last measured interval minus the
     * one of the first, over the seconds between their ends; null with fewer than two intervals.
     */
    static Long growth(NetTestApiStatistics.Interval first, NetTestApiStatistics.Interval last) {
        if (first == null || last == first || last.endEpochMs <= first.endEpochMs) {
            return null;
        }
        return (last.slipEnd - first.slipEnd) * 1000 / (last.endEpochMs - first.endEpochMs);
    }

    private void writeRuntime(Json json) {
        NetTestApiRuntime.Sample start = startSample;
        NetTestApiRuntime.Sample end = endSample != null ? endSample : NetTestApiRuntime.read();
        long elapsed = (endSample != null ? endNanos : System.nanoTime()) - startNanos;
        json.key("runtime").begin();
        json.field("heap_used_mb", present(end.heapUsedBytes) ? end.heapUsedBytes / BYTES_PER_MB : null);
        json.field("heap_committed_mb", present(end.heapCommittedBytes) ?
            end.heapCommittedBytes / BYTES_PER_MB : null);
        json.field("gc_count", growth(start == null ? -1 : start.gcCount, end.gcCount));
        json.field("gc_time_ms", growth(start == null ? -1 : start.gcTimeMillis, end.gcTimeMillis));
        Long cpu = growth(start == null ? -1 : start.processCpuNanos, end.processCpuNanos);
        json.field("cpu_percent_mean", cpu != null && elapsed > 0 ? cpu * 100 / elapsed : null);
        json.end();
    }

    private static boolean present(long value) {
        return value >= 0;
    }

    private static Long growth(long start, long end) {
        return present(start) && present(end) && end >= start ? end - start : null;
    }

    /** The vendor version of the runtime (e.g. {@code Temurin-21.0.5+11}), else its vendor; null when neither is set. */
    private static String vendor() {
        String vendorVersion = System.getProperty("java.vendor.version");
        if (vendorVersion != null && !vendorVersion.isEmpty()) {
            return vendorVersion;
        }
        String vendor = System.getProperty("java.vm.vendor");
        return vendor == null || vendor.isEmpty() ? null : vendor;
    }

    private static String garbageCollectors() {
        StringJoiner names = new StringJoiner(", ");
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            names.add(collector.getName());
        }
        return names.length() == 0 ? null : names.toString();
    }

    /** The versions of the tool from its build: {@code version} and {@code qd.version}; empty when absent. */
    static Properties versions() {
        Properties properties = new Properties();
        try (InputStream in = NetTestApiSummary.class.getResourceAsStream("nettest-version.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            // absent: the values are null
        }
        return properties;
    }

    /** A minimal JSON writer: objects, arrays, strings, numbers, booleans and null, with a comma between values. */
    static final class Json {
        private final StringBuilder text = new StringBuilder();
        private boolean comma; // whether the next value needs a comma before it

        Json begin() {
            separate();
            text.append('{');
            comma = false;
            return this;
        }

        Json end() {
            text.append('}');
            comma = true;
            return this;
        }

        Json beginArray() {
            separate();
            text.append('[');
            comma = false;
            return this;
        }

        Json endArray() {
            text.append(']');
            comma = true;
            return this;
        }

        Json key(String key) {
            separate();
            string(key);
            text.append(':');
            comma = false;
            return this;
        }

        Json field(String key, Object value) {
            key(key);
            return value(value);
        }

        Json value(Object value) {
            separate();
            if (value == null) {
                text.append("null");
            } else if (value instanceof String) {
                string((String) value);
            } else if (value instanceof Boolean || value instanceof Long || value instanceof Integer) {
                text.append(value);
            } else if (value instanceof Number) {
                text.append(((Number) value).longValue());
            } else if (value instanceof Collection) {
                text.append('[');
                comma = false;
                for (Object item : (Collection<?>) value) {
                    value(item);
                }
                text.append(']');
            } else {
                string(value.toString());
            }
            comma = true;
            return this;
        }

        private void separate() {
            if (comma) {
                text.append(',');
            }
        }

        private void string(String value) {
            text.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"':
                        text.append("\\\"");
                        break;
                    case '\\':
                        text.append("\\\\");
                        break;
                    case '\n':
                        text.append("\\n");
                        break;
                    case '\r':
                        text.append("\\r");
                        break;
                    case '\t':
                        text.append("\\t");
                        break;
                    default:
                        if (c < 0x20) {
                            text.append(String.format("\\u%04x", (int) c));
                        } else {
                            text.append(c);
                        }
                }
            }
            text.append('"');
        }

        @Override
        public String toString() {
            return text.toString();
        }
    }
}
