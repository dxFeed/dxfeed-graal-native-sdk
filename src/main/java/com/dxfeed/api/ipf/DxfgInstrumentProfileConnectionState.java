package com.dxfeed.api.ipf;

import static com.dxfeed.ipf.live.InstrumentProfileConnection.State.CLOSED;
import static com.dxfeed.ipf.live.InstrumentProfileConnection.State.COMPLETED;
import static com.dxfeed.ipf.live.InstrumentProfileConnection.State.CONNECTED;
import static com.dxfeed.ipf.live.InstrumentProfileConnection.State.CONNECTING;
import static com.dxfeed.ipf.live.InstrumentProfileConnection.State.NOT_CONNECTED;

import com.dxfeed.ipf.live.InstrumentProfileConnection.State;
import java.util.EnumMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_ipf_connection_state_t")
public enum DxfgInstrumentProfileConnectionState {
  DXFG_IPF_CONNECTION_STATE_NOT_CONNECTED,
  DXFG_IPF_CONNECTION_STATE_CONNECTING,
  DXFG_IPF_CONNECTION_STATE_CONNECTED,
  DXFG_IPF_CONNECTION_STATE_COMPLETED,
  DXFG_IPF_CONNECTION_STATE_CLOSED,
  ;

  private static final Map<State, DxfgInstrumentProfileConnectionState> map = new EnumMap<>(State.class);

  static {
    map.put(NOT_CONNECTED, DXFG_IPF_CONNECTION_STATE_NOT_CONNECTED);
    map.put(CONNECTING, DXFG_IPF_CONNECTION_STATE_CONNECTING);
    map.put(CONNECTED, DXFG_IPF_CONNECTION_STATE_CONNECTED);
    map.put(COMPLETED, DXFG_IPF_CONNECTION_STATE_COMPLETED);
    map.put(CLOSED, DXFG_IPF_CONNECTION_STATE_CLOSED);
  }

  public static DxfgInstrumentProfileConnectionState of(final State state) {
    return map.get(state);
  }

  @CEnumLookup
  public static native DxfgInstrumentProfileConnectionState fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
