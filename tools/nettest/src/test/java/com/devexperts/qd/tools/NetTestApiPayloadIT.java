// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.socket.ServerSocketTestHelper;
import com.devexperts.qd.stats.QDStats;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import com.dxfeed.promise.Promise;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

/**
 * The bytes per record that the tool's event pools put on the wire (connector statistics: written bytes over written
 * data records) at batch 100, the reference value for equal payloads of the NetTests of all APIs. The value is
 * measured between the first and the third second, without the handshake and the record descriptions, and printed as
 * a {@code PAYLOAD} line.
 */
public class NetTestApiPayloadIT {
    @Test
    public void tradeWithoutNanoTime() throws Exception {
        measure(Trade.class, DXEndpoint.Role.FEED, false, "p", ":0", "-S", "100", "-r", "*/20k");
    }

    @Test
    public void tradeWithNanoTime() throws Exception {
        measure(Trade.class, DXEndpoint.Role.FEED, true, "p", ":0", "-S", "100", "-r", "*/20k");
    }

    @Test
    public void timeAndSaleWithoutNanoTime() throws Exception {
        measure(TimeAndSale.class, DXEndpoint.Role.STREAM_FEED, false, "p", ":0", "-S", "100", "-r", "*/20k",
            "--event", "TimeAndSale", "--role", "STREAM_PUBLISHER");
    }

    @Test
    public void timeAndSaleWithNanoTime() throws Exception {
        measure(TimeAndSale.class, DXEndpoint.Role.STREAM_FEED, true, "p", ":0", "-S", "100", "-r", "*/20k",
            "--event", "TimeAndSale", "--role", "STREAM_PUBLISHER");
    }

    // The first two arguments are "p" and ":0": the tool listens on a port that the system picks.
    @SuppressWarnings("deprecation")
    private static void measure(Class<? extends MarketEvent> type, DXEndpoint.Role consumerRole, boolean nanoTime,
        String... args) throws Exception
    {
        String connectorName = "NetTestApiPayloadIT-" + UUID.randomUUID();
        Promise<Integer> port = ServerSocketTestHelper.createPortPromise(connectorName);
        args[1] = ":0[name=" + connectorName + "]";
        NetTestApiProducerSide side = new NetTestApiProducerSide(NetTestApi.parseArguments(args),
            new NetTestApi.Stop()) {
            @Override
            DXEndpoint createEndpoint(int index) {
                DXEndpoint.Builder builder = DXEndpoint.newBuilder().withRole(settings.role);
                if (nanoTime) {
                    builder.withProperty("dxscheme.nanoTime", "true");
                }
                return builder.build();
            }
        };
        side.start();
        DXEndpoint consumer = NanoTimeProbes.endpoint(consumerRole, nanoTime);
        try {
            AtomicLong received = new AtomicLong();
            DXFeedSubscription<MarketEvent> subscription = consumer.getFeed().createSubscription(type);
            subscription.addEventListener(events -> received.addAndGet(events.size()));
            subscription.addSymbols(Arrays.asList(side.getSymbols()));
            consumer.connect("127.0.0.1:" + port.await(NanoTimeProbes.DELIVERY_SECONDS, TimeUnit.SECONDS));
            Thread.sleep(1_000);
            long[] first = written(side);
            Thread.sleep(2_000);
            long[] last = written(side);

            long records = last[1] - first[1];
            double bytesPerRecord = (double) (last[0] - first[0]) / records;
            System.out.printf(Locale.ROOT, "PAYLOAD %s nanoTime=%s: %.2f bytes per record over %d records%n",
                type.getSimpleName(), nanoTime, bytesPerRecord, records);
            assertTrue("written records " + records, records > 10_000);
            assertTrue("received " + received.get(), received.get() > 10_000);
            assertTrue("bytes per record " + bytesPerRecord, bytesPerRecord > 5 && bytesPerRecord < 100);
        } finally {
            consumer.close();
            assertNull(side.finish());
        }
    }

    /** The written bytes and the written data records of all connectors of the tool. */
    private static long[] written(NetTestApiProducerSide side) {
        long bytes = 0;
        long records = 0;
        for (DXEndpoint endpoint : side.getEndpoints()) {
            for (MessageConnector connector : NetTestApiProducerSide.qdEndpoint(endpoint).getConnectors()) {
                bytes += connector.retrieveCompleteEndpointStats().getWrittenBytes();
                QDStats stats = connector.getStats();
                records += stats == null ? 0 : stats.getValue(QDStats.SValue.IO_DATA_WRITE_RECORDS);
            }
        }
        return new long[] {bytes, records};
    }
}
