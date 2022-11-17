package com.dxfeed.api.endpoint;

import static com.dxfeed.api.NativeUtils.createHandler;
import static com.dxfeed.api.NativeUtils.destroyHandler;
import static com.dxfeed.api.NativeUtils.extractHandler;
import static com.dxfeed.api.NativeUtils.toJavaString;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.Role;
import com.dxfeed.api.DXEndpoint.State;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.api.publisher.DxfgPublisher;
import com.dxfeed.event.EventType;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.word.WordFactory;

@CContext(Directives.class)
public final class EndpointNative {

  public static final Map<DXEndpoint, Long> FEED_HANDLES = new ConcurrentHashMap<>();
  public static final Map<DXEndpoint, Long> PUBLISHER_HANDLES = new ConcurrentHashMap<>();
  public static final Map<Role, Long> INSTANCES = Collections.synchronizedMap(
      new EnumMap<>(Role.class)
  );

  @CEntryPoint(
      name = "dxfg_endpoint_get_instance",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getInstance(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    dxfgEndpoint.setJavaObjectHandler(
        WordFactory.signed(
            INSTANCES.computeIfAbsent(
                DXEndpoint.getInstance().getRole(),
                role -> createHandler(DXEndpoint.getInstance(role)).rawValue()
            )
        )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_instance_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getInstance(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole dxfgEndpointRole,
      final DxfgEndpoint dxfgEndpoint
  ) {
    dxfgEndpoint.setJavaObjectHandler(
        WordFactory.signed(
            INSTANCES.computeIfAbsent(
                dxfgEndpointRole.qdRole,
                role -> createHandler(DXEndpoint.getInstance(role)).rawValue()
            )
        )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_create",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int create(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    dxfgEndpoint.setJavaObjectHandler(createHandler(DXEndpoint.create()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_create_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int create(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole dxfgEndpointRole,
      final DxfgEndpoint dxfgEndpoint
  ) {
    dxfgEndpoint.setJavaObjectHandler(createHandler(DXEndpoint.create(dxfgEndpointRole.qdRole)));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int close(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    toJavaEndpoint(dxfgEndpoint).close();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_close_and_await_termination",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int closeAndAwaitTermination(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    toJavaEndpoint(dxfgEndpoint).closeAndAwaitTermination();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int release(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    final Long feed = FEED_HANDLES.remove(toJavaEndpoint(dxfgEndpoint));
    if (feed != null) {
      destroyHandler(WordFactory.signed(feed));
    }
    final Long publisher = PUBLISHER_HANDLES.remove(toJavaEndpoint(dxfgEndpoint));
    if (publisher != null) {
      destroyHandler(WordFactory.signed(publisher));
    }
    destroyHandler(dxfgEndpoint.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getRole(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CIntPointer role
  ) {
    role.write(
        DxfgEndpointRole.of(toJavaEndpoint(dxfgEndpoint).getRole()).getCValue()
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_set_user",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int user(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CCharPointer user
  ) {
    toJavaEndpoint(dxfgEndpoint).user(toJavaString(user));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_set_password",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int password(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CCharPointer password
  ) {
    toJavaEndpoint(dxfgEndpoint).password(toJavaString(password));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_connect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int connect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CCharPointer address
  ) {
    toJavaEndpoint(dxfgEndpoint).connect(toJavaString(address));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_reconnect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int reconnect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    toJavaEndpoint(dxfgEndpoint).reconnect();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_disconnect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int disconnect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    toJavaEndpoint(dxfgEndpoint).disconnect();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_disconnect_and_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int disconnectAndClear(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    toJavaEndpoint(dxfgEndpoint).disconnectAndClear();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_await_processed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int awaitProcessed(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    toJavaEndpoint(dxfgEndpoint).awaitProcessed();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_await_not_connected",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int awaitNotConnected(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    toJavaEndpoint(dxfgEndpoint).awaitNotConnected();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_state",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getState(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CIntPointer state
  ) {
    state.write(
        DxfgEndpointState.of(toJavaEndpoint(dxfgEndpoint).getState()).getCValue()
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_create_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int createStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpointStateChangeListenerFunc userFunc,
      final VoidPointer userData,
      final DxfgEndpointStateChangeListener listener
  ) {
    final PropertyChangeListener propertyChangeListener = changeEvent -> userFunc.invoke(
        CurrentIsolate.getCurrentThread(),
        DxfgEndpointState.of((State) changeEvent.getOldValue()),
        DxfgEndpointState.of((State) changeEvent.getNewValue()),
        userData
    );
    listener.setJavaObjectHandler(createHandler(propertyChangeListener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_release_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int releaseStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpointStateChangeListener listener
  ) {
    destroyHandler(listener.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_add_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int addStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgEndpointStateChangeListener listener
  ) {
    toJavaEndpoint(dxfgEndpoint)
        .addStateChangeListener(extractHandler(listener.getJavaObjectHandler()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_remove_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int removeStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgEndpointStateChangeListener listener
  ) {
    toJavaEndpoint(dxfgEndpoint)
        .removeStateChangeListener(extractHandler(listener.getJavaObjectHandler()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_feed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getFeed(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgFeed dxfgFeed
  ) {
    final DXEndpoint endpoint = toJavaEndpoint(dxfgEndpoint);
    dxfgFeed.setJavaObjectHandler(
        WordFactory.signed(
            FEED_HANDLES.computeIfAbsent(
                endpoint,
                k -> createHandler(k.getFeed()).rawValue()
            )
        )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_publisher",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getPublisher(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgPublisher dxfgPublisher
  ) {
    final DXEndpoint endpoint = toJavaEndpoint(dxfgEndpoint);
    dxfgPublisher.setJavaObjectHandler(
        WordFactory.signed(
            PUBLISHER_HANDLES.computeIfAbsent(
                endpoint,
                k -> createHandler(k.getPublisher()).rawValue()
            )
        )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_executor_new_fixed_thread_pool",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int newFixedThreadPool(
      final IsolateThread ignoredThread,
      final int nThreads,
      final CCharPointer name,
      final DxfgExecuter dxfgExecuter
  ) {
    dxfgExecuter.setJavaObjectHandler(createHandler(
        Executors.newFixedThreadPool(
            nThreads,
            new PoolThreadFactory(toJavaString(name))
        )
    ));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_executor_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int releaseFixedThreadPool(
      final IsolateThread ignoredThread,
      final DxfgExecuter dxfgExecuter
  ) {
    destroyHandler(dxfgExecuter.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_set_executor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int executor(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgExecuter dxfgExecuter
  ) {
    toJavaEndpoint(dxfgEndpoint).executor(extractHandler(dxfgExecuter.getJavaObjectHandler()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_event_types_size",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getEventTypesSize(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return toJavaEndpoint(dxfgEndpoint).getEventTypes().size();
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_event_types",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getEventTypes(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CIntPointer array
  ) {
    int i = 0;
    final Set<Class<? extends EventType<?>>> eventTypes = toJavaEndpoint(dxfgEndpoint).getEventTypes();
    for (final Class<? extends EventType<?>> eventType : eventTypes) {
      array.addressOf(i++).write(DxfgEventKind.of(eventType).getCValue());
    }
    return EXECUTE_SUCCESSFULLY;
  }

  private static DXEndpoint toJavaEndpoint(final DxfgEndpoint endpoint) {
    return extractHandler(endpoint.getJavaObjectHandler());
  }


  private static class PoolThreadFactory implements ThreadFactory {

    private final String name;
    private final AtomicInteger index = new AtomicInteger();
    private final ThreadGroup group;

    {
      final SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() :
          Thread.currentThread().getThreadGroup();
    }

    PoolThreadFactory(final String name) {
      this.name = name;
    }

    @Override
    public Thread newThread(final Runnable r) {
      final Thread thread = new Thread(group, r, name + "-" + index.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    }
  }
}
