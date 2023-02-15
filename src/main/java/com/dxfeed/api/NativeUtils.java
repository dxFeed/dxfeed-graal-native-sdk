package com.dxfeed.api;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.dxfeed.api.endpoint.DxfgEndpoint;
import com.dxfeed.api.endpoint.DxfgEndpointBuilder;
import com.dxfeed.api.endpoint.DxfgEndpointStateChangeListener;
import com.dxfeed.api.events.DxfgEventClazzList;
import com.dxfeed.api.events.DxfgEventClazzPointer;
import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.api.events.DxfgEventTypeList;
import com.dxfeed.api.events.DxfgEventTypePointer;
import com.dxfeed.api.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.api.exception.DxfgException;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.api.feed.DxfgPromise;
import com.dxfeed.api.feed.DxfgPromiseList;
import com.dxfeed.api.feed.DxfgPromisePointer;
import com.dxfeed.api.ipf.DxfgInstrumentProfile;
import com.dxfeed.api.ipf.DxfgInstrumentProfileCollector;
import com.dxfeed.api.ipf.DxfgInstrumentProfileConnection;
import com.dxfeed.api.ipf.DxfgInstrumentProfileList;
import com.dxfeed.api.ipf.DxfgInstrumentProfilePointer;
import com.dxfeed.api.ipf.DxfgInstrumentProfileReader;
import com.dxfeed.api.ipf.DxfgInstrumentProfileUpdateListener;
import com.dxfeed.api.ipf.DxfgIpfConnectionStateChangeListener;
import com.dxfeed.api.ipf.DxfgIterableInstrumentProfile;
import com.dxfeed.api.javac.DxfgCharPointerList;
import com.dxfeed.api.javac.DxfgCharPointerPointer;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.api.javac.DxfgInputStream;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerPointer;
import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.api.maper.DayFilterMapper;
import com.dxfeed.api.maper.DayMapper;
import com.dxfeed.api.maper.EndpointBuilderMapper;
import com.dxfeed.api.maper.EndpointMapper;
import com.dxfeed.api.maper.EndpointStateChangeListenerMapper;
import com.dxfeed.api.maper.ExceptionMapper;
import com.dxfeed.api.maper.ExecutorMapper;
import com.dxfeed.api.maper.FeedEventListenerMapper;
import com.dxfeed.api.maper.FeedMapper;
import com.dxfeed.api.maper.IndexedEventModelMapper;
import com.dxfeed.api.maper.InputStreamMapper;
import com.dxfeed.api.maper.InstrumentProfileCollectorMapper;
import com.dxfeed.api.maper.InstrumentProfileConnectionMapper;
import com.dxfeed.api.maper.InstrumentProfileMapper;
import com.dxfeed.api.maper.InstrumentProfileReaderMapper;
import com.dxfeed.api.maper.InstrumentProfileUpdateListenerMapper;
import com.dxfeed.api.maper.IpfConnectionStateChangeListenerMapper;
import com.dxfeed.api.maper.IterableInstrumentProfileMapper;
import com.dxfeed.api.maper.JavaObjectHandlerMapper;
import com.dxfeed.api.maper.ListInstrumentProfileMapper;
import com.dxfeed.api.maper.ListJavaObjectHandlerMapper;
import com.dxfeed.api.maper.ListMapper;
import com.dxfeed.api.maper.ListPromiseMapper;
import com.dxfeed.api.maper.ListSessionMapper;
import com.dxfeed.api.maper.ListStringsMapper;
import com.dxfeed.api.maper.Mapper;
import com.dxfeed.api.maper.ObservableListModelListenerMapper;
import com.dxfeed.api.maper.ObservableListModelMapper;
import com.dxfeed.api.maper.ObservableSubscriptionChangeListenerMapper;
import com.dxfeed.api.maper.ObservableSubscriptionMapper;
import com.dxfeed.api.maper.OrderBookModelListenerMapper;
import com.dxfeed.api.maper.OrderBookModelMapper;
import com.dxfeed.api.maper.PromiseMapper;
import com.dxfeed.api.maper.PublisherMapper;
import com.dxfeed.api.maper.ScheduleMapper;
import com.dxfeed.api.maper.SessionFilterMapper;
import com.dxfeed.api.maper.SessionMapper;
import com.dxfeed.api.maper.StringMapper;
import com.dxfeed.api.maper.StringMapperCacheStore;
import com.dxfeed.api.maper.StringMapperUnlimitedStore;
import com.dxfeed.api.maper.SubscriptionMapper;
import com.dxfeed.api.maper.TimeSeriesEventModelMapper;
import com.dxfeed.api.maper.TimeSeriesSubscriptionMapper;
import com.dxfeed.api.model.DxfgIndexedEventModel;
import com.dxfeed.api.model.DxfgObservableListModel;
import com.dxfeed.api.model.DxfgObservableListModelListener;
import com.dxfeed.api.model.DxfgOrderBookModel;
import com.dxfeed.api.model.DxfgOrderBookModelListener;
import com.dxfeed.api.model.DxfgTimeSeriesEventModel;
import com.dxfeed.api.osub.ObservableSubscription;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.api.publisher.DxfgObservableSubscription;
import com.dxfeed.api.publisher.DxfgPublisher;
import com.dxfeed.api.schedule.DxfgDay;
import com.dxfeed.api.schedule.DxfgDayFilter;
import com.dxfeed.api.schedule.DxfgSchedule;
import com.dxfeed.api.schedule.DxfgSession;
import com.dxfeed.api.schedule.DxfgSessionFilter;
import com.dxfeed.api.schedule.DxfgSessionList;
import com.dxfeed.api.schedule.DxfgSessionPointer;
import com.dxfeed.api.source.DxfgIndexedEventSource;
import com.dxfeed.api.source.IndexedEventSourceMapper;
import com.dxfeed.api.subscription.DxfgFeedEventListener;
import com.dxfeed.api.subscription.DxfgSubscription;
import com.dxfeed.api.subscription.DxfgTimeSeriesSubscription;
import com.dxfeed.api.symbol.DxfgSymbol;
import com.dxfeed.api.symbol.DxfgSymbolList;
import com.dxfeed.api.symbol.DxfgSymbolPointer;
import com.dxfeed.api.symbol.ListSymbolMapper;
import com.dxfeed.api.symbol.SymbolMapper;
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
import com.dxfeed.promise.Promise;
import com.dxfeed.schedule.Day;
import com.dxfeed.schedule.DayFilter;
import com.dxfeed.schedule.Schedule;
import com.dxfeed.schedule.Session;
import com.dxfeed.schedule.SessionFilter;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

public final class NativeUtils {

  public static final Finalizer FINALIZER = new Finalizer();
  public static final Mapper<Throwable, DxfgException> MAPPER_EXCEPTION;
  public static final Mapper<EventType<?>, DxfgEventType> MAPPER_EVENT;
  public static final ListMapper<EventType<?>, DxfgEventType, DxfgEventTypePointer, DxfgEventTypeList> MAPPER_EVENTS;
  public static final Mapper<String, CCharPointer> MAPPER_STRING;
  public static final Mapper<String, CCharPointer> MAPPER_STRING_UNLIMITED_STORE;
  public static final Mapper<String, CCharPointer> MAPPER_STRING_CACHE_STORE;
  public static final ListMapper<String, CCharPointer, DxfgCharPointerPointer, DxfgCharPointerList> MAPPER_STRINGS;
  public static final Mapper<Object, DxfgSymbol> MAPPER_SYMBOL;
  public static final ListMapper<Object, DxfgSymbol, DxfgSymbolPointer, DxfgSymbolList> MAPPER_SYMBOLS;
  public static final ListMapper<Class<? extends EventType<?>>, CIntPointer, DxfgEventClazzPointer, DxfgEventClazzList> MAPPER_EVENT_TYPES;
  public static final Mapper<Executor, DxfgExecuter> MAPPER_EXECUTOR;
  public static final Mapper<InputStream, DxfgInputStream> MAPPER_INPUT_STREAM;
  public static final Mapper<DXEndpoint.Builder, DxfgEndpointBuilder> MAPPER_ENDPOINT_BUILDER;
  public static final Mapper<DXEndpoint, DxfgEndpoint> MAPPER_ENDPOINT;
  public static final Mapper<Promise<?>, DxfgPromise> MAPPER_PROMISE;
  public static final Mapper<DXPublisher, DxfgPublisher> MAPPER_PUBLISHER;
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
    MAPPER_EXCEPTION = new ExceptionMapper(new StringMapper());
    MAPPER_STRING = new StringMapper();
    MAPPER_STRINGS = new ListStringsMapper(MAPPER_STRING);
    MAPPER_STRING_UNLIMITED_STORE = new StringMapperUnlimitedStore();
    MAPPER_STRING_CACHE_STORE = new StringMapperCacheStore(3000);
    SingletonScheduledExecutorService.start(
        "clean cache & finalize native",
        () -> {
          ((StringMapperCacheStore) MAPPER_STRING_CACHE_STORE).cleanUp();
          FINALIZER.finalizeResources();
        },
        1000
    );
    MAPPER_INDEXED_EVENT_SOURCE = new IndexedEventSourceMapper(MAPPER_STRING_UNLIMITED_STORE);
    MAPPER_SYMBOL = new SymbolMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_INDEXED_EVENT_SOURCE);
    MAPPER_EVENT = new EventMappers(
        MAPPER_STRING_UNLIMITED_STORE,
        new QuoteMapper(MAPPER_STRING_UNLIMITED_STORE),
        new SeriesMapper(MAPPER_STRING_UNLIMITED_STORE),
        new OptionSaleMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
        new TimeAndSaleMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
        new SpreadOrderMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
        new OrderMapper<>(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
        new AnalyticOrderMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_STRING_CACHE_STORE),
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
        new DailyCandleMapper(MAPPER_STRING_UNLIMITED_STORE, MAPPER_SYMBOL),
        new CandleMapper<>(MAPPER_STRING_UNLIMITED_STORE, MAPPER_SYMBOL)
    );
    MAPPER_EVENTS = new ListEventMapper(MAPPER_EVENT);
    MAPPER_SYMBOLS = new ListSymbolMapper(MAPPER_SYMBOL);
    MAPPER_EVENT_TYPES = new ListEventTypeMapper();
    MAPPER_EXECUTOR = new ExecutorMapper();
    MAPPER_INPUT_STREAM = new InputStreamMapper();
    MAPPER_ENDPOINT_BUILDER = new EndpointBuilderMapper();
    MAPPER_ENDPOINT = new EndpointMapper();
    MAPPER_PROMISE = new PromiseMapper();
    MAPPER_PUBLISHER = new PublisherMapper();
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

    public final Set<Reference<Object>> holder = ConcurrentHashMap.newKeySet();
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    public <T> T wrapObjectWithFinalizer(final T object, final Runnable finalizer) {
      holder.add(new FinalizerReference(object, queue, finalizer));
      return object;
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
