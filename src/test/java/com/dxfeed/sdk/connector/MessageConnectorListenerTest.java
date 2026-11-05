// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorState;
import com.dxfeed.api.DXEndpoint;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.Test;

/**
 * {@link ConnectorStateListener} on endpoints with real QD connectors on loopback: the listener is called on the
 * thread of the dispatcher, keeps working after a {@code connect} to another address, may call {@code connect} and read
 * the statistics from its call, gets the connectors of its own endpoint only, and is silent once deactivated.
 */
public class MessageConnectorListenerTest extends ConnectorTestSupport {

    // A listener that is deactivated stays silent this long while the connector keeps reconnecting.
    private static final long QUIET_MILLIS = 2000;

    private static final class RecordingListener extends ConnectorStateListener {
        final List<MessageConnector> connectors = new CopyOnWriteArrayList<>();
        final List<MessageConnectorState> states = new CopyOnWriteArrayList<>();
        final List<String> threads = new CopyOnWriteArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        volatile Consumer<MessageConnector> action = connector -> {
        };

        @Override
        protected void callNative(final MessageConnector connector) {
            calls.incrementAndGet();
            connectors.add(connector);
            states.add(connector.getState());
            threads.add(Thread.currentThread().getName());
            action.accept(connector);
        }

        boolean saw(final MessageConnector connector, final MessageConnectorState state) {
            for (int i = 0; i < connectors.size(); i++) {
                if (connectors.get(i) == connector && states.get(i) == state) {
                    return true;
                }
            }

            return false;
        }
    }

    @Test
    public void rejectsNullAndForeignEndpoints() {
        final RecordingListener listener = new RecordingListener();

        assertThrows(IllegalArgumentException.class, () -> ConnectorMonitor.addListener(null, listener));
        assertThrows(IllegalArgumentException.class, () -> ConnectorMonitor.removeListener(null, listener));
    }

    @Test
    public void listenerIsCalledOnTheDispatcherThreadAndFollowsNewConnectors() throws Exception {
        final int port = findFreeLoopbackPort();
        final DXEndpoint publisher = startPublisher(port);
        final DXEndpoint feed = create(DXEndpoint.Role.FEED);
        final RecordingListener listener = new RecordingListener();

        ConnectorMonitor.addListener(feed, listener);
        feed.connect("127.0.0.1:" + port);

        final MessageConnector first = MessageConnectors.getConnectors(feed).getFirst();

        await("CONNECTED", () -> listener.saw(first, MessageConnectorState.CONNECTED));

        for (String thread : listener.threads) {
            assertEquals(ConnectorDispatcher.THREAD_NAME, thread);
        }

        // The publisher goes away: the client reports that it is not connected.
        publisher.closeAndAwaitTermination();
        await("a state other than CONNECTED", () -> listener.saw(first, MessageConnectorState.CONNECTING) ||
                listener.saw(first, MessageConnectorState.DISCONNECTED));

        // The connector of a new address is reported without adding the listener again.
        final int otherPort = findFreeLoopbackPort();
        startPublisher(otherPort);
        feed.connect("127.0.0.1:" + otherPort);

        final MessageConnector second = MessageConnectors.getConnectors(feed).getFirst();

        assertFalse(first == second);
        await("CONNECTED of the new connector", () -> listener.saw(second, MessageConnectorState.CONNECTED));
    }

    @Test
    public void listenerMayConnectAndReadStatisticsFromItsCall() throws Exception {
        final int port = findFreeLoopbackPort();
        startPublisher(port);

        final DXEndpoint feed = create(DXEndpoint.Role.FEED);
        final RecordingListener listener = new RecordingListener();
        final CountDownLatch reconnected = new CountDownLatch(1);
        final String target = "127.0.0.1:" + port;

        listener.action = connector -> {
            MessageConnectors.getStatistics(connector);

            if (connector.getAddress().equals("127.0.0.1:1")) {
                feed.connect(target);
            } else if (connector.getState() == MessageConnectorState.CONNECTED) {
                reconnected.countDown();
            }
        };

        ConnectorMonitor.addListener(feed, listener);
        feed.connect("127.0.0.1:1");

        assertTrue("No deadlock: the listener connected the endpoint to the publisher",
                reconnected.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void eachListenerGetsTheConnectorsOfItsOwnEndpoint() throws Exception {
        final int port = findFreeLoopbackPort();
        startPublisher(port);

        final DXEndpoint feed1 = create(DXEndpoint.Role.FEED);
        final DXEndpoint feed2 = create(DXEndpoint.Role.FEED);
        final RecordingListener listener1 = new RecordingListener();
        final RecordingListener listener2 = new RecordingListener();

        ConnectorMonitor.addListener(feed1, listener1);
        ConnectorMonitor.addListener(feed2, listener2);
        feed1.connect("127.0.0.1:" + port);
        feed2.connect("127.0.0.1:" + port);

        final MessageConnector connector1 = MessageConnectors.getConnectors(feed1).getFirst();
        final MessageConnector connector2 = MessageConnectors.getConnectors(feed2).getFirst();

        // Same default names, different endpoints.
        assertEquals(connector1.getName(), connector2.getName());
        await("CONNECTED of both", () -> listener1.saw(connector1, MessageConnectorState.CONNECTED) &&
                listener2.saw(connector2, MessageConnectorState.CONNECTED));

        for (MessageConnector connector : listener1.connectors) {
            assertTrue(connector == connector1);
        }

        for (MessageConnector connector : listener2.connectors) {
            assertTrue(connector == connector2);
        }
    }

    @Test
    public void listenerAddedToConnectedEndpointGetsOnlyLaterChanges() throws Exception {
        final int port = findFreeLoopbackPort();
        startPublisher(port);

        final DXEndpoint feed = create(DXEndpoint.Role.FEED).connect("127.0.0.1:" + port);
        final MessageConnector connector = MessageConnectors.getConnectors(feed).getFirst();

        await("the client socket", () -> connector.getState() == MessageConnectorState.CONNECTED);

        // QD calls a new QD listener for the current connectors within its add: neither the first listener, which
        // creates the monitor, nor the second one may get these calls.
        final RecordingListener first = new RecordingListener();
        final RecordingListener second = new RecordingListener();

        ConnectorMonitor.addListener(feed, first);
        ConnectorMonitor.addListener(feed, second);
        Thread.sleep(QUIET_MILLIS);

        assertEquals(0, first.calls.get());
        assertEquals(0, second.calls.get());

        feed.reconnect();
        await("the calls of a reconnect", () -> first.calls.get() > 0 && second.calls.get() > 0);
    }

    @Test
    public void removedOrDeactivatedListenerIsSilent() throws Exception {
        final DXEndpoint feed = create(DXEndpoint.Role.FEED);
        final RecordingListener removed = new RecordingListener();
        final RecordingListener deactivated = new RecordingListener();
        final RecordingListener watcher = new RecordingListener();

        ConnectorMonitor.addListener(feed, removed);
        // A second add of the same listener changes nothing: one remove below silences it.
        ConnectorMonitor.addListener(feed, removed);
        ConnectorMonitor.addListener(feed, deactivated);
        ConnectorMonitor.addListener(feed, watcher);

        // A connector to a closed port reconnects again and again: a stream of state changes.
        feed.connect("127.0.0.1:1");
        await("the first calls", () -> removed.calls.get() > 0 && deactivated.calls.get() > 0);

        ConnectorMonitor.removeListener(feed, removed);
        deactivated.deactivate();
        // A remove of a listener that is not added is no error.
        ConnectorMonitor.removeListener(feed, new RecordingListener());

        final int removedCalls = removed.calls.get();
        final int deactivatedCalls = deactivated.calls.get();
        final int watched = watcher.calls.get();

        feed.reconnect();
        await("a call of the listener that stays", () -> watcher.calls.get() > watched);
        Thread.sleep(QUIET_MILLIS);

        assertEquals(removedCalls, removed.calls.get());
        assertEquals(deactivatedCalls, deactivated.calls.get());
    }

    @Test
    public void listenerDeactivatesItselfFromItsCall() throws Exception {
        final DXEndpoint feed = create(DXEndpoint.Role.FEED);
        final RecordingListener listener = new RecordingListener();
        final CountDownLatch returned = new CountDownLatch(1);

        listener.action = connector -> {
            listener.deactivate();
            returned.countDown();
        };

        ConnectorMonitor.addListener(feed, listener);
        feed.connect("127.0.0.1:1");

        assertTrue("The deactivation from the call returned", returned.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));

        feed.reconnect();
        Thread.sleep(QUIET_MILLIS);

        assertEquals(1, listener.calls.get());
    }
}
