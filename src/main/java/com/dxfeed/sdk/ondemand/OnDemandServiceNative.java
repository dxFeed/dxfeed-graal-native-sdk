// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ondemand;

import static com.dxfeed.sdk.NativeUtils.MAPPER_ENDPOINT;
import static com.dxfeed.sdk.NativeUtils.MAPPER_ON_DEMAND_SERVICE;
import static com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.ondemand.OnDemandService;
import com.dxfeed.sdk.endpoint.DxfgEndpoint;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOneLong;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNegativeInfinityDouble;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import java.util.Date;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(Directives.class)
public class OnDemandServiceNative {

    @CEntryPoint(
            name = "dxfg_OnDemandService_getInstance",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgOnDemandService dxfg_OnDemandService_getInstance(
            final IsolateThread ignoredThread
    ) {
        return MAPPER_ON_DEMAND_SERVICE.toNative(OnDemandService.getInstance());
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_getInstance2",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgOnDemandService dxfg_OnDemandService_getInstance2(
            final IsolateThread ignoredThread,
            final DxfgEndpoint dxfgEndpoint
    ) {
        return MAPPER_ON_DEMAND_SERVICE.toNative(
                OnDemandService.getInstance(
                        MAPPER_ENDPOINT.toJava(dxfgEndpoint)
                )
        );
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_getEndpoint",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgEndpoint dxfg_OnDemandService_getEndpoint(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        return MAPPER_ENDPOINT.toNative(
                MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).getEndpoint()
        );
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_isReplaySupported",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_isReplaySupported(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        return MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).isReplaySupported() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_isReplay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_isReplay(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        return MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).isReplay() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_isClear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_isClear(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        return MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).isClear() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_getTime",
            exceptionHandler = ExceptionHandlerReturnMinusOneLong.class
    )
    public static long dxfg_OnDemandService_getTime(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        return MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).getTime().getTime();
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_getSpeed",
            exceptionHandler = ExceptionHandlerReturnNegativeInfinityDouble.class
    )
    public static double dxfg_OnDemandService_getSpeed(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        return MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).getSpeed();
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_replay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_replay(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService,
            final long time
    ) {
        MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).replay(new Date(time));
        return EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_replay2",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_replay2(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService,
            final long time,
            final double speed
    ) {
        MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).replay(new Date(time), speed);
        return EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_pause",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_pause(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).pause();
        return EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_stopAndResume",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_stopAndResume(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).stopAndResume();
        return EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_stopAndClear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_stopAndClear(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService
    ) {
        MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).stopAndClear();
        return EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_OnDemandService_setSpeed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_OnDemandService_setSpeed(
            final IsolateThread ignoredThread,
            final DxfgOnDemandService dxfgOnDemandService,
            final double speed
    ) {
        MAPPER_ON_DEMAND_SERVICE.toJava(dxfgOnDemandService).setSpeed(speed);
        return EXECUTE_SUCCESSFULLY;
    }
}
