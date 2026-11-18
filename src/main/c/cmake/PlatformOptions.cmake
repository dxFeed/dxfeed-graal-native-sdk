# SPDX-License-Identifier: MPL-2.0

#
# The compiler flags and the rpath of the platform, shared by the build of src/main/c and by the build of NetTest
# against an SDK archive (src/apps/NetTest/CMakeLists.txt): the SDK library lies next to the executables.
#

if (WIN32)
    set(CMAKE_CXX_FLAGS "/utf-8 /W4 /volatile:iso /EHsc /Zc:throwingNew")
elseif (APPLE)
    set(CMAKE_MACOSX_RPATH "ON")
    set(CMAKE_SKIP_BUILD_RPATH "ON")
    set(CMAKE_BUILD_WITH_INSTALL_RPATH "ON")
    set(CMAKE_INSTALL_RPATH_USE_LINK_PATH "OFF")
    set(CMAKE_BUILD_RPATH_USE_ORIGIN "ON")
    set(CMAKE_INSTALL_RPATH "@loader_path")
elseif (UNIX)
    set(CMAKE_SKIP_BUILD_RPATH "ON")
    set(CMAKE_BUILD_WITH_INSTALL_RPATH "ON")
    set(CMAKE_INSTALL_RPATH_USE_LINK_PATH "OFF")
    set(CMAKE_BUILD_RPATH_USE_ORIGIN "ON")
    set(CMAKE_INSTALL_RPATH "$ORIGIN")
endif ()
