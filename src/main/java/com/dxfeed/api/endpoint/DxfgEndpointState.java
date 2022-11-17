package com.dxfeed.api.endpoint;

import static com.dxfeed.api.DXEndpoint.State.CLOSED;
import static com.dxfeed.api.DXEndpoint.State.CONNECTED;
import static com.dxfeed.api.DXEndpoint.State.CONNECTING;
import static com.dxfeed.api.DXEndpoint.State.NOT_CONNECTED;

import com.dxfeed.api.DXEndpoint.State;
import java.util.EnumMap;
import java.util.Map;
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

  private static final Map<State, DxfgEndpointState> map = new EnumMap<>(State.class);

  static {
    map.put(NOT_CONNECTED, DXFG_ENDPOINT_STATE_NOT_CONNECTED);
    map.put(CONNECTING, DXFG_ENDPOINT_STATE_CONNECTING);
    map.put(CONNECTED, DXFG_ENDPOINT_STATE_CONNECTED);
    map.put(CLOSED, DXFG_ENDPOINT_STATE_CLOSED);
  }

  public static DxfgEndpointState of(final State state) {
    return map.get(state);
  }

  @CEnumLookup
  public static native DxfgEndpointState fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
