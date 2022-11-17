package com.dxfeed.api.endpoint;

import static com.dxfeed.api.DXEndpoint.Role.FEED;
import static com.dxfeed.api.DXEndpoint.Role.LOCAL_HUB;
import static com.dxfeed.api.DXEndpoint.Role.ON_DEMAND_FEED;
import static com.dxfeed.api.DXEndpoint.Role.PUBLISHER;
import static com.dxfeed.api.DXEndpoint.Role.STREAM_FEED;
import static com.dxfeed.api.DXEndpoint.Role.STREAM_PUBLISHER;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.Role;
import java.util.EnumMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_endpoint_role_t")
public enum DxfgEndpointRole {
  DXFG_ENDPOINT_ROLE_FEED(FEED),
  DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED(ON_DEMAND_FEED),
  DXFG_ENDPOINT_ROLE_STREAM_FEED(STREAM_FEED),
  DXFG_ENDPOINT_ROLE_PUBLISHER(PUBLISHER),
  DXFG_ENDPOINT_ROLE_STREAM_PUBLISHER(STREAM_PUBLISHER),
  DXFG_ENDPOINT_ROLE_LOCAL_HUB(LOCAL_HUB),
  ;

  private static final Map<Role, DxfgEndpointRole> map = new EnumMap<>(Role.class);

  static {
    map.put(FEED, DXFG_ENDPOINT_ROLE_FEED);
    map.put(ON_DEMAND_FEED, DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED);
    map.put(STREAM_FEED, DXFG_ENDPOINT_ROLE_STREAM_FEED);
    map.put(PUBLISHER, DXFG_ENDPOINT_ROLE_PUBLISHER);
    map.put(STREAM_PUBLISHER, DXFG_ENDPOINT_ROLE_STREAM_PUBLISHER);
    map.put(LOCAL_HUB, DXFG_ENDPOINT_ROLE_LOCAL_HUB);
  }

  public final DXEndpoint.Role qdRole;

  DxfgEndpointRole(final Role qdRole) {
    this.qdRole = qdRole;
  }

  public static DxfgEndpointRole of(final DXEndpoint.Role role) {
    return map.get(role);
  }

  @CEnumLookup
  public static native DxfgEndpointRole fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
