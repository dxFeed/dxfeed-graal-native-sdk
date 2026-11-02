// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feedfilter;

import com.dxfeed.api.DXFeedFilterListener;
import com.dxfeed.api.DXFeedFilterTracker;
import com.dxfeed.sdk.javac.NativeListener;

/**
 * A {@link DXFeedFilterListener} that calls a native function ({@code dxfg_v4_DXFeedFilterListener_new}): the function
 * gets no handles, and it is not called after the listener is deactivated (its last handle released,
 * {@code deactivateAll}) or in a muted scope. A deactivated listener removes itself from the tracker at the next update:
 * the caller has no handle left to remove it, and the last listener of a tracker keeps the filter updating (an
 * {@code ipf[...]} file is reloaded) until it is removed.
 */
public abstract class FeedFilterListener extends NativeListener implements DXFeedFilterListener {

    @Override
    public final void filterUpdated(final DXFeedFilterTracker tracker) {
        if (!enter()) {
            if (!isActive()) {
                tracker.removeListener(this);
            }
            return;
        }
        try {
            callNative();
        } finally {
            exit();
        }
    }

    /**
     * Calls the native function of the listener.
     */
    protected abstract void callNative();
}
