// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.ipf.live.InstrumentProfileConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

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

    public static synchronized DXEndpoint register(final DXEndpoint endpoint) {
        if (endpoint != null) {
            ENDPOINTS.add(endpoint);
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
            endpoint.closeAndAwaitTermination();
        }
    }
}
