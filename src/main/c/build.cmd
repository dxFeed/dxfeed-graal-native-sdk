cmake --preset="conf-release" -DCMAKE_BUILD_TYPE=Release .
if %errorlevel% neq 0 exit /b %errorlevel%
cmake --build --preset="build-release" --config=Release
if %errorlevel% neq 0 exit /b %errorlevel%
ctest --test-dir build/conf-release -C Release --extra-verbose
if %errorlevel% neq 0 exit /b %errorlevel%
cmake --install build/conf-release --config=Release --strip --component=runtime
if %errorlevel% neq 0 exit /b %errorlevel%
cpack --config build/conf-release/CPackConfig.cmake
if %errorlevel% neq 0 exit /b %errorlevel%
