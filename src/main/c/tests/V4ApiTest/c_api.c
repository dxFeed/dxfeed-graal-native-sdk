// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// A C11 translation unit: the whole C API, the v4 headers included, compiles as C.

#include "v4_c_api.h"

#include <stddef.h>

int32_t v4CTimePeriodSeconds(graal_isolatethread_t *thread, const char *value, int32_t *seconds) {
    dxfg_v4_time_period_t *period = NULL;
    int32_t result = dxfg_v4_TimePeriod_valueOfString(thread, value, &period);

    if (result != DXFG_EXECUTE_SUCCESSFULLY) {
        return result;
    }

    result = dxfg_v4_TimePeriod_getSeconds(thread, period, seconds);

    const int32_t releaseResult = dxfg_v4_JavaObjectHandler_release(thread, &period->handler);

    return result != DXFG_EXECUTE_SUCCESSFULLY ? result : releaseResult;
}
