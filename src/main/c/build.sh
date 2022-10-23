#!/bin/sh
cmake --preset="conf-release" -DCMAKE_BUILD_TYPE=Release .
cmake --build --preset="build-release" --config=Release
ctest --test-dir build/conf-release -C Release --extra-verbose
cmake --install build/conf-release --config=Release --strip --component=runtime
cpack --config build/conf-release/CPackConfig.cmake
