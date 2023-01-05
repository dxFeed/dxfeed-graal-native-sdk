package com.dxfeed.api.endpoint;

import static com.dxfeed.api.NativeUtils.MAPPER_ENDPOINT;
import static com.dxfeed.api.NativeUtils.MAPPER_ENDPOINT_STATE_CHANGE_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENT_TYPES;
import static com.dxfeed.api.NativeUtils.MAPPER_EXECUTOR;
import static com.dxfeed.api.NativeUtils.MAPPER_FEED;
import static com.dxfeed.api.NativeUtils.MAPPER_PUBLISHER;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.State;
import com.dxfeed.api.events.DxfgEventClazzList;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.api.publisher.DxfgPublisher;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public final class EndpointNative {

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getInstance",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_getInstance(
      final IsolateThread ignoredThread
  ) {
    return MAPPER_ENDPOINT.toNative(DXEndpoint.getInstance());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getInstance2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_getInstance2(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole dxfgEndpointRole
  ) {
    return MAPPER_ENDPOINT.toNative(DXEndpoint.create(dxfgEndpointRole.qdRole));
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_create",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_create(
      final IsolateThread ignoredThread
  ) {
    return MAPPER_ENDPOINT.toNative(DXEndpoint.create());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_create2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_create2(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole dxfgEndpointRole
  ) {
    return MAPPER_ENDPOINT.toNative(DXEndpoint.create(dxfgEndpointRole.qdRole));
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_close(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).close();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_closeAndAwaitTermination",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_closeAndAwaitTermination(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).closeAndAwaitTermination();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getRole",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_getRole(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return DxfgEndpointRole.of(MAPPER_ENDPOINT.toJava(dxfgEndpoint).getRole()).getCValue();
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_user",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_user(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CCharPointer user
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).user(MAPPER_STRING.toJava(user));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_password",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_password(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CCharPointer password
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).password(MAPPER_STRING.toJava(password));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_connect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_connect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final CCharPointer address
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).connect(MAPPER_STRING.toJava(address));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_reconnect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_reconnect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).reconnect();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_disconnect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_disconnect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).disconnect();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_disconnectAndClear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_disconnectAndClear(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).disconnectAndClear();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_awaitProcessed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_awaitProcessed(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).awaitProcessed();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_awaitNotConnected",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_awaitNotConnected(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).awaitNotConnected();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getState",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_getState(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return DxfgEndpointState.of(MAPPER_ENDPOINT.toJava(dxfgEndpoint).getState()).getCValue();
  }

  @CEntryPoint(
      name = "dxfg_PropertyChangeListener_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpointStateChangeListener dxfg_PropertyChangeListener_new(
      final IsolateThread ignoredThread,
      final DxfgEndpointStateChangeListenerFunction userFunc,
      final VoidPointer userData
  ) {
    return MAPPER_ENDPOINT_STATE_CHANGE_LISTENER.toNative(
        changeEvent -> userFunc.invoke(
            CurrentIsolate.getCurrentThread(),
            DxfgEndpointState.of((State) changeEvent.getOldValue()),
            DxfgEndpointState.of((State) changeEvent.getNewValue()),
            userData
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_addStateChangeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_addStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgEndpointStateChangeListener listener
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint)
        .addStateChangeListener(MAPPER_ENDPOINT_STATE_CHANGE_LISTENER.toJava(listener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_removeStateChangeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_removeStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgEndpointStateChangeListener listener
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint)
        .removeStateChangeListener(MAPPER_ENDPOINT_STATE_CHANGE_LISTENER.toJava(listener));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getFeed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgFeed dxfg_DXEndpoint_getFeed(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return MAPPER_FEED.toNative(MAPPER_ENDPOINT.toJava(dxfgEndpoint).getFeed());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getPublisher",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPublisher dxfg_DXEndpoint_getPublisher(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return MAPPER_PUBLISHER.toNative(MAPPER_ENDPOINT.toJava(dxfgEndpoint).getPublisher());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_executor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_executor(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgExecuter dxfgExecuter
  ) {
    MAPPER_ENDPOINT.toJava(dxfgEndpoint).executor(MAPPER_EXECUTOR.toJava(dxfgExecuter));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getEventTypes",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventClazzList dxfg_DXEndpoint_getEventTypes(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return MAPPER_EVENT_TYPES.toNativeList(MAPPER_ENDPOINT.toJava(dxfgEndpoint).getEventTypes());
  }
}
