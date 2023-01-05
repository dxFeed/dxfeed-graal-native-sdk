package com.dxfeed.api.model;

import static com.dxfeed.api.NativeUtils.MAPPER_EXECUTOR;
import static com.dxfeed.api.NativeUtils.MAPPER_FEED;
import static com.dxfeed.api.NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_OBSERVABLE_LIST_MODEL_ORDER;
import static com.dxfeed.api.NativeUtils.MAPPER_ORDERS;
import static com.dxfeed.api.NativeUtils.MAPPER_ORDER_BOOK_MODEL;
import static com.dxfeed.api.NativeUtils.MAPPER_ORDER_BOOK_MODEL_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.events.DxfgOrderList;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.model.market.OrderBookModelFilter;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class OrderBookModelNative {

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
      name = "dxfg_OrderBookModel_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgOrderBookModel dxfgOrderBookModel,
      final DxfgExecuter dxfgExecuter
  ) {
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel)
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
      name = "dxfg_OrderBookModel_getBuyOrders",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableListModelOrder dxfg_OrderBookModel_getBuyOrders(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return MAPPER_OBSERVABLE_LIST_MODEL_ORDER.toNative(
        MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getBuyOrders()
    );
  }

  @CEntryPoint(
      name = "dxfg_OrderBookModel_getSellOrders",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableListModelOrder dxfg_OrderBookModel_getSellOrders(
      final IsolateThread ignoreThread,
      final DxfgOrderBookModel dxfgOrderBookModel
  ) {
    return MAPPER_OBSERVABLE_LIST_MODEL_ORDER.toNative(
        MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel).getSellOrders()
    );
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModelOrder_addListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableListModelOrder_addListener(
      final IsolateThread ignoreThread,
      final DxfgObservableListModelOrder dxfgObservableListModelOrder,
      final DxfgObservableListModelListener dxfgObservableListModelListener
  ) {
    MAPPER_OBSERVABLE_LIST_MODEL_ORDER.toJava(dxfgObservableListModelOrder)
        .addListener(MAPPER_OBSERVABLE_LIST_MODEL_LISTENER.toJava(dxfgObservableListModelListener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModelOrder_removeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ObservableListModelOrder_removeListener(
      final IsolateThread ignoreThread,
      final DxfgObservableListModelOrder dxfgObservableListModelOrder,
      final DxfgObservableListModelListener dxfgObservableListModelListener
  ) {
    MAPPER_OBSERVABLE_LIST_MODEL_ORDER.toJava(dxfgObservableListModelOrder)
        .removeListener(
            MAPPER_OBSERVABLE_LIST_MODEL_LISTENER.toJava(dxfgObservableListModelListener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_ObservableListModelOrder_toArray",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgOrderList dxfg_ObservableListModelOrder_toArray(
      final IsolateThread ignoreThread,
      final DxfgObservableListModelOrder dxfgObservableListModelOrder
  ) {
    return MAPPER_ORDERS.toNativeList(
        MAPPER_OBSERVABLE_LIST_MODEL_ORDER.toJava(dxfgObservableListModelOrder)
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
    MAPPER_ORDER_BOOK_MODEL.toJava(dxfgOrderBookModel)
        .addListener(MAPPER_ORDER_BOOK_MODEL_LISTENER.toJava(dxfgOrderBookModelListener));
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
        change -> userFunc.invoke(
            CurrentIsolate.getCurrentThread(),
            MAPPER_ORDER_BOOK_MODEL.toNative(change.getSource()),
            userData
        )
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
          userFunc.invoke(
              CurrentIsolate.getCurrentThread(),
              MAPPER_ORDERS.toNativeList(change.getSource()),
              userData
          );
        }
    );
  }
}
