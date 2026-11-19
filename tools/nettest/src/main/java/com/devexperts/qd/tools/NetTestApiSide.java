// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.logging.Logging;
import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorState;
import com.devexperts.qd.qtp.QDEndpoint;
import com.devexperts.qd.qtp.socket.ServerSocketConnector;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.transport.stats.EndpointStats;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.impl.DXEndpointImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.HdrHistogram.Recorder;

/**
 * What both sides of {@link NetTestApi} share: the symbols, one {@link DXEndpoint} per instance with
 * {@link #NANO_TIME_PROPERTY} on its builder, the statistics intervals on the wall clock, the summary file, the
 * metrics, the first failure and the stop.
 */
abstract class NetTestApiSide {
    private static final Logging log = Logging.getLogging(NetTestApiSide.class);

    /** The time a listening connector has to bind its port, as in the C-API NetTest. */
    static final long START_TIMEOUT_MILLIS = 3_000;

    /** The property of the nanosecond part of the event time, which the tool sets on the builder of every endpoint. */
    static final String NANO_TIME_PROPERTY = "dxscheme.nanoTime";

    static final String STATUS_OK = "ok";
    static final String STATUS_STOPPED_BY_SIGNAL = "stopped_by_signal";
    /** The status of a failure at run time other than the checks of the consumer. */
    static final String STATUS_ERROR = "error";

    final NetTestApi.Settings settings;
    final NetTestConfig config;
    final NetTestApi.Stop stop;
    final NetTestApiStatistics.Counter[] counters;
    final List<MessageConnector> connectors = new CopyOnWriteArrayList<>();
    private final List<List<MessageConnector>> instanceConnectors = new ArrayList<>();
    private final String[] symbols;
    private final String symbolsFailure; // why the symbols could not be read, or null
    private final List<DXEndpoint> endpoints = new ArrayList<>();
    private final NetTestApiSummary summary;
    private volatile List<ConnectorStatistics> connectorStatistics = Collections.emptyList();
    private NetTestApiMetrics metrics; // with --metrics-port, once bound
    private NetTestApiRuntime.Reporter reporter; // set once the endpoints are up
    private Thread ticker;
    private String failureStatus; // the status of the first failure, guarded by this
    private String failure; // the text of the first failure, guarded by this
    private boolean closed; // guarded by this
    private boolean finished; // guarded by this

    NetTestApiSide(NetTestApi.Settings settings, NetTestApi.Stop stop) {
        this.settings = settings;
        this.config = settings.config;
        this.stop = stop;
        String[] loaded;
        String loadFailure = null;
        try {
            loaded = config.ipfPath == null ? NetTestApiSymbols.generated(config.totalSymbols) :
                NetTestApiSymbols.fromFile(config.ipfPath, config.totalSymbols);
        } catch (IOException | RuntimeException e) {
            loaded = new String[0];
            loadFailure = "cannot read the symbols of '" + config.ipfPath + "': " + e.getMessage();
        }
        this.symbols = loaded;
        this.symbolsFailure = loadFailure;
        counters = new NetTestApiStatistics.Counter[config.instanceCount];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = new NetTestApiStatistics.Counter();
            instanceConnectors.add(new CopyOnWriteArrayList<>());
        }
        summary = new NetTestApiSummary(settings, this);
    }

    /** The summary of the run. */
    NetTestApiSummary summary() {
        return summary;
    }

    /** The symbols of all instances, in the order of the specification. */
    String[] getSymbols() {
        return symbols;
    }

    /** The label of the statistics line of the side. */
    abstract String statisticsLabel();

    /** Creates, connects and checks the endpoints and starts the work. Throws when an endpoint fails. */
    abstract void start();

    /** The epoch nanoseconds where the warm-up starts, 0 before it starts. */
    abstract long warmupStartEpochNanos();

    /** The recorder of the e2e lags on the consumer, null on the producer. */
    Recorder lagRecorder() {
        return null;
    }

    /** The schedule of the producer, null on the consumer. */
    NetTestApiSchedule schedule() {
        return null;
    }

    /**
     * The e2e lags of the consumer per bucket of {@link NetTestApiMetrics#LAG_BUCKET_NANOS}, the last one for
     * {@code +Inf}; null on the producer.
     */
    long[] lagBuckets() {
        return null;
    }

    /**
     * Runs the test until {@code --duration} ends, a stop is requested or a failure.
     *
     * @return {@code null} on success, or the reason of the failure.
     */
    String run() {
        summary.started();
        try {
            if (symbolsFailure != null) {
                throw new IllegalStateException(symbolsFailure);
            }
            bindMetrics();
            start();
        } catch (RuntimeException e) {
            String text = e.getMessage() != null ? e.getMessage() : e.toString();
            fail(STATUS_ERROR, text, e instanceof IllegalStateException ? null : e);
            return finish();
        }
        try {
            awaitEnd();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return finish();
    }

    /** Binds the port of {@code --metrics-port} and prints the metrics line; a port in use fails the start. */
    void bindMetrics() {
        if (settings.metricsPort < 0) {
            return;
        }
        NetTestApiMetrics bound;
        try {
            bound = NetTestApiMetrics.start(settings.metricsHost, settings.metricsPort, this);
        } catch (IOException e) {
            throw new IllegalStateException("--metrics-port cannot bind " + settings.metricsHost + ":" +
                settings.metricsPort + ": " + e.getMessage(), e);
        }
        synchronized (this) {
            metrics = bound;
        }
        NetTestApiRuntime.print("{*NetTestMetrics-" + config.name + "*} " + bound.url());
    }

    /** Waits for the end of the run: {@code --duration} or a stop. */
    void awaitEnd() throws InterruptedException {
        stop.await(settings.durationMillis);
    }

    /**
     * Stops the work of the side, prints the last statistics block, closes the endpoints and writes the summary file;
     * the first call does it, the next ones return the failure.
     *
     * @return {@code null} without a failure, or the reason of the first failure.
     */
    String finish() {
        stop.request();
        synchronized (this) {
            if (finished) {
                return failure;
            }
            finished = true;
        }
        stopWork();
        Thread tickerThread;
        NetTestApiRuntime.Reporter lastReporter;
        synchronized (this) {
            tickerThread = ticker;
            lastReporter = reporter;
        }
        if (tickerThread != null) {
            tickerThread.interrupt();
            try {
                tickerThread.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (lastReporter != null) {
            lastReporter.finish();
        }
        readConnectorStatistics();
        close();
        summary.ended();
        if (settings.summaryPath != null) {
            try {
                summary.write(settings.summaryPath);
            } catch (IOException | RuntimeException e) {
                fail(STATUS_ERROR, "cannot write the summary file '" + settings.summaryPath + "': " + e, null);
            }
        }
        NetTestApiMetrics server;
        synchronized (this) {
            server = metrics;
        }
        if (server != null) {
            server.stop();
        }
        return getFailure();
    }

    /** Stops the threads of the side before the last block; nothing by default. */
    void stopWork() {}

    /** The text of the first failure, or {@code null}. */
    synchronized String getFailure() {
        return failure;
    }

    /** The status of the first failure ({@code error}, {@code no_data}, {@code nano_missing}), or {@code null}. */
    synchronized String getFailureStatus() {
        return failureStatus;
    }

    /** The status of the run for the summary: the failure, the signal or {@code ok}. */
    synchronized String status() {
        if (failureStatus != null) {
            return failureStatus;
        }
        return stop.isBySignal() ? STATUS_STOPPED_BY_SIGNAL : STATUS_OK;
    }

    /** Records the first failure and requests the stop. */
    void fail(String status, String text, Throwable cause) {
        synchronized (this) {
            if (failure != null) {
                return;
            }
            failureStatus = status;
            failure = text;
        }
        if (cause != null) {
            log.error(text, cause);
        } else {
            log.error(text);
        }
        stop.request();
    }

    /** A copy of the endpoints created so far. */
    synchronized List<DXEndpoint> getEndpoints() {
        return new ArrayList<>(endpoints);
    }

    /** Creates the endpoint of the instance with {@link #NANO_TIME_PROPERTY} on its builder and keeps it for the close. */
    final DXEndpoint createEndpoint(int index) {
        DXEndpoint endpoint = endpointBuilder(index).build();
        synchronized (this) {
            if (closed) {
                endpoint.close();
                throw new IllegalStateException("stopped while starting");
            }
            endpoints.add(endpoint);
        }
        return endpoint;
    }

    /** The builder of the endpoint of the instance; a test may change its properties. */
    DXEndpoint.Builder endpointBuilder(int index) {
        return DXEndpoint.newBuilder()
            .withRole(settings.role)
            .withName(config.name + "." + index)
            .withProperty(NANO_TIME_PROPERTY, endpointNanoTime(System.getProperty(NANO_TIME_PROPERTY)));
    }

    /** Connects the endpoint of the instance to the address and waits until a listening connector has its port. */
    void connect(int index, DXEndpoint endpoint) {
        endpoint.connect(config.addresses[index - 1]);
        QDEndpoint qdEndpoint = qdEndpoint(endpoint);
        awaitServerSockets(config.name + "." + index, qdEndpoint);
        connectors.addAll(qdEndpoint.getConnectors());
        instanceConnectors.get(index - 1).addAll(qdEndpoint.getConnectors());
    }

    /**
     * Starts the statistics: a thread that ends an interval at each multiple of the period on the wall clock. The
     * first tick only starts the count.
     */
    void startStatistics() {
        NetTestApiStatistics statistics = new NetTestApiStatistics(statisticsLabel(), config.name, counters,
            this::connectionCount, lagRecorder(), schedule(), TimeUnit.MILLISECONDS.toNanos(settings.warmupMillis),
            this::warmupStartEpochNanos);
        NetTestApiRuntime.Reporter created = new NetTestApiRuntime.Reporter(minNanos -> {
            NetTestApiStatistics.Interval interval = statistics.tick(minNanos);
            readConnectorStatistics();
            if (interval == null) {
                return null;
            }
            summary.add(interval);
            return interval.text;
        }, config.name);
        long period = settings.statPeriod.getTime();
        Thread thread = new Thread(() -> tick(created, period), "NetTestApi-statistics-" + config.name);
        thread.setDaemon(true);
        synchronized (this) {
            reporter = created;
            ticker = thread;
        }
        thread.start();
    }

    // Runs the reporter at each multiple of the period on the wall clock until the stop.
    private void tick(NetTestApiRuntime.Reporter target, long periodMillis) {
        while (!stop.isRequested() && !Thread.currentThread().isInterrupted()) {
            long now = System.currentTimeMillis();
            long next = (now / periodMillis + 1) * periodMillis;
            for (long left = next - System.currentTimeMillis(); left > 0; left = next - System.currentTimeMillis()) {
                LockSupport.parkNanos(this, TimeUnit.MILLISECONDS.toNanos(left));
                if (stop.isRequested() || Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
            try {
                target.run();
            } catch (RuntimeException e) {
                log.error("Failed to report the statistics", e);
            }
        }
    }

    /** The open connections of the connectors of all instances. */
    int connectionCount() {
        int count = 0;
        for (MessageConnector connector : connectors) {
            count += connector.getConnectionCount();
        }
        return count;
    }

    /** The open connections of the connectors of the instance {@code index} (from 1). */
    int connectionCount(int index) {
        int count = 0;
        for (MessageConnector connector : instanceConnectors.get(index - 1)) {
            count += connector.getConnectionCount();
        }
        return count;
    }

    /** The statistics v1 of the connectors, read at the last tick or at the end of the run. */
    List<ConnectorStatistics> connectorStatistics() {
        return connectorStatistics;
    }

    // Reads the connector statistics at most once per interval and at the end.
    private void readConnectorStatistics() {
        List<ConnectorStatistics> read = new ArrayList<>();
        for (MessageConnector connector : connectors) {
            read.add(ConnectorStatistics.of(connector));
        }
        connectorStatistics = read;
    }

    /** The symbols received so far, or {@code null} on the producer. */
    Long symbolsSeen() {
        return null;
    }

    /**
     * Whether the endpoints carry the nanosecond part: the value of {@link #NANO_TIME_PROPERTY} on their builders.
     */
    static boolean isNanoTime() {
        return Boolean.parseBoolean(endpointNanoTime(System.getProperty(NANO_TIME_PROPERTY)));
    }

    /**
     * The value of {@link #NANO_TIME_PROPERTY} for the builders: the given {@code -Ddxscheme.nanoTime}, or
     * {@code "true"} without one. The system property alone does not reach the endpoints of the stream roles, so the
     * builder carries it.
     */
    static String endpointNanoTime(String given) {
        return given != null ? given : "true";
    }

    static QDEndpoint qdEndpoint(DXEndpoint endpoint) {
        return ((DXEndpointImpl) endpoint).getQDEndpoint();
    }

    // A listening connector that cannot bind its port retries forever and the statistics would print
    // "connections 0" forever, so the start fails instead.
    private static void awaitServerSockets(String name, QDEndpoint qdEndpoint) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(START_TIMEOUT_MILLIS);
        for (MessageConnector connector : qdEndpoint.getConnectors()) {
            if (!(connector instanceof ServerSocketConnector)) {
                continue;
            }
            while (connector.getState() != MessageConnectorState.CONNECTED) {
                if (System.nanoTime() - deadline > 0) {
                    throw new IllegalStateException(name + ": the server socket " + connector.getAddress() +
                        " did not start in " + START_TIMEOUT_MILLIS + " ms (is the port already in use?)");
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(name + ": interrupted while starting", e);
                }
            }
        }
    }

    /** Closes all endpoints; called by the main thread and by the shutdown hook, at most once. */
    void close() {
        List<DXEndpoint> toClose;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            toClose = new ArrayList<>(endpoints);
        }
        stop.request();
        for (DXEndpoint endpoint : toClose) {
            try {
                endpoint.close();
            } catch (RuntimeException e) {
                log.error("Failed to close " + qdEndpoint(endpoint).getName(), e);
            }
        }
        log.info("Closed " + toClose.size() + " endpoint(s)");
    }

    /** The statistics v1 of one connector (NetTest specification, section "Connector statistics"). */
    static final class ConnectorStatistics {
        final String address;
        final long readBytes;
        final long writtenBytes;
        final long readDataRecords;
        final long writtenDataRecords;
        final long activeConnections;
        final long closedConnections;

        ConnectorStatistics(String address, long readBytes, long writtenBytes, long readDataRecords,
            long writtenDataRecords, long activeConnections, long closedConnections)
        {
            this.address = address;
            this.readBytes = readBytes;
            this.writtenBytes = writtenBytes;
            this.readDataRecords = readDataRecords;
            this.writtenDataRecords = writtenDataRecords;
            this.activeConnections = activeConnections;
            this.closedConnections = closedConnections;
        }

        static ConnectorStatistics of(MessageConnector connector) {
            EndpointStats endpointStats = connector.retrieveCompleteEndpointStats();
            QDStats stats = connector.getStats();
            return new ConnectorStatistics(connector.getAddress(), endpointStats.getReadBytes(),
                endpointStats.getWrittenBytes(), value(stats, QDStats.SValue.IO_DATA_READ_RECORDS),
                value(stats, QDStats.SValue.IO_DATA_WRITE_RECORDS), endpointStats.getActiveConnectionCount(),
                endpointStats.getClosedConnectionCount());
        }

        private static long value(QDStats stats, QDStats.SValue value) {
            return stats == null ? 0 : Math.max(0, stats.getValue(value));
        }
    }
}
