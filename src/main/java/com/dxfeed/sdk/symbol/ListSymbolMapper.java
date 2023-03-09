package com.dxfeed.sdk.symbol;

import com.dxfeed.sdk.maper.ListMapper;
import com.dxfeed.sdk.maper.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListSymbolMapper extends
    ListMapper<Object, DxfgSymbol, DxfgSymbolPointer, DxfgSymbolList> {

  private final Mapper<Object, DxfgSymbol> symbolMapper;

  public ListSymbolMapper(final Mapper<Object, DxfgSymbol> symbolMapper) {
    this.symbolMapper = symbolMapper;
  }

  public void release(final DxfgSymbolList nList) {
    for (int i = 0; i < nList.getSize(); ++i) {
      this.symbolMapper.release(nList.getElements().addressOf(i).read());
    }
    UnmanagedMemory.free(nList.getElements());
    UnmanagedMemory.free(nList);
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
  protected int getSizeCList() {
    return SizeOf.get(DxfgSymbolList.class);
  }
}
