// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static com.devexperts.qd.tools.NanoTimeProbes.DELIVERY_SECONDS;
import static com.devexperts.qd.tools.NanoTimeProbes.NANO_PART;
import static com.devexperts.qd.tools.NanoTimeProbes.SILENCE_SECONDS;
import static com.devexperts.qd.tools.NanoTimeProbes.endpoint;
import static com.devexperts.qd.tools.NanoTimeProbes.listen;
import static com.devexperts.qd.tools.NanoTimeProbes.nanoPart;
import static com.devexperts.qd.tools.NanoTimeProbes.publishUntilReceived;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import org.junit.Test;

/**
 * The mappings that the NetTest specification relies on, with the QD of this build.
 * <ul>
 * <li>The nanosecond part of the event time survives publisher -> multiplexor -> consumer for each publisher role and
 * each consumer role when every hop has {@code dxscheme.nanoTime=true} (the endpoints through their builders, the
 * multiplexor through its system property), and is 0 when any hop lacks it; the events still arrive.</li>
 * <li>Which roles deliver Trade and TimeAndSale to which subscriptions.</li>
 * </ul>
 * The test JVM has no {@code dxscheme.nanoTime} system property (see {@link NanoTimeSystemPropertyIT}).
 */
public class NanoTimeMappingIT {
    // 1. The nanosecond part through the multiplexor: every hop has the property.

    @Test
    public void tradeFromPublisherToFeedKeepsTheNanoPart() throws Exception {
        assertEquals(NANO_PART, throughMux(DXEndpoint.Role.PUBLISHER, true, true, DXEndpoint.Role.FEED, true,
            Trade.class));
    }

    @Test
    public void tradeFromPublisherToStreamFeedKeepsTheNanoPart() throws Exception {
        assertEquals(NANO_PART, throughMux(DXEndpoint.Role.PUBLISHER, true, true, DXEndpoint.Role.STREAM_FEED, true,
            Trade.class));
    }

    @Test
    public void timeAndSaleFromStreamPublisherToFeedKeepsTheNanoPart() throws Exception {
        assertEquals(NANO_PART, throughMux(DXEndpoint.Role.STREAM_PUBLISHER, true, true, DXEndpoint.Role.FEED, true,
            TimeAndSale.class));
    }

    @Test
    public void timeAndSaleFromStreamPublisherToStreamFeedKeepsTheNanoPart() throws Exception {
        assertEquals(NANO_PART, throughMux(DXEndpoint.Role.STREAM_PUBLISHER, true, true, DXEndpoint.Role.STREAM_FEED,
            true, TimeAndSale.class));
    }

    // 1. One hop without the property: the event arrives with the nanosecond part 0.

    @Test
    public void muxWithoutThePropertyDropsTheNanoPart() throws Exception {
        assertEquals(0, throughMux(DXEndpoint.Role.PUBLISHER, true, false, DXEndpoint.Role.FEED, true, Trade.class));
    }

    @Test
    public void consumerWithoutThePropertyGetsNoNanoPart() throws Exception {
        assertEquals(0, throughMux(DXEndpoint.Role.STREAM_PUBLISHER, true, true, DXEndpoint.Role.STREAM_FEED, false,
            TimeAndSale.class));
    }

    @Test
    public void publisherWithoutThePropertySendsNoNanoPart() throws Exception {
        assertEquals(0, throughMux(DXEndpoint.Role.PUBLISHER, false, true, DXEndpoint.Role.FEED, true, Trade.class));
    }

    // 3. The contracts, publisher -> consumer directly, every endpoint with the property.

    @Test
    public void tradeFromPublisherReachesFeed() throws Exception {
        assertNotNull(direct(DXEndpoint.Role.PUBLISHER, DXEndpoint.Role.FEED, Trade.class, DELIVERY_SECONDS));
    }

    @Test
    public void tradeFromPublisherReachesStreamFeed() throws Exception {
        assertNotNull(direct(DXEndpoint.Role.PUBLISHER, DXEndpoint.Role.STREAM_FEED, Trade.class, DELIVERY_SECONDS));
    }

    @Test
    public void tradeFromStreamPublisherDoesNotReachFeed() throws Exception {
        // The ticker subscription of FEED is not fed by the stream contract: nothing arrives and nothing fails.
        assertNull(direct(DXEndpoint.Role.STREAM_PUBLISHER, DXEndpoint.Role.FEED, Trade.class, SILENCE_SECONDS));
    }

    @Test
    public void tradeFromStreamPublisherReachesStreamFeed() throws Exception {
        assertNotNull(direct(DXEndpoint.Role.STREAM_PUBLISHER, DXEndpoint.Role.STREAM_FEED, Trade.class,
            DELIVERY_SECONDS));
    }

    @Test
    public void timeAndSaleFromPublisherReachesStreamFeed() throws Exception {
        assertNotNull(direct(DXEndpoint.Role.PUBLISHER, DXEndpoint.Role.STREAM_FEED, TimeAndSale.class,
            DELIVERY_SECONDS));
    }

    @Test
    public void timeAndSaleFromStreamPublisherReachesStreamFeed() throws Exception {
        assertNotNull(direct(DXEndpoint.Role.STREAM_PUBLISHER, DXEndpoint.Role.STREAM_FEED, TimeAndSale.class,
            DELIVERY_SECONDS));
    }

    /** Publishes through the multiplexor and returns the nanosecond part of the first received event. */
    private static int throughMux(DXEndpoint.Role publisherRole, boolean publisherNanoTime, boolean muxNanoTime,
        DXEndpoint.Role consumerRole, boolean consumerNanoTime, Class<? extends MarketEvent> type) throws Exception
    {
        DXEndpoint publisher = endpoint(publisherRole, publisherNanoTime);
        DXEndpoint consumer = endpoint(consumerRole, consumerNanoTime);
        try {
            int publisherPort = listen(publisher);
            int consumerPort = listen(consumer);
            try (NanoTimeProbes.Mux mux = new NanoTimeProbes.Mux(publisherPort, consumerPort, muxNanoTime)) {
                MarketEvent event = publishUntilReceived(publisher, consumer, type, DELIVERY_SECONDS);
                assertNotNull("no " + type.getSimpleName() + " through the multiplexor within " + DELIVERY_SECONDS +
                    " s; its output:\n" + mux.output(), event);
                return nanoPart(event);
            }
        } finally {
            consumer.close();
            publisher.close();
        }
    }

    /** Publishes to a consumer that connects to the publisher; returns the first received event or null. */
    private static MarketEvent direct(DXEndpoint.Role publisherRole, DXEndpoint.Role consumerRole,
        Class<? extends MarketEvent> type, long waitSeconds) throws Exception
    {
        DXEndpoint publisher = endpoint(publisherRole, true);
        DXEndpoint consumer = endpoint(consumerRole, true);
        try {
            consumer.connect("127.0.0.1:" + listen(publisher));
            return publishUntilReceived(publisher, consumer, type, waitSeconds);
        } finally {
            consumer.close();
            publisher.close();
        }
    }
}
