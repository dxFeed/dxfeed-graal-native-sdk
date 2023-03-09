package com.dxfeed.sdk.subscription;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventTypeList;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListenerFunctionSubscriptionClosed;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListenerFunctionSymbolsAdded;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListenerFunctionSymbolsRemoved;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecuter;
import com.dxfeed.sdk.javac.DxfgFinalizeFunction;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolList;
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
    return NativeUtils.MAPPER_SUBSCRIPTION.toNative(new DXFeedSubscription<>(dxfgClazz.clazz));
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_new2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfg_DXFeedSubscription_new(
      final IsolateThread ignoredThread,
      final DxfgEventClazzList eventClazzList
  ) {
    return NativeUtils.MAPPER_SUBSCRIPTION.toNative(
        new DXFeedSubscription<>(
            NativeUtils.MAPPER_EVENT_TYPES.toJavaList(eventClazzList).toArray(new Class[0])
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).close();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_clear(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).clear();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).addSymbols(NativeUtils.MAPPER_SYMBOLS.toJavaList(symbols));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).addSymbols(NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).removeSymbols(NativeUtils.MAPPER_SYMBOLS.toJavaList(symbols));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).removeSymbols(NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    return NativeUtils.MAPPER_FEED_EVENT_LISTENER.toNative(events -> {
      final DxfgEventTypeList dxfgEventTypeList = NativeUtils.MAPPER_EVENTS.toNativeList(events);
      dxfgFeedEventListenerFunction.invoke(
          CurrentIsolate.getCurrentThread(),
          dxfgEventTypeList,
          userData
      );
      NativeUtils.MAPPER_EVENTS.release(dxfgEventTypeList);
    });
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_addEventListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_addEventListener(
      final IsolateThread ignoreThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
      final DxfgFeedEventListener dxfgFeedEventListener,
      final DxfgFinalizeFunction finalizeFunction,
      final VoidPointer userData
  ) {
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).addEventListener(
        finalizeFunction.isNull()
            ? NativeUtils.MAPPER_FEED_EVENT_LISTENER.toJava(dxfgFeedEventListener)
            : NativeUtils.FINALIZER.wrapObjectWithFinalizer(
                NativeUtils.MAPPER_FEED_EVENT_LISTENER.toJava(dxfgFeedEventListener),
                () -> finalizeFunction.invoke(CurrentIsolate.getCurrentThread(), userData)
            )
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription)
        .removeEventListener(NativeUtils.MAPPER_FEED_EVENT_LISTENER.toJava(dxfgFeedEventListener));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).attach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).detach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_isClosed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedSubscription_isClosed(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).isClosed() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_getEventTypes",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventClazzList dxfg_DXFeedSubscription_getEventTypes(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return NativeUtils.MAPPER_EVENT_TYPES.toNativeList(
        NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).getEventTypes()
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
    return NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).containsEventType(dxfgClazz.clazz) ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_getSymbols",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSymbolList dxfg_DXFeedSubscription_getSymbols(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return NativeUtils.MAPPER_SYMBOLS.toNativeList(NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).getSymbols());
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).setSymbols(NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription)
        .setSymbols(NativeUtils.MAPPER_SYMBOLS.toJavaList(dxfgSymbolList));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_getDecoratedSymbols",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSymbolList dxfg_DXFeedSubscription_getDecoratedSymbols(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return NativeUtils.MAPPER_SYMBOLS.toNativeList(
        NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).getDecoratedSymbols());
  }

  @CEntryPoint(
      name = "dxfg_DXFeedSubscription_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_DXFeedSubscription_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).getExecutor());
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).setExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(dxfgExecuter));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    return NativeUtils.MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toNative(
        new ObservableSubscriptionChangeListener() {

          @Override
          public void symbolsAdded(final Set<?> symbols) {
            final DxfgSymbolList dxfgSymbolList = NativeUtils.MAPPER_SYMBOLS.toNativeList(symbols);
            functionSymbolsAdded.invoke(
                CurrentIsolate.getCurrentThread(),
                dxfgSymbolList,
                userData
            );
            NativeUtils.MAPPER_SYMBOLS.release(dxfgSymbolList);
          }

          @Override
          public void symbolsRemoved(final Set<?> symbols) {
            final DxfgSymbolList dxfgSymbolList = NativeUtils.MAPPER_SYMBOLS.toNativeList(symbols);
            functionSymbolsRemoved.invoke(
                CurrentIsolate.getCurrentThread(),
                dxfgSymbolList,
                userData
            );
            NativeUtils.MAPPER_SYMBOLS.release(dxfgSymbolList);
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
      final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener,
      final DxfgFinalizeFunction finalizeFunction,
      final VoidPointer userData
  ) {
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).addChangeListener(
        finalizeFunction.isNull()
            ? NativeUtils.MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toJava(dxfgFeedEventListener)
            : NativeUtils.FINALIZER.wrapObjectWithFinalizer(
                NativeUtils.MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toJava(dxfgFeedEventListener),
                () -> finalizeFunction.invoke(CurrentIsolate.getCurrentThread(), userData)
            )
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_SUBSCRIPTION.toJava(dxfgSubscription).removeChangeListener(
        NativeUtils.MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toJava(dxfgFeedEventListener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }
}
