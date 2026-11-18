// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertTrue;

import com.devexperts.qd.qtp.socket.ServerSocketTestHelper;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import com.dxfeed.promise.Promise;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The helpers of the probes that the NetTest specification relies on: a publisher and a consumer endpoint that listen
 * on port 0 in the test JVM, and a QD {@code Multiplexor} in its own JVM (the all-in-one jar) that connects to both.
 * The multiplexor reads {@code dxscheme.nanoTime} when its scheme is created, so it runs in a process of its own.
 */
final class NanoTimeProbes {
    /** The nanosecond part of every published event. */
    static final int NANO_PART = 123_456;

    static final String SYMBOL = "NANO";

    /** How long a probe waits for an event that must arrive. */
    static final long DELIVERY_SECONDS = 20;

    /** How long a probe waits before it decides that an event does not arrive. */
    static final long SILENCE_SECONDS = 3;

    private NanoTimeProbes() {
    }

    /** The endpoint of a role, with {@code dxscheme.nanoTime=true} through its builder or without the property. */
    static DXEndpoint endpoint(DXEndpoint.Role role, boolean nanoTime) {
        DXEndpoint.Builder builder = DXEndpoint.newBuilder().withRole(role);
        if (nanoTime) {
            builder.withProperty("dxscheme.nanoTime", "true");
        }
        return builder.build();
    }

    /** Connects the endpoint to a listening port that the system picks and returns the port. */
    @SuppressWarnings("deprecation")
    static int listen(DXEndpoint endpoint) {
        String name = "NanoTimeProbes-" + UUID.randomUUID();
        Promise<Integer> port = ServerSocketTestHelper.createPortPromise(name);
        endpoint.connect(":0[name=" + name + "]");
        return port.await(DELIVERY_SECONDS, TimeUnit.SECONDS);
    }

    /** A Trade or a TimeAndSale of {@link #SYMBOL} at the current time with {@link #NANO_PART}. */
    static MarketEvent event(Class<? extends MarketEvent> type) {
        long now = System.currentTimeMillis();
        if (type == Trade.class) {
            Trade trade = new Trade(SYMBOL);
            trade.setTime(now);
            trade.setTimeNanoPart(NANO_PART);
            trade.setPrice(100.5);
            trade.setSize(10);
            return trade;
        }
        TimeAndSale timeAndSale = new TimeAndSale(SYMBOL);
        timeAndSale.setTime(now);
        timeAndSale.setTimeNanoPart(NANO_PART);
        timeAndSale.setPrice(100.5);
        timeAndSale.setSize(10);
        return timeAndSale;
    }

    static int nanoPart(MarketEvent event) {
        return event instanceof Trade ? ((Trade) event).getTimeNanoPart() : ((TimeAndSale) event).getTimeNanoPart();
    }

    /**
     * Subscribes the consumer to the type and {@link #SYMBOL}, then publishes an event every 100 ms until the first
     * one arrives or the wait is over. Returns the first received event or null.
     */
    static MarketEvent publishUntilReceived(DXEndpoint publisher, DXEndpoint consumer,
        Class<? extends MarketEvent> type, long waitSeconds) throws InterruptedException
    {
        BlockingQueue<MarketEvent> received = new LinkedBlockingQueue<>();
        DXFeedSubscription<MarketEvent> subscription = consumer.getFeed().createSubscription(type);
        subscription.addEventListener(received::addAll);
        subscription.addSymbols(SYMBOL);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(waitSeconds);
        while (System.nanoTime() - deadline < 0) {
            publisher.getPublisher().publishEvents(Collections.singletonList(event(type)));
            MarketEvent event = received.poll(100, TimeUnit.MILLISECONDS);
            if (event != null) {
                return event;
            }
        }
        return null;
    }

    /** A QD Multiplexor in its own JVM that connects to the publisher's and the consumer's ports. */
    static final class Mux implements AutoCloseable {
        private final Process process;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final Thread drain;

        Mux(int publisherPort, int consumerPort, boolean nanoTime) throws IOException {
            Path jar = Paths.get(System.getProperty("nettest.shadedJar"));
            assertTrue("no " + jar, Files.isRegularFile(jar));
            List<String> command = new ArrayList<>();
            command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
            if (nanoTime) {
                command.add("-Ddxscheme.nanoTime=true");
            }
            command.add("-jar");
            command.add(jar.toString());
            command.add("Multiplexor");
            command.add("127.0.0.1:" + publisherPort);
            command.add("127.0.0.1:" + consumerPort);
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            drain = new Thread(() -> {
                try (InputStream in = process.getInputStream()) {
                    in.transferTo(output);
                } catch (IOException e) {
                    // the process is gone
                }
            });
            drain.setDaemon(true);
            drain.start();
        }

        /** What the multiplexor printed so far, for the failure messages. */
        String output() {
            synchronized (output) {
                return output.toString(StandardCharsets.UTF_8);
            }
        }

        @Override
        public void close() throws InterruptedException {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
            drain.join(5_000);
        }
    }
}
