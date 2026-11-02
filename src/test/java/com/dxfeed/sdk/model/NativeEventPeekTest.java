// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import static org.junit.Assert.assertEquals;

import com.dxfeed.sdk.javac.NativeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

/**
 * {@link NativeEventPeek}: the native call is skipped in the muted scope and after
 * {@link NativeListener#deactivateAll()}.
 */
public class NativeEventPeekTest {

    // deactivateAll() cannot be undone through the API: undone by reflection, as in NativeListenerTest.
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
    public void passesEachEventToTheNativeCall() {
        final List<String> calls = new ArrayList<>();
        final NativeEventPeek<String> eventPeek = new NativeEventPeek<>(calls::add);

        List.of("a", "b").forEach(eventPeek);

        assertEquals(List.of("a", "b"), calls);
    }

    @Test
    public void skipsTheNativeCallInTheMutedScope() {
        final List<String> calls = new ArrayList<>();
        final NativeEventPeek<String> eventPeek = new NativeEventPeek<>(calls::add);

        NativeListener.runMuted(() -> eventPeek.accept("muted"));
        eventPeek.accept("after");

        assertEquals(List.of("after"), calls);
    }

    @Test
    public void skipsTheNativeCallAfterDeactivateAll() {
        final List<String> calls = new ArrayList<>();
        final NativeEventPeek<String> eventPeek = new NativeEventPeek<>(calls::add);

        NativeListener.deactivateAll();
        eventPeek.accept("deactivated");

        assertEquals(List.of(), calls);
    }
}
