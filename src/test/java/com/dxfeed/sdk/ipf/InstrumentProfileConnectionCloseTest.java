// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dxfeed.ipf.live.InstrumentProfileCollector;
import com.dxfeed.ipf.live.InstrumentProfileConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * The close of a live instrument profile connection (MDAPI-479, fixed in QDS 3.357 by QD-1802): the handler thread
 * ends, the connection to the server is closed and the collector gets nothing from the stream after {@code close()}.
 * With QDS 3.356 the thread stayed blocked in the socket read, so a non-daemon thread kept the JVM and the isolate
 * tear-down waiting.
 */
public class InstrumentProfileConnectionCloseTest {

    private static final long TIMEOUT_MILLIS = 10_000;

    @Test
    public void closeOfALiveConnectionEndsItsThreadAndItsConnection() throws Exception {
        try (IpfLiveServer server = new IpfLiveServer(200)) {
            final InstrumentProfileCollector collector = new InstrumentProfileCollector();
            final InstrumentProfileConnection connection =
                    InstrumentProfileConnection.createConnection(server.address(), collector);
            final CountDownLatch snapshotDelivered = new CountDownLatch(1);
            final AtomicBoolean closed = new AtomicBoolean();
            final AtomicInteger callsAfterClose = new AtomicInteger();
            final AtomicBoolean bbbReceived = new AtomicBoolean();

            // The listener reads the whole iterator: the collector calls a listener again while its iterator has
            // entries left, so an unread entry would come once more, possibly after close().
            collector.addUpdateListener(instruments -> {
                if (closed.get()) {
                    callsAfterClose.incrementAndGet();
                }

                instruments.forEachRemaining(profile -> {
                    if ("BBB".equals(profile.getSymbol())) {
                        bbbReceived.set(true);
                    }
                });
                snapshotDelivered.countDown();
            });

            connection.start();
            assertTrue(connection.waitUntilCompleted(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            // The collector notifies its listeners on its executor: wait for the snapshot before close().
            assertTrue(snapshotDelivered.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));

            final Thread handler = findThread("IPC:" + server.address());

            closed.set(true);
            connection.close();
            assertEquals(InstrumentProfileConnection.State.CLOSED, connection.getState());

            // Data that reaches the closed connection is not passed to the collector.
            server.send("BBB");

            assertTrue("the connection to the server stays open", server.awaitClientClosed(TIMEOUT_MILLIS));
            handler.join(TIMEOUT_MILLIS);
            assertFalse("the handler thread is alive", handler.isAlive());

            // A barrier: the collector notifies its listeners in order on one thread, and a new listener of a
            // non-empty collector is called once, after every notification queued before it.
            final CountDownLatch barrier = new CountDownLatch(1);

            collector.addUpdateListener(instruments -> {
                instruments.forEachRemaining(profile -> {
                    if ("BBB".equals(profile.getSymbol())) {
                        bbbReceived.set(true);
                    }
                });
                barrier.countDown();
            });
            assertTrue(barrier.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            assertEquals(0, callsAfterClose.get());
            assertFalse("BBB reached the collector after close()", bbbReceived.get());
        }
    }

    private static Thread findThread(final String name) {
        Thread found = null;

        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().equals(name)) {
                found = thread;
            }
        }

        assertNotNull("no thread " + name, found);

        return found;
    }
}
