// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_UTILS_H_
#define DXFEED_GRAAL_NATIVE_API_UTILS_H_

#ifdef __cplusplus
extern "C" {
#endif

#include "graal_isolate.h"

/** @defgroup Utils
 *  @{
 */

/**
 * @brief Frees pointer that was previously allocated in Java method (by UnmanagedMemory).
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] ptr The pointer to release. Pointer not valid after function call.
 */
void dxf_graal_utils_free(graal_isolatethread_t *thread, void *ptr);

/** @} */ // end of Utils

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_UTILS_H_
