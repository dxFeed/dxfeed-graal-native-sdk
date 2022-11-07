package com.dxfeed.api.subscription;

import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.events.DxfgEventPointer;
import com.dxfeed.api.events.DxfgCCharPointerPointer;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.event.EventType;
import com.dxfeed.event.market.ListEventMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public class SubscriptionNative extends BaseNative {

  private static final ListEventMapper EVENT_MAPPER = new ListEventMapper();

  public DXFeedSubscription<?> constructorDXFeedSubscription(
      final Class<?> eventType
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public DXFeedSubscription<?> constructorDXFeedSubscription(
      final Class<?>... eventTypes
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public void attach(final DXFeed feed) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public void detach(final DXFeed feed) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public boolean isClosed() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  @CEntryPoint(
      name = "dxfg_subscription_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int close(
      final IsolateThread ignoredThread,
      final DxfgSubscription dxfgSubscription
  ) {
    getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler()).close();
    return EXECUTE_SUCCESSFULLY;
  }

  public Set<Class<?>> getEventTypes() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public boolean containsEventType(
      final Class<?> eventType
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  @CEntryPoint(
      name = "dxfg_subscription_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int clear(
      final IsolateThread ignoreThread,
      final DxfgSubscription dxfgSubscription
  ) {
    getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler()).clear();
    return EXECUTE_SUCCESSFULLY;
  }

  public Set<?> getSymbols() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public Set<?> getDecoratedSymbols() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public void setSymbols(
      final Collection<?> symbols
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public void setSymbols(
      final Object... symbols
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  @CEntryPoint(
      name = "dxfg_subscription_add_symbols",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int addSymbols(
      final IsolateThread ignoreThread,
      final DxfgSubscription dxfgSubscription,
      final DxfgCCharPointerPointer dxfgCCharPointerPointer,
      final int symbolsSize
  ) {
    final List<String> listSymbols = new ArrayList<>(symbolsSize);
    for (int i = 0; i < symbolsSize; ++i) {
      listSymbols.add(toJavaString(dxfgCCharPointerPointer.addressOf(i).read()));
    }
    getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler())
        .addSymbols(listSymbols);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_subscription_add_symbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int addSymbols(
      final IsolateThread ignoreThread,
      final DxfgSubscription dxfgSubscription,
      final CCharPointer charPointer
  ) {
    getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler())
        .addSymbols(toJavaString(charPointer));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_subscription_remove_symbols",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int removeSymbols(
      final IsolateThread ignoreThread,
      final DxfgSubscription dxfgSubscription,
      final DxfgCCharPointerPointer dxfgCCharPointerPointer,
      final int symbolsSize
  ) {
    final List<String> listSymbols = new ArrayList<>(symbolsSize);
    for (int i = 0; i < symbolsSize; ++i) {
      listSymbols.add(toJavaString(dxfgCCharPointerPointer.addressOf(i).read()));
    }
    getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler())
        .removeSymbols(listSymbols);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_subscription_remove_symbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int removeSymbol(
      final IsolateThread ignoreThread,
      final DxfgSubscription dxfgSubscription,
      final CCharPointer charPointer
  ) {
    getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler())
        .removeSymbols(toJavaString(charPointer));
    return EXECUTE_SUCCESSFULLY;
  }

  public Executor getExecutor() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public void setExecutor(
      final Executor executor
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  @CEntryPoint(
      name = "dxfg_subscription_add_event_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int addEventListener(
      final IsolateThread ignoreThread,
      final DxfgSubscription dxfgSubscription,
      final DxfgEventListener dxfgEventListener
  ) {
    final DXFeedEventListener<EventType<?>> listener = events -> {
      final DxfgEventPointer nativeEvents = EVENT_MAPPER.nativeObject(events);
      final IsolateThread currentThread = CurrentIsolate.getCurrentThread();
      final int size = events.size();
      dxfgEventListener.getEventListener().invoke(currentThread, nativeEvents, size);
      EVENT_MAPPER.delete(nativeEvents, size);
    };
    dxfgEventListener.setJavaObjectHandler(createJavaObjectHandler(listener));
    getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler())
        .addEventListener(listener);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_subscription_remove_event_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int removeEventListener(
      final IsolateThread ignoreThread,
      final DxfgSubscription dxfgSubscription,
      final DxfgEventListener dxfgEventListener
  ) {
    getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler())
        .removeEventListener(
            getDxFeedEventListener(
                dxfgEventListener.getJavaObjectHandler()
            )
        );
    destroyJavaObjectHandler(dxfgEventListener.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  public synchronized void addChangeListener(
      final ObservableSubscriptionChangeListener listener
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public synchronized void removeChangeListener(
      final ObservableSubscriptionChangeListener listener
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }
}
