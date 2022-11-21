package com.dxfeed.api.javac;

import java.util.concurrent.Executor;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_executor_t")
public interface DxfgExecuter extends JavaObjectHandler<Executor> {

}
