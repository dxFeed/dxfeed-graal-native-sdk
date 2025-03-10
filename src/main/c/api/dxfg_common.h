// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK Common declarations
 */

#ifndef DXFG_CONFIG_H
#define DXFG_CONFIG_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "graal_isolate.h"

/** @defgroup Common
 *  @{
 */

#ifndef DXFG_OUT
#    ifndef DOXYGEN_SHOULD_SKIP_THIS
#        define DXFG_OUT
#    endif // DOXYGEN_SHOULD_SKIP_THIS
#endif     /* DXFG_OUT */

/**
 * A successful result that any dxFeed Graal Native SDK function can return.
 */
#define DXFG_EXECUTE_SUCCESSFULLY int32_t(0)

/**
 * A result that signals an error that any dxFeed Graal Native SDK function can return. For information about the thrown
 * exception, see the dxfg_get_and_clear_thread_exception_t() function.
 */
#define DXFG_EXECUTE_FAIL int32_t(-1)

/// Single map entry. A pair of string and double.
typedef struct dxfg_string_to_double_map_entry_t {
    const char *key;
    double value;
} dxfg_string_to_double_map_entry_t;

/**
 * Frees memory occupied by string to double map entries.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] mapEntries The array of map entries ([string, double] pair).
 * @param[in] size A size of the array.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_free_string_to_double_map_entries(graal_isolatethread_t *thread,
                                               dxfg_string_to_double_map_entry_t *mapEntries, int32_t size);

/**
 * Frees unmanaged (GraalVM) memory by pointer.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] pointer The pointer to unmanaged memory.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_free(graal_isolatethread_t *thread, void *pointer);

/**
 * Frees array of C-strings.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] strings The array of strings.
 * @param[in] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_free_strings(graal_isolatethread_t *thread, char** strings, int32_t size);

/** @} */ // end of Common

#ifdef __cplusplus
}
#endif

#endif // DXFG_CONFIG_H
