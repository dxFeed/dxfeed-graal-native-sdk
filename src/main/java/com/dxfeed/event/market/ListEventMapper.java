package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgCandle;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgEventPointer;
import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.api.events.DxfgQuote;
import com.dxfeed.api.events.DxfgTimeAndSale;
import com.dxfeed.event.EventType;
import com.dxfeed.event.candle.Candle;
import java.util.List;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class ListEventMapper implements ListNativeMapper<EventType<?>, DxfgEventPointer> {

  private static final QuoteNativeMapper QUOTE_NATIVE_MAPPER = new QuoteNativeMapper();
  private static final TimeAndSaleNativeMapper TIME_AND_SALE_NATIVE_MAPPER = new TimeAndSaleNativeMapper();
  private static final CandleNativeMapper CANDLE_NATIVE_MAPPER = new CandleNativeMapper();

  @Override
  public DxfgEventPointer nativeObject(final List<EventType<?>> events) {
    final DxfgEventPointer nativeEvents =
        UnmanagedMemory.calloc(SizeOf.get(DxfgEventPointer.class) * events.size());
    for (int i = 0; i < events.size(); ++i) {
      final EventType<?> eventType = events.get(i);
      final DxfgEventType nativeEvent;
      if (eventType instanceof Quote) {
        nativeEvent = QUOTE_NATIVE_MAPPER.nativeObject((Quote) eventType);
      } else if (eventType instanceof TimeAndSale) {
        nativeEvent = TIME_AND_SALE_NATIVE_MAPPER.nativeObject((TimeAndSale) eventType);
      } else if (eventType instanceof Candle) {
        nativeEvent = CANDLE_NATIVE_MAPPER.nativeObject((Candle) eventType);
      } else {
        nativeEvent = WordFactory.nullPointer();
      }
      nativeEvents.addressOf(i).write(nativeEvent);
    }
    return nativeEvents;
  }

  @Override
  public void delete(final DxfgEventPointer nativeEvents, final int size) {
    for (int i = 0; i < size; ++i) {
      final DxfgEventType nativeEvent = nativeEvents.addressOf(i).read();
      if (nativeEvent.isNonNull()) {
        final int kind = nativeEvent.getKind();
        if (DxfgEventKind.fromCValue(kind) == DxfgEventKind.DXFG_EVENT_TYPE_QUOTE) {
          QUOTE_NATIVE_MAPPER.delete((DxfgQuote) nativeEvent);
        } else if (DxfgEventKind.fromCValue(kind) == DxfgEventKind.DXFG_EVENT_TYPE_TIME_AND_SALE) {
          TIME_AND_SALE_NATIVE_MAPPER.delete((DxfgTimeAndSale) nativeEvent);
        } else if (DxfgEventKind.fromCValue(kind) == DxfgEventKind.DXFG_EVENT_TYPE_CANDLE) {
          CANDLE_NATIVE_MAPPER.delete((DxfgCandle) nativeEvent);
        } else {
          throw new UnsupportedOperationException("It has not yet been implemented.");
        }
      }
    }
    UnmanagedMemory.free(nativeEvents);
  }
}
