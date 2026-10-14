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
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/test/java/com/dxfeed/api/experimental/model/TimerSeriesTxModelTest.java (renamed)
package com.dxfeed.sdk.model.tx;

import com.dxfeed.event.market.TimeAndSale;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.dxfeed.event.IndexedEvent.REMOVE_EVENT;
import static com.dxfeed.event.IndexedEvent.SNAPSHOT_BEGIN;
import static com.dxfeed.event.IndexedEvent.SNAPSHOT_END;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class TimeSeriesTxModelTest {
    @RunWith(Parameterized.class)
    public static class CommonTest
        extends AbstractTxModelTest<TimeAndSale, TimeSeriesTxModel<TimeAndSale>, TimeSeriesTxModel.Builder<TimeAndSale>>
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
            TimeAndSale tns = receivedEvents.poll();
            assertNotNull(String.format("TimeAndSale in the %d index must not be null", index), tns);
            assertEquals(String.format("At index %d: Unexpected symbol", index), SYMBOL, tns.getEventSymbol());
            assertEquals(String.format("At index %d: Unexpected index", index), index, tns.getIndex());
            assertEquals(String.format("At index %d: Unexpected size", index), size, tns.getSizeAsDouble(), 0.0);
            assertEquals(String.format("At index %d: Unexpected flags", index), eventFlags, tns.getEventFlags());
        }

        @Override
        protected TimeAndSale createEvent(int index, double size, int eventFlags) {
            TimeAndSale tns = new TimeAndSale(SYMBOL);
            tns.setIndex(index);
            tns.setSizeAsDouble(size);
            tns.setEventFlags(eventFlags);
            return tns;
        }

        @Override
        protected TimeSeriesTxModel.Builder<TimeAndSale> builder() {
            return TimeSeriesTxModel.newBuilder(TimeAndSale.class)
                .withBatchProcessing(isBatchProcessing)
                .withSnapshotProcessing(isSnapshotProcessing)
                .withFeed(feed)
                .withSymbol(SYMBOL)
                .withFromTime(0)
                .withListener((source, events, isSnapshot) -> {
                    ++listenerNotificationCounter;
                    if (isSnapshot)
                        ++snapshotNotificationCounter;
                    receivedEvents.addAll(events);
                })
                .withExecutor(executor);
        }
    }

    public static class TimeSeriesTest extends TxModelTestTemplate<TimeAndSale, TimeSeriesTxModel<TimeAndSale>> {
        @Test
        public void testInitialState() {
            TimeSeriesTxModel.Builder<TimeAndSale> builder = TimeSeriesTxModel.newBuilder(TimeAndSale.class);
            assertThrows(IllegalStateException.class, builder::build); // if the symbol is not set

            model = builder.withSymbol(SYMBOL).build();
            assertTrue(model.isBatchProcessing());
            assertFalse(model.isSnapshotProcessing());
            assertEquals(Long.MAX_VALUE, model.getFromTime());
        }

        @Test
        public void testChangeFromTime() {
            model = builder()
                .withFromTime(Long.MAX_VALUE) // no subscribe
                .build();

            publish(createEvent(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END));
            assertIsChanged(false); // no changes, model is not subscribed

            model.setFromTime(0);
            publish(createEvent(0, 2, SNAPSHOT_BEGIN | SNAPSHOT_END));
            assertIsChanged(true);

            model.setFromTime(1000);
            publish(createEvent(1000, 3, 0));
            assertIsChanged(true);

            model.setFromTime(1000);
            publish(createEvent(0, 3, 0));
            assertIsChanged(false); // no changes, old 'from'

            model.setFromTime(Long.MAX_VALUE);
            publish(createEvent(0, 4, SNAPSHOT_BEGIN | SNAPSHOT_END));
            assertIsChanged(false); // no changes, model is not subscribed
        }

        @Test
        public void testSorting() {
            List<TimeAndSale> events = new ArrayList<>();
            events.add(createEvent(2, 1, SNAPSHOT_BEGIN));
            events.add(createEvent(1, 2, 0));
            events.add(createEvent(0, 3, REMOVE_EVENT | SNAPSHOT_END));

            model = builder()
                .withFromTime(0)
                .withSnapshotProcessing(true)
                .withSorting(TimeSeriesTxModel.SortOrder.ASCENDING)
                .build();
            publish(events);
            assertIsChanged(true);
            assertEvent(1, 2);
            assertEvent(2, 1);
            model.close();

            model = builder()
                .withFromTime(0)
                .withSnapshotProcessing(true)
                .withSorting(TimeSeriesTxModel.SortOrder.DESCENDING)
                .build();
            publish(events);
            assertIsChanged(true);
            assertEvent(2, 1);
            assertEvent(1, 2);
            model.close();

            model = builder()
                .withFromTime(0)
                .withSnapshotProcessing(true)
                .withSorting(TimeSeriesTxModel.SortOrder.NONE)
                .build();
            publish(events);
            assertIsChanged(true);
            assertEvent(2, 1);
            assertEvent(1, 2);
            model.close();
        }

        @Test
        public void testIgnoreRemoveEvents() {
            List<TimeAndSale> events = new ArrayList<>();
            events.add(createEvent(2, 1, SNAPSHOT_BEGIN));
            events.add(createEvent(1, 2, 0));
            events.add(createEvent(0, 3, REMOVE_EVENT | SNAPSHOT_END));

            model = builder()
                .withFromTime(0)
                .ignoreRemoveEvents(true)
                .withSnapshotProcessing(true)
                .build();
            publish(events);
            assertIsChanged(true);

            publish(createEvent(0, 0, REMOVE_EVENT));
            assertIsChanged(false);

            publish(createEvent(0, 0, 0));
            assertIsChanged(true);
        }

        @Test
        public void testIgnoreEventsFromPast() {
            List<TimeAndSale> events = new ArrayList<>();
            events.add(createEvent(2, 1, SNAPSHOT_BEGIN));
            events.add(createEvent(1, 2, 0));
            events.add(createEvent(0, 3, REMOVE_EVENT | SNAPSHOT_END));

            model = builder()
                .withFromTime(0)
                .ignoreEventsFromPast(true)
                .withSnapshotProcessing(true)
                .build();
            publish(events);
            assertIsChanged(true);

            publish(createEvent(0, 0, 0));
            assertIsChanged(false);

            publish(createEvent(1, 0, 0));
            assertIsChanged(false);

            publish(createEvent(2, 0, 0));
            assertIsChanged(true);

            publish(createEvent(3, 0, 0));
            assertIsChanged(true);
        }

        @Test
        public void testEventConsumerCalledAfterFiltering() {
            List<TimeAndSale> consumed = new CopyOnWriteArrayList<>();

            List<TimeAndSale> snapshot = new ArrayList<>();
            snapshot.add(createEvent(2, 1, SNAPSHOT_BEGIN));
            snapshot.add(createEvent(1, 2, 0));
            snapshot.add(createEvent(0, 3, REMOVE_EVENT | SNAPSHOT_END));

            // consumer + ignoreRemoveEvents: consumer must not see remove events
            model = builder()
                .withFromTime(0)
                .ignoreRemoveEvents(true)
                .withSnapshotProcessing(true)
                .withEventPeek(consumed::add)
                .build();

            publish(snapshot);
            assertIsChanged(true);
            // snapshot had one REMOVE_EVENT (time=0), consumer sees only 2 surviving events
            assertEquals("Consumer sees only non-remove snapshot events", 2, consumed.size());

            publish(createEvent(0, 0, REMOVE_EVENT));
            assertIsChanged(false);
            // update REMOVE_EVENT filtered out → consumer not called
            assertEquals("Consumer must not see filtered remove event", 2, consumed.size());

            publish(createEvent(3, 5, 0));
            assertIsChanged(true);
            assertEquals("Consumer called for normal update", 3, consumed.size());
        }

        @Test
        public void testEventConsumerCalledAfterPastFiltering() {
            List<TimeAndSale> consumed = new CopyOnWriteArrayList<>();

            List<TimeAndSale> snapshot = new ArrayList<>();
            snapshot.add(createEvent(2, 1, SNAPSHOT_BEGIN));
            snapshot.add(createEvent(1, 2, 0));
            snapshot.add(createEvent(0, 3, REMOVE_EVENT | SNAPSHOT_END));

            model = builder()
                .withFromTime(0)
                .ignoreEventsFromPast(true)
                .withSnapshotProcessing(true)
                .withEventPeek(consumed::add)
                .build();

            publish(snapshot);
            assertIsChanged(true);
            assertEquals("Consumer sees 2 snapshot events (remove filtered)", 2, consumed.size());

            // time=0 and time=1 are past (lastIndex=time2) → filtered → consumer not called
            publish(createEvent(0, 0, 0));
            assertIsChanged(false);
            assertEquals("Consumer must not see past event", 2, consumed.size());

            publish(createEvent(1, 0, 0));
            assertIsChanged(false);
            assertEquals("Consumer must not see past event", 2, consumed.size());

            publish(createEvent(3, 3, 0));
            assertIsChanged(true);
            assertEquals("Consumer called for future event", 3, consumed.size());
        }

        @Test
        public void testEventConsumerCalledForUpdates() {
            List<TimeAndSale> consumed = new CopyOnWriteArrayList<>();

            // establish snapshot first (using ignoreRemoveEvents for reliable snapshot delivery)
            model = builder()
                .withFromTime(0)
                .ignoreRemoveEvents(true)
                .withSnapshotProcessing(true)
                .withEventPeek(consumed::add)
                .build();

            List<TimeAndSale> snapshot = new ArrayList<>();
            snapshot.add(createEvent(2, 1, SNAPSHOT_BEGIN));
            snapshot.add(createEvent(1, 2, 0));
            snapshot.add(createEvent(0, 3, REMOVE_EVENT | SNAPSHOT_END));
            publish(snapshot);
            assertIsChanged(true);
            assertEquals("Consumer sees 2 non-remove snapshot events", 2, consumed.size());

            // normal update → consumer called
            publish(createEvent(3, 5, 0));
            assertIsChanged(true);
            assertEquals("Consumer called for normal update", 3, consumed.size());

            // another normal update → consumer called again
            publish(createEvent(4, 7, 0));
            assertIsChanged(true);
            assertEquals("Consumer called for each update", 4, consumed.size());
        }

        protected TimeAndSale createEvent(long time, double size, int eventFlags) {
            TimeAndSale tns = new TimeAndSale(SYMBOL);
            tns.setTime(time);
            tns.setSizeAsDouble(size);
            tns.setEventFlags(eventFlags);
            return tns;
        }

        private TimeSeriesTxModel.Builder<TimeAndSale> builder() {
            return TimeSeriesTxModel.newBuilder(TimeAndSale.class)
                .withFeed(feed)
                .withSymbol(SYMBOL)
                .withFromTime(0)
                .withListener((source, events, isSnapshot) -> {
                    ++listenerNotificationCounter;
                    if (isSnapshot)
                        ++snapshotNotificationCounter;
                    receivedEvents.addAll(events);
                })
                .withExecutor(executor);
        }

        private void assertEvent(int time, double size) {
            TimeAndSale tns = receivedEvents.poll();
            assertNotNull(tns);
            assertEquals(SYMBOL, tns.getEventSymbol());
            assertEquals(time, tns.getTime());
            assertEquals(size, tns.getSizeAsDouble(), 0.0);
        }
    }

    public static class ConfirmationTickTest
        extends TxModelTestTemplate<TimeAndSale, TimeSeriesTxModel<TimeAndSale>>
    {
        @Test
        public void testSnapshotDoesNotPrependConfirmation() {
            model = builder().build();

            addEvent(0, 1, SNAPSHOT_BEGIN);
            addEvent(1, 2, 0);
            addEvent(2, 3, SNAPSHOT_END);
            publishDeferred(false);

            assertIsChanged(true);
            assertIsSnapshot(true);
            assertReceivedEventCount(3);
            assertEvent(0, 1);
            assertEvent(1, 2);
            assertEvent(2, 3);
        }

        @Test
        public void testRealtimeUpdateSameBarNoPrepend() {
            model = builder().build();

            addEvent(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
            publishDeferred(false);
            receivedEvents.clear();
            listenerNotificationCounter = 0;

            addEvent(0, 2, 0);
            publishDeferred(false);
            assertIsChanged(true);
            assertReceivedEventCount(1);
            assertEvent(0, 2);
        }

        @Test
        public void testRealtimeNewBarPrependsClosingBar() {
            model = builder().build();

            addEvent(0, 10, SNAPSHOT_BEGIN | SNAPSHOT_END);
            publishDeferred(false);
            receivedEvents.clear();
            listenerNotificationCounter = 0;

            addEvent(1, 20, 0);
            publishDeferred(false);
            assertIsChanged(true);
            assertReceivedEventCount(2);
            assertEvent(0, 10);
            assertEvent(1, 20);
        }

        @Test
        public void testMultipleBarTransitions() {
            model = builder().build();

            addEvent(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
            publishDeferred(false);
            receivedEvents.clear();
            listenerNotificationCounter = 0;

            addEvent(1, 2, 0);
            publishDeferred(false);
            assertReceivedEventCount(2);
            assertEvent(0, 1);
            assertEvent(1, 2);
            receivedEvents.clear();
            listenerNotificationCounter = 0;

            addEvent(1, 3, 0);
            publishDeferred(false);
            assertReceivedEventCount(1);
            assertEvent(1, 3);
            receivedEvents.clear();
            listenerNotificationCounter = 0;

            addEvent(2, 4, 0);
            publishDeferred(false);
            assertReceivedEventCount(2);
            assertEvent(1, 3);
            assertEvent(2, 4);
        }

        @Test
        public void testSnapshotResetsStateThenRealtimeWorks() {
            model = builder().build();

            addEvent(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
            publishDeferred(false);
            receivedEvents.clear();
            listenerNotificationCounter = 0;
            snapshotNotificationCounter = 0;

            addEvent(1, 2, 0);
            publishDeferred(false);
            assertReceivedEventCount(2);
            assertEvent(0, 1);
            assertEvent(1, 2);
            receivedEvents.clear();
            listenerNotificationCounter = 0;
            snapshotNotificationCounter = 0;

            addEvent(5, 50, SNAPSHOT_BEGIN);
            addEvent(6, 60, SNAPSHOT_END);
            publishDeferred(false);
            assertIsSnapshot(true);
            receivedEvents.clear();
            listenerNotificationCounter = 0;
            snapshotNotificationCounter = 0;

            addEvent(7, 70, 0);
            publishDeferred(false);
            assertReceivedEventCount(2);
            assertEvent(6, 60);
            assertEvent(7, 70);
        }

        @Test
        public void testNoConfirmationWhenDisabled() {
            model = TimeSeriesTxModel.newBuilder(TimeAndSale.class)
                .withSymbol(SYMBOL)
                .withFromTime(0)
                .withConfirmationTick(false)
                .withListener((source, events, isSnapshot) -> {
                    ++listenerNotificationCounter;
                    if (isSnapshot)
                        ++snapshotNotificationCounter;
                    receivedEvents.addAll(events);
                })
                .withExecutor(executor)
                .build();

            addEvent(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
            publishDeferred(false);
            receivedEvents.clear();
            listenerNotificationCounter = 0;

            addEvent(1, 2, 0);
            publishDeferred(false);
            assertIsChanged(true);
            assertReceivedEventCount(1);
            assertEvent(1, 2);
        }

        @Test
        public void testConfirmationTickAutoEnablesSnapshotProcessing() {
            model = builder().build();
            assertTrue(model.isSnapshotProcessing());
        }

        private TimeSeriesTxModel.Builder<TimeAndSale> builder() {
            return TimeSeriesTxModel.newBuilder(TimeAndSale.class)
                .withSymbol(SYMBOL)
                .withFromTime(0)
                .withConfirmationTick(true)
                .withListener((source, events, isSnapshot) -> {
                    ++listenerNotificationCounter;
                    if (isSnapshot)
                        ++snapshotNotificationCounter;
                    receivedEvents.addAll(events);
                })
                .withExecutor(executor);
        }

        private void addEvent(int index, double size, int eventFlags) {
            TimeAndSale tns = new TimeAndSale(SYMBOL);
            tns.setIndex(index);
            tns.setSizeAsDouble(size);
            tns.setEventFlags(eventFlags);
            publishedEvents.add(tns);
        }

        private void assertEvent(int index, double size) {
            TimeAndSale tns = receivedEvents.poll();
            assertNotNull("Expected event at index " + index + " but queue was empty", tns);
            assertEquals(SYMBOL, tns.getEventSymbol());
            assertEquals(index, tns.getIndex());
            assertEquals(size, tns.getSizeAsDouble(), 0.0);
        }
    }
}
