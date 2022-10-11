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
typedef enum dxf_graal_error_code_t {
    /**
     * @brief OK.
     */
    DX_EC_SUCCESS = 0,
    /**
     * @brief Shouldn't happen.
     */
    DX_EC_UNKNOWN_ERR,
    /**
     * @brief Represents NullPointerException.
     */
    DX_EC_NULL_POINTER_EX,
    /**
     * @brief Represents IllegalArgumentException.
     */
    DX_EC_ILLEGAL_ARGUMENT_EX,
    /**
     * @brief Represents SecurityException.
     */
    DX_EC_SECURITY_EX,
    /**
     * @brief Represents IllegalStateException.
     */
    DX_EC_ILLEGAL_STATE_EX,
    /**
     * @brief An unknown descriptor was passed.
     */
    DX_EC_UNKNOWN_DESCRIPTOR,
} dxf_graal_error_code_t;

/**
 * @brief Represents the error code.
 */
typedef dxf_graal_error_code_t ERROR_CODE;

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_ERROR_CODES_H_
