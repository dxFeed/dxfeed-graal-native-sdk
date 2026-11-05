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
        final long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;

        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timed out waiting for " + what);
            }

            Thread.sleep(20);
        }
    }

    DXEndpoint startPublisher(final int port) throws InterruptedException {
        final DXEndpoint publisher = create(DXEndpoint.Role.PUBLISHER).connect(":" + port);
        final MessageConnector server = MessageConnectors.getConnectors(publisher).getFirst();

        await("the server socket", () -> server.getState() == MessageConnectorState.CONNECTED);

        return publisher;
    }
}
