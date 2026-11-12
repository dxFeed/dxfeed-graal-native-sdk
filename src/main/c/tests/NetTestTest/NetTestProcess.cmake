# Copyright (c) 2026 Devexperts LLC.
# SPDX-License-Identifier: MPL-2.0

# The process smoke test of NetTest: runs the built tool and checks its exit code and its output.
#   NETTEST            the NetTest executable (the SDK library lies next to it)
#   STATISTICS_HEADER  Statistics.hpp, the one source of the summary line expression (SUMMARY_LINE_REGEX)
#
# 1. A run of 3 seconds against a client address without a listener (no port to pick: connections stay 0): the exit code
#    is 0 and the output has summary lines with published records.
# 2. A run with an unknown flag: the exit code is not 0 and stderr names the flag.
# 3. A run with the GC options of the isolate: the exit code is 0, and stderr has a GC line and, after the tear-down of
#    the isolate, the GC summary.
# 4. A run with an option that the isolate rejects: the exit code is 2 and stderr names the option.

# The expression of the summary line: the string literal of SUMMARY_LINE_REGEX, bracket expressions only, so ECMAScript
# of the header and the regular expressions of CMake read it the same way.
file(READ "${STATISTICS_HEADER}" header)

if (NOT header MATCHES "SUMMARY_LINE_REGEX =[ \r\n]*\"([^\"]+)\"")
    message(FATAL_ERROR "SUMMARY_LINE_REGEX is not found in ${STATISTICS_HEADER}")
endif ()

set(summary_regex "${CMAKE_MATCH_1}")

message(STATUS "The summary line expression: ${summary_regex}")

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

# 4. The rejected option.
execute_process(COMMAND "${NETTEST}" p 127.0.0.1:1 -S 100 --duration 1s -XX:+NetTestNoSuchOption
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 40)
message("exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if (NOT "${code}" STREQUAL "2")
    message(FATAL_ERROR "NetTest exited with ${code} on an option that the isolate rejects, expected 2")
endif ()

if (NOT errors MATCHES "NetTestNoSuchOption")
    message(FATAL_ERROR "stderr does not name the option NetTestNoSuchOption")
endif ()
