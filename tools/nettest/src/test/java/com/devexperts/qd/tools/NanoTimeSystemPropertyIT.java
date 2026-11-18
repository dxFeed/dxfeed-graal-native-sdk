// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static com.devexperts.qd.tools.NanoTimeProbes.DELIVERY_SECONDS;
import static com.devexperts.qd.tools.NanoTimeProbes.endpoint;
import static com.devexperts.qd.tools.NanoTimeProbes.listen;
import static com.devexperts.qd.tools.NanoTimeProbes.NANO_PART;
import static com.devexperts.qd.tools.NanoTimeProbes.publishUntilReceived;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import org.junit.Test;

/**
 * The system property {@code -Ddxscheme.nanoTime=true} alone, without the builder property, in a JVM of its own (the
 * surefire execution {@code nano-time-system-property} of pom.xml sets it). Only the endpoints of the default scheme,
 * PUBLISHER and FEED, take it: an event that passes a STREAM_PUBLISHER or a STREAM_FEED endpoint arrives with the
 * nanosecond part 0, so a NetTest sets the property through the builder of every endpoint.
 */
public class NanoTimeSystemPropertyIT {
    @Test
    public void theJvmHasTheSystemProperty() {
        assertEquals("true", System.getProperty("dxscheme.nanoTime"));
    }

    @Test
    public void publisherToFeed() throws Exception {
        assertEquals(NANO_PART, nanoPart(DXEndpoint.Role.PUBLISHER, DXEndpoint.Role.FEED, Trade.class));
    }

    @Test
    public void publisherToStreamFeed() throws Exception {
        assertEquals(0, nanoPart(DXEndpoint.Role.PUBLISHER, DXEndpoint.Role.STREAM_FEED, Trade.class));
    }

    @Test
    public void streamPublisherToFeed() throws Exception {
        assertEquals(0, nanoPart(DXEndpoint.Role.STREAM_PUBLISHER, DXEndpoint.Role.FEED, TimeAndSale.class));
    }

    @Test
    public void streamPublisherToStreamFeed() throws Exception {
        assertEquals(0, nanoPart(DXEndpoint.Role.STREAM_PUBLISHER, DXEndpoint.Role.STREAM_FEED, TimeAndSale.class));
    }

    /** Publishes to a consumer that connects to the publisher and returns the nanosecond part of the first event. */
    private static int nanoPart(DXEndpoint.Role publisherRole, DXEndpoint.Role consumerRole,
        Class<? extends MarketEvent> type) throws Exception
    {
        DXEndpoint publisher = endpoint(publisherRole, false);
        DXEndpoint consumer = endpoint(consumerRole, false);
        try {
            consumer.connect("127.0.0.1:" + listen(publisher));
            MarketEvent event = publishUntilReceived(publisher, consumer, type, DELIVERY_SECONDS);
            assertNotNull("no " + type.getSimpleName() + " within " + DELIVERY_SECONDS + " s", event);
            return NanoTimeProbes.nanoPart(event);
        } finally {
            consumer.close();
            publisher.close();
        }
    }
}
