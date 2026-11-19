// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorState;
import com.dxfeed.api.DXEndpoint;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.After;

/**
 * The endpoints of a connector test on loopback, closed after each test, and the waits for their connectors.
 */
abstract class ConnectorTestSupport {

    static final long TIMEOUT_MILLIS = 20_000;

    // A server binds a free port at once; four attempts take as long as one wait of TIMEOUT_MILLIS at most.
    private static final long LISTEN_MILLIS = 5_000;
    private static final int LISTEN_ATTEMPTS = 4;

    private final List<DXEndpoint> endpoints = new ArrayList<>();

    @After
    public void tearDown() throws InterruptedException {
        for (DXEndpoint endpoint : endpoints) {
            endpoint.closeAndAwaitTermination();
        }
    }

    DXEndpoint create(final DXEndpoint.Role role) {
        final DXEndpoint endpoint = DXEndpoint.newBuilder().withRole(role).build();
        endpoints.add(endpoint);
        return endpoint;
    }

    static int findFreeLoopbackPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    static void await(final String what, final BooleanSupplier condition) throws InterruptedException {
        if (!awaitFor(TIMEOUT_MILLIS, condition)) {
            throw new AssertionError("Timed out waiting for " + what);
        }
    }

    /**
     * A publisher endpoint whose server connector listens on a free port.
     */
    record Publisher(DXEndpoint endpoint, int port) {}

    /**
     * Starts a publisher on a free port. The probed port is free only until it is closed: an outgoing connection may
     * take it from the ephemeral range before the server binds it, and QD retries the bind later than a test waits. So
     * a server that does not listen soon is closed and the next attempt takes a new port.
     */
    Publisher startPublisher() throws IOException, InterruptedException {
        for (int attempt = 1; ; attempt++) {
            final int port = findFreeLoopbackPort();
            final DXEndpoint publisher = create(DXEndpoint.Role.PUBLISHER).connect(":" + port);
            final MessageConnector server = MessageConnectors.getConnectors(publisher).getFirst();

            if (awaitFor(LISTEN_MILLIS, () -> server.getState() == MessageConnectorState.CONNECTED)) {
                return new Publisher(publisher, port);
            }

            if (attempt == LISTEN_ATTEMPTS) {
                throw new AssertionError("Timed out waiting for the server socket on " + LISTEN_ATTEMPTS + " ports");
            }

            publisher.closeAndAwaitTermination();
            endpoints.remove(publisher);
        }
    }

    private static boolean awaitFor(final long millis, final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + millis;

        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                return false;
            }

            Thread.sleep(20);
        }

        return true;
    }
}
