// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <algorithm>
#include <locale>
#include <string>
#include <unordered_map>
#include <vector>
#include <functional>
#include <iostream>

#include <dxfg_api.h>

#include "Common.hpp"
#include "CommandsContext.hpp"

namespace dxfg {

struct Command {
    using Signature = void(const Command& self, graal_isolatethread_t * /*isolateThread*/, const std::vector<std::string> & /*args*/,
                           const CommandsContext & /*context*/);

    std::string name;
    std::vector<std::string> shortNames;
    std::string description;
    std::string usage;
    std::vector<std::string> examples;
    std::function<Signature> function;

    void operator()(graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
                    const CommandsContext &context) const {
        function(*this, isolateThread, args, context);
    }

    static const Command NONE;
};

struct CommandsRegistry {
    using CommandsStorageType = std::unordered_map<std::string /*commandName*/, Command>;
    using CommandNamesMappingsType = std::unordered_map<std::string /* shortName */, std::string /* longName */,
                                                        CaseInsensitiveHash, CaseInsensitiveEq>;

  private:
    static CommandsStorageType commandsStorage;
    static CommandNamesMappingsType commandNamesMapping;

  public:
    static CommandsContext COMMANDS_CONTEXT;

    static bool hasCommand(const std::string &name) {
        return commandNamesMapping.find(name) != commandNamesMapping.end();
    }

    static void addCommand(const Command &command) {
        if (hasCommand(command.name)) {
            return;
        }

        commandsStorage[command.name] = command;
        commandNamesMapping[command.name] = command.name;

        for (const auto &shortName : command.shortNames) {
            commandNamesMapping[shortName] = command.name;
        }
    }

    static std::pair<bool, const Command &> getCommand(const std::string &name) {
        if (!hasCommand(name)) {
            return {false, Command::NONE};
        }

        return {true, commandsStorage[commandNamesMapping[name]]};
    }

    static void tryRunCommand(graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
                              const std::function<void()> &onFail) {
        if (args.empty()) {
            if (hasCommand("Help")) {
                commandsStorage[commandNamesMapping["Help"]](isolateThread, args, COMMANDS_CONTEXT);
            }

            return;
        }

        if (!hasCommand(args[0])) {
            std::cerr << "Unknown command: " << args[0] << std::endl;

            onFail();

            return;
        }

        auto newArgs = args;

        newArgs.erase(newArgs.begin());
        commandsStorage[commandNamesMapping[args[0]]](isolateThread, newArgs, COMMANDS_CONTEXT);
    }

};

}