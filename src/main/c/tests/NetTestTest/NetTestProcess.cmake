# Copyright (c) 2026 Devexperts LLC.
# SPDX-License-Identifier: MPL-2.0

# The process smoke test of NetTest: runs the built tool and checks its exit code and its output.
#   NETTEST            the NetTest executable (the SDK library lies next to it)
#   STATISTICS_HEADER  Statistics.hpp, the one source of the line expressions (SUMMARY_LINE_REGEX, RUNTIME_LINE_REGEX)
#
# 1. A run of 3 seconds against a client address without a listener (no port to pick: connections stay 0): the exit code
#    is 0 and the output has summary lines with published records and runtime lines with the heap of the isolate.
# 2. A run with an unknown flag: the exit code is not 0 and stderr names the flag.
# 3. A run with the GC options of the isolate: the exit code is 0, and stderr has a GC line and, after the tear-down of
#    the isolate, the GC summary.
# 4. A run with an option that the isolate rejects: the exit code is 2 and stderr names the option.
# 5. --help: the exit code is 0 and stdout has the version of the specification.
# 6. `c` against `p` in two processes over loopback: both exit with 0, the consumer prints its statistics with records
#    and its two connections.
# 7. `c` for TimeAndSale against `p` of Trade, with the watchdog shortened through DXFG_NETTEST_WATCHDOG: the connection
#    is open, no record comes, the consumer exits with 1 and the status no_data.
# 8. `c` against `p` with -Ddxscheme.nanoTime=false: the events lack the nanosecond part, the consumer exits with 1 and
#    the status nano_missing; with -Ddxscheme.nanoTime=false on `c` as well it runs to the end and exits with 0.

# The producer of a pair (see run_pair), when the script runs itself with PRODUCER_ARGS: runs `p` with its stdout
# discarded, so that `p` never writes into the pipe to `c`, which may exit first (SIGPIPE would end `p` on Linux and
# macOS). It exits with 0 when `p` does.
if (DEFINED PRODUCER_ARGS)
    execute_process(COMMAND "${NETTEST}" p ${PRODUCER_ARGS} RESULT_VARIABLE producer_result OUTPUT_QUIET TIMEOUT 40)

    if (NOT "${producer_result}" STREQUAL "0")
        message(FATAL_ERROR "p exited with ${producer_result}")
    endif ()

    return()
endif ()

# The expression of the summary line: two adjacent string literals of SUMMARY_LINE_REGEX, joined here as C++ joins them,
# bracket expressions only, so ECMAScript of the header and the regular expressions of CMake read it the same way.
file(READ "${STATISTICS_HEADER}" header)

if (NOT header MATCHES "SUMMARY_LINE_REGEX =[ \r\n]*\"([^\"]+)\"[ \r\n]*\"([^\"]+)\"")
    message(FATAL_ERROR "SUMMARY_LINE_REGEX is not found in ${STATISTICS_HEADER}")
endif ()

set(summary_regex "${CMAKE_MATCH_1}${CMAKE_MATCH_2}")

message(STATUS "The summary line expression: ${summary_regex}")

# The expression of the runtime line: two adjacent string literals in the header, joined here as C++ joins them.
if (NOT header MATCHES "RUNTIME_LINE_REGEX =[ \r\n]*\"([^\"]+)\"[ \r\n]*\"([^\"]+)\"")
    message(FATAL_ERROR "RUNTIME_LINE_REGEX is not found in ${STATISTICS_HEADER}")
endif ()

set(runtime_regex "${CMAKE_MATCH_1}${CMAKE_MATCH_2}")

message(STATUS "The runtime line expression: ${runtime_regex}")

# 1. The run.
execute_process(COMMAND "${NETTEST}" p 127.0.0.1:1 -S 100 -r */5k -s 1s --duration 3s
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 40)
message("exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if (NOT "${code}" STREQUAL "0")
    message(FATAL_ERROR "NetTest exited with ${code}, expected 0")
endif ()

if (NOT output MATCHES "${summary_regex}")
    message(FATAL_ERROR "The output has no line that matches: ${summary_regex}")
endif ()

if (NOT output MATCHES "instances 1, connections 0")
    message(FATAL_ERROR "The statistics do not show one instance and no connection")
endif ()

# At */5k for 3 seconds a working tool publishes in every full period.
if (NOT output MATCHES "data [1-9][0-9,]* rps")
    message(FATAL_ERROR "The statistics show no published records")
endif ()

if (NOT output MATCHES "${runtime_regex}")
    message(FATAL_ERROR "The output has no line that matches: ${runtime_regex}")
endif ()

# The heap is given on every GC; the GC values may be n/a (G1).
if (NOT output MATCHES "NetTestRuntime-NetTest[*][}] heap [0-9,]+ MB of [0-9,]+ MB")
    message(FATAL_ERROR "The runtime line shows no heap values")
endif ()

# 2. The unknown flag.
execute_process(COMMAND "${NETTEST}" p 127.0.0.1:1 --no-such-flag
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 40)
message("exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if ("${code}" STREQUAL "0")
    message(FATAL_ERROR "NetTest exited with 0 on an unknown flag")
endif ()

if (NOT errors MATCHES "--no-such-flag")
    message(FATAL_ERROR "stderr does not name the flag --no-such-flag")
endif ()

# 3. The GC options. A young generation of 2 MB fills several times in 3 seconds of copied batches.
execute_process(COMMAND "${NETTEST}" p 127.0.0.1:1 -S 100 -r */5k -s 1s --duration 3s
        -Xmx64m -Xmn2m -XX:+PrintGC -XX:+PrintGCSummary
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 40)
message("exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if (NOT "${code}" STREQUAL "0")
    message(FATAL_ERROR "NetTest with the GC options exited with ${code}, expected 0")
endif ()

if (NOT errors MATCHES "GC\\([0-9]+\\) Pause")
    message(FATAL_ERROR "stderr has no GC line of -XX:+PrintGC")
endif ()

if (NOT errors MATCHES "GC summary")
    message(FATAL_ERROR "stderr has no GC summary of -XX:+PrintGCSummary: the isolate was not torn down")
endif ()

# 4. The rejected option. The tool's own line names the isolate options on every platform; the reason that GraalVM
# prints ("Error: ...") comes above it.
execute_process(COMMAND "${NETTEST}" p 127.0.0.1:1 -S 100 --duration 1s -Xmx64m -XX:+NetTestNoSuchOption
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 40)
message("exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if (NOT "${code}" STREQUAL "2")
    message(FATAL_ERROR "NetTest exited with ${code} on an option that the isolate rejects, expected 2")
endif ()

if (NOT output STREQUAL "")
    message(FATAL_ERROR "NetTest printed to stdout on an option that the isolate rejects")
endif ()

# The last line of stderr, byte for byte: the platform newline after it and nothing else.
if (WIN32)
    set(newline "\r?\n")
else ()
    set(newline "\n")
endif ()

if (NOT errors MATCHES "(^|\n)NetTest: the isolate rejected one of the options: -Xmx64m -XX:\\+NetTestNoSuchOption${newline}$")
    message(FATAL_ERROR "stderr does not end with the rejection line naming -Xmx64m -XX:+NetTestNoSuchOption")
endif ()

if (NOT errors MATCHES "Error: [^\n]*NetTestNoSuchOption")
    message(FATAL_ERROR "stderr has no reason of the isolate naming NetTestNoSuchOption")
endif ()

# 5. --help.
execute_process(COMMAND "${NETTEST}" --help
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 40)
message("exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if (NOT "${code}" STREQUAL "0")
    message(FATAL_ERROR "NetTest --help exited with ${code}, expected 0")
endif ()

if (NOT output MATCHES "(^|\n)spec_version nettest/1\r?\n")
    message(FATAL_ERROR "NetTest --help does not print the line spec_version nettest/1")
endif ()

# The pairs of processes: execute_process() starts the commands of one call together as a pipeline, and the output is
# the stdout of `c`. `p` runs under a nested `cmake -P` of this script that discards its stdout, so the pipe to the
# stdin of `c` stays empty and `c` may exit first. A random port above the ephemeral ranges of Linux and Windows; the consumer reconnects
# every 200 ms (a connector property in the address) until `p` listens.
string(RANDOM LENGTH 4 ALPHABET "0123456789" random)
math(EXPR port "20000 + ${random}")
set(consumer_address "127.0.0.1:${port}[reconnectDelay=200]")

# Runs `p` with the producer arguments and `c` with the consumer arguments (the latter after `cmake -E env` when the
# environment is given) and sets producer_code, consumer_code, output and errors in the caller.
function(run_pair producer_args consumer_args environment)
    set(consumer_command "${NETTEST}" c ${consumer_args})

    if (NOT "${environment}" STREQUAL "")
        set(consumer_command "${CMAKE_COMMAND}" -E env "${environment}" ${consumer_command})
    endif ()

    execute_process(COMMAND "${CMAKE_COMMAND}" "-DNETTEST=${NETTEST}" "-DPRODUCER_ARGS=${producer_args}"
            -P "${CMAKE_CURRENT_LIST_FILE}"
            COMMAND ${consumer_command}
            RESULTS_VARIABLE codes
            OUTPUT_VARIABLE pair_output
            ERROR_VARIABLE pair_errors
            TIMEOUT 40)
    message("exit codes: ${codes}\nstdout of c:\n${pair_output}\nstderr of both:\n${pair_errors}")
    list(GET codes 0 producer)
    list(GET codes 1 consumer)
    set(producer_code "${producer}" PARENT_SCOPE)
    set(consumer_code "${consumer}" PARENT_SCOPE)
    set(output "${pair_output}" PARENT_SCOPE)
    set(errors "${pair_errors}" PARENT_SCOPE)
endfunction()

# 6. `c` against `p`.
run_pair(":${port};-S;100;-r;*/2k;-s;1s;--duration;7s"
        "${consumer_address};-S;100;-C;2;-s;1s;--duration;5s" "")

if (NOT "${producer_code}" STREQUAL "0" OR NOT "${consumer_code}" STREQUAL "0")
    message(FATAL_ERROR "p exited with ${producer_code}, c with ${consumer_code}, expected 0 and 0")
endif ()

if (NOT output MATCHES "${summary_regex}")
    message(FATAL_ERROR "The output of c has no line that matches: ${summary_regex}")
endif ()

if (NOT output MATCHES "NetTestConsumerSide-NetTest[*][}] data [1-9][0-9,]* rps, lag -?[0-9,]+ us, instances 2, connections 2")
    message(FATAL_ERROR "The statistics of c show no block with received records over two connections")
endif ()

if (NOT output MATCHES "${runtime_regex}")
    message(FATAL_ERROR "The output of c has no line that matches: ${runtime_regex}")
endif ()

# 7. The watchdog: the connection is open and no record comes.
math(EXPR port "${port} + 1")
set(consumer_address "127.0.0.1:${port}[reconnectDelay=200]")
run_pair(":${port};-S;100;-r;*/2k;-s;1s;--duration;6s"
        "${consumer_address};-S;100;--event;TimeAndSale;-s;1s;--duration;30s" "DXFG_NETTEST_WATCHDOG=2000,2000")

if (NOT "${producer_code}" STREQUAL "0" OR NOT "${consumer_code}" STREQUAL "1")
    message(FATAL_ERROR "p exited with ${producer_code}, c with ${consumer_code}, expected 0 and 1")
endif ()

if (NOT errors MATCHES "NetTest: no_data: no records for 2000 ms after the connection")
    message(FATAL_ERROR "stderr has no no_data line of the watchdog")
endif ()

# 8. The nanosecond check.
math(EXPR port "${port} + 1")
set(consumer_address "127.0.0.1:${port}[reconnectDelay=200]")
run_pair(":${port};-S;100;-r;*/2k;-s;1s;--duration;6s;-Ddxscheme.nanoTime=false"
        "${consumer_address};-S;100;-s;1s;--duration;30s" "")

if (NOT "${producer_code}" STREQUAL "0" OR NOT "${consumer_code}" STREQUAL "1")
    message(FATAL_ERROR "p exited with ${producer_code}, c with ${consumer_code}, expected 0 and 1")
endif ()

if (NOT errors MATCHES "NetTest: nano_missing: the first 1000 events have the nanosecond part 0")
    message(FATAL_ERROR "stderr has no nano_missing line of the nanosecond check")
endif ()

math(EXPR port "${port} + 1")
set(consumer_address "127.0.0.1:${port}[reconnectDelay=200]")
run_pair(":${port};-S;100;-r;*/2k;-s;1s;--duration;6s;-Ddxscheme.nanoTime=false"
        "${consumer_address};-S;100;-s;1s;--duration;4s;-Ddxscheme.nanoTime=false" "")

if (NOT "${producer_code}" STREQUAL "0" OR NOT "${consumer_code}" STREQUAL "0")
    message(FATAL_ERROR "p exited with ${producer_code}, c with ${consumer_code}, expected 0 and 0")
endif ()

if (NOT output MATCHES "NetTestConsumerSide-NetTest[*][}] data [1-9][0-9,]* rps")
    message(FATAL_ERROR "The statistics of c with -Ddxscheme.nanoTime=false show no received records")
endif ()