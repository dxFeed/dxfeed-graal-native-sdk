// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <dxfg_api.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <locale>
#include <mutex>
#include <string>
#include <vector>

namespace dxfg {

#define DXFG_MACRO_CONCAT(a, b) DXFG_MACRO_CONCAT_INNER(a, b)
#define DXFG_MACRO_CONCAT_INNER(a, b) a##b

#define DXFG_MACRO_UNIQUE_NAME(base) DXFG_MACRO_CONCAT(base, __LINE__)

void printException(dxfg_exception_t *exception);
void getException(graal_isolatethread_t *isolateThread);
void printEvent(graal_isolatethread_t *isolateThread, const dxfg_event_type_t *event);
void printEvents(graal_isolatethread_t *isolateThread, dxfg_event_type_list *events, void *userData);
void observableListModelListenerCallback(graal_isolatethread_t *isolateThread, dxfg_event_type_list *orders,
                                         void *userData);
void endpointStateChangeListener(graal_isolatethread_t *isolateThread, dxfg_endpoint_state_t oldState,
                                 dxfg_endpoint_state_t new_state, void *userData);
void stateChangeListener(graal_isolatethread_t *isolateThread, dxfg_ipf_connection_state_t oldState,
                         dxfg_ipf_connection_state_t newState, void *userData);
void finalize(graal_isolatethread_t *isolateThread, void *userData);

inline std::string toUpper(const std::string &s, std::locale locale = std::locale()) {
    std::string result(s.size(), ' ');

    std::transform(s.begin(), s.end(), result.begin(), [locale](auto c) {
        return std::toupper(c, locale);
    });

    return result;
}

struct CaseInsensitiveHash {
    std::size_t operator()(const std::string &s) const {
        return std::hash<std::string>()(toUpper(s));
    }
};

struct CaseInsensitiveEq {
    std::size_t operator()(const std::string &s1, const std::string &s2) const {
        return toUpper(s1) == toUpper(s2);
    }
};

namespace detail {
template <typename Func> struct OnScopeExit {
    Func func;

    // ReSharper disable once CppNonExplicitConvertingConstructor
    OnScopeExit(const Func &f) : func(static_cast<Func &&>(f)) { // NOLINT(*-explicit-constructor)
    }

    // ReSharper disable once CppNonExplicitConvertingConstructor
    OnScopeExit(Func &&f) : func(f) { // NOLINT(*-explicit-constructor)
    }

    OnScopeExit(const OnScopeExit &) = delete;

    ~OnScopeExit() {
        func();
    }
};
} // namespace detail

#define finally(...) detail::OnScopeExit DXFG_MACRO_UNIQUE_NAME(ose__) = __VA_ARGS__

struct StopWatch final {
  private:
    mutable std::mutex mutex_{};
    std::chrono::milliseconds elapsed_{};
    std::chrono::steady_clock::time_point startTimeStamp_{};
    std::atomic<bool> isRunning_{};

  public:
    StopWatch() noexcept {
        reset();
    }

    void start() noexcept {
        if (!isRunning_) {
            std::lock_guard<std::mutex> lock{mutex_};
            startTimeStamp_ = std::chrono::steady_clock::now();
            isRunning_ = true;
        }
    }

    void stop() noexcept {
        if (isRunning_) {
            auto endTimestamp = std::chrono::steady_clock::now();

            std::lock_guard<std::mutex> lock{mutex_};
            auto elapsedThisPeriod = endTimestamp - startTimeStamp_;
            elapsed_ += std::chrono::duration_cast<std::chrono::milliseconds>(elapsedThisPeriod);
            isRunning_ = false;
        }
    }

    void reset() noexcept {
        std::lock_guard<std::mutex> lock{mutex_};

        elapsed_ = std::chrono::milliseconds::zero();
        isRunning_ = false;
        startTimeStamp_ = std::chrono::steady_clock::time_point{};
    }

    void restart() noexcept {
        std::lock_guard<std::mutex> lock{mutex_};

        elapsed_ = std::chrono::milliseconds::zero();
        isRunning_ = true;
        startTimeStamp_ = std::chrono::steady_clock::now();
    }

    bool isRunning() const noexcept {
        return isRunning_;
    }

    std::chrono::milliseconds elapsed() const noexcept {
        std::lock_guard<std::mutex> lock{mutex_};

        auto elapsed = elapsed_;

        if (isRunning_) {
            auto currentTimestamp = std::chrono::steady_clock::now();
            auto elapsedUntilNow = currentTimestamp - startTimeStamp_;

            elapsed += std::chrono::duration_cast<std::chrono::milliseconds>(elapsedUntilNow);
        }

        return elapsed;
    }
};

template <std::size_t Bits> struct hashMixImpl;

template <> struct hashMixImpl<64> {
    constexpr static std::uint64_t fn(std::uint64_t x) noexcept {
        std::uint64_t const m = (static_cast<std::uint64_t>(0xe9846af) << 32) + 0x9b1a615d;

        x ^= x >> 32;
        x *= m;
        x ^= x >> 32;
        x *= m;
        x ^= x >> 28;

        return x;
    }
};

template <> struct hashMixImpl<32> {
    constexpr static std::uint32_t fn(std::uint32_t x) noexcept {
        std::uint32_t const m1 = 0x21f0aaad;
        std::uint32_t const m2 = 0x735a2d97;

        x ^= x >> 16;
        x *= m1;
        x ^= x >> 15;
        x *= m2;
        x ^= x >> 15;

        return x;
    }
};

constexpr static std::size_t hashMix(std::size_t v) noexcept {
    return hashMixImpl<sizeof(std::size_t) * CHAR_BIT>::fn(v);
}

template <class T> constexpr void hashCombine(std::size_t &seed, const T &v) noexcept {
    seed = hashMix(seed + 0x9e3779b9 + std::hash<T>()(v));
}

inline std::size_t getHash(char *str) {
    std::size_t seed = 0;

    if (str == nullptr) {
        return seed;
    }

    auto length = std::strlen(str);

    for (std::size_t i = 0; i < length; i++) {
        hashCombine(seed, str[i]);
    }

    return seed;
}

inline std::size_t getHash(const char *str) {
    std::size_t seed = 0;

    if (str == nullptr) {
        return seed;
    }

    auto length = std::strlen(str);

    for (std::size_t i = 0; i < length; i++) {
        hashCombine(seed, str[i]);
    }

    return seed;
}

constexpr void hashCombine(std::size_t &seed, const char *str) noexcept {
    seed = hashMix(seed + 0x9e3779b9 + getHash(str));
}

constexpr void hashCombine(std::size_t &seed, char *str) noexcept {
    seed = hashMix(seed + 0x9e3779b9 + getHash(str));
}

} // namespace dxfg