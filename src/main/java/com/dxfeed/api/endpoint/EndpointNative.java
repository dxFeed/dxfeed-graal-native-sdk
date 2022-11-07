package com.dxfeed.api.endpoint;

import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.Builder;
import com.dxfeed.api.DXEndpoint.Role;
import com.dxfeed.api.DXEndpoint.State;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.event.EventType;
import java.beans.PropertyChangeListener;
import java.util.Set;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

@CContext(Directives.class)
public final class EndpointNative extends BaseNative {

  public static DXEndpoint getInstance() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public static DXEndpoint getInstance(final Role role) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public static Builder newBuilder() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
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

  public static DXEndpoint create(final Role role) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public static Role getRole() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  @CEntryPoint(
      name = "dxfg_endpoint_get_state",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getState(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CIntPointer result
  ) {
    result.write(
        DxfgEndpointState.fromDxEndpointState(
            getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).getState()
        ).getCValue()
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_add_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int addStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgStateChangeListener listenerPtr
  ) {
    final DXEndpoint dxEndpoint = getDxEndpoint(dxfgEndpoint.getJavaObjectHandler());
    final PropertyChangeListener propertyChangeListener = changeEvent -> listenerPtr.getStateChangeListener()
        .invoke(
            CurrentIsolate.getCurrentThread(),
            DxfgEndpointState.fromDxEndpointState((State) changeEvent.getOldValue()),
            DxfgEndpointState.fromDxEndpointState((State) changeEvent.getNewValue())
        );
    listenerPtr.setJavaObjectHandler(createJavaObjectHandler(propertyChangeListener));
    dxEndpoint.addStateChangeListener(propertyChangeListener);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_endpoint_remove_state_change_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int removeStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgStateChangeListener listenerPtr
  ) {
    final PropertyChangeListener listener = getJavaObject(listenerPtr.getJavaObjectHandler());
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).removeStateChangeListener(listener);
    destroyJavaObjectHandler(listenerPtr.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  public static DXEndpoint executor(Executor executor) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public static DXEndpoint user(String user) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public static DXEndpoint password(String password) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
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
      name = "dxfg_endpoint_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int close(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).close();
    destroyJavaObjectHandler(dxfgEndpoint.getJavaObjectHandler());
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

  public static void awaitProcessed() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
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
    destroyJavaObjectHandler(dxfgEndpoint.getJavaObjectHandler());
    return EXECUTE_SUCCESSFULLY;
  }

  public static Set<Class<? extends EventType<?>>> getEventTypes() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
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
    final DXFeed feed = getDxEndpoint(dxfgEndpoint.getJavaObjectHandler()).getFeed();
    dxfgFeed.setJavaObjectHandler(createJavaObjectHandler(feed));
    return EXECUTE_SUCCESSFULLY;
  }

  public static DXPublisher getPublisher() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }
}
