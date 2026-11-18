// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.devexperts.qd.qtp.socket.ServerSocketTestHelper;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.Trade;
import com.dxfeed.promise.Promise;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

/**
 * The tool sets {@code dxscheme.nanoTime} on the builder of its endpoints: {@code true} by default, the value of
 * {@code -Ddxscheme.nanoTime} when the command line gives one. The consumer is a STREAM_FEED endpoint with the builder
 * property, which takes no system property, and the test JVM has no {@code dxscheme.nanoTime} system property (see
 * {@link NanoTimeSystemPropertyIT}), so the nanosecond part arrives only when the builder of the tool switches it on.
 */
public class NetTestApiNanoTimeIT {
    private static final long MIN_EVENTS = 100;

    @Test
    public void theValueForTheBuilders() {
        assertEquals("true", NetTestApiProducerSide.endpointNanoTime(null));
        assertEquals("false", NetTestApiProducerSide.endpointNanoTime("false"));
        assertEquals("true", NetTestApiProducerSide.endpointNanoTime("true"));
    }

    @Test
    public void aStreamFeedReceivesTheNanosecondPartByDefault() throws Exception {
        // With the system property the PUBLISHER would take the nanosecond part without the builder of the tool.
        assertNull("the system property " + NetTestApiProducerSide.NANO_TIME_PROPERTY,
            System.getProperty(NetTestApiProducerSide.NANO_TIME_PROPERTY));
        long[] received = run();
        assertTrue("events " + received[0], received[0] >= MIN_EVENTS);
        assertTrue("events with a nanosecond part " + received[1], received[1] > 0);
    }

    @Test
    public void falseOnTheCommandLineSwitchesTheNanosecondPartOff() throws Exception {
        // The -D options of the tool are system properties; "false" is what the JVM had without the property.
        String key = NetTestApiProducerSide.NANO_TIME_PROPERTY;
        String before = System.getProperty(key);
        long[] received;
        try {
            received = run("-D" + key + "=false");
        } finally {
            if (before == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, before);
            }
        }
        assertTrue("events " + received[0], received[0] >= MIN_EVENTS);
        assertEquals("events with a nanosecond part", 0, received[1]);
    }

    /**
     * Runs the tool for Trade through PUBLISHER endpoints on a port that the system picks, with a STREAM_FEED consumer
     * that has the builder property, until the consumer has {@link #MIN_EVENTS} events or the wait is over. Returns
     * the received events and those with a nonzero nanosecond part.
     */
    @SuppressWarnings("deprecation")
    private static long[] run(String... options) throws Exception {
        String connectorName = "NetTestApiNanoTimeIT-" + UUID.randomUUID();
        Promise<Integer> port = ServerSocketTestHelper.createPortPromise(connectorName);
        String[] args = new String[options.length + 6];
        System.arraycopy(options, 0, args, 0, options.length);
        System.arraycopy(new String[] {"p", ":0[name=" + connectorName + "]", "-S", "100", "-r", "*/2k"}, 0, args,
            options.length, 6);
        NetTestApiProducerSide side = new NetTestApiProducerSide(NetTestApi.parseArguments(args),
            new NetTestApi.Stop());
        side.start();
        DXEndpoint consumer = NanoTimeProbes.endpoint(DXEndpoint.Role.STREAM_FEED, true);
        try {
            AtomicLong events = new AtomicLong();
            AtomicLong withNanoPart = new AtomicLong();
            DXFeedSubscription<Trade> subscription = consumer.getFeed().createSubscription(Trade.class);
            subscription.addEventListener(received -> {
                for (Trade trade : received) {
                    events.incrementAndGet();
                    if (trade.getTimeNanoPart() != 0) {
                        withNanoPart.incrementAndGet();
                    }
                }
            });
            subscription.addSymbols(Arrays.asList(side.getSymbols()));
            consumer.connect("127.0.0.1:" + port.await(NanoTimeProbes.DELIVERY_SECONDS, TimeUnit.SECONDS));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(NanoTimeProbes.DELIVERY_SECONDS);
            while (events.get() < MIN_EVENTS && System.nanoTime() - deadline < 0) {
                Thread.sleep(50);
            }
            return new long[] {events.get(), withNanoPart.get()};
        } finally {
            consumer.close();
            assertNull(side.finish());
        }
    }
}
