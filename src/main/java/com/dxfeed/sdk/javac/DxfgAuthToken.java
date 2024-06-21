package com.dxfeed.sdk.javac;

import com.devexperts.auth.AuthToken;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_auth_token_t")
public interface DxfgAuthToken extends JavaObjectHandler<AuthToken> {

}
