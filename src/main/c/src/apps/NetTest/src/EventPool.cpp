// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/EventPool.hpp>

#include <chrono>
#include <cmath>
#include <cstring>
#include <limits>
#include <type_traits>
#include <utility>

namespace dxfg {
namespace nettest {

namespace {

constexpr std::int64_t NANOS_PER_SECOND = 1'000'000'000;
constexpr std::int64_t NANOS_PER_MILLI = 1'000'000;

// Copies the string with new[]; free it with delete[].
const char *duplicateSymbol(const char *symbol) {
    const auto length = std::strlen(symbol);
    auto *copy = new char[length + 1];

    std::memcpy(copy, symbol, length + 1);

    return copy;
}

// The time of the event and the sequence for the structure of either kind.
struct Stamp {
    std::int64_t packed;
    std::int32_t nanoPart;
    double price;
    double size;
};

void applyStamp(dxfg_trade_t &event, const Stamp &stamp) {
    event.trade_base.time_sequence = stamp.packed;
    event.trade_base.time_nano_part = stamp.nanoPart;
    event.trade_base.price = stamp.price;
    event.trade_base.size = stamp.size;
}

void applyStamp(dxfg_time_and_sale_t &event, const Stamp &stamp) {
    event.index = stamp.packed;
    event.time_nano_part = stamp.nanoPart;
    event.price = stamp.price;
    event.size = stamp.size;
}

const char *symbolOf(const dxfg_trade_t &event) {
    return event.trade_base.market_event.event_symbol;
}

const char *symbolOf(const dxfg_time_and_sale_t &event) {
    return event.market_event.event_symbol;
}

void setSymbol(dxfg_trade_t &event, const char *symbol) {
    event.trade_base.market_event.event_symbol = symbol;
}

void setSymbol(dxfg_time_and_sale_t &event, const char *symbol) {
    event.market_event.event_symbol = symbol;
}

// The time in nanoseconds since the Unix epoch of the system clock.
std::int64_t nowEpochNanos() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::system_clock::now().time_since_epoch())
        .count();
}

} // namespace

std::int64_t packTimeSequence(std::int64_t epochNanos, std::uint32_t sequence) {
    const auto seconds = epochNanos / NANOS_PER_SECOND;
    const auto millis = (epochNanos / NANOS_PER_MILLI) % 1000;

    return static_cast<std::int64_t>((static_cast<std::uint64_t>(seconds) << 32) |
                                     (static_cast<std::uint64_t>(millis) << 22) |
                                     (sequence & EventPool::SEQUENCE_MASK));
}

void EventBatch::clear() {
    if (owned_) {
        for (auto *event : events_) {
            if (kind_ == EventKind::TRADE) {
                auto *trade = reinterpret_cast<dxfg_trade_t *>(event);

                delete[] symbolOf(*trade);
                delete trade;
            } else {
                auto *sale = reinterpret_cast<dxfg_time_and_sale_t *>(event);

                delete[] symbolOf(*sale);
                delete sale;
            }
        }
    }

    events_.clear();
    owned_ = false;
}

EventPool::EventPool(EventKind kind, Symbols symbols, std::uint64_t seed)
    : kind_(kind), symbols_(std::move(symbols)), random_(seed * 0x9E3779B97F4A7C15ULL + 0x2545F4914F6CDD1DULL) {
    const auto not_a_number = std::numeric_limits<double>::quiet_NaN();

    if (random_ == 0) {
        random_ = 0x2545F4914F6CDD1DULL;
    }

    // Prices of 1.00-1000.99 and integer sizes of 1-1000.
    prices_.resize(TABLE_SIZE);
    sizes_.resize(TABLE_SIZE);

    for (std::size_t index = 0; index < TABLE_SIZE; ++index) {
        prices_[index] = static_cast<double>(100 + nextRandom() % 100'000) / 100.0;
        sizes_[index] = static_cast<double>(1 + nextRandom() % 1000);
    }

    if (kind_ == EventKind::TRADE) {
        trades_.resize(symbols_->size());

        for (std::size_t index = 0; index < trades_.size(); ++index) {
            auto &trade = trades_[index];

            trade = dxfg_trade_t{};
            trade.trade_base.market_event.event_type.clazz = DXFG_EVENT_TRADE;
            trade.trade_base.market_event.event_symbol = (*symbols_)[index].c_str();
            trade.trade_base.price = not_a_number;
            trade.trade_base.change = not_a_number;
            trade.trade_base.size = not_a_number;
            trade.trade_base.day_volume = not_a_number;
            trade.trade_base.day_turnover = not_a_number;
        }
    } else {
        sales_.resize(symbols_->size());

        for (std::size_t index = 0; index < sales_.size(); ++index) {
            auto &sale = sales_[index];

            sale = dxfg_time_and_sale_t{};
            sale.market_event.event_type.clazz = DXFG_EVENT_TIME_AND_SALE;
            sale.market_event.event_symbol = (*symbols_)[index].c_str();
            sale.price = not_a_number;
            sale.size = not_a_number;
            sale.bid_price = not_a_number;
            sale.ask_price = not_a_number;
        }
    }
}

// xorshift64*: cheap and good enough to pick symbols and table rows.
std::uint64_t EventPool::nextRandom() {
    random_ ^= random_ >> 12;
    random_ ^= random_ << 25;
    random_ ^= random_ >> 27;

    return random_ * 0x2545F4914F6CDD1DULL;
}

std::size_t EventPool::size() const {
    return symbols_->size();
}

const dxfg_event_type_t *EventPool::poolEvent(std::size_t index) const {
    if (kind_ == EventKind::TRADE) {
        return reinterpret_cast<const dxfg_event_type_t *>(&trades_[index]);
    }

    return reinterpret_cast<const dxfg_event_type_t *>(&sales_[index]);
}

void EventPool::fillAt(EventBatch &batch, std::size_t count, bool reuse, std::int64_t epochNanos) {
    batch.clear();
    batch.kind_ = kind_;
    batch.events_.reserve(count);
    batch.owned_ = !reuse;

    const auto nanoPart = static_cast<std::int32_t>(epochNanos % NANOS_PER_MILLI);
    const auto poolSize = symbols_->size();

    if (poolSize == 0) {
        return;
    }

    const auto fillKind = [&](auto &events) {
        for (std::size_t item = 0; item < count; ++item) {
            const auto random = nextRandom();
            auto &source = events[(random >> 32) % poolSize];
            const auto row = (random >> 8) % TABLE_SIZE;
            const Stamp stamp{packTimeSequence(epochNanos, sequence_++), nanoPart, prices_[row], sizes_[row]};

            if (reuse) {
                applyStamp(source, stamp);
                batch.events_.push_back(reinterpret_cast<dxfg_event_type_t *>(&source));
            } else {
                using Event = std::remove_reference_t<decltype(source)>;

                auto *copy = new Event(source);

                setSymbol(*copy, duplicateSymbol(symbolOf(source)));
                applyStamp(*copy, stamp);
                batch.events_.push_back(reinterpret_cast<dxfg_event_type_t *>(copy));
            }
        }
    };

    if (kind_ == EventKind::TRADE) {
        fillKind(trades_);
    } else {
        fillKind(sales_);
    }
}

void EventPool::fill(EventBatch &batch, std::size_t count, bool reuse) {
    fillAt(batch, count, reuse, nowEpochNanos());
}

} // namespace nettest
} // namespace dxfg
