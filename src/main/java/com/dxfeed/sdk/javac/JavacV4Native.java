// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link JavacNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(JavacDirectives.class)
public final class JavacV4Native {

    private JavacV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_Object_finalize",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Object_finalize(
            final IsolateThread thread,
            final JavaObjectHandler<Object> dxfgObjectHandler,
            final DxfgFinalizeFunction finalizeFunction,
            final VoidPointer userData
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_Object_finalize(thread, dxfgObjectHandler, finalizeFunction, userData);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_JavaObjectHandler_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_JavaObjectHandler_release(
            final IsolateThread thread,
            final JavaObjectHandler<Object> javaObjectHandler
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_JavaObjectHandler_release(thread, javaObjectHandler);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_NativeListener_deactivate",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_NativeListener_deactivate(
            final IsolateThread thread,
            final JavaObjectHandler<Object> listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_NativeListener_deactivate(thread, listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_NativeListener_deactivateAll",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_NativeListener_deactivateAll(
            final IsolateThread ignoredThread
    ) {
        NativeListener.deactivateAll();

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_JavaObjectHandler_clone",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_JavaObjectHandler_clone(
            final IsolateThread thread,
            final JavaObjectHandler<Object> javaObjectHandler,
            @DxfgOut final DxfgJavaObjectHandlerPointer javaObjectHandlerClone
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_JavaObjectHandler_clone(
                        thread,
                        javaObjectHandler,
                        javaObjectHandlerClone);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_JavaObjectHandler_array_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_JavaObjectHandler_array_release(
            final IsolateThread thread,
            @CConst final JavaObjectHandler<Object> handlersArray,
            final int size
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_JavaObjectHandler_array_release(thread, handlersArray, size);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Object_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Object_new(
            final IsolateThread thread,
            @DxfgOut final DxfgJavaObjectHandlePointer object
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_Object_new(thread, object);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Object_toString",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Object_toString(
            final IsolateThread thread,
            final JavaObjectHandler<Object> dxfgObjectHandler,
            @DxfgOut final WordPointer string
    ) {
        if (string.isNull()) {
            throw new IllegalArgumentException("The `string` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_Object_toString(thread, dxfgObjectHandler);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        string.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Object_equals",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Object_equals(
            final IsolateThread thread,
            final JavaObjectHandler<Object> dxfgObjectHandler,
            final JavaObjectHandler<Object> dxfgOther,
            @DxfgOut final CInt32Pointer equal
    ) {
        if (equal.isNull()) {
            throw new IllegalArgumentException("The `equal` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_Object_equals(thread, dxfgObjectHandler, dxfgOther);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        equal.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Object_hashCode",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Object_hashCode(
            final IsolateThread thread,
            final JavaObjectHandler<Object> dxfgObjectHandler,
            @DxfgOut final CInt32Pointer hashCode
    ) {
        if (hashCode.isNull()) {
            throw new IllegalArgumentException("The `hashCode` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_Object_hashCode(thread, dxfgObjectHandler);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        hashCode.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Comparable_compareTo",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Comparable_compareTo(
            final IsolateThread thread,
            final JavaObjectHandler<Object> objectHandler,
            final JavaObjectHandler<Object> otherHandler,
            @DxfgOut final CInt32Pointer comparison
    ) {
        if (comparison.isNull()) {
            throw new IllegalArgumentException("The `comparison` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_Comparable_compareTo(thread, objectHandler, otherHandler);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        comparison.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_java_object_handler_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_java_object_handler_list_release(
            final IsolateThread thread,
            final DxfgJavaObjectHandlerList cList
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_CList_JavaObjectHandler_release(thread, cList);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_String_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_String_release(
            final IsolateThread thread,
            final CCharPointer string
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_String_release(thread, string);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_string_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_string_list_release(
            final IsolateThread thread,
            final DxfgCStringListPointer strings
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_CList_String_release(thread, strings);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Executors_newFixedThreadPool",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Executors_newFixedThreadPool(
            final IsolateThread thread,
            final int nThreads,
            final CCharPointer name,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgExecutorHandle result = JavacNative.dxfg_Executors_newFixedThreadPool(thread, nThreads, name);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        executor.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Executors_newScheduledThreadPool",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Executors_newScheduledThreadPool(
            final IsolateThread thread,
            final int nThreads,
            final CCharPointer name,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgExecutorHandle result = JavacNative.dxfg_Executors_newScheduledThreadPool(thread, nThreads, name);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        executor.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_new(
            final IsolateThread thread,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgExecutorHandle result = JavacNative.dxfg_ExecutorBaseOnConcurrentLinkedQueue_new(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        executor.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(
            final IsolateThread thread,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(
                        thread,
                        executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_ByteArrayInputStream_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ByteArrayInputStream_new(
            final IsolateThread thread,
            final CCharPointer dxfgBytes,
            final int size,
            @DxfgOut final WordPointer inputStream
    ) {
        if (inputStream.isNull()) {
            throw new IllegalArgumentException("The `inputStream` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInputStream result = JavacNative.dxfg_ByteArrayInputStream_new(thread, dxfgBytes, size);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        inputStream.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_gc",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_gc(
            final IsolateThread thread
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_gc(thread);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_throw_exception",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_throw_exception(
            final IsolateThread thread,
            @DxfgOut final WordPointer object
    ) {
        if (object.isNull()) {
            throw new IllegalArgumentException("The `object` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final JavaObjectHandler<?> result = JavacNative.dxfg_throw_exception(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        object.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getTimeZone",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getTimeZone(
            final IsolateThread thread,
            final CCharPointer ID,
            @DxfgOut final WordPointer timeZone
    ) {
        if (timeZone.isNull()) {
            throw new IllegalArgumentException("The `timeZone` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeZone result = JavacNative.dxfg_TimeZone_getTimeZone(thread, ID);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeZone.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getDefault",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getDefault(
            final IsolateThread thread,
            @DxfgOut final WordPointer timeZone
    ) {
        if (timeZone.isNull()) {
            throw new IllegalArgumentException("The `timeZone` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeZone result = JavacNative.dxfg_TimeZone_getDefault(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeZone.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getID",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getID(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            @DxfgOut final WordPointer id
    ) {
        if (id.isNull()) {
            throw new IllegalArgumentException("The `id` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_TimeZone_getID(thread, dxfgTimeZone);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        id.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getDisplayName",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getDisplayName(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            @DxfgOut final WordPointer displayName
    ) {
        if (displayName.isNull()) {
            throw new IllegalArgumentException("The `displayName` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_TimeZone_getDisplayName(thread, dxfgTimeZone);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        displayName.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getDisplayNameWithStyle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getDisplayNameWithStyle(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            final int daylight,
            final int style,
            @DxfgOut final WordPointer displayName
    ) {
        if (displayName.isNull()) {
            throw new IllegalArgumentException("The `displayName` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_TimeZone_getDisplayName2(thread, dxfgTimeZone, daylight, style);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        displayName.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getDSTSavings",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getDSTSavings(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            @DxfgOut final CInt32Pointer dstSavings
    ) {
        if (dstSavings.isNull()) {
            throw new IllegalArgumentException("The `dstSavings` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_getDSTSavings(thread, dxfgTimeZone);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        dstSavings.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_useDaylightTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_useDaylightTime(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            @DxfgOut final CInt32Pointer useDaylightTime
    ) {
        if (useDaylightTime.isNull()) {
            throw new IllegalArgumentException("The `useDaylightTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_useDaylightTime(thread, dxfgTimeZone);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        useDaylightTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_observesDaylightTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_observesDaylightTime(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            @DxfgOut final CInt32Pointer observesDaylightTime
    ) {
        if (observesDaylightTime.isNull()) {
            throw new IllegalArgumentException("The `observesDaylightTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_observesDaylightTime(thread, dxfgTimeZone);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        observesDaylightTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getOffset",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getOffset(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            final long date,
            @DxfgOut final CInt32Pointer offset
    ) {
        if (offset.isNull()) {
            throw new IllegalArgumentException("The `offset` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_getOffset(thread, dxfgTimeZone, date);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        offset.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getOffsetByCalendarFields",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getOffsetByCalendarFields(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            final int era,
            final int year,
            final int month,
            final int day,
            final int dayOfWeek,
            final int milliseconds,
            @DxfgOut final CInt32Pointer offset
    ) {
        if (offset.isNull()) {
            throw new IllegalArgumentException("The `offset` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_getOffset2(
                        thread,
                        dxfgTimeZone,
                        era,
                        year,
                        month,
                        day,
                        dayOfWeek,
                        milliseconds);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        offset.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_getRawOffset",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_getRawOffset(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            @DxfgOut final CInt32Pointer rawOffset
    ) {
        if (rawOffset.isNull()) {
            throw new IllegalArgumentException("The `rawOffset` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_getRawOffset(thread, dxfgTimeZone);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        rawOffset.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_hasSameRules",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_hasSameRules(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            final DxfgTimeZone other,
            @DxfgOut final CInt32Pointer hasSameRules
    ) {
        if (hasSameRules.isNull()) {
            throw new IllegalArgumentException("The `hasSameRules` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_hasSameRules(thread, dxfgTimeZone, other);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        hasSameRules.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_inDaylightTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_inDaylightTime(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            final long date,
            @DxfgOut final CInt32Pointer inDaylightTime
    ) {
        if (inDaylightTime.isNull()) {
            throw new IllegalArgumentException("The `inDaylightTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_inDaylightTime(thread, dxfgTimeZone, date);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        inDaylightTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_setID",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_setID(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            final CCharPointer ID
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_setID(thread, dxfgTimeZone, ID);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeZone_setRawOffset",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeZone_setRawOffset(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            final int offsetMillis
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimeZone_setRawOffset(thread, dxfgTimeZone, offsetMillis);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimePeriod_ZERO",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimePeriod_ZERO(
            final IsolateThread thread,
            @DxfgOut final WordPointer timePeriod
    ) {
        if (timePeriod.isNull()) {
            throw new IllegalArgumentException("The `timePeriod` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimePeriodHandle result = JavacNative.dxfg_TimePeriod_ZERO(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timePeriod.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimePeriod_UNLIMITED",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimePeriod_UNLIMITED(
            final IsolateThread thread,
            @DxfgOut final WordPointer timePeriod
    ) {
        if (timePeriod.isNull()) {
            throw new IllegalArgumentException("The `timePeriod` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimePeriodHandle result = JavacNative.dxfg_TimePeriod_UNLIMITED(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timePeriod.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimePeriod_valueOf",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimePeriod_valueOf(
            final IsolateThread thread,
            final long value,
            @DxfgOut final WordPointer timePeriod
    ) {
        if (timePeriod.isNull()) {
            throw new IllegalArgumentException("The `timePeriod` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimePeriodHandle result = JavacNative.dxfg_TimePeriod_valueOf(thread, value);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timePeriod.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimePeriod_valueOfString",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimePeriod_valueOfString(
            final IsolateThread thread,
            final CCharPointer value,
            @DxfgOut final WordPointer timePeriod
    ) {
        if (timePeriod.isNull()) {
            throw new IllegalArgumentException("The `timePeriod` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimePeriodHandle result = JavacNative.dxfg_TimePeriod_valueOf2(thread, value);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timePeriod.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimePeriod_getTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimePeriod_getTime(
            final IsolateThread thread,
            final DxfgTimePeriodHandle timePeriod,
            @DxfgOut final CLongPointer time
    ) {
        if (time.isNull()) {
            throw new IllegalArgumentException("The `time` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = JavacNative.dxfg_TimePeriod_getTime(thread, timePeriod);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        time.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimePeriod_getSeconds",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimePeriod_getSeconds(
            final IsolateThread thread,
            final DxfgTimePeriodHandle timePeriod,
            @DxfgOut final CInt32Pointer seconds
    ) {
        if (seconds.isNull()) {
            throw new IllegalArgumentException("The `seconds` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = JavacNative.dxfg_TimePeriod_getSeconds(thread, timePeriod);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        seconds.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimePeriod_getNanos",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimePeriod_getNanos(
            final IsolateThread thread,
            final DxfgTimePeriodHandle timePeriod,
            @DxfgOut final CLongPointer nanos
    ) {
        if (nanos.isNull()) {
            throw new IllegalArgumentException("The `nanos` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = JavacNative.dxfg_TimePeriod_getNanos(thread, timePeriod);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        nanos.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_DEFAULT",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_DEFAULT(
            final IsolateThread thread,
            @DxfgOut final WordPointer timeFormat
    ) {
        if (timeFormat.isNull()) {
            throw new IllegalArgumentException("The `timeFormat` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeFormat result = JavacNative.dxfg_TimeFormat_DEFAULT(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeFormat.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_GMT",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_GMT(
            final IsolateThread thread,
            @DxfgOut final WordPointer timeFormat
    ) {
        if (timeFormat.isNull()) {
            throw new IllegalArgumentException("The `timeFormat` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeFormat result = JavacNative.dxfg_TimeFormat_GMT(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeFormat.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_getInstance",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_getInstance(
            final IsolateThread thread,
            final DxfgTimeZone dxfgTimeZone,
            @DxfgOut final WordPointer timeFormat
    ) {
        if (timeFormat.isNull()) {
            throw new IllegalArgumentException("The `timeFormat` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeFormat result = JavacNative.dxfg_TimeFormat_getInstance(thread, dxfgTimeZone);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeFormat.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_withTimeZone",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_withTimeZone(
            final IsolateThread thread,
            final DxfgTimeFormat dxfgTimeFormat,
            @DxfgOut final WordPointer formatWithTimeZone
    ) {
        if (formatWithTimeZone.isNull()) {
            throw new IllegalArgumentException("The `formatWithTimeZone` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeFormat result = JavacNative.dxfg_TimeFormat_withTimeZone(thread, dxfgTimeFormat);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        formatWithTimeZone.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_withMillis",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_withMillis(
            final IsolateThread thread,
            final DxfgTimeFormat dxfgTimeFormat,
            @DxfgOut final WordPointer formatWithMillis
    ) {
        if (formatWithMillis.isNull()) {
            throw new IllegalArgumentException("The `formatWithMillis` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeFormat result = JavacNative.dxfg_TimeFormat_withMillis(thread, dxfgTimeFormat);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        formatWithMillis.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_asFullIso",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_asFullIso(
            final IsolateThread thread,
            final DxfgTimeFormat dxfgTimeFormat,
            @DxfgOut final WordPointer fullIsoFormat
    ) {
        if (fullIsoFormat.isNull()) {
            throw new IllegalArgumentException("The `fullIsoFormat` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeFormat result = JavacNative.dxfg_TimeFormat_asFullIso(thread, dxfgTimeFormat);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        fullIsoFormat.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_parse",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_parse(
            final IsolateThread thread,
            final DxfgTimeFormat dxfgTimeFormat,
            final CCharPointer value,
            @DxfgOut final CLongPointer time
    ) {
        if (time.isNull()) {
            throw new IllegalArgumentException("The `time` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = JavacNative.dxfg_TimeFormat_parse(thread, dxfgTimeFormat, value);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        time.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_format",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_format(
            final IsolateThread thread,
            final DxfgTimeFormat dxfgTimeFormat,
            final long time,
            @DxfgOut final WordPointer string
    ) {
        if (string.isNull()) {
            throw new IllegalArgumentException("The `string` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_TimeFormat_format(thread, dxfgTimeFormat, time);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        string.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeFormat_getTimeZone",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeFormat_getTimeZone(
            final IsolateThread thread,
            final DxfgTimeFormat dxfgTimeFormat,
            @DxfgOut final WordPointer timeZone
    ) {
        if (timeZone.isNull()) {
            throw new IllegalArgumentException("The `timeZone` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeZone result = JavacNative.dxfg_TimeFormat_getTimeZone(thread, dxfgTimeFormat);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeZone.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_valueOf",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_valueOf(
            final IsolateThread thread,
            final CCharPointer value,
            @DxfgOut final WordPointer authToken
    ) {
        if (authToken.isNull()) {
            throw new IllegalArgumentException("The `authToken` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgAuthToken result = JavacNative.dxfg_AuthToken_valueOf(thread, value);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        authToken.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_createBasicToken",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_createBasicToken(
            final IsolateThread thread,
            final CCharPointer userPassword,
            @DxfgOut final WordPointer authToken
    ) {
        if (authToken.isNull()) {
            throw new IllegalArgumentException("The `authToken` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgAuthToken result = JavacNative.dxfg_AuthToken_createBasicToken(thread, userPassword);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        authToken.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_createBasicTokenWithUserAndPassword",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_createBasicTokenWithUserAndPassword(
            final IsolateThread thread,
            final CCharPointer user,
            final CCharPointer password,
            @DxfgOut final WordPointer authToken
    ) {
        if (authToken.isNull()) {
            throw new IllegalArgumentException("The `authToken` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgAuthToken result = JavacNative.dxfg_AuthToken_createBasicToken2(thread, user, password);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        authToken.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_createBasicTokenOrNull",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_createBasicTokenOrNull(
            final IsolateThread thread,
            final CCharPointer user,
            final CCharPointer password,
            @DxfgOut final WordPointer authToken
    ) {
        if (authToken.isNull()) {
            throw new IllegalArgumentException("The `authToken` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgAuthToken result = JavacNative.dxfg_AuthToken_createBasicTokenOrNull(thread, user, password);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        authToken.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_createBearerToken",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_createBearerToken(
            final IsolateThread thread,
            final CCharPointer token,
            @DxfgOut final WordPointer authToken
    ) {
        if (authToken.isNull()) {
            throw new IllegalArgumentException("The `authToken` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgAuthToken result = JavacNative.dxfg_AuthToken_createBearerToken(thread, token);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        authToken.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_createBearerTokenOrNull",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_createBearerTokenOrNull(
            final IsolateThread thread,
            final CCharPointer token,
            @DxfgOut final WordPointer authToken
    ) {
        if (authToken.isNull()) {
            throw new IllegalArgumentException("The `authToken` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgAuthToken result = JavacNative.dxfg_AuthToken_createBearerTokenOrNull(thread, token);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        authToken.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_createCustomToken",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_createCustomToken(
            final IsolateThread thread,
            final CCharPointer scheme,
            final CCharPointer value,
            @DxfgOut final WordPointer authToken
    ) {
        if (authToken.isNull()) {
            throw new IllegalArgumentException("The `authToken` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgAuthToken result = JavacNative.dxfg_AuthToken_createCustomToken(thread, scheme, value);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        authToken.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_getHttpAuthorization",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_getHttpAuthorization(
            final IsolateThread thread,
            final DxfgAuthToken token,
            @DxfgOut final WordPointer httpAuthorization
    ) {
        if (httpAuthorization.isNull()) {
            throw new IllegalArgumentException("The `httpAuthorization` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_AuthToken_getHttpAuthorization(thread, token);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        httpAuthorization.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_getUser",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_getUser(
            final IsolateThread thread,
            final DxfgAuthToken token,
            @DxfgOut final WordPointer user
    ) {
        if (user.isNull()) {
            throw new IllegalArgumentException("The `user` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_AuthToken_getUser(thread, token);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        user.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_getPassword",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_getPassword(
            final IsolateThread thread,
            final DxfgAuthToken token,
            @DxfgOut final WordPointer password
    ) {
        if (password.isNull()) {
            throw new IllegalArgumentException("The `password` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_AuthToken_getPassword(thread, token);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        password.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_getScheme",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_getScheme(
            final IsolateThread thread,
            final DxfgAuthToken token,
            @DxfgOut final WordPointer scheme
    ) {
        if (scheme.isNull()) {
            throw new IllegalArgumentException("The `scheme` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_AuthToken_getScheme(thread, token);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        scheme.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthToken_getValue",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthToken_getValue(
            final IsolateThread thread,
            final DxfgAuthToken token,
            @DxfgOut final WordPointer value
    ) {
        if (value.isNull()) {
            throw new IllegalArgumentException("The `value` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = JavacNative.dxfg_AuthToken_getValue(thread, token);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        value.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
