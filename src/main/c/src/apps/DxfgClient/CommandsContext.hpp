// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <string>
#include <vector>
#include <mutex>
#include <unordered_map>

namespace dxfg {

class CommandsContext {
    std::string defaultHost_;
    int defaultPort_;
    std::string defaultAddress_;
    std::string defaultWsAddress_;
    int defaultOnDemandPort_;
    std::string defaultOnDemandAddress_;
    std::string defaultUser_;
    std::string defaultPassword_;
    std::string defaultIpfAddress_;
    std::string defaultIpfFilePath_;
    std::vector<std::string> defaultSymbols_;
    mutable std::mutex mutex_{};
    std::unordered_map<std::string, std::string> systemProperties_{};

  public:
    CommandsContext();

    std::string getDefaultHost() const;

    int getDefaultPort() const;

    const std::string &getDefaultPortString() const &;

    const std::string &getDefaultAddress() const &;

    const std::string &getDefaultWsAddress() const &;

    int getDefaultOnDemandPort() const;

    const std::string &getDefaultOnDemandPortString() const &;

    const std::string &getDefaultOnDemandAddress() const &;

    const std::string &getDefaultUser() const &;

    const std::string &getDefaultPassword() const &;

    const std::string &getDefaultIpfAddress() const &;

    const std::string &getDefaultIpfFilePath() const &;

    const std::vector<std::string> &getDefaultSymbols() const &;

    const std::string &getDefaultSymbolsString() const &;

    std::string substituteDefaultValues(std::string templateString) const;

    void setSystemProperties(const std::unordered_map<std::string, std::string>& properties);

    std::unordered_map<std::string, std::string> getSystemProperties() const;
};

} // namespace dxfg