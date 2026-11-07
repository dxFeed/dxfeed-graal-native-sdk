// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The reference adapter of the conformance kit. It speaks the protocol of protocol.md and makes every request a call
// of the C API of the SDK. A wrapper author writes the same program in the wrapper's language on top of the wrapper;
// this one shows which C calls each request stands for.
//
// Threads:
// - The main thread reads the requests, creates the isolate at `start`, makes every call except those of
//   `poll-async` and `release ... thread=worker`, and alone writes the responses.
// - One worker thread is attached to the isolate once at `start` and detached at the end; it runs those two requests,
//   one task at a time.
//
// stdout carries only the protocol: the adapter keeps a copy of the original stdout for it and points stdout at
// stderr, so that whatever else writes to stdout (a log of the SDK, say) goes to stderr, which the runner does not
// parse.

// The ready object includes <windows.h> on Windows; it comes first, with WIN32_LEAN_AND_MEAN and NOMINMAX.
#include "ReadyObject.hpp"

#include "Events.hpp"

#include <dxfg_api.h>

#ifdef _WIN32
#    include <fcntl.h>
#    include <io.h>
#else
#    include <fcntl.h>
#    include <unistd.h>
#endif

#include <cerrno>
#include <chrono>
#include <climits>
#include <condition_variable>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <deque>
#include <functional>
#include <future>
#include <iostream>
#include <map>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <thread>
#include <utility>
#include <vector>

namespace dxfg {
namespace kit {

namespace {

constexpr const char *HELLO = "hello proto=1 adapter=reference caps=publisher,async_poll,ready_signal,teardown";
constexpr const char *MAX_BUFFER_SIZE_PROPERTY = "com.devexperts.qd.impl.matrix.Agent.MaxBufferSize";

// The events that one call of dxfg_v4_DXPublisher_publishEvents() publishes in publish-burst.
constexpr std::int64_t BURST_BATCH = 1000;

// Takes stdout for the protocol: returns a stream on a copy of the original stdout and points stdout at stderr.
FILE *takeStdout() {
    std::fflush(stdout);
#ifdef _WIN32
    const int protocol = _dup(_fileno(stdout));

    if (protocol < 0 || _dup2(_fileno(stderr), _fileno(stdout)) != 0) {
        return nullptr;
    }

    // The SDK takes its stdout from the standard handle of the process, not from the C runtime.
    SetStdHandle(STD_OUTPUT_HANDLE, GetStdHandle(STD_ERROR_HANDLE));

    return _fdopen(protocol, "wb");
#else
    const int protocol = dup(STDOUT_FILENO);

    if (protocol < 0 || fcntl(protocol, F_SETFD, FD_CLOEXEC) != 0 || dup2(STDERR_FILENO, STDOUT_FILENO) < 0) {
        return nullptr;
    }

    return fdopen(protocol, "w");
#endif
}

// The tokens of a request: the parts between single spaces.
std::vector<std::string> split(const std::string &line) {
    std::vector<std::string> tokens;
    std::size_t start = 0;

    while (start < line.size()) {
        std::size_t end = line.find(' ', start);

        if (end == std::string::npos) {
            end = line.size();
        }

        if (end > start) {
            tokens.push_back(line.substr(start, end - start));
        }

        start = end + 1;
    }

    return tokens;
}

// The text with its control characters replaced by spaces, so that it fits in one response line.
std::string printable(std::string text) {
    for (char &c : text) {
        if (static_cast<unsigned char>(c) < 0x20 || c == 0x7F) {
            c = ' ';
        }
    }

    return text;
}

std::optional<std::int64_t> parseInt64(const std::string &text) {
    if (text.empty()) {
        return std::nullopt;
    }

    errno = 0;
    char *end = nullptr;
    const long long value = std::strtoll(text.c_str(), &end, 10);

    if (errno == ERANGE || end != text.c_str() + text.size()) {
        return std::nullopt;
    }

    return static_cast<std::int64_t>(value);
}

// The value of the option `<key>=<value>` among the tokens from the index, or nullopt.
std::optional<std::string> option(const std::vector<std::string> &tokens, std::size_t from, const std::string &key) {
    const std::string prefix = key + "=";

    for (std::size_t i = from; i < tokens.size(); ++i) {
        if (tokens[i].compare(0, prefix.size(), prefix) == 0) {
            return tokens[i].substr(prefix.size());
        }
    }

    return std::nullopt;
}

// Takes the exception of the failed call on the thread: "<class>: <message>". A failed call leaves it for the thread,
// and the next failed call would replace it, so the adapter takes it right after each failure.
std::string takeException(graal_isolatethread_t *thread) {
    dxfg_v4_exception_t *exception = nullptr;

    if (dxfg_v4_get_and_clear_thread_exception(thread, &exception) != DXFG_EXECUTE_SUCCESSFULLY ||
        exception == nullptr) {
        return "no exception";
    }

    std::string text = std::string(exception->class_name != nullptr ? exception->class_name : "?") + ": " +
                       (exception->message != nullptr ? exception->message : "");

    dxfg_v4_Exception_release(thread, exception);

    return printable(text);
}

// The response lines of a poll: the events block of protocol.md, or one error line. With `hold`, a returned list is
// not released but given to the caller.
std::vector<std::string> pollBlock(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue, std::int64_t timeout,
                                   std::int32_t limit, dxfg_v4_event_type_list **hold, std::int64_t *droppedOut) {
    dxfg_v4_event_type_list *events = nullptr;
    const std::int32_t result = dxfg_v4_EventQueue_poll(thread, queue, timeout, limit, &events);
    std::string outcome;

    switch (result) {
    case DXFG_EXECUTE_SUCCESSFULLY:
        outcome = "events";

        break;
    case DXFG_EVENT_QUEUE_NO_EVENTS:
        outcome = "no_events";

        break;
    case DXFG_EVENT_QUEUE_CLOSED:
        outcome = "closed";

        break;
    case DXFG_EXECUTE_FAIL:
        outcome = "fail";
        std::cerr << "adapter: the poll failed: " << takeException(thread) << std::endl;

        break;
    default:
        return {"error dxfg_v4_EventQueue_poll returned " + std::to_string(result)};
    }

    // Read after the poll, so that a gap before the returned events shows in this block.
    std::int64_t dropped = 0;

    if (dxfg_v4_EventQueue_getDroppedEvents(thread, queue, &dropped) != DXFG_EXECUTE_SUCCESSFULLY) {
        const std::string reason = takeException(thread);

        if (events != nullptr) {
            dxfg_v4_event_type_list_release(thread, events);
        }

        return {"error dxfg_v4_EventQueue_getDroppedEvents failed: " + reason};
    }

    std::vector<std::string> lines{"events " + outcome + " dropped=" + std::to_string(dropped)};

    if (events != nullptr) {
        for (std::int32_t i = 0; i < events->size; ++i) {
            lines.push_back(formatEvent(events->elements[i]));
        }

        if (hold != nullptr) {
            *hold = events;
        } else {
            dxfg_v4_event_type_list_release(thread, events);
        }
    }

    lines.emplace_back("end");

    if (droppedOut != nullptr) {
        *droppedOut = dropped;
    }

    return lines;
}

// The worker thread: attached to the isolate once, it runs the posted tasks one at a time and detaches at the end.
class Worker {
    std::mutex mutex_{};
    std::condition_variable changed_{};
    std::deque<std::function<void(graal_isolatethread_t *)>> tasks_{};
    bool busy_ = false;
    bool stopping_ = false;
    std::thread thread_{};

    void loop(graal_isolatethread_t *thread) {
        std::unique_lock<std::mutex> lock(mutex_);

        for (;;) {
            changed_.wait(lock, [this] {
                return stopping_ || !tasks_.empty();
            });

            if (tasks_.empty()) {
                return;
            }

            auto task = std::move(tasks_.front());

            tasks_.pop_front();
            busy_ = true;
            lock.unlock();
            task(thread);
            lock.lock();
            busy_ = false;
            changed_.notify_all();
        }
    }

  public:
    Worker() = default;
    Worker(const Worker &) = delete;
    Worker &operator=(const Worker &) = delete;

    ~Worker() {
        stop();
    }

    bool start(graal_isolate_t *isolate) {
        std::promise<bool> attached;
        auto result = attached.get_future();

        thread_ = std::thread([this, isolate, attached = std::move(attached)]() mutable {
            graal_isolatethread_t *thread = nullptr;

            if (graal_attach_thread(isolate, &thread) != 0) {
                attached.set_value(false);

                return;
            }

            attached.set_value(true);
            loop(thread);
            graal_detach_thread(thread);
        });

        if (!result.get()) {
            thread_.join();

            return false;
        }

        return true;
    }

    void post(std::function<void(graal_isolatethread_t *)> task) {
        std::lock_guard<std::mutex> lock(mutex_);

        tasks_.push_back(std::move(task));
        changed_.notify_all();
    }

    // Runs the function on the worker and returns its result.
    std::string call(std::function<std::string(graal_isolatethread_t *)> function) {
        auto promise = std::make_shared<std::promise<std::string>>();
        auto result = promise->get_future();

        post([promise, function = std::move(function)](graal_isolatethread_t *thread) {
            promise->set_value(function(thread));
        });

        return result.get();
    }

    // Waits until every posted task has run.
    void waitIdle() {
        std::unique_lock<std::mutex> lock(mutex_);

        changed_.wait(lock, [this] {
            return tasks_.empty() && !busy_;
        });
    }

    // Runs the remaining tasks, then detaches and ends the thread.
    void stop() {
        if (!thread_.joinable()) {
            return;
        }

        {
            std::lock_guard<std::mutex> lock(mutex_);

            stopping_ = true;
            changed_.notify_all();
        }

        thread_.join();
    }
};

// The result of a poll-async: the response lines once the poll on the worker has ended.
struct AsyncPoll {
    std::mutex mutex{};
    std::condition_variable ended{};
    bool done = false;
    std::vector<std::string> lines{};
};

struct Endpoint {
    dxfg_v4_endpoint_t *endpoint = nullptr;
    dxfg_v4_feed_t *feed = nullptr;
    dxfg_v4_publisher_t *publisher = nullptr;
};

struct Queue {
    dxfg_v4_subscription_t *subscription = nullptr;
    dxfg_v4_event_queue_t *queue = nullptr;
    std::shared_ptr<AsyncPoll> async{};
    std::unique_ptr<ReadyObject> ready{};
};

// A list that a poll with hold= kept, with the dropped count of that poll.
struct Held {
    dxfg_v4_event_type_list *list = nullptr;
    std::int64_t dropped = 0;
};

class Adapter {
    FILE *out_;
    graal_isolate_t *isolate_ = nullptr;
    graal_isolatethread_t *thread_ = nullptr;
    bool started_ = false;
    std::int64_t expectedRevision_ = DXFG_V4_ADVANCED_REVISION;
    std::optional<std::string> maxBufferSize_{};
    Worker worker_{};
    std::map<std::string, Endpoint> endpoints_{};
    std::map<std::string, Queue> queues_{};
    std::map<std::string, Held> held_{};

    void respond(const std::string &line) {
        std::fputs(line.c_str(), out_);
        std::fputc('\n', out_);
        std::fflush(out_);
    }

    void respondLines(const std::vector<std::string> &lines) {
        for (const auto &line : lines) {
            std::fputs(line.c_str(), out_);
            std::fputc('\n', out_);
        }

        std::fflush(out_);
    }

    // The ok response, or the error of the failed call with its exception.
    void respondCall(bool succeeded, const std::string &call) {
        respond(succeeded ? std::string("ok") : "error " + call + " failed: " + takeException(thread_));
    }

    Endpoint *findEndpoint(const std::string &name) {
        auto found = endpoints_.find(name);

        return found == endpoints_.end() ? nullptr : &found->second;
    }

    Queue *findQueue(const std::string &name) {
        auto found = queues_.find(name);

        return found == queues_.end() ? nullptr : &found->second;
    }

    // config <key>=<value>, before start.
    void config(const std::vector<std::string> &tokens) {
        const std::size_t equals = tokens.size() == 2 ? tokens[1].find('=') : std::string::npos;

        if (started_ || equals == std::string::npos) {
            respond("error bad config");

            return;
        }

        const std::string key = tokens[1].substr(0, equals);
        const std::string value = tokens[1].substr(equals + 1);

        if (key == "expected_advanced_revision" && parseInt64(value)) {
            expectedRevision_ = *parseInt64(value);
            respond("ok");
        } else if (key == "max_buffer_size" && parseInt64(value) && *parseInt64(value) > 0) {
            maxBufferSize_ = value;
            respond("ok");
        } else {
            respond("error unknown config " + tokens[1]);
        }
    }

    // start: the isolate, the configuration, the worker.
    void start() {
        if (started_) {
            respond("error already started");

            return;
        }

        if (graal_create_isolate(nullptr, &isolate_, &thread_) != 0) {
            respond("error graal_create_isolate failed");

            return;
        }

        // A system property of QD: it takes effect if set before the first endpoint.
        if (maxBufferSize_ &&
            dxfg_v4_system_set_property(thread_, MAX_BUFFER_SIZE_PROPERTY, maxBufferSize_->c_str()) != 0) {
            respondCall(false, "dxfg_v4_system_set_property");

            return;
        }

        std::int32_t revision = 0;
        const char *version = nullptr;

        if (dxfg_v4_system_get_advanced_revision(thread_, &revision) != 0) {
            respondCall(false, "dxfg_v4_system_get_advanced_revision");

            return;
        }

        if (dxfg_v4_system_get_version(thread_, &version) != 0) {
            respondCall(false, "dxfg_v4_system_get_version");

            return;
        }

        const std::string versionText = version;

        dxfg_v4_String_release(thread_, version);

        if (!worker_.start(isolate_)) {
            respond("error graal_attach_thread failed for the worker");

            return;
        }

        started_ = true;

        // A wrapper binds the experimental functions only when the revision it was built for is the library's.
        respond(std::string("ready advanced=") + (revision == expectedRevision_ ? "enabled" : "disabled") +
                " version=" + printable(versionText));
    }

    // feature <name>
    void feature(const std::vector<std::string> &tokens) {
        std::int32_t supported = 0;

        if (tokens.size() != 2) {
            respond("error usage: feature <name>");
        } else if (dxfg_v4_system_is_feature_supported(thread_, tokens[1].c_str(), &supported) != 0) {
            respondCall(false, "dxfg_v4_system_is_feature_supported");
        } else {
            respond(supported != 0 ? "present" : "absent");
        }
    }

    // endpoint <ep> local_hub
    void endpoint(const std::vector<std::string> &tokens) {
        if (tokens.size() != 3 || tokens[2] != "local_hub" || findEndpoint(tokens[1]) != nullptr) {
            respond("error usage: endpoint <new name> local_hub");

            return;
        }

        Endpoint created;

        if (dxfg_v4_DXEndpoint_createWithRole(thread_, DXFG_ENDPOINT_ROLE_LOCAL_HUB, &created.endpoint) != 0) {
            respondCall(false, "dxfg_v4_DXEndpoint_createWithRole");

            return;
        }

        // Kept from here on, so that reset releases what was created even if the next calls fail.
        Endpoint &kept = endpoints_[tokens[1]] = created;

        if (dxfg_v4_DXEndpoint_getFeed(thread_, kept.endpoint, &kept.feed) != 0) {
            respondCall(false, "dxfg_v4_DXEndpoint_getFeed");
        } else {
            respondCall(dxfg_v4_DXEndpoint_getPublisher(thread_, kept.endpoint, &kept.publisher) == 0,
                        "dxfg_v4_DXEndpoint_getPublisher");
        }
    }

    // queue <q> <ep> <Type>[,<Type>...]
    void queue(const std::vector<std::string> &tokens) {
        Endpoint *endpoint = tokens.size() == 4 ? findEndpoint(tokens[2]) : nullptr;

        if (endpoint == nullptr || endpoint->feed == nullptr || findQueue(tokens[1]) != nullptr) {
            respond("error usage: queue <new name> <endpoint> <Type>[,<Type>...]");

            return;
        }

        std::vector<dxfg_event_clazz_t> clazzes;
        std::size_t start = 0;

        while (start <= tokens[3].size()) {
            std::size_t end = tokens[3].find(',', start);

            if (end == std::string::npos) {
                end = tokens[3].size();
            }

            const EventType *type = findType(tokens[3].substr(start, end - start));

            if (type == nullptr) {
                respond("error unknown event type " + tokens[3].substr(start, end - start));

                return;
            }

            clazzes.push_back(type->clazz);
            start = end + 1;
        }

        // The list is only read by the call.
        std::vector<dxfg_event_clazz_t *> pointers;

        for (auto &clazz : clazzes) {
            pointers.push_back(&clazz);
        }

        dxfg_v4_event_clazz_list list{static_cast<std::int32_t>(pointers.size()), pointers.data()};
        Queue created;

        if (dxfg_v4_DXFeed_createSubscriptionWithEventTypes(thread_, endpoint->feed, &list, &created.subscription) !=
            0) {
            respondCall(false, "dxfg_v4_DXFeed_createSubscriptionWithEventTypes");

            return;
        }

        Queue &kept = queues_[tokens[1]];

        kept.subscription = created.subscription;
        respondCall(dxfg_v4_EventQueue_new(thread_, kept.subscription, &kept.queue) == 0, "dxfg_v4_EventQueue_new");
    }

    // subscribe <q> <symbol>... [from=<time>]
    void subscribe(const std::vector<std::string> &tokens) {
        Queue *queue = tokens.size() >= 3 ? findQueue(tokens[1]) : nullptr;
        const auto from = option(tokens, 2, "from");
        const auto fromTime = from ? parseInt64(*from) : std::nullopt;

        if (queue == nullptr || queue->queue == nullptr || (from && !fromTime)) {
            respond("error usage: subscribe <queue> <symbol>... [from=<time>]");

            return;
        }

        for (std::size_t i = 2; i < tokens.size(); ++i) {
            if (tokens[i].compare(0, 5, "from=") == 0) {
                continue;
            }

            const auto symbol = percentDecode(tokens[i]);

            if (!symbol) {
                respond("error bad symbol " + tokens[i]);

                return;
            }

            // The symbols are only read by the call.
            dxfg_v4_string_symbol_t stringSymbol{{STRING}, symbol->c_str()};
            dxfg_v4_time_series_subscription_symbol_t timeSeriesSymbol{
                {TIME_SERIES_SUBSCRIPTION}, &stringSymbol.supper, fromTime.value_or(0)};
            dxfg_v4_symbol_t *added = fromTime ? &timeSeriesSymbol.supper : &stringSymbol.supper;

            if (dxfg_v4_DXFeedSubscription_addSymbol(thread_, queue->subscription, added) != 0) {
                respondCall(false, "dxfg_v4_DXFeedSubscription_addSymbol");

                return;
            }
        }

        respond("ok");
    }

    bool publishEvents(Endpoint &endpoint, std::vector<std::unique_ptr<OwnedEvent>> &events) {
        std::vector<dxfg_v4_event_type_t *> pointers;

        for (auto &event : events) {
            pointers.push_back(event->get());
        }

        dxfg_v4_event_type_list list{static_cast<std::int32_t>(pointers.size()), pointers.data()};

        return dxfg_v4_DXPublisher_publishEvents(thread_, endpoint.publisher, &list) == 0;
    }

    // publish <ep> <Type> <field>=<value>...
    void publish(const std::vector<std::string> &tokens) {
        Endpoint *endpoint = tokens.size() >= 3 ? findEndpoint(tokens[1]) : nullptr;
        const EventType *type = tokens.size() >= 3 ? findType(tokens[2]) : nullptr;

        if (endpoint == nullptr || endpoint->publisher == nullptr || type == nullptr) {
            respond("error usage: publish <endpoint> <Type> <field>=<value>...");

            return;
        }

        std::vector<std::unique_ptr<OwnedEvent>> events;

        events.push_back(std::make_unique<OwnedEvent>(*type));

        for (std::size_t i = 3; i < tokens.size(); ++i) {
            const std::size_t equals = tokens[i].find('=');
            std::string error = "bad field " + tokens[i];

            if (equals == std::string::npos ||
                !events.back()->set(tokens[i].substr(0, equals), tokens[i].substr(equals + 1), error)) {
                respond("error " + error);

                return;
            }
        }

        respondCall(publishEvents(*endpoint, events), "dxfg_v4_DXPublisher_publishEvents");
    }

    // publish-burst <ep> <Type> <symbol> <n>
    void publishBurst(const std::vector<std::string> &tokens) {
        Endpoint *endpoint = tokens.size() == 5 ? findEndpoint(tokens[1]) : nullptr;
        const EventType *type = tokens.size() == 5 ? findType(tokens[2]) : nullptr;
        const auto count = tokens.size() == 5 ? parseInt64(tokens[4]) : std::nullopt;

        if (endpoint == nullptr || endpoint->publisher == nullptr || type == nullptr || type->counter == nullptr ||
            !count || *count < 0) {
            respond("error usage: publish-burst <endpoint> <Quote|TimeAndSale> <symbol> <n>");

            return;
        }

        for (std::int64_t first = 1; first <= *count; first += BURST_BATCH) {
            std::vector<std::unique_ptr<OwnedEvent>> events;

            for (std::int64_t i = first; i <= *count && i < first + BURST_BATCH; ++i) {
                std::string error;

                events.push_back(std::make_unique<OwnedEvent>(*type));

                if (!events.back()->set("event_symbol", tokens[3], error) ||
                    !events.back()->set(type->counter, std::to_string(i), error)) {
                    respond("error " + error);

                    return;
                }
            }

            if (!publishEvents(*endpoint, events)) {
                respondCall(false, "dxfg_v4_DXPublisher_publishEvents");

                return;
            }
        }

        respond("ok");
    }

    // The timeout and the limit= of poll and poll-async; false if one is bad.
    static bool pollArguments(const std::vector<std::string> &tokens, std::int64_t &timeout, std::int32_t &limit) {
        const auto parsedTimeout = tokens.size() >= 3 ? parseInt64(tokens[2]) : std::nullopt;
        const auto limitText = option(tokens, 3, "limit");
        const auto parsedLimit = limitText ? parseInt64(*limitText) : std::optional<std::int64_t>(0);

        // A negative limit is passed on: the SDK fails the poll (the outcome fail).
        if (!parsedTimeout || !parsedLimit || *parsedLimit < INT32_MIN || *parsedLimit > INT32_MAX) {
            return false;
        }

        timeout = *parsedTimeout;
        limit = static_cast<std::int32_t>(*parsedLimit);

        return true;
    }

    // poll <q> <timeout> [limit=<n>] [hold=<list>]
    void poll(const std::vector<std::string> &tokens) {
        Queue *queue = tokens.size() >= 3 ? findQueue(tokens[1]) : nullptr;
        const auto hold = option(tokens, 3, "hold");
        std::int64_t timeout = 0;
        std::int32_t limit = 0;

        if (queue == nullptr || queue->queue == nullptr || !pollArguments(tokens, timeout, limit) ||
            (hold && held_.count(*hold) > 0)) {
            respond("error usage: poll <queue> <timeout> [limit=<n>] [hold=<new list>]");

            return;
        }

        dxfg_v4_event_type_list *list = nullptr;
        std::int64_t dropped = 0;

        respondLines(pollBlock(thread_, queue->queue, timeout, limit, hold ? &list : nullptr, &dropped));

        if (list != nullptr) {
            held_[*hold] = Held{list, dropped};
        }
    }

    // read <list>
    void read(const std::vector<std::string> &tokens) {
        auto found = tokens.size() == 2 ? held_.find(tokens[1]) : held_.end();

        if (found == held_.end()) {
            respond("error usage: read <held list>");

            return;
        }

        const Held &held = found->second;
        std::vector<std::string> lines{"events events dropped=" + std::to_string(held.dropped)};

        for (std::int32_t i = 0; i < held.list->size; ++i) {
            lines.push_back(formatEvent(held.list->elements[i]));
        }

        lines.emplace_back("end");
        respondLines(lines);
    }

    // release <list> [thread=worker]
    void release(const std::vector<std::string> &tokens) {
        auto found = tokens.size() >= 2 ? held_.find(tokens[1]) : held_.end();
        const auto thread = option(tokens, 2, "thread");

        if (found == held_.end() || (thread && *thread != "worker")) {
            respond("error usage: release <held list> [thread=worker]");

            return;
        }

        dxfg_v4_event_type_list *list = found->second.list;

        held_.erase(found);

        // Any attached thread may release a list, not only the one that polled it.
        const auto releaseOn = [list](graal_isolatethread_t *on) {
            return dxfg_v4_event_type_list_release(on, list) == 0
                       ? std::string()
                       : "dxfg_v4_event_type_list_release failed: " + takeException(on);
        };
        const std::string failure = thread ? worker_.call(releaseOn) : releaseOn(thread_);

        respond(failure.empty() ? "ok" : "error " + failure);
    }

    // poll-async <q> <timeout> [limit=<n>]
    void pollAsync(const std::vector<std::string> &tokens) {
        Queue *queue = tokens.size() >= 3 ? findQueue(tokens[1]) : nullptr;
        std::int64_t timeout = 0;
        std::int32_t limit = 0;

        if (queue == nullptr || queue->queue == nullptr || !pollArguments(tokens, timeout, limit)) {
            respond("error usage: poll-async <queue> <timeout> [limit=<n>]");

            return;
        }

        if (queue->async) {
            respond("error the previous poll-async of the queue was not awaited");

            return;
        }

        auto async = std::make_shared<AsyncPoll>();
        dxfg_v4_event_queue_t *polled = queue->queue;

        queue->async = async;
        worker_.post([async, polled, timeout, limit](graal_isolatethread_t *thread) {
            auto lines = pollBlock(thread, polled, timeout, limit, nullptr, nullptr);
            std::lock_guard<std::mutex> lock(async->mutex);

            async->lines = std::move(lines);
            async->done = true;
            async->ended.notify_all();
        });
        respond("ok");
    }

    // await <q> <timeout>
    void await(const std::vector<std::string> &tokens) {
        Queue *queue = tokens.size() == 3 ? findQueue(tokens[1]) : nullptr;
        const auto timeout = tokens.size() == 3 ? parseInt64(tokens[2]) : std::nullopt;

        if (queue == nullptr || !timeout || !queue->async) {
            respond("error usage: await <queue with a poll-async> <timeout>");

            return;
        }

        std::shared_ptr<AsyncPoll> async = queue->async;
        std::unique_lock<std::mutex> lock(async->mutex);
        const auto done = [&async] {
            return async->done;
        };

        if (*timeout < 0) {
            async->ended.wait(lock, done);
        } else {
            async->ended.wait_for(lock, std::chrono::milliseconds(*timeout), done);
        }

        if (!async->done) {
            respond("pending");

            return;
        }

        respondLines(async->lines);
        queue->async.reset();
    }

    // ready-arm <q>
    void readyArm(const std::vector<std::string> &tokens) {
        Queue *queue = tokens.size() == 2 ? findQueue(tokens[1]) : nullptr;

        if (queue == nullptr || queue->queue == nullptr) {
            respond("error usage: ready-arm <queue>");

            return;
        }

        auto object = std::make_unique<ReadyObject>();

        if (!object->valid()) {
            respond("error the ready object could not be created");

            return;
        }

        if (dxfg_v4_EventQueue_setReadySignal(thread_, queue->queue,
                                              static_cast<dxfg_v4_ready_signal_kind_t>(ReadyObject::kind()),
                                              object->handle()) != 0) {
            respondCall(false, "dxfg_v4_EventQueue_setReadySignal");

            return;
        }

        // The SDK no longer writes to an older object once the call has returned, so it may be closed now.
        queue->ready = std::move(object);
        respond("ok");
    }

    // ready-wait <q> <timeout>
    void readyWait(const std::vector<std::string> &tokens) {
        Queue *queue = tokens.size() == 3 ? findQueue(tokens[1]) : nullptr;
        const auto timeout = tokens.size() == 3 ? parseInt64(tokens[2]) : std::nullopt;

        if (queue == nullptr || !queue->ready || !timeout) {
            respond("error usage: ready-wait <armed queue> <timeout>");

            return;
        }

        const int milliseconds = *timeout < 0 ? -1 : *timeout > INT_MAX ? INT_MAX : static_cast<int>(*timeout);

        respond(queue->ready->waitAndReset(milliseconds) ? "signaled" : "timeout");
    }

    // Closes and releases everything the requests created; the first failure, or an empty string.
    std::string releaseAll() {
        std::string failure;
        const auto check = [this, &failure](bool succeeded, const std::string &what) {
            if (!succeeded) {
                const std::string reason = takeException(thread_);

                if (failure.empty()) {
                    failure = what + " failed: " + reason;
                }
            }
        };

        // Closing the queues first ends a poll that waits on the worker (DXFG_EVENT_QUEUE_CLOSED).
        for (auto &entry : queues_) {
            if (entry.second.queue != nullptr) {
                check(dxfg_v4_EventQueue_close(thread_, entry.second.queue) == 0, "the close of " + entry.first);
            }
        }

        if (started_) {
            worker_.waitIdle();
        }

        for (auto &entry : held_) {
            check(dxfg_v4_event_type_list_release(thread_, entry.second.list) == 0, "the release of " + entry.first);
        }

        held_.clear();

        for (auto &entry : queues_) {
            Queue &queue = entry.second;

            if (queue.queue != nullptr) {
                check(dxfg_v4_JavaObjectHandler_release(thread_, &queue.queue->handler) == 0,
                      "the release of the queue " + entry.first);
            }

            check(dxfg_v4_DXFeedSubscription_close(thread_, queue.subscription) == 0,
                  "the close of the subscription of " + entry.first);
            check(dxfg_v4_JavaObjectHandler_release(thread_, &queue.subscription->handler) == 0,
                  "the release of the subscription of " + entry.first);
        }

        // The ready objects are closed here, after their queues.
        queues_.clear();

        for (auto &entry : endpoints_) {
            Endpoint &endpoint = entry.second;

            if (endpoint.publisher != nullptr) {
                check(dxfg_v4_JavaObjectHandler_release(thread_, &endpoint.publisher->handler) == 0,
                      "the release of the publisher of " + entry.first);
            }

            if (endpoint.feed != nullptr) {
                check(dxfg_v4_JavaObjectHandler_release(thread_, &endpoint.feed->handler) == 0,
                      "the release of the feed of " + entry.first);
            }

            check(dxfg_v4_DXEndpoint_close(thread_, endpoint.endpoint) == 0, "the close of " + entry.first);
            check(dxfg_v4_JavaObjectHandler_release(thread_, &endpoint.endpoint->handler) == 0,
                  "the release of " + entry.first);
        }

        endpoints_.clear();

        return failure;
    }

    // teardown: the recommended shutdown sequence of dxfg_v4_system_shutdown(), then the isolate is torn down.
    void teardown() {
        respond("ok");

        if (!started_) {
            return;
        }

        // Every poll returns DXFG_EVENT_QUEUE_CLOSED, so the worker can leave the SDK.
        if (dxfg_v4_system_shutdown(thread_) != 0) {
            std::cerr << "adapter: dxfg_v4_system_shutdown failed: " << takeException(thread_) << std::endl;
        }

        const std::string failure = releaseAll();

        if (!failure.empty()) {
            std::cerr << "adapter: " << failure << std::endl;
        }

        // graal_tear_down_isolate() waits for the other attached threads to detach.
        worker_.stop();

        if (graal_tear_down_isolate(thread_) != 0) {
            std::cerr << "adapter: graal_tear_down_isolate failed" << std::endl;
        }
    }

    void handle(const std::vector<std::string> &tokens) {
        const std::string &word = tokens[0];

        if (word == "config") {
            config(tokens);
        } else if (word == "start") {
            start();
        } else if (!started_) {
            respond("error not started");
        } else if (word == "feature") {
            feature(tokens);
        } else if (word == "endpoint") {
            endpoint(tokens);
        } else if (word == "close-endpoint") {
            Endpoint *endpoint = tokens.size() == 2 ? findEndpoint(tokens[1]) : nullptr;

            if (endpoint == nullptr) {
                respond("error usage: close-endpoint <endpoint>");
            } else {
                respondCall(dxfg_v4_DXEndpoint_close(thread_, endpoint->endpoint) == 0, "dxfg_v4_DXEndpoint_close");
            }
        } else if (word == "queue") {
            queue(tokens);
        } else if (word == "subscribe") {
            subscribe(tokens);
        } else if (word == "publish") {
            publish(tokens);
        } else if (word == "publish-burst") {
            publishBurst(tokens);
        } else if (word == "poll") {
            poll(tokens);
        } else if (word == "read") {
            read(tokens);
        } else if (word == "release") {
            release(tokens);
        } else if (word == "poll-async") {
            pollAsync(tokens);
        } else if (word == "await") {
            await(tokens);
        } else if (word == "wakeup" || word == "close-queue") {
            Queue *queue = tokens.size() == 2 ? findQueue(tokens[1]) : nullptr;

            if (queue == nullptr || queue->queue == nullptr) {
                respond("error usage: " + word + " <queue>");
            } else if (word == "wakeup") {
                respondCall(dxfg_v4_EventQueue_wakeUp(thread_, queue->queue) == 0, "dxfg_v4_EventQueue_wakeUp");
            } else {
                respondCall(dxfg_v4_EventQueue_close(thread_, queue->queue) == 0, "dxfg_v4_EventQueue_close");
            }
        } else if (word == "ready-arm") {
            readyArm(tokens);
        } else if (word == "ready-wait") {
            readyWait(tokens);
        } else if (word == "shutdown") {
            respondCall(dxfg_v4_system_shutdown(thread_) == 0, "dxfg_v4_system_shutdown");
        } else if (word == "reset") {
            const std::string failure = releaseAll();

            respond(failure.empty() ? "ok" : "error " + failure);
        } else {
            respond("error unknown request " + word);
        }
    }

  public:
    explicit Adapter(FILE *out) : out_(out) {
    }

    int run() {
        respond(HELLO);

        std::string request;

        while (std::getline(std::cin, request)) {
            if (!request.empty() && request.back() == '\r') {
                request.pop_back();
            }

            std::cerr << "adapter: request " << request << std::endl;

            const auto tokens = split(request);

            if (tokens.empty()) {
                respond("error empty request");
            } else if (tokens[0] == "teardown") {
                teardown();

                return 0;
            } else {
                handle(tokens);
            }
        }

        // EOF: release what is held and exit.
        const std::string failure = releaseAll();

        if (!failure.empty()) {
            std::cerr << "adapter: " << failure << std::endl;
        }

        worker_.stop();

        return 0;
    }
};

} // namespace

} // namespace kit
} // namespace dxfg

int main() {
    FILE *out = dxfg::kit::takeStdout();

    if (out == nullptr) {
        std::cerr << "adapter: cannot take stdout for the protocol" << std::endl;

        return 70;
    }

    return dxfg::kit::Adapter(out).run();
}
