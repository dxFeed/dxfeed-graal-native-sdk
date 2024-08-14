package com.dxfeed.sdk.endpoint;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.State;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutor;
import com.dxfeed.sdk.publisher.DxfgPublisher;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@CContext(Directives.class)
public final class EndpointNative {

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getInstance",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_getInstance(
      final IsolateThread ignoredThread
  ) {
    return NativeUtils.MAPPER_ENDPOINT.toNative(DXEndpoint.getInstance());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getInstance2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_getInstance2(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole dxfgEndpointRole
  ) {
    return NativeUtils.MAPPER_ENDPOINT.toNative(DXEndpoint.create(dxfgEndpointRole.qdRole));
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_create",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_create(
      final IsolateThread ignoredThread
  ) {
    return NativeUtils.MAPPER_ENDPOINT.toNative(DXEndpoint.create());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_create2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_create2(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole dxfgEndpointRole
  ) {
    return NativeUtils.MAPPER_ENDPOINT.toNative(DXEndpoint.create(dxfgEndpointRole.qdRole));
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_close(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).close();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_closeAndAwaitTermination",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_closeAndAwaitTermination(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).closeAndAwaitTermination();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getRole",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_getRole(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return DxfgEndpointRole.of(NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).getRole()).getCValue();
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
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).user(NativeUtils.MAPPER_STRING.toJava(user));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).password(NativeUtils.MAPPER_STRING.toJava(password));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).connect(NativeUtils.MAPPER_STRING.toJava(address));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_reconnect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_reconnect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).reconnect();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_disconnect",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_disconnect(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).disconnect();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_disconnectAndClear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_disconnectAndClear(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).disconnectAndClear();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_awaitProcessed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_awaitProcessed(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).awaitProcessed();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_awaitNotConnected",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_awaitNotConnected(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) throws InterruptedException {
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).awaitNotConnected();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getState",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_getState(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return DxfgEndpointState.of(NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).getState()).getCValue();
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
    return NativeUtils.MAPPER_ENDPOINT_STATE_CHANGE_LISTENER.toNative(
        new PropertyChangeListener() {
          @Override
          public void propertyChange(final PropertyChangeEvent changeEvent) {
            userFunc.invoke(
                CurrentIsolate.getCurrentThread(),
                DxfgEndpointState.of((State) changeEvent.getOldValue()),
                DxfgEndpointState.of((State) changeEvent.getNewValue()),
                userData
            );
          }
        }
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
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).addStateChangeListener(
        NativeUtils.MAPPER_ENDPOINT_STATE_CHANGE_LISTENER.toJava(listener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint)
        .removeStateChangeListener(NativeUtils.MAPPER_ENDPOINT_STATE_CHANGE_LISTENER.toJava(listener));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getFeed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgFeed dxfg_DXEndpoint_getFeed(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return NativeUtils.MAPPER_FEED.toNative(NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).getFeed());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getPublisher",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPublisher dxfg_DXEndpoint_getPublisher(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return NativeUtils.MAPPER_PUBLISHER.toNative(NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).getPublisher());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_executor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_executor(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint,
      final DxfgExecutor dxfgExecutor
  ) {
    NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).executor(NativeUtils.MAPPER_EXECUTOR.toJava(
        dxfgExecutor));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getEventTypes",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventClazzList dxfg_DXEndpoint_getEventTypes(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return NativeUtils.MAPPER_EVENT_TYPES.toNativeList(NativeUtils.MAPPER_ENDPOINT.toJava(dxfgEndpoint).getEventTypes());
  }
}
