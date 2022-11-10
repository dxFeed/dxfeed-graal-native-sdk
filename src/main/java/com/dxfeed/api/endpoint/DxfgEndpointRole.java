package com.dxfeed.api.endpoint;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.Role;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_endpoint_role_t")
public enum DxfgEndpointRole {
  DXFG_ENDPOINT_ROLE_FEED,
  DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED,
  DXFG_ENDPOINT_ROLE_STREAM_FEED,
  DXFG_ENDPOINT_ROLE_PUBLISHER,
  DXFG_ENDPOINT_ROLE_STREAM_PUBLISHER,
  DXFG_ENDPOINT_ROLE_LOCAL_HUB,
  ;

  public static DXEndpoint.Role toDXEndpointRole(final DxfgEndpointRole role) {
    switch (role) {
      case DXFG_ENDPOINT_ROLE_FEED:
        return Role.FEED;
      case DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED:
        return Role.ON_DEMAND_FEED;
      case DXFG_ENDPOINT_ROLE_STREAM_FEED:
        return Role.STREAM_FEED;
      case DXFG_ENDPOINT_ROLE_PUBLISHER:
        return Role.PUBLISHER;
      case DXFG_ENDPOINT_ROLE_STREAM_PUBLISHER:
        return Role.STREAM_PUBLISHER;
      case DXFG_ENDPOINT_ROLE_LOCAL_HUB:
        return Role.LOCAL_HUB;
      default:
        throw new IllegalArgumentException("Unknown DxfgEndpointRole: " + role.getCValue());
    }
  }

  public static DxfgEndpointRole fromDXEndpointRole(final DXEndpoint.Role role) {
    switch (role) {
      case FEED:
        return DXFG_ENDPOINT_ROLE_FEED;
      case ON_DEMAND_FEED:
        return DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED;
      case STREAM_FEED:
        return DXFG_ENDPOINT_ROLE_STREAM_FEED;
      case PUBLISHER:
        return DXFG_ENDPOINT_ROLE_PUBLISHER;
      case STREAM_PUBLISHER:
        return DXFG_ENDPOINT_ROLE_STREAM_PUBLISHER;
      case LOCAL_HUB:
        return DXFG_ENDPOINT_ROLE_LOCAL_HUB;
      default:
        throw new IllegalArgumentException("Unknown DXEndpoint.Role: " + role.name());
    }
  }

  @CEnumLookup
  public static native DxfgEndpointRole fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
