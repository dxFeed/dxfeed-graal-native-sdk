// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <cstdio>
#include <string>
#include <vector>

namespace dxfg {
inline Command systemPropertiesCase{"SystemPropertiesCase",
                                    {"sp"},
                                    "",
                                    "sp [<properties>]",
                                    {},
                                    [](const Command & /*self*/, graal_isolatethread_t *isolateThread,
                                       const std::vector<std::string> & /*args*/,
                                       const dxfg::CommandsContext & /*context*/) {
                                        puts("== SystemProperties ==");

                                        const char *propertyName = "property-name";
                                        const char *string = dxfg_system_get_property(isolateThread, propertyName);

                                        printf("\"%s\" must not be equal to \"property-value\"\n", string);

                                        dxfg_String_release(isolateThread, string);
                                        dxfg_system_set_property(isolateThread, "property-name", "property-value");

                                        const char *value = dxfg_system_get_property(isolateThread, propertyName);

                                        printf("\"%s\" must be equal to \"property-value\"\n", value);
                                        dxfg_String_release(isolateThread, value);
                                    }};
} // namespace dxfg