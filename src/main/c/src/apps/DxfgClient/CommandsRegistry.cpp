// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "CommandsRegistry.hpp"

namespace dxfg {
const Command Command::NONE;

CommandsRegistry::CommandsStorageType CommandsRegistry::commandsStorage{};
CommandsRegistry::CommandNamesMappingsType CommandsRegistry::commandNamesMapping{};
CommandsContext CommandsRegistry::COMMANDS_CONTEXT{};

}