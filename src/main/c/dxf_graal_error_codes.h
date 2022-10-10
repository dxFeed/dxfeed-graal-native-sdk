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
    /// OK.
    DX_EC_SUCCESS = 0,
    /// Shouldn't happen.
    DX_EC_UNKNOWN_ERR,
    /// Represents NPE exception.
    DX_EC_NULL_POINTER_EX,
    /// Represents illegal argument exception.
    DX_EC_ILLEGAL_ARGUMENT_EX,
    /// Represents security exception.
    DX_EC_SECURITY_EX,
    /// Represents IllegalStateException.
    DX_EC_ILLEGAL_STATE_EX,
    /// An unknown descriptor was passed.
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
