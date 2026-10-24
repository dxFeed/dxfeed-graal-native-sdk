// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import com.dxfeed.sdk.javac.DxfgCStringListPointerPointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CDoublePointer;

/**
 * The v4 twins of the functions of {@link InstrumentProfileCustomFieldsNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(IpfDirectives.class)
public final class InstrumentProfileCustomFieldsV4Native {

    private InstrumentProfileCustomFieldsV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_new(
            final IsolateThread thread,
            @DxfgOut final DxfgInstrumentProfileCustomFieldsHandlePointer customFields
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_new(
                        thread,
                        customFields);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_newCopy",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_newCopy(
            final IsolateThread thread,
            final DxfgInstrumentProfileCustomFieldsHandle source,
            @DxfgOut final DxfgInstrumentProfileCustomFieldsHandlePointer customFields
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_new3(
                        thread,
                        source,
                        customFields);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_newWithNamesAndValues",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_newWithNamesAndValues(
            final IsolateThread thread,
            @CConst final DxfgCStringListPointer customFieldsStringList,
            @DxfgOut final DxfgInstrumentProfileCustomFieldsHandlePointer customFields
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_new4(
                        thread,
                        customFieldsStringList,
                        customFields);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_getField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_getField(
            final IsolateThread thread,
            final DxfgInstrumentProfileCustomFieldsHandle customFields,
            @CConst final CCharPointer name,
            @DxfgOut final CCharPointerPointer result
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int status = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_getField(
                        thread,
                        customFields,
                        name,
                        result);

        return OlderEntryPoint.failed(stored, status == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : status;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_setField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_setField(
            final IsolateThread thread,
            final DxfgInstrumentProfileCustomFieldsHandle customFields,
            @CConst final CCharPointer name,
            @CConst final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_setField(
                        thread,
                        customFields,
                        name,
                        value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_getNumericField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_getNumericField(
            final IsolateThread thread,
            final DxfgInstrumentProfileCustomFieldsHandle customFields,
            @CConst final CCharPointer name,
            @DxfgOut final CDoublePointer result
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int status = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_getNumericField(
                        thread,
                        customFields,
                        name,
                        result);

        return OlderEntryPoint.failed(stored, status == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : status;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_setNumericField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_setNumericField(
            final IsolateThread thread,
            final DxfgInstrumentProfileCustomFieldsHandle customFields,
            @CConst final CCharPointer name,
            final double value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_setNumericField(
                        thread,
                        customFields,
                        name,
                        value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_getDateField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_getDateField(
            final IsolateThread thread,
            final DxfgInstrumentProfileCustomFieldsHandle customFields,
            @CConst final CCharPointer name,
            @DxfgOut final CInt32Pointer result
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int status = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_getDateField(
                        thread,
                        customFields,
                        name,
                        result);

        return OlderEntryPoint.failed(stored, status == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : status;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_setDateField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_setDateField(
            final IsolateThread thread,
            final DxfgInstrumentProfileCustomFieldsHandle customFields,
            @CConst final CCharPointer name,
            final int value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_setDateField(
                        thread,
                        customFields,
                        name,
                        value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCustomFields_getNonEmptyFieldNames",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCustomFields_getNonEmptyFieldNames(
            final IsolateThread thread,
            final DxfgInstrumentProfileCustomFieldsHandle customFields,
            @DxfgOut final DxfgCStringListPointerPointer targetFieldNames
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCustomFieldsNative.dxfg_InstrumentProfileCustomFields_getNonEmptyFieldNames(
                        thread,
                        customFields,
                        targetFieldNames);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
