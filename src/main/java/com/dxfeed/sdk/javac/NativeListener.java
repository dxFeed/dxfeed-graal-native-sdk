// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Base class of the Java objects that call a native (C) function with the user data
 * (the listeners created by the {@code dxfg_*Listener_new} functions).
 *
 * <p>A listener is deactivated when the last of its handles is released ({@code dxfg_JavaObjectHandler_release})
 * or explicitly ({@code dxfg_NativeListener_deactivate}). After the deactivation returns, the native function
 * is not called anymore and none of its calls is in progress (except the call on the deactivating thread itself,
 * if it deactivates the listener from its own callback), so the user data can be freed.
 * A deactivated listener that is still added somewhere stays there, but does nothing.
 *
 * <p>{@link #deactivateAll()} ({@code dxfg_v4_NativeListener_deactivateAll}) deactivates all the listeners of the
 * isolate (the static fields are per isolate), and the listeners created after it are inactive from the start.
 *
 * <p>Subclasses wrap every call of the native function:
 * <pre>{@code
 * if (enter()) {
 *     try {
 *         function.invoke(...);
 *     } finally {
 *         exit();
 *     }
 * }
 * }</pre>
 */
public abstract class NativeListener {

    private static final Object REGISTRY_LOCK = new Object();
    // Guarded by REGISTRY_LOCK. Weak: an unreachable listener is not called anymore.
    private static final Set<NativeListener> LISTENERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean allDeactivated;

    private final Object lock = new Object();
    // Guarded by lock.
    private boolean active = true;
    private int handles;
    private int calls;
    private final Map<Thread, Integer> callers = new HashMap<>(4); // the threads in the native function -> depth

    protected NativeListener() {
        synchronized (REGISTRY_LOCK) {
            if (allDeactivated) {
                stopCalls();
            } else {
                LISTENERS.add(this);
            }
        }
    }

    /**
     * Deactivates all the listeners of the isolate, the listeners created afterwards are inactive from the start.
     * Waits for the calls of the native functions in progress on the other threads, as {@link #deactivate()} does.
     */
    public static void deactivateAll() {
        final List<NativeListener> listeners;
        synchronized (REGISTRY_LOCK) {
            allDeactivated = true;
            // Not cleared: a concurrent call must wait for the same calls in progress.
            listeners = new ArrayList<>(LISTENERS);
        }
        // No new call starts on any of them while the calls in progress on one of them are waited for.
        for (final NativeListener listener : listeners) {
            listener.stopCalls();
        }
        for (final NativeListener listener : listeners) {
            listener.deactivate();
        }
    }

    private void stopCalls() {
        synchronized (lock) {
            active = false;
        }
    }

    /**
     * Starts a call of the native function.
     *
     * @return {@code false} if the listener is deactivated and the native function must not be called,
     *         otherwise {@code true} and {@link #exit()} must be called after the native function returns.
     */
    protected final boolean enter() {
        synchronized (lock) {
            if (!active) {
                return false;
            }
            calls++;
            callers.merge(Thread.currentThread(), 1, Integer::sum);
            return true;
        }
    }

    /**
     * Finishes the call of the native function started by {@link #enter()}.
     */
    protected final void exit() {
        synchronized (lock) {
            calls--;
            callers.computeIfPresent(Thread.currentThread(), (thread, depth) -> depth > 1 ? depth - 1 : null);
            if (!active) {
                lock.notifyAll();
            }
        }
    }

    /**
     * Deactivates the listener and waits for the calls of the native function in progress on the other threads.
     */
    public final void deactivate() {
        final Thread current = Thread.currentThread();
        boolean interrupted = false;
        synchronized (lock) {
            active = false;
            while (calls - callers.getOrDefault(current, 0) > 0) {
                try {
                    lock.wait();
                } catch (final InterruptedException e) {
                    interrupted = true; // the guarantee of deactivate() is more important, keep waiting
                }
            }
        }
        if (interrupted) {
            current.interrupt();
        }
    }

    /**
     * @return {@code true} if the listener is not deactivated.
     */
    public final boolean isActive() {
        synchronized (lock) {
            return active;
        }
    }

    // Called when a handle of this listener is created (see JavaObjectHandlerMapper).
    public final void handleCreated() {
        synchronized (lock) {
            handles++;
        }
    }

    // Called when a handle of this listener is released (see JavaObjectHandlerMapper).
    public final void handleReleased() {
        final boolean last;
        synchronized (lock) {
            last = --handles <= 0;
        }
        if (last) {
            deactivate();
        }
    }

    // Identity: the registry is a WeakHashMap, a subclass must not change how the listeners are compared.
    @Override
    public final boolean equals(final Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }
}
