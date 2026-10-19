// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK Common declarations
 */

#ifndef DXFG_COMMON_H
#define DXFG_COMMON_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "graal_isolate.h"

/** @defgroup Common Common
 *  @brief The macros and the functions that every header of the C API uses: the marker of an output parameter, the
 *  deprecation marker, the status results and the functions that free memory allocated by the SDK.
 *
 *  The ownership rules of the handles, the strings and the lists are described in the group @ref Javac.
 *  @{
 */

/**
 * Marks an output parameter of a function: a pointer to a variable that the function writes the result to, e.g.
 * `DXFG_OUT dxfg_endpoint_t **endpoint`. The caller passes the address of its own variable, and the variable is
 * written only if the function succeeds (returns #DXFG_EXECUTE_SUCCESSFULLY). The macro expands to nothing: it only
 * documents the direction of the parameter in the declaration (the `[out]` of the documentation).
 */
#ifndef DXFG_OUT
#    ifndef DOXYGEN_SHOULD_SKIP_THIS
#        define DXFG_OUT
#    endif // DOXYGEN_SHOULD_SKIP_THIS
#endif     /* DXFG_OUT */

/**
 * Marks a deprecated function: the compilers warn where it is used. The function still works, see its description for
 * the replacement.
 */
#ifndef DXFG_DEPRECATED
#    if defined(__cplusplus) && __cplusplus >= 201402L
#        define DXFG_DEPRECATED(message) [[deprecated(message)]]
#    elif defined(__GNUC__) || defined(__clang__)
#        define DXFG_DEPRECATED(message) __attribute__((deprecated(message)))
#    elif defined(_MSC_VER)
#        define DXFG_DEPRECATED(message) __declspec(deprecated(message))
#    else
#        define DXFG_DEPRECATED(message)
#    endif
#endif /* DXFG_DEPRECATED */

/**
 * A successful result that any dxFeed Graal Native SDK function can return: the status of the functions that return
 * `int32_t` status, #DXFG_EXECUTE_SUCCESSFULLY (0) means that the function was executed without an exception.
 */
#define DXFG_EXECUTE_SUCCESSFULLY ((int32_t)0)

/**
 * A result that signals an error that any dxFeed Graal Native SDK function can return: a function that returns
 * `int32_t` status returns #DXFG_EXECUTE_FAIL (-1) when it was terminated by an exception. For information about the
 * thrown exception, see the dxfg_get_and_clear_thread_exception_t() function. Other functions signal an error with
 * another value, which is given in the `@return` of the function.
 */
#define DXFG_EXECUTE_FAIL ((int32_t)-1)

/**
 * @brief A map entry that consists of a string and a double: an element of an array that represents a map from
 * strings to doubles.
 *
 * An array that a function returns (e.g. dxfg_AdditionalUnderlyings_getMap()) is allocated by the SDK together with
 * the strings of the keys: release it with dxfg_free_string_to_double_map_entries(). An array that is passed to a
 * function (e.g. dxfg_AdditionalUnderlyings_valueOf2()) is only read by the SDK: the caller keeps its ownership.
 */
typedef struct dxfg_string_to_double_map_entry_t {
    /** The key: a NUL-terminated UTF-8 string. It belongs to the entry and is freed with the array. */
    const char *key;
    /** The value that corresponds to the key. */
    double value;
} dxfg_string_to_double_map_entry_t;

/**
 * @brief Frees an array of string to double map entries that a function of the SDK returned.
 *
 * It frees the key of every entry and the array itself. Do not use the array and the keys after that. If the array
 * is NULL or `size` is not positive, the function does nothing (an empty array is not freed) and returns
 * #DXFG_EXECUTE_SUCCESSFULLY.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] mapEntries The array of map entries ([string, double] pair) that was returned by the SDK.
 * @param[in] size The number of entries in the array, as returned together with the array.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_free_string_to_double_map_entries(graal_isolatethread_t *thread,
                                               dxfg_string_to_double_map_entry_t *mapEntries, int32_t size);

/**
 * @brief Frees a block of the unmanaged (GraalVM) memory that a function of the SDK allocated, e.g. the array of
 * doubles that dxfg_PriceIncrements_getPriceIncrements() returns.
 *
 * Only a block of memory that a function of the SDK allocated and documents to be released with this function can be
 * passed. Do not use the memory after that.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] pointer The pointer to the unmanaged memory that was returned by the SDK.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_free(graal_isolatethread_t *thread, void *pointer);

/**
 * @brief Frees an array of C strings that a function of the SDK returned.
 *
 * It frees every string and the array itself. Do not use the array and the strings after that. If the array is NULL
 * or `size` is not positive, the function does nothing (an empty array is not freed) and returns
 * #DXFG_EXECUTE_SUCCESSFULLY. A list of strings (dxfg_string_list) is not an array of this kind: it is released with
 * dxfg_CList_String_release().
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] strings The array of the NUL-terminated strings that was returned by the SDK.
 * @param[in] size The number of strings in the array, as returned together with the array.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_free_strings(graal_isolatethread_t *thread, char** strings, int32_t size);

/** @} */ // end of Common

#ifdef __cplusplus
}
#endif

#endif // DXFG_COMMON_H
