package com.dxfeed.api.model;

import static com.dxfeed.api.NativeUtils.FINALIZER;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENTS;
import static com.dxfeed.api.NativeUtils.MAPPER_EXECUTOR;
import static com.dxfeed.api.NativeUtils.MAPPER_FEED;
import static com.dxfeed.api.NativeUtils.MAPPER_INDEXED_EVENT_MODEL;
import static com.dxfeed.api.NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL;
import static com.dxfeed.api.NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_ORDER_BOOK_MODEL;
import static com.dxfeed.api.NativeUtils.MAPPER_ORDER_BOOK_MODEL_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOL;
import static com.dxfeed.api.NativeUtils.MAPPER_TIME_SERIES_EVENT_MODEL;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgEventTypeList;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.api.javac.DxfgFinalizeFunction;
import com.dxfeed.api.symbol.DxfgSymbol;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.model.IndexedEventModel;
import com.dxfeed.model.TimeSeriesEventModel;
import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.model.market.OrderBookModelFilter;
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
    return MAPPER_ORDER_BOOK_MODEL.toNative(new OrderBookModel());
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgIndexedEventModel dxfg_IndexedEventModel_new(
      final IsolateThread ignoredThread,
      final DxfgEventClazz clazz
  ) {
    return MAPPER_INDEXED_EVENT_MODEL.toNative(
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
    return MAPPER_TIME_SERIES_EVENT_MODEL.toNative(
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
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).attach(MAPPER_FEED.toJava(feed));
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).attach(MAPPER_FEED.toJava(feed));
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel)
        .attach(MAPPER_FEED.toJava(feed));
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).detach(MAPPER_FEED.toJava(feed));
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).detach(MAPPER_FEED.toJava(feed));
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel)
        .detach(MAPPER_FEED.toJava(feed));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_close(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).close();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_close(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).close();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_close(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).close();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_OrderBookModel_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return MAPPER_EXECUTOR.toNative(
        MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getExecutor()
    );
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_IndexedEventModel_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    return MAPPER_EXECUTOR.toNative(
        MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).getExecutor()
    );
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_TimeSeriesEventModel_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    return MAPPER_EXECUTOR.toNative(
        MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getExecutor()
    );
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgExecuter dxfgExecuter
  ) {
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel)
        .setExecutor(MAPPER_EXECUTOR.toJava(dxfgExecuter));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel,
      final DxfgExecuter dxfgExecuter
  ) {
    MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel)
        .setExecutor(MAPPER_EXECUTOR.toJava(dxfgExecuter));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
      final DxfgExecuter dxfgExecuter
  ) {
    MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel)
        .setExecutor(MAPPER_EXECUTOR.toJava(dxfgExecuter));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_clear(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).clear();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_clear(
      final IsolateThread ignoreThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).clear();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_clear(
      final IsolateThread ignoreThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).clear();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getFilter",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_getFilter(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    final OrderBookModelFilter filter = MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel)
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
    return EXECUTE_FAIL;
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
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).setFilter(dxfgOrderBookModelFilter.filter);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static CCharPointer dxfg_OrderBookModel_getSymbol(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return MAPPER_STRING.toNative(
        MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getSymbol()
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
    return MAPPER_SYMBOL.toNative(
        MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).getSymbol()
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
    return MAPPER_SYMBOL.toNative(
        MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getSymbol()
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
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).setSymbol(MAPPER_STRING.toJava(symbol));
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel)
        .setSymbol(MAPPER_SYMBOL.toJava(symbol));
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel)
        .setSymbol(MAPPER_SYMBOL.toJava(symbol));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getLotSize",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_getLotSize(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getLotSize();
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
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).setLotSize(lotSize);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedEventModel_getSizeLimit",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedEventModel_getSizeLimit(
      final IsolateThread ignoreThread,
      final DxfgIndexedEventModel dxfgIndexedEventModel
  ) {
    return MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).getSizeLimit();
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
    MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).setSizeLimit(sizeLimit);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_getSizeLimit",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesEventModel_getSizeLimit(
      final IsolateThread ignoreThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    return MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getSizeLimit();
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
    MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).setSizeLimit(sizeLimit);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesEventModel_getFromTime",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static long dxfg_TimeSeriesEventModel_getFromTime(
      final IsolateThread ignoreThread,
      final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
  ) {
    return MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getFromTime();
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
    MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).setFromTime(fromTime);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getBuyOrders",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableListModel dxfg_OrderBookModel_getBuyOrders(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return MAPPER_OBSERVABLE_LIST_MODEL.toNative(
        MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getBuyOrders()
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
    return MAPPER_OBSERVABLE_LIST_MODEL.toNative(
        MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getSellOrders()
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
    return MAPPER_OBSERVABLE_LIST_MODEL.toNative(
        MAPPER_INDEXED_EVENT_MODEL.toJava(dxfgIndexedEventModel).getEventsList()
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
    return MAPPER_OBSERVABLE_LIST_MODEL.toNative(
        MAPPER_TIME_SERIES_EVENT_MODEL.toJava(dxfgTimeSeriesEventModel).getEventsList()
    );
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModel_addListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableListModel_addListener(
      final IsolateThread ignoreThread,
      final DxfgObservableListModel dxfgObservableListModel,
      final DxfgObservableListModelListener dxfgObservableListModelListener,
      final DxfgFinalizeFunction finalizeFunction,
      final VoidPointer userData
  ) {
    MAPPER_OBSERVABLE_LIST_MODEL.toJava(dxfgObservableListModel).addListener(
        FINALIZER.wrapObjectWithFinalizer(
            MAPPER_OBSERVABLE_LIST_MODEL_LISTENER.toJava(dxfgObservableListModelListener),
            () -> finalizeFunction.invoke(CurrentIsolate.getCurrentThread(), userData)
        )
    );
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_OBSERVABLE_LIST_MODEL.toJava(dxfgObservableListModel).removeListener(
        MAPPER_OBSERVABLE_LIST_MODEL_LISTENER.toJava(dxfgObservableListModelListener)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModel_toArray",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventTypeList dxfg_ObservableListModel_toArray(
      final IsolateThread ignoreThread,
      final DxfgObservableListModel dxfgObservableListModel
  ) {
    return MAPPER_EVENTS.toNativeList(
        MAPPER_OBSERVABLE_LIST_MODEL.toJava(dxfgObservableListModel)
    );
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_addListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_OrderBookModel_addListener(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgOrderBookModelListener dxfgOrderBookModelListener,
      final DxfgFinalizeFunction finalizeFunction,
      final VoidPointer userData
  ) {
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).addListener(
        FINALIZER.wrapObjectWithFinalizer(
            MAPPER_ORDER_BOOK_MODEL_LISTENER.toJava(dxfgOrderBookModelListener),
            () -> finalizeFunction.invoke(CurrentIsolate.getCurrentThread(), userData)
        )
    );
    return EXECUTE_SUCCESSFULLY;
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
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel)
        .removeListener(MAPPER_ORDER_BOOK_MODEL_LISTENER.toJava(dxfgOrderBookModelListener));
    return EXECUTE_SUCCESSFULLY;
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
    return MAPPER_ORDER_BOOK_MODEL_LISTENER.toNative(
        change -> {
          final DxfgOrderBookModel orderBookModel = MAPPER_ORDER_BOOK_MODEL.toNative(
              change.getSource());
          userFunc.invoke(
              CurrentIsolate.getCurrentThread(),
              orderBookModel,
              userData
          );
          MAPPER_ORDER_BOOK_MODEL.release(orderBookModel);
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
    return MAPPER_OBSERVABLE_LIST_MODEL_LISTENER.toNative(
        change -> {
          final DxfgEventTypeList dxfgOrderList = MAPPER_EVENTS.toNativeList(change.getSource());
          userFunc.invoke(
              CurrentIsolate.getCurrentThread(),
              dxfgOrderList,
              userData
          );
          MAPPER_EVENTS.release(dxfgOrderList);
        }
    );
  }
}
