package com.dxfeed.api.source;

import com.dxfeed.api.Mapper;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.market.OrderSource;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class IndexedEventSourceMapper extends Mapper<IndexedEventSource, DxfgIndexedEventSource> {

  private final Mapper<String, CCharPointer> stringMapper;

  public IndexedEventSourceMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgIndexedEventSource toNative(final IndexedEventSource jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgIndexedEventSource nObject = UnmanagedMemory.calloc(
        SizeOf.get(DxfgIndexedEventSource.class)
    );
    fillNative(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNative(final IndexedEventSource jObject, final DxfgIndexedEventSource nObject) {
    if (jObject instanceof OrderSource) {
      nObject.setType(DxfgIndexedEventSourceType.ORDER_SOURCE.getCValue());
    } else {
      nObject.setType(DxfgIndexedEventSourceType.INDEXED_EVENT_SOURCE.getCValue());
    }
    nObject.setId(jObject.id());
    nObject.setName(this.stringMapper.toNative(jObject.name()));
  }

  @Override
  public void cleanNative(final DxfgIndexedEventSource nObject) {
    this.stringMapper.release(nObject.getName());
  }

  @Override
  public IndexedEventSource toJava(final DxfgIndexedEventSource nObject) {
    switch (DxfgIndexedEventSourceType.fromCValue(nObject.getType())) {
      case INDEXED_EVENT_SOURCE:
        return new IndexedEventSource(
            nObject.getId(),
            this.stringMapper.toJava(nObject.getName())
        );
      case ORDER_SOURCE:
        if (nObject.getName().isNonNull()) {
          return OrderSource.valueOf(this.stringMapper.toJava(nObject.getName()));
        } else {
          return OrderSource.valueOf(nObject.getId());
        }
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public void fillJava(final DxfgIndexedEventSource nObject, final IndexedEventSource jObject) {
    throw new IllegalStateException("The Java object does not support setters.");
  }
}
