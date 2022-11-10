package com.dxfeed.api.endpoint;

import com.dxfeed.api.DXEndpoint;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_endpoint_state_t")
public enum DxfgEndpointState {
  DXFG_ENDPOINT_STATE_NOT_CONNECTED,
  DXFG_ENDPOINT_STATE_CONNECTING,
  DXFG_ENDPOINT_STATE_CONNECTED,
  DXFG_ENDPOINT_STATE_CLOSED,
  ;

  public static DxfgEndpointState fromDXEndpointState(final DXEndpoint.State state) {
    switch (state) {
      case NOT_CONNECTED:
        return DxfgEndpointState.DXFG_ENDPOINT_STATE_NOT_CONNECTED;
      case CONNECTING:
        return DxfgEndpointState.DXFG_ENDPOINT_STATE_CONNECTING;
      case CONNECTED:
        return DxfgEndpointState.DXFG_ENDPOINT_STATE_CONNECTED;
      case CLOSED:
        return DxfgEndpointState.DXFG_ENDPOINT_STATE_CLOSED;
      default:
        throw new IllegalArgumentException("Unknown DXEndpoint.State: " + state.name());
    }
  }

  @CEnumLookup
  public static native DxfgEndpointState fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
