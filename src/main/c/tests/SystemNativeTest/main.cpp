#include <catch.hpp>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <string>

// ToDo Fix java daemon thread, appears in
// https://github.com/stdcion/dxfeed-graal-native-api/commit/525e2eb5191bd196d08f725baf866f739f2a12b4#diff-390547990893e97ec6012ce95f1b5d4447f855fdbe11c95e183e9386f94f9f59R115
// ToDo Issue https://github.com/oracle/graal/issues/4290 https://github.com/oracle/graal/issues/4024
// https://github.com/oracle/graal/commit/52b709f17b7384f0997fd76476437d9106652de4

namespace dxfg {
namespace test {

TEST_CASE("Check reads unset property", "[SystemNative]") {
    // Setup graal.
    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;
    auto graal_result = graal_create_isolate(nullptr, &isolate, &thread);
    REQUIRE(graal_result == 0);

    // On getting an unset property, returns nullptr.
    auto prop = dxfg_system_get_property(thread, "first_prop");
    REQUIRE(prop == nullptr);
    // Frees nullptr does not result in an error.
    dxfg_system_release_property(thread, prop);

    // Tear down graal.
    // graal_result = graal_detach_all_threads_and_tear_down_isolate(thread);
    REQUIRE(graal_result == 0);
}

TEST_CASE("Check write/read property", "[SystemNative]") {
    // Setup graal.
    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;
    auto graal_result = graal_create_isolate(nullptr, &isolate, &thread);
    REQUIRE(graal_result == 0);

    size_t count = 100;
    std::string key = "key_";
    std::string val = "val_";

    // Sets properties.
    for (size_t i = 0; i < count; ++i) {
        auto currentKey = std::string(key + std::to_string(i));
        auto currentVal = std::string(val + std::to_string(i));
        auto res = dxfg_system_set_property(thread, currentKey.c_str(), currentVal.c_str());
        REQUIRE(res == 0);
    }

    // Gets properties.
    for (size_t i = 0; i < count; ++i) {
        auto currentKey = std::string(key + std::to_string(i));
        auto expectedVal = std::string(val + std::to_string(i));
        auto currentVal = dxfg_system_get_property(thread, currentKey.c_str());
        REQUIRE(currentVal != nullptr);
        REQUIRE(std::string(currentVal) == expectedVal);
        dxfg_system_release_property(thread, currentVal);
    }

    // Tear down graal.
    // graal_result = graal_detach_all_threads_and_tear_down_isolate(thread);
    REQUIRE(graal_result == 0);
}

TEST_CASE("Check in different isolate", "[SystemNative]") {
    // Setup graal.
    graal_isolate_t *isolateFirst = nullptr;
    graal_isolatethread_t *threadFirst = nullptr;
    graal_isolate_t *isolateSecond = nullptr;
    graal_isolatethread_t *threadSecond = nullptr;
    auto graal_result = graal_create_isolate(nullptr, &isolateFirst, &threadFirst);
    REQUIRE(graal_result == 0);
    graal_result = graal_create_isolate(nullptr, &isolateSecond, &threadSecond);
    REQUIRE(graal_result == 0);

    size_t count = 100;
    std::string key = "key_";
    std::string valFirst = "val_first_";
    std::string valSecond = "val_second_";

    // Sets properties.
    for (size_t i = 0; i < count; ++i) {
        auto currentKey = std::string(key + std::to_string(i));
        auto currentValFirst = std::string(valFirst + std::to_string(i));
        auto currentValSecond = std::string(valSecond + std::to_string(i));
        auto res = dxfg_system_set_property(threadFirst, currentKey.c_str(), currentValFirst.c_str());
        REQUIRE(res == 0);
        res = dxfg_system_set_property(threadSecond, currentKey.c_str(), currentValSecond.c_str());
        REQUIRE(res == 0);
    }

    // Gets properties.
    for (size_t i = 0; i < count; ++i) {
        auto currentKey = std::string(key + std::to_string(i));
        auto expectedValFirst = std::string(valFirst + std::to_string(i));
        auto expectedValSecond = std::string(valSecond + std::to_string(i));

        auto currentVal = dxfg_system_get_property(threadFirst, currentKey.c_str());
        REQUIRE(currentVal != nullptr);
        REQUIRE(std::string(currentVal) == expectedValFirst);
        dxfg_system_release_property(threadFirst, currentVal);

        currentVal = dxfg_system_get_property(threadSecond, currentKey.c_str());
        REQUIRE(currentVal != nullptr);
        REQUIRE(std::string(currentVal) == expectedValSecond);
        dxfg_system_release_property(threadFirst, currentVal);
    }

    // Tear down graal.
    // graal_result = graal_detach_all_threads_and_tear_down_isolate(threadFirst);
    REQUIRE(graal_result == 0);

    // graal_result = graal_detach_all_threads_and_tear_down_isolate(threadSecond);
    REQUIRE(graal_result == 0);
}

TEST_CASE("The version is the one of the build", "[SystemNative][V4]") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    const char *version = nullptr;
    REQUIRE(dxfg_v4_system_get_version(isolate.thread, &version) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(version != nullptr);
    CHECK(std::string(version) == DXFG_TEST_SDK_VERSION);
    REQUIRE(dxfg_v4_String_release(isolate.thread, version) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(dxfg_v4_system_get_version(isolate.thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("The advanced revision is the one of the headers", "[SystemNative][V4]") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    int32_t revision = -1;
    REQUIRE(dxfg_v4_system_get_advanced_revision(isolate.thread, &revision) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(revision == DXFG_V4_ADVANCED_REVISION);

    CHECK(dxfg_v4_system_get_advanced_revision(isolate.thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("The features of the headers are supported, unknown ones are not", "[SystemNative][V4]") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    const auto isSupported = [&isolate](const char *name) {
        int32_t supported = -1;
        REQUIRE(dxfg_v4_system_is_feature_supported(isolate.thread, name, &supported) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE((supported == 0 || supported == 1));

        return supported == 1;
    };

    CHECK(isSupported(DXFG_V4_FEATURE_EVENT_QUEUE));
    CHECK(isSupported(DXFG_V4_FEATURE_SHARED_EVENT_QUEUE));
    CHECK_FALSE(isSupported("no_such_feature"));
    CHECK_FALSE(isSupported(""));
    CHECK(takeExceptionClassName(isolate.thread).empty());

    int32_t supported = -1;
    CHECK(dxfg_v4_system_is_feature_supported(isolate.thread, nullptr, &supported) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");
    CHECK(supported == -1);
    CHECK(dxfg_v4_system_is_feature_supported(isolate.thread, DXFG_V4_FEATURE_EVENT_QUEUE, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");
}

} // namespace test
} // namespace dxfg