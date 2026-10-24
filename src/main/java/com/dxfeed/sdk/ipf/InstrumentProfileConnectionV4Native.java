// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link InstrumentProfileConnectionNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(IpfDirectives.class)
public final class InstrumentProfileConnectionV4Native {

    private InstrumentProfileConnectionV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_createConnection",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_createConnection(
            final IsolateThread thread,
            final CCharPointer address,
            final DxfgInstrumentProfileCollector dxfgInstrumentProfileCollector,
            @DxfgOut final WordPointer ipfConnection
    ) {
        if (ipfConnection.isNull()) {
            throw new IllegalArgumentException("The `ipfConnection` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileConnection result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_createConnection(
                        thread,
                        address,
                        dxfgInstrumentProfileCollector);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        ipfConnection.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_getAddress",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_getAddress(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection,
            @DxfgOut final WordPointer address
    ) {
        if (address.isNull()) {
            throw new IllegalArgumentException("The `address` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_getAddress(
                        thread,
                        connection);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        address.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_getUpdatePeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_getUpdatePeriod(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection,
            @DxfgOut final CLongPointer updatePeriod
    ) {
        if (updatePeriod.isNull()) {
            throw new IllegalArgumentException("The `updatePeriod` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_getUpdatePeriod(
                        thread,
                        connection);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        updatePeriod.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_setUpdatePeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_setUpdatePeriod(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection,
            final long newValue
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_setUpdatePeriod(
                        thread,
                        connection,
                        newValue);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_getState",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_getState(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection,
            @DxfgOut final CInt32Pointer state
    ) {
        if (state.isNull()) {
            throw new IllegalArgumentException("The `state` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_getState(
                        thread,
                        connection);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        state.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_getLastModified",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_getLastModified(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection,
            @DxfgOut final CLongPointer lastModified
    ) {
        if (lastModified.isNull()) {
            throw new IllegalArgumentException("The `lastModified` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_getLastModified(
                        thread,
                        connection);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        lastModified.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_start",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_start(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_start(
                        thread,
                        connection);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_close(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_close(
                        thread,
                        connection);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IpfPropertyChangeListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IpfPropertyChangeListener_new(
            final IsolateThread thread,
            final DxfgStateChangeListenerFunction function,
            final VoidPointer userData,
            @DxfgOut final WordPointer ipfConnectionStateChangeListener
    ) {
        if (ipfConnectionStateChangeListener.isNull()) {
            throw new IllegalArgumentException("The `ipfConnectionStateChangeListener` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIpfConnectionStateChangeListener result = InstrumentProfileConnectionNative.dxfg_IpfPropertyChangeListener_new(
                        thread,
                        function,
                        userData);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        ipfConnectionStateChangeListener.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_addStateChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_addStateChangeListener(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection,
            final DxfgIpfConnectionStateChangeListener listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_addStateChangeListener(
                        thread,
                        connection,
                        listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_removeStateChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_removeStateChangeListener(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection,
            final DxfgIpfConnectionStateChangeListener listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_removeStateChangeListener(
                        thread,
                        connection,
                        listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileConnection_waitUntilCompleted",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileConnection_waitUntilCompleted(
            final IsolateThread thread,
            final DxfgInstrumentProfileConnection connection,
            final long timeoutInMs,
            @DxfgOut final CInt32Pointer completed
    ) {
        if (completed.isNull()) {
            throw new IllegalArgumentException("The `completed` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileConnectionNative.dxfg_InstrumentProfileConnection_waitUntilCompleted(
                        thread,
                        connection,
                        timeoutInMs);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        completed.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
