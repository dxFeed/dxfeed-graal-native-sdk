// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The helpers of the tests that publish events to a LOCAL_HUB endpoint of their own isolate.

#ifndef DXFG_TEST_COMMON_HUB_HPP
#define DXFG_TEST_COMMON_HUB_HPP

#include <dxfg_api.h>

#include <atomic>
#include <functional>
#include <mutex>
#include <string>
#include <thread>
#include <utility>
#include <vector>

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

// The threads that called a listener, one entry per call.
class ListenerCalls {
    std::mutex mutex_{};
    std::vector<std::thread::id> threads_{};

  public:
    void add() {
        std::lock_guard<std::mutex> lock(mutex_);

        threads_.push_back(std::this_thread::get_id());
    }

    std::vector<std::thread::id> get() {
        std::lock_guard<std::mutex> lock(mutex_);

        return threads_;
    }
};

// A TimeSeriesTxModel of TimeAndSale from time 0 whose listener runs on the executor and records its calls; closed and
// released at the end.
struct TimeAndSaleTxModel {
    graal_isolatethread_t *thread;
    dxfg_v4_tx_model_listener_t *listener = nullptr;
    dxfg_v4_time_series_tx_model_t *model = nullptr;
    bool ready = false;

    TimeAndSaleTxModel(Hub &hub, dxfg_v4_executor_t *executor, dxfg_v4_symbol_t *symbol, ListenerCalls &calls)
        : thread(hub.thread) {
        dxfg_v4_time_series_tx_model_builder_t *builder = nullptr;
        dxfg_v4_time_series_tx_model_builder_t *written = nullptr;

        // Each step of the builder returns a handle of its own.
        auto step = [this, &written](int32_t result) {
            const bool done = result == 0 && written != nullptr;

            if (written != nullptr) {
                dxfg_v4_JavaObjectHandler_release(thread, &written->handler);
                written = nullptr;
            }

            return done;
        };

        ready = dxfg_v4_TxModelListener_new(thread, &onEventsReceived, &calls, &listener) == 0 &&
                dxfg_v4_TimeSeriesTxModel_newBuilder(thread, DXFG_EVENT_TIME_AND_SALE, &builder) == 0 &&
                step(dxfg_v4_TimeSeriesTxModel_Builder_withFromTime(thread, builder, 0, &written)) &&
                step(dxfg_v4_TimeSeriesTxModel_Builder_withFeed(thread, builder, hub.feed, &written)) &&
                step(dxfg_v4_TimeSeriesTxModel_Builder_withSymbol(thread, builder, symbol, &written)) &&
                step(dxfg_v4_TimeSeriesTxModel_Builder_withListener(thread, builder, listener, &written)) &&
                step(dxfg_v4_TimeSeriesTxModel_Builder_withExecutor(thread, builder, executor, &written)) &&
                dxfg_v4_TimeSeriesTxModel_Builder_build(thread, builder, &model) == 0;

        if (builder != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &builder->handler);
        }
    }

    TimeAndSaleTxModel(const TimeAndSaleTxModel &) = delete;
    TimeAndSaleTxModel &operator=(const TimeAndSaleTxModel &) = delete;

    ~TimeAndSaleTxModel() {
        if (model != nullptr) {
            dxfg_v4_TimeSeriesTxModel_close(thread, model);
            dxfg_v4_JavaObjectHandler_release(thread, &model->handler);
        }

        if (listener != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &listener->handler);
        }
    }

  private:
    static void onEventsReceived(graal_isolatethread_t * /* thread */, dxfg_v4_indexed_event_source_t * /* source */,
                                 dxfg_v4_event_type_list * /* events */, int32_t /* isSnapshot */, void *userData) {
        static_cast<ListenerCalls *>(userData)->add();
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
