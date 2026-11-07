# Copyright (c) 2026 Devexperts LLC.
# SPDX-License-Identifier: MPL-2.0

#
# The conformance kit (src/main/kit): the runner self-tests with a mock adapter.
#

message(STATUS "Test KitTest")

set(kit_dir "${CMAKE_CURRENT_SOURCE_DIR}/../../../kit")

# The runner, as a wrapper author builds it: from its own CMakeLists.txt, without the SDK.
add_subdirectory("${kit_dir}/runner" "${CMAKE_CURRENT_BINARY_DIR}/runner")
set_target_properties(kit-runner PROPERTIES FOLDER "${IDE_FOLDER}")

# The reference adapter, on the library of this build (it uses the DxFeedGraalNativeSdk target defined above).
add_subdirectory("${kit_dir}/adapter" "${CMAKE_CURRENT_BINARY_DIR}/adapter")
set_target_properties(kit-reference-adapter PROPERTIES FOLDER "${IDE_FOLDER}")

# The mock adapter: the protocol without the SDK, with misbehaviour on request.
add_executable(KitMockAdapter MockAdapter/main.cpp)
set_target_properties(KitMockAdapter PROPERTIES
        ${DEFAULT_PROJECT_OPTIONS}
        FOLDER "${IDE_FOLDER}"
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF
        )

#
# Runner self-tests
#

set(self "${CMAKE_CURRENT_SOURCE_DIR}/scenarios/self")

# kit_runner_test(<test> ADAPTER <file> EXIT <code> TIMEOUT <s> ARGS <runner args>... [EXPECT <regex>...]
#                 [REJECT <regex>...]): runs the runner over the adapter and checks its exit code and output.
function(kit_runner_test test)
    cmake_parse_arguments(PARSE_ARGV 1 test "" "ADAPTER;EXIT;TIMEOUT" "ARGS;EXPECT;REJECT")
    list(JOIN test_ARGS "|" args)
    list(JOIN test_EXPECT "|" expect)
    list(JOIN test_REJECT "|" reject)
    add_test(NAME ${test}
            COMMAND ${CMAKE_COMMAND}
            "-DRUNNER=$<TARGET_FILE:kit-runner>"
            "-DARGS=--adapter|${test_ADAPTER}|${args}"
            "-DEXPECT_EXIT=${test_EXIT}"
            "-DEXPECT=${expect}"
            "-DREJECT=${reject}"
            -P "${CMAKE_CURRENT_SOURCE_DIR}/RunnerSelfTest.cmake")
    set_tests_properties(${test} PROPERTIES TIMEOUT ${test_TIMEOUT})
endfunction()

# kit_self_test(<name> EXIT <code> ARGS <runner args>... [EXPECT <regex>...] [REJECT <regex>...]): over the mock.
function(kit_self_test name)
    kit_runner_test(KitTest.Self.${name} ADAPTER $<TARGET_FILE:KitMockAdapter> TIMEOUT 60 ${ARGN})
endfunction()

kit_self_test(Pass EXIT 0 ARGS --strict ${self}/pass
        EXPECT "PASS self-basic" "PASS self-until" "PASS self-values" "summary passed=6 failed=0 skipped=0 errors=0")

kit_self_test(OwnProcess EXIT 0 ARGS --strict ${self}/own-process
        EXPECT "PASS self-own-teardown" "PASS self-own-eof" "summary passed=2 failed=0")

# AE1: a skip that strict mode does not allow fails; an allowed one passes.
kit_self_test(StrictSkip EXIT 1
        ARGS --strict --adapter-arg --absent --adapter-arg event_queue_ready_signal ${self}/pass
        EXPECT "FAIL self-feature" "strict mode does not allow it" "summary passed=5 failed=1")
kit_self_test(AllowedSkip EXIT 0
        ARGS --strict --allow-skip feature-absent --adapter-arg --absent --adapter-arg event_queue_ready_signal
        ${self}/pass
        EXPECT "SKIP self-feature [(]feature-absent[)]" "skips=feature-absent:1")
kit_self_test(CapabilitySkip EXIT 0 ARGS --adapter-arg --caps --adapter-arg async_poll ${self}/pass
        EXPECT "SKIP self-capability [(]capability-absent[)]")

# AE2 with the mock: a revision mismatch skips the scenarios that use experimental functions.
kit_self_test(RevisionMismatch EXIT 0
        ARGS --strict --allow-skip advanced-disabled --expected-advanced-revision 999 ${self}/pass
        EXPECT "SKIP self-experimental [(]advanced-disabled[)]" "summary passed=5 failed=0 skipped=1")

# AE3: a hung adapter is killed after the step deadline, and the next scenario runs in a fresh one.
kit_self_test(Hang EXIT 2 ARGS --step-timeout 2000 ${self}/hang
        EXPECT "PASS self-hang-a" "ERROR self-hang-b" "no response before the deadline" "PASS self-hang-c"
        "summary passed=2 failed=0 skipped=0 errors=1")

kit_self_test(ExitBeforeHello EXIT 2 ARGS --adapter-arg --exit-before-hello --adapter-arg 5 ${self}/hang
        EXPECT "exited with the code 5" "errors=3" REJECT "PASS")
kit_self_test(ExitInScenario EXIT 2 ARGS ${self}/exit EXPECT "ERROR self-exit" "exited with the code 5")
kit_self_test(Garbage EXIT 2 ARGS ${self}/garbage EXPECT "ERROR self-garbage" "not a protocol response")
kit_self_test(Stderr EXIT 1 ARGS ${self}/stderr
        EXPECT "FAIL self-stderr" "expected 'ok', got 'error boom'" "MOCK-STDERR-END")
kit_self_test(ValueMismatch EXIT 1 ARGS ${self}/mismatch EXPECT "bid_price: expected 0, got -0")

# Every check can fail: each scenario of checks/ and dropped/ breaks one check, so none of them passes.
kit_self_test(ChecksFail EXIT 1 ARGS --step-timeout 1000 ${self}/checks
        EXPECT "count self-fail-count.A: expected = 2, got 1" "last self-fail-last.A: price: expected 9, got 1"
        "increasing self-fail-increasing.A: event 2 has price=1 after 2"
        "accounted self-fail-accounted.A: received 1 [+] dropped 0 != 2"
        "the sentinel self-fail-sentinel.END did not arrive before the deadline"
        "2 event[(]s[)] did not arrive before the deadline" "expected the outcome 'no_events'"
        "FAIL self-fail-poll-count" "summary passed=0 failed=8 skipped=0 errors=0")
kit_self_test(DroppedFail EXIT 1 ARGS --adapter-arg --dropped --adapter-arg 5 ${self}/dropped
        EXPECT "dropped: expected = 0, got 5" "monotonic dropped: poll 2 reported 0 after 5"
        "summary passed=0 failed=2")

# A strict run in which every scenario was skipped, even for an allowed reason, checked nothing and fails.
kit_self_test(AllSkipped EXIT 1
        ARGS --strict --allow-skip feature-absent --adapter-arg --absent --adapter-arg event_queue_ready_signal
        --only self-feature ${self}/pass
        EXPECT "SKIP self-feature [(]feature-absent[)]" "FAIL no scenario passed, and strict mode needs at least one")
kit_self_test(ProtocolMismatch EXIT 3 ARGS --adapter-arg --proto --adapter-arg 2 ${self}/pass
        EXPECT "speaks the protocol 2" REJECT "PASS")
kit_self_test(NoScenario EXIT 1 ARGS --only no-such-scenario ${self}/pass EXPECT "no scenario to run")
kit_self_test(BadScenario EXIT 64 ARGS ${self}/bad EXPECT "allowed only in a scenario of its own process")

# The runner and the mock adapter are found next to the other test executables.
add_dependencies(KitMockAdapter kit-runner)

#
# The kit over the reference adapter: strict, with no skip allowed. The runner's total deadline stays below the
# TIMEOUT of each test.
#

set(kit_scenarios "${kit_dir}/scenarios")
set(kit_adapter "$<TARGET_FILE:kit-reference-adapter>")

# The scenarios of one shared adapter process.
add_test(NAME KitTest.Shared COMMAND kit-runner --adapter ${kit_adapter} --strict "${kit_scenarios}/shared")
set_tests_properties(KitTest.Shared PROPERTIES TIMEOUT 120)

# A test per scenario of its own process; a new file is a new test.
file(GLOB kit_own_process CONFIGURE_DEPENDS "${kit_scenarios}/own-process/*.scenario")

foreach (scenario IN LISTS kit_own_process)
    get_filename_component(name "${scenario}" NAME_WE)
    add_test(NAME KitTest.${name}
            COMMAND kit-runner --adapter ${kit_adapter} --strict --total-timeout 25000 "${scenario}")
    set_tests_properties(KitTest.${name} PROPERTIES TIMEOUT 30)
endforeach ()

# AE2: a wrapper built for another advanced revision binds no experimental function; the scenarios that use one are
# skipped, the others pass.
kit_runner_test(KitTest.RevisionMismatch ADAPTER ${kit_adapter} EXIT 0 TIMEOUT 120
        ARGS --strict --allow-skip advanced-disabled --expected-advanced-revision 999 "${kit_scenarios}/shared"
        EXPECT "SKIP kit-ready-signal [(]advanced-disabled[)]" "failed=0 skipped=1 errors=0")

# A feature the SDK does not know is reported absent (not strict: a strict run with no passed scenario fails).
kit_runner_test(KitTest.UnknownFeature ADAPTER ${kit_adapter} EXIT 0 TIMEOUT 30
        ARGS "${CMAKE_CURRENT_SOURCE_DIR}/scenarios/unknown-feature"
        EXPECT "SKIP kit-test-unknown-feature [(]feature-absent[)]")
