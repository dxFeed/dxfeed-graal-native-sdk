package com.dxfeed.api.maper;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.feed.DxfgFeed;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class FeedMapper extends JavaObjectHandlerMapper<DXFeed, DxfgFeed> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgFeed.class);
  }
}
