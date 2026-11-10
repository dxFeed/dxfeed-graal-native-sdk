// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The event pool of one NetTest instance: one pre-built event per symbol, a table of prices and sizes and the batch
// fill that rewrites the time, the sequence, the price and the size.

#ifndef DXFG_NETTEST_EVENT_POOL_HPP
#define DXFG_NETTEST_EVENT_POOL_HPP

#include <NetTest/Args.hpp>
#include <NetTest/SymbolPool.hpp>

#include <dxfg_api.h>

#include <cstddef>
#include <cstdint>
#include <vector>

namespace dxfg {
namespace nettest {

// The events of one publishing call and the list structure that the publish functions take
// (dxfg_v4_event_type_list is dxfg_event_type_list). A batch of a reusing fill points at the events of the pool; a batch
// of a copying fill owns its events and their symbol strings and frees them in clear() or at destruction.
class EventBatch {
    friend class EventPool;

    std::vector<dxfg_event_type_t *> events_{};
    dxfg_event_type_list list_{0, nullptr};
    EventKind kind_ = EventKind::TRADE;
    bool owned_ = false;

  public:
    EventBatch() = default;
    EventBatch(const EventBatch &) = delete;
    EventBatch &operator=(const EventBatch &) = delete;

    ~EventBatch() {
        clear();
    }

    // Frees the owned events (never the events of the pool) and empties the batch.
    void clear();

    std::size_t size() const {
        return events_.size();
    }

    bool owned() const {
        return owned_;
    }

    // The event i of the batch.
    const dxfg_event_type_t *event(std::size_t index) const {
        return events_[index];
    }

    // The list to pass to the publish functions; valid until the next fill or clear() of the batch.
    dxfg_event_type_list *list() {
        list_.size = static_cast<std::int32_t>(events_.size());
        list_.elements = events_.empty() ? nullptr : events_.data();

        return &list_;
    }
};

// Packs the time and the sequence as Trade.time_sequence and TimeAndSale.index do: the seconds in the higher 32 bits,
// the milliseconds in bits 22 to 31, the sequence in bits 0 to 21.
std::int64_t packTimeSequence(std::int64_t epochNanos, std::uint32_t sequence);

// Not thread-safe: one pool for each generator thread.
class EventPool {
    EventKind kind_;
    Symbols symbols_;
    std::vector<dxfg_trade_t> trades_{};
    std::vector<dxfg_time_and_sale_t> sales_{};
    std::vector<double> prices_{};
    std::vector<double> sizes_{};
    std::uint64_t random_;
    std::uint32_t sequence_ = 0;

    std::uint64_t nextRandom();

  public:
    static constexpr std::size_t TABLE_SIZE = 1024;
    static constexpr std::uint32_t SEQUENCE_MASK = (1U << 22) - 1;

    // Builds one event per symbol; the events point into the strings of the shared symbols, which must live as long as
    // the pool does. The seed makes the symbol choice and the price and size table of the instance.
    EventPool(EventKind kind, Symbols symbols, std::uint64_t seed);

    // The number of the pool events, the number of the symbols.
    std::size_t size() const;

    // The pool event of the symbol i.
    const dxfg_event_type_t *poolEvent(std::size_t index) const;

    // The sequence that the next event takes (22 bits, before the mask).
    std::uint32_t nextSequence() const {
        return sequence_;
    }

    // The prices and the sizes of the table (the same length).
    const std::vector<double> &prices() const {
        return prices_;
    }

    const std::vector<double> &sizes() const {
        return sizes_;
    }

    // Fills the batch (clearing it first) with `count` events of random symbols of the pool at the time given in
    // nanoseconds since the Unix epoch: each event gets the time, the nano part, the next sequence of the instance
    // (22 bits, wrapping), a price and a size from the table. With `reuse` the batch points at the pool events, which
    // are rewritten in place (a symbol drawn twice appears twice); otherwise the batch owns fresh copies.
    void fillAt(EventBatch &batch, std::size_t count, bool reuse, std::int64_t epochNanos);

    // The same at the current time.
    void fill(EventBatch &batch, std::size_t count, bool reuse);
};

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_EVENT_POOL_HPP
