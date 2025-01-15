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

typedef struct dxfg_instrument_profile_custom_fields_t {
    dxfg_java_object_handler handler;
} dxfg_instrument_profile_custom_fields_t;

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


int32_t dxfg_InstrumentProfileField_formatNumber(graal_isolatethread_t *thread, double number, char** result);
int32_t dxfg_InstrumentProfileField_parseNumber(graal_isolatethread_t *thread, const char* string, double* number);
int32_t dxfg_InstrumentProfileField_formatDate(graal_isolatethread_t *thread, int32_t date, char** result);
int32_t dxfg_InstrumentProfileField_parseNumber(graal_isolatethread_t *thread, const char* string, int32_t* date);

/** @} */ // end of InstrumentProfile

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_SDK_IPF_H_
