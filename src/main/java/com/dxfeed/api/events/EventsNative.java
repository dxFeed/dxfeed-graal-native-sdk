package com.dxfeed.api.events;

import static com.dxfeed.api.NativeUtils.MAPPER_EVENT;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENTS;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENT_TYPES;
import static com.dxfeed.api.NativeUtils.MAPPER_INDEXED_EVENT_SOURCE;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOL;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOLS;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
import static org.graalvm.nativeimage.c.type.CTypeConversion.toJavaString;

import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.osub.WildcardSymbol;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.candle.CandleSymbol;
import com.dxfeed.event.market.EventMappers;
import com.dxfeed.event.market.OrderSource;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public class EventsNative {

  @CEntryPoint(
      name = "dxfg_Symbol_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSymbol dxfg_Symbol_new(
      final IsolateThread ignoredThread,
      final CCharPointer symbol,
      final DxfgSymbolType symbolType
  ) {
    switch (symbolType) {
      case STRING:
        return MAPPER_SYMBOL.toNative(
            MAPPER_STRING.toJava(symbol)
        );
      case CANDLE:
        return MAPPER_SYMBOL.toNative(
            CandleSymbol.valueOf(MAPPER_STRING.toJava(symbol))
        );
      case WILDCARD:
        return MAPPER_SYMBOL.toNative(WildcardSymbol.ALL);
      default:
        throw new IllegalStateException();
    }
  }

  @CEntryPoint(
      name = "dxfg_Symbol_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Symbol_release(
      final IsolateThread ignoredThread,
      final DxfgSymbol symbol
  ) {
    MAPPER_SYMBOL.release(symbol);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_EventType_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventType dxfg_EventType_new(
      final IsolateThread ignoredThread,
      final CCharPointer symbol,
      final DxfgEventClazz dxfgEventClazz
  ) {
    return ((EventMappers) MAPPER_EVENT).createNativeEvent(dxfgEventClazz, symbol);
  }

  @CEntryPoint(
      name = "dxfg_EventType_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int releaseEvent(
      final IsolateThread ignoredThread,
      final DxfgEventType dxfgEventType
  ) {
    MAPPER_EVENT.release(dxfgEventType);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_EventType_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_EventType_release(
      final IsolateThread ignoredThread,
      final DxfgEventTypeList nEvents
  ) {
    MAPPER_EVENTS.release(nEvents);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_EventClazz_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_EventClazz_release(
      final IsolateThread ignoredThread,
      final DxfgEventClazzList eventClazzList
  ) {
    MAPPER_EVENT_TYPES.release(eventClazzList);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_symbol_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_symbol_release(
      final IsolateThread ignoredThread,
      final DxfgSymbolList symbolList
  ) {
    MAPPER_SYMBOLS.release(symbolList);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventSource_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgIndexedEventSource dxfg_IndexedEventSource_new(
      final IsolateThread ignoredThread,
      final CCharPointer source
  ) {
    return MAPPER_INDEXED_EVENT_SOURCE.toNative(
        source.isNull()
            ? IndexedEventSource.DEFAULT
            : OrderSource.valueOf(toJavaString(source))
    );
  }

  @CEntryPoint(
      name = "dxfg_IndexedEvent_getSource",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgIndexedEventSource dxfg_IndexedEvent_getSource(
      final IsolateThread ignoredThread,
      final DxfgEventType event
  ) {
    switch (DxfgEventClazz.fromCValue(event.getClazz())) {
      case DXFG_EVENT_GREEKS:
      case DXFG_EVENT_CANDLE:
      case DXFG_EVENT_DAILY_CANDLE:
      case DXFG_EVENT_TIME_AND_SALE:
      case DXFG_EVENT_UNDERLYING:
      case DXFG_EVENT_THEO_PRICE:
      case DXFG_EVENT_SERIES:
        return MAPPER_INDEXED_EVENT_SOURCE.toNative(IndexedEventSource.DEFAULT);
      case DXFG_EVENT_ORDER_BASE:
      case DXFG_EVENT_SPREAD_ORDER:
      case DXFG_EVENT_ORDER:
      case DXFG_EVENT_ANALYTIC_ORDER:
        final long index = ((DxfgOrderBase) event).getIndex();
        int sourceId = (int) (index >> 48);
        if (!OrderSource.isSpecialSourceId(sourceId)) {
          sourceId = (int) (index >> 32);
        }
        return MAPPER_INDEXED_EVENT_SOURCE.toNative(OrderSource.valueOf(sourceId));
      default:
        throw new ClassCastException(
            DxfgEventClazz.fromCValue(event.getClazz()).clazz.getCanonicalName()
                + " is not Class<? extends IndexedEvent>"
        );
    }
  }
}
