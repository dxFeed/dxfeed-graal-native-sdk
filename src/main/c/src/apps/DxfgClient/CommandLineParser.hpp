// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <DxfgUtils/StringUtils.hpp>
#include <dxfg_events.h>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <vector>

namespace dxfg {

class CommandLineParser {
    static const std::string propertyPattern_;
    static const std::unordered_map<std::string, dxfg_event_clazz_t> eventTypesMap_;

  public:
    template <typename ArgsCollection, typename ArgIndexType>
    static std::unordered_map<std::string, std::string> parseSystemProperties(const ArgsCollection &args,
                                                                              ArgIndexType &currentArg) {
        std::unordered_map<std::string, std::string> properties{};

        if (args.empty() || currentArg >= args.size()) {
            return properties;
        }

        while (currentArg < args.size() && std::string(args[currentArg]).find(propertyPattern_, 0) == 0) {
            auto strings = StringUtils::splitString(args[currentArg], '=');

            if (strings.size() != 2) {
                throw std::invalid_argument("Incorrect property key/value pair: " + std::string(args[currentArg]));
            }

            const auto key = strings[0].substr(propertyPattern_.length());
            const auto value = strings[1];

            properties.emplace(key, value);
            ++currentArg;
        }

        return properties;
    }

    template <typename ArgsCollection, typename ArgIndexType>
    static std::string parseString(const ArgsCollection &args, ArgIndexType &currentArg, std::string defaultValue = std::string()) {
        if (args.empty() || currentArg >= args.size()) {
            return defaultValue;
        }

        return args[currentArg++];
    }

    template <typename ArgsCollection, typename ArgIndexType>
    static std::string parseAddress(const ArgsCollection &args, ArgIndexType &currentArg, std::string defaultValue = std::string()) {
        return parseString(args, currentArg, defaultValue);
    }

    template <typename ArgsCollection, typename ArgIndexType>
    static std::vector<dxfg_event_clazz_t> parseEventTypes(const ArgsCollection &args, ArgIndexType &currentArg) {
        std::vector<dxfg_event_clazz_t> eventTypes{};

        if (args.empty() || currentArg >= args.size()) {
            return eventTypes;
        }

        const auto stringListEventTypes = StringUtils::splitString(args[currentArg++], ',');

        for (const auto &stringEventType : stringListEventTypes) {
            auto it = eventTypesMap_.find(stringEventType);

            if (it != eventTypesMap_.end()) {
                eventTypes.push_back(it->second);
            } else {
                throw std::invalid_argument("Unknown events type: " + stringEventType);
            }
        }

        return eventTypes;
    }

    template <typename ArgsCollection, typename ArgIndexType>
    static std::vector<std::string> parseSymbols(const ArgsCollection &args, ArgIndexType &currentArg) {
        if (args.empty() || currentArg >= args.size()) {
            return {};
        }

        return StringUtils::splitString(args[currentArg++], ',');
    }
};

} // namespace dxfg
