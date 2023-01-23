package com.dxfeed.api.maper;

import com.dxfeed.api.javac.DxfgCharPointerList;
import com.dxfeed.api.javac.DxfgCharPointerPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class ListStringsMapper
    extends ListMapper<String, CCharPointer, DxfgCharPointerPointer, DxfgCharPointerList> {

  private final Mapper<String, CCharPointer> stringMapper;

  public ListStringsMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  protected String toJava(final CCharPointer nObject) {
    return this.stringMapper.toJava(nObject);
  }

  @Override
  protected CCharPointer toNative(final String jObject) {
    return this.stringMapper.toNative(jObject);
  }

  @Override
  protected void releaseNative(final CCharPointer nObject) {
    this.stringMapper.release(nObject);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgCharPointerList.class);
  }
}
