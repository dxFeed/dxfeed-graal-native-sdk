// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import com.dxfeed.sdk.mappers.ListMapper;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link InstrumentProfileCollectorNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(IpfDirectives.class)
public final class InstrumentProfileCollectorV4Native {

    private InstrumentProfileCollectorV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_instrument_profile_list_wrapper_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_instrument_profile_list_wrapper_release(
            final IsolateThread thread,
            final DxfgInstrumentProfileList dxfgInstrumentProfileList
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_CList_InstrumentProfile_wrapper_release(
                        thread,
                        dxfgInstrumentProfileList);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_instrument_profile_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_instrument_profile_list_release(
            final IsolateThread thread,
            final DxfgInstrumentProfileList dxfgInstrumentProfileList
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_CList_InstrumentProfile_release(
                        thread,
                        dxfgInstrumentProfileList);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_new(
            final IsolateThread thread,
            @DxfgOut final WordPointer ipfCollector
    ) {
        if (ipfCollector.isNull()) {
            throw new IllegalArgumentException("The `ipfCollector` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileCollector result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_new(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        ipfCollector.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_getLastUpdateTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_getLastUpdateTime(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            @DxfgOut final CLongPointer lastUpdateTime
    ) {
        if (lastUpdateTime.isNull()) {
            throw new IllegalArgumentException("The `lastUpdateTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_getLastUpdateTime(
                        thread,
                        collector);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        lastUpdateTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_updateInstrumentProfileHandle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_updateInstrumentProfileHandle(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            final DxfgInstrumentProfile dxfgInstrumentProfile
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_updateInstrumentProfile(
                        thread,
                        collector,
                        dxfgInstrumentProfile);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_updateInstrumentProfileHandles",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_updateInstrumentProfileHandles(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            final DxfgInstrumentProfileList dxfgInstrumentProfiles,
            final JavaObjectHandler<Object> generation
    ) {
        // The collector applies the profiles one by one: a NULL element found by it leaves the earlier ones applied.
        ListMapper.requireNoNullElements(dxfgInstrumentProfiles, "dxfgInstrumentProfiles");

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_updateInstrumentProfiles(
                        thread,
                        collector,
                        dxfgInstrumentProfiles,
                        generation);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_removeGenerations",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_removeGenerations(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            final DxfgJavaObjectHandlerList generations
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_removeGenerations(
                        thread,
                        collector,
                        generations);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_view",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_view(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            @DxfgOut final WordPointer iterableIp
    ) {
        if (iterableIp.isNull()) {
            throw new IllegalArgumentException("The `iterableIp` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIterableInstrumentProfile result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_view(thread, collector);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        iterableIp.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_getExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_getExecutor(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgExecutorHandle result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_getExecutor(
                        thread,
                        collector);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        executor.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_setExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_setExecutor(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_setExecutor(
                        thread,
                        collector,
                        executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_addUpdateListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_addUpdateListener(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            final DxfgInstrumentProfileUpdateListener dxfgListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_addUpdateListener(
                        thread,
                        collector,
                        dxfgListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_removeUpdateListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_removeUpdateListener(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            final DxfgInstrumentProfileUpdateListener dxfgInstrumentProfileUpdateListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_removeUpdateListener(
                        thread,
                        collector,
                        dxfgInstrumentProfileUpdateListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Iterable_InstrumentProfile_hasNext",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Iterable_InstrumentProfile_hasNext(
            final IsolateThread thread,
            final DxfgIterableInstrumentProfile dxfgIterableInstrumentProfile,
            @DxfgOut final CInt32Pointer hasNext
    ) {
        if (hasNext.isNull()) {
            throw new IllegalArgumentException("The `hasNext` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_Iterable_InstrumentProfile_hasNext(
                        thread,
                        dxfgIterableInstrumentProfile);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        hasNext.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Iterable_InstrumentProfile_nextHandle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Iterable_InstrumentProfile_nextHandle(
            final IsolateThread thread,
            final DxfgIterableInstrumentProfile dxfgIterableInstrumentProfile,
            @DxfgOut final WordPointer instrumentProfile
    ) {
        if (instrumentProfile.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfile` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfile result = InstrumentProfileCollectorNative.dxfg_Iterable_InstrumentProfile_next(
                        thread,
                        dxfgIterableInstrumentProfile);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfile.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileUpdateListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileUpdateListener_new(
            final IsolateThread thread,
            final DxfgInstrumentProfileUpdateListenerFunction dxfgFunction,
            final VoidPointer userData,
            @DxfgOut final WordPointer ipfUpdateListener
    ) {
        if (ipfUpdateListener.isNull()) {
            throw new IllegalArgumentException("The `ipfUpdateListener` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileUpdateListener result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileUpdateListener_new(
                        thread,
                        dxfgFunction,
                        userData);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        ipfUpdateListener.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_updateInstrumentProfile",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_updateInstrumentProfile(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            final DxfgInstrumentProfile2Pointer instrumentProfile
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_updateInstrumentProfile2(
                        thread,
                        collector,
                        instrumentProfile);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileCollector_updateInstrumentProfiles",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileCollector_updateInstrumentProfiles(
            final IsolateThread thread,
            final DxfgInstrumentProfileCollector collector,
            @CConst final DxfgInstrumentProfile2ListPointer instrumentProfiles,
            final JavaObjectHandler<Object> generation
    ) {
        // The collector applies the profiles one by one: a NULL element found by it leaves the earlier ones applied.
        ListMapper.requireNoNullElements(instrumentProfiles, "instrumentProfiles");

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_InstrumentProfileCollector_updateInstrumentProfiles3(
                        thread,
                        collector,
                        instrumentProfiles,
                        generation);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Iterable_InstrumentProfile_next",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Iterable_InstrumentProfile_next(
            final IsolateThread thread,
            final DxfgIterableInstrumentProfile dxfgIterableInstrumentProfile,
            @DxfgOut final DxfgInstrumentProfile2PointerPointer instrumentProfile
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileCollectorNative.dxfg_Iterable_InstrumentProfile_next2(
                        thread,
                        dxfgIterableInstrumentProfile,
                        instrumentProfile);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
