// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief The v4 functions of dxfg_ondemand.h and the v4 names of its types (see the group @ref V4_OnDemand).
 */

#ifndef DXFG_V4_ONDEMAND_H
#define DXFG_V4_ONDEMAND_H

#include "dxfg_ondemand.h"
#include "dxfg_v4_common.h"
#include "dxfg_v4_endpoint.h"
#include "dxfg_v4_javac.h"

#ifdef __cplusplus
extern "C" {
#endif

/** @defgroup V4_OnDemand OnDemand (v4)
 *  @brief The v4 functions and type names of the group @ref OnDemand. Every function returns
 *  #DXFG_EXECUTE_SUCCESSFULLY or #DXFG_EXECUTE_FAIL and writes its result to a #DXFG_OUT parameter,
 *  see @ref V4_Conventions.
 *  @{
 */

/** @copydoc dxfg_java_object_handler */
typedef dxfg_java_object_handler dxfg_v4_java_object_handler;

/** @copydoc dxfg_on_demand_service_t */
typedef dxfg_on_demand_service_t dxfg_v4_on_demand_service_t;

/**
 * @brief Returns the on-demand service of the default endpoint with the #DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED role.
 *
 * The endpoint is the application-wide default endpoint of that role (dxfg_v4_OnDemandService_getEndpoint() returns it;
 * dxfg_v4_DXEndpoint_getInstanceWithRole() creates a new endpoint instead), which is not connected to any other
 * real-time or delayed data feed. It is created on the first call, is configured by the system properties and is closed
 * by dxfg_v4_system_close_all_and_await_termination(). To switch another feed to the replay and back, use
 * dxfg_v4_OnDemandService_getInstanceByEndpoint() with the endpoint of that feed.
 *
 * The call fails if the on-demand implementation is not available in the library.
 *
 * Java: `OnDemandService.getInstance()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[out] onDemandService The pointer to the variable that receives the service (release the handle with
 * dxfg_v4_JavaObjectHandler_release()); must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_getInstance(graal_isolatethread_t *thread,
                                            DXFG_OUT dxfg_v4_on_demand_service_t **onDemandService);

/**
 * @brief Returns the on-demand service of the specified endpoint.
 *
 * Each endpoint is associated with one service, so the calls for the same endpoint return handles of the same
 * service. The endpoint must have the role #DXFG_ENDPOINT_ROLE_FEED or #DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED, otherwise
 * the call fails. It also fails if the on-demand implementation is not available in the library.
 *
 * Java: `OnDemandService.getInstance(DXEndpoint)`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint; the caller keeps its ownership.
 * @param[out] onDemandService The pointer to the variable that receives the service (release the handle with
 * dxfg_v4_JavaObjectHandler_release()); must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_getInstanceByEndpoint(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint,
                                                      DXFG_OUT dxfg_v4_on_demand_service_t **onDemandService);

/**
 * @brief Returns the endpoint that is associated with the service.
 *
 * Java: `OnDemandService.getEndpoint()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[out] endpoint The pointer to the variable that receives the endpoint (release the handle with
 * dxfg_v4_JavaObjectHandler_release(); this does not close the endpoint); must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_getEndpoint(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                            DXFG_OUT dxfg_v4_endpoint_t **endpoint);

/**
 * @brief Tells whether the on-demand historical data replay mode is supported.
 *
 * It is supported when the endpoint is connected to an address with the "(ondemand:<address>)" component that
 * specifies an on-demand historical data provider. While it is not supported, the replay functions fail.
 *
 * Java: `OnDemandService.isReplaySupported()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[out] replaySupported The pointer to the variable that receives 1 if the replay mode is supported, 0 if not;
 * must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_isReplaySupported(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                                  DXFG_OUT int32_t *replaySupported);

/**
 * @brief Tells whether the service is in the replay mode.
 *
 * The replay mode is in effect after dxfg_v4_OnDemandService_replay() or dxfg_v4_OnDemandService_replayWithSpeed() and
 * before dxfg_v4_OnDemandService_stopAndResume() or dxfg_v4_OnDemandService_stopAndClear().
 *
 * Java: `OnDemandService.isReplay()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[out] replay The pointer to the variable that receives 1 if the service is in the replay mode, 0 if not; must
 * not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_isReplay(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                         DXFG_OUT int32_t *replay);

/**
 * @brief Tells whether the service is in the clear mode.
 *
 * The clear mode is in effect after dxfg_v4_OnDemandService_stopAndClear() and before
 * dxfg_v4_OnDemandService_stopAndResume(), dxfg_v4_OnDemandService_replay() or
 * dxfg_v4_OnDemandService_replayWithSpeed().
 *
 * Java: `OnDemandService.isClear()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[out] clear The pointer to the variable that receives 1 if the service is in the clear mode, 0 if not; must not
 * be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_isClear(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                        DXFG_OUT int32_t *clear);

/**
 * @brief Returns the current or the last replay time.
 *
 * In the replay mode it is the time that is being replayed, otherwise the last time that was replayed. If the replay
 * was never started, it is 0 (the Unix epoch).
 *
 * Java: `OnDemandService.getTime()`, the result of `Date.getTime()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[out] time The pointer to the variable that receives the time in milliseconds since the Unix epoch (UTC); must
 * not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_getTime(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                        DXFG_OUT int64_t *time);

/**
 * @brief Returns the replay speed.
 *
 * The speed is measured with respect to the real-time playback speed: 1 is the real-time speed. It is 0 while the
 * service is not in the replay mode.
 *
 * Java: `OnDemandService.getSpeed()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[out] speed The pointer to the variable that receives the replay speed; must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_getSpeed(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                         DXFG_OUT double *speed);

/**
 * @brief Turns the replay mode on from the specified time with the real-time speed (1).
 *
 * The call is the same as dxfg_v4_OnDemandService_replayWithSpeed() with the speed 1. It fails if the replay mode is
 * not supported (see dxfg_v4_OnDemandService_isReplaySupported()).
 *
 * Java: `OnDemandService.replay(Date)`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[in] time The time to start the replay from, in milliseconds since the Unix epoch (UTC).
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_replay(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                       int64_t time);

/**
 * @brief Turns the replay mode on from the specified time with the specified speed.
 *
 * The feed is disconnected from the other data providers, pre-buffers the historical tick data and replays it as if it
 * came from the data feeds now. After the call dxfg_v4_OnDemandService_isReplay() returns 1,
 * dxfg_v4_OnDemandService_isClear() returns 0, dxfg_v4_OnDemandService_getTime() returns the time that is being
 * replayed and dxfg_v4_OnDemandService_getSpeed() returns `speed`. The call fails if the replay mode is not supported
 * (see dxfg_v4_OnDemandService_isReplaySupported()) or if the speed is negative.
 *
 * Java: `OnDemandService.replay(Date, double)`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[in] time The time to start the replay from, in milliseconds since the Unix epoch (UTC).
 * @param[in] speed The speed to start the replay with: 1 is the real-time speed, more than 1 is faster, less than 1 is
 * slower and 0 is a pause.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_replayWithSpeed(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                                int64_t time, double speed);

/**
 * @brief Pauses the replay and keeps the data snapshot.
 *
 * It is the same as dxfg_v4_OnDemandService_setSpeed() with the speed 0: the current replay time is captured and the
 * replay stops at it. Outside the replay mode the call has no effect (and succeeds).
 *
 * Java: `OnDemandService.pause()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_pause(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service);

/**
 * @brief Stops the replay and resumes the ordinary data feed.
 *
 * The call has no effect if the service is neither in the replay mode nor in the clear mode. After the call
 * dxfg_v4_OnDemandService_isReplay() and dxfg_v4_OnDemandService_isClear() return 0, dxfg_v4_OnDemandService_getTime()
 * returns the last replayed time and dxfg_v4_OnDemandService_getSpeed() returns 0. An endpoint with the role
 * #DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED has no ordinary feed, so for it the call works like
 * dxfg_v4_OnDemandService_stopAndClear().
 *
 * Java: `OnDemandService.stopAndResume()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_stopAndResume(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service);

/**
 * @brief Stops the incoming data and clears it without resuming the data updates.
 *
 * The call works both in the replay mode and for the ordinary data feed: all the incoming data updates are suspended
 * and the current data is cleared. After the call dxfg_v4_OnDemandService_isReplay() returns 0,
 * dxfg_v4_OnDemandService_isClear() returns 1, dxfg_v4_OnDemandService_getTime() returns the last replayed time and
 * dxfg_v4_OnDemandService_getSpeed() returns 0. The ordinary feed is resumed by
 * dxfg_v4_OnDemandService_stopAndResume(), the replay is continued by dxfg_v4_OnDemandService_replay() or
 * dxfg_v4_OnDemandService_replayWithSpeed().
 *
 * Java: `OnDemandService.stopAndClear()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_stopAndClear(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service);

/**
 * @brief Changes the replay speed and continues the replay at the current time.
 *
 * The speed is measured with respect to the real-time playback speed. The call fails if the speed is negative, or if
 * the service is not in the replay mode and the speed is not 0.
 *
 * Java: `OnDemandService.setSpeed(double)`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The service.
 * @param[in] speed The new replay speed: 1 is the real-time speed, more than 1 is faster, less than 1 is slower and 0
 * is a pause.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_OnDemandService_setSpeed(graal_isolatethread_t *thread, dxfg_v4_on_demand_service_t *service,
                                         double speed);

/** @} */ // end of V4_OnDemand

#ifdef __cplusplus
}
#endif

#endif // DXFG_V4_ONDEMAND_H
