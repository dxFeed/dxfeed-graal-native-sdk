package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.live.InstrumentProfileConnection;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

import java.util.concurrent.TimeUnit;

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
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toNative(
        InstrumentProfileConnection.createConnection(
            NativeUtils.MAPPER_STRING.toJava(address),
            NativeUtils.MAPPER_INSTRUMENT_PROFILE_COLLECTOR.toJava(dxfgInstrumentProfileCollector)
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
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).getAddress()
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
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).getUpdatePeriod();
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
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).setUpdatePeriod(newValue);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_getState",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_getState(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    return DxfgInstrumentProfileConnectionState.of(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).getState()
    ).getCValue();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_getLastModified",
      exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
  )
  public static long dxfg_InstrumentProfileConnection_getLastModified(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).getLastModified();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_start",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_start(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).start();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileConnection_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileConnection_close(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileConnection connection
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).close();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    return NativeUtils.MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER.toNative(
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
      final DxfgIpfConnectionStateChangeListener listener
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).addStateChangeListener(
        NativeUtils.MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER.toJava(listener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection).removeStateChangeListener(
        NativeUtils.MAPPER_IPF_CONNECTION_STATE_CHANGE_LISTENER.toJava(listener)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE_CONNECTION.toJava(connection)
        .waitUntilCompleted(timeoutInMs, TimeUnit.MILLISECONDS) ? 1 : 0;
  }
}
