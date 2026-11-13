// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.devexperts.qd.qtp.socket.ServerSocketTestHelper;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.osub.ObservableSubscription;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import com.dxfeed.promise.Promise;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * The tool in-process on a loopback port: a FEED endpoint receives what the generators publish. The tool listens on
 * port 0, so no port is taken and released before the test uses it.
 */
public class NetTestApiLoopbackTest {
    private static final long DEADLINE_SECONDS = 15;

    @Test
    public void feedReceivesTradesWithCurrentTime() throws InterruptedException {
        MarketEvent event = receiveFirst(Trade.class, "p", ":0", "-S", "100");

        long now = System.currentTimeMillis();
        Trade trade = (Trade) event;
        assertTrue("time " + trade.getTime() + " must be within 10 s of " + now,
            Math.abs(now - trade.getTime()) < 10_000);
        assertTrue(trade.getPrice() > 0);
    }

    @Test
    public void feedReceivesTimeAndSalesFromStreamPublisher() throws InterruptedException {
        MarketEvent event = receiveFirst(TimeAndSale.class, "p", ":0", "-S", "100", "--event", "TimeAndSale",
            "--role", "STREAM_PUBLISHER", "--reuse", "-r", "*/10k");

        long now = System.currentTimeMillis();
        assertTrue(Math.abs(now - ((TimeAndSale) event).getTime()) < 10_000);
    }

    @Test
    public void failedPublishingStopsTheRunAndClosesAllEndpoints() {
        // A client address: the instances need no peer to start.
        NetTestApi.Settings settings = NetTestApi.parseArguments("p", "127.0.0.1:1", "-S", "10", "-C", "3");
        NetTestApiProducerSide side = new NetTestApiProducerSide(settings, new NetTestApi.Stop()) {
            @Override
            NetTestApiGeneratorThread createGenerator(int index, DXEndpoint endpoint) {
                DXPublisher publisher = index == 2 ? new FailingPublisher() : endpoint.getPublisher();
                return new NetTestApiGeneratorThread(index, this, qdEndpoint(endpoint), publisher);
            }
        };

        String failure = side.run(); // without --duration it ends only through the failure

        assertNotNull(failure);
        assertTrue(failure, failure.contains("publish failure for the test"));
        assertEquals(3, side.getEndpoints().size());
        for (DXEndpoint endpoint : side.getEndpoints()) {
            assertEquals(DXEndpoint.State.CLOSED, endpoint.getState());
        }
    }

    @Test
    public void busyPortFailsTheStart() throws Exception {
        // The test holds the port for the whole case (exclusively on Windows, where Java binds so by default).
        try (ServerSocket occupant = new ServerSocket(0)) {
            NetTestApiProducerSide side = new NetTestApiProducerSide(
                NetTestApi.parseArguments("p", ":" + occupant.getLocalPort(), "-S", "1", "--duration", "20s"),
                new NetTestApi.Stop());
            long start = System.nanoTime();

            String failure = side.run();

            assertNotNull(failure);
            assertTrue(failure, failure.contains("did not start in 3000 ms"));
            assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 10);
            assertEquals(DXEndpoint.State.CLOSED, side.getEndpoints().get(0).getState());
        }
    }

    // The first two arguments are "p" and ":0": the tool listens on a port that the system picks, and QD's test helper
    // reports it.
    @SuppressWarnings("deprecation")
    private static MarketEvent receiveFirst(Class<? extends MarketEvent> type, String... args)
        throws InterruptedException
    {
        String connectorName = "NetTestApiLoopbackTest-" + UUID.randomUUID();
        Promise<Integer> port = ServerSocketTestHelper.createPortPromise(connectorName);
        args[1] = ":0[name=" + connectorName + "]";
        NetTestApiProducerSide side = start(args);
        DXEndpoint feed = DXEndpoint.create(DXEndpoint.Role.FEED);
        try {
            int localPort = port.await(DEADLINE_SECONDS, TimeUnit.SECONDS);
            BlockingQueue<MarketEvent> received = new LinkedBlockingQueue<>();
            DXFeedSubscription<MarketEvent> subscription = feed.getFeed().createSubscription(type);
            subscription.addEventListener(received::addAll);
            Set<String> symbols = new HashSet<>(Arrays.asList(side.getSymbols()));
            subscription.addSymbols(symbols);
            feed.connect("127.0.0.1:" + localPort);

            MarketEvent event = received.poll(DEADLINE_SECONDS, TimeUnit.SECONDS);

            assertNotNull("no " + type.getSimpleName() + " within " + DEADLINE_SECONDS + " s", event);
            assertTrue(symbols.contains(event.getEventSymbol()));
            return event;
        } finally {
            feed.close();
            assertNull(side.finish());
            assertEquals(1, side.getEndpoints().size());
            assertEquals(DXEndpoint.State.CLOSED, side.getEndpoints().get(0).getState());
        }
    }

    private static NetTestApiProducerSide start(String... args) {
        NetTestApiProducerSide side = new NetTestApiProducerSide(NetTestApi.parseArguments(args),
            new NetTestApi.Stop());
        side.start();
        return side;
    }

    private static final class FailingPublisher extends DXPublisher {
        @Override
        public void publishEvents(Collection<?> events) {
            throw new IllegalStateException("publish failure for the test");
        }

        @Override
        public <E> ObservableSubscription<E> getSubscription(Class<E> eventType) {
            throw new UnsupportedOperationException();
        }
    }
}
