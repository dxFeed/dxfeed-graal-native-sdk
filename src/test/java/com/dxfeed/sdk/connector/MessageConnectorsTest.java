// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorState;
import com.devexperts.qd.stats.QDStats;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.Quote;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * {@link MessageConnectors} against real QD connectors on loopback: a publisher endpoint with a server connector and
 * feed endpoints with client connectors.
 */
public class MessageConnectorsTest extends ConnectorTestSupport {

    private static final String SYMBOL = "CONNECTOR-TEST";

    @Test
    public void rejectsNullEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> MessageConnectors.getConnectors(null));
        assertThrows(IllegalArgumentException.class, () -> MessageConnectors.getQDEndpoint(null));
    }

    @Test
    public void localHubAndNeverConnectedEndpointsHaveNoConnectors() {
        assertTrue(MessageConnectors.getConnectors(create(DXEndpoint.Role.LOCAL_HUB)).isEmpty());
        assertTrue(MessageConnectors.getConnectors(create(DXEndpoint.Role.FEED)).isEmpty());
    }

    @Test
    public void clientToClosedPortIsConnecting() {
        final DXEndpoint feed = create(DXEndpoint.Role.FEED).connect("127.0.0.1:1");
        final List<MessageConnector> connectors = MessageConnectors.getConnectors(feed);

        assertEquals(1, connectors.size());

        final MessageConnector connector = connectors.getFirst();

        assertEquals(MessageConnectors.KIND_CLIENT_SOCKET, MessageConnectors.getKind(connector));
        assertEquals("127.0.0.1:1", MessageConnectors.getAddress(connector));
        assertEquals(MessageConnectorState.CONNECTING, connector.getState());
        assertTrue(connector.isActive());
        assertEquals(0, connector.getConnectionCount());
        assertTrue(MessageConnectors.getCurrentAddresses(connector).isEmpty());

        final MessageConnectorStatistics statistics = MessageConnectors.getStatistics(connector);

        assertEquals(0, statistics.getActiveConnectionCount());
        assertEquals(0, statistics.getReadDataRecords());
    }

    @Test
    public void connectedClientReportsAddressAndCountsDataRecords() throws Exception {
        final int port = findFreeLoopbackPort();
        final DXEndpoint publisher = startPublisher(port);
        final DXEndpoint feed = create(DXEndpoint.Role.FEED).connect("127.0.0.1:" + port);
        final MessageConnector client = MessageConnectors.getConnectors(feed).getFirst();

        await("the connection", () -> client.getState() == MessageConnectorState.CONNECTED);

        assertEquals(1, client.getConnectionCount());
        assertEquals(List.of("127.0.0.1:" + port), MessageConnectors.getCurrentAddresses(client));

        final MessageConnector server = MessageConnectors.getConnectors(publisher).getFirst();

        assertEquals(MessageConnectors.KIND_SERVER_SOCKET, MessageConnectors.getKind(server));
        assertTrue(MessageConnectors.getCurrentAddresses(server).isEmpty());
        await("the accepted connection", () -> server.getConnectionCount() == 1);

        final LinkedBlockingQueue<Quote> received = new LinkedBlockingQueue<>();
        final DXFeedSubscription<Quote> subscription = feed.getFeed().createSubscription(Quote.class);

        subscription.addEventListener(received::addAll);
        subscription.addSymbols(SYMBOL);

        final MessageConnectorStatistics before = MessageConnectors.getStatistics(client);
        final Quote quote = new Quote(SYMBOL);

        quote.setBidPrice(1.5);

        // Publish until the subscription reaches the publisher and the quote comes back.
        final long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        Quote event = null;

        while (event == null && System.currentTimeMillis() < deadline) {
            publisher.getPublisher().publishEvents(List.of(quote));
            event = received.poll(100, TimeUnit.MILLISECONDS);
        }

        assertTrue("No quote received", event != null);

        // The iOS builds have no qds-monitoring: QD keeps no statistics there, and the data record counts stay 0. The
        // build says which case to expect, so that statistics lost elsewhere fail the test.
        final boolean recordsCounted = client.getStats().getType() != QDStats.SType.VOID;

        assertEquals("QD's statistics", Boolean.parseBoolean(System.getProperty("dxfg.test.qdMonitoring", "true")),
                recordsCounted);

        if (recordsCounted) {
            await("the data record count",
                    () -> MessageConnectors.getStatistics(client).getReadDataRecords() > before.getReadDataRecords());
        }

        final MessageConnectorStatistics after = MessageConnectors.getStatistics(client);

        assertTrue(after.getReadBytes() > before.getReadBytes());
        assertTrue(after.getWrittenBytes() > 0);
        assertEquals(1, after.getActiveConnectionCount());

        if (recordsCounted) {
            assertTrue(MessageConnectors.getStatistics(server).getWrittenDataRecords() > 0);
        } else {
            assertEquals(0, after.getReadDataRecords());
            assertEquals(0, MessageConnectors.getStatistics(server).getWrittenDataRecords());
        }

        // Reading the statistics resets nothing.
        assertEquals(after.getReadDataRecords(), MessageConnectors.getStatistics(client).getReadDataRecords());
    }

    @Test
    public void connectToAnotherAddressReplacesConnectors() throws Exception {
        final int port = findFreeLoopbackPort();
        startPublisher(port);

        final DXEndpoint feed = create(DXEndpoint.Role.FEED).connect("127.0.0.1:1");
        final MessageConnector first = MessageConnectors.getConnectors(feed).getFirst();

        feed.connect("127.0.0.1:1");

        assertSame("The same address keeps the connector", first, MessageConnectors.getConnectors(feed).getFirst());

        feed.connect("127.0.0.1:" + port);

        final MessageConnector second = MessageConnectors.getConnectors(feed).getFirst();

        assertNotSame(first, second);
        assertEquals("127.0.0.1:" + port, MessageConnectors.getAddress(second));
        assertEquals(MessageConnectorState.DISCONNECTED, first.getState());
        assertFalse(first.isActive());

        await("the connection", () -> second.getState() == MessageConnectorState.CONNECTED);

        feed.reconnect();

        assertSame("reconnect keeps the connector", second, MessageConnectors.getConnectors(feed).getFirst());
        await("the closed connection count", () -> second.getClosedConnectionCount() > 0);
        assertEquals(second.getClosedConnectionCount(),
                MessageConnectors.getStatistics(second).getClosedConnectionCount());
    }

    @Test
    public void multiAddressCreatesOneConnectorPerAddress() {
        final DXEndpoint feed = create(DXEndpoint.Role.FEED).connect("(127.0.0.1:1)(127.0.0.1:2)");
        final List<MessageConnector> connectors = MessageConnectors.getConnectors(feed);

        assertEquals(2, connectors.size());
        assertEquals("127.0.0.1:1", MessageConnectors.getAddress(connectors.get(0)));
        assertEquals("127.0.0.1:2", MessageConnectors.getAddress(connectors.get(1)));
    }

    @Test
    public void addressCredentialsAreMasked() {
        final DXEndpoint feed = create(DXEndpoint.Role.FEED).connect("127.0.0.1:1[user=demo,password=secret]");
        final String address = MessageConnectors.getAddress(MessageConnectors.getConnectors(feed).getFirst());

        assertTrue(address, address.startsWith("127.0.0.1:1"));
        assertFalse(address, address.contains("secret"));
    }

    @Test
    public void closedEndpointReturnsNoConnectorsAndOldOnesStayReadable() throws InterruptedException {
        final DXEndpoint feed = create(DXEndpoint.Role.FEED).connect("127.0.0.1:1");
        final MessageConnector connector = MessageConnectors.getConnectors(feed).getFirst();

        feed.closeAndAwaitTermination();

        assertTrue(MessageConnectors.getConnectors(feed).isEmpty());
        assertEquals(MessageConnectorState.DISCONNECTED, connector.getState());
        MessageConnectors.getStatistics(connector);
    }
}
