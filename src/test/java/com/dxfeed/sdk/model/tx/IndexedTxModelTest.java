/*
 * !++
 * QDS - Quick Data Signalling Library
 * !-
 * Copyright (C) 2002 - 2024 Devexperts LLC
 * !-
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 * !__
 */
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/test/java/com/dxfeed/api/experimental/model/IndexedTxModelTest.java
package com.dxfeed.sdk.model.tx;

import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.market.Order;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.event.market.Side;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.dxfeed.event.IndexedEvent.SNAPSHOT_BEGIN;
import static com.dxfeed.event.IndexedEvent.SNAPSHOT_END;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class IndexedTxModelTest {
    @RunWith(Parameterized.class)
    public static class CommonTest
        extends AbstractTxModelTest<Order, IndexedTxModel<Order>, IndexedTxModel.Builder<Order>>
    {
        @Parameterized.Parameter()
        public boolean isBatchProcessing;
        @Parameterized.Parameter(1)
        public boolean isSnapshotProcessing;

        @Parameterized.Parameters(name = "{index} - isBatchProcessing:{0}; isSnapshotProcessing:{1};")
        public static Iterable<Object[]> params() {
            return IntStream.range(0, 4)
                .mapToObj(i -> new Object[]{(i & 1) != 0, (i & 2) != 0})
                .collect(Collectors.toList());
        }

        @Override
        protected void assertEvent(int index, double size, int eventFlags) {
            Order order = receivedEvents.poll();
            assertNotNull(String.format("Order in the %d index must not be null", index), order);
            assertEquals(String.format("At index %d: Unexpected symbol", index), SYMBOL, order.getEventSymbol());
            assertEquals(String.format("At index %d: Unexpected index", index), index, order.getIndex());
            assertEquals(String.format("At index %d: Unexpected size", index), size, order.getSizeAsDouble(), 0.0);
            assertEquals(String.format("At index %d: Unexpected flags", index), eventFlags, order.getEventFlags());
        }

        @Override
        protected Order createEvent(int index, double size, int eventFlags) {
            Order order = new Order(SYMBOL);
            order.setIndex(index);
            order.setSource(OrderSource.DEFAULT);
            order.setSizeAsDouble(size);
            order.setEventFlags(eventFlags);
            order.setOrderSide(Side.BUY);
            return order;
        }

        @Override
        protected IndexedTxModel.Builder<Order> builder() {
            return IndexedTxModel.newBuilder(Order.class)
                .withBatchProcessing(isBatchProcessing)
                .withSnapshotProcessing(isSnapshotProcessing)
                .withFeed(feed)
                .withSymbol(SYMBOL)
                .withListener((source, events, isSnapshot) -> {
                    ++listenerNotificationCounter;
                    if (isSnapshot)
                        ++snapshotNotificationCounter;
                    receivedEvents.addAll(events);
                })
                .withExecutor(executor);
        }
    }

    public static class IndexedTest extends TxModelTestTemplate<Order, IndexedTxModel<Order>> {
        @Test
        public void testInitialState() {
            IndexedTxModel.Builder<Order> builder = IndexedTxModel.newBuilder(Order.class);
            assertThrows(IllegalStateException.class, builder::build); // if the symbol is not set

            model = builder.withSymbol(SYMBOL).build();
            assertTrue(model.isBatchProcessing());
            assertFalse(model.isSnapshotProcessing());
            assertTrue(model.getSources().isEmpty());
        }

        @Test
        public void testChangeSource() {
            Order DEX = createOrder(OrderSource.DEX, 1);
            Order ntv = createOrder(OrderSource.ntv, 2);
            Order NTV = createOrder(OrderSource.NTV, 3);

            model = builder()
                .withSources(OrderSource.AGGREGATE_ASK, OrderSource.AGGREGATE_BID) // add two sources
                .withSources(Arrays.asList(OrderSource.ntv, OrderSource.NTV)) // override previous sources
                .build();
            Set<IndexedEventSource> sources = model.getSources();
            assertEquals(2, sources.size());
            assertTrue(sources.containsAll(Arrays.asList(OrderSource.ntv, OrderSource.NTV)));

            publish(DEX); // publish an unsubscribed source
            assertIsChanged(false);

            publish(ntv, NTV, DEX); // publish two subscribed and unsubscribed sources
            assertIsChanged(true);
            assertSnapshotNotification(2);
            assertReceivedEventCount(2);

            model.setSources(OrderSource.DEX); // change source
            sources = model.getSources();
            assertEquals(1, sources.size());
            assertTrue(sources.contains(OrderSource.DEX));

            publish(DEX); // publish a subscribed source
            assertIsChanged(true);
            assertSnapshotNotification(1);
            assertReceivedEventCount(3);
        }

        @Test
        public void testEmptySource() {
            Order DEX = createOrder(OrderSource.DEX, 1);
            Order ntv = createOrder(OrderSource.ntv, 2);
            Order NTV = createOrder(OrderSource.NTV, 3);

            model = builder().build();
            Set<IndexedEventSource> sources = model.getSources();
            assertTrue(sources.isEmpty());

            publish(ntv, NTV, DEX); // empty sources means subscribing to all available sources
            assertIsChanged(true);
            assertSnapshotNotification(3);
            assertReceivedEventCount(3);
        }

        protected void publish(Order... order) {
            publisher.publishEvents(Arrays.asList(order));
        }

        protected Order createOrder(OrderSource source, double size) {
            Order order = new Order(SYMBOL);
            order.setIndex(0);
            order.setSource(source);
            order.setSizeAsDouble(size);
            order.setEventFlags(SNAPSHOT_BEGIN | SNAPSHOT_END);
            order.setOrderSide(Side.BUY);
            return order;
        }

        protected IndexedTxModel.Builder<Order> builder() {
            return IndexedTxModel.newBuilder(Order.class)
                .withFeed(feed)
                .withSymbol(SYMBOL)
                .withListener((source, events, isSnapshot) -> {
                    ++listenerNotificationCounter;
                    if (isSnapshot)
                        ++snapshotNotificationCounter;
                    receivedEvents.addAll(events);
                })
                .withExecutor(executor);
        }
    }
}
