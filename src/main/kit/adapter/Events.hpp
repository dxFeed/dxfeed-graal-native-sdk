// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The event values of the protocol (protocol.md, "Values"): one field table per event type of the kit, which maps the
// field names of the protocol to the members of the C structs, and the text form of the values.

#ifndef DXFG_KIT_EVENTS_HPP
#define DXFG_KIT_EVENTS_HPP

#include <dxfg_api.h>

#include <cstddef>
#include <cstdint>
#include <deque>
#include <memory>
#include <optional>
#include <string>
#include <vector>

namespace dxfg {
namespace kit {

// The C type of a struct member.
enum class Kind { I16, I32, I64, DOUBLE, STRING };

// A member of an event struct: its name in the protocol (the member name, base structs flattened), its type and its
// offset from the start of the event struct.
struct Field {
    const char *name;
    Kind kind;
    std::size_t offset;
};

// An event type of the kit: its name in the protocol, its class, the size of its struct and its fields. The counter
// is the field that publish-burst sets to the number of the event, or NULL.
struct EventType {
    const char *name;
    dxfg_event_clazz_t clazz;
    std::size_t size;
    std::vector<Field> fields;
    const char *counter;
};

// The type of the name (Quote, Profile, TimeAndSale, Order, Candle, Message), or NULL.
const EventType *findType(const std::string &name);

// The type of the class, or NULL.
const EventType *findType(dxfg_event_clazz_t clazz);

// Percent-encodes every byte that is not a letter, a digit, '-', '.' or '_'.
std::string percentEncode(const std::string &bytes);

// Decodes %XX sequences; nullopt if one is malformed.
std::optional<std::string> percentDecode(const std::string &text);

// The event line of the protocol: "event <Type> <field>=<value>...", every field of the table.
std::string formatEvent(const dxfg_event_type_t *event);

// An event that the adapter publishes: a zeroed struct of the type with the class set, plus the strings its members
// point to. The SDK only reads the event during dxfg_v4_DXPublisher_publishEvents(), so the event and its strings
// belong to the adapter and are freed with this object.
class OwnedEvent {
    const EventType &type_;
    // Words, not std::max_align_t: with GCC on x86-64 its value-initialization leaves its padding and the unused bytes
    // of its long double member unset, so the event was not zeroed there. The members of the events need no more than
    // 8-byte alignment.
    std::unique_ptr<std::uint64_t[]> storage_;
    std::deque<std::string> strings_;

  public:
    explicit OwnedEvent(const EventType &type);

    // Sets the field from its protocol text; false with the reason if the field or the value is bad.
    bool set(const std::string &name, const std::string &value, std::string &error);

    dxfg_event_type_t *get() {
        return reinterpret_cast<dxfg_event_type_t *>(storage_.get());
    }
};

} // namespace kit
} // namespace dxfg

#endif // DXFG_KIT_EVENTS_HPP
