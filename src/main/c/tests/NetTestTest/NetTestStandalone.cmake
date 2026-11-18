# Copyright (c) 2026 Devexperts LLC.
# SPDX-License-Identifier: MPL-2.0

# NetTest built against the SDK archive that the build of the repository has made (mvn package puts it into target/),
# as a user builds it outside the repository: the archive is unpacked and src/apps/NetTest is the top-level project.
#   ARCHIVE_GLOB       the archives of the platform: target/graal-native-sdk-<arch>.zip, -<arch>-<variant>.zip of a
#                      build variant; the newest one is taken, -static-lib.zip never
#   SOURCE             src/apps/NetTest
#   WORK               an empty directory of the test (removed and created anew)
#   CONFIG             the configuration to build (the configuration of the test)
#   GENERATOR          the generator of the build in the tree, and its PLATFORM, TOOLSET and INSTANCE (each may be absent)
#   C_COMPILER         the compilers of the build in the tree (not passed to a Visual Studio generator)
#   CXX_COMPILER
#   OSX_ARCHITECTURES  CMAKE_OSX_ARCHITECTURES of the build in the tree (may be absent)
#   STATISTICS_HEADER  Statistics.hpp, the one source of SUMMARY_LINE_REGEX
#
# 1. Configure and build: both exit with 0.
# 2. The built tool runs 2 seconds against a client address without a listener: the exit code is 0 and the output has
#    a summary line. The SDK library is found only next to the tool (the post-build copy and the rpath $ORIGIN or
#    @loader_path): the unpacked archive is neither on PATH nor in the library path.

file(READ "${STATISTICS_HEADER}" header)

if (NOT header MATCHES "SUMMARY_LINE_REGEX =[ \r\n]*\"([^\"]+)\"")
    message(FATAL_ERROR "SUMMARY_LINE_REGEX is not found in ${STATISTICS_HEADER}")
endif ()

set(summary_regex "${CMAKE_MATCH_1}")

# The archive. mvn clean package leaves one; a local target/ may keep the archives of earlier variant builds. The
# static library archive of the Windows static runtime (-static-lib.zip) has no shared library to build against.
file(GLOB archives "${ARCHIVE_GLOB}")
list(FILTER archives EXCLUDE REGEX "-static-lib\\.zip$")

if (NOT archives)
    message(FATAL_ERROR "No SDK archive matches ${ARCHIVE_GLOB}: run mvn package before the tests")
endif ()

set(archive "")
set(archive_time 0)

foreach (candidate IN LISTS archives)
    file(TIMESTAMP "${candidate}" candidate_time "%s" UTC)

    if (candidate_time GREATER archive_time)
        set(archive "${candidate}")
        set(archive_time "${candidate_time}")
    endif ()
endforeach ()

message(STATUS "The archive: ${archive}")

file(REMOVE_RECURSE "${WORK}")
file(MAKE_DIRECTORY "${WORK}/sdk")
file(ARCHIVE_EXTRACT INPUT "${archive}" DESTINATION "${WORK}/sdk")

# 1. Configure and build.
set(configure_arguments -S "${SOURCE}" -B "${WORK}/build" "-DDXFG_SDK_DIR=${WORK}/sdk" "-DCMAKE_BUILD_TYPE=${CONFIG}"
        -G "${GENERATOR}")

if (DEFINED PLATFORM AND NOT "${PLATFORM}" STREQUAL "")
    list(APPEND configure_arguments -A "${PLATFORM}")
endif ()

if (DEFINED TOOLSET AND NOT "${TOOLSET}" STREQUAL "")
    list(APPEND configure_arguments -T "${TOOLSET}")
endif ()

if (DEFINED INSTANCE AND NOT "${INSTANCE}" STREQUAL "")
    list(APPEND configure_arguments "-DCMAKE_GENERATOR_INSTANCE=${INSTANCE}")
endif ()

if (NOT GENERATOR MATCHES "^Visual Studio")
    list(APPEND configure_arguments "-DCMAKE_C_COMPILER=${C_COMPILER}" "-DCMAKE_CXX_COMPILER=${CXX_COMPILER}")
endif ()

if (DEFINED OSX_ARCHITECTURES AND NOT "${OSX_ARCHITECTURES}" STREQUAL "")
    list(APPEND configure_arguments "-DCMAKE_OSX_ARCHITECTURES=${OSX_ARCHITECTURES}")
endif ()

execute_process(COMMAND "${CMAKE_COMMAND}" ${configure_arguments}
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 120)
message("configure exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if (NOT "${code}" STREQUAL "0")
    message(FATAL_ERROR "The configuration against the archive exited with ${code}, expected 0")
endif ()

execute_process(COMMAND "${CMAKE_COMMAND}" --build "${WORK}/build" --config "${CONFIG}" --parallel
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 240)
message("build exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if (NOT "${code}" STREQUAL "0")
    message(FATAL_ERROR "The build against the archive exited with ${code}, expected 0")
endif ()

# The tool: in build/<config>/ of a multi-config generator, in build/ of the others.
set(nettest "")

foreach (candidate "${WORK}/build/${CONFIG}/NetTest.exe" "${WORK}/build/${CONFIG}/NetTest" "${WORK}/build/NetTest.exe"
        "${WORK}/build/NetTest")
    if (EXISTS "${candidate}" AND NOT IS_DIRECTORY "${candidate}")
        set(nettest "${candidate}")
        break()
    endif ()
endforeach ()

if (nettest STREQUAL "")
    message(FATAL_ERROR "The build against the archive made no NetTest executable in ${WORK}/build")
endif ()

# 2. The run.
execute_process(COMMAND "${nettest}" p 127.0.0.1:1 -S 100 -r */5k -s 1s --duration 2s
        RESULT_VARIABLE code
        OUTPUT_VARIABLE output
        ERROR_VARIABLE errors
        TIMEOUT 40)
message("exit code: ${code}\nstdout:\n${output}\nstderr:\n${errors}")

if (NOT "${code}" STREQUAL "0")
    message(FATAL_ERROR "NetTest built against the archive exited with ${code}, expected 0")
endif ()

if (NOT output MATCHES "${summary_regex}")
    message(FATAL_ERROR "The output has no line that matches: ${summary_regex}")
endif ()
