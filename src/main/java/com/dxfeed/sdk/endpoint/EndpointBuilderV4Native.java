// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.endpoint;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import java.io.IOException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link EndpointBuilderNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(EndpointDirectives.class)
public final class EndpointBuilderV4Native {

    private EndpointBuilderV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_newBuilder",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_newBuilder(
            final IsolateThread thread,
            @DxfgOut final WordPointer builder
    ) {
        if (builder.isNull()) {
            throw new IllegalArgumentException("The `builder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEndpointBuilder result = EndpointBuilderNative.dxfg_DXEndpoint_newBuilder(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        builder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_Builder_withRole",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_Builder_withRole(
            final IsolateThread thread,
            final DxfgEndpointBuilder builder,
            final DxfgEndpointRole role
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointBuilderNative.dxfg_DXEndpoint_Builder_withRole(thread, builder, role);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_Builder_withName",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_Builder_withName(
            final IsolateThread thread,
            final DxfgEndpointBuilder builder,
            final CCharPointer name
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointBuilderNative.dxfg_DXEndpoint_Builder_withName(thread, builder, name);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_Builder_withProperty",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_Builder_withProperty(
            final IsolateThread thread,
            final DxfgEndpointBuilder builder,
            final CCharPointer key,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointBuilderNative.dxfg_DXEndpoint_Builder_withProperty(thread, builder, key, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_Builder_withProperties",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_Builder_withProperties(
            final IsolateThread thread,
            final DxfgEndpointBuilder builder,
            final CCharPointer pathToFile
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointBuilderNative.dxfg_DXEndpoint_Builder_withProperties(thread, builder, pathToFile);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_Builder_withExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_Builder_withExecutor(
            final IsolateThread thread,
            final DxfgEndpointBuilder builder,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointBuilderNative.dxfg_DXEndpoint_Builder_withExecutor(thread, builder, executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_Builder_supportsProperty",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_Builder_supportsProperty(
            final IsolateThread thread,
            final DxfgEndpointBuilder builder,
            final CCharPointer key,
            @DxfgOut final CInt32Pointer supported
    ) {
        if (supported.isNull()) {
            throw new IllegalArgumentException("The `supported` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointBuilderNative.dxfg_DXEndpoint_Builder_supportsProperty(thread, builder, key);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        supported.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_Builder_build",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_Builder_build(
            final IsolateThread thread,
            final DxfgEndpointBuilder builder,
            @DxfgOut final WordPointer endpoint
    ) {
        if (endpoint.isNull()) {
            throw new IllegalArgumentException("The `endpoint` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEndpoint result = EndpointBuilderNative.dxfg_DXEndpoint_Builder_build(thread, builder);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        endpoint.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
