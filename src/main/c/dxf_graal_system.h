// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_SYSTEM_H_
#define DXFEED_GRAAL_NATIVE_API_SYSTEM_H_

#ifdef __cplusplus
extern "C" {
#endif

#include "dxf_graal_error_codes.h"
#include "graal_isolate.h"

/** @defgroup System
 *  @{
 */

/**
 * @brief Sets the system property indicated by the specified key.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] key The name of the system property.
 * @param[in] value The value of the system property.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxf_graal_system_set_property(graal_isolatethread_t *thread, const char *key, const char *value);

/**
 * @brief Gets the system property indicated by the specified key.
 * @warning Return value must be free by dxf_graal_utils_free().
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] key The name of the system property.
 * @return The string value of the system property, or null if there is no property with that key or error occur.
 * Don't forget to delete with dxf_graal_utils_free().
 */
const char *dxf_graal_system_get_property(graal_isolatethread_t *thread, const char *key);

/** @} */ // end of System

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_SYSTEM_H_
