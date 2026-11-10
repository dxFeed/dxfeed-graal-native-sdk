// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The properties file of dxfg_v4_DXEndpoint_Builder_withProperties() (and of the deprecated
// dxfg_DXEndpoint_Builder_withProperties()) is closed when the call returns, also when it fails (MDAPI-514). Before
// 3.16.0 the file stayed open until the garbage collection: on Windows it could not be deleted meanwhile.

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <atomic>
#include <filesystem>
#include <fstream>
#include <random>
#include <string>
#include <system_error>

namespace dxfg {
namespace test {

namespace {

graal_isolatethread_t *isolateThread() {
    static const Isolate isolate{};

    return isolate.thread;
}

// A properties file with the given content in the temporary directory, deleted by the destructor if still there.
struct PropertiesFile {
    std::filesystem::path path;

    explicit PropertiesFile(const std::string &content) {
        // A random part keeps the runs that share the temporary directory, as on a CI agent, apart.
        static const auto run = std::random_device{}();
        static std::atomic<int> counter{0};

        path = std::filesystem::temp_directory_path() / ("dxfg-builder-properties-" + std::to_string(run) + "-" +
                                                         std::to_string(++counter) + ".properties");

        std::ofstream stream(path, std::ios::binary | std::ios::trunc);

        stream << content;
    }

    ~PropertiesFile() {
        std::error_code ignored;

        std::filesystem::remove(path, ignored);
    }

    // The path as a UTF-8 string for the SDK.
    std::string utf8() const {
        const auto text = path.u8string();

        return {text.begin(), text.end()};
    }

    // Deletes the file; on Windows it fails while the file is open.
    bool remove() const {
        std::error_code error;

        return std::filesystem::remove(path, error) && !error && !std::filesystem::exists(path);
    }
};

} // namespace

TEST_CASE("dxfg_v4_DXEndpoint_Builder_withProperties closes the file before it returns") {
    const auto thread = isolateThread();
    REQUIRE(thread != nullptr);

    const PropertiesFile file("dxfeed.aggregationPeriod=1s\n");
    dxfg_v4_endpoint_builder_t *builder = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_newBuilder(thread, &builder) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_Builder_withProperties(thread, builder, file.utf8().c_str()) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());
    CHECK(file.remove());

    dxfg_v4_JavaObjectHandler_release(thread, &builder->handler);
}

TEST_CASE("dxfg_v4_DXEndpoint_Builder_withProperties closes the file when the file is malformed") {
    const auto thread = isolateThread();
    REQUIRE(thread != nullptr);

    // A broken Unicode escape: java.util.Properties.load() throws IllegalArgumentException.
    const PropertiesFile file("dxfeed.aggregationPeriod=\\uZZZZ\n");
    dxfg_v4_endpoint_builder_t *builder = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_newBuilder(thread, &builder) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_Builder_withProperties(thread, builder, file.utf8().c_str()) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(file.remove());

    dxfg_v4_JavaObjectHandler_release(thread, &builder->handler);
}

TEST_CASE("dxfg_v4_DXEndpoint_Builder_withProperties fails for a missing file") {
    const auto thread = isolateThread();
    REQUIRE(thread != nullptr);

    const PropertiesFile file("");
    REQUIRE(file.remove());

    dxfg_v4_endpoint_builder_t *builder = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_newBuilder(thread, &builder) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_Builder_withProperties(thread, builder, file.utf8().c_str()) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.io.FileNotFoundException");

    dxfg_v4_JavaObjectHandler_release(thread, &builder->handler);
}

TEST_CASE("The deprecated dxfg_DXEndpoint_Builder_withProperties closes the file before it returns") {
    const auto thread = isolateThread();
    REQUIRE(thread != nullptr);

    const PropertiesFile file("dxfeed.aggregationPeriod=1s\n");
    dxfg_endpoint_builder_t *builder = dxfg_DXEndpoint_newBuilder(thread);

    REQUIRE(builder != nullptr);
    CHECK(dxfg_DXEndpoint_Builder_withProperties(thread, builder, file.utf8().c_str()) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(file.remove());

    dxfg_JavaObjectHandler_release(thread, &builder->handler);
}

} // namespace test
} // namespace dxfg
