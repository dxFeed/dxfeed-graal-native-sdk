// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.qds;

import static com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.devexperts.qd.QDFactory;
import com.devexperts.qd.tools.Tools;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import java.util.Arrays;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public class QdsNative {

    @CEntryPoint(
            name = "dxfg_Tools_parseSymbols",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgCStringListPointer dxfg_Tools_parseSymbols(
            final IsolateThread ignoredThread,
            final CCharPointer symbolList
    ) {
        return NativeUtils.MAPPER_STRINGS.toNativeList(
                Arrays.asList(
                        Tools.parseSymbols(NativeUtils.MAPPER_STRING.toJava(symbolList), QDFactory.getDefaultScheme())
                )
        );
    }

    @CEntryPoint(
            name = "dxfg_Tools_main",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_Tools_main(
            final IsolateThread ignoredThread,
            final DxfgCStringListPointer dxfgCStringListPointer
    ) {
        Tools.main(NativeUtils.MAPPER_STRINGS.toJavaList(dxfgCStringListPointer).toArray(new String[0]));
        return EXECUTE_SUCCESSFULLY;
    }
}
