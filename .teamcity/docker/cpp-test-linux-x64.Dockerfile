#
# Dockerfile for building and testing C/C++ code on Linux
#
# This Dockerfile creates an environment for building and running the C tests of the SDK
# (src/main/c, CMake + CTest) against a library built in the GraalVM image (graalvm-linux-x64.Dockerfile).
# The library is built for glibc 2.17 (Oracle Linux 7), so the tests also check that it works on a newer distribution.
# The image has no JDK or Maven: the library is built in the GraalVM image.
#
# Tools:
# - build: CMake, Make, Ninja, GCC 13 (gcc-toolset-13, the default: the C code of the SDK does not build with GCC 11),
#   GCC 11 (/usr/bin/gcc, /usr/bin/g++), Clang;
# - analysis and debugging: the ASan/LSan/TSan/UBSan runtimes, clang-tidy, clang-format, GDB, Valgrind;
# - other: Git (CMake FetchContent), Python 3, binutils, curl and the archivers.
#
# BUILD
#
# Run from .teamcity (the build context).
# docker build --pull -t <name:tag> -f docker/cpp-test-linux-x64.Dockerfile .
#
# RUN
#
# docker run --rm -v "$(pwd):/mnt" <name:tag> "cd src/main/c && cmake --preset=conf-release . && ..."
#

FROM oraclelinux:9-slim

RUN microdnf install -y --setopt=install_weak_deps=0 --enablerepo=ol9_codeready_builder \
        cmake make ninja-build gcc gcc-c++ \
        gcc-toolset-13-gcc gcc-toolset-13-gcc-c++ \
        gcc-toolset-13-libasan-devel gcc-toolset-13-liblsan-devel gcc-toolset-13-libtsan-devel \
        gcc-toolset-13-libubsan-devel \
        libasan liblsan libtsan libubsan \
        clang clang-tools-extra \
        gdb valgrind \
        git python3 \
        binutils file which findutils diffutils procps-ng \
        curl tar gzip unzip zip xz && \
    microdnf clean all && \
    rm -rf /var/cache/dnf /var/cache/yum

ENV PATH="/opt/rh/gcc-toolset-13/root/usr/bin:${PATH}"

ENTRYPOINT ["/bin/bash", "-lc"]

WORKDIR /mnt/
