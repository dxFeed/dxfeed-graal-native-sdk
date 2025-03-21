// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK CandleWebService, HistoryEndpoint functions and types declarations
 */

#ifndef DXFG_CANDLEWEBSERVICE_H
#define DXFG_CANDLEWEBSERVICE_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "dxfg_javac.h"
#include "graal_isolate.h"

/** @defgroup CandleWebService
 *  @{
 */

typedef struct dxfg_history_endpoint_t {
    dxfg_java_object_handler handler;
} dxfg_history_endpoint_t;

typedef struct dxfg_history_endpoint_builder_t {
    dxfg_java_object_handler handler;
} dxfg_history_endpoint_builder_t;

typedef enum dxfg_history_endpoint_compression_t {
    DXFG_HISTORY_ENDPOINT_COMPRESSION_NONE = 0,
    DXFG_HISTORY_ENDPOINT_COMPRESSION_ZIP,
    DXFG_HISTORY_ENDPOINT_COMPRESSION_GZIP,
} dxfg_history_endpoint_compression_t;

typedef enum dxfg_history_endpoint_format_t {
    DXFG_HISTORY_ENDPOINT_FORMAT_TEXT = 0,
    DXFG_HISTORY_ENDPOINT_FORMAT_CSV,
    DXFG_HISTORY_ENDPOINT_FORMAT_BINARY,
} dxfg_history_endpoint_format_t;

/** @} */ // end of CandleWebService

#ifdef __cplusplus
}
#endif

#endif // DXFG_CANDLEWEBSERVICE_H
