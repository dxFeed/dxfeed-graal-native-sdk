// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ondemand;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.endpoint.DxfgEndpoint;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.NativeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link OnDemandServiceNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(OnDemandDirectives.class)
public final class OnDemandServiceV4Native {

    private OnDemandServiceV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_getInstance",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_getInstance(
            final IsolateThread thread,
            @DxfgOut final WordPointer onDemandService
    ) {
        if (onDemandService.isNull()) {
            throw new IllegalArgumentException("The `onDemandService` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgOnDemandService result = OnDemandServiceNative.dxfg_OnDemandService_getInstance(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        onDemandService.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_getInstanceByEndpoint",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_getInstanceByEndpoint(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final WordPointer onDemandService
    ) {
        if (onDemandService.isNull()) {
            throw new IllegalArgumentException("The `onDemandService` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgOnDemandService result = OnDemandServiceNative.dxfg_OnDemandService_getInstance2(thread, dxfgEndpoint);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        onDemandService.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_getEndpoint",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_getEndpoint(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            @DxfgOut final WordPointer endpoint
    ) {
        if (endpoint.isNull()) {
            throw new IllegalArgumentException("The `endpoint` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEndpoint result = OnDemandServiceNative.dxfg_OnDemandService_getEndpoint(thread, dxfgOnDemandService);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        endpoint.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_isReplaySupported",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_isReplaySupported(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            @DxfgOut final CInt32Pointer replaySupported
    ) {
        if (replaySupported.isNull()) {
            throw new IllegalArgumentException("The `replaySupported` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_isReplaySupported(thread, dxfgOnDemandService);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        replaySupported.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_isReplay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_isReplay(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            @DxfgOut final CInt32Pointer replay
    ) {
        if (replay.isNull()) {
            throw new IllegalArgumentException("The `replay` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_isReplay(thread, dxfgOnDemandService);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        replay.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_isClear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_isClear(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            @DxfgOut final CInt32Pointer clear
    ) {
        if (clear.isNull()) {
            throw new IllegalArgumentException("The `clear` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_isClear(thread, dxfgOnDemandService);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        clear.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_getTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_getTime(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            @DxfgOut final CLongPointer time
    ) {
        if (time.isNull()) {
            throw new IllegalArgumentException("The `time` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = OnDemandServiceNative.dxfg_OnDemandService_getTime(thread, dxfgOnDemandService);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        time.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_getSpeed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_getSpeed(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            @DxfgOut final CDoublePointer speed
    ) {
        if (speed.isNull()) {
            throw new IllegalArgumentException("The `speed` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final double result = OnDemandServiceNative.dxfg_OnDemandService_getSpeed(thread, dxfgOnDemandService);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_NEGATIVE_INFINITY)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        speed.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_replay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_replay(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            final long time
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_replay(thread, dxfgOnDemandService, time);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_replayWithSpeed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_replayWithSpeed(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            final long time,
            final double speed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_replay2(
                        thread,
                        dxfgOnDemandService,
                        time,
                        speed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_pause",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_pause(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_pause(thread, dxfgOnDemandService);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_stopAndResume",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_stopAndResume(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_stopAndResume(thread, dxfgOnDemandService);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_stopAndClear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_stopAndClear(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_stopAndClear(thread, dxfgOnDemandService);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_setSpeed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_setSpeed(
            final IsolateThread thread,
            final DxfgOnDemandService dxfgOnDemandService,
            final double speed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OnDemandServiceNative.dxfg_OnDemandService_setSpeed(thread, dxfgOnDemandService, speed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandPropertyChangeListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandPropertyChangeListener_new(
            final IsolateThread ignoredThread,
            final DxfgOnDemandPropertyChangeListenerFunction function,
            final VoidPointer userData,
            @DxfgOut final WordPointer listener
    ) {
        if (function.isNull()) {
            throw new IllegalArgumentException("The `function` pointer is null");
        }

        if (listener.isNull()) {
            throw new IllegalArgumentException("The `listener` pointer is null");
        }

        listener.write(NativeUtils.MAPPER_ON_DEMAND_PROPERTY_CHANGE_LISTENER.toNative(
                new PropertyListener(function, userData)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_addPropertyChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_addPropertyChangeListener(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService,
            final DxfgOnDemandPropertyChangeListener listener
    ) {
        NativeUtils.MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).addPropertyChangeListener(
                toJavaListener(listener));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OnDemandService_removePropertyChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OnDemandService_removePropertyChangeListener(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService,
            final DxfgOnDemandPropertyChangeListener listener
    ) {
        NativeUtils.MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).removePropertyChangeListener(
                toJavaListener(listener));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    private static PropertyChangeListener toJavaListener(final DxfgOnDemandPropertyChangeListener listener) {
        if (listener.isNull()) {
            throw new IllegalArgumentException("The `listener` handle is null");
        }

        return NativeUtils.MAPPER_ON_DEMAND_PROPERTY_CHANGE_LISTENER.toJava(listener);
    }

    /**
     * The listener of {@code dxfg_v4_OnDemandPropertyChangeListener_new}: it passes the name and the two values of a
     * property change to the native function.
     */
    private static final class PropertyListener extends NativeListener implements PropertyChangeListener {
        private final DxfgOnDemandPropertyChangeListenerFunction function;
        private final VoidPointer userData;

        PropertyListener(final DxfgOnDemandPropertyChangeListenerFunction function, final VoidPointer userData) {
            this.function = function;
            this.userData = userData;
        }

        @Override
        public void propertyChange(final PropertyChangeEvent changeEvent) {
            if (!enter()) {
                return;
            }
            try {
                final DxfgOnDemandPropertyValue oldValue = StackValue.get(DxfgOnDemandPropertyValue.class);
                final DxfgOnDemandPropertyValue newValue = StackValue.get(DxfgOnDemandPropertyValue.class);

                fillValue(oldValue, changeEvent.getOldValue());
                fillValue(newValue, changeEvent.getNewValue());

                final CCharPointer propertyName = NativeUtils.MAPPER_STRING.toNative(changeEvent.getPropertyName());
                try {
                    function.invoke(CurrentIsolate.getCurrentThread(), propertyName, oldValue, newValue, userData);
                } finally {
                    NativeUtils.MAPPER_STRING.release(propertyName);
                }
            } finally {
                exit();
            }
        }

        private static void fillValue(final DxfgOnDemandPropertyValue nativeValue, final Object javaValue) {
            final int kind = OnDemandPropertyValueKind.of(javaValue);

            nativeValue.setKind(kind);
            nativeValue.setBooleanValue(kind == OnDemandPropertyValueKind.BOOLEAN && (Boolean) javaValue ? 1 : 0);
            nativeValue.setDoubleValue(kind == OnDemandPropertyValueKind.DOUBLE ? (Double) javaValue : 0.0);
            nativeValue.setTimeValue(kind == OnDemandPropertyValueKind.TIME ? ((Date) javaValue).getTime() : 0L);
        }
    }
}
