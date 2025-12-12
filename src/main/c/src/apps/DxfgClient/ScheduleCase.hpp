// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "Common.hpp"

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <cstdio>
#include <string>
#include <vector>

namespace dxfg {
inline Command scheduleCase{
    "ScheduleCase",
    {"sch"},
    "",
    "sch [<properties>] [<ipfFilePath>]",
    {"sch %defaultIpfFilePath%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        puts("== Schedule ==");

        std::size_t argIndex = 0;
        const auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultIpfFilePath());

        dxfg_system_set_property(isolateThread, "com.dxfeed.schedule.download", "auto");

        dxfg_instrument_profile_reader_t *reader = dxfg_InstrumentProfileReader_new(isolateThread);
        dxfg_instrument_profile_list *profiles =
            dxfg_InstrumentProfileReader_readFromFile(isolateThread, reader, address.c_str());

        for (int i = 0; i < profiles->size; ++i) {
            dxfg_instrument_profile_t *profile = profiles->elements[i];
            dxfg_schedule_t *schedule = dxfg_Schedule_getInstance(isolateThread, profile);
            const char *name = dxfg_Schedule_getName(isolateThread, schedule);

            printf("  Schedule %s\n", name);
            dxfg_String_release(isolateThread, name);
            dxfg_JavaObjectHandler_release(isolateThread, &schedule->handler);

            dxfg_string_list *venues = dxfg_Schedule_getTradingVenues(isolateThread, profile);

            for (int j = 0; j < venues->size; ++j) {
                dxfg_schedule_t *schedule2 = dxfg_Schedule_getInstance3(isolateThread, profile, venues->elements[j]);
                const char *name2 = dxfg_Schedule_getName(isolateThread, schedule2);

                printf("  Schedule %s\n", name2);
                dxfg_String_release(isolateThread, name2);
                dxfg_JavaObjectHandler_release(isolateThread, &schedule2->handler);
            }

            dxfg_CList_String_release(isolateThread, venues);

            const auto s = dxfg_InstrumentProfile_getSymbol(isolateThread, profiles->elements[i]);

            printf("  Schedule %s\n", s);
            dxfg_String_release(isolateThread, s);
        }

        dxfg_CList_InstrumentProfile_release(isolateThread, profiles);
        dxfg_JavaObjectHandler_release(isolateThread, &reader->handler);
    }};

inline Command schedule2Case{"Schedule2Case",
                             {"sch2"},
                             "",
                             "sch2 [<properties>]",
                             {},
                             [](const Command & /*self*/, graal_isolatethread_t *isolateThread,
                                const std::vector<std::string> & /*args*/, const dxfg::CommandsContext & /*context*/) {
                                 puts("== Schedule2 ==");

                                 dxfg_system_set_property(isolateThread, "com.dxfeed.schedule.download", "auto");

                                 dxfg_schedule_t *schedule =
                                     dxfg_Schedule_getInstance2(isolateThread, "(tz=GMT;de=2300;0=)");
                                 dxfg_day_t *day = dxfg_Schedule_getDayById(isolateThread, schedule, 42);
                                 int32_t dayId = dxfg_Day_getDayId(isolateThread, day);

                                 printf("  dayId %d should be 42\n", dayId);

                                 const char *timeZoneGetId = dxfg_Schedule_getTimeZone_getID(isolateThread, schedule);

                                 printf("  timeZoneGetId = %s\n", timeZoneGetId);

                                 const char *name = dxfg_Schedule_getName(isolateThread, schedule);

                                 printf("  schedule %s\n", name);
                                 dxfg_String_release(isolateThread, name);
                                 dxfg_String_release(isolateThread, timeZoneGetId);
                                 dxfg_JavaObjectHandler_release(isolateThread, &day->handler);
                                 dxfg_JavaObjectHandler_release(isolateThread, &schedule->handler);
                             }};

} // namespace dxfg