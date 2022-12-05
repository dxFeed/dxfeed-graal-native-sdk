package com.dxfeed.api.maper;

import com.dxfeed.api.javac.DxfgExecuter;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ExecutorMapper extends JavaObjectHandlerMapper<Executor, DxfgExecuter> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgExecuter.class);
  }
}
