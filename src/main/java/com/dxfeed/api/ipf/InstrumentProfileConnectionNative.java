package com.dxfeed.api.ipf;

import static com.dxfeed.api.NativeUtils.FINALIZER;
import static com.dxfeed.api.NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR;
import static com.dxfeed.api.NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION;
import static com.dxfeed.api.NativeUtils.MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.javac.DxfgFinalizeFunction;
import com.dxfeed.ipf.live.InstrumentProfileConnection;
import java.util.concurrent.TimeUnit;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class InstrumentProfileConnectionNative {

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_createConnection",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfileConnection dxfg_InstrumentProfileConnection_createConnection(
      final IsolateThread ignoredThread,
      final CCharPointer address,
      final DxfgInstrumentProfileCollector dxfgInstrumentProfileCollector
  ) {
    return MAPPER_INSTRUMENT_PROFILE_CONNECTION.toNative(
        InstrumentProfileConnection.createConnection(
            MAPPER_STRING.toJava(address),
            MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(dxfgInstrumentProfileCollector)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_getAddress",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfileConnection_getAddress(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    return MAPPER_STRING.toNative(
        MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).getAddress()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_getUpdatePeriod",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_InstrumentProfileConnection_getUpdatePeriod(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    return MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).getUpdatePeriod();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_setUpdatePeriod",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_setUpdatePeriod(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection,
      final long newValue
  ) {
    MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).setUpdatePeriod(newValue);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_getLastModified",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_InstrumentProfileConnection_getLastModified(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    return MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).getLastModified();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_start",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_start(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).start();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_close(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).close();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IpfPropertyChangeListener_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgIpfConnectionStateChangeListener dxfg_IpfPropertyChangeListener_new(
      final IsolateThread ignoredThread,
      final DxfgStateChangeListenerFunction function,
      final VoidPointer userData
  ) {
    return MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER.toNative(
        changeEvent -> function.invoke(
            CurrentIsolate.getCurrentThread(),
            DxfgInstrumentProfileConnectionState.of(
                (InstrumentProfileConnection.State) changeEvent.getOldValue()
            ),
            DxfgInstrumentProfileConnectionState.of(
                (InstrumentProfileConnection.State) changeEvent.getNewValue()
            ),
            userData
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_addStateChangeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_addStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection,
      final DxfgIpfConnectionStateChangeListener listener,
      final DxfgFinalizeFunction finalizeFunction,
      final VoidPointer userData
  ) {
    MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).addStateChangeListener(
        FINALIZER.wrapObjectWithFinalizer(
            MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER.toJava(listener),
            () -> finalizeFunction.invoke(CurrentIsolate.getCurrentThread(), userData)
        )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_removeStateChangeListener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_removeStateChangeListener(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection,
      final DxfgIpfConnectionStateChangeListener listener
  ) {
    MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).removeStateChangeListener(
        MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER.toJava(listener)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_waitUntilCompleted",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_waitUntilCompleted(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection,
      final long timeoutInMs
  ) {
    MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection)
        .waitUntilCompleted(timeoutInMs, TimeUnit.MILLISECONDS);
    return EXECUTE_SUCCESSFULLY;
  }
}
