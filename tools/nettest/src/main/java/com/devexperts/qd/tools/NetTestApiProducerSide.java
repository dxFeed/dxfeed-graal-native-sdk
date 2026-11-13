// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.logging.Logging;
import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorState;
import com.devexperts.qd.qtp.QDEndpoint;
import com.devexperts.qd.qtp.socket.ServerSocketConnector;
import com.devexperts.qd.util.RateLimiter;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.impl.DXEndpointImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The producer side of {@link NetTestApi}: one {@link DXEndpoint} and one {@link NetTestApiGeneratorThread} per
 * instance. It reuses the symbols and the statistics of QD NetTest, which read {@link #threads} and
 * {@link #connectors}.
 */
class NetTestApiProducerSide extends NetTestSide {
    private static final Logging log = Logging.getLogging(NetTestApiProducerSide.class);

    /** The time a listening connector has to bind its port, as in the C-API NetTest. */
    static final long START_TIMEOUT_MILLIS = 3_000;
    private static final long JOIN_TIMEOUT_MILLIS = 5_000;

    final NetTestApi.Settings settings;
    final NetTestApi.Stop stop;
    final RateLimiter rateLimiter; // shared by all generators, guarded by itself; null without -r
    private final String[] symbolStrings;
    private final List<DXEndpoint> endpoints = new ArrayList<>();
    private final List<NetTestApiGeneratorThread> generators = new ArrayList<>();
    private Throwable failure; // the first failure of a generator, guarded by this
    private boolean closed; // guarded by this

    NetTestApiProducerSide(NetTestApi.Settings settings, NetTestApi.Stop stop) {
        super(settings.config);
        this.settings = settings;
        this.stop = stop;
        this.rateLimiter = settings.config.rateLimiters == null ? null : settings.config.rateLimiters.get("*");
        // Short symbols are cipher-coded: SymbolList keeps only the cipher of them.
        symbolStrings = new String[symbols.size()];
        for (int i = 0; i < symbolStrings.length; i++) {
            String symbol = symbols.getSymbol(i);
            symbolStrings[i] = symbol != null ? symbol : SCHEME.getCodec().decode(symbols.getCipher(i));
        }
        if (symbolStrings.length == 0) {
            throw new IllegalArgumentException("no symbols to publish");
        }
    }

    /** The decoded symbols of all instances. */
    String[] getSymbols() {
        return symbolStrings;
    }

    /**
     * Runs the test until {@code --duration} ends, a stop is requested or a generator fails.
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
            stop.await(settings.durationMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return finish();
    }

    /**
     * Stops the generators and closes the endpoints.
     *
     * @return {@code null} when no generator failed, or the reason of the first failure.
     */
    String finish() {
        stop.request();
        stopGenerators();
        close();
        synchronized (this) {
            return failure == null ? null : "publishing failed: " + failure;
        }
    }

    /** The endpoints created so far, for tests. */
    synchronized List<DXEndpoint> getEndpoints() {
        return new ArrayList<>(endpoints);
    }

    /** Creates, connects and checks the endpoints, then starts the generators. Throws when an endpoint fails. */
    @Override
    public void start() {
        log.info("Publishing " + (settings.event == NetTestApi.EventKind.TRADE ? "Trade" : "TimeAndSale") +
            " events through " + settings.role + " endpoints, batch " + settings.batch +
            (settings.reuse ? ", reused events" : ", new events"));
        for (int i = 1; i <= config.instanceCount; i++) {
            DXEndpoint endpoint = createEndpoint(i);
            synchronized (this) {
                if (closed) {
                    endpoint.close();
                    throw new IllegalStateException("stopped while starting");
                }
                endpoints.add(endpoint);
            }
            endpoint.connect(config.addresses[i - 1]);
            QDEndpoint qdEndpoint = qdEndpoint(endpoint);
            awaitServerSockets(config.name + "." + i, qdEndpoint);
            connectors.addAll(qdEndpoint.getConnectors());
            NetTestApiGeneratorThread generator = createGenerator(i, endpoint);
            generators.add(generator);
            threads.add(generator);
        }
        qdEndpoint(endpoints.get(0)).registerMonitoringTask(statisticsCollector);
        for (NetTestApiGeneratorThread generator : generators) {
            generator.start();
        }
    }

    /** Creates the endpoint of the instance; the statistics period goes to the first one. */
    DXEndpoint createEndpoint(int index) {
        DXEndpoint.Builder builder = DXEndpoint.newBuilder()
            .withRole(settings.role)
            .withName(config.name + "." + index);
        if (index == 1) {
            builder.withProperty("monitoring.stat", settings.statPeriod.toString());
        }
        return builder.build();
    }

    NetTestApiGeneratorThread createGenerator(int index, DXEndpoint endpoint) {
        return new NetTestApiGeneratorThread(index, this, qdEndpoint(endpoint), endpoint.getPublisher());
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

    /** Records the first failure of a generator and requests the stop. */
    void fail(Throwable t) {
        synchronized (this) {
            if (failure != null) {
                return;
            }
            failure = t;
        }
        log.error("Publishing failed", t);
        stop.request();
    }

    private void stopGenerators() {
        for (NetTestApiGeneratorThread generator : generators) {
            generator.interrupt();
        }
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(JOIN_TIMEOUT_MILLIS);
        for (NetTestApiGeneratorThread generator : generators) {
            long left = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            try {
                if (left > 0) {
                    generator.join(left);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (generator.isAlive()) {
                log.warn(generator.getName() + " did not stop in " + JOIN_TIMEOUT_MILLIS + " ms");
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
