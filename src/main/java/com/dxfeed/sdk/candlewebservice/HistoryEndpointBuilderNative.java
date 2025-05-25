// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.candlewebservice;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;

@CContext(Directives.class)
public class HistoryEndpointBuilderNative {

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_Builder_withAddress",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_Builder_withAddress(
            final IsolateThread ignoreThread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @CConst final CCharPointer address
    ) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_HISTORY_ENDPOINT_BUILDER.toJava(builder)
                .withAddress(NativeUtils.MAPPER_STRING.toJava(address));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_Builder_withUserName",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_Builder_withUserName(
            final IsolateThread ignoreThread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @CConst final CCharPointer userName
    ) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_HISTORY_ENDPOINT_BUILDER.toJava(builder)
                .withUserName(NativeUtils.MAPPER_STRING.toJava(userName));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_Builder_withPassword",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_Builder_withPassword(
            final IsolateThread ignoreThread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @CConst final CCharPointer password
    ) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_HISTORY_ENDPOINT_BUILDER.toJava(builder)
                .withPassword(NativeUtils.MAPPER_STRING.toJava(password));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_Builder_withAuthToken",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_Builder_withAuthToken(
            final IsolateThread ignoreThread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @CConst final CCharPointer authToken
    ) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_HISTORY_ENDPOINT_BUILDER.toJava(builder)
                .withAuthToken(NativeUtils.MAPPER_STRING.toJava(authToken));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_Builder_withCompression",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_Builder_withCompression(
            final IsolateThread ignoreThread,
            final DxfgHistoryEndpointBuilderHandle builder,
            final DxfgHistoryEndpointCompression compression
    ) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_HISTORY_ENDPOINT_BUILDER.toJava(builder)
                .withCompression(compression.qdCompression);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_Builder_withFormat",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_Builder_withFormat(
            final IsolateThread ignoreThread,
            final DxfgHistoryEndpointBuilderHandle builder,
            final DxfgHistoryEndpointFormat format
    ) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_HISTORY_ENDPOINT_BUILDER.toJava(builder)
                .withFormat(format.qdFormat);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_Builder_build",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_newBuilder(
            final IsolateThread ignoreThread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @DxfgOut final DxfgHistoryEndpointHandlePointer endpoint
    ) {
        if (endpoint.isNull()) {
            throw new IllegalArgumentException("The `endpoint` pointer is null");
        }

        //noinspection DataFlowIssue
        endpoint.write(NativeUtils.MAPPER_HISTORY_ENDPOINT.toNative(
                NativeUtils.MAPPER_HISTORY_ENDPOINT_BUILDER.toJava(builder).build()));
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
