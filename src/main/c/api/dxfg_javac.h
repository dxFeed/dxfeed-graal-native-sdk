// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK Java functions and types declarations
 */

#ifndef DXFEED_GRAAL_NATIVE_SDK_JAVAC_H_
#define DXFEED_GRAAL_NATIVE_SDK_JAVAC_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "graal_isolate.h"

/** @defgroup Javac
 *  @{
 */

typedef struct dxfg_java_object_handler {
    void *java_object_handle;
} dxfg_java_object_handler;

typedef struct dxfg_list {
    int32_t size;
    void **elements;
} dxfg_list;

typedef struct dxfg_java_object_handler_list {
    int32_t size;
    dxfg_java_object_handler **elements;
} dxfg_java_object_handler_list;

typedef struct dxfg_string_list {
    int32_t size;
    const char **elements;
} dxfg_string_list;

typedef struct dxfg_executor_t {
    dxfg_java_object_handler handler;
} dxfg_executor_t;

typedef struct dxfg_input_stream_t {
    dxfg_java_object_handler handler;
} dxfg_input_stream_t;

typedef struct dxfg_time_period_t {
    dxfg_java_object_handler handler;
} dxfg_time_period_t;

typedef struct dxfg_time_format_t {
    dxfg_java_object_handler handler;
} dxfg_time_format_t;

typedef struct dxfg_time_zone_t {
    dxfg_java_object_handler handler;
} dxfg_time_zone_t;

typedef struct dxfg_auth_token_t {
    dxfg_java_object_handler handler;
} dxfg_auth_token_t;

/**
 * Specifies a <i>rounding behavior</i> for numerical operations capable of discarding precision. Each rounding mode
 * indicates how the least significant returned digit of a rounded result is to be calculated. If fewer digits are
 * returned than the digits needed to represent the exact numerical result, the discarded digits will be referred to
 * as the <i>discarded fraction</i> regardless the digits' contribution to the value of the number. In other words,
 * considered as a numerical value, the discarded fraction could have an absolute value greater than one.
 *
 * https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/math/RoundingMode.html
 */
typedef enum dxfg_rounding_mode_t {
    /**
     * Rounding mode to round away from zero. Always increments the digit prior to a non-zero discarded fraction.
     * Note that this rounding mode never decreases the magnitude of the calculated value.
     *
     *<p>Example:
     *<table border>
     * <caption><b>Rounding mode UP Examples</b></caption>
     *<tr valign=top><th>Input Number</th>
     *    <th>Input rounded to one digit<br> with `DXFG_ROUNDING_MODE_UP` rounding</th>
     *<tr align=right><td>5.5</td>  <td>6</td>
     *<tr align=right><td>2.5</td>  <td>3</td>
     *<tr align=right><td>1.6</td>  <td>2</td>
     *<tr align=right><td>1.1</td>  <td>2</td>
     *<tr align=right><td>1.0</td>  <td>1</td>
     *<tr align=right><td>-1.0</td> <td>-1</td>
     *<tr align=right><td>-1.1</td> <td>-2</td>
     *<tr align=right><td>-1.6</td> <td>-2</td>
     *<tr align=right><td>-2.5</td> <td>-3</td>
     *<tr align=right><td>-5.5</td> <td>-6</td>
     *</table>
     */
    DXFG_ROUNDING_MODE_UP = 0,

    /**
     * Rounding mode to round towards zero. Never increments the digit prior to a discarded fraction (i.e., truncates).
     * Note that this rounding mode never increases the magnitude of the calculated value.
     *
     *<p>Example:
     *<table border>
     * <caption><b>Rounding mode DOWN Examples</b></caption>
     *<tr valign=top><th>Input Number</th>
     *    <th>Input rounded to one digit<br> with `DXFG_ROUNDING_MODE_DOWN` rounding</th>
     *<tr align=right><td>5.5</td>  <td>5</td>
     *<tr align=right><td>2.5</td>  <td>2</td>
     *<tr align=right><td>1.6</td>  <td>1</td>
     *<tr align=right><td>1.1</td>  <td>1</td>
     *<tr align=right><td>1.0</td>  <td>1</td>
     *<tr align=right><td>-1.0</td> <td>-1</td>
     *<tr align=right><td>-1.1</td> <td>-1</td>
     *<tr align=right><td>-1.6</td> <td>-1</td>
     *<tr align=right><td>-2.5</td> <td>-2</td>
     *<tr align=right><td>-5.5</td> <td>-5</td>
     *</table>
     */
    DXFG_ROUNDING_MODE_DOWN,

    /**
     * Rounding mode to round towards positive infinity. If the result is positive, behaves as for
     * #DXFG_ROUNDING_MODE_UP; if negative, behaves as for #DXFG_ROUNDING_MODE_DOWN. Note that this rounding mode never
     * decreases the calculated value.
     *
     *<p>Example:
     *<table border>
     * <caption><b>Rounding mode CEILING Examples</b></caption>
     *<tr valign=top><th>Input Number</th>
     *    <th>Input rounded to one digit<br> with `DXFG_ROUNDING_MODE_CEILING` rounding</th>
     *<tr align=right><td>5.5</td>  <td>6</td>
     *<tr align=right><td>2.5</td>  <td>3</td>
     *<tr align=right><td>1.6</td>  <td>2</td>
     *<tr align=right><td>1.1</td>  <td>2</td>
     *<tr align=right><td>1.0</td>  <td>1</td>
     *<tr align=right><td>-1.0</td> <td>-1</td>
     *<tr align=right><td>-1.1</td> <td>-1</td>
     *<tr align=right><td>-1.6</td> <td>-1</td>
     *<tr align=right><td>-2.5</td> <td>-2</td>
     *<tr align=right><td>-5.5</td> <td>-5</td>
     *</table>
     */
    DXFG_ROUNDING_MODE_CEILING,

    /**
     * Rounding mode to round towards negative infinity. If the result is positive, behave as for
     * #DXFG_ROUNDING_MODE_DOWN; if negative, behave as for #DXFG_ROUNDING_MODE_UP. Note that this rounding mode never
     * increases the calculated value.
     *
     *<p>Example:
     *<table border>
     * <caption><b>Rounding mode FLOOR Examples</b></caption>
     *<tr valign=top><th>Input Number</th>
     *    <th>Input rounded to one digit<br> with `DXFG_ROUNDING_MODE_FLOOR` rounding</th>
     *<tr align=right><td>5.5</td>  <td>5</td>
     *<tr align=right><td>2.5</td>  <td>2</td>
     *<tr align=right><td>1.6</td>  <td>1</td>
     *<tr align=right><td>1.1</td>  <td>1</td>
     *<tr align=right><td>1.0</td>  <td>1</td>
     *<tr align=right><td>-1.0</td> <td>-1</td>
     *<tr align=right><td>-1.1</td> <td>-2</td>
     *<tr align=right><td>-1.6</td> <td>-2</td>
     *<tr align=right><td>-2.5</td> <td>-3</td>
     *<tr align=right><td>-5.5</td> <td>-6</td>
     *</table>
     */
    DXFG_ROUNDING_MODE_FLOOR,

    /**
     * Rounding mode to round towards "nearest neighbor" unless both neighbors are equidistant, in which case round up.
     * Behaves as for #DXFG_ROUNDING_MODE_UP if the discarded fraction is <= 0.5; otherwise, behaves as for
     * #DXFG_ROUNDING_MODE_DOWN. Note that this is the rounding mode commonly taught at school.
     *
     *<p>Example:
     *<table border>
     * <caption><b>Rounding mode HALF_UP Examples</b></caption>
     *<tr valign=top><th>Input Number</th>
     *    <th>Input rounded to one digit<br> with `DXFG_ROUNDING_MODE_HALF_UP` rounding</th>
     *<tr align=right><td>5.5</td>  <td>6</td>
     *<tr align=right><td>2.5</td>  <td>3</td>
     *<tr align=right><td>1.6</td>  <td>2</td>
     *<tr align=right><td>1.1</td>  <td>1</td>
     *<tr align=right><td>1.0</td>  <td>1</td>
     *<tr align=right><td>-1.0</td> <td>-1</td>
     *<tr align=right><td>-1.1</td> <td>-1</td>
     *<tr align=right><td>-1.6</td> <td>-2</td>
     *<tr align=right><td>-2.5</td> <td>-3</td>
     *<tr align=right><td>-5.5</td> <td>-6</td>
     *</table>
     */
    DXFG_ROUNDING_MODE_HALF_UP,

    /**
     * Rounding mode to round towards "nearest neighbor" unless both neighbors are equidistant, in which case round
     * down. Behaves as for #DXFG_ROUNDING_MODE_UP if the discarded fraction is > 0.5; otherwise, behaves as for
     * #DXFG_ROUNDING_MODE_DOWN.
     *
     *<p>Example:
     *<table border>
     * <caption><b>Rounding mode HALF_DOWN Examples</b></caption>
     *<tr valign=top><th>Input Number</th>
     *    <th>Input rounded to one digit<br> with `DXFG_ROUNDING_MODE_HALF_DOWN` rounding</th>
     *<tr align=right><td>5.5</td>  <td>5</td>
     *<tr align=right><td>2.5</td>  <td>2</td>
     *<tr align=right><td>1.6</td>  <td>2</td>
     *<tr align=right><td>1.1</td>  <td>1</td>
     *<tr align=right><td>1.0</td>  <td>1</td>
     *<tr align=right><td>-1.0</td> <td>-1</td>
     *<tr align=right><td>-1.1</td> <td>-1</td>
     *<tr align=right><td>-1.6</td> <td>-2</td>
     *<tr align=right><td>-2.5</td> <td>-2</td>
     *<tr align=right><td>-5.5</td> <td>-5</td>
     *</table>
     */
    DXFG_ROUNDING_MODE_HALF_DOWN,

    /**
     * Rounding mode to round towards the "nearest neighbor" unless both neighbors are equidistant, in which case, round
     * towards the even neighbor. Behaves as for #DXFG_ROUNDING_MODE_HALF_UP if the digit to the left of the
     * discarded fraction is odd; behaves as for #DXFG_ROUNDING_MODE_HALF_DOWN if it's even. Note that this is the
     * rounding mode that statistically minimizes cumulative error when applied repeatedly over a sequence of
     * calculations. It is sometimes known as "Banker's rounding" and is chiefly used in the USA. This rounding mode is
     * analogous to the rounding policy used for `float` and `double` arithmetic in Java.
     *
     *<p>Example:
     *<table border>
     * <caption><b>Rounding mode HALF_EVEN Examples</b></caption>
     *<tr valign=top><th>Input Number</th>
     *    <th>Input rounded to one digit<br> with `DXFG_ROUNDING_MODE_HALF_EVEN` rounding</th>
     *<tr align=right><td>5.5</td>  <td>6</td>
     *<tr align=right><td>2.5</td>  <td>2</td>
     *<tr align=right><td>1.6</td>  <td>2</td>
     *<tr align=right><td>1.1</td>  <td>1</td>
     *<tr align=right><td>1.0</td>  <td>1</td>
     *<tr align=right><td>-1.0</td> <td>-1</td>
     *<tr align=right><td>-1.1</td> <td>-1</td>
     *<tr align=right><td>-1.6</td> <td>-2</td>
     *<tr align=right><td>-2.5</td> <td>-2</td>
     *<tr align=right><td>-5.5</td> <td>-6</td>
     *</table>
     */
    DXFG_ROUNDING_MODE_HALF_EVEN,

    /**
     * Rounding mode to assert that the requested operation has an exact result, hence no rounding is necessary. If
     * this rounding mode is specified on an operation that yields an inexact result, an Java `ArithmeticException` is
     * thrown.
     *
     *<p>Example:
     *<table border>
     * <caption><b>Rounding mode UNNECESSARY Examples</b></caption>
     *<tr valign=top><th>Input Number</th>
     *    <th>Input rounded to one digit<br> with `DXFG_ROUNDING_MODE_UNNECESSARY` rounding</th>
     *<tr align=right><td>5.5</td>  <td>throw `ArithmeticException`</td>
     *<tr align=right><td>2.5</td>  <td>throw `ArithmeticException`</td>
     *<tr align=right><td>1.6</td>  <td>throw `ArithmeticException`</td>
     *<tr align=right><td>1.1</td>  <td>throw `ArithmeticException`</td>
     *<tr align=right><td>1.0</td>  <td>1</td>
     *<tr align=right><td>-1.0</td> <td>-1</td>
     *<tr align=right><td>-1.1</td> <td>throw `ArithmeticException`</td>
     *<tr align=right><td>-1.6</td> <td>throw `ArithmeticException`</td>
     *<tr align=right><td>-2.5</td> <td>throw `ArithmeticException`</td>
     *<tr align=right><td>-5.5</td> <td>throw `ArithmeticException`</td>
     *</table>
     */
    DXFG_ROUNDING_MODE_UNNECESSARY,
} dxfg_rounding_mode_t;

typedef void (*dxfg_finalize_function)(graal_isolatethread_t *thread, void *user_data);

int32_t dxfg_Object_finalize(graal_isolatethread_t *thread, dxfg_java_object_handler *handler,
                             dxfg_finalize_function finalize, void *user_data);

/**
 * Frees the memory occupied by the с data structure and release the reference to the java object.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] handler The java object handle to free.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_JavaObjectHandler_release(graal_isolatethread_t *thread, dxfg_java_object_handler *handler);

/**
 * Creates another reference to the Java object (also allocates a memory to the new handle's data structure).
 *
 * Sample:
 * ```c
 * dxfg_cfi_value_t *values = {};
 * int32_t size = 0;
 * dxfg_cfi_value_t *clone_of_options_cfi_value = {};
 *
 * // ...
 *
 * dxfg_CFI_decipher(isolate_thread, cfi, &values, &size);
 *
 * for (int32_t i = 0; i < size; i++) {
 * // ...
 *
 *     dxfg_cfi_value_t *cfi_value = (dxfg_cfi_value_t *)(&values[i].handler);
 * // ...
 *     dxfg_JavaObjectHandler_clone(
 *         isolate_thread,
 *         (dxfg_java_object_handler *)cfi_value,
 *         (dxfg_java_object_handler **)(&clone_of_options_cfi_value));
 * }
 * ```
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] javaObjectHandler A source of the object handle.
 * @param[out] javaObjectHandlerClone A pointer to a clone of the object handle.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object clone handle.
 */
int32_t dxfg_JavaObjectHandler_clone(graal_isolatethread_t *thread, dxfg_java_object_handler *javaObjectHandler,
                                     dxfg_java_object_handler **javaObjectHandlerClone);

/**
 * Releases an array of handles (or simple structures containing only handle). Also frees up the memory occupied by
 * these handles/structures with handles.
 *
 * Sample:
 * ```c
 * // dxfg_cfi_value_t *values = {};
 * // int32_t size = {};
 * dxfg_JavaObjectHandler_array_release(isolate_thread, (const dxfg_java_object_handler **)values, size);
 * ```
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] handlersArray The array of handles.
 * @param[in] size A size of the array.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_JavaObjectHandler_array_release(graal_isolatethread_t *thread,
                                             const dxfg_java_object_handler **handlersArray, int32_t size);

///
const char *dxfg_Object_toString(graal_isolatethread_t *thread, dxfg_java_object_handler *object);

///
int32_t dxfg_Object_equals(graal_isolatethread_t *thread, dxfg_java_object_handler *object,
                           dxfg_java_object_handler *other);

///
int32_t dxfg_Object_hashCode(graal_isolatethread_t *thread, dxfg_java_object_handler *object);

///
int32_t dxfg_Comparable_compareTo(graal_isolatethread_t *thread, dxfg_java_object_handler *object,
                                  dxfg_java_object_handler *other);

// free the memory occupied by the с data structure (list and all elements) and release the reference to the java object
// for all elements
int32_t dxfg_CList_JavaObjectHandler_release(graal_isolatethread_t *thread, dxfg_java_object_handler_list *list);

int32_t dxfg_String_release(graal_isolatethread_t *thread, const char *string);
int32_t dxfg_CList_String_release(graal_isolatethread_t *thread, dxfg_string_list *string);

// read the "Threads and locks" sections at https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedSubscription.html
dxfg_executor_t *dxfg_Executors_newFixedThreadPool(graal_isolatethread_t *thread, int nThreads,
                                                   const char *nameThreads);
dxfg_executor_t *dxfg_Executors_newScheduledThreadPool(graal_isolatethread_t *thread, int nThreads,
                                                       const char *nameThreads);
dxfg_executor_t *dxfg_ExecutorBaseOnConcurrentLinkedQueue_new(graal_isolatethread_t *thread);
int32_t dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(graal_isolatethread_t *thread,
                                                                        dxfg_executor_t *executor);

dxfg_input_stream_t *dxfg_ByteArrayInputStream_new(graal_isolatethread_t *thread, const char *bytes, int32_t size);

int32_t dxfg_gc(graal_isolatethread_t *thread);                                // only for testing
dxfg_java_object_handler *dxfg_throw_exception(graal_isolatethread_t *thread); // only for testing

dxfg_time_zone_t *dxfg_TimeZone_getTimeZone(graal_isolatethread_t *thread, const char *ID);
dxfg_time_zone_t *dxfg_TimeZone_getDefault(graal_isolatethread_t *thread);
const char *dxfg_TimeZone_getID(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone);
const char *dxfg_TimeZone_getDisplayName(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone);
const char *dxfg_TimeZone_getDisplayName2(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone, int32_t daylight,
                                          int32_t style);
int32_t dxfg_TimeZone_getDSTSavings(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone);
int32_t dxfg_TimeZone_useDaylightTime(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone);
int32_t dxfg_TimeZone_observesDaylightTime(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone);
int32_t dxfg_TimeZone_getOffset(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone, int64_t date);
int32_t dxfg_TimeZone_getOffset2(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone, int32_t era, int32_t year,
                                 int32_t month, int32_t day, int32_t dayOfWeek, int32_t milliseconds);
int32_t dxfg_TimeZone_getRawOffset(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone);
int32_t dxfg_TimeZone_hasSameRules(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone, dxfg_time_zone_t *other);
int32_t dxfg_TimeZone_inDaylightTime(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone, int64_t date);
int32_t dxfg_TimeZone_setID(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone, const char *ID);
int32_t dxfg_TimeZone_setRawOffset(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone, int32_t offsetMillis);
dxfg_time_period_t *dxfg_TimePeriod_ZERO(graal_isolatethread_t *thread);
dxfg_time_period_t *dxfg_TimePeriod_UNLIMITED(graal_isolatethread_t *thread);
dxfg_time_period_t *dxfg_TimePeriod_valueOf(graal_isolatethread_t *thread, int64_t value);
dxfg_time_period_t *dxfg_TimePeriod_valueOf2(graal_isolatethread_t *thread, const char *value);
int64_t dxfg_TimePeriod_getTime(graal_isolatethread_t *thread, dxfg_time_period_t *timePeriod);
int32_t dxfg_TimePeriod_getSeconds(graal_isolatethread_t *thread, dxfg_time_period_t *timePeriod);
int64_t dxfg_TimePeriod_getNanos(graal_isolatethread_t *thread, dxfg_time_period_t *timePeriod);
dxfg_time_format_t *dxfg_TimeFormat_DEFAULT(graal_isolatethread_t *thread);
dxfg_time_format_t *dxfg_TimeFormat_GMT(graal_isolatethread_t *thread);
dxfg_time_format_t *dxfg_TimeFormat_getInstance(graal_isolatethread_t *thread, dxfg_time_zone_t *timeZone);
dxfg_time_format_t *dxfg_TimeFormat_withTimeZone(graal_isolatethread_t *thread, dxfg_time_format_t *timeFormat);
dxfg_time_format_t *dxfg_TimeFormat_withMillis(graal_isolatethread_t *thread, dxfg_time_format_t *timeFormat);
dxfg_time_format_t *dxfg_TimeFormat_asFullIso(graal_isolatethread_t *thread, dxfg_time_format_t *timeFormat);
int64_t dxfg_TimeFormat_parse(graal_isolatethread_t *thread, dxfg_time_format_t *timeFormat, const char *value);
const char *dxfg_TimeFormat_format(graal_isolatethread_t *thread, dxfg_time_format_t *timeFormat, int64_t value);
dxfg_time_zone_t *dxfg_TimeFormat_getTimeZone(graal_isolatethread_t *thread, dxfg_time_format_t *timeFormat);
dxfg_auth_token_t *dxfg_AuthToken_valueOf(graal_isolatethread_t *thread, const char *string);
dxfg_auth_token_t *dxfg_AuthToken_createBasicToken(graal_isolatethread_t *thread, const char *userPassword);
dxfg_auth_token_t *dxfg_AuthToken_createBasicToken2(graal_isolatethread_t *thread, const char *user,
                                                    const char *password);
dxfg_auth_token_t *dxfg_AuthToken_createBasicTokenOrNull(graal_isolatethread_t *thread, const char *user,
                                                         const char *password);
dxfg_auth_token_t *dxfg_AuthToken_createBearerToken(graal_isolatethread_t *thread, const char *token);
dxfg_auth_token_t *dxfg_AuthToken_createBearerTokenOrNull(graal_isolatethread_t *thread, const char *token);
dxfg_auth_token_t *dxfg_AuthToken_createCustomToken(graal_isolatethread_t *thread, const char *scheme,
                                                    const char *value);
const char *dxfg_AuthToken_getHttpAuthorization(graal_isolatethread_t *thread, dxfg_auth_token_t *authToken);
const char *dxfg_AuthToken_getUser(graal_isolatethread_t *thread, dxfg_auth_token_t *authToken);
const char *dxfg_AuthToken_getPassword(graal_isolatethread_t *thread, dxfg_auth_token_t *authToken);
const char *dxfg_AuthToken_getScheme(graal_isolatethread_t *thread, dxfg_auth_token_t *authToken);
const char *dxfg_AuthToken_getValue(graal_isolatethread_t *thread, dxfg_auth_token_t *authToken);

/** @} */ // end of Javac

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_SDK_JAVAC_H_
