// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Args.hpp>

#include <cmath>
#include <cstdlib>
#include <limits>

namespace dxfg {
namespace nettest {

namespace {

// The largest symbol pool, batch and number of instances that the tool accepts. A batch shares one time, so it holds at
// most 2^22 events: the 22-bit sequence (EventPool::SEQUENCE_MASK) must not wrap inside one batch.
constexpr std::int64_t MAX_SYMBOLS = 100'000'000;
constexpr std::int64_t MAX_BATCH = 1 << 22;
constexpr std::int64_t MAX_INSTANCES = 1'000;

bool isDigits(const std::string &text) {
    if (text.empty()) {
        return false;
    }

    for (const char symbol : text) {
        if (symbol < '0' || symbol > '9') {
            return false;
        }
    }

    return true;
}

// Parses a positive decimal integer of at most `limit`; nullopt otherwise.
std::optional<std::int64_t> parsePositive(const std::string &text, std::int64_t limit) {
    if (!isDigits(text) || text.size() > 18) {
        return std::nullopt;
    }

    const auto value = std::strtoll(text.c_str(), nullptr, 10);

    if (value < 1 || value > limit) {
        return std::nullopt;
    }

    return value;
}

// The state of one parsing: the error is set by the first failure.
struct Parser {
    Options options{};
    std::string error{};
    bool addressSeen = false;
    bool symbolsSeen = false;

    bool fail(std::string message) {
        if (error.empty()) {
            error = std::move(message);
        }

        return false;
    }

    bool parseSymbols(const std::string &value) {
        const auto at = value.find('@');
        const auto head = value.substr(0, at);

        if (head.find('{') != std::string::npos) {
            return fail("unsupported QD flag: the symbol length '{len,max}' in '-S " + value + "'");
        }

        if (head.find('/') != std::string::npos) {
            return fail("unsupported QD flag: the symbol split '/r' or '/s' in '-S " + value + "'");
        }

        const auto total = parsePositive(head, MAX_SYMBOLS);

        if (!total) {
            return fail("-S needs <total> or <total>@<file>, where <total> is a number from 1 to " +
                        std::to_string(MAX_SYMBOLS) + ": '" + value + "'");
        }

        options.totalSymbols = *total;
        options.symbolsFile.clear();

        if (at != std::string::npos) {
            options.symbolsFile = value.substr(at + 1);

            if (options.symbolsFile.empty()) {
                return fail("-S has no file name after '@': '" + value + "'");
            }
        }

        symbolsSeen = true;

        return true;
    }

    bool parseRateOption(const std::string &value) {
        if (value.find(';') != std::string::npos) {
            return fail("unsupported QD flag: the rate limiter bucket ';<time>' in '-r " + value + "'");
        }

        if (value.size() < 3 || value.compare(0, 2, "*/") != 0) {
            return fail("-r needs */<rate> (for example */500k): '" + value + "'");
        }

        const auto rate = parseRate(value.substr(2));

        if (!rate) {
            return fail("-r has a bad rate (a positive number with an optional k, M or G): '" + value + "'");
        }

        options.recordsPerSecond = *rate;

        return true;
    }

    bool parseIntegerOption(const char *flag, const std::string &value, std::int64_t limit, std::int64_t &out) {
        const auto parsed = parsePositive(value, limit);

        if (!parsed) {
            return fail(std::string(flag) + " needs a number from 1 to " + std::to_string(limit) + ": '" + value +
                        "'");
        }

        out = *parsed;

        return true;
    }

    bool parseDurationOption(const char *flag, const std::string &value, std::int64_t &out) {
        const auto parsed = parseDuration(value);

        if (!parsed) {
            return fail(std::string(flag) + " needs a positive time with ms, s or m (for example 10s): '" + value +
                        "'");
        }

        out = *parsed;

        return true;
    }

    bool parseProperty(const std::string &argument) {
        const auto equals = argument.find('=');

        if (equals == std::string::npos || equals < 3) {
            return fail("-D needs <key>=<value>: '" + argument + "'");
        }

        options.properties.emplace_back(argument.substr(2, equals - 2), argument.substr(equals + 1));

        return true;
    }

    // The isolate checks the -XX: options and the values of -Xmx, -Xms and -Xmn (a wrong one fails its creation), but
    // ignores the other -X arguments, so that a typo there would go unnoticed: they are rejected here.
    bool parseIsolateArgument(const std::string &argument) {
        const bool hasValue = argument.size() > 4;
        const bool heapSize = hasValue && (argument.compare(0, 4, "-Xmx") == 0 || argument.compare(0, 4, "-Xms") == 0 ||
                                           argument.compare(0, 4, "-Xmn") == 0);
        const bool runtimeOption = hasValue && argument.compare(0, 4, "-XX:") == 0;

        if (!heapSize && !runtimeOption) {
            return fail(argument + " is not an option of the isolate, which takes -Xmx<size>, -Xms<size>, -Xmn<size> " +
                        "and -XX:<option>");
        }

        options.isolateArguments.push_back(argument);

        return true;
    }

    bool run(const std::vector<std::string> &args) {
        if (args.size() < 2) {
            return fail("no mode");
        }

        if (args[1] == "c") {
            return fail("unsupported mode 'c' (the consumer mode of QD NetTest); only the mode 'p' exists");
        }

        if (args[1] != "p") {
            return fail("unknown mode '" + args[1] + "'; only the mode 'p' exists");
        }

        for (std::size_t index = 2; index < args.size(); ++index) {
            const auto &argument = args[index];

            // The flags with a value take the next argument as it is.
            const auto value = [&](const char *flag) -> const std::string * {
                if (index + 1 >= args.size()) {
                    fail(std::string("missing value of ") + flag);

                    return nullptr;
                }

                return &args[++index];
            };

            if (argument == "-S") {
                const auto *text = value("-S");

                if (text == nullptr || !parseSymbols(*text)) {
                    return false;
                }
            } else if (argument == "-C") {
                const auto *text = value("-C");
                std::int64_t instances = 0;

                if (text == nullptr || !parseIntegerOption("-C", *text, MAX_INSTANCES, instances)) {
                    return false;
                }

                options.instances = static_cast<std::int32_t>(instances);
            } else if (argument == "-r") {
                const auto *text = value("-r");

                if (text == nullptr || !parseRateOption(*text)) {
                    return false;
                }
            } else if (argument == "-s") {
                const auto *text = value("-s");

                if (text == nullptr || !parseDurationOption("-s", *text, options.periodMillis)) {
                    return false;
                }
            } else if (argument == "-n") {
                const auto *text = value("-n");

                if (text == nullptr) {
                    return false;
                }

                if (text->empty()) {
                    return fail("-n needs a non-empty name");
                }

                options.name = *text;
            } else if (argument == "--batch") {
                const auto *text = value("--batch");
                std::int64_t batch = 0;

                if (text == nullptr || !parseIntegerOption("--batch", *text, MAX_BATCH, batch)) {
                    return false;
                }

                options.batch = static_cast<std::int32_t>(batch);
            } else if (argument == "--event") {
                const auto *text = value("--event");

                if (text == nullptr) {
                    return false;
                }

                if (*text == "Trade") {
                    options.event = EventKind::TRADE;
                } else if (*text == "TimeAndSale") {
                    options.event = EventKind::TIME_AND_SALE;
                } else {
                    return fail("--event is Trade or TimeAndSale: '" + *text + "'");
                }
            } else if (argument == "--duration") {
                const auto *text = value("--duration");
                std::int64_t duration = 0;

                if (text == nullptr || !parseDurationOption("--duration", *text, duration)) {
                    return false;
                }

                options.durationMillis = duration;
            } else if (argument == "--reuse") {
                options.reuse = true;
            } else if (argument == "--legacy") {
                options.legacy = true;
            } else if (argument == "-c") {
                return fail("unsupported QD flag: -c (the collector)");
            } else if (argument.compare(0, 2, "-D") == 0) {
                if (!parseProperty(argument)) {
                    return false;
                }
            } else if (argument.compare(0, 2, "-X") == 0) {
                if (!parseIsolateArgument(argument)) {
                    return false;
                }
            } else if (!argument.empty() && argument[0] == '-' && argument.size() > 1) {
                return fail("unknown flag " + argument);
            } else if (!addressSeen) {
                options.address = argument;
                addressSeen = true;
            } else {
                return fail("unexpected argument '" + argument + "' after the address '" + options.address + "'");
            }
        }

        if (!addressSeen || options.address.empty()) {
            return fail("the address is missing");
        }

        if (!symbolsSeen) {
            return fail("-S is missing");
        }

        if (options.instances > 1 && isServerAddress(options.address)) {
            return fail("-C " + std::to_string(options.instances) + " cannot be used with the server address '" +
                        options.address + "': every instance would bind the same port");
        }

        return true;
    }
};

} // namespace

std::optional<std::int64_t> parseDuration(const std::string &text) {
    std::int64_t multiplier = 0;
    std::size_t suffixLength = 0;

    if (text.size() > 2 && text.compare(text.size() - 2, 2, "ms") == 0) {
        multiplier = 1;
        suffixLength = 2;
    } else if (text.size() > 1 && text.back() == 's') {
        multiplier = 1'000;
        suffixLength = 1;
    } else if (text.size() > 1 && text.back() == 'm') {
        multiplier = 60'000;
        suffixLength = 1;
    } else {
        return std::nullopt;
    }

    const auto number = text.substr(0, text.size() - suffixLength);

    if (!isDigits(number) || number.size() > 15) {
        return std::nullopt;
    }

    const auto value = std::strtoll(number.c_str(), nullptr, 10);

    if (value < 1 || value > std::numeric_limits<std::int64_t>::max() / multiplier) {
        return std::nullopt;
    }

    return value * multiplier;
}

std::optional<std::int64_t> parseRate(const std::string &text) {
    if (text.empty()) {
        return std::nullopt;
    }

    double multiplier = 1;
    auto number = text;

    switch (text.back()) {
    case 'k':
        multiplier = 1e3;
        number.pop_back();
        break;
    case 'M':
        multiplier = 1e6;
        number.pop_back();
        break;
    case 'G':
        multiplier = 1e9;
        number.pop_back();
        break;
    default:
        break;
    }

    // Digits with at most one dot: no sign, no exponent, no spaces.
    if (number.empty() || number.front() == '.' || number.back() == '.') {
        return std::nullopt;
    }

    std::size_t dots = 0;

    for (const char symbol : number) {
        if (symbol == '.') {
            ++dots;
        } else if (symbol < '0' || symbol > '9') {
            return std::nullopt;
        }
    }

    if (dots > 1) {
        return std::nullopt;
    }

    const auto rate = std::strtod(number.c_str(), nullptr) * multiplier;

    if (!(rate >= 1.0) || rate > 1e12) {
        return std::nullopt;
    }

    return static_cast<std::int64_t>(std::llround(rate));
}

bool isServerAddress(const std::string &address) {
    return !address.empty() && address.front() == ':';
}

ParseResult parseArguments(const std::vector<std::string> &args) {
    Parser parser;
    ParseResult result;

    result.ok = parser.run(args);
    result.error = parser.error;

    if (result.ok) {
        result.options = std::move(parser.options);
    }

    return result;
}

std::string usageText() {
    return "Usage: NetTest p <address> -S <symbols> [options]\n"
           "\n"
           "  <address>            host:port to connect to, :port to listen on\n"
           "  -S <total>           the symbol pool: <total> random symbols of 1-6 capital letters\n"
           "  -S <total>@<file>    the first <total> symbols of the IPF file, in file order\n"
           "  -C <n>               the number of instances, default 1 (not above 1 with a :port address)\n"
           "  -r */<rate>          the total rate in records per second, with k, M or G (for example */1M)\n"
           "  -s <period>          the statistics period, default 10s (ms, s or m)\n"
           "  -n <name>            the name in the statistics line, default NetTest\n"
           "  --batch <n>          the events per publishing call, default 100\n"
           "  --event <type>       Trade (default) or TimeAndSale\n"
           "  --reuse              republish the pre-built events in place instead of copying them\n"
           "  --legacy             publish through the deprecated dxfg_DXPublisher_publishEvents\n"
           "  --duration <time>    end the run after the time (ms, s or m); Ctrl+C ends it at once\n"
           "  -D<key>=<value>      set a system property before the first endpoint is created\n"
           "  -Xmx<size>, -Xms<size>, -Xmn<size>\n"
           "                       the maximum, initial and young generation heap sizes of the isolate (512m, 1g)\n"
           "  -XX:<option>         an option of the isolate (for example -XX:+PrintGC -XX:+PrintGCSummary)\n"
           "\n"
           "The consumer mode 'c' and the QD flags -c, /r and /s symbol splits, {len,max} symbol lengths and the\n"
           "rate limiter bucket ;<time> are not supported.\n";
}

bool applySystemProperties(graal_isolatethread_t *thread, const Options &options, std::string &error) {
    for (const auto &[key, value] : options.properties) {
        if (dxfg_v4_system_set_property(thread, key.c_str(), value.c_str()) != DXFG_EXECUTE_SUCCESSFULLY) {
            error = "cannot set the system property '" + key + "'";

            return false;
        }
    }

    return true;
}

} // namespace nettest
} // namespace dxfg
