package com.dxfeed.api.endpoint;

import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.State;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.event.EventType;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final Map<DXEndpoint, Long> SINGLETON_ENDPOINT_HANDLES = new ConcurrentHashMap<>();

  private static final Map<DXEndpoint, Long> FEED_HANDLES = new ConcurrentHashMap<>();

  private static final Map<DXEndpoint, Long> PUBLISHER_HANDLES = new ConcurrentHashMap<>();

  @CEntryPoint(
      name = "dxfg_endpoint_get_instance",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getInstance(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) {
    return getInstance(ignoredThread, DxfgEndpointRole.DXFG_ENDPOINT_ROLE_FEED, endpoint);
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_instance_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getInstance(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole role,
      final DxfgEndpoint endpoint
  ) {
    final DXEndpoint dxEndpoint = DXEndpoint.getInstance(DxfgEndpointRole.toDXEndpointRole(role));
    endpoint.setJavaObjectHandler(
        WordFactory.pointer(SINGLETON_ENDPOINT_HANDLES.computeIfAbsent(
            dxEndpoint,
            k -> createJavaObjectHandler(dxEndpoint).rawValue()))
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_create",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int create(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) {
    endpoint.setJavaObjectHandler(createEndpoint());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_create_with_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int create(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole role,
      final DxfgEndpoint endpoint
  ) {
    endpoint.setJavaObjectHandler(createEndpoint(role));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int close(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) {
    getEndpoint(endpoint)
        .close();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_close_and_await_termination",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int closeAndAwaitTermination(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) throws InterruptedException {
    getEndpoint(endpoint)
        .closeAndAwaitTermination();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int release(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) {
    releaseEndpoint(endpoint);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_role",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getRole(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final CIntPointer role
  ) {
    role.write(
        DxfgEndpointRole.fromDXEndpointRole(getEndpoint(endpoint).getRole()).getCValue()
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_set_user",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int user(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final CCharPointer user
  ) {
    getEndpoint(endpoint)
        .user(toJavaString(user));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_set_password",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int password(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final CCharPointer password
  ) {
    getEndpoint(endpoint)
        .password(toJavaString(password));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_connect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int connect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final CCharPointer address
  ) {
    getEndpoint(endpoint)
        .connect(toJavaString(address));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_reconnect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int reconnect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) {
    getEndpoint(endpoint)
        .reconnect();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_disconnect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int disconnect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) {
    getEndpoint(endpoint)
        .disconnect();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_disconnect_and_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int disconnectAndClear(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) {
    getEndpoint(endpoint)
        .disconnectAndClear();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_await_processed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int awaitProcessed(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) throws InterruptedException {
    getEndpoint(endpoint)
        .awaitProcessed();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_await_not_connected",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int awaitNotConnected(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint
  ) throws InterruptedException {
    getEndpoint(endpoint)
        .awaitNotConnected();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_state",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getState(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final CIntPointer state
  ) {
    state.write(
        DxfgEndpointState.fromDXEndpointState(getEndpoint(endpoint).getState()).getCValue()
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
        DxfgEndpointState.fromDXEndpointState((State) changeEvent.getOldValue()),
        DxfgEndpointState.fromDXEndpointState((State) changeEvent.getNewValue()),
        userData
    );
    listener.setJavaObjectHandler(createJavaObjectHandler(propertyChangeListener));
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
    destroyJavaObjectHandler(listener.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_add_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int addStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final DxfgEndpointStateChangeListener listener
  ) {
    getEndpoint(endpoint)
        .addStateChangeListener(getJavaObject(listener.getJavaObjectHandler()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_remove_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int removeStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final DxfgEndpointStateChangeListener listener
  ) {
    getEndpoint(endpoint)
        .removeStateChangeListener(getJavaObject(listener.getJavaObjectHandler()));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_feed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getFeed(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final DxfgFeed feed
  ) {
    feed.setJavaObjectHandler(createFeed(getEndpoint(endpoint)));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_publisher",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getPublisher(
      final IsolateThread ignoredThread,
      final DxfgEndpoint endpoint,
      final DxfgFeed publisher
  ) {
    publisher.setJavaObjectHandler(createPublisher(getEndpoint(endpoint)));
    return EXECUTE_SUCCESSFULLY;
  }

  private static ObjectHandle createEndpoint() {
    return createJavaObjectHandler(DXEndpoint.create());
  }

  private static ObjectHandle createEndpoint(DxfgEndpointRole role) {
    return createJavaObjectHandler(DXEndpoint.create(DxfgEndpointRole.toDXEndpointRole(role)));
  }

  private static void releaseEndpoint(DxfgEndpoint endpoint) {
    releaseFeed(getEndpoint(endpoint));
    releasePublisher(getEndpoint(endpoint));
    destroyJavaObjectHandler(endpoint.getJavaObjectHandler());
  }

  private static DXEndpoint getEndpoint(DxfgEndpoint endpoint) {
    return getJavaObject(endpoint.getJavaObjectHandler());
  }

  private static ObjectHandle createFeed(DXEndpoint endpoint) {
    return WordFactory.pointer(FEED_HANDLES.computeIfAbsent(
        endpoint,
        k -> createJavaObjectHandler(endpoint.getFeed()).rawValue()
    ));
  }

  private static void releaseFeed(DXEndpoint endpoint) {
    destroyJavaObjectHandler(WordFactory.pointer(FEED_HANDLES.remove(endpoint)));
  }

  private static ObjectHandle createPublisher(DXEndpoint endpoint) {
    return WordFactory.pointer(PUBLISHER_HANDLES.computeIfAbsent(
        endpoint,
        k -> createJavaObjectHandler(endpoint.getPublisher()).rawValue()
    ));
  }

  private static void releasePublisher(DXEndpoint endpoint) {
    destroyJavaObjectHandler(WordFactory.pointer(PUBLISHER_HANDLES.remove(endpoint)));
  }

  public static DXEndpoint executor(Executor executor) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public static Set<Class<? extends EventType<?>>> getEventTypes() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }
}
