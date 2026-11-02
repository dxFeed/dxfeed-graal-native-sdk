// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feedfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedFilter;
import com.dxfeed.api.DXFeedFilterListener;
import com.dxfeed.api.DXFeedFilterTracker;
import com.dxfeed.event.market.Quote;
import com.dxfeed.sdk.javac.NativeListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link FeedFilterListener} on the tracker of a dynamic filter: an {@code ipf[...]} expression with an update period,
 * whose file the test rewrites. The listener is called on the thread of the IPF updater; it can add and remove
 * listeners of its tracker from the call without a deadlock, and it is silent once deactivated.
 */
public class FeedFilterListenerTest {

    private static final long TIMEOUT_SECONDS = 20;
    // Longer than the update period: an update would have come.
    private static final long QUIET_MILLIS = 2500;

    private DXEndpoint endpoint;
    private Path ipfFile;

    private static final class CountingListener extends FeedFilterListener {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch called = new CountDownLatch(1);
        volatile Runnable action = () -> {
        };

        @Override
        protected void callNative() {
            calls.incrementAndGet();
            action.run();
            called.countDown();
        }
    }

    @Before
    public void createEndpointAndFile() throws IOException {
        endpoint = DXEndpoint.create(DXEndpoint.Role.LOCAL_HUB);
        Files.createDirectories(Path.of("target"));
        // A relative name: QD reads the expression with its own escaping, a Windows path would need it.
        ipfFile = Path.of("target", "feed-filter-" + System.nanoTime() + ".ipf");
        writeIpf("IBM");
    }

    @After
    public void closeEndpointAndDeleteFile() throws IOException {
        endpoint.close();
        Files.deleteIfExists(ipfFile);
    }

    @Test
    public void listenerSeesTheUpdateAndTheTrackerTheNewFilter() throws Exception {
        final DXFeedFilter filter = buildDynamicFilter();
        final DXFeedFilterTracker tracker = filter.getTracker();
        final CountingListener listener = new CountingListener();
        tracker.addListener(listener);

        assertTrue(filter.isDynamic());
        assertTrue(filter.accept(Quote.class, "IBM"));
        assertFalse(filter.accept(Quote.class, "MSFT"));

        writeIpf("MSFT");

        assertTrue(listener.called.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        final DXFeedFilter current = tracker.getCurrentFilter();
        assertNotSame(filter, current);
        assertSame(tracker, current.getTracker());
        assertEquals("ipf", current.getCategory(Quote.class, "MSFT"));
        assertFalse(current.accept(Quote.class, "IBM"));
        // The old version does not change.
        assertTrue(filter.accept(Quote.class, "IBM"));

        tracker.removeListener(listener);
        listener.deactivate();
    }

    @Test
    public void listenerAddsAndRemovesListenersFromItsCall() throws Exception {
        final DXFeedFilterTracker tracker = buildDynamicFilter().getTracker();
        final CountingListener first = new CountingListener();
        final CountingListener second = new CountingListener();
        // The first call replaces the listener with the second one, on the thread of the update.
        first.action = () -> {
            tracker.addListener(second);
            tracker.removeListener(first);
        };
        tracker.addListener(first);

        writeIpf("MSFT");
        assertTrue(first.called.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        writeIpf("AAPL");
        assertTrue(second.called.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(1, first.calls.get());

        tracker.removeListener(second);
        second.deactivate();
        first.deactivate();
    }

    @Test
    public void deactivatedListenerIsNotCalled() throws Exception {
        final DXFeedFilterTracker tracker = buildDynamicFilter().getTracker();
        final CountingListener silenced = new CountingListener();
        final CountingListener witness = new CountingListener();
        tracker.addListener(silenced);
        tracker.addListener(witness);
        silenced.deactivate();

        writeIpf("MSFT");

        // The listeners are called one after another on the same thread, the silenced one first.
        assertTrue(witness.called.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(0, silenced.calls.get());

        tracker.removeListener(silenced);
        tracker.removeListener(witness);
        witness.deactivate();
    }

    @Test
    public void deactivatedListenerRemovesItselfAtTheNextUpdate() {
        final RecordingTracker tracker = new RecordingTracker();
        final CountingListener listener = new CountingListener();

        // Muted: not called, still added.
        NativeListener.runMuted(() -> listener.filterUpdated(tracker));
        assertEquals(0, listener.calls.get());
        assertTrue(tracker.removed.isEmpty());

        listener.deactivate();
        listener.filterUpdated(tracker);

        assertEquals(0, listener.calls.get());
        assertEquals(List.of(listener), tracker.removed);
    }

    @Test
    public void staticFilterIgnoresListeners() throws Exception {
        final DXFeedFilter filter = DXFeedFilter.newBuilder(endpoint).withCategory("prefix", "A*").build();
        final CountingListener listener = new CountingListener();

        assertFalse(filter.isDynamic());
        filter.getTracker().addListener(listener);
        Thread.sleep(QUIET_MILLIS);

        assertEquals(0, listener.calls.get());
        assertSame(filter, filter.getTracker().getCurrentFilter());
        listener.deactivate();
    }

    // A tracker that records the removed listeners.
    private static final class RecordingTracker implements DXFeedFilterTracker {
        final List<DXFeedFilterListener> removed = new ArrayList<>();

        @Override
        public DXFeedFilter getCurrentFilter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addListener(final DXFeedFilterListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeListener(final DXFeedFilterListener listener) {
            removed.add(listener);
        }
    }

    private DXFeedFilter buildDynamicFilter() {
        final String expression = "ipf[" + ipfFile.toString().replace('\\', '/') + ",update=1s]";

        return DXFeedFilter.newBuilder(endpoint).withCategory("ipf", expression).build();
    }

    // Rewrites the file with one stock and moves its time forward: the updater compares the last modified times.
    private void writeIpf(final String symbol) throws IOException {
        final String text = "#STOCK::=TYPE,SYMBOL\nSTOCK," + symbol + "\n##COMPLETE\n";
        final long previous = Files.exists(ipfFile) ? Files.getLastModifiedTime(ipfFile).toMillis() : 0;
        Files.write(ipfFile, text.getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(ipfFile,
                FileTime.fromMillis(Math.max(System.currentTimeMillis(), previous + 2000)));
    }
}
