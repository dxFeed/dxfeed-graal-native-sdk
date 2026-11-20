// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.impl.DXEndpointImpl;
import com.dxfeed.sdk.connector.ConnectionErrors;
import com.dxfeed.ipf.live.InstrumentProfileConnection;
import com.dxfeed.sdk.javac.InPlaceExecutor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * The endpoints and instrument profile connections created through the SDK in this isolate
 * (including the default endpoints of {@code DXEndpoint.getInstance()}, {@code DXFeed.getInstance()}, etc.).
 *
 * <p>Their threads (e.g. "DXEndpoint-DXExecutorThread-N") do not stop on {@link Thread#interrupt()},
 * so {@code graal_tear_down_isolate} waits for them forever while any of them is open.
 * {@link #closeAllAndAwaitTermination()} closes them all.
 */
public final class IsolateResources {

    // Weak: an object that became unreachable has no threads anymore.
    private static final Set<DXEndpoint> ENDPOINTS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<InstrumentProfileConnection> CONNECTIONS = Collections.newSetFromMap(new WeakHashMap<>());

    private IsolateResources() {
    }

    /**
     * Registers an endpoint created through the SDK; the first registration also starts to capture its connection
     * errors. Registering it again changes nothing.
     */
    public static DXEndpoint register(final DXEndpoint endpoint) {
        if (endpoint != null) {
            final boolean added;

            synchronized (IsolateResources.class) {
                added = ENDPOINTS.add(endpoint);
            }

            // Outside the lock: it adds a QD listener, which QD calls under its own locks.
            if (added) {
                ConnectionErrors.watch(endpoint);
            }
        }
        return endpoint;
    }

    public static synchronized InstrumentProfileConnection register(final InstrumentProfileConnection connection) {
        if (connection != null) {
            CONNECTIONS.add(connection);
        }
        return connection;
    }

    /**
     * Closes all the instrument profile connections and the endpoints and waits for the termination of the endpoints.
     * The objects remain usable only for reading their state.
     */
    public static void closeAllAndAwaitTermination() throws InterruptedException {
        final List<InstrumentProfileConnection> connections;
        final List<DXEndpoint> endpoints;
        synchronized (IsolateResources.class) {
            connections = new ArrayList<>(CONNECTIONS);
            endpoints = new ArrayList<>(ENDPOINTS);
            CONNECTIONS.clear();
            ENDPOINTS.clear();
        }
        for (final InstrumentProfileConnection connection : connections) {
            connection.close();
        }
        for (final DXEndpoint endpoint : endpoints) {
            closeAndAwaitTermination(endpoint);
        }
    }

    /**
     * {@link DXEndpoint#closeAndAwaitTermination()} that also returns for an endpoint with an {@link InPlaceExecutor}.
     *
     * <p>QD schedules the notification about the closed state on the endpoint's executor and waits until it runs (the
     * feed also waits for its delivery tasks). An in-place executor runs tasks only when asked, so for such an endpoint
     * the endpoint is closed on a helper thread while the calling thread runs the tasks of the executor, as
     * {@code processAllPendingTasks} would.
     */
    public static void closeAndAwaitTermination(final DXEndpoint endpoint) throws InterruptedException {
        // Closing the endpoint gets or creates its executor anyway (to notify about the closed state).
        final Executor executor = endpoint instanceof DXEndpointImpl
            ? ((DXEndpointImpl) endpoint).getOrCreateExecutor()
            : null;

        if (!(executor instanceof InPlaceExecutor)) {
            endpoint.closeAndAwaitTermination();
            return;
        }

        final InPlaceExecutor inPlaceExecutor = (InPlaceExecutor) executor;
        final CompletableFuture<Void> closed = new CompletableFuture<>();
        final Thread closer = new Thread(() -> {
            try {
                endpoint.closeAndAwaitTermination();
                closed.complete(null);
            } catch (final Throwable e) {
                closed.completeExceptionally(e);
            } finally {
                inPlaceExecutor.wakeUp();
            }
        }, "dxfg-endpoint-close");
        closer.setDaemon(true);
        closer.start();

        try {
            inPlaceExecutor.processTasksUntilDone(closed);
        } catch (final InterruptedException e) {
            closer.interrupt();
            throw e;
        }

        try {
            closed.join();
        } catch (final CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }
}
