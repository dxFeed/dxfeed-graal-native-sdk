package com.dxfeed.sdk.maper;

import com.dxfeed.sdk.javac.DxfgExecuter;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ExecutorMapper extends JavaObjectHandlerMapper<Executor, DxfgExecuter> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgExecuter.class);
  }
}
