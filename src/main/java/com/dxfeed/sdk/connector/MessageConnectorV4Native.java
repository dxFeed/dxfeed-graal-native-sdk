// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorState;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.endpoint.DxfgEndpoint;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.word.PointerBase;

/**
 * The functions of dxfg_v4_message_connector.h: the message connectors of an endpoint and the listeners of their
 * state changes (experimental).
 */
@CContext(MessageConnectorDirectives.class)
public final class MessageConnectorV4Native {

    // DXFG_V4_MESSAGE_CONNECTOR_STATE_*: the ordinals of MessageConnectorState.
    private static final int STATE_CONNECTING = 0;
    private static final int STATE_CONNECTED = 1;
    private static final int STATE_DISCONNECTED = 2;

    private MessageConnectorV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getConnectors",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getConnectors(
            final IsolateThread ignoredThread,
            final DxfgEndpoint endpoint,
            @DxfgOut final WordPointer connectors
    ) {
        checkPointer(connectors, "connectors");
        checkHandle(endpoint, "endpoint");

        connectors.write(NativeUtils.MAPPER_JAVA_OBJECT_HANDLERS.toNativeList(
                MessageConnectors.getConnectors(NativeUtils.MAPPER_ENDPOINT.toJava(endpoint))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_message_connector_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_message_connector_list_release(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorList connectors
    ) {
        if (connectors.isNonNull()) {
            NativeUtils.MAPPER_JAVA_OBJECT_HANDLERS.release(connectors);
        }

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_getName",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_getName(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final CCharPointerPointer name
    ) {
        checkPointer(name, "name");

        name.write(NativeUtils.MAPPER_STRING.toNative(toJava(connector).getName()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_getKind",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_getKind(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final CIntPointer kind
    ) {
        checkPointer(kind, "kind");

        kind.write(MessageConnectors.getKind(toJava(connector)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_getAddress",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_getAddress(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final CCharPointerPointer address
    ) {
        checkPointer(address, "address");

        address.write(NativeUtils.MAPPER_STRING.toNative(MessageConnectors.getAddress(toJava(connector))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_getCurrentAddresses",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_getCurrentAddresses(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final WordPointer addresses
    ) {
        checkPointer(addresses, "addresses");

        addresses.write(NativeUtils.MAPPER_STRINGS.toNativeList(MessageConnectors.getCurrentAddresses(
                toJava(connector))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_getState",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_getState(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final CIntPointer state
    ) {
        checkPointer(state, "state");

        state.write(toNativeState(toJava(connector).getState()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_isActive",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_isActive(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final CIntPointer active
    ) {
        checkPointer(active, "active");

        active.write(toJava(connector).isActive() ? 1 : 0);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_getConnectionCount",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_getConnectionCount(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final CIntPointer count
    ) {
        checkPointer(count, "count");

        count.write(toJava(connector).getConnectionCount());

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_getClosedConnectionCount",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_getClosedConnectionCount(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final CLongPointer count
    ) {
        checkPointer(count, "count");

        count.write(toJava(connector).getClosedConnectionCount());

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnector_getStatistics",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnector_getStatistics(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorHandle connector,
            @DxfgOut final DxfgMessageConnectorStats stats
    ) {
        checkPointer(stats, "stats");

        final MessageConnectorStatistics statistics = MessageConnectors.getStatistics(toJava(connector));

        stats.setReadBytes(statistics.getReadBytes());
        stats.setWrittenBytes(statistics.getWrittenBytes());
        stats.setReadDataRecords(statistics.getReadDataRecords());
        stats.setWrittenDataRecords(statistics.getWrittenDataRecords());
        stats.setActiveConnectionCount(statistics.getActiveConnectionCount());
        stats.setClosedConnectionCount(statistics.getClosedConnectionCount());

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_MessageConnectorListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_MessageConnectorListener_new(
            final IsolateThread ignoredThread,
            final DxfgMessageConnectorListenerFunction userFunc,
            final VoidPointer userData,
            @DxfgOut final WordPointer listener
    ) {
        checkPointer(listener, "listener");

        if (userFunc.isNull()) {
            throw new IllegalArgumentException("The `user_func` function is null");
        }

        final class Listener extends ConnectorStateListener {
            @Override
            protected void callNative(final MessageConnector connector) {
                // The handle is valid during the call only: the function clones it to keep it.
                final DxfgMessageConnectorHandle handle = NativeUtils.MAPPER_MESSAGE_CONNECTOR.toNative(connector);

                try {
                    userFunc.invoke(CurrentIsolate.getCurrentThread(), handle, userData);
                } finally {
                    NativeUtils.MAPPER_MESSAGE_CONNECTOR.release(handle);
                }
            }
        }

        listener.write(NativeUtils.MAPPER_MESSAGE_CONNECTOR_LISTENER.toNative(new Listener()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_addMessageConnectorListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_addMessageConnectorListener(
            final IsolateThread ignoredThread,
            final DxfgEndpoint endpoint,
            final DxfgMessageConnectorListenerHandle listener
    ) {
        checkHandle(endpoint, "endpoint");
        checkHandle(listener, "listener");

        ConnectorMonitor.addListener(NativeUtils.MAPPER_ENDPOINT.toJava(endpoint),
                NativeUtils.MAPPER_MESSAGE_CONNECTOR_LISTENER.toJava(listener));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_removeMessageConnectorListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_removeMessageConnectorListener(
            final IsolateThread ignoredThread,
            final DxfgEndpoint endpoint,
            final DxfgMessageConnectorListenerHandle listener
    ) {
        checkHandle(endpoint, "endpoint");
        checkHandle(listener, "listener");

        ConnectorMonitor.removeListener(NativeUtils.MAPPER_ENDPOINT.toJava(endpoint),
                NativeUtils.MAPPER_MESSAGE_CONNECTOR_LISTENER.toJava(listener));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    static int toNativeState(final MessageConnectorState state) {
        return switch (state) {
            case CONNECTING -> STATE_CONNECTING;
            case CONNECTED -> STATE_CONNECTED;
            case DISCONNECTED -> STATE_DISCONNECTED;
        };
    }

    private static MessageConnector toJava(final DxfgMessageConnectorHandle connector) {
        checkHandle(connector, "connector");

        return NativeUtils.MAPPER_MESSAGE_CONNECTOR.toJava(connector);
    }

    private static void checkPointer(final PointerBase pointer, final String name) {
        if (pointer.isNull()) {
            throw new IllegalArgumentException("The `" + name + "` pointer is null");
        }
    }

    private static void checkHandle(final PointerBase handle, final String name) {
        if (handle.isNull()) {
            throw new IllegalArgumentException("The `" + name + "` handle is null");
        }
    }
}
