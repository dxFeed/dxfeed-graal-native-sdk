// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.logging.Logging;
import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorState;
import com.devexperts.qd.qtp.QDEndpoint;
import com.devexperts.qd.qtp.socket.ServerSocketConnector;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.impl.DXEndpointImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * What both sides of {@link NetTestApi} share: the symbols of QD NetTest ({@link NetTestSide}), one {@link DXEndpoint}
 * per instance with {@link #NANO_TIME_PROPERTY} on its builder, the statistics of the specification, the first
 * failure and the stop.
 */
abstract class NetTestApiSide extends NetTestSide {
    private static final Logging log = Logging.getLogging(NetTestApiSide.class);

    /** The time a listening connector has to bind its port, as in the C-API NetTest. */
    static final long START_TIMEOUT_MILLIS = 3_000;

    /** The property of the nanosecond part of the event time, which the tool sets on the builder of every endpoint. */
    static final String NANO_TIME_PROPERTY = "dxscheme.nanoTime";

    /** The status of a failure at run time other than the checks of the consumer. */
    static final String STATUS_ERROR = "error";

    final NetTestApi.Settings settings;
    final NetTestApi.Stop stop;
    final NetTestApiStatistics.Counter[] counters;
    private final String[] symbolStrings;
    private final List<DXEndpoint> endpoints = new ArrayList<>();
    private NetTestApiRuntime.Reporter reporter; // set once the endpoints are up
    private String failureStatus; // the status of the first failure, guarded by this
    private String failure; // the text of the first failure, guarded by this
    private boolean closed; // guarded by this

    NetTestApiSide(NetTestApi.Settings settings, NetTestApi.Stop stop) {
        super(settings.config);
        this.settings = settings;
        this.stop = stop;
        // Short symbols are cipher-coded: SymbolList keeps only the cipher of them.
        symbolStrings = new String[symbols.size()];
        for (int i = 0; i < symbolStrings.length; i++) {
            String symbol = symbols.getSymbol(i);
            symbolStrings[i] = symbol != null ? symbol : SCHEME.getCodec().decode(symbols.getCipher(i));
        }
        if (symbolStrings.length == 0) {
            throw new IllegalArgumentException("no symbols");
        }
        counters = new NetTestApiStatistics.Counter[config.instanceCount];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = new NetTestApiStatistics.Counter();
        }
    }

    /** The decoded symbols of all instances. */
    String[] getSymbols() {
        return symbolStrings;
    }

    /** The label of the statistics line of the side. */
    abstract String statisticsLabel();

    /**
     * Runs the test until {@code --duration} ends, a stop is requested or a failure.
     *
     * @return {@code null} on success, or the reason of the failure.
     */
    String run() {
        try {
            start();
        } catch (RuntimeException e) {
            log.error("Failed to start", e);
            close();
            return e.getMessage() != null ? e.getMessage() : e.toString();
        }
        try {
            awaitEnd();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return finish();
    }

    /** Waits for the end of the run: {@code --duration} or a stop. */
    void awaitEnd() throws InterruptedException {
        stop.await(settings.durationMillis);
    }

    /**
     * Stops the work of the side, logs the last statistics block and closes the endpoints.
     *
     * @return {@code null} without a failure, or the reason of the first failure.
     */
    String finish() {
        stop.request();
        stopWork();
        NetTestApiRuntime.Reporter lastReporter;
        synchronized (this) {
            lastReporter = reporter;
        }
        if (lastReporter != null) {
            lastReporter.finish();
        }
        close();
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

    /**
     * Creates the endpoint of the instance with {@link #NANO_TIME_PROPERTY} on its builder and keeps it for the
     * close; the statistics period goes to the first one.
     */
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
        DXEndpoint.Builder builder = DXEndpoint.newBuilder()
            .withRole(settings.role)
            .withName(config.name + "." + index)
            .withProperty(NANO_TIME_PROPERTY, endpointNanoTime(System.getProperty(NANO_TIME_PROPERTY)));
        if (index == 1) {
            builder.withProperty("monitoring.stat", settings.statPeriod.toString());
        }
        return builder;
    }

    /** Connects the endpoint of the instance to the address and waits until a listening connector has its port. */
    void connect(int index, DXEndpoint endpoint) {
        endpoint.connect(config.addresses[index - 1]);
        QDEndpoint qdEndpoint = qdEndpoint(endpoint);
        awaitServerSockets(config.name + "." + index, qdEndpoint);
        connectors.addAll(qdEndpoint.getConnectors());
    }

    /** Starts the statistics of the specification as a monitoring task of the first endpoint. */
    void startStatistics() {
        NetTestApiStatistics statistics = new NetTestApiStatistics(statisticsLabel(), config.name, counters,
            this::connectionCount);
        NetTestApiRuntime.Reporter created = new NetTestApiRuntime.Reporter(statistics::tick, config.name);
        synchronized (this) {
            reporter = created;
        }
        qdEndpoint(getEndpoints().get(0)).registerMonitoringTask(created);
    }

    /** The open connections of the connectors of all instances. */
    int connectionCount() {
        int count = 0;
        for (MessageConnector connector : connectors) {
            count += connector.getConnectionCount();
        }
        return count;
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

    // NetTestSide.start() is replaced, so these factories of QD NetTest are never called.

    @Override
    protected void createDistributor(QDEndpoint endpoint, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void createAgent(QDEndpoint endpoint, int index) {
        throw new UnsupportedOperationException();
    }
}
