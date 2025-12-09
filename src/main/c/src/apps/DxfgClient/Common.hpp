// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <algorithm>
#include <locale>
#include <string>
#include <vector>
#include <mutex>
#include <atomic>
#include <chrono>

namespace dxfg {

#define DXFG_MACRO_CONCAT(a, b) DXFG_MACRO_CONCAT_INNER(a, b)
#define DXFG_MACRO_CONCAT_INNER(a, b) a##b

#define DXFG_MACRO_UNIQUE_NAME(base) DXFG_MACRO_CONCAT(base, __LINE__)

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

} // namespace dxfg