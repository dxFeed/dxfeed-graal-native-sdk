// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.dxfeed.sdk.javac.NativeListener;

/**
 * A listener of the state changes of the message connectors of an endpoint that calls a native function
 * ({@code dxfg_v4_MessageConnectorListener_new}). It is called on the thread of {@link ConnectorDispatcher}, never
 * on a QD thread, and not after it is deactivated (its last handle released, {@code deactivateAll}) or in a muted
 * scope. A deactivated listener is removed from its endpoints at their next state change.
 */
public abstract class ConnectorStateListener extends NativeListener {

    /**
     * Calls the native function with the connector whose state changed.
     *
     * @return {@code false} if the listener is deactivated, so that the endpoint drops it
     */
    final boolean connectorChanged(final MessageConnector connector) {
        if (!enter()) {
            return isActive();
        }

        try {
            callNative(connector);
        } finally {
            exit();
        }

        return true;
    }

    /**
     * Calls the native function of the listener.
     */
    protected abstract void callNative(MessageConnector connector);
}
