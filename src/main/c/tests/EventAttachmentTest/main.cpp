// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The attachment of Message and Configuration (see dxfg_message_t): a UTF-8 string or NULL in C, the attachment object
// in Java. The tests pass the events through the QD text tapes, where Java writes the attachment as
// "<toString()> <Base64 of the serialized object>".

#include <catch.hpp>
#include <dxfg_api.h>

#include <cstdio>
#include <fstream>
#include <map>
#include <mutex>
#include <optional>
#include <sstream>
#include <string>
#include <vector>

namespace dxfg {
namespace test {

namespace {

// Written by the Java API (QD 3.355): Message("MSG-STRING", "attachment"), Message("MSG-NULL"),
// Message("MSG-INTEGER", 42), Configuration("CFG-STRING", "attachment", 1), Configuration("CFG-NULL", null, 2)
// and Configuration("CFG-INTEGER", 42, 3). The text tape of QD 3.355 writes the Configuration without an attachment as
// "null " and cannot read it back: Configuration.getAttachment() throws (EOFException), the SDK gives NULL then.
constexpr const char *JAVA_TAPE =
    "==DXP3\ttype=tape\tversion=QDS-3.355\topt=hs\ttime=field\t+RAW_DATA\n"
    "==RAW_DATA\n"
    "=Message\tEventSymbol\tEventTime\tMessage\n"
    "Message\tMSG-STRING\t20260927-135506.046+0000\t\"attachment rO0ABXQACmF0dGFjaG1lbnQ\"\n"
    "Message\tMSG-NULL\t20260927-135506.046+0000\t\\NULL\n"
    "Message\tMSG-INTEGER\t20260927-135506.046+0000\t\"42 "
    "rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAq\"\n"
    "=Configuration\tEventSymbol\tEventTime\tVersion\tConfiguration\n"
    "Configuration\tCFG-STRING\t20260927-135506.046+0000\t1\t\"attachment rO0ABXQACmF0dGFjaG1lbnQ\"\n"
    "Configuration\tCFG-NULL\t20260927-135506.046+0000\t2\t\"null \"\n"
    "Configuration\tCFG-INTEGER\t20260927-135506.046+0000\t3\t\"42 "
    "rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAq\"\n"
    "==\n";

// How Java writes the String "attachment" and the event without an attachment (e.g. new Message("MSG-NULL")).
constexpr const char *JAVA_STRING_ATTACHMENT = "\"attachment rO0ABXQACmF0dGFjaG1lbnQ\"";
constexpr const char *JAVA_NO_ATTACHMENT = "\\NULL";

struct ReceivedEvent {
    dxfg_event_clazz_t clazz;
    std::string symbol;
    std::optional<std::string> attachment;
    int32_t version;
};

graal_isolatethread_t *isolateThread() {
    static graal_isolatethread_t *thread = [] {
        graal_isolate_t *isolate = nullptr;
        graal_isolatethread_t *result = nullptr;

        return graal_create_isolate(nullptr, &isolate, &result) == 0 ? result : nullptr;
    }();

    return thread;
}

dxfg_java_object_handler *handler(void *object) {
    return static_cast<dxfg_java_object_handler *>(object);
}

std::optional<std::string> toOptional(const void *attachment) {
    if (attachment == nullptr) {
        return std::nullopt;
    }

    return std::string(static_cast<const char *>(attachment));
}

// The Windows tape connector needs a relative path (the paths are relative to the current directory).
std::string tempPath(const std::string &name) {
    return "EventAttachmentTest-" + name + ".txt";
}

void writeFile(const std::string &path, const std::string &content) {
    std::ofstream file(path, std::ios::binary);

    file << content;
}

dxfg_endpoint_t *buildEndpoint(graal_isolatethread_t *thread, dxfg_endpoint_role_t role) {
    dxfg_endpoint_builder_t *builder = dxfg_DXEndpoint_newBuilder(thread);

    REQUIRE(builder != nullptr);
    REQUIRE(dxfg_DXEndpoint_Builder_withRole(thread, builder, role) == 0);
    // The tape connector subscribes to everything with the wildcard.
    REQUIRE(dxfg_DXEndpoint_Builder_withProperty(thread, builder, "dxfeed.wildcard.enable", "true") == 0);

    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_Builder_build(thread, builder);

    dxfg_JavaObjectHandler_release(thread, handler(builder));
    REQUIRE(endpoint != nullptr);

    return endpoint;
}

struct Collector {
    std::mutex mutex{};
    std::vector<ReceivedEvent> events{};
};

// The SDK frees the events after the listener returns, so the listener copies the attachments.
void collectEvents(graal_isolatethread_t *, dxfg_event_type_list *events, void *userData) {
    auto *collector = static_cast<Collector *>(userData);
    std::lock_guard lock{collector->mutex};

    for (int32_t i = 0; i < events->size; ++i) {
        const dxfg_event_type_t *event = events->elements[i];

        if (event->clazz == DXFG_EVENT_MESSAGE) {
            const auto *message = reinterpret_cast<const dxfg_message_t *>(event);

            collector->events.push_back(
                {event->clazz, message->event_symbol, toOptional(message->attachment), 0});
        } else if (event->clazz == DXFG_EVENT_CONFIGURATION) {
            const auto *configuration = reinterpret_cast<const dxfg_configuration_t *>(event);

            collector->events.push_back({event->clazz, configuration->event_symbol,
                                         toOptional(configuration->attachment), configuration->version});
        }
    }
}

// Reads the Message and Configuration events of the tape by the key "<clazz>:<symbol>".
std::map<std::string, ReceivedEvent> readTape(const std::string &path) {
    graal_isolatethread_t *thread = isolateThread();
    dxfg_endpoint_t *endpoint = buildEndpoint(thread, DXFG_ENDPOINT_ROLE_STREAM_FEED);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    Collector collector{};
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &collectEvents, &collector);
    dxfg_wildcard_symbol_t wildcard{{WILDCARD}};
    std::vector<dxfg_subscription_t *> subscriptions{};

    REQUIRE(feed != nullptr);
    REQUIRE(listener != nullptr);

    for (const auto clazz : {DXFG_EVENT_MESSAGE, DXFG_EVENT_CONFIGURATION}) {
        dxfg_subscription_t *subscription = dxfg_DXFeed_createSubscription(thread, feed, clazz);

        REQUIRE(subscription != nullptr);
        REQUIRE(dxfg_DXFeedSubscription_addEventListener(thread, subscription, listener) == 0);
        REQUIRE(dxfg_DXFeedSubscription_addSymbol(thread, subscription, &wildcard.supper) == 0);
        subscriptions.push_back(subscription);
    }

    const auto address = "file:" + path + "[speed=max]";

    REQUIRE(dxfg_DXEndpoint_connect(thread, endpoint, address.c_str()) == 0);
    REQUIRE(dxfg_DXEndpoint_awaitNotConnected(thread, endpoint) == 0);
    REQUIRE(dxfg_DXEndpoint_awaitProcessed(thread, endpoint) == 0);
    // Waits for the listener calls.
    REQUIRE(dxfg_DXEndpoint_closeAndAwaitTermination(thread, endpoint) == 0);

    for (auto *subscription : subscriptions) {
        dxfg_JavaObjectHandler_release(thread, handler(subscription));
    }

    dxfg_JavaObjectHandler_release(thread, handler(listener));
    dxfg_JavaObjectHandler_release(thread, handler(feed));
    dxfg_JavaObjectHandler_release(thread, handler(endpoint));

    std::lock_guard lock{collector.mutex};
    std::map<std::string, ReceivedEvent> result{};

    for (const auto &event : collector.events) {
        const auto *prefix = event.clazz == DXFG_EVENT_MESSAGE ? "Message:" : "Configuration:";

        result.emplace(prefix + event.symbol, event);
    }

    return result;
}

void writeTape(const std::string &path, std::vector<dxfg_event_type_t *> events) {
    graal_isolatethread_t *thread = isolateThread();

    std::remove(path.c_str());

    dxfg_endpoint_t *endpoint = buildEndpoint(thread, DXFG_ENDPOINT_ROLE_PUBLISHER);
    const auto address = "tape:" + path + "[format=text]";

    REQUIRE(dxfg_DXEndpoint_connect(thread, endpoint, address.c_str()) == 0);

    dxfg_publisher_t *publisher = dxfg_DXEndpoint_getPublisher(thread, endpoint);
    dxfg_event_type_list list{static_cast<int32_t>(events.size()), events.data()};

    REQUIRE(publisher != nullptr);
    REQUIRE(dxfg_DXPublisher_publishEvents(thread, publisher, &list) == 0);
    REQUIRE(dxfg_DXEndpoint_awaitProcessed(thread, endpoint) == 0);
    REQUIRE(dxfg_DXEndpoint_closeAndAwaitTermination(thread, endpoint) == 0);

    dxfg_JavaObjectHandler_release(thread, handler(publisher));
    dxfg_JavaObjectHandler_release(thread, handler(endpoint));
}

// Returns the last column (the attachment) of the data lines of the tape by the key "<record>:<symbol>".
std::map<std::string, std::string> tapeAttachments(const std::string &path) {
    std::ifstream file(path);
    std::map<std::string, std::string> result{};
    std::string line{};

    REQUIRE(file.good());

    while (std::getline(file, line)) {
        if (line.empty() || line[0] == '=') {
            continue;
        }

        std::vector<std::string> columns{};
        std::stringstream stream(line);
        std::string column{};

        while (std::getline(stream, column, '\t')) {
            columns.push_back(column);
        }

        if (columns.size() >= 3) {
            result[columns[0] + ":" + columns[1]] = columns.back();
        }
    }

    return result;
}

// The events that the caller allocates itself: the SDK only reads them.
struct CallerEvents {
    dxfg_message_t messageString{{DXFG_EVENT_MESSAGE}, "MSG-STRING", 0, const_cast<char *>("attachment")};
    dxfg_message_t messageNull{{DXFG_EVENT_MESSAGE}, "MSG-NULL", 0, nullptr};
    dxfg_configuration_t configurationString{{DXFG_EVENT_CONFIGURATION}, "CFG-STRING", 0, 1,
                                             const_cast<char *>("attachment")};
    dxfg_configuration_t configurationNull{{DXFG_EVENT_CONFIGURATION}, "CFG-NULL", 0, 2, nullptr};

    std::vector<dxfg_event_type_t *> list() {
        return {&messageString.event_type, &messageNull.event_type, &configurationString.event_type,
                &configurationNull.event_type};
    }
};

} // namespace

TEST_CASE("The attachments written by Java are read in C as strings", "[EventAttachment]") {
    REQUIRE(isolateThread() != nullptr);

    const auto path = tempPath("java");

    writeFile(path, JAVA_TAPE);

    const auto events = readTape(path);

    std::remove(path.c_str());
    REQUIRE(events.size() == 6);

    // A String attachment is the same string.
    CHECK(events.at("Message:MSG-STRING").attachment == "attachment");
    CHECK(events.at("Configuration:CFG-STRING").attachment == "attachment");

    // No attachment is NULL (for CFG-NULL: the attachment that cannot be deserialized), the other events are delivered.
    CHECK(events.at("Message:MSG-NULL").attachment == std::nullopt);
    CHECK(events.at("Configuration:CFG-NULL").attachment == std::nullopt);

    // A non-string attachment is its string representation.
    CHECK(events.at("Message:MSG-INTEGER").attachment == "42");
    CHECK(events.at("Configuration:CFG-INTEGER").attachment == "42");

    // The Configuration version is kept.
    CHECK(events.at("Configuration:CFG-STRING").version == 1);
    CHECK(events.at("Configuration:CFG-NULL").version == 2);
    CHECK(events.at("Configuration:CFG-INTEGER").version == 3);
}

TEST_CASE("The attachments published from C are written as Java writes them", "[EventAttachment]") {
    REQUIRE(isolateThread() != nullptr);

    const auto path = tempPath("c");
    CallerEvents events{};

    writeTape(path, events.list());

    const auto attachments = tapeAttachments(path);

    std::remove(path.c_str());
    REQUIRE(attachments.size() == 4);

    // A string is a Java String.
    CHECK(attachments.at("Message:MSG-STRING") == JAVA_STRING_ATTACHMENT);
    CHECK(attachments.at("Configuration:CFG-STRING") == JAVA_STRING_ATTACHMENT);

    // NULL is no attachment (not an empty one: Java cannot read "null " of the text tapes back).
    CHECK(attachments.at("Message:MSG-NULL") == JAVA_NO_ATTACHMENT);
    CHECK(attachments.at("Configuration:CFG-NULL") == JAVA_NO_ATTACHMENT);

    // The caller's events are unchanged.
    CHECK(std::string(static_cast<const char *>(events.messageString.attachment)) == "attachment");
    CHECK(events.messageNull.attachment == nullptr);
    CHECK(std::string(static_cast<const char *>(events.configurationString.attachment)) == "attachment");
    CHECK(events.configurationNull.attachment == nullptr);
}

TEST_CASE("The attachments are unchanged after a round trip C -> Java -> C", "[EventAttachment]") {
    REQUIRE(isolateThread() != nullptr);

    const auto path = tempPath("round-trip");
    CallerEvents published{};

    writeTape(path, published.list());

    const auto events = readTape(path);

    std::remove(path.c_str());
    REQUIRE(events.size() == 4);
    CHECK(events.at("Message:MSG-STRING").attachment == "attachment");
    CHECK(events.at("Message:MSG-NULL").attachment == std::nullopt);
    CHECK(events.at("Configuration:CFG-STRING").attachment == "attachment");
    CHECK(events.at("Configuration:CFG-STRING").version == 1);
    CHECK(events.at("Configuration:CFG-NULL").attachment == std::nullopt);
    CHECK(events.at("Configuration:CFG-NULL").version == 2);
}

TEST_CASE("The attachment of an event created by the SDK is freed by the SDK", "[EventAttachment]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    for (const auto clazz : {DXFG_EVENT_MESSAGE, DXFG_EVENT_CONFIGURATION}) {
        dxfg_event_type_t *event = dxfg_EventType_new(thread, "SYMBOL", clazz);

        REQUIRE(event != nullptr);

        void *attachment = clazz == DXFG_EVENT_MESSAGE ? reinterpret_cast<dxfg_message_t *>(event)->attachment
                                                       : reinterpret_cast<dxfg_configuration_t *>(event)->attachment;

        CHECK(attachment == nullptr);
        CHECK(dxfg_EventType_release(thread, event) == 0);
    }
}

} // namespace test
} // namespace dxfg
