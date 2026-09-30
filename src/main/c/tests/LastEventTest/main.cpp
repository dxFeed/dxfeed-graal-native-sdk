// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// dxfg_DXFeed_getLastEvent2 and dxfg_DXFeed_getLastEvents2: the given events are only read, the results are new events
// with the last events, or copies of the given events when the last events are not available (as in Java).
//
// The memory checks need the heap statistics of glibc (mallinfo2), so they run on Linux only.

#include <catch.hpp>
#include <dxfg_api.h>

#include <chrono>
#include <cstring>
#include <string>
#include <thread>

#if defined(__GLIBC__)
#include <malloc.h>
#endif

namespace dxfg {
namespace test {

namespace {

constexpr const char *WITH_DATA = "SUBSCRIBED-WITH-DATA";
constexpr const char *NO_DATA = "SUBSCRIBED-NO-DATA";
constexpr const char *NOT_SUBSCRIBED = "NOT-SUBSCRIBED";

// A LOCAL_HUB endpoint with a Profile subscription to WITH_DATA and NO_DATA, and a Profile published for WITH_DATA.
struct Hub {
    graal_isolatethread_t *thread = nullptr;
    dxfg_endpoint_t *endpoint = nullptr;
    dxfg_feed_t *feed = nullptr;
    bool ready = false;

    Hub() {
        graal_isolate_t *isolate = nullptr;

        if (graal_create_isolate(nullptr, &isolate, &thread) != 0) {
            return;
        }

        endpoint = dxfg_DXEndpoint_create2(thread, DXFG_ENDPOINT_ROLE_LOCAL_HUB);
        feed = dxfg_DXEndpoint_getFeed(thread, endpoint);

        dxfg_subscription_t *subscription = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_PROFILE);

        for (const char *symbol : {WITH_DATA, NO_DATA}) {
            dxfg_string_symbol_t stringSymbol{{STRING}, symbol};

            dxfg_DXFeedSubscription_addSymbol(thread, subscription, &stringSymbol.supper);
        }

        dxfg_profile_t profile{};

        profile.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
        profile.market_event.event_symbol = WITH_DATA;
        profile.description = "published description";
        profile.status_reason = "published status reason";
        profile.beta = 2.5;

        dxfg_event_type_t *events[] = {&profile.market_event.event_type};
        dxfg_event_type_list list{1, events};
        dxfg_publisher_t *publisher = dxfg_DXEndpoint_getPublisher(thread, endpoint);

        dxfg_DXPublisher_publishEvents(thread, publisher, &list);
        dxfg_JavaObjectHandler_release(thread, &publisher->handler);

        // Waits for the published event to get to the feed.
        dxfg_string_symbol_t symbol{{STRING}, WITH_DATA};

        for (int i = 0; i < 500 && !ready; ++i) {
            auto *last = reinterpret_cast<dxfg_profile_t *>(
                dxfg_DXFeed_getLastEventIfSubscribed(thread, feed, DXFG_EVENT_PROFILE, &symbol.supper));

            ready = last != nullptr && last->description != nullptr;

            if (last != nullptr) {
                dxfg_EventType_release(thread, &last->market_event.event_type);
            }

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

// The caller's event: the SDK only reads it, so its strings are literals here.
dxfg_profile_t callerProfile(const char *symbol) {
    dxfg_profile_t profile{};

    profile.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
    profile.market_event.event_symbol = symbol;
    profile.description = "caller's description";
    profile.status_reason = "caller's status reason";
    profile.beta = 1.5;

    return profile;
}

std::string text(const char *value) {
    return value == nullptr ? "<NULL>" : value;
}

// Checks that the event is the caller's event of the symbol, unchanged.
void checkCallerProfile(const dxfg_profile_t &profile, const char *symbol) {
    CHECK(text(profile.market_event.event_symbol) == symbol);
    CHECK(text(profile.description) == "caller's description");
    CHECK(text(profile.status_reason) == "caller's status reason");
    CHECK(profile.beta == 1.5);
}

void checkPublishedProfile(const dxfg_profile_t &profile) {
    CHECK(text(profile.market_event.event_symbol) == WITH_DATA);
    CHECK(text(profile.description) == "published description");
    CHECK(text(profile.status_reason) == "published status reason");
    CHECK(profile.beta == 2.5);
}

// Clears the exception of the failed call; returns whether there was one.
bool clearException(graal_isolatethread_t *thread) {
    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

    if (exception == nullptr) {
        return false;
    }

    dxfg_Exception_release(thread, exception);

    return true;
}

#if defined(__GLIBC__)
constexpr int WARM_UP_ITERATIONS = 1'000;
constexpr int ITERATIONS = 20'000;

// Less than one leaked string per iteration.
constexpr double MAX_GROWTH_PER_ITERATION = 8.0;

// Runs the action (it returns false on a failure) after warming up and returns the growth of the heap in use, in bytes
// per iteration. No assertions in the loop: they may allocate.
template <typename Action> double heapGrowthPerIteration(Action action) {
    bool succeeded = true;

    for (int i = 0; i < WARM_UP_ITERATIONS; ++i) {
        succeeded = action() && succeeded;
    }

    const auto before = mallinfo2().uordblks;

    for (int i = 0; i < ITERATIONS; ++i) {
        succeeded = action() && succeeded;
    }

    const auto after = mallinfo2().uordblks;

    REQUIRE(succeeded);

    return (static_cast<double>(after) - static_cast<double>(before)) / ITERATIONS;
}
#endif

} // namespace

TEST_CASE("dxfg_DXFeed_getLastEvent2 returns the last event when it is available", "[LastEvent]") {
    REQUIRE(hub().ready);

    const dxfg_profile_t given = callerProfile(WITH_DATA);
    dxfg_event_type_t *lastEvent = nullptr;

    REQUIRE(dxfg_DXFeed_getLastEvent2(hub().thread, hub().feed, &given.market_event.event_type, &lastEvent) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(lastEvent != nullptr);

    auto *last = reinterpret_cast<dxfg_profile_t *>(lastEvent);

    CHECK(last->market_event.event_type.clazz == DXFG_EVENT_PROFILE);
    checkPublishedProfile(*last);
    // The given event is only read.
    checkCallerProfile(given, WITH_DATA);
    CHECK(dxfg_EventType_release(hub().thread, lastEvent) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("dxfg_DXFeed_getLastEvent2 returns a copy of the given event when the last event is not available",
          "[LastEvent]") {
    REQUIRE(hub().ready);

    for (const char *symbol : {NO_DATA, NOT_SUBSCRIBED}) {
        INFO("symbol: " << symbol);

        const dxfg_profile_t given = callerProfile(symbol);
        dxfg_event_type_t *lastEvent = nullptr;

        REQUIRE(dxfg_DXFeed_getLastEvent2(hub().thread, hub().feed, &given.market_event.event_type, &lastEvent) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(lastEvent != nullptr);

        auto *last = reinterpret_cast<dxfg_profile_t *>(lastEvent);

        CHECK(last != &given);
        checkCallerProfile(*last, symbol);
        // A copy: the strings are the SDK's, not the caller's.
        CHECK(last->description != given.description);
        CHECK(dxfg_EventType_release(hub().thread, lastEvent) == DXFG_EXECUTE_SUCCESSFULLY);
    }
}

TEST_CASE("dxfg_DXFeed_getLastEvents2 returns the last events in the order of the given events", "[LastEvent]") {
    REQUIRE(hub().ready);

    dxfg_profile_t given[] = {callerProfile(NO_DATA), callerProfile(WITH_DATA), callerProfile(NOT_SUBSCRIBED)};
    dxfg_event_type_t *elements[] = {&given[0].market_event.event_type, &given[1].market_event.event_type,
                                     &given[2].market_event.event_type};
    const dxfg_event_type_list events{3, elements};
    dxfg_event_type_list *last = nullptr;

    REQUIRE(dxfg_DXFeed_getLastEvents2(hub().thread, hub().feed, &events, &last) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(last != nullptr);
    REQUIRE(last->size == 3);
    checkCallerProfile(*reinterpret_cast<dxfg_profile_t *>(last->elements[0]), NO_DATA);
    checkPublishedProfile(*reinterpret_cast<dxfg_profile_t *>(last->elements[1]));
    checkCallerProfile(*reinterpret_cast<dxfg_profile_t *>(last->elements[2]), NOT_SUBSCRIBED);
    // The given events are only read.
    checkCallerProfile(given[1], WITH_DATA);
    CHECK(events.elements == elements);
    CHECK(dxfg_CList_EventType_release(hub().thread, last) == DXFG_EXECUTE_SUCCESSFULLY);

    const dxfg_event_type_list empty{0, nullptr};
    dxfg_event_type_list *none = nullptr;

    REQUIRE(dxfg_DXFeed_getLastEvents2(hub().thread, hub().feed, &empty, &none) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(none != nullptr);
    CHECK(none->size == 0);
    CHECK(dxfg_CList_EventType_release(hub().thread, none) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("dxfg_DXFeed_getLastEvent2 and dxfg_DXFeed_getLastEvents2 fail on a wrong argument", "[LastEvent]") {
    REQUIRE(hub().ready);

    graal_isolatethread_t *thread = hub().thread;
    dxfg_feed_t *feed = hub().feed;
    // Not NULL: the functions write NULL into the result on an error.
    auto *const notSet = reinterpret_cast<dxfg_event_type_t *>(&thread);
    auto *const notSetList = reinterpret_cast<dxfg_event_type_list *>(&thread);

    SECTION("dxfg_DXFeed_getLastEvent2") {
        dxfg_event_type_t *lastEvent = notSet;

        CHECK(dxfg_DXFeed_getLastEvent2(thread, feed, nullptr, &lastEvent) == DXFG_EXECUTE_FAIL);
        CHECK(clearException(thread));
        CHECK(lastEvent == nullptr);

        // TimeAndSale is not a lasting event.
        dxfg_time_and_sale_t timeAndSale{};

        timeAndSale.market_event.event_type.clazz = DXFG_EVENT_TIME_AND_SALE;
        timeAndSale.market_event.event_symbol = WITH_DATA;
        lastEvent = notSet;
        CHECK(dxfg_DXFeed_getLastEvent2(thread, feed, &timeAndSale.market_event.event_type, &lastEvent) ==
              DXFG_EXECUTE_FAIL);
        CHECK(clearException(thread));
        CHECK(lastEvent == nullptr);

        const dxfg_profile_t given = callerProfile(WITH_DATA);

        CHECK(dxfg_DXFeed_getLastEvent2(thread, feed, &given.market_event.event_type, nullptr) == DXFG_EXECUTE_FAIL);
        CHECK(clearException(thread));
    }

    SECTION("dxfg_DXFeed_getLastEvents2") {
        dxfg_event_type_list *lastEvents = notSetList;

        CHECK(dxfg_DXFeed_getLastEvents2(thread, feed, nullptr, &lastEvents) == DXFG_EXECUTE_FAIL);
        CHECK(clearException(thread));
        CHECK(lastEvents == nullptr);

        dxfg_event_type_t *elements[] = {nullptr};
        const dxfg_event_type_list withNull{1, elements};

        lastEvents = notSetList;
        CHECK(dxfg_DXFeed_getLastEvents2(thread, feed, &withNull, &lastEvents) == DXFG_EXECUTE_FAIL);
        CHECK(clearException(thread));
        CHECK(lastEvents == nullptr);

        const dxfg_event_type_list empty{0, nullptr};

        CHECK(dxfg_DXFeed_getLastEvents2(thread, feed, &empty, nullptr) == DXFG_EXECUTE_FAIL);
        CHECK(clearException(thread));
    }
}

TEST_CASE("dxfg_DXFeed_getLastEvent2 and dxfg_DXFeed_getLastEvents2 keep no memory", "[LastEvent]") {
    REQUIRE(hub().ready);

#if defined(__GLIBC__)
    graal_isolatethread_t *thread = hub().thread;
    dxfg_feed_t *feed = hub().feed;

    for (const char *symbol : {WITH_DATA, NO_DATA}) {
        INFO("symbol: " << symbol);

        const dxfg_profile_t given = callerProfile(symbol);
        const auto growth = heapGrowthPerIteration([thread, feed, &given] {
            dxfg_event_type_t *lastEvent = nullptr;

            return dxfg_DXFeed_getLastEvent2(thread, feed, &given.market_event.event_type, &lastEvent) == 0 &&
                   dxfg_EventType_release(thread, lastEvent) == 0;
        });

        INFO("Heap growth per dxfg_DXFeed_getLastEvent2: " << growth << " bytes");
        CHECK(growth < MAX_GROWTH_PER_ITERATION);
    }

    dxfg_profile_t given[] = {callerProfile(WITH_DATA), callerProfile(NO_DATA)};
    dxfg_event_type_t *elements[] = {&given[0].market_event.event_type, &given[1].market_event.event_type};
    const dxfg_event_type_list events{2, elements};
    const auto growth = heapGrowthPerIteration([thread, feed, &events] {
        dxfg_event_type_list *lastEvents = nullptr;

        return dxfg_DXFeed_getLastEvents2(thread, feed, &events, &lastEvents) == 0 &&
               dxfg_CList_EventType_release(thread, lastEvents) == 0;
    });

    INFO("Heap growth per dxfg_DXFeed_getLastEvents2: " << growth << " bytes");
    CHECK(growth < MAX_GROWTH_PER_ITERATION);
#else
    WARN("Skipped: needs the heap statistics of glibc");
#endif
}

} // namespace test
} // namespace dxfg
