// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.qds;

import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link QdsNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(QdsDirectives.class)
public final class QdsV4Native {

    private QdsV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_Tools_parseSymbols",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Tools_parseSymbols(
            final IsolateThread thread,
            final CCharPointer symbolList,
            @DxfgOut final WordPointer symbols
    ) {
        if (symbols.isNull()) {
            throw new IllegalArgumentException("The `symbols` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgCStringListPointer result = QdsNative.dxfg_Tools_parseSymbols(thread, symbolList);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        symbols.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Tools_main",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Tools_main(
            final IsolateThread thread,
            final DxfgCStringListPointer dxfgCStringListPointer
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = QdsNative.dxfg_Tools_main(thread, dxfgCStringListPointer);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
