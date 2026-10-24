// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CConst;

/**
 * The v4 twins of the functions of {@link InstrumentProfile2Native}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(IpfDirectives.class)
public final class InstrumentProfile2V4Native {

    private InstrumentProfile2V4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_instrument_profile_data_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_instrument_profile_data_release(
            final IsolateThread thread,
            final DxfgInstrumentProfile2Pointer instrumentProfile
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfile2Native.dxfgInstrumentProfileFree(thread, instrumentProfile);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_instrument_profile_data_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_instrument_profile_data_list_release(
            final IsolateThread thread,
            @CConst final DxfgInstrumentProfile2ListPointer instrumentProfiles
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfile2Native.dxfgInstrumentProfile2ListFree(thread, instrumentProfiles);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
