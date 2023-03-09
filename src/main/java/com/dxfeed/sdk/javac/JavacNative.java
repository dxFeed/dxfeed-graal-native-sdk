package com.dxfeed.sdk.javac;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.Pointer;

@CContext(Directives.class)
public class JavacNative {

  @CEntryPoint(
      name = "dxfg_JavaObjectHandler_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_JavaObjectHandler_release(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> javaObjectHandler
  ) {
    NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.release(javaObjectHandler);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_JavaObjectHandler_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_JavaObjectHandler_release(
      final IsolateThread ignoredThread,
      final DxfgJavaObjectHandlerList cList
  ) {
    NativeUtils.MAPPER_JAVA_OBJECT_HANDLERS.release(cList);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_String_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_String_release(
      final IsolateThread ignoreThread,
      final CCharPointer string
  ) {
    NativeUtils.MAPPER_STRING.release(string);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_String_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_String_release(
      final IsolateThread ignoreThread,
      final DxfgCharPointerList strings
  ) {
    NativeUtils.MAPPER_STRINGS.release(strings);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    return NativeUtils.MAPPER_EXECUTOR.toNative(
        Executors.newFixedThreadPool(
            nThreads,
            new PoolThreadFactory(NativeUtils.MAPPER_STRING.toJava(name))
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_Executors_newScheduledThreadPool",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_Executors_newScheduledThreadPool(
      final IsolateThread ignoredThread,
      final int nThreads,
      final CCharPointer name
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(
        Executors.newScheduledThreadPool(
            nThreads,
            new PoolThreadFactory(NativeUtils.MAPPER_STRING.toJava(name))
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_ExecutorBaseOnConcurrentLinkedQueue_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_ExecutorBaseOnConcurrentLinkedQueue_new(
      final IsolateThread ignoredThread
  ) {
    return NativeUtils.MAPPER_EXECUTOR.toNative(new ExecutorBaseOnConcurrentLinkedQueue());
  }

  @CEntryPoint(
      name = "dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(
      final IsolateThread ignoredThread,
      final DxfgExecuter executor
  ) {
    ((ExecutorBaseOnConcurrentLinkedQueue) NativeUtils.MAPPER_EXECUTOR.toJava(
        executor)).processAllPendingTasks();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_gc",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_gc(final IsolateThread ignoredThread) {
    System.gc();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_throw_exception",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static JavaObjectHandler<?> dxfg_throw_exception(final IsolateThread ignoredThread) {
    throw new RuntimeException();
  }

  @CEntryPoint(
      name = "dxfg_ByteArrayInputStream_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInputStream dxfg_ByteArrayInputStream_new(
      final IsolateThread ignoredThread,
      final CCharPointer dxfgBytes,
      final int size
  ) {
    final byte[] bytes = new byte[size];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = ((Pointer) dxfgBytes).readByte(i);
    }
    return NativeUtils.MAPPER_INPUT_STREAM.toNative(new ByteArrayInputStream(bytes));
  }

  private static class ExecutorBaseOnConcurrentLinkedQueue implements Executor {

    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void execute(final Runnable command) {
      taskQueue.add(command);
    }

    public void processAllPendingTasks() {
      Runnable task;
      while ((task = taskQueue.poll()) != null) {
        task.run();
      }
    }
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
