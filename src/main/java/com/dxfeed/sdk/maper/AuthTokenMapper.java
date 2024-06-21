package com.dxfeed.sdk.maper;

import com.devexperts.auth.AuthToken;
import com.dxfeed.sdk.javac.DxfgAuthToken;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class AuthTokenMapper extends JavaObjectHandlerMapper<AuthToken, DxfgAuthToken> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgAuthToken.class);
  }
}
