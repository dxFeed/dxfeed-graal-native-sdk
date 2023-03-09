package com.dxfeed.sdk.javac;

import java.io.InputStream;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_input_stream_t")
public interface DxfgInputStream extends JavaObjectHandler<InputStream> {

}
