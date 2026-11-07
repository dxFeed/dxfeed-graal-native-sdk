// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "Events.hpp"

#include <cerrno>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <limits>

namespace dxfg {
namespace kit {

namespace {

// A member of the struct T.
#define KIT_FIELD(T, member, kind) Field{#member, Kind::kind, offsetof(T, member)}

// A member of the base struct B that the struct T embeds as its member `base`.
#define KIT_BASE_FIELD(T, base, B, member, kind) Field{#member, Kind::kind, offsetof(T, base) + offsetof(B, member)}

// The members of dxfg_market_event_t, embedded as the member `market_event` of T.
#define KIT_MARKET_EVENT(T)                                                                                            \
    KIT_BASE_FIELD(T, market_event, dxfg_market_event_t, event_symbol, STRING),                                        \
        KIT_BASE_FIELD(T, market_event, dxfg_market_event_t, event_time, I64)

// A member of dxfg_order_base_t, embedded as the member `order_base` of dxfg_order_t.
#define KIT_ORDER_FIELD(member, kind)                                                                                  \
    Field {                                                                                                            \
        #member, Kind::kind, offsetof(dxfg_order_t, order_base) + offsetof(dxfg_order_base_t, member)                 \
    }

// The members of dxfg_market_event_t inside dxfg_order_base_t inside dxfg_order_t.
#define KIT_ORDER_MARKET_FIELD(member, kind)                                                                           \
    Field {                                                                                                            \
        #member, Kind::kind,                                                                                           \
            offsetof(dxfg_order_t, order_base) + offsetof(dxfg_order_base_t, market_event) +                           \
                offsetof(dxfg_market_event_t, member)                                                                  \
    }

const std::vector<EventType> &eventTypes() {
    static const std::vector<EventType> types{
        {"Quote",
         DXFG_EVENT_QUOTE,
         sizeof(dxfg_quote_t),
         {
             KIT_MARKET_EVENT(dxfg_quote_t),
             KIT_FIELD(dxfg_quote_t, time_millis_sequence, I32),
             KIT_FIELD(dxfg_quote_t, time_nano_part, I32),
             KIT_FIELD(dxfg_quote_t, bid_time, I64),
             KIT_FIELD(dxfg_quote_t, bid_exchange_code, I16),
             KIT_FIELD(dxfg_quote_t, bid_price, DOUBLE),
             KIT_FIELD(dxfg_quote_t, bid_size, DOUBLE),
             KIT_FIELD(dxfg_quote_t, ask_time, I64),
             KIT_FIELD(dxfg_quote_t, ask_exchange_code, I16),
             KIT_FIELD(dxfg_quote_t, ask_price, DOUBLE),
             KIT_FIELD(dxfg_quote_t, ask_size, DOUBLE),
         },
         "bid_price"},
        {"Profile",
         DXFG_EVENT_PROFILE,
         sizeof(dxfg_profile_t),
         {
             KIT_MARKET_EVENT(dxfg_profile_t),
             KIT_FIELD(dxfg_profile_t, description, STRING),
             KIT_FIELD(dxfg_profile_t, status_reason, STRING),
             KIT_FIELD(dxfg_profile_t, halt_start_time, I64),
             KIT_FIELD(dxfg_profile_t, halt_end_time, I64),
             KIT_FIELD(dxfg_profile_t, high_limit_price, DOUBLE),
             KIT_FIELD(dxfg_profile_t, low_limit_price, DOUBLE),
             KIT_FIELD(dxfg_profile_t, high_52_week_price, DOUBLE),
             KIT_FIELD(dxfg_profile_t, low_52_week_price, DOUBLE),
             KIT_FIELD(dxfg_profile_t, beta, DOUBLE),
             KIT_FIELD(dxfg_profile_t, earnings_per_share, DOUBLE),
             KIT_FIELD(dxfg_profile_t, dividend_frequency, DOUBLE),
             KIT_FIELD(dxfg_profile_t, ex_dividend_amount, DOUBLE),
             KIT_FIELD(dxfg_profile_t, ex_dividend_day_id, I32),
             KIT_FIELD(dxfg_profile_t, shares, DOUBLE),
             KIT_FIELD(dxfg_profile_t, free_float, DOUBLE),
             KIT_FIELD(dxfg_profile_t, flags, I32),
         },
         nullptr},
        {"TimeAndSale",
         DXFG_EVENT_TIME_AND_SALE,
         sizeof(dxfg_time_and_sale_t),
         {
             KIT_MARKET_EVENT(dxfg_time_and_sale_t),
             KIT_FIELD(dxfg_time_and_sale_t, event_flags, I32),
             KIT_FIELD(dxfg_time_and_sale_t, index, I64),
             KIT_FIELD(dxfg_time_and_sale_t, time_nano_part, I32),
             KIT_FIELD(dxfg_time_and_sale_t, exchange_code, I16),
             KIT_FIELD(dxfg_time_and_sale_t, price, DOUBLE),
             KIT_FIELD(dxfg_time_and_sale_t, size, DOUBLE),
             KIT_FIELD(dxfg_time_and_sale_t, bid_price, DOUBLE),
             KIT_FIELD(dxfg_time_and_sale_t, ask_price, DOUBLE),
             KIT_FIELD(dxfg_time_and_sale_t, exchange_sale_conditions, STRING),
             KIT_FIELD(dxfg_time_and_sale_t, flags, I32),
             KIT_FIELD(dxfg_time_and_sale_t, buyer, STRING),
             KIT_FIELD(dxfg_time_and_sale_t, seller, STRING),
             KIT_FIELD(dxfg_time_and_sale_t, trade_id, I64),
         },
         "price"},
        {"Order",
         DXFG_EVENT_ORDER,
         sizeof(dxfg_order_t),
         {
             KIT_ORDER_MARKET_FIELD(event_symbol, STRING),
             KIT_ORDER_MARKET_FIELD(event_time, I64),
             KIT_ORDER_FIELD(event_flags, I32),
             KIT_ORDER_FIELD(index, I64),
             KIT_ORDER_FIELD(time_sequence, I64),
             KIT_ORDER_FIELD(time_nano_part, I32),
             KIT_ORDER_FIELD(action_time, I64),
             KIT_ORDER_FIELD(order_id, I64),
             KIT_ORDER_FIELD(aux_order_id, I64),
             KIT_ORDER_FIELD(price, DOUBLE),
             KIT_ORDER_FIELD(size, DOUBLE),
             KIT_ORDER_FIELD(executed_size, DOUBLE),
             KIT_ORDER_FIELD(count, I64),
             KIT_ORDER_FIELD(flags, I32),
             KIT_ORDER_FIELD(trade_id, I64),
             KIT_ORDER_FIELD(trade_price, DOUBLE),
             KIT_ORDER_FIELD(trade_size, DOUBLE),
             KIT_FIELD(dxfg_order_t, market_maker, STRING),
         },
         nullptr},
        {"Candle",
         DXFG_EVENT_CANDLE,
         sizeof(dxfg_candle_t),
         {
             KIT_FIELD(dxfg_candle_t, event_symbol, STRING),
             KIT_FIELD(dxfg_candle_t, event_time, I64),
             KIT_FIELD(dxfg_candle_t, event_flags, I32),
             KIT_FIELD(dxfg_candle_t, index, I64),
             KIT_FIELD(dxfg_candle_t, count, I64),
             KIT_FIELD(dxfg_candle_t, open, DOUBLE),
             KIT_FIELD(dxfg_candle_t, high, DOUBLE),
             KIT_FIELD(dxfg_candle_t, low, DOUBLE),
             KIT_FIELD(dxfg_candle_t, close, DOUBLE),
             KIT_FIELD(dxfg_candle_t, volume, DOUBLE),
             KIT_FIELD(dxfg_candle_t, vwap, DOUBLE),
             KIT_FIELD(dxfg_candle_t, bid_volume, DOUBLE),
             KIT_FIELD(dxfg_candle_t, ask_volume, DOUBLE),
             KIT_FIELD(dxfg_candle_t, imp_volatility, DOUBLE),
             KIT_FIELD(dxfg_candle_t, open_interest, DOUBLE),
         },
         nullptr},
        // The attachment of a Message is a UTF-8 string or NULL in C (dxfg_message_t).
        {"Message",
         DXFG_EVENT_MESSAGE,
         sizeof(dxfg_message_t),
         {
             KIT_FIELD(dxfg_message_t, event_symbol, STRING),
             KIT_FIELD(dxfg_message_t, event_time, I64),
             KIT_FIELD(dxfg_message_t, attachment, STRING),
         },
         nullptr},
    };

    return types;
}

#undef KIT_FIELD
#undef KIT_BASE_FIELD
#undef KIT_MARKET_EVENT
#undef KIT_ORDER_FIELD
#undef KIT_ORDER_MARKET_FIELD

bool isUnreserved(unsigned char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '.' ||
           c == '_';
}

int hexValue(char c) {
    if (c >= '0' && c <= '9') {
        return c - '0';
    }

    if (c >= 'a' && c <= 'f') {
        return c - 'a' + 10;
    }

    if (c >= 'A' && c <= 'F') {
        return c - 'A' + 10;
    }

    return -1;
}

std::string formatDouble(double value) {
    if (std::isnan(value)) {
        return "nan";
    }

    if (std::isinf(value)) {
        return value > 0 ? "inf" : "-inf";
    }

    char buffer[32];

    std::snprintf(buffer, sizeof buffer, "%.17g", value);

    return buffer;
}

// The value of the member at the address, as the protocol writes it.
std::string formatValue(const unsigned char *member, Kind kind) {
    switch (kind) {
    case Kind::I16: {
        std::int16_t value;

        std::memcpy(&value, member, sizeof value);

        return std::to_string(value);
    }
    case Kind::I32: {
        std::int32_t value;

        std::memcpy(&value, member, sizeof value);

        return std::to_string(value);
    }
    case Kind::I64: {
        std::int64_t value;

        std::memcpy(&value, member, sizeof value);

        return std::to_string(value);
    }
    case Kind::DOUBLE: {
        double value;

        std::memcpy(&value, member, sizeof value);

        return formatDouble(value);
    }
    case Kind::STRING:
    default: {
        const char *value;

        std::memcpy(&value, member, sizeof value);

        return value == nullptr ? "~" : percentEncode(value);
    }
    }
}

// A decimal integer of the range, the whole text.
std::optional<std::int64_t> parseInteger(const std::string &text, std::int64_t min, std::int64_t max) {
    if (text.empty()) {
        return std::nullopt;
    }

    errno = 0;
    char *end = nullptr;
    const long long value = std::strtoll(text.c_str(), &end, 10);

    if (errno == ERANGE || end != text.c_str() + text.size() || value < min || value > max) {
        return std::nullopt;
    }

    return static_cast<std::int64_t>(value);
}

// nan, inf, -inf or a decimal number, the whole text.
std::optional<double> parseDouble(const std::string &text) {
    if (text == "nan") {
        return std::numeric_limits<double>::quiet_NaN();
    }

    if (text == "inf") {
        return std::numeric_limits<double>::infinity();
    }

    if (text == "-inf") {
        return -std::numeric_limits<double>::infinity();
    }

    if (text.empty() || text.find_first_not_of("0123456789+-.eE") != std::string::npos) {
        return std::nullopt;
    }

    char *end = nullptr;
    const double value = std::strtod(text.c_str(), &end);

    if (end != text.c_str() + text.size()) {
        return std::nullopt;
    }

    return value;
}

} // namespace

const EventType *findType(const std::string &name) {
    for (const auto &type : eventTypes()) {
        if (name == type.name) {
            return &type;
        }
    }

    return nullptr;
}

const EventType *findType(dxfg_event_clazz_t clazz) {
    for (const auto &type : eventTypes()) {
        if (clazz == type.clazz) {
            return &type;
        }
    }

    return nullptr;
}

std::string percentEncode(const std::string &bytes) {
    static const char HEX[] = "0123456789ABCDEF";
    std::string text;

    text.reserve(bytes.size());

    for (const char c : bytes) {
        const auto byte = static_cast<unsigned char>(c);

        if (isUnreserved(byte)) {
            text += c;
        } else {
            text += '%';
            text += HEX[byte >> 4];
            text += HEX[byte & 0xF];
        }
    }

    return text;
}

std::optional<std::string> percentDecode(const std::string &text) {
    std::string bytes;

    bytes.reserve(text.size());

    for (std::size_t i = 0; i < text.size(); ++i) {
        if (text[i] != '%') {
            bytes += text[i];

            continue;
        }

        if (i + 2 >= text.size()) {
            return std::nullopt;
        }

        const int high = hexValue(text[i + 1]);
        const int low = hexValue(text[i + 2]);

        if (high < 0 || low < 0) {
            return std::nullopt;
        }

        bytes += static_cast<char>(high * 16 + low);
        i += 2;
    }

    return bytes;
}

std::string formatEvent(const dxfg_event_type_t *event) {
    const EventType *type = findType(event->clazz);

    if (type == nullptr) {
        return "event Unknown clazz=" + std::to_string(static_cast<int>(event->clazz));
    }

    const auto *bytes = reinterpret_cast<const unsigned char *>(event);
    std::string line = "event ";

    line += type->name;

    for (const auto &field : type->fields) {
        line += ' ';
        line += field.name;
        line += '=';
        line += formatValue(bytes + field.offset, field.kind);
    }

    return line;
}

// The storage of OwnedEvent is an array of words.
static_assert(alignof(dxfg_quote_t) <= alignof(std::uint64_t) && alignof(dxfg_profile_t) <= alignof(std::uint64_t) &&
                  alignof(dxfg_time_and_sale_t) <= alignof(std::uint64_t) &&
                  alignof(dxfg_order_t) <= alignof(std::uint64_t) && alignof(dxfg_candle_t) <= alignof(std::uint64_t) &&
                  alignof(dxfg_message_t) <= alignof(std::uint64_t),
              "an event needs more alignment than a word");

OwnedEvent::OwnedEvent(const EventType &type)
    : type_(type),
      storage_(std::make_unique<std::uint64_t[]>((type.size + sizeof(std::uint64_t) - 1) / sizeof(std::uint64_t))) {
    get()->clazz = type.clazz;
}

bool OwnedEvent::set(const std::string &name, const std::string &value, std::string &error) {
    const Field *field = nullptr;

    for (const auto &candidate : type_.fields) {
        if (name == candidate.name) {
            field = &candidate;

            break;
        }
    }

    if (field == nullptr) {
        error = std::string("unknown field ") + type_.name + "." + name;

        return false;
    }

    auto *member = reinterpret_cast<unsigned char *>(storage_.get()) + field->offset;
    const auto bad = [&] {
        error = "bad value of " + name + ": " + value;

        return false;
    };

    switch (field->kind) {
    case Kind::I16: {
        auto parsed = parseInteger(value, std::numeric_limits<std::int16_t>::min(),
                                   std::numeric_limits<std::int16_t>::max());

        if (!parsed) {
            return bad();
        }

        const auto narrow = static_cast<std::int16_t>(*parsed);

        std::memcpy(member, &narrow, sizeof narrow);

        return true;
    }
    case Kind::I32: {
        auto parsed = parseInteger(value, std::numeric_limits<std::int32_t>::min(),
                                   std::numeric_limits<std::int32_t>::max());

        if (!parsed) {
            return bad();
        }

        const auto narrow = static_cast<std::int32_t>(*parsed);

        std::memcpy(member, &narrow, sizeof narrow);

        return true;
    }
    case Kind::I64: {
        auto parsed = parseInteger(value, std::numeric_limits<std::int64_t>::min(),
                                   std::numeric_limits<std::int64_t>::max());

        if (!parsed) {
            return bad();
        }

        std::memcpy(member, &*parsed, sizeof *parsed);

        return true;
    }
    case Kind::DOUBLE: {
        auto parsed = parseDouble(value);

        if (!parsed) {
            return bad();
        }

        std::memcpy(member, &*parsed, sizeof *parsed);

        return true;
    }
    case Kind::STRING:
    default: {
        const char *pointer = nullptr;

        if (value != "~") {
            auto decoded = percentDecode(value);

            if (!decoded) {
                return bad();
            }

            strings_.push_back(std::move(*decoded));
            pointer = strings_.back().c_str();
        }

        std::memcpy(member, &pointer, sizeof pointer);

        return true;
    }
    }
}

} // namespace kit
} // namespace dxfg
