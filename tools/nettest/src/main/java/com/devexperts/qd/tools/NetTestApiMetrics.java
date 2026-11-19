// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@code /metrics} endpoint of {@code --metrics-port} (NetTest specification, section "Metrics"): the text
 * exposition format of Prometheus from the counters that the tool keeps, served by the JDK's HTTP server on one
 * thread.
 */
final class NetTestApiMetrics {
    static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    static final String PATH = "/metrics";

    /** The upper bounds of the e2e lag buckets in nanoseconds, without {@code +Inf}. */
    static final long[] LAG_BUCKET_NANOS = {50_000, 100_000, 250_000, 500_000, 1_000_000, 2_500_000, 5_000_000,
        10_000_000, 25_000_000, 50_000_000, 100_000_000, 250_000_000, 1_000_000_000};
    private static final String[] LAG_BUCKET_LABELS = {"5e-05", "0.0001", "0.00025", "0.0005", "0.001", "0.0025",
        "0.005", "0.01", "0.025", "0.05", "0.1", "0.25", "1"};

    private static final double NANOS_PER_SECOND = 1e9;
    private static final double MILLIS_PER_SECOND = 1e3;

    private final HttpServer server;
    private final ExecutorService executor;

    private NetTestApiMetrics(HttpServer server, ExecutorService executor) {
        this.server = server;
        this.executor = executor;
    }

    /**
     * Binds {@code host:port} (port 0 takes a free one) and serves the metrics of the side.
     *
     * @throws IOException when the port cannot be bound, for example when it is in use.
     */
    static NetTestApiMetrics start(String host, int port, NetTestApiSide side) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "NetTestApi-metrics-" + side.config.name);
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext(PATH, exchange -> serve(exchange, side));
        server.start();
        return new NetTestApiMetrics(server, executor);
    }

    /** The URL of the metrics with the bound port. */
    String url() {
        InetSocketAddress address = server.getAddress();
        String host = address.getHostString();
        if (host.indexOf(':') >= 0) {
            host = "[" + host + "]";
        }
        return "http://" + host + ":" + address.getPort() + PATH;
    }

    /** Stops serving and frees the port. */
    void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    /** The bucket of a lag in {@link #LAG_BUCKET_NANOS}: its index, or the length for {@code +Inf}. */
    static int lagBucket(long lagNanos) {
        long value = NetTestApiStatistics.histogramValue(lagNanos);
        for (int i = 0; i < LAG_BUCKET_NANOS.length; i++) {
            if (value <= LAG_BUCKET_NANOS[i]) {
                return i;
            }
        }
        return LAG_BUCKET_NANOS.length;
    }

    private static void serve(HttpExchange exchange, NetTestApiSide side) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            byte[] body = scrape(side).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        } finally {
            exchange.close();
        }
    }

    /** The metrics of the side in the text exposition format. */
    static String scrape(NetTestApiSide side) {
        NetTestApi.Settings settings = side.settings;
        boolean consumer = !settings.isProducer();
        String labels = "tool=\"" + NetTestApiSummary.IMPLEMENTATION + "\",side=\"" +
            (consumer ? "consumer" : "producer") + "\",name=\"" + escape(settings.config.name) + "\"";
        long[][] values = new long[side.counters.length][NetTestApiStatistics.Counter.SIZE];
        long[] totals = new long[NetTestApiStatistics.Counter.SIZE];
        for (int i = 0; i < side.counters.length; i++) {
            side.counters[i].read(values[i]);
            for (int k = 0; k < totals.length; k++) {
                totals[k] += values[i][k];
            }
        }
        Properties versions = NetTestApiSummary.versions();
        StringBuilder text = new StringBuilder();

        family(text, "nettest_info", "gauge", "The configuration of the NetTest run.");
        text.append("nettest_info{").append(labels)
            .append(",spec_version=\"").append(NetTestApi.SPEC_VERSION)
            .append("\",version=\"").append(escape(versions.getProperty("version", "")))
            .append("\",sdk_version=\"").append(escape(versions.getProperty("version", "")))
            .append("\",role=\"").append(settings.role.name())
            .append("\",event=\"").append(settings.event.text)
            .append("\",collector=\"").append(settings.collector == null ? "" : settings.collector)
            .append("\"} 1\n");

        family(text, "nettest_records_total", "counter", consumer ? "The received events." : "The published events.");
        for (int i = 0; i < values.length; i++) {
            text.append("nettest_records_total{").append(labels).append(",connection=\"").append(i + 1)
                .append("\"} ").append(values[i][NetTestApiStatistics.Counter.RECORDS]).append('\n');
        }

        if (consumer) {
            long[] buckets = side.lagBuckets();
            family(text, "nettest_e2e_lag_seconds", "histogram", "The receive time minus the time of the event.");
            long cumulative = 0;
            for (int i = 0; i < LAG_BUCKET_LABELS.length; i++) {
                cumulative += buckets[i];
                text.append("nettest_e2e_lag_seconds_bucket{").append(labels).append(",le=\"")
                    .append(LAG_BUCKET_LABELS[i]).append("\"} ").append(cumulative).append('\n');
            }
            cumulative += buckets[LAG_BUCKET_LABELS.length];
            text.append("nettest_e2e_lag_seconds_bucket{").append(labels).append(",le=\"+Inf\"} ")
                .append(cumulative).append('\n');
            sample(text, "nettest_e2e_lag_seconds_sum", labels,
                seconds(totals[NetTestApiStatistics.Counter.NANOS]));
            sample(text, "nettest_e2e_lag_seconds_count", labels, Long.toString(cumulative));
            family(text, "nettest_e2e_lag_negative_total", "counter", "The events received with a negative lag.");
            sample(text, "nettest_e2e_lag_negative_total", labels,
                Long.toString(totals[NetTestApiStatistics.Counter.NEGATIVE]));
        } else {
            family(text, "nettest_publish_seconds_total", "counter",
                "The time of the batch fills and the publishing calls.");
            sample(text, "nettest_publish_seconds_total", labels,
                seconds(totals[NetTestApiStatistics.Counter.CALL_NANOS]));
            family(text, "nettest_publish_calls_total", "counter", "The publishing calls.");
            sample(text, "nettest_publish_calls_total", labels,
                Long.toString(totals[NetTestApiStatistics.Counter.CALLS]));
            NetTestApiSchedule schedule = side.schedule();
            if (schedule != null && schedule.isLimited()) {
                family(text, "nettest_schedule_slip_seconds", "gauge", "The schedule slip of the last batch.");
                sample(text, "nettest_schedule_slip_seconds", labels, seconds(schedule.lastSlip()));
            }
        }

        family(text, "nettest_connections", "gauge", "The open connections.");
        for (int i = 1; i <= side.counters.length; i++) {
            text.append("nettest_connections{").append(labels).append(",connection=\"").append(i).append("\"} ")
                .append(side.connectionCount(i)).append('\n');
        }

        long readBytes = 0;
        long writtenBytes = 0;
        long readRecords = 0;
        long writtenRecords = 0;
        List<NetTestApiSide.ConnectorStatistics> connectors = side.connectorStatistics();
        for (NetTestApiSide.ConnectorStatistics connector : connectors) {
            readBytes += connector.readBytes;
            writtenBytes += connector.writtenBytes;
            readRecords += connector.readDataRecords;
            writtenRecords += connector.writtenDataRecords;
        }
        family(text, "nettest_connector_read_bytes_total", "counter", "The bytes read by the connectors.");
        sample(text, "nettest_connector_read_bytes_total", labels, Long.toString(readBytes));
        family(text, "nettest_connector_written_bytes_total", "counter", "The bytes written by the connectors.");
        sample(text, "nettest_connector_written_bytes_total", labels, Long.toString(writtenBytes));
        family(text, "nettest_connector_read_records_total", "counter", "The data records read by the connectors.");
        sample(text, "nettest_connector_read_records_total", labels, Long.toString(readRecords));
        family(text, "nettest_connector_written_records_total", "counter",
            "The data records written by the connectors.");
        sample(text, "nettest_connector_written_records_total", labels, Long.toString(writtenRecords));

        NetTestApiRuntime.Sample runtime = NetTestApiRuntime.read();
        if (runtime.heapUsedBytes >= 0) {
            family(text, "nettest_runtime_heap_used_bytes", "gauge", "The used heap.");
            sample(text, "nettest_runtime_heap_used_bytes", labels, Long.toString(runtime.heapUsedBytes));
        }
        if (runtime.heapCommittedBytes >= 0) {
            family(text, "nettest_runtime_heap_committed_bytes", "gauge", "The committed heap.");
            sample(text, "nettest_runtime_heap_committed_bytes", labels, Long.toString(runtime.heapCommittedBytes));
        }
        if (runtime.gcCount >= 0 && runtime.gcTimeMillis >= 0) {
            family(text, "nettest_runtime_gc_total", "counter", "The collections.");
            sample(text, "nettest_runtime_gc_total", labels, Long.toString(runtime.gcCount));
            family(text, "nettest_runtime_gc_seconds_total", "counter", "The time of the collections.");
            sample(text, "nettest_runtime_gc_seconds_total", labels,
                Double.toString(runtime.gcTimeMillis / MILLIS_PER_SECOND));
        }
        if (runtime.processCpuNanos >= 0) {
            family(text, "nettest_runtime_cpu_seconds_total", "counter", "The CPU time of the process.");
            sample(text, "nettest_runtime_cpu_seconds_total", labels, seconds(runtime.processCpuNanos));
        }
        return text.toString();
    }

    private static void family(StringBuilder text, String name, String type, String help) {
        text.append("# HELP ").append(name).append(' ').append(help).append('\n');
        text.append("# TYPE ").append(name).append(' ').append(type).append('\n');
    }

    private static void sample(StringBuilder text, String name, String labels, String value) {
        text.append(name).append('{').append(labels).append("} ").append(value).append('\n');
    }

    private static String seconds(long nanos) {
        return Double.toString(nanos / NANOS_PER_SECOND);
    }

    /** Escapes a label value: the backslash, the double quote and the line feed. */
    static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
