// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <dxfg_api.h>

#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <chrono>
#include <cstdio>
#include <string>
#include <vector>

void getException(graal_isolatethread_t *isolateThread);

namespace dxfg {
Command instrumentProfileFieldCase{"InstrumentProfileFieldCase",
                                   {"ipfi"},
                                   "",
                                   "ipfi [<properties>]",
                                   {"ipfi"},
                                   [](const Command & /*self*/, graal_isolatethread_t *isolateThread,
                                      const std::vector<std::string> &, const dxfg::CommandsContext &) {
                                       using namespace std::chrono_literals;

                                       puts("== InstrumentProfileField ==");

                                       for (const auto n :
                                            std::vector<double>{0.0, -1234567, 19.1231236861273, 1282121}) {
                                           char *str{};
                                           auto r = dxfg_InstrumentProfileField_formatNumber(isolateThread, n, &str);

                                           if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                                               getException(isolateThread);
                                           } else {
                                               double n2{};

                                               dxfg_InstrumentProfileField_parseNumber(isolateThread, str, &n2);
                                               printf("%f -> '%s' -> %f\n", n, str, n2);
                                               dxfg_String_release(isolateThread, str);
                                           }
                                       }

                                       putchar('\n');

                                       for (const auto n : std::vector<std::int32_t>{-124, 0, 12345, 3748489}) {
                                           char *str{};
                                           auto r = dxfg_InstrumentProfileField_formatDate(isolateThread, n, &str);

                                           if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                                               getException(isolateThread);
                                           } else {
                                               std::int32_t n2{};

                                               dxfg_InstrumentProfileField_parseDate(isolateThread, str, &n2);
                                               printf("%d -> '%s' -> %d\n", n, str, n2);
                                               dxfg_String_release(isolateThread, str);
                                           }
                                       }
                                   }};
} // namespace dxfg