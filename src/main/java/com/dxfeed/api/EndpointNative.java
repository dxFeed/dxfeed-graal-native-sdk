// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api;

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

@CContext(EndpointNative.EndpointNativeDirectives.class)
public class EndpointNative {
    private static final HashMap<Long, DXEndpoint> ENDPOINT_MAP = new HashMap<>();
    private static final HashMap<Long, PropertyChangeListener> STATE_CHANGE_LISTENER_MAP = new HashMap<>();

    static class EndpointNativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve("com.dxfeed", "src/main/c/dxf_graal_endpoint.h"));
        }
    }

    @CEnum("dxf_graal_error_code_t")
    public enum EndpointState {
        DXF_ENDPOINT_NOT_CONNECTED,
        DXF_ENDPOINT_CONNECTING,
        DXF_ENDPOINT_CONNECTED,
        DXF_ENDPOINT_CLOSED,
        DXF_ENDPOINT_UNKNOWN;

        public static EndpointState fromDxEndpointState(DXEndpoint.State state) {
            switch (state) {
                case NOT_CONNECTED:
                    return EndpointState.DXF_ENDPOINT_NOT_CONNECTED;
                case CONNECTING:
                    return EndpointState.DXF_ENDPOINT_CONNECTING;
                case CONNECTED:
                    return EndpointState.DXF_ENDPOINT_CONNECTED;
                case CLOSED:
                    return EndpointState.DXF_ENDPOINT_CLOSED;
                default:
                    return EndpointState.DXF_ENDPOINT_UNKNOWN;
            }
        }

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native EndpointState fromCValue(int value);
    }

    interface StateChangeListenerPtr extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(EndpointState oldState, EndpointState newState);
    }

    @CEntryPoint(name = "dxf_endpoint_create")
    public static ErrorCodes endpointCreate(IsolateThread ignoredThread, CLongPointer endpointDesc) {
        if (endpointDesc.isNull()) {
            return ErrorCodes.DX_EC_NULL_POINTER_EX;
        }
        try {
            var endpoint = DXEndpoint.newBuilder()
                    .withRole(DXEndpoint.Role.FEED)
                    .withProperties(System.getProperties())
                    .build();
            endpointDesc.write(putEndpointToMap(endpoint));
            return ErrorCodes.DX_EC_SUCCESS;
        } catch (IllegalStateException e) {
            return ErrorCodes.DX_EC_ILLEGAL_STATE_EX;
        } catch (Exception e) {
            return ErrorCodes.DX_EC_UNKNOWN_ERR;
        }
    }

    @CEntryPoint(name = "dxf_endpoint_connect")
    public static ErrorCodes endpointConnect(IsolateThread ignoredThread, long endpointDesc, CCharPointer address) {
        if (address.isNull()) {
            return ErrorCodes.DX_EC_NULL_POINTER_EX;
        }
        var endpoint = ENDPOINT_MAP.get(endpointDesc);
        if (endpoint == null) {
            return ErrorCodes.DX_EC_UNKNOWN_DESCRIPTOR;
        }
        try {
            endpoint.connect(CTypeConversion.toJavaString(address));
            return ErrorCodes.DX_EC_SUCCESS;
        } catch (Exception e) {
            return ErrorCodes.DX_EC_UNKNOWN_ERR;
        }
    }

    @CEntryPoint(name = "dxf_endpoint_reconnect")
    public static ErrorCodes endpointReconnect(IsolateThread ignoredThread, long endpointDesc) {
        var endpoint = ENDPOINT_MAP.get(endpointDesc);
        if (endpoint == null) {
            return ErrorCodes.DX_EC_UNKNOWN_DESCRIPTOR;
        }
        endpoint.reconnect();
        return ErrorCodes.DX_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxf_endpoint_disconnect")
    public static ErrorCodes endpointDisconnect(IsolateThread ignoredThread, long endpointDesc) {
        var endpoint = ENDPOINT_MAP.get(endpointDesc);
        if (endpoint == null) {
            return ErrorCodes.DX_EC_UNKNOWN_DESCRIPTOR;
        }
        endpoint.disconnect();
        return ErrorCodes.DX_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxf_endpoint_close")
    public static ErrorCodes endpointClose(IsolateThread ignoredThread, long endpointDesc) {
        var endpoint = ENDPOINT_MAP.remove(endpointDesc);
        if (endpoint == null) {
            return ErrorCodes.DX_EC_UNKNOWN_DESCRIPTOR;
        }
        endpoint.close();
        return ErrorCodes.DX_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxf_endpoint_get_state")
    public static EndpointState endpointGetState(IsolateThread ignoredThread, long endpointDesc) {
        var endpoint = ENDPOINT_MAP.get(endpointDesc);
        if (endpoint == null) {
            return EndpointState.DXF_ENDPOINT_UNKNOWN;
        }
        return EndpointState.fromDxEndpointState(endpoint.getState());
    }

    @CEntryPoint(name = "dxf_endpoint_add_state_change_listener")
    public static ErrorCodes endpointAddStateChangeListener(IsolateThread ignoredThread, long endpointDesc, StateChangeListenerPtr listenerPtr) {
        if (listenerPtr.isNull()) {
            return ErrorCodes.DX_EC_NULL_POINTER_EX;
        }
        var endpoint = ENDPOINT_MAP.get(endpointDesc);
        if (endpoint == null) {
            return ErrorCodes.DX_EC_UNKNOWN_DESCRIPTOR;
        }
        if (!STATE_CHANGE_LISTENER_MAP.containsKey(listenerPtr.rawValue())) {
            PropertyChangeListener listener = e -> listenerPtr.invoke(
                    EndpointState.fromDxEndpointState((DXEndpoint.State) e.getOldValue()),
                    EndpointState.fromDxEndpointState((DXEndpoint.State) e.getNewValue()));
            endpoint.addStateChangeListener(listener);
            STATE_CHANGE_LISTENER_MAP.put(listenerPtr.rawValue(), listener);
        }
        return ErrorCodes.DX_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxf_endpoint_remove_state_change_listener")
    public static ErrorCodes endpointRemoveStateChangeListener(IsolateThread ignoredThread, long endpointDesc, StateChangeListenerPtr listenerPtr) {
        if (listenerPtr.isNull()) {
            return ErrorCodes.DX_EC_NULL_POINTER_EX;
        }
        var endpoint = ENDPOINT_MAP.get(endpointDesc);
        if (endpoint == null) {
            return ErrorCodes.DX_EC_UNKNOWN_DESCRIPTOR;
        }
        var listener = STATE_CHANGE_LISTENER_MAP.remove(listenerPtr.rawValue());
        if (listener != null) {
            endpoint.removeStateChangeListener(listener);
        }
        return ErrorCodes.DX_EC_SUCCESS;
    }

    /**
     * Puts endpoint object to map for avoid GC
     * and returns descriptor (primitive value) associated with this endpoint object.
     *
     * @param endpoint The DXEndpoint.
     * @return Returns descriptor.
     */
    private static long putEndpointToMap(DXEndpoint endpoint) {
        long desc = ENDPOINT_MAP.size();
        var oldEndpoint = ENDPOINT_MAP.put(desc, endpoint);
        // Shouldn't happen.
        if (oldEndpoint != null) {
            oldEndpoint.close();
        }
        return desc;
    }
}
