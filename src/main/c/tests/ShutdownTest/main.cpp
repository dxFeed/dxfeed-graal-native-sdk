// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// dxfg_v4_NativeListener_deactivateAll() and dxfg_v4_system_shutdown(): they wait for a callback in progress on another
// thread, no listener is called after they return, a listener created afterwards is never called. The deactivation
// cannot be undone for an isolate, so every test case creates its own isolate.

#include <catch.hpp>
#include <dxfg_api.h>

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

// A new isolate; never torn down, its threads end with the process.
struct Isolate {
    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;

    Isolate() {
        if (graal_create_isolate(nullptr, &isolate, &thread) != 0) {
            thread = nullptr;
        }
    }
};

// A LOCAL_HUB endpoint and its publisher.
struct Hub {
    graal_isolatethread_t *thread;
    dxfg_v4_endpoint_t *endpoint = nullptr;
    dxfg_v4_feed_t *feed = nullptr;
    dxfg_v4_publisher_t *publisher = nullptr;
    bool ready = false;

    explicit Hub(graal_isolatethread_t *thread) : thread(thread) {
        ready = thread != nullptr &&
                dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_LOCAL_HUB, &endpoint) == 0 &&
                dxfg_v4_DXEndpoint_getFeed(thread, endpoint, &feed) == 0 &&
                dxfg_v4_DXEndpoint_getPublisher(thread, endpoint, &publisher) == 0;
    }

    ~Hub() {
        if (publisher != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &publisher->handler);
        }

        if (feed != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &feed->handler);
        }

        if (endpoint != nullptr) {
            dxfg_v4_DXEndpoint_close(thread, endpoint);
            dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);
        }
    }

    bool publish() {
        dxfg_v4_profile_t profile{};

        profile.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
        profile.market_event.event_symbol = SYMBOL;
        profile.description = "description";

        dxfg_v4_event_type_t *events[] = {&profile.market_event.event_type};
        dxfg_v4_event_type_list list{1, events};

        return dxfg_v4_DXPublisher_publishEvents(thread, publisher, &list) == 0;
    }

    // Publishes until the listener is called: the subscription reaches the publisher asynchronously.
    bool publishUntilCalled(Calls &calls) {
        for (int i = 0; i < 500 && calls.calls() == 0; ++i) {
            if (!publish()) {
                return false;
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }

        return calls.waitForCall();
    }

    // Publishes for NO_CALL_TIMEOUT: a listener that is called gets the events.
    bool publishForAWhile() {
        for (auto end = std::chrono::steady_clock::now() + NO_CALL_TIMEOUT; std::chrono::steady_clock::now() < end;) {
            if (!publish()) {
                return false;
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }

        return true;
    }
};

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

// Runs the function on a new thread attached to the isolate.
class Attached {
    std::atomic<bool> returned_{false};
    std::atomic<int32_t> result_{1};
    std::thread thread_;

  public:
    Attached(graal_isolate_t *isolate, int32_t (*function)(graal_isolatethread_t *))
        : thread_([this, isolate, function] {
              graal_isolatethread_t *thread = nullptr;

              if (graal_attach_thread(isolate, &thread) != 0) {
                  result_ = -2;
              } else {
                  result_ = function(thread);
                  graal_detach_thread(thread);
              }

              returned_ = true;
          }) {
    }

    ~Attached() {
        if (thread_.joinable()) {
            thread_.join();
        }
    }

    bool returned() const {
        return returned_;
    }

    int32_t join() {
        thread_.join();

        return result_;
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

TEST_CASE("dxfg_v4_NativeListener_deactivateAll waits for a callback, then no listener is called",
          "[Shutdown]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    static Calls blocked{true, false}; // Static: the listener may outlive the test case if it fails.
    static Calls idle{};
    Listened blockedListened(hub, blocked);
    Listened idleListened(hub, idle);

    REQUIRE(blockedListened.ready);
    REQUIRE(idleListened.ready);
    REQUIRE(hub.publishUntilCalled(blocked)); // The call blocks on a thread of the endpoint.

    Attached deactivating(isolate.isolate, &dxfg_v4_NativeListener_deactivateAll);

    std::this_thread::sleep_for(NO_CALL_TIMEOUT);
    CHECK_FALSE(deactivating.returned()); // It waits for the blocked call.

    blocked.release();
    CHECK(deactivating.join() == DXFG_EXECUTE_SUCCESSFULLY);

    const int blockedCalls = blocked.calls();
    const int idleCalls = idle.calls();

    REQUIRE(hub.publishForAWhile());
    CHECK(blocked.calls() == blockedCalls);
    CHECK(idle.calls() == idleCalls);

    // A listener created afterwards is never called.
    static Calls later{};
    Listened laterListened(hub, later);

    REQUIRE(laterListened.ready);
    REQUIRE(hub.publishForAWhile());
    CHECK(later.calls() == 0);
}

TEST_CASE("dxfg_v4_NativeListener_deactivateAll called from a callback does not wait for that call", "[Shutdown]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    static Calls deactivating{false, true};
    Listened listened(hub, deactivating);

    REQUIRE(listened.ready);
    REQUIRE(hub.publishUntilCalled(deactivating));

    for (int i = 0; i < 1000 && deactivating.deactivateAllResult == 1; ++i) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    CHECK(deactivating.deactivateAllResult == DXFG_EXECUTE_SUCCESSFULLY);

    const int calls = deactivating.calls();

    REQUIRE(hub.publishForAWhile());
    CHECK(deactivating.calls() == calls);
}

TEST_CASE("dxfg_v4_system_shutdown closes the endpoints after the callbacks", "[Shutdown]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    static Calls blocked{true, false};
    Listened listened(hub, blocked);

    REQUIRE(listened.ready);
    REQUIRE(hub.publishUntilCalled(blocked));

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
