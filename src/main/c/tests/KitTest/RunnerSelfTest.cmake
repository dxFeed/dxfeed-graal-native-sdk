# Copyright (c) 2026 Devexperts LLC.
# SPDX-License-Identifier: MPL-2.0

# Runs the kit runner and checks its exit code and output.
#   RUNNER       the runner executable
#   ARGS         its arguments, separated by "|"
#   EXPECT_EXIT  the expected exit code
#   EXPECT       regular expressions that the output must match, separated by "|"
#   REJECT       regular expressions that the output must not match, separated by "|"
string(REPLACE "|" ";" args "${ARGS}")
execute_process(COMMAND "${RUNNER}" ${args}
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE output
        TIMEOUT 100)
message("${output}")

if (NOT "${code}" STREQUAL "${EXPECT_EXIT}")
    message(FATAL_ERROR "The runner exited with ${code}, expected ${EXPECT_EXIT}")
endif ()

if (DEFINED EXPECT AND NOT "${EXPECT}" STREQUAL "")
    string(REPLACE "|" ";" expected "${EXPECT}")

    foreach (pattern IN LISTS expected)
        if (NOT output MATCHES "${pattern}")
            message(FATAL_ERROR "The output does not match: ${pattern}")
        endif ()
    endforeach ()
endif ()

if (DEFINED REJECT AND NOT "${REJECT}" STREQUAL "")
    string(REPLACE "|" ";" rejected "${REJECT}")

    foreach (pattern IN LISTS rejected)
        if (output MATCHES "${pattern}")
            message(FATAL_ERROR "The output matches: ${pattern}")
        endif ()
    endforeach ()
endif ()
