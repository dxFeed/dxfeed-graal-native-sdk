package com.dxfeed.api.ipf;

import static com.dxfeed.api.NativeUtils.FINALIZER;
import static com.dxfeed.api.NativeUtils.MAPPER_EXECUTOR;
import static com.dxfeed.api.NativeUtils.MAPPER_INSTRUMENT_PROFILE;
import static com.dxfeed.api.NativeUtils.MAPPER_INSTRUMENT_PROFILES;
import static com.dxfeed.api.NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR;
import static com.dxfeed.api.NativeUtils.MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_ITERABLE_INSTRUMENT_PROFILE;
import static com.dxfeed.api.NativeUtils.MAPPER_JAVA_OBJECT_HANDLER;
import static com.dxfeed.api.NativeUtils.MAPPER_JAVA_OBJECT_HANDLERS;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.api.javac.DxfgFinalizeFunction;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.ipf.live.InstrumentProfileCollector;
import java.util.HashSet;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class InstrumentProfileCollectorNative {

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileCollector dxfg_InstrumentProfileCollector_new(
      final IsolateThread ignoredThread
  ) {
    return MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toNative(new InstrumentProfileCollector());
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_getLastUpdateTime",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_InstrumentProfileCollector_getLastUpdateTime(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector
  ) {
    return MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).getLastUpdateTime();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_updateInstrumentProfile",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCollector_updateInstrumentProfile(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector,
      final DxfgInstrumentProfile dxfgInstrumentProfile
  ) {
    MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).updateInstrumentProfile(
        MAPPER_INSTRUMENT_PROFILE.toJava(dxfgInstrumentProfile)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_updateInstrumentProfiles",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCollector_updateInstrumentProfiles(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector,
      final DxfgInstrumentProfileList dxfgInstrumentProfiles,
      final JavaObjectHandler<Object> generation
  ) {
    MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).updateInstrumentProfiles(
        MAPPER_INSTRUMENT_PROFILES.toJavaList(dxfgInstrumentProfiles),
        MAPPER_JAVA_OBJECT_HANDLER.toJava(generation)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_removeGenerations",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCollector_removeGenerations(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector,
      final DxfgJavaObjectHandlerList generations
  ) {
    MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).removeGenerations(
        new HashSet<>(MAPPER_JAVA_OBJECT_HANDLERS.toJavaList(generations))
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_view",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgIterableInstrumentProfile dxfg_InstrumentProfileCollector_view(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector
  ) {
    return MAPPER_ITERABLE_INSTRUMENT_PROFILE.toNative(
        MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).view().iterator()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_getExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgExecuter dxfg_InstrumentProfileCollector_getExecutor(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector
  ) {
    return MAPPER_EXECUTOR.toNative(
        MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).getExecutor()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_setExecutor",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCollector_setExecutor(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector,
      final DxfgExecuter dxfgExecuter
  ) {
    MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).setExecutor(
        MAPPER_EXECUTOR.toJava(dxfgExecuter)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_addUpdateListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCollector_addUpdateListener(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector,
      final DxfgInstrumentProfileUpdateListener dxfgListener,
      final DxfgFinalizeFunction finalizeFunction,
      final VoidPointer userData
  ) {
    MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).addUpdateListener(
        finalizeFunction.isNull()
            ? MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER.toJava(dxfgListener)
            : FINALIZER.wrapObjectWithFinalizer(
                MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER.toJava(dxfgListener),
                () -> finalizeFunction.invoke(CurrentIsolate.getCurrentThread(), userData)
            )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_removeUpdateListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCollector_removeUpdateListener(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector,
      final DxfgInstrumentProfileUpdateListener dxfgInstrumentProfileUpdateListener
  ) {
    MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).removeUpdateListener(
        MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER.toJava(dxfgInstrumentProfileUpdateListener)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Iterable_InstrumentProfile_hasNext",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Iterable_InstrumentProfile_hasNext(
      final IsolateThread ignoredThread,
      final DxfgIterableInstrumentProfile dxfgIterableInstrumentProfile
  ) {
    return MAPPER_ITERABLE_INSTRUMENT_PROFILE.toJava(dxfgIterableInstrumentProfile).hasNext() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Iterable_InstrumentProfile_next",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfile dxfg_Iterable_InstrumentProfile_next(
      final IsolateThread ignoredThread,
      final DxfgIterableInstrumentProfile dxfgIterableInstrumentProfile
  ) {
    return MAPPER_INSTRUMENT_PROFILE.toNative(
        MAPPER_ITERABLE_INSTRUMENT_PROFILE.toJava(dxfgIterableInstrumentProfile).next()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileUpdateListener_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileUpdateListener dxfg_InstrumentProfileUpdateListener_new(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileUpdateListenerFunction dxfgFunction,
      final VoidPointer userData
  ) {
    return MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER.toNative(
        instruments -> {
          final DxfgIterableInstrumentProfile iterator
              = MAPPER_ITERABLE_INSTRUMENT_PROFILE.toNative(instruments);
          dxfgFunction.invoke(
              CurrentIsolate.getCurrentThread(),
              iterator,
              userData
          );
          MAPPER_ITERABLE_INSTRUMENT_PROFILE.release(iterator);
        });
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_release(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile dxfgInstrumentProfile
  ) {
    MAPPER_INSTRUMENT_PROFILE.release(dxfgInstrumentProfile);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_InstrumentProfile_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_InstrumentProfile_release(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileList dxfgInstrumentProfileList
  ) {
    MAPPER_INSTRUMENT_PROFILES.release(dxfgInstrumentProfileList);
    return EXECUTE_SUCCESSFULLY;
  }
}
