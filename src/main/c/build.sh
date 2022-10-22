#!/bin/sh
cmake --preset="conf-release" -DCMAKE_BUILD_TYPE=Release .
cmake --build --preset="build-release" --config=Release
cmake --install build/conf-release --config=Release --strip --component=runtime
