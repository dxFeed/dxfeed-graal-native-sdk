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

void getException(graal_isolatethread_t *isolateThread);

namespace dxfg {
inline Command exceptionCase{"ExceptionCase",
                      {"ex"},
                      "",
                      "ex [<properties>]",
                      {},
                      [](const Command & /*self*/, graal_isolatethread_t *isolateThread,
                         const std::vector<std::string> & /*args*/, const dxfg::CommandsContext & /*context*/) {
                          puts("== Exception ==");

                          dxfg_java_object_handler *object = dxfg_throw_exception(isolateThread);

                          if (!object) {
                              getException(isolateThread);
                          }

                          dxfg_JavaObjectHandler_release(isolateThread, object);
                      }};
} // namespace dxfg