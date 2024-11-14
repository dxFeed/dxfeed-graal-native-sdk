// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK OnDemand functions and types declarations
 */

#ifndef DXFEED_GRAAL_NATIVE_SDK_ONDEMAND_H_
#define DXFEED_GRAAL_NATIVE_SDK_ONDEMAND_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "graal_isolate.h"
#include "dxfg_endpoint.h"
#include "dxfg_javac.h"

/** @defgroup OnDemand
 *  @{
 */

/**
 * @brief Forward declarations.
 */
typedef struct dxfg_java_object_handler dxfg_java_object_handler;

/**
 * @brief The onDemandService.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ondemand/onDemandService.html">Javadoc</a>
 */
typedef struct dxfg_on_demand_service_t {
    dxfg_java_object_handler handler;
} dxfg_on_demand_service_t;

dxfg_on_demand_service_t*         dxfg_OnDemandService_getInstance(graal_isolatethread_t *thread);
dxfg_on_demand_service_t*         dxfg_OnDemandService_getInstance2(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
dxfg_endpoint_t*                  dxfg_OnDemandService_getEndpoint(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
int32_t                           dxfg_OnDemandService_isReplaySupported(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
int32_t                           dxfg_OnDemandService_isReplay(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
int32_t                           dxfg_OnDemandService_isClear(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
int64_t                           dxfg_OnDemandService_getTime(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
double                            dxfg_OnDemandService_getSpeed(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
int32_t                           dxfg_OnDemandService_replay(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service, int64_t time);
int32_t                           dxfg_OnDemandService_replay2(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service, int64_t time, double speed);
int32_t                           dxfg_OnDemandService_pause(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
int32_t                           dxfg_OnDemandService_stopAndResume(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
int32_t                           dxfg_OnDemandService_stopAndClear(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service);
int32_t                           dxfg_OnDemandService_setSpeed(graal_isolatethread_t *thread, dxfg_on_demand_service_t *service, double speed);

/** @} */ // end of OnDemand

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_SDK_ONDEMAND_H_