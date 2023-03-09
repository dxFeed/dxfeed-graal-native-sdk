package com.dxfeed.sdk.feed;

import com.dxfeed.promise.Promise;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_promise_t")
public interface DxfgPromise extends JavaObjectHandler<Promise<?>> {

}
