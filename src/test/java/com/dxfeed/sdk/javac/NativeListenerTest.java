// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.After;
import org.junit.Test;

/**
 * {@link NativeListener#deactivateAll()}. It cannot be undone through the API, so every test undoes it by reflection
 * afterwards: the tests do not depend on each other, and the other tests of the JVM get active listeners.
 */
public class NativeListenerTest {

    private static final long TIMEOUT_SECONDS = 10;
    private static final int ITERATIONS = 200;
    private static final int CREATORS = 4;
    private static final int LISTENERS_PER_CREATOR = 2000;

    private static final class TestListener extends NativeListener {
        final AtomicInteger calls = new AtomicInteger();

        boolean call(final Runnable function) {
            if (!enter()) {
                return false;
            }
            try {
                calls.incrementAndGet();
                function.run();
            } finally {
                exit();
            }
            return true;
        }
    }

    // Also empties the registry: the deactivated listeners stay there until they are collected, and every
    // deactivateAll() would go through them again.
    @After
    public void undoDeactivateAll() throws ReflectiveOperationException {
        final Field lock = NativeListener.class.getDeclaredField("REGISTRY_LOCK");
        final Field listeners = NativeListener.class.getDeclaredField("LISTENERS");
        final Field allDeactivated = NativeListener.class.getDeclaredField("allDeactivated");
        lock.setAccessible(true);
        listeners.setAccessible(true);
        allDeactivated.setAccessible(true);
        synchronized (lock.get(null)) {
            ((Set<?>) listeners.get(null)).clear();
            allDeactivated.setBoolean(null, false);
        }
    }

    @Test
    public void deactivateAllWaitsForTheCallsAndDeactivatesTheListenersCreatedLater() throws InterruptedException {
        final TestListener blocked = new TestListener();
        final TestListener idle = new TestListener();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch released = new CountDownLatch(1);
        final AtomicBoolean ownCallReturned = new AtomicBoolean();

        // The call blocks, then calls deactivateAll() itself: that call must not wait for its own call.
        final Thread caller = start(() -> blocked.call(() -> {
            entered.countDown();
            await(released);
            NativeListener.deactivateAll();
            ownCallReturned.set(true);
        }));
        assertTrue(entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // deactivateAll() deactivates all the listeners first, then waits for the blocked call (the only Object.wait()).
        final Thread deactivating = start(NativeListener::deactivateAll);
        assertTrue("deactivateAll() waits for the call on the other thread", awaitCondition(() ->
            !blocked.isActive() && !idle.isActive() && deactivating.getState() == Thread.State.WAITING));
        assertFalse(idle.call(() -> {}));

        released.countDown();
        caller.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        deactivating.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        assertFalse(caller.isAlive());
        assertFalse(deactivating.isAlive());
        assertTrue(ownCallReturned.get());

        assertFalse(blocked.call(() -> {}));
        assertEquals(1, blocked.calls.get());
        assertEquals(0, idle.calls.get());

        final TestListener later = new TestListener();
        assertFalse(later.isActive());
        assertFalse(later.call(() -> {}));
        assertEquals(0, later.calls.get());
    }

    @Test
    public void deactivateAllDeactivatesTheListenersCreatedConcurrently() throws Exception {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            final AtomicBoolean stop = new AtomicBoolean();
            final AtomicInteger created = new AtomicInteger();
            final List<List<TestListener>> listeners = new ArrayList<>();
            final List<Thread> creators = new ArrayList<>();

            // The listeners are created before, during and after deactivateAll().
            for (int i = 0; i < CREATORS; i++) {
                final List<TestListener> own = new ArrayList<>();
                listeners.add(own);
                creators.add(start(() -> {
                    while (!stop.get() && own.size() < LISTENERS_PER_CREATOR) {
                        own.add(new TestListener());
                        created.incrementAndGet();
                    }
                }));
            }
            assertTrue(awaitCondition(() -> created.get() >= CREATORS * 100));
            NativeListener.deactivateAll();
            stop.set(true);
            for (final Thread creator : creators) {
                creator.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
                assertFalse(creator.isAlive());
            }

            for (final List<TestListener> own : listeners) {
                for (final TestListener listener : own) {
                    assertFalse("a listener is active after deactivateAll(), iteration " + iteration,
                        listener.isActive());
                }
            }
            undoDeactivateAll();
        }
    }

    private static Thread start(final Runnable task) {
        final Thread thread = new Thread(task, "NativeListenerTest");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static boolean awaitCondition(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() - deadline > 0) {
                return false;
            }
            Thread.sleep(1);
        }
        return true;
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
