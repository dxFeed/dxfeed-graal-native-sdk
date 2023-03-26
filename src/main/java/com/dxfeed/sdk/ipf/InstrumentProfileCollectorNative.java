package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.live.InstrumentProfileCollector;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.javac.DxfgExecuter;
import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.sdk.javac.JavaObjectHandler;
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
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toNative(new InstrumentProfileCollector());
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_getLastUpdateTime",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_InstrumentProfileCollector_getLastUpdateTime(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).getLastUpdateTime();
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
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).updateInstrumentProfile(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(dxfgInstrumentProfile)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).updateInstrumentProfiles(
        NativeUtils.MAPPER_INSTRUMENT_PROFILES.toJavaList(dxfgInstrumentProfiles),
        NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava(generation)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).removeGenerations(
        new HashSet<>(NativeUtils.MAPPER_JAVA_OBJECT_HANDLERS.toJavaList(generations))
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_view",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgIterableInstrumentProfile dxfg_InstrumentProfileCollector_view(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector
  ) {
    return NativeUtils.MAPPER_ITERABLE_INSTRUMENT_PROFILE.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).view().iterator()
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
    return NativeUtils.MAPPER_EXECUTOR.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).getExecutor()
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
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).setExecutor(
        NativeUtils.MAPPER_EXECUTOR.toJava(dxfgExecuter)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCollector_addUpdateListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCollector_addUpdateListener(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCollector collector,
      final DxfgInstrumentProfileUpdateListener dxfgListener
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).addUpdateListener(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER.toJava(dxfgListener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(collector).removeUpdateListener(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER.toJava(dxfgInstrumentProfileUpdateListener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Iterable_InstrumentProfile_hasNext",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Iterable_InstrumentProfile_hasNext(
      final IsolateThread ignoredThread,
      final DxfgIterableInstrumentProfile dxfgIterableInstrumentProfile
  ) {
    return NativeUtils.MAPPER_ITERABLE_INSTRUMENT_PROFILE.toJava(dxfgIterableInstrumentProfile).hasNext() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Iterable_InstrumentProfile_next",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfile dxfg_Iterable_InstrumentProfile_next(
      final IsolateThread ignoredThread,
      final DxfgIterableInstrumentProfile dxfgIterableInstrumentProfile
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toNative(
        NativeUtils.MAPPER_ITERABLE_INSTRUMENT_PROFILE.toJava(dxfgIterableInstrumentProfile).next()
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
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_UPDATE_LISTENER.toNative(
        instruments -> {
          final DxfgIterableInstrumentProfile iterator
              = NativeUtils.MAPPER_ITERABLE_INSTRUMENT_PROFILE.toNative(instruments);
          dxfgFunction.invoke(
              CurrentIsolate.getCurrentThread(),
              iterator,
              userData
          );
          NativeUtils.MAPPER_ITERABLE_INSTRUMENT_PROFILE.release(iterator);
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
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.release(dxfgInstrumentProfile);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_InstrumentProfile_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_InstrumentProfile_release(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileList dxfgInstrumentProfileList
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILES.release(dxfgInstrumentProfileList);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }
}
