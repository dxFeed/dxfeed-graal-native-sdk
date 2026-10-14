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
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/test/java/com/dxfeed/api/experimental/model/AbstractTxModelTest.java
package com.dxfeed.sdk.model.tx;

import com.dxfeed.event.IndexedEvent;
import org.junit.Test;

import static com.dxfeed.event.IndexedEvent.REMOVE_EVENT;
import static com.dxfeed.event.IndexedEvent.SNAPSHOT_BEGIN;
import static com.dxfeed.event.IndexedEvent.SNAPSHOT_END;
import static com.dxfeed.event.IndexedEvent.SNAPSHOT_SNIP;
import static com.dxfeed.event.IndexedEvent.TX_PENDING;

@SuppressWarnings("unchecked")
abstract class AbstractTxModelTest<E extends IndexedEvent<?>, M extends AbstractTxModel<E>, B extends AbstractTxModel.Builder<E, B>>
    extends TxModelTestTemplate<E, M>
{
    @Test
    public void testSnapshotAndUpdate() {
        model = (M) builder().build();

        addToPublish(1, 1, SNAPSHOT_BEGIN);
        addToPublish(0, 2, SNAPSHOT_END);
        addToPublish(1, 3);
        addToPublish(3, 4);
        addToPublish(2, 5);
        publishDeferred(false);
        assertSnapshotNotification(1); // only one snapshot
        assertListenerNotification(model.isBatchProcessing() ? 2 : 4);
        assertReceivedEventCount(5);
        assertEvent(1, 1, model.isSnapshotProcessing() ? 0 : SNAPSHOT_BEGIN);
        assertEvent(0, 2, model.isSnapshotProcessing() ? 0 : SNAPSHOT_END);
        assertEvent(1, 3, 0); // event with index 1 is not merged
        assertEvent(3, 4, 0);
        assertEvent(2, 5, 0);
    }

    @Test
    public void testEmptySnapshot() {
        model = (M) builder().build();

        addToPublish(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END | REMOVE_EVENT);
        publishDeferred(false);
        assertSnapshotNotification(1); // only one snapshot
        if (model.isSnapshotProcessing()) {
            assertReceivedEventCount(0); // event with REMOVE_EVENT flag was removed inside the snapshot
        } else {
            assertReceivedEventCount(1); // event with REMOVE_EVENT flag is saved
            assertEvent(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END | REMOVE_EVENT);
        }
    }

    @Test
    public void testSnapshotWithPending() {
        model = (M) builder().build();

        addToPublish(1, 1, SNAPSHOT_BEGIN);
        publishDeferred(false);
        assertIsChanged(false); // not processed yet
        assertReceivedEventCount(0);

        addToPublish(0, 2, SNAPSHOT_END | TX_PENDING);
        publishDeferred(false);
        assertIsChanged(false); // not processed yet, because the pending flag is set

        addToPublish(1, 3); // event without pending
        publishDeferred(false);
        assertListenerNotification(1); // since the transaction ended here
        assertSnapshotNotification(1); // and it's all a one snapshot
        if (model.isSnapshotProcessing()) {
            assertReceivedEventCount(2); // the same indices within a snapshot were merged
            assertEvent(1, 3, 0);
            assertEvent(0, 2, 0);
        } else {
            assertReceivedEventCount(3);
            assertEvent(1, 1, SNAPSHOT_BEGIN);
            assertEvent(0, 2, SNAPSHOT_END | TX_PENDING);
            assertEvent(1, 3, 0);
        }
    }

    @Test
    public void testMultipleSnapshot() {
        model = (M) builder().build();

        addToPublish(1, 1, SNAPSHOT_BEGIN);
        publishDeferred(false);
        assertIsChanged(false); // not processed yet
        assertReceivedEventCount(0);

        addToPublish(0, 2, SNAPSHOT_END);
        addToPublish(2, 3, SNAPSHOT_BEGIN);
        publishDeferred(false);
        assertListenerNotification(1);
        assertSnapshotNotification(1); // only one snapshot so far, beginning of the second one is in the buffer
        assertReceivedEventCount(2);
        assertEvent(1, 1, model.isSnapshotProcessing() ? 0 : SNAPSHOT_BEGIN);
        assertEvent(0, 2, model.isSnapshotProcessing() ? 0 : SNAPSHOT_END);

        addToPublish(0, 4, SNAPSHOT_END); // end of second snapshot
        addToPublish(3, 5); // update after second snapshot
        publishDeferred(false);
        assertSnapshotNotification(1);
        assertListenerNotification(2);
        assertReceivedEventCount(3);
        assertEvent(2, 3, model.isSnapshotProcessing() ? 0 : SNAPSHOT_BEGIN);
        assertEvent(0, 4, model.isSnapshotProcessing() ? 0 : SNAPSHOT_END);
        assertEvent(3, 5, 0);
    }

    @Test
    public void testMultipleSnapshotInOneBatch() {
        model = (M) builder().build();

        addToPublish(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
        addToPublish(0, 2, SNAPSHOT_BEGIN | SNAPSHOT_SNIP);
        addToPublish(0, 3, SNAPSHOT_BEGIN | REMOVE_EVENT | SNAPSHOT_SNIP | SNAPSHOT_END);
        publishDeferred(false);
        assertListenerNotification(3);
        assertSnapshotNotification(3);
        if (model.isSnapshotProcessing()) {
            assertReceivedEventCount(2); // no event with REMOVE_EVENT flag
            assertEvent(0, 1, 0);
            assertEvent(0, 2, 0);
        } else {
            assertReceivedEventCount(3);
            assertEvent(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
            assertEvent(0, 2, SNAPSHOT_BEGIN | SNAPSHOT_SNIP);
            assertEvent(0, 3, SNAPSHOT_BEGIN | REMOVE_EVENT | SNAPSHOT_SNIP | SNAPSHOT_END);
        }
    }

    @Test
    public void testIncompleteSnapshot() {
        model = (M) builder().build();

        addToPublish(1, 1, SNAPSHOT_BEGIN);
        publishDeferred(false);
        assertIsChanged(false); // not processed yet
        assertReceivedEventCount(0);

        addToPublish(2, 2, SNAPSHOT_BEGIN); // yet another snapshot begins
        addToPublish(3, 3); // event part of a snapshot
        publishDeferred(false);
        assertIsChanged(false); // not processed yet
        assertReceivedEventCount(0);

        addToPublish(4, 4, SNAPSHOT_BEGIN); // start new snapshot
        publishDeferred(false);
        assertIsChanged(false);
        assertReceivedEventCount(0);

        addToPublish(0, 5, SNAPSHOT_END); // full snapshot
        addToPublish(5, 6); // update event after the snapshot end in the same batch
        addToPublish(6, 7); // yet another update event after the snapshot end in the same batch
        publishDeferred(false);
        assertListenerNotification(model.isBatchProcessing() ? 2 : 3);
        assertSnapshotNotification(1); // of which one snapshot
        assertReceivedEventCount(4); // chunks of previous snapshots have been deleted
        assertEvent(4, 4, model.isSnapshotProcessing() ? 0 : SNAPSHOT_BEGIN);
        assertEvent(0, 5, model.isSnapshotProcessing() ? 0 : SNAPSHOT_END);
        assertEvent(5, 6, 0);
        assertEvent(6, 7, 0);

        addToPublish(7, 4, SNAPSHOT_BEGIN); // the snapshot hasn't ended yet
        publishDeferred(false);
        assertIsChanged(false); // not processed yet
    }

    @Test
    public void testPending() {
        model = (M) builder().build();

        addToPublish(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
        publishDeferred(false);
        assertIsChanged(true);
        assertIsSnapshot(true);
        assertReceivedEventCount(1);

        addToPublish(1, 2, TX_PENDING); // publish pending event
        addToPublish(2, 3, TX_PENDING); // publish pending event, same index as the previous one
        publishDeferred(false);
        assertIsChanged(false); // not processed yet

        addToPublish(3, 4, 0); // publish without pending
        addToPublish(4, 5, 0); // publish without pending
        publishDeferred(false);
        assertListenerNotification(model.isBatchProcessing() ? 1 : 2);
        assertIsSnapshot(false);
        assertReceivedEventCount(5); // all published events, without merge
        assertEvent(0, 1, model.isSnapshotProcessing() ? 0 : SNAPSHOT_BEGIN | SNAPSHOT_END);
        assertEvent(1, 2, TX_PENDING);
        assertEvent(2, 3, TX_PENDING);
        assertEvent(3, 4, 0);
        assertEvent(4, 5, 0);
    }

    @Test
    public void testEventsWithoutSnapshot() {
        model = (M) builder().build();

        addToPublish(2, 1);
        addToPublish(3, 2);
        addToPublish(1, 3);
        addToPublish(1, 4); // same index as the previous one
        addToPublish(0, 5);
        publishDeferred(false);
        assertListenerNotification(model.isBatchProcessing() ? 1 : 5);
        assertReceivedEventCount(5); // events prior to the snapshot are transmitted as is
        assertEvent(2, 1, 0);
        assertEvent(3, 2, 0);
        assertEvent(1, 3, 0);
        assertEvent(1, 4, 0); // same index as the previous one, without merge
        assertEvent(0, 5, 0);

        addToPublish(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
        addToPublish(1, 2);
        publishDeferred(false);
        assertIsChanged(true);
        assertReceivedEventCount(2); // after receiving a snapshot, all events are received
        assertEvent(0, 1, model.isSnapshotProcessing() ? 0 : SNAPSHOT_BEGIN | SNAPSHOT_END);
        assertEvent(1, 2, 0);
    }

    @Test
    public void testSnapshotWithRemoveAndPending() {
        model = (M) builder().build();

        addToPublish(7, 1, SNAPSHOT_BEGIN);
        addToPublish(6, 2);
        addToPublish(5, 3, REMOVE_EVENT);
        addToPublish(4, 4);
        addToPublish(3, 5);
        addToPublish(2, 6, TX_PENDING);
        addToPublish(2, 7);
        addToPublish(1, 8);
        addToPublish(0, Double.NaN, SNAPSHOT_END | TX_PENDING | REMOVE_EVENT);
        addToPublish(1, 9);
        publishDeferred(false);
        if (model.isSnapshotProcessing()) {
            assertReceivedEventCount(6);
            assertEvent(7, 1, 0);
            assertEvent(6, 2, 0);
            assertEvent(4, 4, 0);
            assertEvent(3, 5, 0);
            assertEvent(2, 7, 0);
            assertEvent(1, 9, 0);
        } else {
            assertReceivedEventCount(10);
            assertEvent(7, 1, SNAPSHOT_BEGIN);
            assertEvent(6, 2, 0);
            assertEvent(5, 3, REMOVE_EVENT);
            assertEvent(4, 4, 0);
            assertEvent(3, 5, 0);
            assertEvent(2, 6, TX_PENDING);
            assertEvent(2, 7, 0);
            assertEvent(1, 8, 0);
            assertEvent(0, Double.NaN, SNAPSHOT_END | TX_PENDING | REMOVE_EVENT);
            assertEvent(1, 9, 0);
        }
    }

    @Test
    public void testAttachFeed() {
        model = (M) builder().withFeed(null).build(); // create a model instance without attaching it to a feed

        // add an event to be published, with SNAPSHOT_BEGIN and SNAPSHOT_END flags
        addToPublish(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
        publishDeferred(true); // publish the event
        assertIsChanged(false); // verify that there is no change since the model is not attached to the feed

        // attach the model to the feed and publish another event
        model.attach(feed);
        addToPublish(0, 1, SNAPSHOT_BEGIN | SNAPSHOT_END);
        publishDeferred(true); // publish the event
        assertIsChanged(true); // verify that the model registers the change
        assertReceivedEventCount(1); // verify that the event is received

        // detach the model from the feed and publish another event
        model.detach(feed);
        addToPublish(0, 2, SNAPSHOT_BEGIN | SNAPSHOT_END);
        publishDeferred(true); // publish the event
        assertIsChanged(false); // verify that there is no change since the model is detached

        // reattach the model to the feed and publish another event
        model.attach(feed);
        addToPublish(0, 3, SNAPSHOT_BEGIN | SNAPSHOT_END);
        publishDeferred(true); // publish the event
        assertIsChanged(true); // verify that the model registers the change
        assertReceivedEventCount(2); // verify that two events are now received
        assertEvent(0, 1, model.isSnapshotProcessing() ? 0 : SNAPSHOT_BEGIN | SNAPSHOT_END);
        assertEvent(0, 3, model.isSnapshotProcessing() ? 0 : SNAPSHOT_BEGIN | SNAPSHOT_END);
    }

    @Test
    public void testCloseAbruptly() {
        model = (M) builder().build();

        addToPublish(0, 12.34, SNAPSHOT_BEGIN | SNAPSHOT_END);
        publishDeferred(true);
        assertIsChanged(true);
        assertIsSnapshot(true);

        model.close();

        addToPublish(2, 56.78, 0); // emulate stale events processing
        publishDeferred(true);
        assertIsChanged(false); // no change after close
    }

    protected void addToPublish(int index, double size) {
        addToPublish(index, size, 0);
    }

    protected void addToPublish(int index, double size, int eventFlags) {
        E event = createEvent(index, size, eventFlags);
        publishedEvents.add(event);
    }

    protected abstract void assertEvent(int index, double size, int eventFlags);

    protected abstract E createEvent(int index, double size, int eventFlags);

    protected abstract B builder();
}
