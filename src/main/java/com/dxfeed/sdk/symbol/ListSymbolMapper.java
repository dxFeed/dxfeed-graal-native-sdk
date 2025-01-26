package com.dxfeed.sdk.symbol;

import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListSymbolMapper extends
    ListMapper<Object, DxfgSymbol, DxfgSymbolPointer, DxfgSymbolList> {

  private final Mapper<Object, DxfgSymbol> symbolMapper;

  public ListSymbolMapper(final Mapper<Object, DxfgSymbol> symbolMapper) {
    this.symbolMapper = symbolMapper;
  }

  public void release(final DxfgSymbolList nativeList) {
    for (int i = 0; i < nativeList.getSize(); ++i) {
      this.symbolMapper.release(nativeList.getElements().addressOf(i).read());
    }
    UnmanagedMemory.free(nativeList.getElements());
    UnmanagedMemory.free(nativeList);
  }

  @Override
  protected Object toJava(final DxfgSymbol nObject) {
    return this.symbolMapper.toJava(nObject);
  }

  @Override
  protected DxfgSymbol toNative(final Object jObject) {
    return this.symbolMapper.toNative(jObject);
  }

  @Override
  protected void releaseNative(final DxfgSymbol nObject) {
    this.symbolMapper.release(nObject);
  }

  @Override
  protected int getNativeListSize() {
    return SizeOf.get(DxfgSymbolList.class);
  }
}
