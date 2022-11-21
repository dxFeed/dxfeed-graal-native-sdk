package com.dxfeed.api.endpoint;

import static com.dxfeed.api.NativeUtils.MAPPER_ENEVET_TYPES;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.NativeUtils.newJavaObjectHandler;
import static com.dxfeed.api.NativeUtils.toJava;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.State;
import com.dxfeed.api.events.DxfgEventClazzList;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.feed.DxfgFeed;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.api.publisher.DxfgPublisher;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
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
    return newJavaObjectHandler(DXEndpoint.getInstance());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getInstance2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_getInstance2(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole dxfgEndpointRole
  ) {
    return newJavaObjectHandler(DXEndpoint.create(dxfgEndpointRole.qdRole));
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_create",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_create(
      final IsolateThread ignoredThread
  ) {
    return newJavaObjectHandler(DXEndpoint.create());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_create2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEndpoint dxfg_DXEndpoint_create2(
      final IsolateThread ignoredThread,
      final DxfgEndpointRole dxfgEndpointRole
  ) {
    return newJavaObjectHandler(DXEndpoint.create(dxfgEndpointRole.qdRole));
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXEndpoint_close(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    toJava(dxfgEndpoint).close();
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
    toJava(dxfgEndpoint).closeAndAwaitTermination();
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
    return DxfgEndpointRole.of(toJava(dxfgEndpoint).getRole()).getCValue();
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
    toJava(dxfgEndpoint).user(MAPPER_STRING.toJavaObject(user));
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
    toJava(dxfgEndpoint).password(MAPPER_STRING.toJavaObject(password));
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
    toJava(dxfgEndpoint).connect(MAPPER_STRING.toJavaObject(address));
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
    toJava(dxfgEndpoint).reconnect();
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
    toJava(dxfgEndpoint).disconnect();
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
    toJava(dxfgEndpoint).disconnectAndClear();
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
    toJava(dxfgEndpoint).awaitProcessed();
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
    toJava(dxfgEndpoint).awaitNotConnected();
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
    return DxfgEndpointState.of(toJava(dxfgEndpoint).getState()).getCValue();
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
    return newJavaObjectHandler((PropertyChangeListener) changeEvent -> userFunc.invoke(
        CurrentIsolate.getCurrentThread(),
        DxfgEndpointState.of((State) changeEvent.getOldValue()),
        DxfgEndpointState.of((State) changeEvent.getNewValue()),
        userData
    ));
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
    toJava(dxfgEndpoint).addStateChangeListener(toJava(listener));
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
    toJava(dxfgEndpoint).removeStateChangeListener(toJava(listener));
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
    return newJavaObjectHandler(toJava(dxfgEndpoint).getFeed());
  }

  @CEntryPoint(
      name = "dxfg_DXEndpoint_getPublisher",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPublisher dxfg_DXEndpoint_getPublisher(
      final IsolateThread ignoredThread,
      final DxfgEndpoint dxfgEndpoint
  ) {
    return newJavaObjectHandler(toJava(dxfgEndpoint).getPublisher());
  }

  @CEntryPoint(
      name = "dxfg_Executors_newFixedThreadPool",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_Executors_newFixedThreadPool(
      final IsolateThread ignoredThread,
      final int nThreads,
      final CCharPointer name
  ) {
    return newJavaObjectHandler(
        Executors.newFixedThreadPool(
            nThreads,
            new PoolThreadFactory(MAPPER_STRING.toJavaObject(name))
        )
    );
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
    toJava(dxfgEndpoint).executor(toJava(dxfgExecuter));
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
    return MAPPER_ENEVET_TYPES.toNativeList(toJava(dxfgEndpoint).getEventTypes());
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
