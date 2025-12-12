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

inline void instrumentProfileUpdateListenerCallback(graal_isolatethread_t *thread, dxfg_iterable_ip_t *profiles,
                                                    void * /* user_data */) {
    while (dxfg_Iterable_InstrumentProfile_hasNext(thread, profiles) == 1) {
        dxfg_instrument_profile_t *profile = dxfg_Iterable_InstrumentProfile_next(thread, profiles);

        if (!profile) {
            getException(thread);
        }

        const auto s = dxfg_InstrumentProfile_getSymbol(thread, profile);

        printf("  Profile %s\n", s);

        dxfg_String_release(thread, s);
        dxfg_InstrumentProfile_release(thread, profile);
    }
}

inline void instrumentProfileUpdateListenerCallback2(graal_isolatethread_t *thread, dxfg_iterable_ip_t *profiles,
                                                    void * /* user_data */) {
    while (dxfg_Iterable_InstrumentProfile_hasNext(thread, profiles) == 1) {
        dxfg_instrument_profile2_t* profile{};
        auto result = dxfg_Iterable_InstrumentProfile_next2(thread, profiles, &profile);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(thread);
        }

        printf("  Profile %s\n", profile->symbol);

        dxfg_instrument_profile_free(thread, profile);
    }
}

inline void ipfPropertyChangeListenerCallback(graal_isolatethread_t * /* thread */,
                                              dxfg_ipf_connection_state_t old_state,
                                              dxfg_ipf_connection_state_t new_state, void * /* user_data */) {
    printf("  IPF stateChangeListener %d -> %d\n", old_state, new_state);
}

inline Command liveIpfCase{
    "LiveIpfCase",
    {"li"},
    "",
    "li [<properties>] [<address>]",
    {"li %defaultIpfAddress%", "li -DLiveIpfCase.variant=2 %defaultIpfAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        puts("== LiveIpf ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultIpfAddress());

        dxfg_ipf_collector_t *collector = dxfg_InstrumentProfileCollector_new(isolateThread);
        dxfg_ipf_connection_t *connection =
            dxfg_InstrumentProfileConnection_createConnection(isolateThread, address.c_str(), collector);

        printf("  InstrumentProfileCollector state %d\n",
               dxfg_InstrumentProfileConnection_getState(isolateThread, connection));

        dxfg_ipf_update_listener_t *listener{};

        auto systemProperties = context.getSystemProperties();

        if (systemProperties.count("LiveIpfCase.variant") > 0 && systemProperties.at("LiveIpfCase.variant") == "2") {
            listener =
                dxfg_InstrumentProfileUpdateListener_new(isolateThread, &instrumentProfileUpdateListenerCallback2, nullptr);

        } else {
            listener =
                dxfg_InstrumentProfileUpdateListener_new(isolateThread, &instrumentProfileUpdateListenerCallback, nullptr);
        }

        dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), &finalize, nullptr);
        dxfg_InstrumentProfileCollector_addUpdateListener(isolateThread, collector, listener);

        dxfg_ipf_connection_state_change_listener_t *listenerState =
            dxfg_IpfPropertyChangeListener_new(isolateThread, &ipfPropertyChangeListenerCallback, nullptr);

        dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listenerState), &finalize,
                             nullptr);
        dxfg_InstrumentProfileConnection_addStateChangeListener(isolateThread, connection, listenerState);
        dxfg_InstrumentProfileConnection_start(isolateThread, connection);
        printf("  InstrumentProfileCollector state %d\n",
               dxfg_InstrumentProfileConnection_getState(isolateThread, connection));
        dxfg_InstrumentProfileConnection_waitUntilCompleted(isolateThread, connection, 30000);
        printf("  InstrumentProfileCollector state %d\n",
               dxfg_InstrumentProfileConnection_getState(isolateThread, connection));
        dxfg_InstrumentProfileConnection_close(isolateThread, connection);
        printf("  InstrumentProfileCollector state %d\n",
               dxfg_InstrumentProfileConnection_getState(isolateThread, connection));
        dxfg_JavaObjectHandler_release(isolateThread, &collector->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &connection->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &listenerState->handler);
    }};
} // namespace dxfg