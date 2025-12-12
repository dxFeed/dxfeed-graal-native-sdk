// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "Common.hpp"

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <cstdint>
#include <cstdio>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
inline Command loggingCase{
    "LoggingCase",
    {"l"},
    "",
    "l [<properties>]",
    {},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> & /*args*/,
       const dxfg::CommandsContext & /*context*/) {
        puts("== Logging ==");

        dxfg_system_set_property(isolateThread, "log.className", "com.devexperts.logging.InterceptableLogging");
        dxfg_system_set_property(isolateThread, "log.level", "OFF");

        dxfg_logging_listener_t *listener{};

        dxfg_logging_listener_new(
            isolateThread,
            [](graal_isolatethread_t * /* thread */, dxfg_logging_level_t /* level */, int64_t /* timestamp */,
               const char * /* threadName */, int64_t threadId, const char * /* loggerName */,
               const char * /* message */, dxfg_exception_t * /* exception */, const char *formattedMessage,
               void * /* user_data */) {
                std::cerr << "[" + std::to_string(threadId) + "]:" + formattedMessage;
            },
            nullptr, &listener);

        dxfg_logging_set_listener(isolateThread, listener);

        getException(isolateThread); // to init com.dxfeed.sdk.NativeUtils

        auto ep = dxfg_DXEndpoint_getInstance(isolateThread);

        std::this_thread::sleep_for(std::chrono::milliseconds(1000));

        dxfg_logging_set_log_level(isolateThread, DXFG_LOGGING_LEVEL_INFO);

        dxfg_DXEndpoint_close(isolateThread, ep);

        dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &ep->handler);
    }};
} // namespace dxfg