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
Command readerIpfCase{"ReaderIpfCase",
                      {"ri"},
                      "",
                      "ri [<properties>] [<ipfFilePath>]",
                      {"ri %defaultIpfFilePath%"},
                      [](const Command & /*self*/, graal_isolatethread_t *isolateThread,
                         const std::vector<std::string> &args, const dxfg::CommandsContext &context) {
                          puts("== ReaderIpf ==");

                          std::size_t argIndex = 0;
                          const auto address =
                              dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultIpfFilePath());

                          dxfg_instrument_profile_reader_t *reader = dxfg_InstrumentProfileReader_new(isolateThread);
                          dxfg_instrument_profile_list *profiles =
                              dxfg_InstrumentProfileReader_readFromFile(isolateThread, reader, address.c_str());

                          for (int i = 0; i < profiles->size; ++i) {
                              const auto s = dxfg_InstrumentProfile_getSymbol(isolateThread, profiles->elements[i]);

                              printf("  profile %s\n", s);

                              dxfg_String_release(isolateThread, s);
                          }

                          dxfg_CList_InstrumentProfile_release(isolateThread, profiles);
                          dxfg_JavaObjectHandler_release(isolateThread, &reader->handler);
                      }};
} // namespace dxfg
