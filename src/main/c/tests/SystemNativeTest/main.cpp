#include <catch.hpp>
#include <dxfg_api.h>
#include <thread>

// ToDo Fix java daemon thread, appears in https://github.com/stdcion/dxfeed-graal-native-api/commit/525e2eb5191bd196d08f725baf866f739f2a12b4#diff-390547990893e97ec6012ce95f1b5d4447f855fdbe11c95e183e9386f94f9f59R115
// ToDo Issue https://github.com/oracle/graal/issues/4290 https://github.com/oracle/graal/issues/4024 https://github.com/oracle/graal/commit/52b709f17b7384f0997fd76476437d9106652de4

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
    //graal_result = graal_detach_all_threads_and_tear_down_isolate(thread);
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
    //graal_result = graal_detach_all_threads_and_tear_down_isolate(thread);
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
    //graal_result = graal_detach_all_threads_and_tear_down_isolate(threadFirst);
    REQUIRE(graal_result == 0);

    //graal_result = graal_detach_all_threads_and_tear_down_isolate(threadSecond);
    REQUIRE(graal_result == 0);
}

} // namespace test
} // namespace dxfg