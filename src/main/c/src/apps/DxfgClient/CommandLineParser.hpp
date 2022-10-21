// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <dxfg_events.h>
#include <string>
#include <unordered_map>
#include <vector>

namespace dxfg {

class CommandLineParser {
  private:
    static const std::string propertyPattern_;
    static const std::unordered_map<std::string, dxfg_event_type_t> typesMap_;

  public:
    static std::unordered_map<std::string, std::string> parseSystemProperties(char **argv, int &currentArg);
    static std::string parseAddress(char **argv, int &currentArg);
    static std::vector<dxfg_event_type_t> parseEventTypes(char **argv, int &currentArg);
    static std::vector<std::string> parseSymbols(char **argv, int &currentArg);
};

} // namespace dxfg
