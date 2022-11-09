package com.dxfeed.api.subscription;

import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.events.DxfgCCharPointerPointer;
import com.dxfeed.api.events.DxfgEventPointer;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.event.EventType;
import com.dxfeed.event.market.CandleExchangeMapper;
import com.dxfeed.event.market.CandleMapper;
import com.dxfeed.event.market.CandlePeriodMapper;
import com.dxfeed.event.market.CandlePriceLevelMapper;
import com.dxfeed.event.market.CandleSymbolMapper;
import com.dxfeed.event.market.ListEventMapper;
import com.dxfeed.event.market.QuoteMapper;
import com.dxfeed.event.market.StringMapper;
import com.dxfeed.event.market.TimeAndSaleMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class SubscriptionNative extends BaseNative {

  private static final ListEventMapper EVENT_MAPPER;
  private static final Map<Long, DXFeedEventListener<EventType<?>>> EVENT_LISTENERS = new HashMap<>();

  static {
    final StringMapper stringMapper = new StringMapper();
    EVENT_MAPPER = new ListEventMapper(
        new QuoteMapper(stringMapper),
        new TimeAndSaleMapper(stringMapper),
        new CandleMapper(
            new CandleSymbolMapper(
                stringMapper,
                new CandlePeriodMapper(stringMapper),
                new CandleExchangeMapper(),
                new CandlePriceLevelMapper()
            )
        )
    );
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

  @CEntryPoint(
      name = "dxfg_subscription_add_event_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int addEventListener(
      final IsolateThread ignoreThread,
      final DxfgSubscription dxfgSubscription,
      final DxfgEventListener dxfgEventListener,
      final VoidPointer userData
  ) {
    if (!EVENT_LISTENERS.containsKey(dxfgEventListener.rawValue())) {
      final DXFeedEventListener<EventType<?>> listener = events -> {
        final DxfgEventPointer nativeEvents = EVENT_MAPPER.nativeObject(events);
        final IsolateThread currentThread = CurrentIsolate.getCurrentThread();
        final int size = events.size();
        dxfgEventListener.invoke(currentThread, nativeEvents, size, userData);
        EVENT_MAPPER.delete(nativeEvents, size);
      };
      EVENT_LISTENERS.put(dxfgEventListener.rawValue(), listener);
      getDxFeedSubscription(dxfgSubscription.getJavaObjectHandler()).addEventListener(listener);
    }
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
            EVENT_LISTENERS.remove(dxfgEventListener.rawValue())
        );
    return EXECUTE_SUCCESSFULLY;
  }

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

  public Set<Class<?>> getEventTypes() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public boolean containsEventType(
      final Class<?> eventType
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public Set<?> getSymbols() {
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

  public Set<?> getDecoratedSymbols() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public Executor getExecutor() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public void setExecutor(
      final Executor executor
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
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
