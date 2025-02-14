// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK IPF functions and types declarations
 */

#ifndef DXFEED_GRAAL_NATIVE_SDK_IPF_H_
#define DXFEED_GRAAL_NATIVE_SDK_IPF_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "graal_isolate.h"
#include "dxfg_javac.h"

/** @defgroup InstrumentProfile
 *  @{
 */

/**
 * @brief The InstrumentProfileConnection.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/live/InstrumentProfileConnection.html">Javadoc</a>
 */
typedef struct dxfg_ipf_connection_t {
    dxfg_java_object_handler handler;
} dxfg_ipf_connection_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/InstrumentProfileReader.html">Javadoc</a>
 */
typedef struct dxfg_instrument_profile_reader_t {
    dxfg_java_object_handler handler;
} dxfg_instrument_profile_reader_t;

/**
 * @brief The PropertyChangeListener.
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/beans/PropertyChangeListener.html">Javadoc</a>
 */
typedef struct dxfg_ipf_connection_state_change_listener_t {
    dxfg_java_object_handler handler;
} dxfg_ipf_connection_state_change_listener_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/live/InstrumentProfileConnection.State.html">Javadoc</a>
 */
typedef enum dxfg_ipf_connection_state_t {
    DXFG_IPF_CONNECTION_STATE_NOT_CONNECTED = 0,
    DXFG_IPF_CONNECTION_STATE_CONNECTING,
    DXFG_IPF_CONNECTION_STATE_CONNECTED,
    DXFG_IPF_CONNECTION_STATE_COMPLETED,
    DXFG_IPF_CONNECTION_STATE_CLOSED,
} dxfg_ipf_connection_state_t;

typedef void (*dxfg_ipf_connection_state_change_listener_func)(graal_isolatethread_t *thread, dxfg_ipf_connection_state_t old_state, dxfg_ipf_connection_state_t new_state, void *user_data);

/**
 * @brief The InstrumentProfileCollector.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/live/InstrumentProfileCollector.html">Javadoc</a>
 */
typedef struct dxfg_ipf_collector_t {
    dxfg_java_object_handler handler;
} dxfg_ipf_collector_t;


/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/InstrumentProfile.html">Javadoc</a>
 */
typedef struct dxfg_instrument_profile_t {
    dxfg_java_object_handler handler;
} dxfg_instrument_profile_t;

typedef struct dxfg_instrument_profile_list {
    int32_t size;
    dxfg_instrument_profile_t **elements;
} dxfg_instrument_profile_list;

/**
 * @brief The InstrumentProfileUpdateListener.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/live/InstrumentProfileUpdateListener.html">Javadoc</a>
 */
typedef struct dxfg_ipf_update_listener_t {
    dxfg_java_object_handler handler;
} dxfg_ipf_update_listener_t;

typedef struct dxfg_iterable_ip_t {
    dxfg_java_object_handler handler;
} dxfg_iterable_ip_t;

typedef void (*dxfg_ipf_update_listener_function)(graal_isolatethread_t *thread, dxfg_iterable_ip_t *profiles, void *user_data);

dxfg_ipf_connection_t*                        dxfg_InstrumentProfileConnection_createConnection(graal_isolatethread_t *thread, const char *address, dxfg_ipf_collector_t *collector);
const char*                                   dxfg_InstrumentProfileConnection_getAddress(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection);
int64_t                                       dxfg_InstrumentProfileConnection_getUpdatePeriod(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection);
int32_t                                       dxfg_InstrumentProfileConnection_setUpdatePeriod(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection, int64_t newValue);
dxfg_ipf_connection_state_t                   dxfg_InstrumentProfileConnection_getState(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection);
int64_t                                       dxfg_InstrumentProfileConnection_getLastModified(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection);
int32_t                                       dxfg_InstrumentProfileConnection_start(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection);
int32_t                                       dxfg_InstrumentProfileConnection_close(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection);
dxfg_ipf_connection_state_change_listener_t*  dxfg_IpfPropertyChangeListener_new(graal_isolatethread_t *thread, dxfg_ipf_connection_state_change_listener_func user_func, void *user_data);
int32_t                                       dxfg_InstrumentProfileConnection_addStateChangeListener(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection, dxfg_ipf_connection_state_change_listener_t *listener);
int32_t                                       dxfg_InstrumentProfileConnection_removeStateChangeListener(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection, dxfg_ipf_connection_state_change_listener_t *listener);
int32_t                                       dxfg_InstrumentProfileConnection_waitUntilCompleted(graal_isolatethread_t *thread, dxfg_ipf_connection_t *connection, int64_t timeoutInMs);

int32_t                                       dxfg_CList_InstrumentProfile_wrapper_release(graal_isolatethread_t *thread, dxfg_instrument_profile_list *ips); // release only the struct dxfg_instrument_profile_list and keep the elements
int32_t                                       dxfg_CList_InstrumentProfile_release(graal_isolatethread_t *thread, dxfg_instrument_profile_list *ips);

dxfg_ipf_collector_t*                         dxfg_InstrumentProfileCollector_new(graal_isolatethread_t *thread);
int64_t                                       dxfg_InstrumentProfileCollector_getLastUpdateTime(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector);
int32_t                                       dxfg_InstrumentProfileCollector_updateInstrumentProfile(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfileCollector_updateInstrumentProfiles(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, dxfg_instrument_profile_list *ips, dxfg_java_object_handler *generation);
int32_t                                       dxfg_InstrumentProfileCollector_removeGenerations(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, dxfg_java_object_handler_list *generations);
dxfg_iterable_ip_t*                           dxfg_InstrumentProfileCollector_view(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector);
dxfg_executor_t*                              dxfg_InstrumentProfileCollector_getExecutor(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector);
int32_t                                       dxfg_InstrumentProfileCollector_setExecutor(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, dxfg_executor_t *executor);
int32_t                                       dxfg_InstrumentProfileCollector_addUpdateListener(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, dxfg_ipf_update_listener_t* listener);
int32_t                                       dxfg_InstrumentProfileCollector_removeUpdateListener(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, dxfg_ipf_update_listener_t* listener);

int32_t                                       dxfg_Iterable_InstrumentProfile_hasNext(graal_isolatethread_t *thread, dxfg_iterable_ip_t *iterable_ip);
dxfg_instrument_profile_t*                    dxfg_Iterable_InstrumentProfile_next(graal_isolatethread_t *thread, dxfg_iterable_ip_t *iterable_ip);

dxfg_ipf_update_listener_t*                   dxfg_InstrumentProfileUpdateListener_new(graal_isolatethread_t *thread, dxfg_ipf_update_listener_function user_func, void *user_data);

dxfg_instrument_profile_reader_t*             dxfg_InstrumentProfileReader_new(graal_isolatethread_t *thread);
int64_t                                       dxfg_InstrumentProfileReader_getLastModified(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader);
int32_t                                       dxfg_InstrumentProfileReader_wasComplete(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader);
dxfg_instrument_profile_list*                 dxfg_InstrumentProfileReader_readFromFile(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address);
dxfg_instrument_profile_list*                 dxfg_InstrumentProfileReader_readFromFile2(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, const char *user, const char *password);
dxfg_instrument_profile_list*                 dxfg_InstrumentProfileReader_readFromFile3(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, dxfg_auth_token_t *token);
const char*                                   dxfg_InstrumentProfileReader_resolveSourceURL(graal_isolatethread_t *thread, const char *address);
dxfg_instrument_profile_list*                 dxfg_InstrumentProfileReader_read2(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, const char* address);
dxfg_instrument_profile_list*                 dxfg_InstrumentProfileReader_readCompressed(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is);
dxfg_instrument_profile_list*                 dxfg_InstrumentProfileReader_read(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is);
dxfg_instrument_profile_t*                    dxfg_InstrumentProfile_new(graal_isolatethread_t *thread);
dxfg_instrument_profile_t*                    dxfg_InstrumentProfile_new2(graal_isolatethread_t *thread, dxfg_instrument_profile_t* ip);
const char*                                   dxfg_InstrumentProfile_getType(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setType(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getSymbol(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setSymbol(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getDescription(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setDescription(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getLocalSymbol(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setLocalSymbol(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getLocalDescription(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setLocalDescription(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getCountry(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setCountry(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getOPOL(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setOPOL(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getExchangeData(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setExchangeData(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getExchanges(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setExchanges(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getCurrency(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setCurrency(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getBaseCurrency(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setBaseCurrency(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getCFI(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setCFI(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getISIN(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setISIN(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getSEDOL(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setSEDOL(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getCUSIP(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setCUSIP(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
int32_t                                       dxfg_InstrumentProfile_getICB(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setICB(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, int32_t value);
int32_t                                       dxfg_InstrumentProfile_getSIC(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setSIC(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, int32_t value);
double                                        dxfg_InstrumentProfile_getMultiplier(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setMultiplier(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, double value);
const char*                                   dxfg_InstrumentProfile_getProduct(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setProduct(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getUnderlying(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setUnderlying(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
double                                        dxfg_InstrumentProfile_getSPC(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setSPC(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, double value);
const char*                                   dxfg_InstrumentProfile_getAdditionalUnderlyings(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setAdditionalUnderlyings(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getMMY(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setMMY(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
int32_t                                       dxfg_InstrumentProfile_getExpiration(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setExpiration(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, int32_t value);
int32_t                                       dxfg_InstrumentProfile_getLastTrade(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setLastTrade(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, int32_t value);
double                                        dxfg_InstrumentProfile_getStrike(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setStrike(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, double value);
const char*                                   dxfg_InstrumentProfile_getOptionType(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setOptionType(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getExpirationStyle(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setExpirationStyle(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getSettlementStyle(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setSettlementStyle(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getPriceIncrements(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setPriceIncrements(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getTradingHours(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_setTradingHours(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *value);
const char*                                   dxfg_InstrumentProfile_getField(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *name);
int32_t                                       dxfg_InstrumentProfile_setField(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *name, const char *value);
double                                        dxfg_InstrumentProfile_getNumericField(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *name);
int32_t                                       dxfg_InstrumentProfile_setNumericField(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *name, double value);
int32_t                                       dxfg_InstrumentProfile_getDateField(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *name);
int32_t                                       dxfg_InstrumentProfile_setDateField(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip, const char *name, int32_t value);
dxfg_string_list*                             dxfg_InstrumentProfile_getNonEmptyCustomFieldNames(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);
int32_t                                       dxfg_InstrumentProfile_release(graal_isolatethread_t *thread, dxfg_instrument_profile_t *ip);

/// A structure that contains a handle to custom instrument profile fields (InstrumentProfileCustomFields).
/// Use the dxfg_JavaObjectHandler_clone() function to extend life of this handle (clone the object reference)
typedef struct dxfg_instrument_profile_custom_fields_t {
    dxfg_java_object_handler handler;
} dxfg_instrument_profile_custom_fields_t;

/**
 * A structure that contains the instrument profile fields, as well as a computed hash of the custom fields (InstrumentProfileCustomFields).
 * If the custom fields pointer handle is NULL, the fields are empty. The hash will then be 0.
 */
typedef struct dxfg_instrument_profile2_t {
  char* type;
  char* symbol;
  char* description;
  char* local_symbol;
  char* local_description;
  char* country;
  char* opol;
  char* exchange_data;
  char* exchanges;
  char* currency;
  char* base_currency;
  char* cfi;
  char* isin;
  char* sedol;
  char* cusip;
  int32_t icb;
  int32_t sic;
  double multiplier;
  char* product;
  char* underlying;
  double spc;
  char* additional_underlyings;
  char* mmy;
  int32_t expiration;
  int32_t last_trade;
  double strike;
  char* option_type;
  char* expiration_style;
  char* settlement_style;
  char* price_increments;
  char* trading_hours;
  dxfg_instrument_profile_custom_fields_t* instrument_profile_custom_fields;
  int32_t instrument_profile_custom_fields_hash;
} dxfg_instrument_profile2_t;

typedef struct dxfg_instrument_profile2_list_t {
    int32_t size;
    dxfg_instrument_profile2_t **elements;
} dxfg_instrument_profile2_list_t;

/**
 * Formats the number according to the internal number format in QD for instrument profile fields.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] number The source number.
 * @param[out] result The formatted number.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_String_release() to free the result.
 */
int32_t dxfg_InstrumentProfileField_formatNumber(graal_isolatethread_t *thread, double number, DXFG_OUT char** result);

/**
 * Parses a number according to the internal number format in QD for instrument profile fields.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] string The source string.
 * @param[out] number The parsed number.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileField_parseNumber(graal_isolatethread_t *thread, const char* string, DXFG_OUT double* number);

/**
 * Formats the date according to the internal date format in QD for instrument profile fields.
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] date The source date (the number of days since January 1, 1970, 00:00:00 GMT)
 * @param[out] result The formatted date.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_String_release() to free the result.
 */
int32_t dxfg_InstrumentProfileField_formatDate(graal_isolatethread_t *thread, int32_t date, DXFG_OUT char** result);

/**
 * Parses a date according to the internal date format in QD for instrument profile fields.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] string The source string.
 * @param[out] date The parsed date (the number of days since January 1, 1970, 00:00:00 GMT)
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileField_parseDate(graal_isolatethread_t *thread, const char* string, DXFG_OUT int32_t* date);

/**
 * Creates a new instance of InstrumentProfileCustomFields and returns a handle to it.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[out] custom_fields The handle of the InstrumentProfileCustomFields instance.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_InstrumentProfileCustomFields_new(graal_isolatethread_t *thread, DXFG_OUT dxfg_instrument_profile_custom_fields_t **custom_fields);

/**
 * Creates a new instance of InstrumentProfileCustomFields using an array of string fields (key - value, consecutive),
 * rehashes the fields, and returns a handle to the new instance.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields_array The array string fields.
 * @param[in] size The array's size.
 * @param[out] custom_fields The handle of the InstrumentProfileCustomFields instance.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_InstrumentProfileCustomFields_new2(graal_isolatethread_t *thread, const char** custom_fields_array, int32_t size, DXFG_OUT dxfg_instrument_profile_custom_fields_t **custom_fields);

/**
 * Creates a copy of InstrumentProfileCustomFields and returns a handle to it.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] source The handle of the source InstrumentProfileCustomFields instance.
 * @param[out] custom_fields The handle of the InstrumentProfileCustomFields instance.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_InstrumentProfileCustomFields_new3(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t * source, DXFG_OUT dxfg_instrument_profile_custom_fields_t **custom_fields);

/**
 * Creates a new instance of InstrumentProfileCustomFields using a list of string fields (key - value, consecutive),
 * rehashes the fields, and returns a handle to the new instance.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields_string_list The list of string fields.
 * @param[out] custom_fields The handle of the InstrumentProfileCustomFields instance.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_InstrumentProfileCustomFields_new4(graal_isolatethread_t *thread, const dxfg_string_list* custom_fields_string_list, DXFG_OUT dxfg_instrument_profile_custom_fields_t **custom_fields);


/**
 * Gets the value (or NULL) of a field by name.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields The custom fields handle.
 * @param[in] name The field name.
 * @param[out] result The field value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_String_release() to free the result
 */
int32_t dxfg_InstrumentProfileCustomFields_getField(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t *custom_fields, const char *name, DXFG_OUT char** result);

/**
 * Sets the value of a field by name.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields The custom fields handle.
 * @param[in] name The field name.
 * @param[in] value The field value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCustomFields_setField(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t *custom_fields, const char *name, const char* value);

/**
 * Gets the numeric value (or 0) of a field by name or a date if the string representation looks like a date.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields The custom fields handle.
 * @param[in] name The field name.
 * @param[out] result The numeric value of the field.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCustomFields_getNumericField(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t *custom_fields, const char *name, DXFG_OUT double* result);

/**
 * Sets the numeric value of a field by name.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields The custom fields handle.
 * @param[in] name The field name.
 * @param[in] value The field value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCustomFields_setNumericField(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t *custom_fields, const char *name, double value);

/**
 * Gets the date (the number of days since January 1, 1970, 00:00:00 GMT) value (or 0) of a field by name.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields The custom fields handle.
 * @param[in] name The field name.
 * @param[out] result The date value of the field.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCustomFields_getDateField(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t *custom_fields, const char *name, DXFG_OUT int32_t* result);

/**
 * Sets the date value of a field by name.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields The custom fields handle.
 * @param[in] name The field name.
 * @param[in] value The field value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCustomFields_setDateField(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t *custom_fields, const char *name, int32_t value);

/**
 * Creates and populates a list of strings with field names.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields The custom fields handle.
 * @param[out] target_field_names The list of field names.
 * @param[out] updated A flag indicating whether any names were written. May not be used (this pointer may be NULL).
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * @deprecated Because the updated parameter is useless. Use dxfg_InstrumentProfileCustomFields_getNonEmptyFieldNames instead.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_CList_String_release() to free the result.
 */
int32_t dxfg_InstrumentProfileCustomFields_addNonEmptyFieldNames(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t *custom_fields, DXFG_OUT dxfg_string_list** target_field_names, DXFG_OUT int32_t* updated);

/**
 * Creates and populates a list of strings with field names.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] custom_fields The custom fields handle.
 * @param[out] target_field_names The list of field names.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_CList_String_release() to free the result.
 */
int32_t dxfg_InstrumentProfileCustomFields_getNonEmptyFieldNames(graal_isolatethread_t *thread, dxfg_instrument_profile_custom_fields_t *custom_fields, DXFG_OUT dxfg_string_list** target_field_names);

/**
 * Updates the instrument profile inside the collector.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] collector The IPF collector handle.
 * @param[int] instrument_profile An instrument whose fields are used to update an instrument within a collector.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCollector_updateInstrumentProfile2(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, dxfg_instrument_profile2_t *instrument_profile);

/**
 * Updates the instrument profile inside the collector.
 * This is a caching version of the function. All string fields of the instrument profile, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] collector The IPF collector handle.
 * @param[int] instrument_profile An instrument whose fields are used to update an instrument within a collector.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCollector_updateInstrumentProfile2_cached(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, dxfg_instrument_profile2_t *instrument_profile);

/**
 * Updates multiple instrument profiles within a collector.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] collector The IPF collector handle.
 * @param[in] instrument_profiles An array of instrument profiles whose fields will be used to update the instrument
 * profiles within the collector.
 * @param[in] size The array size.
 * @param[in] generation An optional parameter (may be NULL) to version the instrument profile update process.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCollector_updateInstrumentProfiles2(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, const dxfg_instrument_profile2_t* instrument_profiles, int32_t size, dxfg_java_object_handler *generation);

/**
 * Updates multiple instrument profiles within a collector.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] collector The IPF collector handle.
 * @param[in] instrument_profiles An array of instrument profiles whose fields will be used to update the instrument profiles within the collector.
 * @param[in] size The array size.
 * @param[in] generation An optional parameter (may be NULL) to version the instrument profile update process.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCollector_updateInstrumentProfiles2_cached(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, const dxfg_instrument_profile2_t* instrument_profiles, int32_t size, dxfg_java_object_handler *generation);

/**
 * Updates multiple instrument profiles within a collector.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] collector The IPF collector handle.
 * @param[in] instrument_profiles A list of instrument profiles whose fields will be used to update the instrument
 * profiles within the collector.
 * @param[in] generation An optional parameter (may be NULL) to version the instrument profile update process.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCollector_updateInstrumentProfiles3(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, const dxfg_instrument_profile2_list_t* instrument_profiles, dxfg_java_object_handler *generation);

/**
 * Updates multiple instrument profiles within a collector.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] collector The IPF collector handle.
 * @param[in] instrument_profiles A list of instrument profiles whose fields will be used to update the instrument
 * profiles within the collector.
 * @param[in] generation An optional parameter (may be NULL) to version the instrument profile update process.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_InstrumentProfileCollector_updateInstrumentProfiles3_cached(graal_isolatethread_t *thread, dxfg_ipf_collector_t *collector, const dxfg_instrument_profile2_list_t* instrument_profiles, dxfg_java_object_handler *generation);

/**
 * Returns the next instrument profile.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] iterable_ip The handle to an iterable wrapper for a list of instrument profiles.
 * @param[out] instrument_profile The next instrument profile.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile_free() to free the result.
 */
int32_t dxfg_Iterable_InstrumentProfile_next2(graal_isolatethread_t *thread, dxfg_iterable_ip_t *iterable_ip, DXFG_OUT dxfg_instrument_profile2_t **instrument_profile);

/**
 * Returns the next instrument profile.
 * This is a caching version of the function. All string fields of the instrument profile, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] iterable_ip The handle to an iterable wrapper for a list of instrument profiles.
 * @param[out] instrument_profile The next instrument profile.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile_free_cached() to free the result.
 */
int32_t dxfg_Iterable_InstrumentProfile_next2_cached(graal_isolatethread_t *thread, dxfg_iterable_ip_t *iterable_ip, DXFG_OUT dxfg_instrument_profile2_t **instrument_profile);

/**
 * Reads and returns instrument profiles from a file.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile4(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a file.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile4_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a file.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[in] user The base auth user name.
 * @param[in] password The base auth user password.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile5(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, const char *user, const char *password, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a file.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[in] user The base auth user name.
 * @param[in] password The base auth user password.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile5_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, const char *user, const char *password, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a file.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[in] token The token handle.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile6(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, dxfg_auth_token_t *token, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a file.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[in] token The token handle.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile6_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, dxfg_auth_token_t *token, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a file.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile7(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a file.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile7_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a file.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[in] user The base auth user name.
 * @param[in] password The base auth user password.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile8(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, const char *user, const char *password, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a file.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[in] user The base auth user name.
 * @param[in] password The base auth user password.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile8_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, const char *user, const char *password, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a file.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[in] token The token handle.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile9(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, dxfg_auth_token_t *token, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a file.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] address The file address.
 * @param[in] token The token handle.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readFromFile9_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, const char *address, dxfg_auth_token_t *token, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a input stream.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_read3(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a input stream.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_read3_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a input stream.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[in] address The file address (for debug purposes).
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_read4(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, const char *address, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a input stream.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[in] address The file address (for debug purposes).
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_read4_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, const char *address, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a input stream.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_read5(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a input stream.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_read5_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a input stream.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[in] address The file address (for debug purposes).
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_read6(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, const char *address, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a input stream.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[in] address The file address (for debug purposes).
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_read6_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, const char *address, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a compressed input stream.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readCompressed2(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a compressed input stream.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[out] instrument_profiles The resulting array of instrument profiles.
 * @param[out] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profiles_array_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readCompressed2_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, DXFG_OUT dxfg_instrument_profile2_t **instrument_profiles, DXFG_OUT int32_t* size);

/**
 * Reads and returns instrument profiles from a compressed input stream.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readCompressed3(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Reads and returns instrument profiles from a compressed input stream.
 * This is a caching version of the function. All string fields of the instrument profiles, except for custom fields, will be cached (in theory, no extra copies will be created).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] reader The IPF reader handle.
 * @param[in] is The input stream handle.
 * @param[out] instrument_profiles The resulting list of instrument profiles.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_instrument_profile2_list_free_cached() to free the result.
 */
int32_t dxfg_InstrumentProfileReader_readCompressed3_cached(graal_isolatethread_t *thread, dxfg_instrument_profile_reader_t *reader, dxfg_input_stream_t* is, DXFG_OUT dxfg_instrument_profile2_list_t **instrument_profiles);

/**
 * Frees up memory occupied by instrument profile fields, releases custom fields.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] instrument_profile The instrument profile.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_instrument_profile_free(graal_isolatethread_t *thread, dxfg_instrument_profile2_t *instrument_profile);

/**
 * Frees up memory occupied by instrument profile fields if necessary or reduces reference counters in cache, releases
 * custom fields.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] instrument_profile The instrument profile.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_instrument_profile_free_cached(graal_isolatethread_t *thread, dxfg_instrument_profile2_t *instrument_profile);

/**
 * Frees up memory occupied by instrument profile array fields, releases custom fields.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] instrument_profiles The instrument profiles array.
 * @param[in] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_instrument_profiles_array_free(graal_isolatethread_t *thread, dxfg_instrument_profile2_t *instrument_profiles, int32_t size);

/**
 * Frees up memory occupied by instrument profile array fields if necessary or reduces reference counters in cache, releases custom fields.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] instrument_profiles The instrument profiles array.
 * @param[in] size The array size.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_instrument_profiles_array_free_cached(graal_isolatethread_t *thread, dxfg_instrument_profile2_t *instrument_profiles, int32_t size);

/**
 * Frees up memory occupied by instrument profiles list.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] instrument_profiles The instrument profiles list.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_instrument_profile2_list_free(graal_isolatethread_t *thread, dxfg_instrument_profile2_list_t* instrument_profiles);

/**
 * Frees up memory occupied by instrument profiles list.
 * Frees up memory occupied by instrument profile fields if necessary or reduces reference counters in cache.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] instrument_profiles The instrument profiles list.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_instrument_profile2_list_free_cached(graal_isolatethread_t *thread, dxfg_instrument_profile2_list_t* instrument_profiles);

/** @} */ // end of InstrumentProfile

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_SDK_IPF_H_
