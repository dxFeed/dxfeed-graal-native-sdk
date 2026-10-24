// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The older functions next to their dxfg_v4_ twins: the same results, and the objects of one generation work with the
// functions of the other. The macro is defined empty before the headers, which disables the deprecation of the older
// functions (the way integrators can silence it).
#define DXFG_DEPRECATED(message)

#include <catch.hpp>
#include <dxfg_api.h>

#include "isolate.hpp"

#include <atomic>
#include <chrono>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace test {

TEST_CASE("The older and the v4 functions create equal objects that each generation releases", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_time_period_t *older = dxfg_TimePeriod_valueOf2(thread, "PT1M30S");
    dxfg_v4_time_period_t *v4 = nullptr;

    REQUIRE(older != nullptr);
    REQUIRE(dxfg_v4_TimePeriod_valueOfString(thread, "PT1M30S", &v4) == DXFG_EXECUTE_SUCCESSFULLY);

    int32_t equal = 0;

    CHECK(dxfg_v4_Object_equals(thread, &older->handler, &v4->handler, &equal) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);
    CHECK(dxfg_Object_equals(thread, &older->handler, &v4->handler) == 1);
    CHECK(dxfg_TimePeriod_getSeconds(thread, v4) == 90);

    // Each one is released by the other generation's release function.
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &older->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_JavaObjectHandler_release(thread, &v4->handler) == DXFG_EXECUTE_SUCCESSFULLY);

    const char *olderString = dxfg_TimeZone_getID(thread, nullptr);

    CHECK(olderString == nullptr); // An error of the older function: NULL.

    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

    CHECK(exception != nullptr);
    CHECK(dxfg_v4_Exception_release(thread, exception) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The exception of a failed function is taken by either generation", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_Exception_release(thread, dxfg_get_and_clear_thread_exception_t(thread)); // Left by an earlier test.

    // An older function fails, the v4 function takes the exception.
    CHECK(dxfg_TimePeriod_valueOf2(thread, "not a period") == nullptr);

    dxfg_v4_exception_t *v4Exception = nullptr;

    REQUIRE(dxfg_v4_get_and_clear_thread_exception(thread, &v4Exception) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(v4Exception != nullptr);

    const std::string className = v4Exception->class_name == nullptr ? "" : v4Exception->class_name;

    dxfg_Exception_release(thread, v4Exception);

    // A v4 function fails with the same exception, the older function takes it.
    dxfg_v4_time_period_t *period = nullptr;

    CHECK(dxfg_v4_TimePeriod_valueOfString(thread, "not a period", &period) == DXFG_EXECUTE_FAIL);

    dxfg_exception_t *olderException = dxfg_get_and_clear_thread_exception_t(thread);

    REQUIRE(olderException != nullptr);
    CHECK(className == (olderException->class_name == nullptr ? "" : olderException->class_name));
    CHECK(dxfg_get_and_clear_thread_exception_t(thread) == nullptr);

    dxfg_Exception_release(thread, olderException);
}

TEST_CASE("A twin returns -1 where the older function returns INT32_MIN", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    CHECK(dxfg_TimeZone_setRawOffset(thread, nullptr, 0) == INT32_MIN);

    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

    CHECK(exception != nullptr);
    dxfg_Exception_release(thread, exception);

    CHECK(dxfg_v4_TimeZone_setRawOffset(thread, nullptr, 0) == DXFG_EXECUTE_FAIL);

    exception = dxfg_get_and_clear_thread_exception_t(thread);

    CHECK(exception != nullptr);
    dxfg_Exception_release(thread, exception);
}

TEST_CASE("A list that an older function returned is released by the v4 function", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_string_list *list = dxfg_Tools_parseSymbols(thread, "AAPL,IBM");

    REQUIRE(list != nullptr);
    CHECK(list->size == 2);
    CHECK(dxfg_v4_string_list_release(thread, list) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A list of event types is released by the release function of either generation", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_event_clazz_t quote = DXFG_EVENT_QUOTE;
    dxfg_event_clazz_t trade = DXFG_EVENT_TRADE;
    dxfg_event_clazz_t *elements[] = {&quote, &trade};
    dxfg_event_clazz_list_t eventTypes{2, elements};
    dxfg_subscription_t *subscription = dxfg_DXFeedSubscription_new2(thread, &eventTypes);

    REQUIRE(subscription != nullptr);

    dxfg_v4_event_clazz_list *fromV4 = nullptr;

    REQUIRE(dxfg_v4_DXFeedSubscription_getEventTypes(thread, subscription, &fromV4) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(fromV4 != nullptr);
    CHECK(fromV4->size == 2);
    CHECK(dxfg_CList_EventClazz_release(thread, fromV4) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_event_clazz_list_t *fromOlder = dxfg_DXFeedSubscription_getEventTypes(thread, subscription);

    REQUIRE(fromOlder != nullptr);
    CHECK(fromOlder->size == 2);
    CHECK(dxfg_v4_event_clazz_list_release(thread, fromOlder) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_JavaObjectHandler_release(thread, &subscription->handler);
}

TEST_CASE("A list of symbols is released by the release function of either generation", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_subscription_t *subscription = dxfg_DXFeedSubscription_new(thread, DXFG_EVENT_QUOTE);

    REQUIRE(subscription != nullptr);

    dxfg_string_symbol_t first{{STRING}, "V4-FIRST"};
    dxfg_string_symbol_t second{{STRING}, "V4-SECOND"};

    REQUIRE(dxfg_DXFeedSubscription_addSymbol(thread, subscription, &first.supper) == 0);
    REQUIRE(dxfg_DXFeedSubscription_addSymbol(thread, subscription, &second.supper) == 0);

    dxfg_v4_symbol_list *fromV4 = nullptr;

    REQUIRE(dxfg_v4_DXFeedSubscription_getSymbols(thread, subscription, &fromV4) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(fromV4 != nullptr);
    CHECK(fromV4->size == 2);
    CHECK(dxfg_CList_symbol_release(thread, fromV4) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_symbol_list *fromOlder = dxfg_DXFeedSubscription_getSymbols(thread, subscription);

    REQUIRE(fromOlder != nullptr);
    CHECK(fromOlder->size == 2);
    CHECK(dxfg_v4_symbol_list_release(thread, fromOlder) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_JavaObjectHandler_release(thread, &subscription->handler);
}

TEST_CASE("dxfg_DXFeed_getLastEvent2 and its twin get the same event and fail alike", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_endpoint_t *hub = dxfg_DXEndpoint_create2(thread, DXFG_ENDPOINT_ROLE_LOCAL_HUB);

    REQUIRE(hub != nullptr);

    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(thread, hub);
    dxfg_subscription_t *subscription = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_PROFILE);
    dxfg_string_symbol_t symbol{{STRING}, "V4-PARITY"};

    REQUIRE(dxfg_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) == 0);

    dxfg_profile_t profile{};

    profile.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
    profile.market_event.event_symbol = symbol.symbol;
    profile.description = "published description";

    dxfg_event_type_t *events[] = {&profile.market_event.event_type};
    dxfg_event_type_list list{1, events};
    dxfg_publisher_t *publisher = dxfg_DXEndpoint_getPublisher(thread, hub);

    REQUIRE(dxfg_DXPublisher_publishEvents(thread, publisher, &list) == 0);
    dxfg_JavaObjectHandler_release(thread, &publisher->handler);

    dxfg_profile_t given{};

    given.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
    given.market_event.event_symbol = symbol.symbol;

    dxfg_event_type_t *fromOlder = nullptr;
    dxfg_event_type_t *fromV4 = nullptr;

    // Waits for the published event to get to the feed.
    for (int i = 0; i < 500; ++i) {
        REQUIRE(dxfg_DXFeed_getLastEvent2(thread, feed, &given.market_event.event_type, &fromOlder) == 0);

        if (reinterpret_cast<dxfg_profile_t *>(fromOlder)->description != nullptr) {
            break;
        }

        dxfg_EventType_release(thread, fromOlder);
        fromOlder = nullptr;
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    REQUIRE(fromOlder != nullptr);
    REQUIRE(dxfg_v4_DXFeed_getLastEvent(thread, feed, &given.market_event.event_type, &fromV4) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(fromV4 != nullptr);
    CHECK(std::string(reinterpret_cast<dxfg_profile_t *>(fromV4)->description) ==
          reinterpret_cast<dxfg_profile_t *>(fromOlder)->description);
    dxfg_EventType_release(thread, fromOlder);
    dxfg_v4_EventType_release(thread, fromV4);

    // TimeAndSale is not a lasting event: both fail, both write NULL (the exception that the v4 conventions in
    // dxfg_common.h name for getLastEvent and getLastEvents).
    dxfg_time_and_sale_t timeAndSale{};

    timeAndSale.market_event.event_type.clazz = DXFG_EVENT_TIME_AND_SALE;
    timeAndSale.market_event.event_symbol = symbol.symbol;
    fromOlder = &given.market_event.event_type;
    fromV4 = &given.market_event.event_type;
    CHECK(dxfg_DXFeed_getLastEvent2(thread, feed, &timeAndSale.market_event.event_type, &fromOlder) ==
          DXFG_EXECUTE_FAIL);

    dxfg_exception_t *olderException = dxfg_get_and_clear_thread_exception_t(thread);

    CHECK(dxfg_v4_DXFeed_getLastEvent(thread, feed, &timeAndSale.market_event.event_type, &fromV4) ==
          DXFG_EXECUTE_FAIL);

    dxfg_exception_t *v4Exception = dxfg_get_and_clear_thread_exception_t(thread);

    REQUIRE(olderException != nullptr);
    REQUIRE(v4Exception != nullptr);
    CHECK(std::string(v4Exception->class_name) == olderException->class_name);
    CHECK(fromOlder == nullptr);
    CHECK(fromV4 == nullptr);
    dxfg_Exception_release(thread, olderException);
    dxfg_Exception_release(thread, v4Exception);

    // The same for a list: both fail, both write NULL.
    dxfg_event_type_t *wrongEvents[] = {&given.market_event.event_type, &timeAndSale.market_event.event_type};
    dxfg_event_type_list wrongList{2, wrongEvents};
    dxfg_event_type_list marker{};
    dxfg_event_type_list *listFromOlder = &marker;
    dxfg_event_type_list *listFromV4 = &marker;

    CHECK(dxfg_DXFeed_getLastEvents2(thread, feed, &wrongList, &listFromOlder) == DXFG_EXECUTE_FAIL);
    olderException = dxfg_get_and_clear_thread_exception_t(thread);
    CHECK(dxfg_v4_DXFeed_getLastEvents(thread, feed, &wrongList, &listFromV4) == DXFG_EXECUTE_FAIL);
    v4Exception = dxfg_get_and_clear_thread_exception_t(thread);

    REQUIRE(olderException != nullptr);
    REQUIRE(v4Exception != nullptr);
    CHECK(std::string(v4Exception->class_name) == olderException->class_name);
    CHECK(listFromOlder == nullptr);
    CHECK(listFromV4 == nullptr);
    dxfg_Exception_release(thread, olderException);
    dxfg_Exception_release(thread, v4Exception);

    dxfg_DXFeedSubscription_close(thread, subscription);
    dxfg_JavaObjectHandler_release(thread, &subscription->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_DXEndpoint_close(thread, hub);
    dxfg_JavaObjectHandler_release(thread, &hub->handler);
}

namespace {

// A listener that calls a failing function and leaves its exception in the thread, which the conventions allow.
void failInListener(graal_isolatethread_t *thread, dxfg_event_type_list * /*events*/, void *userData) {
    dxfg_v4_time_period_t *period = nullptr;

    dxfg_v4_TimePeriod_valueOfString(thread, "not a period", &period);
    static_cast<std::atomic<int> *>(userData)->fetch_add(1);
}

} // namespace

TEST_CASE("A twin succeeds when a callback that it runs leaves an exception", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_Exception_release(thread, dxfg_get_and_clear_thread_exception_t(thread)); // Left by an earlier test.

    // The executor runs the calls of the listener on the thread that calls processAllPendingTasks.
    dxfg_executor_t *executor = dxfg_ExecutorBaseOnConcurrentLinkedQueue_new(thread);
    dxfg_endpoint_builder_t *builder = dxfg_DXEndpoint_newBuilder(thread);

    REQUIRE(executor != nullptr);
    REQUIRE(builder != nullptr);
    REQUIRE(dxfg_DXEndpoint_Builder_withRole(thread, builder, DXFG_ENDPOINT_ROLE_LOCAL_HUB) == 0);
    REQUIRE(dxfg_DXEndpoint_Builder_withExecutor(thread, builder, executor) == 0);

    dxfg_endpoint_t *hub = dxfg_DXEndpoint_Builder_build(thread, builder);

    dxfg_JavaObjectHandler_release(thread, &builder->handler);
    REQUIRE(hub != nullptr);

    std::atomic<int> calls{0};
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(thread, hub);
    dxfg_subscription_t *subscription = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &failInListener, &calls);

    REQUIRE(subscription != nullptr);
    REQUIRE(listener != nullptr);
    REQUIRE(dxfg_DXFeedSubscription_addEventListener(thread, subscription, listener) == 0);

    dxfg_string_symbol_t symbol{{STRING}, "V4-API-TEST"};

    REQUIRE(dxfg_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) == 0);

    dxfg_quote_t quote{};

    quote.market_event.event_type.clazz = DXFG_EVENT_QUOTE;
    quote.market_event.event_symbol = symbol.symbol;

    dxfg_event_type_t *events[] = {&quote.market_event.event_type};
    dxfg_event_type_list list{1, events};
    dxfg_publisher_t *publisher = dxfg_DXEndpoint_getPublisher(thread, hub);

    REQUIRE(dxfg_DXPublisher_publishEvents(thread, publisher, &list) == 0);
    dxfg_JavaObjectHandler_release(thread, &publisher->handler);

    int32_t status = DXFG_EXECUTE_SUCCESSFULLY;

    for (int i = 0; i < 500 && calls.load() == 0 && status == DXFG_EXECUTE_SUCCESSFULLY; ++i) {
        status = dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(thread, executor);
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    CHECK(calls.load() > 0);
    CHECK(status == DXFG_EXECUTE_SUCCESSFULLY);

    // The exception that the listener left stays stored, as with the older function.
    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

    CHECK(exception != nullptr);
    dxfg_Exception_release(thread, exception);

    dxfg_DXFeedSubscription_removeEventListener(thread, subscription, listener);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_DXFeedSubscription_close(thread, subscription);
    dxfg_JavaObjectHandler_release(thread, &subscription->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_DXEndpoint_close(thread, hub);
    dxfg_JavaObjectHandler_release(thread, &hub->handler);
    dxfg_JavaObjectHandler_release(thread, &executor->handler);
}

TEST_CASE("The older and the v4 readers give the same instrument profiles, each generation releases them",
          "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_instrument_profile_reader_t *reader = dxfg_InstrumentProfileReader_new(thread);

    REQUIRE(reader != nullptr);

    dxfg_v4_instrument_profile_data_list *v4 = nullptr;

    REQUIRE(dxfg_v4_InstrumentProfileReader_readFromFile(thread, reader, V4_API_TEST_IPF_FILE, &v4) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(v4 != nullptr);

    std::vector<std::string> expected{};

    for (int32_t i = 0; i < v4->size; ++i) {
        expected.emplace_back(v4->elements[i]->symbol);
    }

    CHECK(expected.size() == 10);

    // The list form of the older function.
    dxfg_instrument_profile2_list_t *list = nullptr;
    std::vector<std::string> symbols{};

    REQUIRE(dxfg_InstrumentProfileReader_readFromFile7(thread, reader, V4_API_TEST_IPF_FILE, &list) == 0);
    REQUIRE(list != nullptr);

    for (int32_t i = 0; i < list->size; ++i) {
        symbols.emplace_back(list->elements[i]->symbol);
    }

    CHECK(symbols == expected);
    CHECK(dxfg_v4_instrument_profile_data_list_release(thread, list) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_instrument_profile2_list_free(thread, v4) == 0);

    // The array + size form, whose twin is the v4 list function.
    dxfg_instrument_profile2_t *array = nullptr;
    int32_t size = 0;

    symbols.clear();
    REQUIRE(dxfg_InstrumentProfileReader_readFromFile4(thread, reader, V4_API_TEST_IPF_FILE, &array, &size) == 0);

    for (int32_t i = 0; i < size; ++i) {
        symbols.emplace_back(array[i].symbol);
    }

    CHECK(symbols == expected);
    CHECK(dxfg_instrument_profiles_array_free(thread, array, size) == 0);

    // The handle form.
    dxfg_instrument_profile_list *handles = dxfg_InstrumentProfileReader_readFromFile(thread, reader,
                                                                                      V4_API_TEST_IPF_FILE);

    REQUIRE(handles != nullptr);
    symbols.clear();

    for (int32_t i = 0; i < handles->size; ++i) {
        const char *symbol = dxfg_InstrumentProfile_getSymbol(thread, handles->elements[i]);

        REQUIRE(symbol != nullptr);
        symbols.emplace_back(symbol);
        dxfg_String_release(thread, symbol);
    }

    CHECK(symbols == expected);
    CHECK(dxfg_v4_instrument_profile_list_release(thread, handles) == DXFG_EXECUTE_SUCCESSFULLY);
    dxfg_JavaObjectHandler_release(thread, &reader->handler);
}

TEST_CASE("The older and the v4 schedule and glossary functions give the same values", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    // A schedule from the same definition.
    const char *definition = "(tz=America/New_York;0=p04000930r09301600a16002000)";
    dxfg_schedule_t *olderSchedule = dxfg_Schedule_getInstance2(thread, definition);
    dxfg_v4_schedule_t *v4Schedule = nullptr;
    char *olderDefinition = nullptr;
    char *v4Definition = nullptr;
    int32_t equal = 0;

    REQUIRE(olderSchedule != nullptr);
    REQUIRE(dxfg_v4_Schedule_getInstanceByDefinition(thread, definition, &v4Schedule) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Object_equals(thread, &olderSchedule->handler, &v4Schedule->handler, &equal) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);
    REQUIRE(dxfg_Schedule_getDefinition(thread, olderSchedule, &olderDefinition) == 0);
    REQUIRE(dxfg_v4_Schedule_getDefinition(thread, v4Schedule, &v4Definition) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(std::string(olderDefinition) == v4Definition);
    CHECK(dxfg_v4_String_release(thread, olderDefinition) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_String_release(thread, v4Definition) == 0);

    // A list of sessions returned by the older function is released together with its sessions by the v4 function.
    dxfg_day_t *olderDay = dxfg_Schedule_getDayByTime(thread, olderSchedule, 0);
    dxfg_session_list *olderSessions = nullptr;

    REQUIRE(olderDay != nullptr);
    olderSessions = dxfg_Day_getSessions(thread, olderDay);
    REQUIRE(olderSessions != nullptr);
    CHECK(olderSessions->size == 5);
    CHECK(dxfg_v4_session_list_release(thread, olderSessions) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_Day_release(thread, olderDay) == 0);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &olderSchedule->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_JavaObjectHandler_release(thread, &v4Schedule->handler) == 0);

    // The array of the older function and the list of the v4 one hold the same increments.
    dxfg_price_increments_t *priceIncrements = nullptr;
    double *olderIncrements = nullptr;
    int32_t size = 0;
    dxfg_v4_double_list *v4Increments = nullptr;

    REQUIRE(dxfg_PriceIncrements_valueOf(thread, "0.0001 1; 0.01", &priceIncrements) == 0);
    REQUIRE(dxfg_PriceIncrements_getPriceIncrements(thread, priceIncrements, &olderIncrements, &size) == 0);
    REQUIRE(dxfg_v4_PriceIncrements_getPriceIncrements(thread, priceIncrements, &v4Increments) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(std::vector<double>(olderIncrements, olderIncrements + size) ==
          std::vector<double>(v4Increments->elements, v4Increments->elements + v4Increments->size));
    CHECK(dxfg_free(thread, olderIncrements) == 0);
    CHECK(dxfg_v4_double_list_release(thread, v4Increments) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_JavaObjectHandler_release(thread, &priceIncrements->handler) == 0);

    // The same map entries.
    dxfg_additional_underlyings_t *underlyings = nullptr;
    dxfg_string_to_double_map_entry_t *olderEntries = nullptr;
    dxfg_v4_string_to_double_map_entry_list *v4Entries = nullptr;

    REQUIRE(dxfg_AdditionalUnderlyings_valueOf(thread, "SE 50; FIS 53.875", &underlyings) == 0);
    REQUIRE(dxfg_AdditionalUnderlyings_getMap(thread, underlyings, &olderEntries, &size) == 0);
    REQUIRE(dxfg_v4_AdditionalUnderlyings_getMap(thread, underlyings, &v4Entries) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(v4Entries->size == size);

    for (int32_t i = 0; i < size; ++i) {
        CHECK(std::string(olderEntries[i].key) == v4Entries->elements[i].key);
        CHECK(olderEntries[i].value == v4Entries->elements[i].value);
    }

    CHECK(dxfg_free_string_to_double_map_entries(thread, olderEntries, size) == 0);
    CHECK(dxfg_v4_string_to_double_map_entry_list_release(thread, v4Entries) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_JavaObjectHandler_release(thread, &underlyings->handler) == 0);

    // The same values of a CFI code: an array of handle structures and a list of pointers to handles.
    dxfg_cfi_t *cfi = nullptr;
    dxfg_cfi_value_t *olderValues = nullptr;
    dxfg_v4_cfi_value_list *v4Values = nullptr;

    REQUIRE(dxfg_CFI_valueOf(thread, "ESVUFR", &cfi) == 0);
    REQUIRE(dxfg_CFI_decipher(thread, cfi, &olderValues, &size) == 0);
    REQUIRE(dxfg_v4_CFI_decipher(thread, cfi, &v4Values) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(v4Values->size == size);

    for (int32_t i = 0; i < size; ++i) {
        int16_t olderCode = 0;
        int16_t v4Code = -1;

        REQUIRE(dxfg_CFI_Value_getCode(thread, &olderValues[i], &olderCode) == 0);
        REQUIRE(dxfg_v4_CFI_Value_getCode(thread, v4Values->elements[i], &v4Code) == DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(olderCode == v4Code);
    }

    CHECK(dxfg_JavaObjectHandler_array_release(thread, (const dxfg_java_object_handler **)olderValues, size) == 0);
    CHECK(dxfg_v4_cfi_value_list_release(thread, v4Values) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_JavaObjectHandler_release(thread, &cfi->handler) == 0);
}

TEST_CASE("The older and the v4 functions of the services and the models give the same values", "[V4Api][Parity]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_system_set_property(thread, "dxfg.v4.parity.property", "parity value") == DXFG_EXECUTE_SUCCESSFULLY);

    // A property: a string of the older function is released by the v4 release function, and the other way round.
    const char *olderValue = dxfg_system_get_property(thread, "dxfg.v4.parity.property");
    const char *v4Value = nullptr;

    REQUIRE(olderValue != nullptr);
    REQUIRE(dxfg_v4_system_get_property(thread, "dxfg.v4.parity.property", &v4Value) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(v4Value != nullptr);
    CHECK(std::string(olderValue) == std::string(v4Value));
    CHECK(dxfg_v4_system_release_property(thread, olderValue) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_system_release_property(thread, v4Value) == DXFG_EXECUTE_SUCCESSFULLY);

    // A missing property: NULL from both, without an exception.
    const char *v4Missing = "any value that the function overwrites";

    CHECK(dxfg_system_get_property(thread, "dxfg.v4.parity.missing.property") == nullptr);
    CHECK(dxfg_v4_system_get_property(thread, "dxfg.v4.parity.missing.property", &v4Missing) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(v4Missing == nullptr);
    CHECK(dxfg_get_and_clear_thread_exception_t(thread) == nullptr);

    // The filter of an order book model: the older function returns the enum value, the v4 one writes it.
    dxfg_order_book_model_t *model = dxfg_OrderBookModel_new(thread);
    auto filter = static_cast<dxfg_v4_order_book_model_filter_t>(-1);

    REQUIRE(model != nullptr);
    REQUIRE(dxfg_OrderBookModel_setFilter(thread, model, AGGREGATE) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_OrderBookModel_getFilter(thread, model) == AGGREGATE);
    CHECK(dxfg_v4_OrderBookModel_getFilter(thread, model, &filter) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(filter == AGGREGATE);
    CHECK(dxfg_OrderBookModel_close(thread, model) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &model->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

} // namespace test
} // namespace dxfg
