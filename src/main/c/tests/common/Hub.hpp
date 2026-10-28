// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The helpers of the tests that publish events to a LOCAL_HUB endpoint of their own isolate.

#ifndef DXFG_TEST_COMMON_HUB_HPP
#define DXFG_TEST_COMMON_HUB_HPP

#include <dxfg_api.h>

#include <atomic>
#include <functional>
#include <string>
#include <thread>
#include <utility>

namespace dxfg {
namespace test {

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

// A LOCAL_HUB endpoint, its feed and its publisher.
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

    Hub(const Hub &) = delete;
    Hub &operator=(const Hub &) = delete;

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

    bool publish(dxfg_v4_event_type_t **events, int32_t size) {
        dxfg_v4_event_type_list list{size, events};

        return dxfg_v4_DXPublisher_publishEvents(thread, publisher, &list) == 0;
    }
};

// Runs the function on a new thread attached to the isolate, then detaches the thread.
class Attached {
    std::atomic<bool> returned_{false};
    std::atomic<int32_t> result_{1};
    std::thread thread_;

  public:
    Attached(graal_isolate_t *isolate, std::function<int32_t(graal_isolatethread_t *)> function)
        : thread_([this, isolate, function = std::move(function)] {
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

    Attached(const Attached &) = delete;
    Attached &operator=(const Attached &) = delete;

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

// Takes the exception of the calling thread: its Java class name, or an empty string if there is none.
inline std::string takeExceptionClassName(graal_isolatethread_t *thread) {
    dxfg_v4_exception_t *exception = nullptr;

    if (dxfg_v4_get_and_clear_thread_exception(thread, &exception) != 0 || exception == nullptr) {
        return {};
    }

    std::string className = exception->class_name != nullptr ? exception->class_name : "";

    dxfg_v4_Exception_release(thread, exception);

    return className;
}

} // namespace test
} // namespace dxfg

#endif // DXFG_TEST_COMMON_HUB_HPP
