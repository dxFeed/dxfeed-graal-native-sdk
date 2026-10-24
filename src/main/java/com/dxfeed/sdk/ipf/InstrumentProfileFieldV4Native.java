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
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CDoublePointer;

/**
 * The v4 twins of the functions of {@link InstrumentProfileFieldNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(IpfDirectives.class)
public final class InstrumentProfileFieldV4Native {

    private InstrumentProfileFieldV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileField_formatNumber",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileField_formatNumber(
            final IsolateThread thread,
            final double number,
            @DxfgOut final CCharPointerPointer result
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int status = InstrumentProfileFieldNative.dxfg_InstrumentProfileField_formatNumber(
                        thread,
                        number,
                        result);

        return OlderEntryPoint.failed(stored, status == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : status;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileField_parseNumber",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileField_parseNumber(
            final IsolateThread thread,
            @CConst final CCharPointer string,
            @DxfgOut final CDoublePointer number
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileFieldNative.dxfg_InstrumentProfileField_parseNumber(
                        thread,
                        string,
                        number);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileField_formatDate",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileField_formatDate(
            final IsolateThread thread,
            final int date,
            @DxfgOut final CCharPointerPointer result
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int status = InstrumentProfileFieldNative.dxfg_InstrumentProfileField_formatDate(thread, date, result);

        return OlderEntryPoint.failed(stored, status == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : status;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileField_parseDate",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileField_parseDate(
            final IsolateThread thread,
            @CConst final CCharPointer string,
            @DxfgOut final CInt32Pointer date
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileFieldNative.dxfg_InstrumentProfileField_parseDate(thread, string, date);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
