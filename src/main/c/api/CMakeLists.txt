# SPDX-License-Identifier: MPL-2.0

cmake_minimum_required(VERSION 3.21)

# Set project
project(DxFeedGraalNativeSdk)

# Print status
message(STATUS "Imported lib ${PROJECT_NAME}")

set(DXFG_SRC_LOCATION ${CMAKE_CURRENT_SOURCE_DIR})
set(DXFG_BIN_LOCATION ${CMAKE_CURRENT_SOURCE_DIR})

if (DEFINED GRAAL_CUSTOM_SRC_LOCATION)
    set(DXFG_SRC_LOCATION ${GRAAL_CUSTOM_SRC_LOCATION})
endif ()

if (DEFINED GRAAL_CUSTOM_BIN_LOCATION)
    set(DXFG_BIN_LOCATION ${GRAAL_CUSTOM_BIN_LOCATION})
endif ()

option(GRAAL_NATIVE_SDK_INSTALL "Prepare install" ON)

# Add imported library
add_library(${PROJECT_NAME} SHARED IMPORTED GLOBAL)

# Set empty library prefix
set(DXFG_LIB_PREFIX "")
# Set empty library extension
set(DXFG_LIB_EXTENSION "")

# Add platform specific variable
if (WIN32)
    set(DXFG_LIB_PREFIX "")
    set(DXFG_LIB_EXTENSION "dll")
elseif (APPLE)
    set(DXFG_LIB_PREFIX "lib")
    set(DXFG_LIB_EXTENSION "dylib")
elseif (UNIX)
    set(DXFG_LIB_PREFIX "lib")
    set(DXFG_LIB_EXTENSION "so")
else ()
    message(ERROR "Unknown platform!")
endif ()


#
# Project options
#

# Target properties
set_target_properties(${PROJECT_NAME}
        PROPERTIES
        INTERFACE_INCLUDE_DIRECTORIES "${DXFG_SRC_LOCATION}"
        # Path to dynamic library
        IMPORTED_LOCATION "${DXFG_BIN_LOCATION}/${DXFG_LIB_PREFIX}${PROJECT_NAME}.${DXFG_LIB_EXTENSION}"
        # For Windows
        IMPORTED_IMPLIB "${DXFG_BIN_LOCATION}/${DXFG_LIB_PREFIX}${PROJECT_NAME}.lib"
        # For Linux and macOS
        IMPORTED_NO_SONAME "TRUE"
        )

if (GRAAL_NATIVE_SDK_INSTALL)
    # Deployment
    install(IMPORTED_RUNTIME_ARTIFACTS ${PROJECT_NAME}
            RUNTIME DESTINATION ${INSTALL_BIN} COMPONENT runtime
            LIBRARY DESTINATION ${INSTALL_SHARED} COMPONENT runtime
            )
endif ()
