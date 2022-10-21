// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_ERROR_CODES_H_
#define DXFEED_GRAAL_NATIVE_API_ERROR_CODES_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

/**
 * @brief List of error codes.
 */
typedef enum dxfg_error_code_t {
    /**
     * @brief OK.
     */
    DXFG_EC_SUCCESS = 0,
    /**
     * @brief Shouldn't happen.
     */
    DXFG_EC_UNKNOWN_ERR,
    /**
     * @brief Represents NullPointerException.
     */
    DXFG_EC_NULL_POINTER_EX,
    /**
     * @brief Represents IllegalArgumentException.
     */
    DXFG_EC_ILLEGAL_ARGUMENT_EX,
    /**
     * @brief Represents SecurityException.
     */
    DXFG_EC_SECURITY_EX,
    /**
     * @brief Represents IllegalStateException.
     */
    DXFG_EC_ILLEGAL_STATE_EX,
    /**
     * @brief Represents InterruptedException.
     */
    DXFG_EC_INTERRUPTED_EX,
    /**
     * @brief An unknown descriptor was passed.
     */
    DXFG_EC_UNKNOWN_DESCRIPTOR,
} dxfg_error_code_t;

/**
 * @brief Represents the error code.
 */
typedef dxfg_error_code_t ERROR_CODE;

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_ERROR_CODES_H_
