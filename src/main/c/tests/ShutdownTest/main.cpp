// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// dxfg_v4_NativeListener_deactivateAll() and dxfg_v4_system_shutdown(): they wait for a callback in progress on another
// thread, no listener is called after they return, a listener created afterwards is never called. The deactivation
// cannot be undone for an isolate, so every test case creates its own isolate.

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <atomic>
#include <chrono>
#include <condition_variable>
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

} // namespace test
} // namespace dxfg
