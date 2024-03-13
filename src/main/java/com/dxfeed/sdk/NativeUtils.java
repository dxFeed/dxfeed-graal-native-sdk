package com.dxfeed.sdk;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.devexperts.util.TimeFormat;
import com.devexperts.util.TimePeriod;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.Builder;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.osub.ObservableSubscription;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.event.market.AnalyticOrderMapper;
import com.dxfeed.event.market.CandleMapper;
import com.dxfeed.event.market.ConfigurationMapper;
import com.dxfeed.event.market.DailyCandleMapper;
import com.dxfeed.event.market.EventMappers;
import com.dxfeed.event.market.GreeksMapper;
import com.dxfeed.event.market.ListEventMapper;
import com.dxfeed.event.market.ListEventTypeMapper;
import com.dxfeed.event.market.MessageMapper;
import com.dxfeed.event.market.OptionSaleMapper;
import com.dxfeed.event.market.OrderBaseMapper;
import com.dxfeed.event.market.OrderMapper;
import com.dxfeed.event.market.OtcMarketsOrderMapper;
import com.dxfeed.event.market.ProfileMapper;
import com.dxfeed.event.market.QuoteMapper;
import com.dxfeed.event.market.SeriesMapper;
import com.dxfeed.event.market.SpreadOrderMapper;
import com.dxfeed.event.market.SummaryMapper;
import com.dxfeed.event.market.TheoPriceMapper;
import com.dxfeed.event.market.TimeAndSaleMapper;
import com.dxfeed.event.market.TradeETHMapper;
import com.dxfeed.event.market.TradeMapper;
import com.dxfeed.event.market.UnderlyingMapper;
import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.ipf.InstrumentProfileReader;
import com.dxfeed.ipf.live.InstrumentProfileCollector;
import com.dxfeed.ipf.live.InstrumentProfileConnection;
import com.dxfeed.ipf.live.InstrumentProfileUpdateListener;
import com.dxfeed.model.IndexedEventModel;
import com.dxfeed.model.ObservableListModel;
import com.dxfeed.model.ObservableListModelListener;
import com.dxfeed.model.TimeSeriesEventModel;
import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.model.market.OrderBookModelListener;
import com.dxfeed.ondemand.OnDemandService;
import com.dxfeed.promise.Promise;
import com.dxfeed.schedule.Day;
import com.dxfeed.schedule.DayFilter;
import com.dxfeed.schedule.Schedule;
import com.dxfeed.schedule.Session;
import com.dxfeed.schedule.SessionFilter;
import com.dxfeed.sdk.endpoint.DxfgEndpoint;
import com.dxfeed.sdk.endpoint.DxfgEndpointBuilder;
import com.dxfeed.sdk.endpoint.DxfgEndpointStateChangeListener;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventClazzPointer;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.events.DxfgEventTypeList;
import com.dxfeed.sdk.events.DxfgEventTypePointer;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.sdk.exception.DxfgException;
import com.dxfeed.sdk.exception.DxfgStackTraceElement;
import com.dxfeed.sdk.exception.DxfgStackTraceElementList;
import com.dxfeed.sdk.exception.DxfgStackTraceElementPointer;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.feed.DxfgPromise;
import com.dxfeed.sdk.feed.DxfgPromiseList;
import com.dxfeed.sdk.feed.DxfgPromisePointer;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileCollector;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileConnection;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileList;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfilePointer;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileReader;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileUpdateListener;
import com.dxfeed.sdk.ipf.DxfgIpfConnectionStateChangeListener;
import com.dxfeed.sdk.ipf.DxfgIterableInstrumentProfile;
import com.dxfeed.sdk.javac.DxfgCharPointerList;
import com.dxfeed.sdk.javac.DxfgCharPointerPointer;
import com.dxfeed.sdk.javac.DxfgExecuter;
import com.dxfeed.sdk.javac.DxfgInputStream;
import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerPointer;
import com.dxfeed.sdk.javac.DxfgTimeFormat;
import com.dxfeed.sdk.javac.DxfgTimePeriod;
import com.dxfeed.sdk.javac.DxfgTimeZone;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import com.dxfeed.sdk.maper.DayFilterMapper;
import com.dxfeed.sdk.maper.DayMapper;
import com.dxfeed.sdk.maper.DxfgStackTraceElementMapper;
import com.dxfeed.sdk.maper.EndpointBuilderMapper;
import com.dxfeed.sdk.maper.EndpointMapper;
import com.dxfeed.sdk.maper.EndpointStateChangeListenerMapper;
import com.dxfeed.sdk.maper.ExceptionMapper;
import com.dxfeed.sdk.maper.ExecutorMapper;
import com.dxfeed.sdk.maper.FeedEventListenerMapper;
import com.dxfeed.sdk.maper.FeedMapper;
import com.dxfeed.sdk.maper.IndexedEventModelMapper;
import com.dxfeed.sdk.maper.InputStreamMapper;
import com.dxfeed.sdk.maper.InstrumentProfileCollectorMapper;
import com.dxfeed.sdk.maper.InstrumentProfileConnectionMapper;
import com.dxfeed.sdk.maper.InstrumentProfileMapper;
import com.dxfeed.sdk.maper.InstrumentProfileReaderMapper;
import com.dxfeed.sdk.maper.InstrumentProfileUpdateListenerMapper;
import com.dxfeed.sdk.maper.IpfConnectionStateChangeListenerMapper;
import com.dxfeed.sdk.maper.IterableInstrumentProfileMapper;
import com.dxfeed.sdk.maper.JavaObjectHandlerMapper;
import com.dxfeed.sdk.maper.ListInstrumentProfileMapper;
import com.dxfeed.sdk.maper.ListJavaObjectHandlerMapper;
import com.dxfeed.sdk.maper.ListMapper;
import com.dxfeed.sdk.maper.ListPromiseMapper;
import com.dxfeed.sdk.maper.ListSessionMapper;
import com.dxfeed.sdk.maper.ListStackTraceElementMapper;
import com.dxfeed.sdk.maper.ListStringsMapper;
import com.dxfeed.sdk.maper.Mapper;
import com.dxfeed.sdk.maper.ObservableListModelListenerMapper;
import com.dxfeed.sdk.maper.ObservableListModelMapper;
import com.dxfeed.sdk.maper.ObservableSubscriptionChangeListenerMapper;
import com.dxfeed.sdk.maper.ObservableSubscriptionMapper;
import com.dxfeed.sdk.maper.OnDemandServiceMapper;
import com.dxfeed.sdk.maper.OrderBookModelListenerMapper;
import com.dxfeed.sdk.maper.OrderBookModelMapper;
import com.dxfeed.sdk.maper.PromiseMapper;
import com.dxfeed.sdk.maper.PublisherMapper;
import com.dxfeed.sdk.maper.ScheduleMapper;
import com.dxfeed.sdk.maper.SessionFilterMapper;
import com.dxfeed.sdk.maper.SessionMapper;
import com.dxfeed.sdk.maper.StringMapper;
import com.dxfeed.sdk.maper.StringMapperCacheStore;
import com.dxfeed.sdk.maper.SubscriptionMapper;
import com.dxfeed.sdk.maper.TimeFormatMapper;
import com.dxfeed.sdk.maper.TimePeriodMapper;
import com.dxfeed.sdk.maper.TimeSeriesEventModelMapper;
import com.dxfeed.sdk.maper.TimeSeriesSubscriptionMapper;
import com.dxfeed.sdk.maper.TimeZoneMapper;
import com.dxfeed.sdk.model.DxfgIndexedEventModel;
import com.dxfeed.sdk.model.DxfgObservableListModel;
import com.dxfeed.sdk.model.DxfgObservableListModelListener;
import com.dxfeed.sdk.model.DxfgOrderBookModel;
import com.dxfeed.sdk.model.DxfgOrderBookModelListener;
import com.dxfeed.sdk.model.DxfgTimeSeriesEventModel;
import com.dxfeed.sdk.ondemand.DxfgOnDemandService;
import com.dxfeed.sdk.publisher.DxfgObservableSubscription;
import com.dxfeed.sdk.publisher.DxfgPublisher;
import com.dxfeed.sdk.schedule.DxfgDay;
import com.dxfeed.sdk.schedule.DxfgDayFilter;
import com.dxfeed.sdk.schedule.DxfgSchedule;
import com.dxfeed.sdk.schedule.DxfgSession;
import com.dxfeed.sdk.schedule.DxfgSessionFilter;
import com.dxfeed.sdk.schedule.DxfgSessionList;
import com.dxfeed.sdk.schedule.DxfgSessionPointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import com.dxfeed.sdk.source.IndexedEventSourceMapper;
import com.dxfeed.sdk.subscription.DxfgFeedEventListener;
import com.dxfeed.sdk.subscription.DxfgSubscription;
import com.dxfeed.sdk.subscription.DxfgTimeSeriesSubscription;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolList;
import com.dxfeed.sdk.symbol.DxfgSymbolPointer;
import com.dxfeed.sdk.symbol.ListSymbolMapper;
import com.dxfeed.sdk.symbol.SymbolMapper;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

public final class NativeUtils {

  public static final Finalizer FINALIZER = new Finalizer();
  public static final Mapper<StackTraceElement, DxfgStackTraceElement> MAPPER_STACK_TRACE_ELEMENT;
  public static final ListMapper<StackTraceElement, DxfgStackTraceElement, DxfgStackTraceElementPointer, DxfgStackTraceElementList> MAPPER_STACK_TRACE_ELEMENTS;
  public static final Mapper<Throwable, DxfgException> MAPPER_EXCEPTION;
  public static final Mapper<EventType<?>, DxfgEventType> MAPPER_EVENT;
  public static final ListMapper<EventType<?>, DxfgEventType, DxfgEventTypePointer, DxfgEventTypeList> MAPPER_EVENTS;
  public static final Mapper<String, CCharPointer> MAPPER_STRING;
  public static final Mapper<String, CCharPointer> MAPPER_STRING_CACHE_STORE;
  public static final ListMapper<String, CCharPointer, DxfgCharPointerPointer, DxfgCharPointerList> MAPPER_STRINGS;
  public static final Mapper<Object, DxfgSymbol> MAPPER_SYMBOL;
  public static final ListMapper<Object, DxfgSymbol, DxfgSymbolPointer, DxfgSymbolList> MAPPER_SYMBOLS;
  public static final ListMapper<Class<? extends EventType<?>>, CIntPointer, DxfgEventClazzPointer, DxfgEventClazzList> MAPPER_EVENT_TYPES;
  public static final Mapper<Executor, DxfgExecuter> MAPPER_EXECUTOR;
  public static final Mapper<TimePeriod, DxfgTimePeriod> MAPPER_TIME_PERIOD;
  public static final Mapper<TimeFormat, DxfgTimeFormat> MAPPER_TIME_FORMAT;
  public static final Mapper<TimeZone, DxfgTimeZone> MAPPER_TIME_ZONE;
  public static final Mapper<InputStream, DxfgInputStream> MAPPER_INPUT_STREAM;
  public static final Mapper<Builder, DxfgEndpointBuilder> MAPPER_ENDPOINT_BUILDER;
  public static final Mapper<DXEndpoint, DxfgEndpoint> MAPPER_ENDPOINT;
  public static final Mapper<Promise<?>, DxfgPromise> MAPPER_PROMISE;
  public static final Mapper<DXPublisher, DxfgPublisher> MAPPER_PUBLISHER;
  public static final Mapper<OnDemandService, DxfgOnDemandService> MAPPER_ON_DEMAND_SERVICE;
  public static final Mapper<PropertyChangeListener, DxfgEndpointStateChangeListener> MAPPER_ENDPOINT_STATE_CHANGE_LISTENER;
  public static final Mapper<OrderBookModel, DxfgOrderBookModel> MAPPER_ORDER_BOOK_MODEL;
  public static final Mapper<IndexedEventModel<?>, DxfgIndexedEventModel> MAPPER_INDEXED_EVENT_MODEL;
  public static final Mapper<TimeSeriesEventModel<?>, DxfgTimeSeriesEventModel> MAPPER_TIME_SERIES_EVENT_MODEL;
  public static final Mapper<ObservableListModel<? extends IndexedEvent<?>>, DxfgObservableListModel> MAPPER_OBSERVABLE_LIST_MODEL;
  public static final Mapper<OrderBookModelListener, DxfgOrderBookModelListener> MAPPER_ORDER_BOOK_MODEL_LISTENER;
  public static final Mapper<ObservableListModelListener<? super IndexedEvent<?>>, DxfgObservableListModelListener> MAPPER_OBSERVABLE_LIST_MODEL_LISTENER;
  public static final Mapper<ObservableSubscription<? extends EventType<?>>, DxfgObservableSubscription> MAPPER_OBSERVABLE_SUBSCRIPTION;
  public static final Mapper<ObservableSubscriptionChangeListener, DxfgObservableSubscriptionChangeListener> MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER;
  public static final Mapper<DXFeedSubscription<EventType<?>>, DxfgSubscription<DXFeedSubscription<EventType<?>>>> MAPPER_SUBSCRIPTION;
  public static final Mapper<DXFeedTimeSeriesSubscription<? extends TimeSeriesEvent<?>>, DxfgTimeSeriesSubscription> MAPPER_TIME_SERIES_SUBSCRIPTION;
  public static final Mapper<Object, JavaObjectHandler<Object>> MAPPER_JAVA_OBJECT_HANDLER;
  public static final ListMapper<Object, JavaObjectHandler<?>, DxfgJavaObjectHandlerPointer, DxfgJavaObjectHandlerList> MAPPER_JAVA_OBJECT_HANDLERS;
  public static final Mapper<IndexedEventSource, DxfgIndexedEventSource> MAPPER_INDEXED_EVENT_SOURCE;
  public static final Mapper<DXFeed, DxfgFeed> MAPPER_FEED;
  public static final Mapper<DXFeedEventListener<EventType<?>>, DxfgFeedEventListener> MAPPER_FEED_EVENT_LISTENER;
  public static final ListMapper<Promise<?>, DxfgPromise, DxfgPromisePointer, DxfgPromiseList> MAPPER_PROMISES;
  public static final Mapper<InstrumentProfileConnection, DxfgInstrumentProfileConnection> MAPPER_INSTRUMENT_PROFILE_CONNECTION;
  public static final Mapper<InstrumentProfile, DxfgInstrumentProfile> MAPPER_INSTRUMENT_PROFILE;
  public static final ListMapper<InstrumentProfile, DxfgInstrumentProfile, DxfgInstrumentProfilePointer, DxfgInstrumentProfileList> MAPPER_INSTRUMENT_PROFILES;
  public static final Mapper<Iterator<InstrumentProfile>, DxfgIterableInstrumentProfile> MAPPER_ITERABLE_INSTRUMENT_PROFILE;
  public static final Mapper<InstrumentProfileCollector, DxfgInstrumentProfileCollector> MAPPER_INSTRUMENT_PROFILE_COLLECTOR;
  public static final Mapper<InstrumentProfileReader, DxfgInstrumentProfileReader> MAPPER_INSTRUMENT_PROFILE_READER;
  public static final Mapper<PropertyChangeListener, DxfgIpfConnectionStateChangeListener> MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER;
  public static final Mapper<InstrumentProfileUpdateListener, DxfgInstrumentProfileUpdateListener> MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER;
  public static final Mapper<Session, DxfgSession> MAPPER_SESSION;
  public static final ListMapper<Session, DxfgSession, DxfgSessionPointer, DxfgSessionList> MAPPER_SESSIONS;
  public static final Mapper<Day, DxfgDay> MAPPER_DAY;
  public static final Mapper<SessionFilter, DxfgSessionFilter> MAPPER_SESSION_FILTER;
  public static final Mapper<DayFilter, DxfgDayFilter> MAPPER_DAY_FILTER;
  public static final Mapper<Schedule, DxfgSchedule> MAPPER_SCHEDULE;

  static {
    MAPPER_STRING = new StringMapper();
    MAPPER_STACK_TRACE_ELEMENT = new DxfgStackTraceElementMapper(MAPPER_STRING);
    MAPPER_STACK_TRACE_ELEMENTS = new ListStackTraceElementMapper(MAPPER_STACK_TRACE_ELEMENT);
    MAPPER_EXCEPTION = new ExceptionMapper(MAPPER_STRING, MAPPER_STACK_TRACE_ELEMENTS);
    MAPPER_STRINGS = new ListStringsMapper(MAPPER_STRING);
    MAPPER_STRING_CACHE_STORE = new StringMapperCacheStore(3000);
    SingletonScheduledExecutorService.start(
        "clean cache & finalize native",
        () -> {
          try {
            ((StringMapperCacheStore) MAPPER_STRING_CACHE_STORE).cleanUp();
            FINALIZER.finalizeResources();
          } catch (final Throwable throwable) {
            throwable.printStackTrace();
          }
        },
        1000
    );
    MAPPER_INDEXED_EVENT_SOURCE = new IndexedEventSourceMapper(MAPPER_STRING);
    MAPPER_SYMBOL = new SymbolMapper(MAPPER_STRING, MAPPER_INDEXED_EVENT_SOURCE);
    MAPPER_EVENT = new EventMappers(
        MAPPER_STRING,
        new QuoteMapper(MAPPER_STRING),
        new SeriesMapper(MAPPER_STRING),
        new OptionSaleMapper(MAPPER_STRING, MAPPER_STRING_CACHE_STORE),
        new TimeAndSaleMapper(MAPPER_STRING, MAPPER_STRING_CACHE_STORE),
        new SpreadOrderMapper(MAPPER_STRING, MAPPER_STRING_CACHE_STORE),
        new OrderMapper<>(MAPPER_STRING, MAPPER_STRING_CACHE_STORE),
        new AnalyticOrderMapper(MAPPER_STRING, MAPPER_STRING_CACHE_STORE),
        new OtcMarketsOrderMapper(MAPPER_STRING, MAPPER_STRING_CACHE_STORE),
        new MessageMapper(MAPPER_STRING),
        new OrderBaseMapper(MAPPER_STRING),
        new ConfigurationMapper(MAPPER_STRING),
        new TradeMapper(MAPPER_STRING),
        new TradeETHMapper(MAPPER_STRING),
        new TheoPriceMapper(MAPPER_STRING),
        new UnderlyingMapper(MAPPER_STRING),
        new GreeksMapper(MAPPER_STRING),
        new SummaryMapper(MAPPER_STRING),
        new ProfileMapper(MAPPER_STRING, MAPPER_STRING_CACHE_STORE),
        new DailyCandleMapper(MAPPER_STRING, MAPPER_SYMBOL),
        new CandleMapper<>(MAPPER_STRING, MAPPER_SYMBOL)
    );
    MAPPER_EVENTS = new ListEventMapper(MAPPER_EVENT);
    MAPPER_SYMBOLS = new ListSymbolMapper(MAPPER_SYMBOL);
    MAPPER_EVENT_TYPES = new ListEventTypeMapper();
    MAPPER_EXECUTOR = new ExecutorMapper();
    MAPPER_TIME_FORMAT = new TimeFormatMapper();
    MAPPER_TIME_PERIOD = new TimePeriodMapper();
    MAPPER_TIME_ZONE = new TimeZoneMapper();
    MAPPER_INPUT_STREAM = new InputStreamMapper();
    MAPPER_ENDPOINT_BUILDER = new EndpointBuilderMapper();
    MAPPER_ENDPOINT = new EndpointMapper();
    MAPPER_PROMISE = new PromiseMapper();
    MAPPER_PUBLISHER = new PublisherMapper();
    MAPPER_ON_DEMAND_SERVICE = new OnDemandServiceMapper();
    MAPPER_ENDPOINT_STATE_CHANGE_LISTENER = new EndpointStateChangeListenerMapper();
    MAPPER_ORDER_BOOK_MODEL = new OrderBookModelMapper();
    MAPPER_INDEXED_EVENT_MODEL = new IndexedEventModelMapper();
    MAPPER_TIME_SERIES_EVENT_MODEL = new TimeSeriesEventModelMapper();
    MAPPER_OBSERVABLE_LIST_MODEL = new ObservableListModelMapper();
    MAPPER_ORDER_BOOK_MODEL_LISTENER = new OrderBookModelListenerMapper();
    MAPPER_OBSERVABLE_LIST_MODEL_LISTENER = new ObservableListModelListenerMapper();
    MAPPER_OBSERVABLE_SUBSCRIPTION = new ObservableSubscriptionMapper();
    MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER = new ObservableSubscriptionChangeListenerMapper();
    MAPPER_SUBSCRIPTION = new SubscriptionMapper<>();
    MAPPER_TIME_SERIES_SUBSCRIPTION = new TimeSeriesSubscriptionMapper();
    MAPPER_JAVA_OBJECT_HANDLER = new JavaObjectHandlerMapper<>();
    MAPPER_JAVA_OBJECT_HANDLERS = new ListJavaObjectHandlerMapper(MAPPER_JAVA_OBJECT_HANDLER);
    MAPPER_FEED = new FeedMapper();
    MAPPER_FEED_EVENT_LISTENER = new FeedEventListenerMapper();
    MAPPER_PROMISES = new ListPromiseMapper(MAPPER_PROMISE);
    MAPPER_INSTRUMENT_PROFILE_CONNECTION = new InstrumentProfileConnectionMapper();
    MAPPER_INSTRUMENT_PROFILE = new InstrumentProfileMapper();
    MAPPER_INSTRUMENT_PROFILES = new ListInstrumentProfileMapper(MAPPER_INSTRUMENT_PROFILE);
    MAPPER_ITERABLE_INSTRUMENT_PROFILE = new IterableInstrumentProfileMapper();
    MAPPER_INSTRUMENT_PROFILE_COLLECTOR = new InstrumentProfileCollectorMapper();
    MAPPER_INSTRUMENT_PROFILE_READER = new InstrumentProfileReaderMapper();
    MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER = new IpfConnectionStateChangeListenerMapper();
    MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER = new InstrumentProfileUpdateListenerMapper();
    MAPPER_SESSION = new SessionMapper();
    MAPPER_SESSIONS = new ListSessionMapper(MAPPER_SESSION);
    MAPPER_DAY = new DayMapper();
    MAPPER_SESSION_FILTER = new SessionFilterMapper();
    MAPPER_DAY_FILTER = new DayFilterMapper();
    MAPPER_SCHEDULE = new ScheduleMapper();
  }

  public static class Finalizer {

    private final Set<Reference<Object>> holder = ConcurrentHashMap.newKeySet();
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    public void createFinalizer(final Object object, final Runnable finalizer) {
      holder.add(new FinalizerReference(object, queue, finalizer));
    }

    private void finalizeResources() {
      Reference<?> reference;
      while ((reference = queue.poll()) != null) {
        ((FinalizerReference) reference).finalizeResources();
        holder.remove(reference);
      }
    }


    private static class FinalizerReference extends PhantomReference<Object> {

      private final Runnable finalize;

      public FinalizerReference(
          final Object referent,
          final ReferenceQueue<Object> q,
          final Runnable finalize
      ) {
        super(referent, q);
        this.finalize = finalize;
      }

      public void finalizeResources() {
        this.finalize.run();
      }
    }
  }
}

class SingletonScheduledExecutorService {

  private static volatile ScheduledExecutorService instance;

  public static void start(final String name, final Runnable task, final int periodInMs) {
    if (instance == null) {
      synchronized (SingletonScheduledExecutorService.class) {
        if (instance == null) {
          instance = Executors.newScheduledThreadPool(1, runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName(name);
            return thread;
          });
          instance.scheduleAtFixedRate(task, periodInMs, periodInMs, MILLISECONDS);
        }
      }
    }
  }
}
