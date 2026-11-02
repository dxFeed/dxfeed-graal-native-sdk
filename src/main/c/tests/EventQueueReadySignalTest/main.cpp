// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The ready signal of an event queue through the C API (dxfg_v4_EventQueue_setReadySignal()): the host waits for an OS
// object of its own (an eventfd or a pipe on Linux, a pipe on macOS, an event on Windows), resets it and polls with the
// timeout 0. The cases cover the events, a poll that stops at its limit, the wake-up, the close, the removal and the
// replacement of the signal, a signal set while work is pending, the refused objects and, on Linux and macOS, a pipe
// whose read end is closed.

#ifdef _WIN32
#    ifndef WIN32_LEAN_AND_MEAN
#        define WIN32_LEAN_AND_MEAN
#    endif
#    ifndef NOMINMAX
#        define NOMINMAX
#    endif
#    include <windows.h>
#else
#    include <fcntl.h>
#    include <poll.h>
#    include <unistd.h>

#    include <cerrno>

#    ifdef __linux__
#        include <sys/eventfd.h>
#    endif
#endif

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <algorithm>
#include <chrono>
#include <cstdint>
#include <initializer_list>
#include <iterator>
#include <memory>
#include <set>
#include <string>
#include <thread>
#include <utility>
#include <vector>

namespace dxfg {
namespace test {

namespace {

// The wait for a signal that has to come: it returns as soon as the object is ready.
constexpr int READY_TIMEOUT_MS = 10'000;
// The wait for a signal that must not come.
constexpr int NOT_READY_TIMEOUT_MS = 300;
constexpr int64_t POLL_TIMEOUT_MS = 10'000;
constexpr auto RETURN_TIMEOUT = std::chrono::seconds(10);
constexpr auto SETTLE_TIME = std::chrono::milliseconds(300);

// The objects of the platform that a host gives to a queue.
enum class Mode { EVENTFD, PIPE, EVENT };

#ifdef _WIN32
constexpr Mode MODES[] = {Mode::EVENT};
constexpr dxfg_v4_ready_signal_kind_t OWN_KIND = DXFG_V4_READY_SIGNAL_WINDOWS_EVENT;
constexpr dxfg_v4_ready_signal_kind_t OTHER_KIND = DXFG_V4_READY_SIGNAL_FD;
#elif defined(__linux__)
constexpr Mode MODES[] = {Mode::EVENTFD, Mode::PIPE};
constexpr dxfg_v4_ready_signal_kind_t OWN_KIND = DXFG_V4_READY_SIGNAL_FD;
constexpr dxfg_v4_ready_signal_kind_t OTHER_KIND = DXFG_V4_READY_SIGNAL_WINDOWS_EVENT;
#else
constexpr Mode MODES[] = {Mode::PIPE};
constexpr dxfg_v4_ready_signal_kind_t OWN_KIND = DXFG_V4_READY_SIGNAL_FD;
constexpr dxfg_v4_ready_signal_kind_t OTHER_KIND = DXFG_V4_READY_SIGNAL_WINDOWS_EVENT;
#endif

// The value 3 is not a kind (the kinds are 0 to 2), and it is a value of the enum for the compiler.
constexpr auto UNKNOWN_KIND = static_cast<dxfg_v4_ready_signal_kind_t>(3);

const char *modeName(Mode mode) {
    switch (mode) {
    case Mode::EVENTFD:
        return "eventfd";
    case Mode::PIPE:
        return "pipe";
    default:
        return "event";
    }
}

#ifndef _WIN32
// A pipe: fds[0] is the read end, fds[1] the write end; both ends are non-blocking if the flag is set.
bool makePipe(int (&fds)[2], bool nonBlocking) {
#    ifdef __linux__
    return pipe2(fds, nonBlocking ? O_NONBLOCK : 0) == 0;
#    else
    if (pipe(fds) != 0) {
        return false;
    }

    for (const int fd : fds) {
        if (nonBlocking) {
            const int flags = fcntl(fd, F_GETFL);

            if (flags < 0 || fcntl(fd, F_SETFL, flags | O_NONBLOCK) != 0) {
                close(fds[0]);
                close(fds[1]);

                return false;
            }
        }
    }

    return true;
#    endif
}
#endif

// The OS object of the host: an object that the SDK makes ready, which the test waits for and resets. The object is
// closed by the destructor, so a test closes its queue first.
class ReadyObject {
#ifdef _WIN32
    HANDLE event_ = nullptr;
#else
    int readFd_ = -1;
    int writeFd_ = -1;
#endif

  public:
    explicit ReadyObject([[maybe_unused]] Mode mode) {
#ifdef _WIN32
        // Auto-reset: a wait resets it.
        event_ = CreateEventW(nullptr, FALSE, FALSE, nullptr);
#else
#    ifdef __linux__
        if (mode == Mode::EVENTFD) {
            readFd_ = writeFd_ = eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC);

            return;
        }
#    endif
        int fds[2];

        if (mode == Mode::PIPE && makePipe(fds, true)) {
            readFd_ = fds[0];
            writeFd_ = fds[1];
        }
#endif
    }

    ReadyObject(const ReadyObject &) = delete;
    ReadyObject &operator=(const ReadyObject &) = delete;

    ~ReadyObject() {
#ifdef _WIN32
        if (event_ != nullptr) {
            CloseHandle(event_);
        }
#else
        closeReadEnd();

        if (writeFd_ >= 0) {
            close(writeFd_);
        }
#endif
    }

    bool valid() const {
#ifdef _WIN32
        return event_ != nullptr;
#else
        return writeFd_ >= 0;
#endif
    }

    // What the host passes to the SDK: the descriptor (the write end of a pipe) or the HANDLE.
    int64_t handle() const {
#ifdef _WIN32
        return static_cast<int64_t>(reinterpret_cast<intptr_t>(event_));
#else
        return writeFd_;
#endif
    }

    // Waits for the object, then resets it: a poll of the descriptor, then a read until EAGAIN (an eventfd gives its
    // counter at once); a wait for the auto-reset event. Returns false if the object is not ready within the timeout.
    bool waitAndReset(int timeoutMs) {
#ifdef _WIN32
        return WaitForSingleObject(event_, static_cast<DWORD>(timeoutMs)) == WAIT_OBJECT_0;
#else
        pollfd descriptor{readFd_, POLLIN, 0};
        int result;

        do {
            result = ::poll(&descriptor, 1, timeoutMs);
        } while (result < 0 && errno == EINTR);

        if (result <= 0 || (descriptor.revents & POLLIN) == 0) {
            return false;
        }

        char buffer[64];

        while (read(readFd_, buffer, sizeof buffer) > 0) {
        }

        return true;
#endif
    }

#ifndef _WIN32
    // Closes the read end of a pipe: the SDK writes to a pipe that nobody reads. The object cannot be waited for after.
    void closeReadEnd() {
        if (readFd_ >= 0 && readFd_ != writeFd_) {
            close(readFd_);
        }

        readFd_ = -1;
    }
#endif
};

// The isolate of all the cases; the hubs and the queues of the cases are closed by the cases.
Isolate &isolate() {
    static Isolate instance;

    return instance;
}

bool publishQuotes(Hub &hub, const std::vector<std::pair<const char *, double>> &quotes) {
    std::vector<dxfg_v4_quote_t> events(quotes.size());
    std::vector<dxfg_v4_event_type_t *> pointers{};

    for (std::size_t i = 0; i < quotes.size(); ++i) {
        events[i].market_event.event_type.clazz = DXFG_EVENT_QUOTE;
        events[i].market_event.event_symbol = quotes[i].first;
        events[i].bid_price = quotes[i].second;
        pointers.push_back(&events[i].market_event.event_type);
    }

    return hub.publish(pointers.data(), static_cast<int32_t>(pointers.size()));
}

std::vector<std::string> symbolsOf(const dxfg_v4_event_type_list *events) {
    std::vector<std::string> symbols{};

    for (int32_t i = 0; i < events->size; ++i) {
        symbols.emplace_back(reinterpret_cast<dxfg_v4_quote_t *>(events->elements[i])->market_event.event_symbol);
    }

    return symbols;
}

// A Quote subscription of the feed; closed and released at the end.
struct Subscription {
    graal_isolatethread_t *thread;
    dxfg_v4_subscription_t *subscription = nullptr;

    explicit Subscription(Hub &hub) : thread(hub.thread) {
        dxfg_v4_DXFeed_createSubscription(thread, hub.feed, DXFG_EVENT_QUOTE, &subscription);
    }

    Subscription(const Subscription &) = delete;
    Subscription &operator=(const Subscription &) = delete;

    ~Subscription() {
        if (subscription != nullptr) {
            dxfg_v4_DXFeedSubscription_close(thread, subscription);
            dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
        }
    }

    bool addSymbol(const char *symbol) {
        dxfg_v4_string_symbol_t stringSymbol{{STRING}, symbol};

        return subscription != nullptr &&
               dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &stringSymbol.supper) == 0;
    }
};

// A host: the object, a LOCAL_HUB and a queue, closed by the destructor before the object (the stop order of the
// contract, the loop closes the object after the queue). The queue is of one subscription (symbols A, B and C), or
// shared by the subscriptions that the case attaches.
struct Fixture {
    ReadyObject object;
    Hub hub;
    graal_isolatethread_t *thread;
    dxfg_v4_event_queue_t *queue = nullptr;
    Subscription single;
    std::vector<std::unique_ptr<Subscription>> attached{};
    bool ready = false;

    Fixture(Mode mode, bool shared) : object(mode), hub(isolate().thread), thread(hub.thread), single(hub) {
        if (shared) {
            ready = object.valid() && hub.ready && dxfg_v4_EventQueue_create(thread, &queue) == 0;
        } else {
            ready = object.valid() && hub.ready && single.subscription != nullptr &&
                    dxfg_v4_EventQueue_new(thread, single.subscription, &queue) == 0 && single.addSymbol("A") &&
                    single.addSymbol("B") && single.addSymbol("C");
        }
    }

    Fixture(const Fixture &) = delete;
    Fixture &operator=(const Fixture &) = delete;

    ~Fixture() {
        if (queue != nullptr) {
            dxfg_v4_EventQueue_close(thread, queue);
        }

        attached.clear();

        if (queue != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &queue->handler);
        }
    }

    // Attaches a new subscription of the symbol with the tag to the shared queue.
    bool attach(const char *symbol, int64_t tag) {
        auto &added = attached.emplace_back(std::make_unique<Subscription>(hub));

        return added->subscription != nullptr && dxfg_v4_EventQueue_attach(thread, queue, added->subscription, tag) == 0 &&
               added->addSymbol(symbol);
    }

    int32_t setSignal(ReadyObject &signal) const {
        return dxfg_v4_EventQueue_setReadySignal(thread, queue, OWN_KIND, signal.handle());
    }

    int32_t setSignal() {
        return setSignal(object);
    }

    int32_t removeSignal() const {
        return dxfg_v4_EventQueue_setReadySignal(thread, queue, DXFG_V4_READY_SIGNAL_NONE, 0);
    }

    // One poll with the timeout 0 and the limit; the result is the one of the poll, the symbols are added to the list.
    int32_t poll(std::vector<std::string> &symbols, int32_t limit = 0, int64_t timeoutMs = 0) const {
        dxfg_v4_event_type_list *events = nullptr;
        const int32_t result = dxfg_v4_EventQueue_poll(thread, queue, timeoutMs, limit, &events);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            const auto batch = symbolsOf(events);

            symbols.insert(symbols.end(), batch.begin(), batch.end());
            dxfg_v4_event_type_list_release(thread, events);
        }

        return result;
    }

    // The loop of a host: wait for the object, reset it, poll with the timeout 0; until the number of events or the
    // time is out.
    std::vector<std::string> pollWhenReady(std::size_t count) {
        std::vector<std::string> symbols{};

        for (auto end = std::chrono::steady_clock::now() + RETURN_TIMEOUT;
             symbols.size() < count && std::chrono::steady_clock::now() < end;) {
            if (object.waitAndReset(READY_TIMEOUT_MS)) {
                poll(symbols);
            }
        }

        return symbols;
    }

    // Polls until the queue has no events within 200 ms: the events that the queue holds, without the object.
    std::vector<std::string> pollAll() const {
        std::vector<std::string> symbols{};

        while (poll(symbols, 0, 200) == DXFG_EXECUTE_SUCCESSFULLY) {
        }

        return symbols;
    }
};

// The Java exception class of the last failed call of the thread, or an empty string if there is none.
std::string exceptionOf(const Fixture &fixture) {
    return takeExceptionClassName(fixture.thread);
}

bool returnsWithin(Attached &attached) {
    for (auto end = std::chrono::steady_clock::now() + RETURN_TIMEOUT; std::chrono::steady_clock::now() < end;) {
        if (attached.returned()) {
            return true;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    return attached.returned();
}

const std::vector<std::string> ABC{"A", "B", "C"};

} // namespace

TEST_CASE("A queue makes the object ready when events arrive: wait, reset, poll with the timeout 0") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK(exceptionOf(host).empty());

            // No work, no signal.
            CHECK_FALSE(host.object.waitAndReset(NOT_READY_TIMEOUT_MS));

            // The first poll of a host that waits for the object returns the events.
            REQUIRE(publishQuotes(host.hub, {{"A", 1}, {"B", 2}, {"C", 3}}));
            REQUIRE(host.object.waitAndReset(READY_TIMEOUT_MS));

            std::vector<std::string> symbols{};

            REQUIRE(host.poll(symbols) == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK_FALSE(symbols.empty());

            // The events arrive in one delivery or more: the host goes on with its loop.
            for (auto end = std::chrono::steady_clock::now() + RETURN_TIMEOUT;
                 symbols.size() < ABC.size() && std::chrono::steady_clock::now() < end;) {
                if (host.object.waitAndReset(READY_TIMEOUT_MS)) {
                    host.poll(symbols);
                }
            }

            CHECK(symbols == ABC);
            CHECK(host.poll(symbols) == DXFG_EVENT_QUEUE_NO_EVENTS);

            // Each poll arms the queue again: the next events make the object ready again.
            REQUIRE(publishQuotes(host.hub, {{"B", 4}}));
            CHECK(host.pollWhenReady(1) == std::vector<std::string>{"B"});
            REQUIRE(publishQuotes(host.hub, {{"C", 5}}));
            CHECK(host.pollWhenReady(1) == std::vector<std::string>{"C"});
            CHECK(exceptionOf(host).empty());
        }
    }
}

TEST_CASE("A poll that stops at its limit makes the object ready again while work is left") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, true);

            REQUIRE(host.ready);

            // Three subscriptions on one queue: three tasks, since a single retrieval is not split by the limit.
            REQUIRE(host.attach("A", 1));
            REQUIRE(host.attach("B", 2));
            REQUIRE(host.attach("C", 3));
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(publishQuotes(host.hub, {{"A", 1}, {"B", 2}, {"C", 3}}));
            REQUIRE(host.object.waitAndReset(READY_TIMEOUT_MS));

            // The other deliveries queue their tasks without a signal; they arrive in the queue during the settle time.
            // A delivery that comes later finds the queue armed and signals by itself, so the loop below holds either
            // way.
            std::this_thread::sleep_for(SETTLE_TIME);

            std::vector<std::string> symbols{};

            for (int i = 1; i <= 3; ++i) {
                INFO("poll " << i);
                REQUIRE(host.poll(symbols, 1) == DXFG_EXECUTE_SUCCESSFULLY);
                CHECK(symbols.size() == static_cast<std::size_t>(i));

                if (i < 3) {
                    // The events remain: the poll signals before it returns.
                    REQUIRE(host.object.waitAndReset(READY_TIMEOUT_MS));
                }
            }

            CHECK(std::set<std::string>(symbols.begin(), symbols.end()) ==
                  std::set<std::string>(ABC.begin(), ABC.end()));

            // Nothing is left: the object is not ready, and the next poll has no events.
            CHECK_FALSE(host.object.waitAndReset(NOT_READY_TIMEOUT_MS));
            CHECK(host.poll(symbols, 1) == DXFG_EVENT_QUEUE_NO_EVENTS);
            CHECK(symbols.size() == 3);
            CHECK(exceptionOf(host).empty());
        }
    }
}

TEST_CASE("A wake-up makes the object ready, the poll returns no events") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK_FALSE(host.object.waitAndReset(NOT_READY_TIMEOUT_MS));

            REQUIRE(dxfg_v4_EventQueue_wakeUp(host.thread, host.queue) == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(host.object.waitAndReset(READY_TIMEOUT_MS));

            std::vector<std::string> symbols{};

            // The wake-up is consumed by this poll: without it the poll would wait for its timeout.
            CHECK(host.poll(symbols, 0, POLL_TIMEOUT_MS) == DXFG_EVENT_QUEUE_NO_EVENTS);
            CHECK(symbols.empty());
            CHECK(exceptionOf(host).empty());

            // The queue is armed again.
            REQUIRE(dxfg_v4_EventQueue_wakeUp(host.thread, host.queue) == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK(host.object.waitAndReset(READY_TIMEOUT_MS));
            CHECK(host.poll(symbols) == DXFG_EVENT_QUEUE_NO_EVENTS);
        }
    }
}

TEST_CASE("The close of a queue on another thread makes the object ready, the poll returns closed") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK_FALSE(host.object.waitAndReset(NOT_READY_TIMEOUT_MS));

            auto *queue = host.queue;
            Attached closer(isolate().isolate,
                            [queue](graal_isolatethread_t *thread) { return dxfg_v4_EventQueue_close(thread, queue); });

            REQUIRE(returnsWithin(closer));
            CHECK(closer.join() == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(host.object.waitAndReset(READY_TIMEOUT_MS));

            std::vector<std::string> symbols{};
            dxfg_v4_event_type_list *events = reinterpret_cast<dxfg_v4_event_type_list *>(1);

            CHECK(dxfg_v4_EventQueue_poll(host.thread, host.queue, 0, 0, &events) == DXFG_EVENT_QUEUE_CLOSED);
            CHECK(events == nullptr);
            CHECK(host.poll(symbols, 0, POLL_TIMEOUT_MS) == DXFG_EVENT_QUEUE_CLOSED);
            CHECK(exceptionOf(host).empty());
        }
    }
}

TEST_CASE("The close of a queue on another thread during a poll makes the object ready, the next poll returns closed") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);

            // The host is inside a poll that waits for events.
            auto *queue = host.queue;
            Attached poller(isolate().isolate, [queue](graal_isolatethread_t *thread) {
                dxfg_v4_event_type_list *events = nullptr;
                const int32_t result = dxfg_v4_EventQueue_poll(thread, queue, POLL_TIMEOUT_MS, 0, &events);

                if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                    dxfg_v4_event_type_list_release(thread, events);
                }

                return result;
            });

            std::this_thread::sleep_for(SETTLE_TIME);
            REQUIRE_FALSE(poller.returned());

            Attached closer(isolate().isolate,
                            [queue](graal_isolatethread_t *thread) { return dxfg_v4_EventQueue_close(thread, queue); });

            REQUIRE(returnsWithin(closer));
            CHECK(closer.join() == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(returnsWithin(poller));
            CHECK(poller.join() == DXFG_EVENT_QUEUE_CLOSED);

            // The close signals once for the loop, whatever the poll in progress returned.
            REQUIRE(host.object.waitAndReset(READY_TIMEOUT_MS));

            std::vector<std::string> symbols{};

            CHECK(host.poll(symbols) == DXFG_EVENT_QUEUE_CLOSED);
            CHECK(exceptionOf(host).empty());
        }
    }
}

TEST_CASE("After the removal of the signal the object stays not ready, polling still returns the events") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(host.removeSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK(exceptionOf(host).empty());

            REQUIRE(publishQuotes(host.hub, {{"A", 1}, {"B", 2}, {"C", 3}}));
            CHECK_FALSE(host.object.waitAndReset(NOT_READY_TIMEOUT_MS));
            CHECK(host.pollAll() == ABC);

            // Removing a signal that is not set is not an error.
            CHECK(host.removeSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK(exceptionOf(host).empty());
        }
    }
}

TEST_CASE("A new signal replaces the old one") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            // Declared first: closed after the queue, as the contract says.
            ReadyObject other(mode);
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(other.valid());
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(host.setSignal(other) == DXFG_EXECUTE_SUCCESSFULLY);

            REQUIRE(publishQuotes(host.hub, {{"A", 1}}));
            REQUIRE(other.waitAndReset(READY_TIMEOUT_MS));
            CHECK_FALSE(host.object.waitAndReset(NOT_READY_TIMEOUT_MS));

            std::vector<std::string> symbols{};

            CHECK(host.poll(symbols) == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK(symbols == std::vector<std::string>{"A"});
            CHECK(exceptionOf(host).empty());
        }
    }
}

TEST_CASE("A signal set while events are pending makes the object ready") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(publishQuotes(host.hub, {{"A", 1}}));
            std::this_thread::sleep_for(SETTLE_TIME);

            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(host.object.waitAndReset(READY_TIMEOUT_MS));

            std::vector<std::string> symbols{};

            CHECK(host.poll(symbols) == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK(symbols == std::vector<std::string>{"A"});
            CHECK(exceptionOf(host).empty());
        }
    }
}

TEST_CASE("A signal set while a wake-up is pending makes the object ready before the call returns") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(dxfg_v4_EventQueue_wakeUp(host.thread, host.queue) == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);
            CHECK(host.object.waitAndReset(0));

            std::vector<std::string> symbols{};

            CHECK(host.poll(symbols) == DXFG_EVENT_QUEUE_NO_EVENTS);
            CHECK(exceptionOf(host).empty());
        }
    }
}

TEST_CASE("An object that is not usable is refused with an IllegalArgumentException, the signal stays") {
    for (const Mode mode : MODES) {
        SUBCASE(modeName(mode)) {
            Fixture host(mode, false);

            REQUIRE(host.ready);
            REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);

            const auto refused = [&host](dxfg_v4_ready_signal_kind_t kind, int64_t handle) {
                INFO("kind " << static_cast<int>(kind) << ", handle " << handle);
                CHECK(dxfg_v4_EventQueue_setReadySignal(host.thread, host.queue, kind, handle) == DXFG_EXECUTE_FAIL);
                CHECK(exceptionOf(host) == "java.lang.IllegalArgumentException");
            };

        #ifdef _WIN32
            // The kind of another OS.
            refused(OTHER_KIND, host.object.handle());

            // Not an event handle.
            refused(OWN_KIND, 0);
            refused(OWN_KIND, -1);
        #else
            // The kind of another OS.
            refused(OTHER_KIND, host.object.handle());

            // A blocking pipe.
            int blocking[2];

            REQUIRE(makePipe(blocking, false));
            refused(OWN_KIND, blocking[1]);
            close(blocking[0]);
            close(blocking[1]);

            // The read end of a non-blocking pipe.
            int pipeFds[2];

            REQUIRE(makePipe(pipeFds, true));
            refused(OWN_KIND, pipeFds[0]);
            close(pipeFds[0]);

            // A closed descriptor.
            const int closedFd = pipeFds[1];

            close(closedFd);
            refused(OWN_KIND, closedFd);

            // Out of the range of descriptors.
            refused(OWN_KIND, static_cast<int64_t>(INT32_MAX) + 1);
            refused(OWN_KIND, -1);
        #endif

            // A kind that does not exist.
            refused(UNKNOWN_KIND, host.object.handle());

            // The refusals left the signal as it was.
            REQUIRE(publishQuotes(host.hub, {{"A", 1}}));
            CHECK(host.pollWhenReady(1) == std::vector<std::string>{"A"});
        }
    }
}

#ifndef _WIN32
TEST_CASE("A pipe whose read end is closed does not stop the process or the queue") {
    Fixture host(Mode::PIPE, false);

    REQUIRE(host.ready);
    REQUIRE(host.setSignal() == DXFG_EXECUTE_SUCCESSFULLY);

    // The write of the SDK fails with EPIPE: the signal is raised by the kernel and the isolate ignores it.
    host.object.closeReadEnd();
    REQUIRE(publishQuotes(host.hub, {{"A", 1}}));
    std::this_thread::sleep_for(SETTLE_TIME);
    CHECK(host.pollAll() == std::vector<std::string>{"A"});

    // The failed write armed the queue again: the next events are signalled too, and polling still works.
    REQUIRE(publishQuotes(host.hub, {{"B", 2}}));
    std::this_thread::sleep_for(SETTLE_TIME);
    CHECK(host.pollAll() == std::vector<std::string>{"B"});
    CHECK(exceptionOf(host).empty());
}
#endif

} // namespace test
} // namespace dxfg
