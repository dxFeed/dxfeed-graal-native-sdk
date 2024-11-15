// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK QDS functions declarations
 */

#ifndef DXFEED_GRAAL_NATIVE_SDK_QDS_H_
#define DXFEED_GRAAL_NATIVE_SDK_QDS_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "graal_isolate.h"
#include "dxfg_javac.h"

/** @defgroup Qds
 *  @{
 */
dxfg_string_list*   dxfg_Tools_parseSymbols(graal_isolatethread_t *thread, const char* symbolList);
int32_t             dxfg_Tools_main(graal_isolatethread_t *thread, dxfg_string_list* args);
/** @} */ // end of QDS

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_SDK_QDS_H_




