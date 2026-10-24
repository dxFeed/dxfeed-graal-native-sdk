// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#ifndef DXFG_V4_API_TEST_V4_C_API_H
#define DXFG_V4_API_TEST_V4_C_API_H

#include <dxfg_api.h>

#ifdef __cplusplus
extern "C" {
#endif

// Implemented in c_api.c, a C11 translation unit: writes the seconds of the time period that `value` describes.
int32_t v4CTimePeriodSeconds(graal_isolatethread_t *thread, const char *value, int32_t *seconds);

#ifdef __cplusplus
}
#endif

#endif // DXFG_V4_API_TEST_V4_C_API_H
