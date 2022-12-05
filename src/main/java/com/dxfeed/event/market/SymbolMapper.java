package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.events.DxfgSymbol;
import com.dxfeed.api.events.DxfgSymbolType;
import com.dxfeed.api.osub.WildcardSymbol;
import com.dxfeed.event.candle.CandleSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class SymbolMapper extends Mapper<Object, DxfgSymbol> {

  private final Mapper<String, CCharPointer> stringMapper;

  public SymbolMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgSymbol toNative(final Object jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgSymbol nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgSymbol.class));
    fillNative(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNative(final Object jObject, final DxfgSymbol nObject) {
    if (jObject instanceof CandleSymbol) {
      nObject.setSymbolType(DxfgSymbolType.CANDLE.getCValue());
      nObject.setSymbolString(this.stringMapper.toNative(jObject.toString()));
    } else if (jObject instanceof String) {
      nObject.setSymbolType(DxfgSymbolType.STRING.getCValue());
      nObject.setSymbolString(this.stringMapper.toNative(jObject.toString()));
    } else if (jObject instanceof WildcardSymbol) {
      nObject.setSymbolType(DxfgSymbolType.WILDCARD.getCValue());
      nObject.setSymbolString(WordFactory.nullPointer());
    }
  }

  @Override
  public void cleanNative(final DxfgSymbol nObject) {
    this.stringMapper.cleanNative(nObject.getSymbolString());
  }

  @Override
  public Object toJava(final DxfgSymbol nObject) {
    if (nObject.isNull()) {
      return null;
    }
    return DxfgSymbolType.fromCValue(nObject.getSymbolType())
        .createSymbolFunction.apply(this.stringMapper.toJava(nObject.getSymbolString()));
  }

  @Override
  public void fillJava(final DxfgSymbol nObject, final Object jObject) {
    throw new IllegalStateException();
  }
}
