// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api;

import com.oracle.svm.core.c.CTypedef;
import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import static com.dxfeed.api.ErrorNative.ErrorCodes;

import java.beans.PropertyChangeListener;
import java.util.*;

@CContext(EndpointNative.NativeDirectives.class)
public final class EndpointNative {
    public static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/api/dxfg_endpoint.h"));
        }
    }

    private static final HashMap<Long, DXEndpoint> ENDPOINT_MAP = new HashMap<>();
    private static final HashMap<Long, PropertyChangeListener> STATE_CHANGE_LISTENER_MAP = new HashMap<>();

    private EndpointNative() {
        throw new IllegalStateException("Native class");
    }

    @CEnum("dxfg_endpoint_state_t")
    public enum EndpointStateNative {
        DXFG_ENDPOINT_STATE_NOT_CONNECTED,
        DXFG_ENDPOINT_STATE_CONNECTING,
        DXFG_ENDPOINT_STATE_CONNECTED,
        DXFG_ENDPOINT_STATE_CLOSED,
        DXFG_ENDPOINT_STATE_UNKNOWN;

        public static EndpointStateNative fromDxEndpointState(DXEndpoint.State state) {
            switch (state) {
                case NOT_CONNECTED:
                    return EndpointStateNative.DXFG_ENDPOINT_STATE_NOT_CONNECTED;
                case CONNECTING:
                    return EndpointStateNative.DXFG_ENDPOINT_STATE_CONNECTING;
                case CONNECTED:
                    return EndpointStateNative.DXFG_ENDPOINT_STATE_CONNECTED;
                case CLOSED:
                    return EndpointStateNative.DXFG_ENDPOINT_STATE_CLOSED;
                default:
                    return EndpointStateNative.DXFG_ENDPOINT_STATE_UNKNOWN;
            }
        }

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native EndpointStateNative fromCValue(int value);
    }

    @CTypedef(name = "dxfg_endpoint_on_change_state")
    interface StateChangeListenerPtr extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(EndpointStateNative oldState, EndpointStateNative newState);
    }

    @CEntryPoint(name = "dxfg_endpoint_create")
    private static ErrorCodes create(IsolateThread ignoredThread, CLongPointer endpointDescriptor) {
        if (endpointDescriptor.isNull()) {
            return ErrorCodes.DXFG_EC_NULL_POINTER_EX;
        }
        try {
            var endpoint = DXEndpoint.newBuilder()
                    .withRole(DXEndpoint.Role.FEED)
                    .withProperties(System.getProperties())
                    .build();
            endpointDescriptor.write(putEndpointToMap(endpoint));
            return ErrorCodes.DXFG_EC_SUCCESS;
        } catch (IllegalStateException e) {
            return ErrorCodes.DXFG_EC_ILLEGAL_STATE_EX;
        } catch (Exception e) {
            return ErrorCodes.DXFG_EC_UNKNOWN_ERR;
        }
    }

    @CEntryPoint(name = "dxfg_endpoint_close")
    private static ErrorCodes close(IsolateThread ignoredThread, long endpointDescriptor) {
        var endpoint = removeEndpointFromMap(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        endpoint.close();
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_endpoint_close_and_await_termination")
    private static ErrorCodes closeAndAwaitTermination(IsolateThread ignoredThread, long endpointDescriptor) {
        var endpoint = removeEndpointFromMap(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        try {
            endpoint.awaitProcessed();
            endpoint.closeAndAwaitTermination();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ErrorCodes.DXFG_EC_INTERRUPTED_EX;
        } catch (Exception e) {
            return ErrorCodes.DXFG_EC_UNKNOWN_ERR;
        }
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_endpoint_connect")
    private static ErrorCodes connect(IsolateThread ignoredThread, long endpointDescriptor, CCharPointer address) {
        if (address.isNull()) {
            return ErrorCodes.DXFG_EC_NULL_POINTER_EX;
        }
        var endpoint = getEndpointByDescriptor(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        try {
            endpoint.connect(CTypeConversion.toJavaString(address));
            return ErrorCodes.DXFG_EC_SUCCESS;
        } catch (Exception e) {
            return ErrorCodes.DXFG_EC_UNKNOWN_ERR;
        }
    }

    @CEntryPoint(name = "dxfg_endpoint_reconnect")
    private static ErrorCodes reconnect(IsolateThread ignoredThread, long endpointDescriptor) {
        var endpoint = getEndpointByDescriptor(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        endpoint.reconnect();
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_endpoint_disconnect")
    private static ErrorCodes disconnect(IsolateThread ignoredThread, long endpointDescriptor) {
        var endpoint = getEndpointByDescriptor(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        endpoint.disconnect();
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_endpoint_await_not_connected")
    private static ErrorCodes awaitNotConnected(IsolateThread ignoredThread, long endpointDescriptor) {
        var endpoint = getEndpointByDescriptor(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        try {
            endpoint.awaitNotConnected();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ErrorCodes.DXFG_EC_INTERRUPTED_EX;
        } catch (Exception e) {
            return ErrorCodes.DXFG_EC_UNKNOWN_ERR;
        }
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_endpoint_disconnect_and_clear")
    private static ErrorCodes disconnectAndClear(IsolateThread ignoredThread, long endpointDescriptor) {
        var endpoint = getEndpointByDescriptor(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        endpoint.disconnectAndClear();
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_endpoint_get_state")
    private static EndpointStateNative getState(IsolateThread ignoredThread, long endpointDescriptor) {
        var endpoint = getEndpointByDescriptor(endpointDescriptor);
        if (endpoint == null) {
            return EndpointStateNative.DXFG_ENDPOINT_STATE_UNKNOWN;
        }
        return EndpointStateNative.fromDxEndpointState(endpoint.getState());
    }

    @CEntryPoint(name = "dxfg_endpoint_add_state_change_listener")
    private static ErrorCodes addStateChangeListener(IsolateThread ignoredThread,
                                                     long endpointDescriptor, StateChangeListenerPtr listenerPtr) {
        if (listenerPtr.isNull()) {
            return ErrorCodes.DXFG_EC_NULL_POINTER_EX;
        }
        var endpoint = getEndpointByDescriptor(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        if (!STATE_CHANGE_LISTENER_MAP.containsKey(listenerPtr.rawValue())) {
            PropertyChangeListener listener = e -> listenerPtr.invoke(
                    EndpointStateNative.fromDxEndpointState((DXEndpoint.State) e.getOldValue()),
                    EndpointStateNative.fromDxEndpointState((DXEndpoint.State) e.getNewValue()));
            endpoint.addStateChangeListener(listener);
            STATE_CHANGE_LISTENER_MAP.put(listenerPtr.rawValue(), listener);
        }
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_endpoint_remove_state_change_listener")
    private static ErrorCodes removeStateChangeListener(IsolateThread ignoredThread,
                                                        long endpointDescriptor, StateChangeListenerPtr listenerPtr) {
        if (listenerPtr.isNull()) {
            return ErrorCodes.DXFG_EC_NULL_POINTER_EX;
        }
        var endpoint = getEndpointByDescriptor(endpointDescriptor);
        if (endpoint == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        var listener = STATE_CHANGE_LISTENER_MAP.remove(listenerPtr.rawValue());
        if (listener != null) {
            endpoint.removeStateChangeListener(listener);
        }
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    /**
     * Gets endpoint object from endpoint map by descriptor.
     *
     * @param endpointDescriptor The endpoint descriptor.
     * @return Returns DXEndpoint or null if this descriptor not exist.
     */
    public static DXEndpoint getEndpointByDescriptor(long endpointDescriptor) {
        return ENDPOINT_MAP.get(endpointDescriptor);
    }

    /**
     * Gets feed object from endpoint map by descriptor.
     *
     * @param endpointDescriptor The endpoint descriptor.
     * @return Returns DXFeed or null if this descriptor not exist.
     */
    public static DXFeed getFeedByDescriptor(long endpointDescriptor) {
        var endpoint = ENDPOINT_MAP.get(endpointDescriptor);
        return endpoint != null ? endpoint.getFeed() : null;
    }

    /**
     * Puts endpoint object to map for avoid GC
     * and returns descriptor (primitive value) associated with this endpoint object.
     *
     * @param endpoint The DXEndpoint.
     * @return Returns descriptor.
     */
    private static long putEndpointToMap(DXEndpoint endpoint) {
        long descriptor = ENDPOINT_MAP.size();
        var oldEndpoint = ENDPOINT_MAP.put(descriptor, endpoint);
        // Shouldn't happen.
        if (oldEndpoint != null) {
            oldEndpoint.close();
        }
        return descriptor;
    }

    /**
     * Removes endpoint object from map.
     *
     * @param endpointDescriptor The endpoint descriptor.
     * @return Returns the removed endpoint object, or null if descriptor not exist.
     */
    private static DXEndpoint removeEndpointFromMap(long endpointDescriptor) {
        return ENDPOINT_MAP.remove(endpointDescriptor);
    }
}
