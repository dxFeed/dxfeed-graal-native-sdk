package com.dxfeed.api.feed;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.promise.Promise;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_promise_t")
public interface DxfgPromise extends JavaObjectHandler<Promise<?>> {

}
