// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.InstrumentProfileField;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CDoublePointer;

@CContext(Directives.class)
public class InstrumentProfileFieldNative {

    @CEntryPoint(
            name = "dxfg_InstrumentProfileField_formatNumber",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_InstrumentProfileField_formatNumber(
            final IsolateThread ignoredThread,
            double number,
            @DxfgOut final CCharPointerPointer result
    ) {
        if (result.isNull()) {
            throw new IllegalArgumentException("The `result` pointer is null");
        }

        result.write(NativeUtils.MAPPER_STRING.toNative(InstrumentProfileField.formatNumber(number)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_InstrumentProfileField_parseNumber",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_InstrumentProfileField_parseNumber(
            final IsolateThread ignoredThread,
            @CConst CCharPointer string,
            @DxfgOut final CDoublePointer number
    ) {
        if (number.isNull()) {
            throw new IllegalArgumentException("The `number` pointer is null");
        }

        number.write(InstrumentProfileField.parseNumber(NativeUtils.MAPPER_STRING.toJava(string)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_InstrumentProfileField_formatDate",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_InstrumentProfileField_formatDate(
            final IsolateThread ignoredThread,
            int date,
            @DxfgOut final CCharPointerPointer result
    ) {
        if (result.isNull()) {
            throw new IllegalArgumentException("The `result` pointer is null");
        }

        result.write(NativeUtils.MAPPER_STRING.toNative(InstrumentProfileField.formatDate(date)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_InstrumentProfileField_parseDate",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_InstrumentProfileField_parseDate(
            final IsolateThread ignoredThread,
            @CConst CCharPointer string,
            @DxfgOut final CInt32Pointer date
    ) {
        if (date.isNull()) {
            throw new IllegalArgumentException("The `date` pointer is null");
        }

        date.write(InstrumentProfileField.parseDate(NativeUtils.MAPPER_STRING.toJava(string)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
