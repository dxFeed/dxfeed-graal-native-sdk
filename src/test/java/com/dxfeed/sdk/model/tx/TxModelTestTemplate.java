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
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/test/java/com/dxfeed/api/experimental/model/TxModelTestTemplate.java
package com.dxfeed.sdk.model.tx;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.event.IndexedEvent;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// ThreadCleanCheck.before()/after() of the original are removed: its test-jar (QD dxlib) is not public.
public abstract class TxModelTestTemplate<E extends IndexedEvent<?>, M extends AbstractTxModel<E>> {
    protected static final String SYMBOL = "TEST-SYMBOL";

    protected final List<E> publishedEvents = new ArrayList<>();
    protected final Queue<E> receivedEvents = new ArrayDeque<>();
    protected final Executor executor = Runnable::run;

    protected DXEndpoint endpoint;
    protected DXFeed feed;
    protected DXPublisher publisher;
    protected M model;

    protected int listenerNotificationCounter;
    protected int snapshotNotificationCounter;

    @Before
    public void setUp() {
        endpoint = DXEndpoint.create(DXEndpoint.Role.LOCAL_HUB);
        feed = endpoint.getFeed();
        publisher = endpoint.getPublisher();
    }

    @After
    public void tearDown() {
        snapshotNotificationCounter = 0;
        listenerNotificationCounter = 0;
        receivedEvents.clear();
        publishedEvents.clear();
        model.close();
        endpoint.close();
    }

    protected void assertIsChanged(boolean isChanged) {
        assertTrue(isChanged ? listenerNotificationCounter > 0 : listenerNotificationCounter == 0);
        listenerNotificationCounter = 0;
    }

    protected void assertIsSnapshot(boolean isSnapshot) {
        assertTrue(isSnapshot ? snapshotNotificationCounter > 0 : snapshotNotificationCounter == 0);
        snapshotNotificationCounter = 0;
    }

    protected void assertListenerNotification(int count) {
        assertEquals(count, listenerNotificationCounter);
        listenerNotificationCounter = 0;
    }

    protected void assertSnapshotNotification(int count) {
        assertEquals(count, snapshotNotificationCounter);
        snapshotNotificationCounter = 0;
    }

    protected void assertReceivedEventCount(int count) {
        assertEquals(count, receivedEvents.size());
    }

    protected void publishDeferred(boolean withPublisher) {
        if (withPublisher) {
            publisher.publishEvents(publishedEvents);
        } else {
            model.processEvents(publishedEvents);
        }
        publishedEvents.clear();
    }

    protected void publish(E event) {
        publisher.publishEvents(Collections.singletonList(event));
    }

    protected void publish(List<E> event) {
        publisher.publishEvents(event);
    }
}
