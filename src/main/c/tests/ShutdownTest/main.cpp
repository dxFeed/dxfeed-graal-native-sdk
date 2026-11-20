// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// dxfg_v4_NativeListener_deactivateAll() and dxfg_v4_system_shutdown(): they wait for a callback in progress on another
// thread, no listener is called after they return, a listener created afterwards is never called. A second
// dxfg_v4_system_shutdown() succeeds and closes what was created after the first; a new isolate created after the
// shutdown of another delivers events, also after graal_tear_down_isolate() of the other one; the promise handlers,
// the finalizers and the schedule filters are still called after the shutdown. The deactivation cannot be undone for
// an isolate, so every test case creates its own isolate. The case that tears an isolate down runs in a process of its
// own (the test suite "TearDown", see CMakeLists.txt).

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstdlib>
#include <future>
#include <mutex>
#include <thread>

namespace dxfg {
namespace test {

namespace {

constexpr auto CALL_TIMEOUT = std::chrono::seconds(10);
constexpr auto NO_CALL_TIMEOUT = std::chrono::milliseconds(500);
constexpr const char *SYMBOL = "SHUTDOWN-TEST";

// The calls of a listener. The first call blocks until release() if `blocking`, and calls
// dxfg_v4_NativeListener_deactivateAll() if `deactivatesAll`.
struct Calls {
    bool blocking = false;
    bool deactivatesAll = false;

    std::mutex mutex{};
    std::condition_variable changed{};
    int count = 0;
    bool released = false;
    std::atomic<int32_t> deactivateAllResult{1};

    void call(graal_isolatethread_t *thread) {
        std::unique_lock<std::mutex> lock(mutex);
        const bool first = ++count == 1;

        changed.notify_all();

        if (first && blocking) {
            changed.wait(lock, [this] { return released; });
        }

        if (first && deactivatesAll) {
            lock.unlock();
            deactivateAllResult = dxfg_v4_NativeListener_deactivateAll(thread);
        }
    }

    bool waitForCall() {
        std::unique_lock<std::mutex> lock(mutex);

        return changed.wait_for(lock, CALL_TIMEOUT, [this] { return count > 0; });
    }

    void release() {
        std::lock_guard<std::mutex> lock(mutex);

        released = true;
        changed.notify_all();
    }

    int calls() {
        std::lock_guard<std::mutex> lock(mutex);

        return count;
    }
};

void onEvents(graal_isolatethread_t *thread, dxfg_v4_event_type_list *, void *userData) {
    static_cast<Calls *>(userData)->call(thread);
}

// Publishes a Profile of SYMBOL.
bool publishProfile(Hub &hub) {
    dxfg_v4_profile_t profile{};

    profile.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
    profile.market_event.event_symbol = SYMBOL;
    profile.description = "description";

    dxfg_v4_event_type_t *events[] = {&profile.market_event.event_type};

    return hub.publish(events, 1);
}

// Publishes until the listener is called: the subscription reaches the publisher asynchronously.
bool publishUntilCalled(Hub &hub, Calls &calls) {
    for (int i = 0; i < 500 && calls.calls() == 0; ++i) {
        if (!publishProfile(hub)) {
            return false;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    return calls.waitForCall();
}

// Publishes for NO_CALL_TIMEOUT: a listener that is called gets the events.
bool publishForAWhile(Hub &hub) {
    for (auto end = std::chrono::steady_clock::now() + NO_CALL_TIMEOUT; std::chrono::steady_clock::now() < end;) {
        if (!publishProfile(hub)) {
            return false;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    return true;
}

// A Profile subscription to SYMBOL with a listener of the calls.
struct Listened {
    graal_isolatethread_t *thread;
    dxfg_v4_subscription_t *subscription = nullptr;
    dxfg_v4_feed_event_listener_t *listener = nullptr;
    bool ready = false;

    Listened(Hub &hub, Calls &calls) : thread(hub.thread) {
        dxfg_v4_string_symbol_t symbol{{STRING}, SYMBOL};

        ready = dxfg_v4_DXFeed_createSubscription(thread, hub.feed, DXFG_EVENT_PROFILE, &subscription) == 0 &&
                dxfg_v4_DXFeedEventListener_new(thread, &onEvents, &calls, &listener) == 0 &&
                dxfg_v4_DXFeedSubscription_addEventListener(thread, subscription, listener) == 0 &&
                dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) == 0;
    }

    ~Listened() {
        if (subscription != nullptr) {
            dxfg_v4_DXFeedSubscription_close(thread, subscription);
            dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
        }

        if (listener != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &listener->handler);
        }
    }
};

// Releases the calls at the end of the scope, also when a REQUIRE fails: then a thread that waits for a blocked call
// returns, and its Attached can be joined.
struct ReleaseOnExit {
    Calls &calls;

    ~ReleaseOnExit() {
        calls.release();
    }
};

dxfg_v4_endpoint_state_t stateOf(const Hub &hub) {
    auto state = static_cast<dxfg_v4_endpoint_state_t>(-1);

    REQUIRE(dxfg_v4_DXEndpoint_getState(hub.thread, hub.endpoint, &state) == DXFG_EXECUTE_SUCCESSFULLY);

    return state;
}

// A hub of a new isolate whose listener gets the published events: the isolate works.
bool delivers(const Isolate &isolate, Calls &calls) {
    Hub hub(isolate.thread);

    if (!hub.ready) {
        return false;
    }

    Listened listened(hub, calls);

    return listened.ready && publishUntilCalled(hub, calls);
}

// The calls of the callbacks that are not listeners: static, since the SDK may call them after the test case ends (the
// finalizer) and the day filter of dxfg_v4_DayFilter_new() gets no user data.
std::atomic<int> finalizerCalls{0};
std::atomic<int> dayFilterCalls{0};

void onPromiseDone(graal_isolatethread_t *, dxfg_promise_t *, void *userData) {
    ++*static_cast<std::atomic<int> *>(userData);
}

void onFinalized(graal_isolatethread_t *, void *userData) {
    ++*static_cast<std::atomic<int> *>(userData);
}

int32_t acceptDay(graal_isolatethread_t *, dxfg_day_t *) {
    ++dayFilterCalls;

    return 1;
}

int32_t acceptDayWithUserData(graal_isolatethread_t *, dxfg_v4_day_t *, void *userData) {
    ++*static_cast<std::atomic<int> *>(userData);

    return 1;
}

int32_t acceptSessionWithUserData(graal_isolatethread_t *, dxfg_v4_session_t *, void *userData) {
    ++*static_cast<std::atomic<int> *>(userData);

    return 1;
}

// The next day after the day that the filter accepts, as yyyymmdd; 0 if none is found.
int32_t findNextDay(graal_isolatethread_t *thread, dxfg_v4_day_t *from, dxfg_v4_day_filter_t *filter) {
    dxfg_v4_day_t *next = nullptr;
    int32_t yearMonthDay = 0;

    REQUIRE(dxfg_v4_Day_findNextDay(thread, from, filter, &next) == DXFG_EXECUTE_SUCCESSFULLY);

    if (next != nullptr) {
        REQUIRE(dxfg_v4_Day_getYearMonthDay(thread, next, &yearMonthDay) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(dxfg_v4_Day_release(thread, next) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    return yearMonthDay;
}

} // namespace

TEST_CASE("dxfg_v4_NativeListener_deactivateAll waits for a callback, then no listener is called") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    static Calls blocked{true, false}; // Static: the listener may outlive the test case if it fails.
    static Calls idle{};
    Listened blockedListened(hub, blocked);
    Listened idleListened(hub, idle);

    REQUIRE(blockedListened.ready);
    REQUIRE(idleListened.ready);
    REQUIRE(publishUntilCalled(hub, blocked)); // The call blocks on a thread of the endpoint.

    Attached deactivating(isolate.isolate, &dxfg_v4_NativeListener_deactivateAll);

    std::this_thread::sleep_for(NO_CALL_TIMEOUT);
    CHECK_FALSE(deactivating.returned()); // It waits for the blocked call.

    blocked.release();
    CHECK(deactivating.join() == DXFG_EXECUTE_SUCCESSFULLY);

    const int blockedCalls = blocked.calls();
    const int idleCalls = idle.calls();

    REQUIRE(publishForAWhile(hub));
    CHECK(blocked.calls() == blockedCalls);
    CHECK(idle.calls() == idleCalls);

    // A listener created afterwards is never called.
    static Calls later{};
    Listened laterListened(hub, later);

    REQUIRE(laterListened.ready);
    REQUIRE(publishForAWhile(hub));
    CHECK(later.calls() == 0);
}

TEST_CASE("dxfg_v4_NativeListener_deactivateAll called from a callback does not wait for that call") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    static Calls deactivating{false, true};
    Listened listened(hub, deactivating);

    REQUIRE(listened.ready);
    REQUIRE(publishUntilCalled(hub, deactivating));

    for (int i = 0; i < 1000 && deactivating.deactivateAllResult == 1; ++i) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    CHECK(deactivating.deactivateAllResult == DXFG_EXECUTE_SUCCESSFULLY);

    const int calls = deactivating.calls();

    REQUIRE(publishForAWhile(hub));
    CHECK(deactivating.calls() == calls);
}

TEST_CASE("dxfg_v4_system_shutdown closes the endpoints after the callbacks") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    static Calls blocked{true, false};
    Listened listened(hub, blocked);

    REQUIRE(listened.ready);
    REQUIRE(publishUntilCalled(hub, blocked));

    Attached shuttingDown(isolate.isolate, &dxfg_v4_system_shutdown);
    ReleaseOnExit releaseOnExit{blocked}; // Destroyed before shuttingDown.

    std::this_thread::sleep_for(NO_CALL_TIMEOUT);
    CHECK_FALSE(shuttingDown.returned());

    dxfg_v4_endpoint_state_t state{};

    REQUIRE(dxfg_v4_DXEndpoint_getState(hub.thread, hub.endpoint, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state != DXFG_ENDPOINT_STATE_CLOSED); // The listeners first.

    blocked.release();
    CHECK(shuttingDown.join() == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXEndpoint_getState(hub.thread, hub.endpoint, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state == DXFG_ENDPOINT_STATE_CLOSED);
}

TEST_CASE("A second dxfg_v4_system_shutdown succeeds and closes the endpoints created after the first") {
    Isolate isolate{};
    REQUIRE(isolate.thread != nullptr);

    Hub first(isolate.thread);
    REQUIRE(first.ready);
    REQUIRE(dxfg_v4_system_shutdown(isolate.thread) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(stateOf(first) == DXFG_ENDPOINT_STATE_CLOSED);

    // Nothing is left to close: the second call returns at once.
    const auto start = std::chrono::steady_clock::now();

    CHECK(dxfg_v4_system_shutdown(isolate.thread) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(std::chrono::steady_clock::now() - start < CALL_TIMEOUT);

    // An endpoint created afterwards works, but its listeners are never called; the next call closes it.
    Hub second(isolate.thread);
    REQUIRE(second.ready);
    CHECK(stateOf(second) != DXFG_ENDPOINT_STATE_CLOSED);

    static Calls later{};
    Listened laterListened(second, later);

    REQUIRE(laterListened.ready);
    REQUIRE(publishForAWhile(second));
    CHECK(later.calls() == 0);

    CHECK(dxfg_v4_system_shutdown(isolate.thread) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(stateOf(second) == DXFG_ENDPOINT_STATE_CLOSED);
}

TEST_CASE("A new isolate created after dxfg_v4_system_shutdown of another delivers events") {
    Isolate shutDown{};
    REQUIRE(shutDown.thread != nullptr);

    {
        Hub hub(shutDown.thread);
        REQUIRE(hub.ready);
        REQUIRE(dxfg_v4_system_shutdown(shutDown.thread) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    // The shutdown acts on its own isolate only.
    Isolate created{};
    REQUIRE(created.thread != nullptr);

    static Calls calls{};

    CHECK(delivers(created, calls));
    CHECK(dxfg_v4_system_shutdown(created.thread) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A new isolate created after dxfg_v4_system_shutdown and graal_tear_down_isolate of another delivers events" *
          doctest::test_suite("TearDown")) {
    Isolate tornDown{};
    REQUIRE(tornDown.thread != nullptr);

    static Calls before{};

    CHECK(delivers(tornDown, before));
    REQUIRE(dxfg_v4_system_shutdown(tornDown.thread) == DXFG_EXECUTE_SUCCESSFULLY);

    // graal_tear_down_isolate() must run on the thread that owns the isolate thread, so a watchdog ends the process
    // if it does not return in time: a thread blocked in it cannot be joined.
    std::promise<void> tearDownReturned;
    auto tearDownDone = tearDownReturned.get_future();
    std::thread watchdog([&tearDownDone] {
        if (tearDownDone.wait_for(CALL_TIMEOUT) != std::future_status::ready) {
            std::fprintf(stderr, "graal_tear_down_isolate() did not return in time\n");
            std::fflush(stderr);
            std::_Exit(1);
        }
    });

    const int tearDownResult = graal_tear_down_isolate(tornDown.thread);

    tearDownReturned.set_value();
    watchdog.join();
    REQUIRE(tearDownResult == 0);

    Isolate created{};
    REQUIRE(created.thread != nullptr);

    static Calls after{};

    CHECK(delivers(created, after));
    CHECK(dxfg_v4_system_shutdown(created.thread) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("After dxfg_v4_system_shutdown the promise handlers, the finalizers and the schedule filters are called") {
    Isolate isolate{};
    auto *thread = isolate.thread;
    REQUIRE(thread != nullptr);

    // Registered before the shutdown.
    std::atomic<int> handlerCalls{0};
    std::atomic<int> asyncHandlerCalls{0};
    dxfg_v4_promise_t *promise = nullptr;
    dxfg_v4_executor_t *executor = nullptr;

    REQUIRE(dxfg_v4_Promise_new(thread, &promise) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_Promise_whenDone(thread, promise, &onPromiseDone, &handlerCalls) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_new(thread, &executor) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_Promise_whenDoneAsync(thread, promise, &onPromiseDone, &asyncHandlerCalls, executor) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_java_object_handler *object = nullptr;

    REQUIRE(dxfg_v4_Object_new(thread, &object) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_Object_finalize(thread, object, &onFinalized, &finalizerCalls) == DXFG_EXECUTE_SUCCESSFULLY);

    std::atomic<int> dayWithUserDataCalls{0};
    std::atomic<int> sessionCalls{0};
    dxfg_v4_day_filter_t *dayFilter = nullptr;
    dxfg_v4_day_filter_t *dayFilterWithUserData = nullptr;
    dxfg_v4_session_filter_t *sessionFilter = nullptr;

    REQUIRE(dxfg_v4_DayFilter_new(thread, &acceptDay, &dayFilter) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DayFilter_newWithUserData(thread, 0, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY,
                                              &acceptDayWithUserData, &dayWithUserDataCalls,
                                              &dayFilterWithUserData) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_SessionFilter_newWithUserData(thread, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY,
                                                  &acceptSessionWithUserData, &sessionCalls,
                                                  &sessionFilter) == DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dxfg_v4_system_shutdown(thread) == DXFG_EXECUTE_SUCCESSFULLY);

    // A promise completed after the shutdown calls its handlers: whenDone() on the completing thread, whenDoneAsync()
    // by its executor.
    REQUIRE(dxfg_v4_Promise_complete(thread, promise, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(handlerCalls == 1);
    REQUIRE(dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(thread, executor) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(asyncHandlerCalls == 1);

    // The filters created before and after the shutdown call their functions.
    dxfg_v4_day_filter_t *laterDayFilter = nullptr;

    REQUIRE(dxfg_v4_DayFilter_new(thread, &acceptDay, &laterDayFilter) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_schedule_t *schedule = nullptr;

    REQUIRE(dxfg_v4_Schedule_getInstanceByDefinition(thread, "(tz=America/New_York;0=p04000930r09301600a16002000)",
                                                     &schedule) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_day_t *wednesday = nullptr;

    REQUIRE(dxfg_v4_Schedule_getDayByYearMonthDay(thread, schedule, 20240703, &wednesday) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    const int dayCallsBefore = dayFilterCalls;

    CHECK(findNextDay(thread, wednesday, dayFilter) == 20240704);
    CHECK(findNextDay(thread, wednesday, laterDayFilter) == 20240704);
    CHECK(dayFilterCalls == dayCallsBefore + 2);
    CHECK(findNextDay(thread, wednesday, dayFilterWithUserData) == 20240704);
    CHECK(dayWithUserDataCalls == 1);

    dxfg_v4_session_t *session = nullptr;

    REQUIRE(dxfg_v4_Day_findFirstSession(thread, wednesday, sessionFilter, &session) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(session != nullptr);
    CHECK(sessionCalls == 1);

    // An object collected after the shutdown calls its finalizer (on a thread of the SDK, about once a second).
    REQUIRE(dxfg_v4_JavaObjectHandler_release(thread, object) == DXFG_EXECUTE_SUCCESSFULLY);

    for (int i = 0; i < 40 && finalizerCalls == 0; ++i) {
        REQUIRE(dxfg_v4_gc(thread) == DXFG_EXECUTE_SUCCESSFULLY);
        std::this_thread::sleep_for(std::chrono::milliseconds(250));
    }

    CHECK(finalizerCalls == 1);

    if (session != nullptr) {
        dxfg_v4_Session_release(thread, session);
    }

    dxfg_v4_Day_release(thread, wednesday);
    dxfg_v4_JavaObjectHandler_release(thread, &schedule->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &sessionFilter->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &dayFilterWithUserData->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &laterDayFilter->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &dayFilter->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &executor->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &promise->handler);
}

} // namespace test
} // namespace dxfg
