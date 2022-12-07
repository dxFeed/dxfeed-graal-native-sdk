package com.dxfeed.api.subscription;

import static com.dxfeed.api.NativeUtils.MAPPER_EVENTS;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENT_TYPES;
import static com.dxfeed.api.NativeUtils.MAPPER_EXECUTOR;
import static com.dxfeed.api.NativeUtils.MAPPER_FEED;
import static com.dxfeed.api.NativeUtils.MAPPER_FEED_EVENT_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_SUBSCRIPTION;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOL;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOLS;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgEventClazzList;
import com.dxfeed.api.events.DxfgEventTypeList;
import com.dxfeed.api.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.api.events.DxfgObservableSubscriptionChangeListenerFunctionSubscriptionClosed;
import com.dxfeed.api.events.DxfgObservableSubscriptionChangeListenerFunctionSymbolsAdded;
import com.dxfeed.api.events.DxfgObservableSubscriptionChangeListenerFunctionSymbolsRemoved;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.api.symbol.DxfgSymbol;
import com.dxfeed.api.symbol.DxfgSymbolList;
import com.dxfeed.event.EventType;
import java.util.Set;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class SubscriptionNative {

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfg_DXFeedSubscription_new(
      final IsolateThread ignoredThread,
      final DxfgEventClazz dxfgClazz
  ) {
    return MAPPER_SUBSCRIPTION.toNative(new DXFeedSubscription<>(dxfgClazz.clazz));
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_new2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfg_DXFeedSubscription_new(
      final IsolateThread ignoredThread,
      final DxfgEventClazzList eventClazzList
  ) {
    return MAPPER_SUBSCRIPTION.toNative(
        new DXFeedSubscription<>(
            MAPPER_EVENT_TYPES.toJavaList(eventClazzList).toArray(new Class[0])
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_close(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).close();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_clear(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).clear();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_addSymbols",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_addSymbols(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgSymbolList symbols
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).addSymbols(MAPPER_SYMBOLS.toJavaList(symbols));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_addSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_addSymbol(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgSymbol dxfgSymbol
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).addSymbols(MAPPER_SYMBOL.toJava(dxfgSymbol));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_removeSymbols",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_removeSymbols(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgSymbolList symbols
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).removeSymbols(MAPPER_SYMBOLS.toJavaList(symbols));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_removeSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_removeSymbol(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgSymbol dxfgSymbol
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).removeSymbols(MAPPER_SYMBOL.toJava(dxfgSymbol));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedEventListener_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgFeedEventListener dxfg_DXFeedEventListener_new(
      final IsolateThread ignoreThread,
      final DxfgFeedEventListenerFunction dxfgFeedEventListenerFunction,
      final VoidPointer userData
  ) {
    return MAPPER_FEED_EVENT_LISTENER.toNative(events -> {
      final DxfgEventTypeList dxfgEventTypeList = MAPPER_EVENTS.toNativeList(events);
      dxfgFeedEventListenerFunction.invoke(
          CurrentIsolate.getCurrentThread(),
          dxfgEventTypeList,
          userData
      );
      MAPPER_EVENTS.release(dxfgEventTypeList);
    });
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_addEventListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_addEventListener(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgFeedEventListener dxfgFeedEventListener
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription)
        .addEventListener(MAPPER_FEED_EVENT_LISTENER.toJava(dxfgFeedEventListener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_removeEventListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_removeEventListener(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgFeedEventListener dxfgFeedEventListener
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription)
        .removeEventListener(MAPPER_FEED_EVENT_LISTENER.toJava(dxfgFeedEventListener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_attach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_attach(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgFeed feed
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).attach(MAPPER_FEED.toJava(feed));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_detach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_detach(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgFeed feed
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).detach(MAPPER_FEED.toJava(feed));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_isClosed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_isClosed(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).isClosed() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_getEventTypes",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventClazzList dxfg_DXFeedSubscription_getEventTypes(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return MAPPER_EVENT_TYPES.toNativeList(
        MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).getEventTypes()
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_containsEventType",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_containsEventType(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgEventClazz dxfgClazz
  ) {
    return MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).containsEventType(dxfgClazz.clazz) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_getSymbols",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSymbolList dxfg_DXFeedSubscription_getSymbols(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return MAPPER_SYMBOLS.toNativeList(MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).getSymbols());
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_setSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_setSymbol(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgSymbol dxfgSymbol
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).setSymbols(MAPPER_SYMBOL.toJava(dxfgSymbol));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_setSymbols",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_setSymbols(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgSymbolList dxfgSymbolList
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription)
        .setSymbols(MAPPER_SYMBOLS.toJavaList(dxfgSymbolList));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_getDecoratedSymbols",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSymbolList dxfg_DXFeedSubscription_getDecoratedSymbols(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return MAPPER_SYMBOLS.toNativeList(
        MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).getDecoratedSymbols());
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_DXFeedSubscription_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return MAPPER_EXECUTOR.toNative(MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).getExecutor());
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgExecuter dxfgExecuter
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).setExecutor(MAPPER_EXECUTOR.toJava(dxfgExecuter));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_ObservableSubscriptionChangeListener_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgObservableSubscriptionChangeListener dxfg_ObservableSubscriptionChangeListener_new(
      final IsolateThread ignoreThread,
      final DxfgObservableSubscriptionChangeListenerFunctionSymbolsAdded functionSymbolsAdded,
      final DxfgObservableSubscriptionChangeListenerFunctionSymbolsRemoved functionSymbolsRemoved,
      final DxfgObservableSubscriptionChangeListenerFunctionSubscriptionClosed functionSubscriptionClosed,
      final VoidPointer userData
  ) {
    return MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toNative(
        new ObservableSubscriptionChangeListener() {

          @Override
          public void symbolsAdded(final Set<?> symbols) {
            functionSymbolsAdded.invoke(
                CurrentIsolate.getCurrentThread(),
                MAPPER_SYMBOLS.toNativeList(symbols),
                userData
            );
          }

          @Override
          public void symbolsRemoved(final Set<?> symbols) {
            functionSymbolsRemoved.invoke(
                CurrentIsolate.getCurrentThread(),
                MAPPER_SYMBOLS.toNativeList(symbols),
                userData
            );
          }

          @Override
          public void subscriptionClosed() {
            functionSubscriptionClosed.invoke(
                CurrentIsolate.getCurrentThread(),
                userData
            );
          }
        }
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_addChangeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_addChangeListener(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).addChangeListener(
        MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toJava(dxfgFeedEventListener)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_removeChangeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_removeChangeListener(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
  ) {
    MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).removeChangeListener(
        MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toJava(dxfgFeedEventListener)
    );
    return EXECUTE_SUCCESSFULLY;
  }
}
