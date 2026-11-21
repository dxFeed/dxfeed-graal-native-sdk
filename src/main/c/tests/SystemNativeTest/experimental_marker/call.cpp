// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// Compiled only by the test SystemNativeTest_ExperimentalMarker, which expects the compiler to reject the call: the
// marker DXFG_EXPERIMENTAL, defined as a deprecation attribute, lands on the experimental function.
#define DXFG_EXPERIMENTAL [[deprecated("experimental")]]

#include <dxfg_api.h>

int32_t callExperimentalFunction(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint) {
    dxfg_v4_connection_error_t *error = nullptr;

    return dxfg_v4_DXEndpoint_getLastConnectionError(thread, endpoint, &error);
}
