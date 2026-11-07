// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The OS object that a host gives to an event queue as its ready signal (dxfg_v4_EventQueue_setReadySignal()): an
// eventfd or a non-blocking pipe on Linux, a non-blocking pipe on macOS, an auto-reset event on Windows. The host
// waits for it, resets it and polls with the timeout 0.
//
// The reference adapter of the conformance kit uses it for ready-arm and ready-wait; the test
// EventQueueReadySignalTest of the SDK includes it too.

#ifndef DXFG_KIT_READY_OBJECT_HPP
#define DXFG_KIT_READY_OBJECT_HPP

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

#include <cstdint>

namespace dxfg {
namespace kit {

// The objects of the platforms.
enum class Mode { EVENTFD, PIPE, EVENT };

// The object a host of this platform uses by default.
#ifdef _WIN32
constexpr Mode DEFAULT_MODE = Mode::EVENT;
#elif defined(__linux__)
constexpr Mode DEFAULT_MODE = Mode::EVENTFD;
#else
constexpr Mode DEFAULT_MODE = Mode::PIPE;
#endif

#ifndef _WIN32
// A pipe: fds[0] is the read end, fds[1] the write end; both ends are non-blocking if the flag is set, and both are
// closed on exec.
inline bool makePipe(int (&fds)[2], bool nonBlocking) {
#    ifdef __linux__
    return pipe2(fds, (nonBlocking ? O_NONBLOCK : 0) | O_CLOEXEC) == 0;
#    else
    if (pipe(fds) != 0) {
        return false;
    }

    for (const int fd : fds) {
        const int flags = fcntl(fd, F_GETFL);

        if (fcntl(fd, F_SETFD, FD_CLOEXEC) != 0 || flags < 0 ||
            (nonBlocking && fcntl(fd, F_SETFL, flags | O_NONBLOCK) != 0)) {
            close(fds[0]);
            close(fds[1]);

            return false;
        }
    }

    return true;
#    endif
}
#endif

// The OS object of the host: an object that the SDK makes ready, which the host waits for and resets. The destructor
// closes it, so the host removes the signal or closes the queue first (or sees DXFG_EVENT_QUEUE_CLOSED).
class ReadyObject {
#ifdef _WIN32
    HANDLE event_ = nullptr;
#else
    int readFd_ = -1;
    int writeFd_ = -1;
#endif

  public:
    explicit ReadyObject([[maybe_unused]] Mode mode = DEFAULT_MODE) {
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

    // The kind of the object for dxfg_v4_EventQueue_setReadySignal(), as an int: DXFG_V4_READY_SIGNAL_WINDOWS_EVENT (2)
    // on Windows, DXFG_V4_READY_SIGNAL_FD (1) elsewhere.
    static constexpr int kind() {
#ifdef _WIN32
        return 2;
#else
        return 1;
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

} // namespace kit
} // namespace dxfg

#endif // DXFG_KIT_READY_OBJECT_HPP
