// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.sdk.javac.NativeListener;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The event peek of a time series model ({@code dxfg_v4_TimeSeriesTxModel_Builder_withEventPeekFunction}): it passes
 * each event to the native function, unless the listener is deactivated
 * ({@code dxfg_v4_NativeListener_deactivateAll}) or the call comes inside {@link NativeListener#runMuted(Runnable)}
 * (a task of a closed event queue).
 *
 * <p>No handle refers to it, so only {@link NativeListener#deactivateAll()} deactivates it.
 *
 * @param <E> the type of the events.
 */
public final class NativeEventPeek<E> extends NativeListener implements Consumer<E> {

    private final Consumer<? super E> nativeCall;

    /**
     * @param nativeCall calls the native function with the event.
     */
    public NativeEventPeek(final Consumer<? super E> nativeCall) {
        this.nativeCall = Objects.requireNonNull(nativeCall, "nativeCall");
    }

    @Override
    public void accept(final E event) {
        if (!enter()) {
            return;
        }

        try {
            nativeCall.accept(event);
        } finally {
            exit();
        }
    }
}
