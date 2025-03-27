// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "CommandsContext.hpp"

#include <unordered_map>

namespace dxfg {

CommandsContext::CommandsContext() {
    using namespace std::string_literals;

    defaultHost_ = "demo.dxfeed.com";
    defaultPort_ = 7300;
    defaultToolsHost_ = "tools.dxfeed.com";
    defaultAddress_ = defaultHost_ + ":"s + std::to_string(defaultPort_);
    defaultWsAddress_ = "dxlink:wss://"s + defaultHost_ + "/dxlink-ws"s;
    defaultOnDemandPort_ = 7680;
    defaultOnDemandAddress_ = "(ondemand:"s + defaultHost_ + ":"s + std::to_string(defaultOnDemandPort_) + ")"s;
    defaultUser_ = "demo";
    defaultPassword_ = "demo";
    defaultIpfAddress_ = "https://"s + defaultUser_ + ":" + defaultPassword_ + "@" + defaultToolsHost_ + "/ipf"s;
    defaultIpfFilePath_ = "../../../../../ipf.txt";
    defaultCandleDataAddress_ = "https://"s + defaultToolsHost_ + "/candledata"s;
    defaultSymbols_ = {"AAPL", "IBM"};
}

const std::string& CommandsContext::getDefaultHost() const& {
    return defaultHost_;
}

int CommandsContext::getDefaultPort() const {
    return defaultPort_;
}

const std::string& CommandsContext::getDefaultToolsHost() const& {
    return defaultToolsHost_;
}

const std::string &CommandsContext::getDefaultPortString() const & {
    const static std::string result = std::to_string(defaultPort_);

    return result;
}

const std::string &CommandsContext::getDefaultAddress() const & {
    return defaultAddress_;
}

const std::string &CommandsContext::getDefaultWsAddress() const & {
    return defaultWsAddress_;
}

int CommandsContext::getDefaultOnDemandPort() const {
    return defaultOnDemandPort_;
}

const std::string &CommandsContext::getDefaultOnDemandPortString() const & {
    const static std::string result = std::to_string(defaultOnDemandPort_);

    return result;
}

const std::string &CommandsContext::getDefaultOnDemandAddress() const & {
    return defaultOnDemandAddress_;
}

const std::string &CommandsContext::getDefaultUser() const & {
    return defaultUser_;
}

const std::string &CommandsContext::getDefaultPassword() const & {
    return defaultPassword_;
}

const std::string &CommandsContext::getDefaultIpfAddress() const & {
    return defaultIpfAddress_;
}

const std::string &CommandsContext::getDefaultIpfFilePath() const & {
    return defaultIpfFilePath_;
}

const std::string &CommandsContext::getDefaultCandleDataAddress() const & {
    return defaultCandleDataAddress_;
}

const std::vector<std::string> &CommandsContext::getDefaultSymbols() const & {
    return defaultSymbols_;
}

const std::string &CommandsContext::getDefaultSymbolsString() const & {
    const static std::string result = [this]() -> std::string {
        std::string r{};

        for (const auto &s : defaultSymbols_) {
            r += s + ",";
        }

        return r.substr(0, r.size() - 1);
    }();

    return result;
}

std::string CommandsContext::substituteDefaultValues(std::string templateString) const {
    static const std::unordered_map<std::string, std::reference_wrapper<const std::string>> mapping{
        {"%defaultHost%", std::cref(defaultHost_)},
        {"%defaultPort%", std::cref(getDefaultPortString())},
        {"%defaultToolsHost%", std::cref(defaultToolsHost_)},
        {"%defaultAddress%", std::cref(defaultAddress_)},
        {"%defaultWsAddress%", std::cref(defaultWsAddress_)},
        {"%defaultOnDemandPort%", std::cref(getDefaultOnDemandPortString())},
        {"%defaultOnDemandAddress%", std::cref(defaultOnDemandAddress_)},
        {"%defaultUser%", std::cref(defaultUser_)},
        {"%defaultPassword%", std::cref(defaultPassword_)},
        {"%defaultIpfAddress%", std::cref(defaultIpfAddress_)},
        {"%defaultIpfFilePath%", std::cref(defaultIpfFilePath_)},
        {"%defaultCandleDataAddress%", std::cref(defaultCandleDataAddress_)},
        {"%defaultSymbols%", std::cref(getDefaultSymbolsString())},
    };

    for (const auto &m : mapping) {
        auto i = templateString.find(m.first);

        if (i != std::string::npos) {
            templateString = templateString.replace(i, m.first.size(), m.second);
        }
    }

    return templateString;
}

void CommandsContext::setSystemProperties(const std::unordered_map<std::string, std::string> &properties) {
    std::lock_guard<std::mutex> lock(mutex_);

    systemProperties_ = properties;
}

std::unordered_map<std::string, std::string> CommandsContext::getSystemProperties() const {
    std::lock_guard<std::mutex> lock(mutex_);

    return systemProperties_;
}

} // namespace dxfg