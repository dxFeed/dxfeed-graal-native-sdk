// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The dxfg_v4_ functions: status results, out parameters, NULL as "no value". This file calls the v4 functions only
// (a deprecated function is a compile error here), parity.cpp compares them with the older ones.

#include <catch.hpp>
#include <dxfg_api.h>

#include "isolate.hpp"
#include "v4_c_api.h"

#include <atomic>
#include <chrono>
#include <map>
#include <mutex>
#include <set>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace test {

namespace {

// Takes the stored exception of the thread and returns its class name, an empty string if no exception is stored.
std::string takeExceptionClassName(graal_isolatethread_t *thread) {
    dxfg_v4_exception_t *exception = nullptr;

    REQUIRE(dxfg_v4_get_and_clear_thread_exception(thread, &exception) == DXFG_EXECUTE_SUCCESSFULLY);

    if (exception == nullptr) {
        return {};
    }

    std::string className = exception->class_name == nullptr ? "<no canonical name>" : exception->class_name;

    REQUIRE(dxfg_v4_Exception_release(thread, exception) == DXFG_EXECUTE_SUCCESSFULLY);

    return className;
}

// Copies the string that a function returned and releases it.
std::string takeString(graal_isolatethread_t *thread, const char *string) {
    REQUIRE(string != nullptr);

    std::string result = string;

    REQUIRE(dxfg_v4_String_release(thread, string) == DXFG_EXECUTE_SUCCESSFULLY);

    return result;
}

} // namespace

TEST_CASE("A C11 translation unit includes dxfg_api.h and calls the v4 functions", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    int32_t seconds = 0;

    REQUIRE(v4CTimePeriodSeconds(thread, "15s", &seconds) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(seconds == 15);
}

TEST_CASE("dxfg_v4_get_and_clear_thread_exception writes NULL when no exception is stored", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    takeExceptionClassName(thread); // An exception that an earlier test left.

    dxfg_v4_exception_t marker{};
    dxfg_v4_exception_t *exception = &marker;

    REQUIRE(dxfg_v4_get_and_clear_thread_exception(thread, &exception) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(exception == nullptr);
}

TEST_CASE("A failed v4 function returns -1 and stores the exception, which is taken once", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_v4_time_period_t marker{};
    dxfg_v4_time_period_t *period = &marker;

    REQUIRE(dxfg_v4_TimePeriod_valueOfString(thread, "not a period", &period) == DXFG_EXECUTE_FAIL);
    CHECK(period == &marker); // The out variable is written only on success.

    dxfg_v4_exception_t *exception = nullptr;

    REQUIRE(dxfg_v4_get_and_clear_thread_exception(thread, &exception) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(exception != nullptr);
    CHECK(exception->class_name != nullptr);
    CHECK(exception->stack_trace != nullptr);
    CHECK(dxfg_v4_Exception_release(thread, exception) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A successful v4 function keeps the exception that an earlier failed one stored", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    takeExceptionClassName(thread); // An exception that an earlier test left.

    dxfg_v4_time_period_t *period = nullptr;

    REQUIRE(dxfg_v4_TimePeriod_valueOfString(thread, "not a period", &period) == DXFG_EXECUTE_FAIL);
    REQUIRE(dxfg_v4_TimePeriod_valueOfString(thread, "1s", &period) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &period->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
}

TEST_CASE("dxfg_v4_Exception_release accepts NULL", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);
    CHECK(dxfg_v4_Exception_release(thread, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A NULL out pointer is an IllegalArgumentException", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    CHECK(dxfg_v4_TimeZone_getDefault(thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    CHECK(dxfg_v4_get_and_clear_thread_exception(thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("dxfg_v4_Object_new and dxfg_v4_JavaObjectHandler_clone write a new handle", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_v4_java_object_handler *object = nullptr;
    dxfg_v4_java_object_handler *clone = nullptr;
    int32_t equal = 0;

    REQUIRE(dxfg_v4_Object_new(thread, &object) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(object != nullptr);
    REQUIRE(dxfg_v4_JavaObjectHandler_clone(thread, object, &clone) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(clone != nullptr);
    CHECK(clone != object);
    CHECK(dxfg_v4_Object_equals(thread, object, clone, &equal) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);

    CHECK(dxfg_v4_Object_new(thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_JavaObjectHandler_clone(thread, object, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, clone) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, object) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A NULL handle is an error, not a value", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    int32_t offset = 0;

    CHECK(dxfg_v4_TimeZone_getRawOffset(thread, nullptr, &offset) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
}

TEST_CASE("A NULL string written to the out variable means no value", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_v4_auth_token_t *token = nullptr;

    REQUIRE(dxfg_v4_AuthToken_createBearerToken(thread, "secret", &token) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(token != nullptr);

    const char marker[] = "marker";
    const char *user = marker;

    CHECK(dxfg_v4_AuthToken_getUser(thread, token, &user) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(user == nullptr);

    const char *scheme = nullptr;

    REQUIRE(dxfg_v4_AuthToken_getScheme(thread, token, &scheme) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeString(thread, scheme) == "Bearer");
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &token->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The values are written to the out variables", "[V4Api]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_v4_time_period_t *fromMillis = nullptr;
    dxfg_v4_time_period_t *fromString = nullptr;

    REQUIRE(dxfg_v4_TimePeriod_valueOf(thread, 1500, &fromMillis) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_TimePeriod_valueOfString(thread, "1.5s", &fromString) == DXFG_EXECUTE_SUCCESSFULLY);

    int64_t time = 0;
    int32_t equal = 0;
    int32_t comparison = 1;
    int32_t hashCode = 0;
    int32_t otherHashCode = 1;

    CHECK(dxfg_v4_TimePeriod_getTime(thread, fromString, &time) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(time == 1500);
    CHECK(dxfg_v4_Object_equals(thread, &fromMillis->handler, &fromString->handler, &equal) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);
    CHECK(dxfg_v4_Comparable_compareTo(thread, &fromMillis->handler, &fromMillis->handler, &comparison) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(comparison == 0);
    // TimePeriod is not Comparable: an error (the older function returns INT32_MIN), not a value.
    CHECK(dxfg_v4_Comparable_compareTo(thread, &fromMillis->handler, &fromString->handler, &comparison) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.ClassCastException");
    CHECK(dxfg_v4_Object_hashCode(thread, &fromMillis->handler, &hashCode) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Object_hashCode(thread, &fromString->handler, &otherHashCode) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(hashCode == otherHashCode);

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &fromMillis->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &fromString->handler) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_time_zone_t *timeZone = nullptr;
    dxfg_v4_time_format_t *format = nullptr;

    REQUIRE(dxfg_v4_TimeZone_getTimeZone(thread, "GMT", &timeZone) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_TimeFormat_getInstance(thread, timeZone, &format) == DXFG_EXECUTE_SUCCESSFULLY);

    int32_t rawOffset = -1;
    const char *id = nullptr;
    const char *formatted = nullptr;
    int64_t parsed = 0;

    CHECK(dxfg_v4_TimeZone_getRawOffset(thread, timeZone, &rawOffset) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(rawOffset == 0);
    REQUIRE(dxfg_v4_TimeZone_getID(thread, timeZone, &id) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeString(thread, id) == "GMT");

    // -1 is an ordinary value of a v4 function: a time one millisecond before the epoch.
    REQUIRE(dxfg_v4_TimeFormat_format(thread, format, -1, &formatted) == DXFG_EXECUTE_SUCCESSFULLY);

    const std::string text = takeString(thread, formatted);

    REQUIRE(dxfg_v4_TimeFormat_parse(thread, format, text.c_str(), &parsed) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(parsed == -1000); // The format has no milliseconds: -1 ms is 1969-12-31 23:59:59.

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &format->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &timeZone->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}


namespace {

constexpr const char *WITH_DATA = "V4-WITH-DATA";
constexpr const char *NOT_SUBSCRIBED = "V4-NOT-SUBSCRIBED";

// A LOCAL_HUB endpoint made with the v4 functions only: a Profile subscription to WITH_DATA and a Profile published for
// it.
struct Hub {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_endpoint_t *endpoint = nullptr;
    dxfg_v4_feed_t *feed = nullptr;
    dxfg_v4_subscription_t *subscription = nullptr;
    bool ready = false;

    Hub() {
        if (thread == nullptr ||
            dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_LOCAL_HUB, &endpoint) != 0 ||
            dxfg_v4_DXEndpoint_getFeed(thread, endpoint, &feed) != 0 ||
            dxfg_v4_DXFeed_createSubscription(thread, feed, DXFG_EVENT_PROFILE, &subscription) != 0) {
            return;
        }

        dxfg_v4_string_symbol_t symbol{{STRING}, WITH_DATA};

        if (dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) != 0) {
            return;
        }

        dxfg_v4_profile_t profile{};

        profile.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
        profile.market_event.event_symbol = WITH_DATA;
        profile.description = "published description";

        dxfg_v4_event_type_t *events[] = {&profile.market_event.event_type};
        dxfg_v4_event_type_list list{1, events};
        dxfg_v4_publisher_t *publisher = nullptr;

        if (dxfg_v4_DXEndpoint_getPublisher(thread, endpoint, &publisher) != 0 ||
            dxfg_v4_DXPublisher_publishEvents(thread, publisher, &list) != 0 ||
            dxfg_v4_JavaObjectHandler_release(thread, &publisher->handler) != 0) {
            return;
        }

        // Waits for the published event to get to the feed.
        for (int i = 0; i < 500 && !ready; ++i) {
            dxfg_v4_event_type_t *last = nullptr;

            if (dxfg_v4_DXFeed_getLastEventIfSubscribed(thread, feed, DXFG_EVENT_PROFILE, &symbol.supper, &last) != 0) {
                return;
            }

            ready = last != nullptr && reinterpret_cast<dxfg_v4_profile_t *>(last)->description != nullptr;
            dxfg_v4_EventType_release(thread, last);

            if (!ready) {
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
            }
        }
    }
};

Hub &hub() {
    static Hub instance{};

    return instance;
}

} // namespace

TEST_CASE("An endpoint is created, connected and closed with the v4 functions", "[V4Api][Endpoint]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_v4_endpoint_t *endpoint = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_FEED, &endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(endpoint != nullptr);

    // The enum out variables are written with the values of the enums.
    auto role = static_cast<dxfg_v4_endpoint_role_t>(-1);
    auto state = static_cast<dxfg_v4_endpoint_state_t>(-1);

    CHECK(dxfg_v4_DXEndpoint_getRole(thread, endpoint, &role) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(role == DXFG_ENDPOINT_ROLE_FEED);
    CHECK(dxfg_v4_DXEndpoint_getState(thread, endpoint, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state == DXFG_ENDPOINT_STATE_NOT_CONNECTED);

    // Nothing listens on the port: the endpoint keeps connecting.
    REQUIRE(dxfg_v4_DXEndpoint_connect(thread, endpoint, "127.0.0.1:1") == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_getState(thread, endpoint, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state == DXFG_ENDPOINT_STATE_CONNECTING);

    REQUIRE(dxfg_v4_DXEndpoint_close(thread, endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_getState(thread, endpoint, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state == DXFG_ENDPOINT_STATE_CLOSED);

    CHECK(dxfg_v4_DXEndpoint_getRole(thread, endpoint, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_DXEndpoint_getState(thread, nullptr, &state) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("dxfg_v4_DXFeed_getLastEventIfSubscribed writes NULL without a subscription", "[V4Api][Endpoint]") {
    REQUIRE(hub().ready);

    graal_isolatethread_t *thread = hub().thread;
    dxfg_v4_string_symbol_t symbol{{STRING}, NOT_SUBSCRIBED};
    dxfg_v4_event_type_t marker{};
    dxfg_v4_event_type_t *event = &marker;

    CHECK(dxfg_v4_DXFeed_getLastEventIfSubscribed(thread, hub().feed, DXFG_EVENT_PROFILE, &symbol.supper, &event) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(event == nullptr);
    CHECK(takeExceptionClassName(thread).empty());

    event = &marker;
    CHECK(dxfg_v4_DXFeed_getLastEventIfSubscribed(thread, nullptr, DXFG_EVENT_PROFILE, &symbol.supper, &event) ==
          DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
    CHECK(event == &marker);
}

TEST_CASE("dxfg_v4_DXFeed_getLastEvent writes a new event", "[V4Api][Endpoint]") {
    REQUIRE(hub().ready);

    graal_isolatethread_t *thread = hub().thread;
    dxfg_v4_profile_t given{};

    given.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
    given.market_event.event_symbol = WITH_DATA;

    dxfg_v4_event_type_t *last = nullptr;

    REQUIRE(dxfg_v4_DXFeed_getLastEvent(thread, hub().feed, &given.market_event.event_type, &last) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(last != nullptr);
    CHECK(last != &given.market_event.event_type);
    CHECK(std::string(reinterpret_cast<dxfg_v4_profile_t *>(last)->description) == "published description");
    CHECK(dxfg_v4_EventType_release(thread, last) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A promise without an exception writes NULL, a cancelled one writes its exception", "[V4Api][Endpoint]") {
    REQUIRE(hub().ready);

    graal_isolatethread_t *thread = hub().thread;
    dxfg_v4_string_symbol_t symbol{{STRING}, NOT_SUBSCRIBED};
    dxfg_v4_promise_event_t *promise = nullptr;

    // Nothing is published for the symbol: the promise is not done.
    REQUIRE(dxfg_v4_DXFeed_getLastEventPromise(thread, hub().feed, DXFG_EVENT_PROFILE, &symbol.supper, &promise) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(promise != nullptr);

    int32_t done = -1;
    dxfg_v4_exception_t marker{};
    dxfg_v4_exception_t *exception = &marker;

    CHECK(dxfg_v4_Promise_isDone(thread, &promise->handler, &done) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(done == 0);
    CHECK(dxfg_v4_Promise_getException(thread, &promise->handler, &exception) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(exception == nullptr);

    // The wait times out and cancels the promise.
    CHECK(dxfg_v4_Promise_awaitWithTimeout(thread, &promise->handler, 10) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.util.concurrent.CancellationException");
    CHECK(dxfg_v4_Promise_isDone(thread, &promise->handler, &done) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(done == 1);
    REQUIRE(dxfg_v4_Promise_getException(thread, &promise->handler, &exception) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(exception != nullptr);
    REQUIRE(exception != &marker);
    CHECK(std::string(exception->class_name) == "java.util.concurrent.CancellationException");
    CHECK(dxfg_v4_Exception_release(thread, exception) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(dxfg_v4_Promise_getException(thread, nullptr, &exception) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &promise->handler.handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("dxfg_v4_promise_list_release releases the promises", "[V4Api][Endpoint]") {
    REQUIRE(hub().ready);

    graal_isolatethread_t *thread = hub().thread;
    dxfg_v4_string_symbol_t first{{STRING}, WITH_DATA};
    dxfg_v4_string_symbol_t second{{STRING}, NOT_SUBSCRIBED};
    dxfg_v4_symbol_t *elements[] = {&first.supper, &second.supper};
    dxfg_v4_symbol_list symbols{2, elements};
    dxfg_v4_promise_list *promises = nullptr;

    REQUIRE(dxfg_v4_DXFeed_getLastEventsPromises(thread, hub().feed, DXFG_EVENT_PROFILE, &symbols, &promises) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(promises != nullptr);
    CHECK(promises->list.size == 2);

    for (int32_t i = 0; i < promises->list.size; ++i) {
        CHECK(dxfg_v4_Promise_cancel(thread, reinterpret_cast<dxfg_v4_promise_t *>(promises->list.elements[i])) ==
              DXFG_EXECUTE_SUCCESSFULLY);
    }

    CHECK(dxfg_v4_promise_list_release(thread, promises) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_promise_list_release(thread, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
}

namespace {

// Returns the string symbols of a list as a set, the order of the symbols of a subscription is not defined.
std::set<std::string> stringSymbols(const dxfg_v4_symbol_list *symbols) {
    std::set<std::string> result{};

    for (int32_t i = 0; i < symbols->size; ++i) {
        REQUIRE(symbols->elements[i]->type == STRING);
        result.insert(reinterpret_cast<const dxfg_v4_string_symbol_t *>(symbols->elements[i])->symbol);
    }

    return result;
}

} // namespace

TEST_CASE("The symbols of a subscription are written as symbol lists", "[V4Api][Endpoint]") {
    REQUIRE(hub().ready);

    graal_isolatethread_t *thread = hub().thread;
    dxfg_v4_subscription_t *subscription = nullptr;

    REQUIRE(dxfg_v4_DXFeed_createSubscription(thread, hub().feed, DXFG_EVENT_QUOTE, &subscription) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_string_symbol_t first{{STRING}, "V4-FIRST"};
    dxfg_v4_string_symbol_t second{{STRING}, "V4-SECOND"};
    dxfg_v4_symbol_t *elements[] = {&first.supper, &second.supper};
    dxfg_v4_symbol_list added{2, elements};

    REQUIRE(dxfg_v4_DXFeedSubscription_addSymbols(thread, subscription, &added) == DXFG_EXECUTE_SUCCESSFULLY);

    const std::set<std::string> expected{"V4-FIRST", "V4-SECOND"};
    dxfg_v4_symbol_list *symbols = nullptr;

    REQUIRE(dxfg_v4_DXFeedSubscription_getSymbols(thread, subscription, &symbols) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(symbols != nullptr);
    CHECK(stringSymbols(symbols) == expected);
    CHECK(dxfg_v4_symbol_list_release(thread, symbols) == DXFG_EXECUTE_SUCCESSFULLY);

    symbols = nullptr;
    REQUIRE(dxfg_v4_DXFeedSubscription_getDecoratedSymbols(thread, subscription, &symbols) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(symbols != nullptr);
    CHECK(stringSymbols(symbols) == expected);
    CHECK(dxfg_v4_symbol_list_release(thread, symbols) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(dxfg_v4_DXFeedSubscription_close(thread, subscription) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

namespace {

// What a listener received: the description of the first Profile of LISTENED. The listener is called on a thread of
// the SDK, the events are valid only during the call.
struct Received {
    std::mutex mutex{};
    std::string description{};
    std::atomic<bool> done{false};
};

constexpr const char *LISTENED = "V4-LISTENED";

void onEvents(graal_isolatethread_t *, dxfg_v4_event_type_list *events, void *userData) {
    auto *received = static_cast<Received *>(userData);

    for (int32_t i = 0; i < events->size && !received->done; ++i) {
        auto *profile = reinterpret_cast<dxfg_v4_profile_t *>(events->elements[i]);

        if (profile->market_event.event_type.clazz == DXFG_EVENT_PROFILE && profile->description != nullptr &&
            std::string(profile->market_event.event_symbol) == LISTENED) {
            std::lock_guard<std::mutex> lock(received->mutex);

            received->description = profile->description;
            received->done = true;
        }
    }
}

} // namespace

TEST_CASE("A listener made with the v4 functions receives a published event", "[V4Api][Endpoint]") {
    REQUIRE(hub().ready);

    graal_isolatethread_t *thread = hub().thread;
    // Static: a call of the listener that is still running when the test ends must not see a destroyed object.
    static Received received{};
    dxfg_v4_subscription_t *subscription = nullptr;
    dxfg_v4_feed_event_listener_t *listener = nullptr;

    REQUIRE(dxfg_v4_DXFeed_createSubscription(thread, hub().feed, DXFG_EVENT_PROFILE, &subscription) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedEventListener_new(thread, &onEvents, &received, &listener) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(listener != nullptr);
    REQUIRE(dxfg_v4_DXFeedSubscription_addEventListener(thread, subscription, listener) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_string_symbol_t symbol{{STRING}, LISTENED};

    REQUIRE(dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_profile_t profile{};

    profile.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
    profile.market_event.event_symbol = LISTENED;
    profile.description = "listened description";

    dxfg_v4_event_type_t *events[] = {&profile.market_event.event_type};
    dxfg_v4_event_type_list list{1, events};
    dxfg_v4_publisher_t *publisher = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_getPublisher(thread, hub().endpoint, &publisher) == DXFG_EXECUTE_SUCCESSFULLY);

    // The subscription reaches the publisher asynchronously: the event is published until the listener gets it.
    for (int i = 0; i < 500 && !received.done; ++i) {
        REQUIRE(dxfg_v4_DXPublisher_publishEvents(thread, publisher, &list) == DXFG_EXECUTE_SUCCESSFULLY);
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    REQUIRE(received.done);

    {
        std::lock_guard<std::mutex> lock(received.mutex);

        CHECK(received.description == "listened description");
    }

    CHECK(dxfg_v4_DXFeedSubscription_removeEventListener(thread, subscription, listener) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXFeedSubscription_close(thread, subscription) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &publisher->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

namespace {

// The symbols of src/test/resources/data/ipf.txt, in the order of the file.
const std::vector<std::string> IPF_SYMBOLS{"GOOG", "AAPL", "AB", "AA", "A", "AC", "B", "BB", "BC", "Q"};

std::vector<std::string> dataSymbols(const dxfg_v4_instrument_profile_data_list *profiles) {
    std::vector<std::string> result{};

    for (int32_t i = 0; i < profiles->size; ++i) {
        result.emplace_back(profiles->elements[i]->symbol);
    }

    return result;
}

} // namespace

TEST_CASE("Instrument profiles are read from a file as structures and as handles", "[V4Api][Ipf]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_instrument_profile_reader_t *reader = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_InstrumentProfileReader_new(thread, &reader) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_instrument_profile_data_list *profiles = nullptr;
    int32_t complete = 0;

    REQUIRE(dxfg_v4_InstrumentProfileReader_readFromFile(thread, reader, V4_API_TEST_IPF_FILE, &profiles) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(profiles != nullptr);
    CHECK(dataSymbols(profiles) == IPF_SYMBOLS);
    CHECK(dxfg_v4_InstrumentProfileReader_wasComplete(thread, reader, &complete) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(complete == 1);
    CHECK(dxfg_v4_instrument_profile_data_list_release(thread, profiles) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_instrument_profile_list *handles = nullptr;

    REQUIRE(dxfg_v4_InstrumentProfileReader_readFromFileAsHandles(thread, reader, V4_API_TEST_IPF_FILE, &handles) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(handles != nullptr);

    std::vector<std::string> symbols{};

    for (int32_t i = 0; i < handles->size; ++i) {
        const char *symbol = nullptr;

        REQUIRE(dxfg_v4_InstrumentProfile_getSymbol(thread, handles->elements[i], &symbol) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        symbols.push_back(takeString(thread, symbol));
    }

    CHECK(symbols == IPF_SYMBOLS);
    CHECK(dxfg_v4_instrument_profile_list_release(thread, handles) == DXFG_EXECUTE_SUCCESSFULLY);

    // A missing file is an error, the out variable is not written.
    profiles = nullptr;
    CHECK(dxfg_v4_InstrumentProfileReader_readFromFile(thread, reader, "v4-missing.ipf", &profiles) ==
          DXFG_EXECUTE_FAIL);
    CHECK(profiles == nullptr);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &reader->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

// The symbols in the view of the collector.
std::set<std::string> viewedSymbols(graal_isolatethread_t *thread, dxfg_v4_ipf_collector_t *collector) {
    dxfg_v4_iterable_ip_t *view = nullptr;
    std::set<std::string> viewed{};

    REQUIRE(dxfg_v4_InstrumentProfileCollector_view(thread, collector, &view) == DXFG_EXECUTE_SUCCESSFULLY);

    for (;;) {
        int32_t hasNext = 0;
        dxfg_v4_instrument_profile_data_t *profile = nullptr;

        REQUIRE(dxfg_v4_Iterable_InstrumentProfile_hasNext(thread, view, &hasNext) == DXFG_EXECUTE_SUCCESSFULLY);

        if (hasNext == 0) {
            break;
        }

        REQUIRE(dxfg_v4_Iterable_InstrumentProfile_next(thread, view, &profile) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(profile != nullptr);
        viewed.insert(profile->symbol);
        CHECK(dxfg_v4_instrument_profile_data_release(thread, profile) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &view->handler) == DXFG_EXECUTE_SUCCESSFULLY);

    return viewed;
}

TEST_CASE("A collector takes a list of instrument profiles, a list with a NULL element changes nothing",
          "[V4Api][Ipf]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_instrument_profile_reader_t *reader = nullptr;
    dxfg_v4_instrument_profile_data_list *profiles = nullptr;
    dxfg_v4_instrument_profile_list *handles = nullptr;
    dxfg_v4_ipf_collector_t *collector = nullptr;
    int64_t lastUpdateTime = -1;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_InstrumentProfileReader_new(thread, &reader) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileReader_readFromFile(thread, reader, V4_API_TEST_IPF_FILE, &profiles) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(profiles != nullptr);
    REQUIRE(profiles->size > 0);
    REQUIRE(dxfg_v4_InstrumentProfileReader_readFromFileAsHandles(thread, reader, V4_API_TEST_IPF_FILE, &handles) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(handles != nullptr);
    REQUIRE(handles->size > 0);
    REQUIRE(dxfg_v4_InstrumentProfileCollector_new(thread, &collector) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileCollector_getLastUpdateTime(thread, collector, &lastUpdateTime) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    // The first element is a new profile: the collector would store it before it reaches the NULL element.
    dxfg_v4_instrument_profile_data_t *elements[] = {profiles->elements[0], nullptr};
    dxfg_v4_instrument_profile_data_list withNull{2, elements};
    dxfg_v4_instrument_profile_data_list noElements{1, nullptr};
    dxfg_v4_instrument_profile_t *handleElements[] = {handles->elements[0], nullptr};
    dxfg_v4_instrument_profile_list handlesWithNull{2, handleElements};

    CHECK(dxfg_v4_InstrumentProfileCollector_updateInstrumentProfiles(thread, collector, &withNull, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_InstrumentProfileCollector_updateInstrumentProfiles(thread, collector, &noElements, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_InstrumentProfileCollector_updateInstrumentProfileHandles(thread, collector, &handlesWithNull,
                                                                            nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(viewedSymbols(thread, collector).empty());

    int64_t unchangedTime = -1;

    REQUIRE(dxfg_v4_InstrumentProfileCollector_getLastUpdateTime(thread, collector, &unchangedTime) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(unchangedTime == lastUpdateTime);

    REQUIRE(dxfg_v4_InstrumentProfileCollector_updateInstrumentProfiles(thread, collector, profiles, nullptr) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(viewedSymbols(thread, collector) == std::set<std::string>(IPF_SYMBOLS.begin(), IPF_SYMBOLS.end()));

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &collector->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_instrument_profile_list_release(thread, handles) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_instrument_profile_data_list_release(thread, profiles) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &reader->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("Custom fields are made from a list of names and values, an empty list may have NULL elements",
          "[V4Api][Ipf]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_instrument_profile_custom_fields_t *customFields = nullptr;
    dxfg_v4_string_list *names = nullptr;

    REQUIRE(thread != nullptr);

    dxfg_v4_string_list empty{0, nullptr};

    REQUIRE(dxfg_v4_InstrumentProfileCustomFields_newWithNamesAndValues(thread, &empty, &customFields) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(customFields != nullptr);
    REQUIRE(dxfg_v4_InstrumentProfileCustomFields_getNonEmptyFieldNames(thread, customFields, &names) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(names != nullptr);
    CHECK(names->size == 0);
    CHECK(dxfg_v4_string_list_release(thread, names) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &customFields->handler) == DXFG_EXECUTE_SUCCESSFULLY);

    const char *namesAndValues[] = {"V4_FIRST", "1", "V4_SECOND", "2"};
    dxfg_v4_string_list list{4, namesAndValues};
    char *value = nullptr;

    customFields = nullptr;
    REQUIRE(dxfg_v4_InstrumentProfileCustomFields_newWithNamesAndValues(thread, &list, &customFields) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(customFields != nullptr);
    REQUIRE(dxfg_v4_InstrumentProfileCustomFields_getField(thread, customFields, "V4_SECOND", &value) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeString(thread, value) == "2");
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &customFields->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The state of an instrument profile connection is written as an enum value", "[V4Api][Ipf]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_ipf_collector_t *collector = nullptr;
    dxfg_v4_ipf_connection_t *connection = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_InstrumentProfileCollector_new(thread, &collector) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileConnection_createConnection(thread, V4_API_TEST_IPF_FILE, collector,
                                                                 &connection) == DXFG_EXECUTE_SUCCESSFULLY);

    auto state = static_cast<dxfg_v4_ipf_connection_state_t>(-1);

    REQUIRE(dxfg_v4_InstrumentProfileConnection_getState(thread, connection, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state == DXFG_IPF_CONNECTION_STATE_NOT_CONNECTED);
    CHECK(dxfg_v4_InstrumentProfileConnection_close(thread, connection) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileConnection_getState(thread, connection, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state == DXFG_IPF_CONNECTION_STATE_CLOSED);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &connection->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &collector->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

namespace {

// Sessions in New York: pre-market 04:00-09:30, regular 09:30-16:00, after-market 16:00-20:00 on every day.
constexpr const char *SCHEDULE_DEFINITION = "(tz=America/New_York;0=p04000930r09301600a16002000)";

} // namespace

TEST_CASE("The days and the sessions of a schedule are written to the out variables", "[V4Api][Schedule]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_schedule_t *schedule = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_Schedule_getInstanceByDefinition(thread, SCHEDULE_DEFINITION, &schedule) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(schedule != nullptr);

    // A time before the epoch is a value, not an error: -1 is in the after-market session of 1969-12-31, which starts
    // at 16:00 EST (21:00 UTC).
    dxfg_v4_session_t *session = nullptr;
    auto type = static_cast<dxfg_v4_session_type_t>(-1);
    int64_t startTime = 0;

    REQUIRE(dxfg_v4_Schedule_getSessionByTime(thread, schedule, -1, &session) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(session != nullptr);
    CHECK(dxfg_v4_Session_getType(thread, session, &type) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(type == DXFG_SESSION_TYPE_AFTER_MARKET);
    CHECK(dxfg_v4_Session_getStartTime(thread, session, &startTime) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(startTime == -3 * 3600 * 1000);

    dxfg_v4_day_t *day = nullptr;
    int32_t yearMonthDay = 0;

    REQUIRE(dxfg_v4_Session_getDay(thread, session, &day) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(day != nullptr);
    CHECK(dxfg_v4_Day_getYearMonthDay(thread, day, &yearMonthDay) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(yearMonthDay == 19691231);

    // A NULL out pointer is an error.
    CHECK(dxfg_v4_Day_getDayId(thread, day, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // The day has one session of each kind and two non-trading ones (before 04:00 and after 20:00).
    dxfg_v4_session_list *sessions = nullptr;
    std::vector<dxfg_v4_session_type_t> types{};

    REQUIRE(dxfg_v4_Day_getSessions(thread, day, &sessions) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(sessions != nullptr);

    for (int32_t i = 0; i < sessions->size; ++i) {
        REQUIRE(dxfg_v4_Session_getType(thread, sessions->elements[i], &type) == DXFG_EXECUTE_SUCCESSFULLY);
        types.push_back(type);
    }

    CHECK(types == std::vector<dxfg_v4_session_type_t>{DXFG_SESSION_TYPE_NO_TRADING, DXFG_SESSION_TYPE_PRE_MARKET,
                                                       DXFG_SESSION_TYPE_REGULAR, DXFG_SESSION_TYPE_AFTER_MARKET,
                                                       DXFG_SESSION_TYPE_NO_TRADING});
    CHECK(dxfg_v4_session_list_release(thread, sessions) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_session_list_release(thread, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Day_getSessions(thread, day, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // The wrapper release frees the list only: the sessions stay usable and are released one by one.
    sessions = nullptr;
    REQUIRE(dxfg_v4_Day_getSessions(thread, day, &sessions) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(sessions != nullptr);

    std::vector<dxfg_v4_session_t *> kept(sessions->elements, sessions->elements + sessions->size);

    CHECK(dxfg_v4_session_list_wrapper_release(thread, sessions) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(kept.size() == 5);
    CHECK(dxfg_v4_Session_getType(thread, kept[2], &type) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(type == DXFG_SESSION_TYPE_REGULAR);

    for (dxfg_v4_session_t *keptSession : kept) {
        CHECK(dxfg_v4_Session_release(thread, keptSession) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    // A day that is not found is written as NULL, and the function succeeds.
    dxfg_v4_day_filter_t *holiday = nullptr;
    dxfg_v4_day_t *nextHoliday = day; // Any value that the function overwrites.

    REQUIRE(dxfg_v4_DayFilter_getInstance(thread, DXFG_DAY_FILTER_HOLIDAY, &holiday) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Day_findNextDay(thread, day, holiday, &nextHoliday) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(nextHoliday == nullptr);
    CHECK(takeExceptionClassName(thread).empty());

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &holiday->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Day_release(thread, day) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Session_release(thread, session) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &schedule->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("Price increments are written as a list of doubles and made from such a list", "[V4Api][Glossary]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_price_increments_t *priceIncrements = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_PriceIncrements_valueOf(thread, "0.0001 1; 0.01", &priceIncrements) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_double_list *increments = nullptr;

    REQUIRE(dxfg_v4_PriceIncrements_getPriceIncrements(thread, priceIncrements, &increments) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(increments != nullptr);
    CHECK(std::vector<double>(increments->elements, increments->elements + increments->size) ==
          std::vector<double>{0.0001, 1, 0.01});

    dxfg_v4_price_increments_t *copy = nullptr;
    char *text = nullptr;
    char *copyText = nullptr;

    REQUIRE(dxfg_v4_PriceIncrements_valueOfIncrements(thread, increments, &copy) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_PriceIncrements_getText(thread, priceIncrements, &text) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_PriceIncrements_getText(thread, copy, &copyText) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeString(thread, copyText) == takeString(thread, text));
    CHECK(dxfg_v4_double_list_release(thread, increments) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_double_list_release(thread, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);

    // A list of a positive size without elements is an error, the out variable is not written.
    const dxfg_v4_double_list noElements{2, nullptr};
    dxfg_v4_price_increments_t *notWritten = nullptr;

    CHECK(dxfg_v4_PriceIncrements_valueOfIncrements(thread, &noElements, &notWritten) == DXFG_EXECUTE_FAIL);
    CHECK(notWritten == nullptr);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // A NULL out pointer is an error; so is a NULL handle, which fails in the older function: the list is not written.
    dxfg_v4_double_list *notWrittenList = nullptr;

    CHECK(dxfg_v4_PriceIncrements_getPriceIncrements(thread, priceIncrements, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_PriceIncrements_getPriceIncrements(thread, nullptr, &notWrittenList) == DXFG_EXECUTE_FAIL);
    CHECK(notWrittenList == nullptr);
    CHECK_FALSE(takeExceptionClassName(thread).empty());

    // A NULL list and an empty list without elements make EMPTY.
    const dxfg_v4_double_list emptyList{0, nullptr};

    for (const dxfg_v4_double_list *list : {static_cast<const dxfg_v4_double_list *>(nullptr), &emptyList}) {
        dxfg_v4_price_increments_t *made = nullptr;
        char *madeText = nullptr;

        REQUIRE(dxfg_v4_PriceIncrements_valueOfIncrements(thread, list, &made) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(made != nullptr);
        REQUIRE(dxfg_v4_PriceIncrements_getText(thread, made, &madeText) == DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(takeString(thread, madeText).empty());
        CHECK(dxfg_v4_JavaObjectHandler_release(thread, &made->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &copy->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &priceIncrements->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("Additional underlyings are written as a list of map entries and made from such a list",
          "[V4Api][Glossary]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_additional_underlyings_t *underlyings = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_AdditionalUnderlyings_valueOf(thread, "SE 50; FIS 53.875", &underlyings) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_string_to_double_map_entry_list *map = nullptr;
    std::map<std::string, double> entries{};

    REQUIRE(dxfg_v4_AdditionalUnderlyings_getMap(thread, underlyings, &map) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(map != nullptr);

    for (int32_t i = 0; i < map->size; ++i) {
        entries[map->elements[i].key] = map->elements[i].value;
    }

    CHECK(entries == std::map<std::string, double>{{"FIS", 53.875}, {"SE", 50}});

    // The copy has the same entries; its text lists them in the order of the keys.
    dxfg_v4_additional_underlyings_t *copy = nullptr;
    char *copyText = nullptr;
    double spc = 0;

    REQUIRE(dxfg_v4_AdditionalUnderlyings_valueOfMap(thread, map, &copy) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_AdditionalUnderlyings_getText(thread, copy, &copyText) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeString(thread, copyText) == "FIS 53.875; SE 50");
    CHECK(dxfg_v4_AdditionalUnderlyings_getSPC(thread, copy, "SE", &spc) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(spc == 50);
    CHECK(dxfg_v4_string_to_double_map_entry_list_release(thread, map) == DXFG_EXECUTE_SUCCESSFULLY);

    // The map of EMPTY is an empty list.
    dxfg_v4_additional_underlyings_t *empty = nullptr;

    REQUIRE(dxfg_v4_AdditionalUnderlyings_EMPTY(thread, &empty) == DXFG_EXECUTE_SUCCESSFULLY);
    map = nullptr;
    REQUIRE(dxfg_v4_AdditionalUnderlyings_getMap(thread, empty, &map) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(map != nullptr);
    CHECK(map->size == 0);
    CHECK(dxfg_v4_string_to_double_map_entry_list_release(thread, map) == DXFG_EXECUTE_SUCCESSFULLY);

    // A list of a positive size without elements is an error, the out variable is not written.
    const dxfg_v4_string_to_double_map_entry_list noElements{1, nullptr};
    dxfg_v4_additional_underlyings_t *notWritten = nullptr;

    CHECK(dxfg_v4_AdditionalUnderlyings_valueOfMap(thread, &noElements, &notWritten) == DXFG_EXECUTE_FAIL);
    CHECK(notWritten == nullptr);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // A NULL out pointer is an error; so is a NULL handle, which fails in the older function: the list is not written.
    map = nullptr;
    CHECK(dxfg_v4_AdditionalUnderlyings_getMap(thread, underlyings, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_AdditionalUnderlyings_getMap(thread, nullptr, &map) == DXFG_EXECUTE_FAIL);
    CHECK(map == nullptr);
    CHECK_FALSE(takeExceptionClassName(thread).empty());

    // A NULL list and an empty list without elements make an object without entries.
    const dxfg_v4_string_to_double_map_entry_list emptyList{0, nullptr};

    for (const dxfg_v4_string_to_double_map_entry_list *list :
         {static_cast<const dxfg_v4_string_to_double_map_entry_list *>(nullptr), &emptyList}) {
        dxfg_v4_additional_underlyings_t *made = nullptr;
        char *madeText = nullptr;

        REQUIRE(dxfg_v4_AdditionalUnderlyings_valueOfMap(thread, list, &made) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(made != nullptr);
        REQUIRE(dxfg_v4_AdditionalUnderlyings_getText(thread, made, &madeText) == DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(takeString(thread, madeText).empty());
        CHECK(dxfg_v4_JavaObjectHandler_release(thread, &made->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &empty->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &copy->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &underlyings->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The values of a CFI code and of its attributes are written as lists of handles", "[V4Api][Glossary]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_cfi_t *cfi = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_CFI_valueOf(thread, "ESVUFR", &cfi) == DXFG_EXECUTE_SUCCESSFULLY);

    // A value for each of the 6 characters of the code.
    dxfg_v4_cfi_value_list *values = nullptr;

    REQUIRE(dxfg_v4_CFI_decipher(thread, cfi, &values) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(values != nullptr);
    REQUIRE(values->size == 6);

    dxfg_v4_cfi_attribute_t *attribute = nullptr;
    dxfg_v4_cfi_value_list *attributeValues = nullptr;

    REQUIRE(dxfg_v4_CFI_Value_getAttribute(thread, values->elements[2], &attribute) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(attribute != nullptr);
    REQUIRE(dxfg_v4_CFI_Attribute_getValues(thread, attribute, &attributeValues) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(attributeValues != nullptr);
    CHECK(attributeValues->size > 0);

    // A NULL out pointer is an error.
    CHECK(dxfg_v4_CFI_decipher(thread, cfi, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_CFI_Attribute_getValues(thread, attribute, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    CHECK(dxfg_v4_cfi_value_list_release(thread, attributeValues) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_cfi_value_list_release(thread, values) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_cfi_value_list_release(thread, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &attribute->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &cfi->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A system property is written to the out variable, a missing one as NULL", "[V4Api][System]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_system_set_property(thread, "dxfg.v4.test.property", "v4 value") == DXFG_EXECUTE_SUCCESSFULLY);

    const char *value = nullptr;

    REQUIRE(dxfg_v4_system_get_property(thread, "dxfg.v4.test.property", &value) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(value != nullptr);
    CHECK(std::string(value) == "v4 value");
    CHECK(dxfg_v4_system_release_property(thread, value) == DXFG_EXECUTE_SUCCESSFULLY);

    // A missing property is "no value": NULL, and the function succeeds.
    const char *missing = "any value that the function overwrites";

    CHECK(dxfg_v4_system_get_property(thread, "dxfg.v4.test.missing.property", &missing) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(missing == nullptr);
    CHECK(takeExceptionClassName(thread).empty());

    // A NULL out pointer is an error.
    CHECK(dxfg_v4_system_get_property(thread, "dxfg.v4.test.property", nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("dxfg_v4_Tools_parseSymbols writes a list of strings", "[V4Api][Qds]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_string_list *symbols = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_Tools_parseSymbols(thread, "AAPL,IBM", &symbols) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(symbols != nullptr);
    REQUIRE(symbols->size == 2);
    CHECK(std::set<std::string>{symbols->elements[0], symbols->elements[1]} == std::set<std::string>{"AAPL", "IBM"});
    CHECK(dxfg_v4_string_list_release(thread, symbols) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(dxfg_v4_Tools_parseSymbols(thread, "AAPL", nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("The filter of an order book model is written as an enum value", "[V4Api][Model]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_order_book_model_t *model = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_OrderBookModel_new(thread, &model) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(model != nullptr);

    auto filter = static_cast<dxfg_v4_order_book_model_filter_t>(-1);

    REQUIRE(dxfg_v4_OrderBookModel_setFilter(thread, model, REGIONAL) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_OrderBookModel_getFilter(thread, model, &filter) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(filter == REGIONAL);

    CHECK(dxfg_v4_OrderBookModel_getFilter(thread, model, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    dxfg_v4_observable_list_model_t *buyOrders = nullptr;
    dxfg_v4_event_type_list *orders = nullptr;

    REQUIRE(dxfg_v4_OrderBookModel_getBuyOrders(thread, model, &buyOrders) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(buyOrders != nullptr);
    REQUIRE(dxfg_v4_ObservableListModel_toArray(thread, buyOrders, &orders) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(orders != nullptr);
    CHECK(orders->size == 0);

    CHECK(dxfg_v4_event_type_list_release(thread, orders) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &buyOrders->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_OrderBookModel_close(thread, model) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &model->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

namespace {

constexpr const char *TX_MODEL_SYMBOL = "V4-TX-MODEL";

// The calls of a tx model listener: whether each call was a snapshot and the sizes of its orders.
struct TxCalls {
    std::mutex mutex;
    std::vector<std::pair<bool, std::vector<double>>> calls;

    std::size_t count() {
        std::lock_guard<std::mutex> lock(mutex);

        return calls.size();
    }
};

void onTxEvents(graal_isolatethread_t * /* thread */, dxfg_v4_indexed_event_source_t * /* source */,
                dxfg_v4_event_type_list *events, int32_t isSnapshot, void *userData) {
    std::vector<double> sizes{};

    for (int32_t i = 0; events != nullptr && i < events->size; ++i) {
        sizes.push_back(reinterpret_cast<dxfg_v4_order_t *>(events->elements[i])->order_base.size);
    }

    auto *calls = static_cast<TxCalls *>(userData);
    std::lock_guard<std::mutex> lock(calls->mutex);

    calls->calls.emplace_back(isSnapshot != 0, sizes);
}

dxfg_v4_order_t txOrder(int32_t sourceId, int64_t index, double size, int32_t eventFlags) {
    dxfg_v4_order_t order{};

    order.order_base.market_event.event_type.clazz = DXFG_EVENT_ORDER;
    order.order_base.market_event.event_symbol = TX_MODEL_SYMBOL;
    order.order_base.event_flags = eventFlags;
    order.order_base.index = (static_cast<int64_t>(sourceId) << 32) | index;
    order.order_base.size = size;
    order.order_base.price = 10.0 + static_cast<double>(index);
    order.order_base.flags = 1 << 2; // Side.BUY: only empty orders may have an undefined side.

    return order;
}

// Releases the builder handle that a builder function wrote (each call writes a new handle of the same builder). The
// variable is passed by its address: the status argument is the call that writes it.
void releaseBuilder(graal_isolatethread_t *thread, int32_t status, dxfg_v4_indexed_tx_model_builder_t **builder) {
    REQUIRE(status == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(*builder != nullptr);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &(*builder)->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    *builder = nullptr;
}

} // namespace

TEST_CASE("An indexed tx model built with the v4 functions receives a published snapshot", "[V4Api][Model]") {
    REQUIRE(hub().ready);

    graal_isolatethread_t *thread = hub().thread;
    // Static: a call of the listener that is still running when the test ends must not see a destroyed object.
    static TxCalls calls{};
    dxfg_v4_indexed_event_source_t *source = nullptr;
    dxfg_v4_tx_model_listener_t *listener = nullptr;
    dxfg_v4_indexed_tx_model_builder_t *builder = nullptr;
    dxfg_v4_indexed_tx_model_builder_t *written = nullptr;

    REQUIRE(dxfg_v4_IndexedEventSource_new(thread, "ntv", &source) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_TxModelListener_new(thread, &onTxEvents, &calls, &listener) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_IndexedTxModel_newBuilder(thread, DXFG_EVENT_ORDER, &builder) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(builder != nullptr);

    dxfg_v4_indexed_event_source_t *elements[] = {source};
    dxfg_v4_indexed_event_source_list sources{1, elements};
    dxfg_v4_string_symbol_t symbol{{STRING}, TX_MODEL_SYMBOL};

    releaseBuilder(thread, dxfg_v4_IndexedTxModel_Builder_withSources(thread, builder, &sources, &written), &written);
    releaseBuilder(thread, dxfg_v4_IndexedTxModel_Builder_withBatchProcessing(thread, builder, 0, &written), &written);
    releaseBuilder(thread, dxfg_v4_IndexedTxModel_Builder_withSnapshotProcessing(thread, builder, 1, &written),
                   &written);
    releaseBuilder(thread, dxfg_v4_IndexedTxModel_Builder_withFeed(thread, builder, hub().feed, &written), &written);
    releaseBuilder(thread, dxfg_v4_IndexedTxModel_Builder_withSymbol(thread, builder, &symbol.supper, &written),
                   &written);
    releaseBuilder(thread, dxfg_v4_IndexedTxModel_Builder_withListener(thread, builder, listener, &written), &written);

    dxfg_v4_indexed_tx_model_t *model = nullptr;

    REQUIRE(dxfg_v4_IndexedTxModel_Builder_build(thread, builder, &model) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(model != nullptr);

    int32_t batchProcessing = -1;
    int32_t snapshotProcessing = -1;
    dxfg_v4_indexed_event_source_list *modelSources = nullptr;

    CHECK(dxfg_v4_IndexedTxModel_isBatchProcessing(thread, model, &batchProcessing) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(batchProcessing == 0);
    CHECK(dxfg_v4_IndexedTxModel_isSnapshotProcessing(thread, model, &snapshotProcessing) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(snapshotProcessing == 1);
    REQUIRE(dxfg_v4_IndexedTxModel_getSources(thread, model, &modelSources) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(modelSources != nullptr);
    REQUIRE(modelSources->size == 1);
    CHECK(std::string(modelSources->elements[0]->name) == "ntv");
    CHECK(dxfg_v4_indexed_event_source_list_release(thread, modelSources) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(dxfg_v4_IndexedTxModel_isBatchProcessing(thread, model, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // The snapshot of three orders comes in one call (QD requires the snapshot to end with index 0). The subscription
    // reaches the publisher asynchronously: the snapshot is published until the listener gets it.
    constexpr int32_t SNAPSHOT_BEGIN = 0x04;
    constexpr int32_t SNAPSHOT_END = 0x08;
    dxfg_v4_order_t snapshot[] = {txOrder(source->id, 2, 30.0, SNAPSHOT_BEGIN), txOrder(source->id, 1, 20.0, 0),
                                  txOrder(source->id, 0, 10.0, SNAPSHOT_END)};
    dxfg_v4_event_type_t *events[] = {&snapshot[0].order_base.market_event.event_type,
                                      &snapshot[1].order_base.market_event.event_type,
                                      &snapshot[2].order_base.market_event.event_type};
    dxfg_v4_event_type_list list{3, events};
    dxfg_v4_publisher_t *publisher = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_getPublisher(thread, hub().endpoint, &publisher) == DXFG_EXECUTE_SUCCESSFULLY);

    for (int i = 0; i < 500 && calls.count() == 0; ++i) {
        REQUIRE(dxfg_v4_DXPublisher_publishEvents(thread, publisher, &list) == DXFG_EXECUTE_SUCCESSFULLY);
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    REQUIRE(calls.count() > 0);

    {
        std::lock_guard<std::mutex> lock(calls.mutex);
        std::vector<double> sizes = calls.calls[0].second;

        CHECK(calls.calls[0].first);
        CHECK(std::multiset<double>(sizes.begin(), sizes.end()) == std::multiset<double>{10.0, 20.0, 30.0});
    }

    CHECK(dxfg_v4_IndexedTxModel_close(thread, model) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &model->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &builder->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &publisher->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_IndexedEventSource_release(thread, source) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The on-demand service of an endpoint writes its state to the out variables", "[V4Api][OnDemand]") {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_v4_endpoint_t *endpoint = nullptr;
    dxfg_v4_on_demand_service_t *service = nullptr;

    REQUIRE(thread != nullptr);
    REQUIRE(dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED, &endpoint) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_OnDemandService_getInstanceByEndpoint(thread, endpoint, &service) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(service != nullptr);

    // Not connected: no replay, and the speed is 0 (the real-time data).
    int32_t replaySupported = -1;
    int32_t replay = -1;
    double speed = -1.0;
    dxfg_v4_endpoint_t *serviceEndpoint = nullptr;
    int32_t equal = 0;

    CHECK(dxfg_v4_OnDemandService_isReplaySupported(thread, service, &replaySupported) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(replaySupported == 0);
    CHECK(dxfg_v4_OnDemandService_isReplay(thread, service, &replay) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(replay == 0);
    CHECK(dxfg_v4_OnDemandService_getSpeed(thread, service, &speed) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(speed == 0.0);
    REQUIRE(dxfg_v4_OnDemandService_getEndpoint(thread, service, &serviceEndpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(serviceEndpoint != nullptr);
    CHECK(dxfg_v4_Object_equals(thread, &endpoint->handler, &serviceEndpoint->handler, &equal) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);

    CHECK(dxfg_v4_OnDemandService_getSpeed(thread, service, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &serviceEndpoint->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &service->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_close(thread, endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The symbol of an event model is written to the out variable, a missing one as NULL", "[V4Api][EventModel]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_v4_string_symbol_t marker{{STRING}, "marker"};
    dxfg_v4_indexed_event_model_t *indexedModel = nullptr;
    dxfg_v4_symbol_t *symbol = &marker.supper;

    REQUIRE(dxfg_v4_IndexedEventModel_new(thread, DXFG_EVENT_ORDER, &indexedModel) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(indexedModel != nullptr);

    // No symbol is set: NULL is written and the call succeeds.
    CHECK(dxfg_v4_IndexedEventModel_getSymbol(thread, indexedModel, &symbol) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(symbol == nullptr);

    dxfg_v4_string_symbol_t aapl{{STRING}, "AAPL"};

    REQUIRE(dxfg_v4_IndexedEventModel_setSymbol(thread, indexedModel, &aapl.supper) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_IndexedEventModel_getSymbol(thread, indexedModel, &symbol) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(symbol != nullptr);
    REQUIRE(symbol->type == STRING);
    CHECK(std::string(reinterpret_cast<const dxfg_v4_string_symbol_t *>(symbol)->symbol) == "AAPL");
    CHECK(dxfg_v4_Symbol_release(thread, symbol) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_IndexedEventModel_close(thread, indexedModel) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &indexedModel->handler) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_time_series_event_model_t *timeSeriesModel = nullptr;

    symbol = &marker.supper;
    REQUIRE(dxfg_v4_TimeSeriesEventModel_new(thread, DXFG_EVENT_CANDLE, &timeSeriesModel) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(timeSeriesModel != nullptr);
    CHECK(dxfg_v4_TimeSeriesEventModel_getSymbol(thread, timeSeriesModel, &symbol) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(symbol == nullptr);
    CHECK(dxfg_v4_TimeSeriesEventModel_close(thread, timeSeriesModel) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &timeSeriesModel->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A failed call of a model or a service leaves the out variable unchanged", "[V4Api][EventModel]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    // A NULL handle fails in the older function, after the twin has checked the out pointer.
    dxfg_v4_string_symbol_t marker{{STRING}, "marker"};
    dxfg_v4_symbol_t *symbol = &marker.supper;

    CHECK(dxfg_v4_IndexedEventModel_getSymbol(thread, nullptr, &symbol) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
    CHECK(symbol == &marker.supper);

    auto filter = static_cast<dxfg_v4_order_book_model_filter_t>(-1);

    CHECK(dxfg_v4_OrderBookModel_getFilter(thread, nullptr, &filter) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
    CHECK(filter == static_cast<dxfg_v4_order_book_model_filter_t>(-1));

    double speed = 42.0;

    CHECK(dxfg_v4_OnDemandService_getSpeed(thread, nullptr, &speed) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
    CHECK(speed == 42.0);
}

} // namespace test
} // namespace dxfg
