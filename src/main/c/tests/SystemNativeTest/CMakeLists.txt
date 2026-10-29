#
# Executable name and options
#

# Target name
set(target SystemNativeTest)

# Output name
set(output_name ${target})

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CXX_EXTENSIONS OFF)
set(C_EXTENSIONS OFF)

# Print status
message(STATUS "Test ${target}")


#
# Sources
#

set(include_path "${CMAKE_CURRENT_SOURCE_DIR}/")
set(source_path "${CMAKE_CURRENT_SOURCE_DIR}/")

file(GLOB headers CONFIGURE_DEPENDS
        "${include_path}/*.h"
        "${include_path}/*.hpp"
        )
file(GLOB sources CONFIGURE_DEPENDS
        "${source_path}/*.c"
        "${source_path}/*.cpp"
        "${source_path}/*.h"
        "${source_path}/*.hpp"
        )


#
# Create executable
#

# Build executable
add_executable(${target})

# Add sources to target
target_sources(${target}
        PRIVATE
        "${headers}"
        "${sources}"
        )


#
# Project options
#

set_target_properties(${target}
        PROPERTIES
        ${DEFAULT_PROJECT_OPTIONS}
        FOLDER "${IDE_FOLDER}"
        OUTPUT_NAME "${output_name}"
        )


#
# Include directories
#

target_include_directories(${target}
        PRIVATE
        ${DEFAULT_INCLUDE_DIRECTORIES}
        ${PROJECT_BINARY_DIR}/include
        ${PROJECT_BINARY_DIR}/source/include
        ${CMAKE_CURRENT_SOURCE_DIR}/../common
        )


#
# Libraries
#

target_link_libraries(${target}
        PRIVATE
        ${DEFAULT_LIBRARIES}
        Catch2::Catch2
        DxFeedGraalNativeSdk
        )


#
# Compile definitions
#

# The version that dxfg_v4_system_get_version() must report: project.version, the first <version> of pom.xml.
file(READ "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../pom.xml" POM_XML)
string(REGEX MATCH "<version>([^<]+)</version>" POM_VERSION_TAG "${POM_XML}")

if (NOT CMAKE_MATCH_1)
    message(FATAL_ERROR "No <version> in pom.xml")
endif ()

target_compile_definitions(${target}
        PRIVATE
        ${DEFAULT_COMPILE_DEFINITIONS}
        DXFG_TEST_SDK_VERSION="${CMAKE_MATCH_1}"
        )


#
# Compile options
#

target_compile_options(${target}
        PRIVATE
        ${DEFAULT_COMPILE_OPTIONS}
        )


#
# Linker options
#

target_link_options(${target}
        PRIVATE
        ${DEFAULT_LINKER_OPTIONS}
        )


#
# Testing
#

add_test(NAME ${target} COMMAND $<TARGET_FILE:${target}>)

# A call of an experimental function with DXFG_EXPERIMENTAL defined as a deprecation attribute: the test builds it
# with the deprecation warning as an error and passes when the compiler names the function and the message.
add_library(${target}_ExperimentalMarker OBJECT EXCLUDE_FROM_ALL "${source_path}/experimental_marker/call.cpp")
target_link_libraries(${target}_ExperimentalMarker PRIVATE DxFeedGraalNativeSdk)

if (MSVC)
    target_compile_options(${target}_ExperimentalMarker PRIVATE /we4996)
else ()
    target_compile_options(${target}_ExperimentalMarker PRIVATE -Werror=deprecated-declarations)
endif ()

add_test(NAME ${target}_ExperimentalMarker
        COMMAND ${CMAKE_COMMAND} --build ${CMAKE_BINARY_DIR} --target ${target}_ExperimentalMarker --config $<CONFIG>)
set_tests_properties(${target}_ExperimentalMarker PROPERTIES
        PASS_REGULAR_EXPRESSION "dxfg_v4_DXFeedSubscription_getTotalDroppedEvents[^\r\n]*experimental")


#
# Custom commands
#

# Copy shared library to output directory
add_custom_command(TARGET ${target} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:DxFeedGraalNativeSdk> $<TARGET_FILE_DIR:${target}>
        )
