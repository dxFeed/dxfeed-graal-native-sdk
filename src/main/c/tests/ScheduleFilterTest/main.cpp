// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// Day and session filters with user data (dxfg_v4_DayFilter_newWithUserData(),
// dxfg_v4_SessionFilter_newWithUserData()): the function gets the user data, the conditions of the Java constructor
// gate the result before the function is called, -1 means "any", the other invalid values fail the call, the filters
// work in the search functions of a day and a schedule, and dxfg_v4_NativeListener_deactivateAll() does not stop them.

#include <catch.hpp>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <string>
#include <vector>

namespace dxfg {
namespace test {

namespace {

const Isolate &isolate() {
    static const Isolate instance{};

    return instance;
}

// Sessions in New York: pre-market 04:00-09:30, regular 09:30-16:00, after-market 16:00-20:00 on the trading days
// (Monday to Friday); 2024-07-04 (Thursday) is a holiday. The weekends and the holiday have no trading sessions.
constexpr const char *SCHEDULE_DEFINITION = "(tz=America/New_York;hd=20240704;0=p04000930r09301600a16002000)";

constexpr int32_t SATURDAY_MASK = 1 << 6; // Bit N for the day of the week N, Monday is 1.

// The calls of a day filter function: the days it got and the answer it gives.
struct DayCalls {
    std::vector<int32_t> days{};
    std::vector<void *> userData{};
    int32_t acceptFrom = 0; // The function accepts the days from this one (yyyymmdd) on.
    int32_t acceptValue = 1; // The value the function returns for an accepted day.

    static int32_t filter(graal_isolatethread_t *thread, dxfg_v4_day_t *day, void *userData) {
        auto *calls = static_cast<DayCalls *>(userData);
        int32_t yearMonthDay = 0;

        dxfg_v4_Day_getYearMonthDay(thread, day, &yearMonthDay);
        calls->days.push_back(yearMonthDay);
        calls->userData.push_back(userData);

        return yearMonthDay >= calls->acceptFrom ? calls->acceptValue : 0;
    }
};

// The calls of a session filter function: the session types it got and the type it accepts.
struct SessionCalls {
    std::vector<dxfg_v4_session_type_t> types{};
    std::vector<void *> userData{};
    int32_t acceptedType = -1; // -1 accepts every session.

    static int32_t filter(graal_isolatethread_t *thread, dxfg_v4_session_t *session, void *userData) {
        auto *calls = static_cast<SessionCalls *>(userData);
        auto type = static_cast<dxfg_v4_session_type_t>(-1);

        dxfg_v4_Session_getType(thread, session, &type);
        calls->types.push_back(type);
        calls->userData.push_back(userData);

        return calls->acceptedType == -1 || calls->acceptedType == static_cast<int32_t>(type) ? 1 : 0;
    }
};

struct Schedule {
    graal_isolatethread_t *thread;
    dxfg_v4_schedule_t *schedule = nullptr;

    explicit Schedule(graal_isolatethread_t *thread) : thread(thread) {
        REQUIRE(dxfg_v4_Schedule_getInstanceByDefinition(thread, SCHEDULE_DEFINITION, &schedule) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(schedule != nullptr);
    }

    ~Schedule() {
        dxfg_v4_JavaObjectHandler_release(thread, &schedule->handler);
    }

    dxfg_v4_day_t *day(int32_t yearMonthDay) const {
        dxfg_v4_day_t *result = nullptr;

        REQUIRE(dxfg_v4_Schedule_getDayByYearMonthDay(thread, schedule, yearMonthDay, &result) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(result != nullptr);

        return result;
    }
};

dxfg_v4_day_filter_t *newDayFilter(graal_isolatethread_t *thread, int32_t mask, int32_t holiday, int32_t shortDay,
                                   int32_t trading, DayCalls &calls) {
    dxfg_v4_day_filter_t *filter = nullptr;

    REQUIRE(dxfg_v4_DayFilter_newWithUserData(thread, mask, holiday, shortDay, trading, &DayCalls::filter, &calls,
                                              &filter) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(filter != nullptr);

    return filter;
}

dxfg_v4_session_filter_t *newSessionFilter(graal_isolatethread_t *thread, int32_t type, int32_t trading,
                                           SessionCalls &calls) {
    dxfg_v4_session_filter_t *filter = nullptr;

    REQUIRE(dxfg_v4_SessionFilter_newWithUserData(thread, type, trading, &SessionCalls::filter, &calls, &filter) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(filter != nullptr);

    return filter;
}

// The next day after `from` that the filter accepts, as yyyymmdd; 0 if none is found.
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

// The type of the first session of the day that the filter accepts; -1 if none is found.
int32_t findFirstSessionType(graal_isolatethread_t *thread, dxfg_v4_day_t *day, dxfg_v4_session_filter_t *filter) {
    dxfg_v4_session_t *session = nullptr;
    auto type = static_cast<dxfg_v4_session_type_t>(-1);

    REQUIRE(dxfg_v4_Day_findFirstSession(thread, day, filter, &session) == DXFG_EXECUTE_SUCCESSFULLY);

    if (session == nullptr) {
        return -1;
    }

    REQUIRE(dxfg_v4_Session_getType(thread, session, &type) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_Session_release(thread, session) == DXFG_EXECUTE_SUCCESSFULLY);

    return static_cast<int32_t>(type);
}

void release(graal_isolatethread_t *thread, dxfg_v4_day_filter_t *filter) {
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &filter->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

void release(graal_isolatethread_t *thread, dxfg_v4_session_filter_t *filter) {
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &filter->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

} // namespace

TEST_CASE("A day filter calls its function with the user data for the days that pass its conditions",
          "[ScheduleFilter]") {
    auto *thread = isolate().thread;

    REQUIRE(thread != nullptr);

    Schedule schedule(thread);
    dxfg_v4_day_t *wednesday = schedule.day(20240703);

    // Trading days only: the holiday and the weekend are rejected before the function is called.
    DayCalls calls{};
    calls.acceptFrom = 20240708;

    auto *filter = newDayFilter(thread, 0, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, 1, calls);

    CHECK(findNextDay(thread, wednesday, filter) == 20240708);
    CHECK(calls.days == std::vector<int32_t>{20240705, 20240708});
    CHECK(calls.userData == std::vector<void *>{&calls, &calls});
    CHECK(takeExceptionClassName(thread).empty());
    release(thread, filter);

    dxfg_v4_Day_release(thread, wednesday);
}

TEST_CASE("A day filter accepts a day only when its function returns 1", "[ScheduleFilter]") {
    auto *thread = isolate().thread;

    REQUIRE(thread != nullptr);

    Schedule schedule(thread);
    dxfg_v4_day_t *wednesday = schedule.day(20240703);

    for (const int32_t acceptValue : {2, -1}) {
        INFO("the function returns " << acceptValue);

        DayCalls calls{};
        calls.acceptValue = acceptValue;

        auto *filter = newDayFilter(thread, SATURDAY_MASK, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY,
                                    calls);

        CHECK(findNextDay(thread, wednesday, filter) == 0);
        CHECK_FALSE(calls.days.empty());
        CHECK(takeExceptionClassName(thread).empty());
        release(thread, filter);
    }

    dxfg_v4_Day_release(thread, wednesday);
}

TEST_CASE("The conditions of a day filter gate its result whatever the function says", "[ScheduleFilter]") {
    auto *thread = isolate().thread;

    REQUIRE(thread != nullptr);

    Schedule schedule(thread);
    dxfg_v4_day_t *wednesday = schedule.day(20240703);

    struct Case {
        int32_t mask;
        int32_t holiday;
        int32_t shortDay;
        int32_t trading;
        int32_t expected;
    };

    // The function accepts every day: the result is the first day that passes the conditions.
    for (const auto &[mask, holiday, shortDay, trading, expected] : std::vector<Case>{
             {0, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, 20240704},
             {0, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, 1, 20240705},
             {0, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, 0, 20240704},
             {0, 1, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, 20240704},
             {0, 0, DXFG_V4_FILTER_ANY, 0, 20240706},
             {0, DXFG_V4_FILTER_ANY, 0, 1, 20240705},
             {SATURDAY_MASK, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, 20240706},
         }) {
        INFO("mask " << mask << ", holiday " << holiday << ", short day " << shortDay << ", trading " << trading);

        DayCalls calls{};
        auto *filter = newDayFilter(thread, mask, holiday, shortDay, trading, calls);

        CHECK(findNextDay(thread, wednesday, filter) == expected);
        CHECK(calls.days == std::vector<int32_t>{expected});
        release(thread, filter);
    }

    // The function rejects every day the conditions pass: nothing is found within the search limit.
    DayCalls rejecting{};
    rejecting.acceptFrom = 99999999;

    auto *filter = newDayFilter(thread, SATURDAY_MASK, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, 1, rejecting);

    CHECK(findNextDay(thread, wednesday, filter) == 0);
    CHECK(rejecting.days.empty()); // No Saturday is a trading day.
    release(thread, filter);

    dxfg_v4_Day_release(thread, wednesday);
}

TEST_CASE("A day filter with an invalid argument is not created", "[ScheduleFilter]") {
    auto *thread = isolate().thread;

    REQUIRE(thread != nullptr);

    DayCalls calls{};

    struct Case {
        int32_t holiday;
        int32_t shortDay;
        int32_t trading;
    };

    for (const auto &[holiday, shortDay, trading] :
         std::vector<Case>{{2, -1, -1}, {-1, 2, -1}, {-1, -1, 2}, {-2, -1, -1}, {-1, -1, INT32_MIN}}) {
        INFO("holiday " << holiday << ", short day " << shortDay << ", trading " << trading);

        dxfg_v4_day_filter_t *filter = nullptr;

        CHECK(dxfg_v4_DayFilter_newWithUserData(thread, 0, holiday, shortDay, trading, &DayCalls::filter, &calls,
                                                &filter) == DXFG_EXECUTE_FAIL);
        CHECK(filter == nullptr);
        CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    }

    dxfg_v4_day_filter_t *filter = nullptr;

    CHECK(dxfg_v4_DayFilter_newWithUserData(thread, 0, -1, -1, -1, nullptr, &calls, &filter) == DXFG_EXECUTE_FAIL);
    CHECK(filter == nullptr);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_DayFilter_newWithUserData(thread, 0, -1, -1, -1, &DayCalls::filter, &calls, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("A session filter calls its function with the user data for the sessions that pass its conditions",
          "[ScheduleFilter]") {
    auto *thread = isolate().thread;

    REQUIRE(thread != nullptr);

    Schedule schedule(thread);
    dxfg_v4_day_t *friday = schedule.day(20240705);

    // Trading sessions only: the non-trading sessions at the ends of the day are rejected before the function.
    SessionCalls calls{};
    calls.acceptedType = DXFG_SESSION_TYPE_AFTER_MARKET;

    auto *filter = newSessionFilter(thread, DXFG_V4_FILTER_ANY, 1, calls);

    CHECK(findFirstSessionType(thread, friday, filter) == DXFG_SESSION_TYPE_AFTER_MARKET);
    CHECK(calls.types == std::vector<dxfg_v4_session_type_t>{DXFG_SESSION_TYPE_PRE_MARKET, DXFG_SESSION_TYPE_REGULAR,
                                                             DXFG_SESSION_TYPE_AFTER_MARKET});
    CHECK(calls.userData == std::vector<void *>{&calls, &calls, &calls});
    release(thread, filter);

    dxfg_v4_Day_release(thread, friday);
}

TEST_CASE("The conditions of a session filter gate its result whatever the function says", "[ScheduleFilter]") {
    auto *thread = isolate().thread;

    REQUIRE(thread != nullptr);

    Schedule schedule(thread);
    dxfg_v4_day_t *friday = schedule.day(20240705);
    dxfg_v4_day_t *holiday = schedule.day(20240704);

    struct Case {
        dxfg_v4_day_t *day;
        int32_t type;
        int32_t trading;
        int32_t expected;
    };

    // The function accepts every session: the result is the first session that passes the conditions.
    for (const auto &[day, type, trading, expected] : std::vector<Case>{
             {friday, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, DXFG_SESSION_TYPE_NO_TRADING},
             {friday, DXFG_V4_FILTER_ANY, 1, DXFG_SESSION_TYPE_PRE_MARKET},
             {friday, DXFG_SESSION_TYPE_REGULAR, DXFG_V4_FILTER_ANY, DXFG_SESSION_TYPE_REGULAR},
             {friday, DXFG_SESSION_TYPE_AFTER_MARKET, 1, DXFG_SESSION_TYPE_AFTER_MARKET},
             {friday, DXFG_SESSION_TYPE_REGULAR, 0, -1},
             {holiday, DXFG_V4_FILTER_ANY, 1, -1},
             {holiday, DXFG_SESSION_TYPE_NO_TRADING, 0, DXFG_SESSION_TYPE_NO_TRADING},
         }) {
        INFO("type " << type << ", trading " << trading << ", expected " << expected);

        SessionCalls calls{};
        auto *filter = newSessionFilter(thread, type, trading, calls);

        CHECK(findFirstSessionType(thread, day, filter) == expected);

        if (expected == -1) {
            CHECK(calls.types.empty());
        } else {
            CHECK(calls.types == std::vector<dxfg_v4_session_type_t>{static_cast<dxfg_v4_session_type_t>(expected)});
        }

        release(thread, filter);
    }

    dxfg_v4_Day_release(thread, holiday);
    dxfg_v4_Day_release(thread, friday);
}

TEST_CASE("A session filter with an invalid argument is not created", "[ScheduleFilter]") {
    auto *thread = isolate().thread;

    REQUIRE(thread != nullptr);

    SessionCalls calls{};

    struct Case {
        int32_t type;
        int32_t trading;
    };

    for (const auto &[type, trading] :
         std::vector<Case>{{4, -1}, {-2, -1}, {INT32_MAX, -1}, {DXFG_SESSION_TYPE_REGULAR, 2}, {-1, -2}}) {
        INFO("type " << type << ", trading " << trading);

        dxfg_v4_session_filter_t *filter = nullptr;

        CHECK(dxfg_v4_SessionFilter_newWithUserData(thread, type, trading, &SessionCalls::filter, &calls, &filter) ==
              DXFG_EXECUTE_FAIL);
        CHECK(filter == nullptr);
        CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    }

    dxfg_v4_session_filter_t *filter = nullptr;

    CHECK(dxfg_v4_SessionFilter_newWithUserData(thread, -1, -1, nullptr, &calls, &filter) == DXFG_EXECUTE_FAIL);
    CHECK(filter == nullptr);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_SessionFilter_newWithUserData(thread, -1, -1, &SessionCalls::filter, &calls, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("A session filter with user data works in a schedule search", "[ScheduleFilter]") {
    auto *thread = isolate().thread;

    REQUIRE(thread != nullptr);

    Schedule schedule(thread);
    dxfg_v4_day_t *friday = schedule.day(20240705);
    int64_t fridayStart = 0;

    REQUIRE(dxfg_v4_Day_getStartTime(thread, friday, &fridayStart) == DXFG_EXECUTE_SUCCESSFULLY);

    // The nearest regular session to the start of Friday is Friday's: Thursday is a holiday.
    SessionCalls calls{};
    auto *filter = newSessionFilter(thread, DXFG_SESSION_TYPE_REGULAR, DXFG_V4_FILTER_ANY, calls);
    dxfg_v4_session_t *session = nullptr;

    REQUIRE(dxfg_v4_Schedule_findNearestSessionByTime(thread, schedule.schedule, fridayStart, filter, &session) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(session != nullptr);

    dxfg_v4_day_t *day = nullptr;
    int32_t yearMonthDay = 0;
    auto type = static_cast<dxfg_v4_session_type_t>(-1);

    REQUIRE(dxfg_v4_Session_getDay(thread, session, &day) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_Day_getYearMonthDay(thread, day, &yearMonthDay) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(yearMonthDay == 20240705);
    CHECK(dxfg_v4_Session_getType(thread, session, &type) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(type == DXFG_SESSION_TYPE_REGULAR);
    REQUIRE_FALSE(calls.types.empty());

    for (std::size_t i = 0; i < calls.types.size(); ++i) {
        CHECK(calls.types[i] == DXFG_SESSION_TYPE_REGULAR);
        CHECK(calls.userData[i] == &calls);
    }

    dxfg_v4_Day_release(thread, day);
    dxfg_v4_Session_release(thread, session);
    release(thread, filter);
    dxfg_v4_Day_release(thread, friday);
}

TEST_CASE("dxfg_v4_NativeListener_deactivateAll does not stop the filters with user data", "[ScheduleFilter]") {
    // An isolate of its own: deactivateAll() deactivates the listeners of the whole isolate.
    const Isolate own{};
    auto *thread = own.thread;

    REQUIRE(thread != nullptr);

    Schedule schedule(thread);
    dxfg_v4_day_t *friday = schedule.day(20240705);
    DayCalls dayCalls{};
    SessionCalls sessionCalls{};
    auto *dayFilter = newDayFilter(thread, 0, DXFG_V4_FILTER_ANY, DXFG_V4_FILTER_ANY, 1, dayCalls);
    auto *sessionFilter = newSessionFilter(thread, DXFG_SESSION_TYPE_REGULAR, DXFG_V4_FILTER_ANY, sessionCalls);

    REQUIRE(dxfg_v4_NativeListener_deactivateAll(thread) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(findNextDay(thread, friday, dayFilter) == 20240708);
    CHECK(dayCalls.days == std::vector<int32_t>{20240708});
    CHECK(findFirstSessionType(thread, friday, sessionFilter) == DXFG_SESSION_TYPE_REGULAR);
    CHECK(sessionCalls.types == std::vector<dxfg_v4_session_type_t>{DXFG_SESSION_TYPE_REGULAR});

    release(thread, sessionFilter);
    release(thread, dayFilter);
    dxfg_v4_Day_release(thread, friday);
}

} // namespace test
} // namespace dxfg
