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
import com.devexperts.transport.stats.EndpointStats;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.Quote;
import java.lang.reflect.Proxy;
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
        final Publisher started = startPublisher();
        final DXEndpoint publisher = started.endpoint();
        final int port = started.port();
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
        final int port = startPublisher().port();

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
        MessageConnectors.getIoStatistics(connector);
    }

    @Test
    public void ioStatisticsCountBothDirectionsAndTheRoundTripTime() throws Exception {
        assertThrows(NullPointerException.class, () -> MessageConnectors.getIoStatistics(null));

        final Publisher started = startPublisher();
        final DXEndpoint publisher = started.endpoint();
        final MessageConnector server = MessageConnectors.getConnectors(publisher).getFirst();
        final DXEndpoint feed = create(DXEndpoint.Role.FEED).connect("127.0.0.1:" + started.port());
        final MessageConnector client = MessageConnectors.getConnectors(feed).getFirst();

        await("the connection", () -> client.getState() == MessageConnectorState.CONNECTED);

        final DXFeedSubscription<Quote> subscription = feed.getFeed().createSubscription(Quote.class);

        subscription.addSymbols(SYMBOL);

        final boolean recordsCounted = client.getStats().getType() != QDStats.SType.VOID;

        assertEquals("QD's statistics", Boolean.parseBoolean(System.getProperty("dxfg.test.qdMonitoring", "true")),
                recordsCounted);

        if (!recordsCounted) {
            // Without qds-monitoring the ten counters stay 0; the connections are counted.
            final MessageConnectorIoStatistics statistics = MessageConnectors.getIoStatistics(client);

            assertEquals(statistics.toString(), 0, statistics.getReadBytes());
            assertEquals(statistics.toString(), 0, statistics.getWrittenSubRecords());
            assertEquals(statistics.toString(), 1, statistics.getActiveConnectionCount());

            return;
        }

        final Quote quote = new Quote(SYMBOL);

        // A new price each time: QD sends no record that does not change the quote (the default price is NaN).
        quote.setBidPrice(1.5);

        // Publish until the round-trip time, known after the heartbeats have gone both ways, reaches the sums.
        await("the round-trip time and the lag of the data read", () -> {
            quote.setBidPrice(quote.getBidPrice() + 1);
            publisher.getPublisher().publishEvents(List.of(quote));

            final MessageConnectorIoStatistics statistics = MessageConnectors.getIoStatistics(client);

            return statistics.getReadRttSumMicros() > 0 && statistics.getReadDataLagSumMicros() > 0;
        });

        final MessageConnectorStatistics v1 = MessageConnectors.getStatistics(client);
        final MessageConnectorIoStatistics feedSide = MessageConnectors.getIoStatistics(client);
        final MessageConnectorIoStatistics publisherSide = MessageConnectors.getIoStatistics(server);

        assertEquals(1, feedSide.getActiveConnectionCount());
        assertEquals(0, feedSide.getClosedConnectionCount());
        // The same QD counter as the v1 statistics, read later, so no smaller.
        assertTrue(feedSide.toString(), feedSide.getReadDataRecords() >= v1.getReadDataRecords());

        assertTrue(feedSide.toString(), feedSide.getReadDataRecords() > 0);
        assertTrue(feedSide.toString(), feedSide.getWrittenSubRecords() > 0);
        assertTrue(feedSide.toString(), feedSide.getReadBytes() > 0);
        assertTrue(feedSide.toString(), feedSide.getWrittenBytes() > 0);
        assertTrue(feedSide.toString(), feedSide.getReadRttSumMicros() > 0);
        assertTrue(feedSide.toString(), feedSide.getReadDataLagSumMicros() > 0);
        assertEquals(feedSide.toString(), 0, feedSide.getWrittenDataRecords());
        assertEquals(feedSide.toString(), 0, feedSide.getWrittenDataLagSumMicros());

        assertTrue(publisherSide.toString(), publisherSide.getReadSubRecords() > 0);
        assertTrue(publisherSide.toString(), publisherSide.getWrittenDataRecords() > 0);
        assertEquals(publisherSide.toString(), 0, publisherSide.getReadDataRecords());
        assertEquals(publisherSide.toString(), 0, publisherSide.getReadDataLagSumMicros());
        assertTrue(publisherSide.toString(), publisherSide.getWrittenDataLagSumMicros() >= 0);
    }

    @Test
    public void ioStatisticsMapEachCounterToItsOwnField() {
        // Distinct values for each counter, so a swapped pair fails.
        final QDStats stats = new QDStats(QDStats.SType.CONNECTION);

        stats.updateIOReadDataRecord();

        for (int i = 0; i < 2; i++) {
            stats.updateIOWriteDataRecord();
        }

        for (int i = 0; i < 3; i++) {
            stats.updateIOReadSubRecord();
        }

        for (int i = 0; i < 4; i++) {
            stats.updateIOWriteSubRecord();
        }

        stats.updateIOReadBytes(5);
        stats.updateIOWriteBytes(6);
        stats.updateIOReadDataLags(7);
        stats.updateIOWriteDataLags(8);
        stats.updateIOReadRtts(9);
        stats.updateIOWriteRtts(10);

        final EndpointStats endpointStats = new EndpointStats();

        endpointStats.setActiveConnectionCount(11);
        endpointStats.setClosedConnectionCount(12);

        final MessageConnector connector = (MessageConnector) Proxy.newProxyInstance(
                MessageConnector.class.getClassLoader(), new Class<?>[] {MessageConnector.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getStats" -> stats;
                    case "retrieveCompleteEndpointStats" -> endpointStats;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        final MessageConnectorIoStatistics statistics = MessageConnectors.getIoStatistics(connector);

        assertEquals(1, statistics.getReadDataRecords());
        assertEquals(2, statistics.getWrittenDataRecords());
        assertEquals(3, statistics.getReadSubRecords());
        assertEquals(4, statistics.getWrittenSubRecords());
        assertEquals(5, statistics.getReadBytes());
        assertEquals(6, statistics.getWrittenBytes());
        assertEquals(7, statistics.getReadDataLagSumMicros());
        assertEquals(8, statistics.getWrittenDataLagSumMicros());
        assertEquals(9, statistics.getReadRttSumMicros());
        assertEquals(10, statistics.getWrittenRttSumMicros());
        assertEquals(11, statistics.getActiveConnectionCount());
        assertEquals(12, statistics.getClosedConnectionCount());
    }
}
