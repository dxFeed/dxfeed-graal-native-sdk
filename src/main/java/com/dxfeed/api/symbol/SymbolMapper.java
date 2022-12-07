package com.dxfeed.api.symbol;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.osub.IndexedEventSubscriptionSymbol;
import com.dxfeed.api.osub.TimeSeriesSubscriptionSymbol;
import com.dxfeed.api.osub.WildcardSymbol;
import com.dxfeed.api.source.DxfgIndexedEventSource;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.candle.CandleSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class SymbolMapper extends Mapper<Object, DxfgSymbol> {

  private final Mapper<String, CCharPointer> stringMapper;
  private final Mapper<IndexedEventSource, DxfgIndexedEventSource> mapperSource;

  public SymbolMapper(
      final Mapper<String, CCharPointer> stringMapper,
      final Mapper<IndexedEventSource, DxfgIndexedEventSource> mapperSource
  ) {
    this.stringMapper = stringMapper;
    this.mapperSource = mapperSource;
  }

  @Override
  public DxfgSymbol toNative(final Object jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgSymbol nObject;
    if (jObject instanceof String) {
      nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgStringSymbol.class));
    } else if (jObject instanceof CandleSymbol) {
      nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandleSymbol.class));
    } else if (jObject instanceof TimeSeriesSubscriptionSymbol) {
      nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTimeSeriesSubscriptionSymbol.class));
    } else if (jObject instanceof IndexedEventSubscriptionSymbol) {
      nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgIndexedEventSubscriptionSymbol.class));
    } else if (jObject instanceof WildcardSymbol) {
      nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgWildcardSymbol.class));
    } else {
      throw new IllegalStateException();
    }
    fillNative(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNative(final Object jObject, final DxfgSymbol nObject) {
    if (jObject instanceof String) {
      final DxfgStringSymbol dxfgStringSymbol = (DxfgStringSymbol) nObject;
      final String stringSymbol = (String) jObject;
      dxfgStringSymbol.setType(DxfgSymbolType.STRING.getCValue());
      dxfgStringSymbol.setSymbol(this.stringMapper.toNative(stringSymbol));
    } else if (jObject instanceof CandleSymbol) {
      final DxfgCandleSymbol dxfgCandleSymbol = (DxfgCandleSymbol) nObject;
      final CandleSymbol candleSymbol = (CandleSymbol) jObject;
      dxfgCandleSymbol.setType(DxfgSymbolType.CANDLE.getCValue());
      dxfgCandleSymbol.setSymbol(this.stringMapper.toNative(candleSymbol.toString()));
    } else if (jObject instanceof WildcardSymbol) {
      nObject.setType(DxfgSymbolType.WILDCARD.getCValue());
    } else if (jObject instanceof TimeSeriesSubscriptionSymbol) {
      final DxfgTimeSeriesSubscriptionSymbol dxfgTssSymbol = (DxfgTimeSeriesSubscriptionSymbol) nObject;
      final TimeSeriesSubscriptionSymbol<?> tssSymbol = (TimeSeriesSubscriptionSymbol<?>) jObject;
      dxfgTssSymbol.setType(DxfgSymbolType.TIME_SERIES_SUBSCRIPTION.getCValue());
      dxfgTssSymbol.setSymbol(toNative(tssSymbol.getEventSymbol()));
      dxfgTssSymbol.setFromTime(tssSymbol.getFromTime());
    } else if (jObject instanceof IndexedEventSubscriptionSymbol) {
      final DxfgIndexedEventSubscriptionSymbol dxfgIesSymbol = (DxfgIndexedEventSubscriptionSymbol) nObject;
      final IndexedEventSubscriptionSymbol<?> iesSymbol = (IndexedEventSubscriptionSymbol<?>) jObject;
      dxfgIesSymbol.setType(DxfgSymbolType.INDEXED_EVENT_SUBSCRIPTION.getCValue());
      dxfgIesSymbol.setSymbol(toNative(iesSymbol.getEventSymbol()));
      dxfgIesSymbol.setSource(this.mapperSource.toNative(iesSymbol.getSource()));
    }
  }

  @Override
  public void cleanNative(final DxfgSymbol nObject) {
    switch (DxfgSymbolType.fromCValue(nObject.getType())) {
      case STRING:
        this.stringMapper.release(((DxfgStringSymbol) nObject).getSymbol());
        break;
      case CANDLE:
        this.stringMapper.release(((DxfgCandleSymbol) nObject).getSymbol());
        break;
      case TIME_SERIES_SUBSCRIPTION:
        release(((DxfgTimeSeriesSubscriptionSymbol) nObject).getSymbol());
        break;
      case INDEXED_EVENT_SUBSCRIPTION:
        final DxfgIndexedEventSubscriptionSymbol dxfgIesSymbol = (DxfgIndexedEventSubscriptionSymbol) nObject;
        release(dxfgIesSymbol.getSymbol());
        this.mapperSource.release(dxfgIesSymbol.getSource());
        break;
    }
  }

  @Override
  public Object toJava(final DxfgSymbol nObject) {
    if (nObject.isNull()) {
      return null;
    }
    switch (DxfgSymbolType.fromCValue(nObject.getType())) {
      case STRING:
        return this.stringMapper.toJava(((DxfgStringSymbol) nObject).getSymbol());
      case CANDLE:
        return CandleSymbol.valueOf(
            this.stringMapper.toJava(((DxfgCandleSymbol) nObject).getSymbol())
        );
      case WILDCARD:
        return WildcardSymbol.ALL;
      case TIME_SERIES_SUBSCRIPTION:
        final DxfgTimeSeriesSubscriptionSymbol dxfgTssSymbol = (DxfgTimeSeriesSubscriptionSymbol) nObject;
        return new TimeSeriesSubscriptionSymbol<>(
            toJava(dxfgTssSymbol.getSymbol()),
            dxfgTssSymbol.getFromTime()
        );
      case INDEXED_EVENT_SUBSCRIPTION:
        final DxfgIndexedEventSubscriptionSymbol dxfgIesSymbol = (DxfgIndexedEventSubscriptionSymbol) nObject;
        return new IndexedEventSubscriptionSymbol<>(
            toJava(dxfgIesSymbol.getSymbol()),
            this.mapperSource.toJava(dxfgIesSymbol.getSource())
        );
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public void fillJava(final DxfgSymbol nObject, final Object jObject) {
    throw new IllegalStateException("The Java object does not support setters.");
  }
}
