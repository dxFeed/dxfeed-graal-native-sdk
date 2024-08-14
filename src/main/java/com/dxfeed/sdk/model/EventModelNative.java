package com.dxfeed.sdk.model;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.model.IndexedEventModel;
import com.dxfeed.model.ObservableListModelListener;
import com.dxfeed.model.TimeSeriesEventModel;
import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.model.market.OrderBookModelFilter;
import com.dxfeed.model.market.OrderBookModelListener;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventTypeList;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutor;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class EventModelNative {

  @CEntryPoint(
      name = "dxfg_OrderBookModel_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgOrderBookModel dxfg_OrderBookModel_new(
      final IsolateThread ignoredThread
  ) {
    return NativeUtils.MAPPER_ORDER_BOOK_MODEL.toNative(new OrderBookModel());
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgIndexedEventModel dxfg_IndexedEventModel_new(
      final IsolateThread ignoredThread,
      final DxfgEventClazz clazz
  ) {
    return NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toNative(
        new IndexedEventModel<>((Class<? extends IndexedEvent<?>>) clazz.clazz));
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeSeriesEventModel dxfg_TimeSeriesEventModel_new(
      final IsolateThread ignoredThread,
      final DxfgEventClazz clazz
  ) {
    return NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toNative(
        new TimeSeriesEventModel<>((Class<? extends TimeSeriesEvent<?>>) clazz.clazz));
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_attach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_attach(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgFeed feed
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).attach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_attach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_attach(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel,
      final DxfgFeed feed
  ) {
    NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).attach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_attach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_attach(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
      final DxfgFeed feed
  ) {
    NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel)
        .attach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_detach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_detach(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgFeed feed
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).detach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_detach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_detach(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel,
      final DxfgFeed feed
  ) {
    NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).detach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_detach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_detach(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
      final DxfgFeed feed
  ) {
    NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel)
        .detach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_close(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).close();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_close(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).close();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_close(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).close();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecutor dxfg_OrderBookModel_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(
        NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getExecutor()
    );
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecutor dxfg_IndexedEventModel_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(
        NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).getExecutor()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecutor dxfg_TimeSeriesEventModel_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(
        NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getExecutor()
    );
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgExecutor dxfgExecutor
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel)
        .setExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(dxfgExecutor));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel,
      final DxfgExecutor dxfgExecutor
  ) {
    NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel)
        .setExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(dxfgExecutor));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
      final DxfgExecutor dxfgExecutor
  ) {
    NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel)
        .setExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(dxfgExecutor));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_clear(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).clear();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_clear(
      final IsolateThread ignoreThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).clear();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_clear(
      final IsolateThread ignoreThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).clear();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getFilter",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_getFilter(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    final OrderBookModelFilter filter = NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel)
        .getFilter();
    if (filter == OrderBookModelFilter.ALL) {
      return DxfgOrderBookModelFilter.ALL.getCValue();
    } else if (filter == OrderBookModelFilter.REGIONAL) {
      return DxfgOrderBookModelFilter.REGIONAL.getCValue();
    } else if (filter == OrderBookModelFilter.AGGREGATE) {
      return DxfgOrderBookModelFilter.AGGREGATE.getCValue();
    } else if (filter == OrderBookModelFilter.ORDER) {
      return DxfgOrderBookModelFilter.ORDER.getCValue();
    } else if (filter == OrderBookModelFilter.COMPOSITE_REGIONAL) {
      return DxfgOrderBookModelFilter.COMPOSITE_REGIONAL.getCValue();
    } else if (filter == OrderBookModelFilter.COMPOSITE_REGIONAL_AGGREGATE) {
      return DxfgOrderBookModelFilter.COMPOSITE_REGIONAL_AGGREGATE.getCValue();
    } else if (filter == OrderBookModelFilter.COMPOSITE) {
      return DxfgOrderBookModelFilter.COMPOSITE.getCValue();
    }
    return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_setFilter",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_setFilter(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgOrderBookModelFilter dxfgOrderBookModelFilter
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).setFilter(dxfgOrderBookModelFilter.filter);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getSymbol",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_OrderBookModel_getSymbol(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getSymbol()
    );
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_getSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static DxfgSymbol dxfg_IndexedEventModel_getSymbol(
      final IsolateThread ignoreThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    return NativeUtils.MAPPER_SYMBOL.toNative(
        NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).getSymbol()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_getSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static DxfgSymbol dxfg_TimeSeriesEventModel_getSymbol(
      final IsolateThread ignoreThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    return NativeUtils.MAPPER_SYMBOL.toNative(
        NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getSymbol()
    );
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_setSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_setSymbol(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final CCharPointer symbol
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).setSymbol(NativeUtils.MAPPER_STRING.toJava(symbol));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_setSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_setSymbol(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel,
      final DxfgSymbol symbol
  ) {
    NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel)
        .setSymbol(NativeUtils.MAPPER_SYMBOL.toJava(symbol));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_setSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_setSymbol(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
      final DxfgSymbol symbol
  ) {
    NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel)
        .setSymbol(NativeUtils.MAPPER_SYMBOL.toJava(symbol));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getLotSize",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_getLotSize(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getLotSize();
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_setLotSize",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_setLotSize(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final int lotSize
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).setLotSize(lotSize);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_getSizeLimit",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_getSizeLimit(
      final IsolateThread ignoreThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    return NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).getSizeLimit();
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_setSizeLimit",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_setSizeLimit(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel,
      final int sizeLimit
  ) {
    NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).setSizeLimit(sizeLimit);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_getSizeLimit",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_getSizeLimit(
      final IsolateThread ignoreThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    return NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getSizeLimit();
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_setSizeLimit",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_setSizeLimit(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
      final int sizeLimit
  ) {
    NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).setSizeLimit(sizeLimit);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_getFromTime",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static long dxfg_TimeSeriesEventModel_getFromTime(
      final IsolateThread ignoreThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    return NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getFromTime();
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_setFromTime",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_setFromTime(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
      final long fromTime
  ) {
    NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).setFromTime(fromTime);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getBuyOrders",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableListModel dxfg_OrderBookModel_getBuyOrders(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL.toNative(
        NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getBuyOrders()
    );
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getSellOrders",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableListModel dxfg_OrderBookModel_getSellOrders(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL.toNative(
        NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getSellOrders()
    );
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_getEventsList",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableListModel dxfg_IndexedEventModel_getEventsList(
      final IsolateThread ignoreThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    return NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL.toNative(
        NativeUtils.MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).getEventsList()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_getEventsList",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableListModel dxfg_TimeSeriesEventModel_getEventsList(
      final IsolateThread ignoreThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    return NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL.toNative(
        NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getEventsList()
    );
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModel_addListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableListModel_addListener(
      final IsolateThread ignoreThread,
      final DxfgObservableListModel dxfgObservableListModel,
      final DxfgObservableListModelListener dxfgObservableListModelListener
  ) {
    NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL.toJava(dxfgObservableListModel).addListener(
        NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL_LISTENER.toJava(dxfgObservableListModelListener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModel_removeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableListModel_removeListener(
      final IsolateThread ignoreThread,
      final DxfgObservableListModel dxfgObservableListModel,
      final DxfgObservableListModelListener dxfgObservableListModelListener
  ) {
    NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL.toJava(dxfgObservableListModel).removeListener(
        NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL_LISTENER.toJava(dxfgObservableListModelListener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModel_toArray",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventTypeList dxfg_ObservableListModel_toArray(
      final IsolateThread ignoreThread,
      final DxfgObservableListModel dxfgObservableListModel
  ) {
    return NativeUtils.MAPPER_EVENTS.toNativeList(
        NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL.toJava(dxfgObservableListModel)
    );
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_addListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_addListener(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgOrderBookModelListener dxfgOrderBookModelListener
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).addListener(
        NativeUtils.MAPPER_ORDER_BOOK_MODEL_LISTENER.toJava(dxfgOrderBookModelListener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_removeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_removeListener(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgOrderBookModelListener dxfgOrderBookModelListener
  ) {
    NativeUtils.MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel)
        .removeListener(NativeUtils.MAPPER_ORDER_BOOK_MODEL_LISTENER.toJava(dxfgOrderBookModelListener));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModelListener_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgOrderBookModelListener dxfg_OrderBookModelListener_new(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModelListenerFunction userFunc,
      final VoidPointer userData
  ) {
    return NativeUtils.MAPPER_ORDER_BOOK_MODEL_LISTENER.toNative(
        new OrderBookModelListener() {
          @Override
          public void modelChanged(final Change change) {
            final DxfgOrderBookModel orderBookModel = NativeUtils.MAPPER_ORDER_BOOK_MODEL.toNative(
                change.getSource());
            userFunc.invoke(
                CurrentIsolate.getCurrentThread(),
                orderBookModel,
                userData
            );
            NativeUtils.MAPPER_ORDER_BOOK_MODEL.release(orderBookModel);
          }
        }
    );
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModelListener_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableListModelListener dxfg_ObservableListModelListener_new(
      final IsolateThread ignoredThread,
      final DxfgObservableListModelListenerFunction userFunc,
      final VoidPointer userData
  ) {
    return NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL_LISTENER.toNative(
        new ObservableListModelListener<IndexedEvent<?>>() {
          @Override
          public void modelChanged(final Change<? extends IndexedEvent<?>> change) {
            final DxfgEventTypeList dxfgOrderList = NativeUtils.MAPPER_EVENTS.toNativeList(
                change.getSource());
            userFunc.invoke(
                CurrentIsolate.getCurrentThread(),
                dxfgOrderList,
                userData
            );
            NativeUtils.MAPPER_EVENTS.release(dxfgOrderList);
          }
        }
    );
  }
}
