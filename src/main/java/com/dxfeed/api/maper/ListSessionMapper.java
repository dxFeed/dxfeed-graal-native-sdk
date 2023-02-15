package com.dxfeed.api.maper;

import com.dxfeed.api.schedule.DxfgSession;
import com.dxfeed.api.schedule.DxfgSessionList;
import com.dxfeed.api.schedule.DxfgSessionPointer;
import com.dxfeed.schedule.Session;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListSessionMapper
    extends ListMapper<Session, DxfgSession, DxfgSessionPointer, DxfgSessionList> {

  private final Mapper<Session, DxfgSession> mapper;

  public ListSessionMapper(final Mapper<Session, DxfgSession> mapper) {
    this.mapper = mapper;
  }

  @Override
  protected Session toJava(final DxfgSession nObject) {
    return this.mapper.toJava(nObject);
  }

  @Override
  protected DxfgSession toNative(final Session jObject) {
    return this.mapper.toNative(jObject);
  }

  @Override
  protected void releaseNative(final DxfgSession nObject) {
    this.mapper.release(nObject);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgSessionList.class);
  }
}
