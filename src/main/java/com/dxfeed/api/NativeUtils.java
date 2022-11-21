package com.dxfeed.api;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.dxfeed.api.events.DxfgEventClazzList;
import com.dxfeed.api.events.DxfgEventClazzPointer;
import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.api.events.DxfgEventTypeList;
import com.dxfeed.api.events.DxfgEventTypePointer;
import com.dxfeed.api.events.DxfgSymbol;
import com.dxfeed.api.events.DxfgSymbolList;
import com.dxfeed.api.events.DxfgSymbolPointer;
import com.dxfeed.api.exception.DxfgException;
import com.dxfeed.api.feed.DxfgPromise;
import com.dxfeed.api.feed.DxfgPromiseList;
import com.dxfeed.api.feed.DxfgPromisePointer;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerPointer;
import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.event.EventType;
import com.dxfeed.event.market.AnalyticOrderMapper;
import com.dxfeed.event.market.CandleExchangeMapper;
import com.dxfeed.event.market.CandleMapper;
import com.dxfeed.event.market.CandlePeriodMapper;
import com.dxfeed.event.market.CandlePriceLevelMapper;
import com.dxfeed.event.market.CandleSymbolMapper;
import com.dxfeed.event.market.ConfigurationMapper;
import com.dxfeed.event.market.DailyCandleMapper;
import com.dxfeed.event.market.EventMappers;
import com.dxfeed.event.market.ExceptionMapper;
import com.dxfeed.event.market.GreeksMapper;
import com.dxfeed.event.market.ListEventMapper;
import com.dxfeed.event.market.ListEventTypeMapper;
import com.dxfeed.event.market.ListJavaObjectHandlerMapper;
import com.dxfeed.event.market.ListMapper;
import com.dxfeed.event.market.ListPromiseMapper;
import com.dxfeed.event.market.ListSymbolMapper;
import com.dxfeed.event.market.Mapper;
import com.dxfeed.event.market.MessageMapper;
import com.dxfeed.event.market.OrderBaseMapper;
import com.dxfeed.event.market.OrderMapper;
import com.dxfeed.event.market.ProfileMapper;
import com.dxfeed.event.market.QuoteMapper;
import com.dxfeed.event.market.SeriesMapper;
import com.dxfeed.event.market.SpreadOrderMapper;
import com.dxfeed.event.market.StringMapper;
import com.dxfeed.event.market.StringMapperCacheStore;
import com.dxfeed.event.market.StringMapperUnlimitedStore;
import com.dxfeed.event.market.SummaryMapper;
import com.dxfeed.event.market.SymbolMapper;
import com.dxfeed.event.market.TheoPriceMapper;
import com.dxfeed.event.market.TimeAndSaleMapper;
import com.dxfeed.event.market.TradeETHMapper;
import com.dxfeed.event.market.TradeMapper;
import com.dxfeed.event.market.UnderlyingMapper;
import com.dxfeed.promise.Promise;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

public final class NativeUtils {

  public static final Mapper<Throwable, DxfgException> MAPPER_EXCEPTION = new ExceptionMapper(
      new StringMapper());
  public static final Mapper<EventType<?>, DxfgEventType> MAPPER_EVENT;
  public static final ListMapper<EventType<?>, DxfgEventType, DxfgEventTypePointer, DxfgEventTypeList> MAPPER_EVENTS;
  public static final Mapper<String, CCharPointer> MAPPER_STRING = new StringMapper();
  public static final Mapper<String, CCharPointer> MAPPER_STRING_UNLIMITED_STORE = new StringMapperUnlimitedStore();
  public static final Mapper<String, CCharPointer> MAPPER_STRING_CACHE_STORE = new StringMapperCacheStore(
      3000);
  public static final Mapper<Object, DxfgSymbol> MAPPER_SYMBOL = new SymbolMapper(
      MAPPER_STRING_UNLIMITED_STORE);
  public static final ListMapper<Object, DxfgSymbol, DxfgSymbolPointer, DxfgSymbolList> MAPPER_SYMBOLS = new ListSymbolMapper(
      MAPPER_SYMBOL);
  public static final ListMapper<Class<? extends EventType<?>>, CIntPointer, DxfgEventClazzPointer, DxfgEventClazzList> MAPPER_ENEVET_TYPES = new ListEventTypeMapper();
  public static final ListMapper<Object, JavaObjectHandler<?>, DxfgJavaObjectHandlerPointer, DxfgJavaObjectHandlerList> MAPPER_JAVA_OBJECT_HANDLERS = new ListJavaObjectHandlerMapper();
  public static final ListMapper<Promise<?>, DxfgPromise<?>, DxfgPromisePointer, DxfgPromiseList> MAPPER_PROMISES = new ListPromiseMapper();

  static {
    SingletonScheduledExecutorService.start(
        () -> ((StringMapperCacheStore) MAPPER_STRING_CACHE_STORE).cleanUp(), 1000);
    final CandleSymbolMapper candleSymbolMapper = new CandleSymbolMapper(
        MAPPER_STRING_UNLIMITED_STORE,
        new CandlePeriodMapper(MAPPER_STRING_CACHE_STORE),
        new CandleExchangeMapper(),
        new CandlePriceLevelMapper()
    );
    MAPPER_EVENT = new EventMappers(
        MAPPER_STRING_UNLIMITED_STORE,
        new QuoteMapper(MAPPER_STRING_UNLIMITED_STORE),
        new SeriesMapper(MAPPER_STRING_UNLIMITED_STORE),
        new TimeAndSaleMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
        new SpreadOrderMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
        new OrderMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
        new AnalyticOrderMapper(MAPPER_STRING_UNLIMITED_STORE),
        new MessageMapper(MAPPER_STRING),
        new OrderBaseMapper(MAPPER_STRING_UNLIMITED_STORE),
        new ConfigurationMapper(MAPPER_STRING),
        new TradeMapper(MAPPER_STRING_UNLIMITED_STORE),
        new TradeETHMapper(MAPPER_STRING_UNLIMITED_STORE),
        new TheoPriceMapper(MAPPER_STRING_UNLIMITED_STORE),
        new UnderlyingMapper(MAPPER_STRING_UNLIMITED_STORE),
        new GreeksMapper(MAPPER_STRING_UNLIMITED_STORE),
        new SummaryMapper(MAPPER_STRING_UNLIMITED_STORE),
        new ProfileMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
        new DailyCandleMapper(candleSymbolMapper),
        new CandleMapper(candleSymbolMapper)
    );
    MAPPER_EVENTS = new ListEventMapper(MAPPER_EVENT);
  }

  public static <V extends JavaObjectHandler<?>> V newJavaObjectHandler(final Object object) {
    final V javaObjectHandler = UnmanagedMemory.calloc(SizeOf.get(JavaObjectHandler.class));
    javaObjectHandler.setJavaObjectHandler(ObjectHandles.getGlobal().create(object));
    return javaObjectHandler;
  }

  public static <T> T toJava(final JavaObjectHandler<T> javaObjectHandler) {
    return ObjectHandles.getGlobal().get(javaObjectHandler.getJavaObjectHandler());
  }

  public static void release(final JavaObjectHandler<?> javaObjectHandler) {
    ObjectHandles.getGlobal().destroy(javaObjectHandler.getJavaObjectHandler());
    UnmanagedMemory.free(javaObjectHandler);
  }
}

class SingletonScheduledExecutorService {

  private static volatile ScheduledExecutorService instance;

  public static void start(final Runnable task, final int periodInMs) {
    if (instance == null) {
      synchronized (SingletonScheduledExecutorService.class) {
        if (instance == null) {
          instance = Executors.newScheduledThreadPool(1, runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("clean cache");
            return thread;
          });
          instance.scheduleAtFixedRate(task, periodInMs, periodInMs, MILLISECONDS);
        }
      }
    }
  }
}
