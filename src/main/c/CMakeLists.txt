# SPDX-License-Identifier: MPL-2.0

#
# CMake options
#

# CMake version
cmake_minimum_required(VERSION 3.20 FATAL_ERROR)


#
# Configure CMake environment
#

# Include cmake modules
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")
include(FetchContent)
include(cmake/Custom.cmake)
include(cmake/GetGitRevisionDescription.cmake)

# Set policies
set_policy(CMP0135 NEW) # Sets the timestamps of extracted contents (FetchContent) to the time of extraction.


#
# Project description and (meta) information
#

# Get git revision
get_git_head_revision(GIT_REFSPEC GIT_SHA1)
string(SUBSTRING "${GIT_SHA1}" 0 12 GIT_REV)
if (NOT GIT_SHA1)
    set(GIT_REV "0")
endif ()

# Meta information about the project
set(META_PROJECT_NAME "dxfeed-graal-native-api-samples")
set(META_PROJECT_DESCRIPTION "The wrapper over QD (Java) implementation with GraalVM (native-image) for provide C-ABI ")
set(META_AUTHOR_ORGANIZATION "Devexperts LLC")
set(META_VERSION_MAJOR "1")
set(META_VERSION_MINOR "0")
set(META_VERSION_PATCH "0")
set(META_VERSION_REVISION "${GIT_REV}")
set(META_VERSION "${META_VERSION_MAJOR}.${META_VERSION_MINOR}.${META_VERSION_PATCH}")
set(META_NAME_VERSION "${META_PROJECT_NAME} v${META_VERSION} (${META_VERSION_REVISION})")
set(META_CMAKE_INIT_SHA "${GIT_REV}")

string(MAKE_C_IDENTIFIER ${META_PROJECT_NAME} META_PROJECT_ID)
string(TOUPPER ${META_PROJECT_ID} META_PROJECT_ID)


#
# Declare project
#

# Set project
project(${META_PROJECT_NAME}
        VERSION ${META_VERSION}
        DESCRIPTION ${META_PROJECT_DESCRIPTION}
        LANGUAGES C CXX
        )
message(STATUS "System: ${CMAKE_SYSTEM_NAME}")
message(STATUS "Architecture: ${CMAKE_HOST_SYSTEM_PROCESSOR}")
message(STATUS "Project: ${META_PROJECT_NAME}")
message(STATUS "BuildType: ${CMAKE_BUILD_TYPE}")
message(STATUS "SkipTests: ${SKIP_TESTS}")

#
# Project configuration options
#

if (PROJECT_IS_TOP_LEVEL)
    # Generate folders for IDE targets (e.g., VisualStudio solutions)
    set_property(GLOBAL PROPERTY USE_FOLDERS ON)
    set(IDE_FOLDER "")
endif ()

if (PROJECT_IS_TOP_LEVEL)
    # Set output directories (identically for different platforms)
    set(OUTPUT_PATH ${CMAKE_CURRENT_SOURCE_DIR}/bin)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${OUTPUT_PATH}/$<CONFIG>)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_$<CONFIG> ${OUTPUT_PATH}/$<CONFIG>)
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${OUTPUT_PATH}/$<CONFIG>)
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_$<CONFIG> ${OUTPUT_PATH}/$<CONFIG>)
    set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${OUTPUT_PATH}/$<CONFIG>)
    set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_$<CONFIG> ${OUTPUT_PATH}/$<CONFIG>)
endif ()
message(STATUS "Output path ${OUTPUT_PATH}")

# Create version file
file(WRITE "${OUTPUT_PATH}/${META_PROJECT_NAME}-version" "${META_NAME_VERSION}")


#
# Configure platform specific variable
#

set(ARCH_POSTFIX "")
if (WIN32)
    set(ARCH_POSTFIX "amd64-windows")
    set(CMAKE_CXX_FLAGS "/utf-8 /W4 /volatile:iso /EHsc /Zc:throwingNew")
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS "ON")
elseif (APPLE)
    if (${CMAKE_HOST_SYSTEM_PROCESSOR} STREQUAL "arm64")
        set(ARCH_POSTFIX "aarch64-osx")
    else ()
        set(ARCH_POSTFIX "x86_64-osx")
    endif ()
    set(CMAKE_MACOSX_RPATH "ON")
    set(CMAKE_SKIP_BUILD_RPATH "ON")
    set(CMAKE_BUILD_WITH_INSTALL_RPATH "ON")
    set(CMAKE_INSTALL_RPATH_USE_LINK_PATH "OFF")
    set(CMAKE_BUILD_RPATH_USE_ORIGIN "ON")
    set(CMAKE_INSTALL_RPATH "@loader_path")
elseif (UNIX)
    set(ARCH_POSTFIX "amd64-linux")
    set(CMAKE_SKIP_BUILD_RPATH "ON")
    set(CMAKE_BUILD_WITH_INSTALL_RPATH "ON")
    set(CMAKE_INSTALL_RPATH_USE_LINK_PATH "OFF")
    set(CMAKE_BUILD_RPATH_USE_ORIGIN "ON")
    set(CMAKE_INSTALL_RPATH "$ORIGIN")
else ()
    message(ERROR "Unknown platform!")
endif ()


#
# Deployment
#

# Installation paths
set(INSTALL_BIN ${CMAKE_CURRENT_SOURCE_DIR}/install)
set(INSTALL_SHARED ${CMAKE_CURRENT_SOURCE_DIR}/install)
set(INSTALL_LIB ${CMAKE_CURRENT_SOURCE_DIR}/install)


#
# External dependencies
#

# Add date lib
FetchContent_Declare(date
        GIT_REPOSITORY https://github.com/HowardHinnant/date.git
        GIT_TAG v3.0.1
        )
FetchContent_MakeAvailable(date)


#
# Project modules
#

# Set compile options
include(cmake/CompileOptions.cmake)

# Add GRAAL_API
set(GRAAL_CUSTOM_BIN_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../target/native-image/)
add_subdirectory(api)

# Add sources directory
add_subdirectory(extern)
add_subdirectory(src/apps)
add_subdirectory(src/libs)
if (NOT ${SKIP_TESTS})
    enable_testing() # Must be in top-level file
    #add_subdirectory(tests)
endif ()
