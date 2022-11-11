package com.dxfeed.api.endpoint;

import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.State;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.event.EventType;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.word.WordFactory;

@CContext(Directives.class)
public final class EndpointNative extends BaseNative {

  private static final Map<DXEndpoint, Long> ENDPOINT_OBJECT_HANDLES = new HashMap<>();

  private static final Map<DXEndpoint, Long> FEED_OBJECT_HANDLES = new HashMap<>();

  @CEntryPoint(
      name = "dxfg_endpoint_get_instance",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getInstance(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return getInstance(ignoredThread, DxfgEndpointRole.DXFG_ENDPOINT_ROLE_FEED, dxfgEndpoint);
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_instance_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getInstance(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole role,
      final DxfgEndpoint dxfgEndpoint
  ) {
    synchronized (ENDPOINT_OBJECT_HANDLES) {
      final DXEndpoint dxEndpoint = DXEndpoint.getInstance(DxfgEndpointRole.toDXEndpointRole(role));
      if (!ENDPOINT_OBJECT_HANDLES.containsKey(dxEndpoint)) {
        final ObjectHandle javaObjectHandler = createJavaObjectHandler(dxEndpoint);
        ENDPOINT_OBJECT_HANDLES.put(dxEndpoint, javaObjectHandler.rawValue());
      }
      dxfgEndpoint.setJavaObjectHandler(
          WordFactory.pointer(ENDPOINT_OBJECT_HANDLES.get(dxEndpoint))
      );
    }
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
    dxfgEndpoint.setJavaObjectHandler(createJavaObjectHandler(DXEndpoint.create()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_create_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int create(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole role,
      final DxfgEndpoint dxfgEndpoint
  ) {
    dxfgEndpoint.setJavaObjectHandler(
        createJavaObjectHandler(DXEndpoint.create(DxfgEndpointRole.toDXEndpointRole(role))));
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).close();
    destroyDxfgEndpoint(dxfgEndpoint);
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).closeAndAwaitTermination();
    destroyDxfgEndpoint(dxfgEndpoint);
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
        DxfgEndpointRole.fromDXEndpointRole(
            getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).getRole()
        ).getCValue());
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).user(toJavaString(user));
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).password(toJavaString(password));
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).connect(toJavaString(address));
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).reconnect();
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).disconnect();
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).disconnectAndClear();
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).awaitProcessed();
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
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).awaitNotConnected();
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
        DxfgEndpointState.fromDXEndpointState(
            getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).getState()
        ).getCValue()
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_create_property_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int createPropertyChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpointStateChangeListener userFunc,
      final VoidPointer userData,
      final DxfgEndpointPropertyChangeListener listener
  ) {
    final PropertyChangeListener propertyChangeListener = changeEvent -> userFunc.invoke(
        CurrentIsolate.getCurrentThread(),
        DxfgEndpointState.fromDXEndpointState((State) changeEvent.getOldValue()),
        DxfgEndpointState.fromDXEndpointState((State) changeEvent.getNewValue()),
        userData
    );
    listener.setJavaObjectHandler(createJavaObjectHandler(propertyChangeListener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_close_property_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int closePropertyChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpointPropertyChangeListener listener
  ) {
    destroyJavaObjectHandler(listener.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_add_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int addStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgEndpointPropertyChangeListener listener
  ) {
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler())
        .addStateChangeListener(getJavaObject(listener.getJavaObjectHandler()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_remove_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int removeStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgEndpointPropertyChangeListener listener
  ) {
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler())
        .removeStateChangeListener(getJavaObject(listener.getJavaObjectHandler()));
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
    synchronized (FEED_OBJECT_HANDLES) {
      final DXEndpoint dxEndpoint = getDxEndpoint(dxfgEndpoint.getJavaObjectHandler());
      if (!FEED_OBJECT_HANDLES.containsKey(dxEndpoint)) {
        final ObjectHandle javaObjectHandler = createJavaObjectHandler(dxEndpoint.getFeed());
        FEED_OBJECT_HANDLES.put(dxEndpoint, javaObjectHandler.rawValue());
      }
      dxfgFeed.setJavaObjectHandler(
          WordFactory.pointer(FEED_OBJECT_HANDLES.get(dxEndpoint))
      );
    }
    return EXECUTE_SUCCESSFULLY;
  }

  private static void destroyDxfgEndpoint(final DxfgEndpoint dxfgEndpoint) {
    final DXEndpoint dxEndpoint = getDxEndpoint(dxfgEndpoint.getJavaObjectHandler());
    synchronized (FEED_OBJECT_HANDLES) {
      if (FEED_OBJECT_HANDLES.containsKey(dxEndpoint)) {
        destroyJavaObjectHandler(
            WordFactory.pointer(FEED_OBJECT_HANDLES.remove(dxEndpoint))
        );
      }
    }
    destroyJavaObjectHandler(dxfgEndpoint.getJavaObjectHandler());
  }

  public static DXEndpoint executor(Executor executor) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public static Set<Class<? extends EventType<?>>> getEventTypes() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public static DXPublisher getPublisher() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }
}
