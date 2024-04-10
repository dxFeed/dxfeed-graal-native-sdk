#!/bin/bash

maven() {
    set -eux

    local -r version="$1"
    local -r install_path="$2"
    local -r base_url="https://dlcdn.apache.org/maven/"

    if [[ -z "${version}" || -z "${install_path}" ]]; then
        echo "Usage: install maven <version> <install_path>"
        return 1
    fi

    mkdir -p "${install_path}"
    local -r download_url="${base_url}/maven-${version:0:1}/${version}/binaries/apache-maven-${version}-bin.tar.gz"
    curl -fsSL --retry 3 "${download_url}" | bsdtar -xzf - -C "${install_path}" --strip-components=1
}

graalvm() {
    set -eux

    local -r version="$1"
    local -r platform="$2"
    local -r install_path="$3"
    local -r base_url="https://github.com/graalvm/graalvm-ce-builds/releases/download/"

    if [[ -z "${version}" || -z "${platform}" || -z "${install_path}" ]]; then
        echo "Usage: install graalvm <version> <platform> <install_path>"
        return 1
    fi

    local -r platform_os="${platform%-*}"  # Extracts part before the last dash.
    local -r platform_arch="${platform##*-}"  # Extracts part after the last dash.
    local file_extension="tar.gz"
    local version_tag release_tag distribution_tag os_tag arch_tag suffix

    # Starting in 2023, GraalVM will align with the six-month JDK release cadence.
    # GraalVM releases are identified by the JDK release they provide, for example, jdk-17.0.0.
    # Prior to 2023, GraalVM release numbering was based on the calendar year (and java version),
    # for example, java11-22.3.1, java17-22.3.1.
    if [[ "${version}" =~ ^java ]]; then
        release_tag="$(echo "${version}" | grep -oP '\d+\.\d+\.\d+$')"
        version_tag="vm-${release_tag}"
        distribution_tag="graalvm-ce-java$(echo "${version}" | grep -oP 'java\K\d+')"
        case "$platform_os" in
            "linux") os_tag="-linux" ;;
            "osx") os_tag="-darwin" ;;
            "win") os_tag="-windows"; file_extension="zip";  ;;
        esac
        case "$platform_arch" in
            "x64") arch_tag="-amd64" ;;
            "arm64") arch_tag="-aarch64" ;;
        esac
        suffix="-${release_tag}.${file_extension}"
    elif [[ "${version}" =~ ^jdk- ]]; then
        version_tag="${version}"
        distribution_tag="graalvm-community-${version}"
        case "$platform_os" in
            "linux") os_tag="_linux" ;;
            "osx") os_tag="_macos" ;;
            "win") os_tag="_windows"; file_extension="zip"; ;;
        esac
        case "$platform_arch" in
            "x64") arch_tag="-x64" ;;
            "arm64") arch_tag="-aarch64" ;;
        esac
        suffix="_bin.${file_extension}"
    else
        echo "Invalid GraalVM version format: '${version}'. Allowed formats:"
        echo "    java<java_version>-<version> (legacy format)"
        echo "    jdk-<jdk_version>"
        return 1
    fi

    mkdir -p "${install_path}"
    local -r download_url="${base_url}/${version_tag}/${distribution_tag}${os_tag}${arch_tag}${suffix}"
    curl -fsSL --retry 3 "${download_url}" | bsdtar -xzf - -C "${install_path}" --strip-components=1

    # gu does not exist in GraalVM versions above 22.3.3
    # and native-image installation is not required.
    cd "${install_path}/bin"
    local -r gu=$(find . -maxdepth 1 -name "gu*" -print -quit)
    if [ -n "${gu}" ]; then
        "${gu}" install native-image
    fi
}

vs_build_tools() {
    set -eux

    local -r version="$1"
    local -r install_path="$2"
    local -r base_url="https://aka.ms/vs/"

    if [[ -z "${version}" || -z "${install_path}" ]]; then
        echo "Usage: install vs_build_tools <version> <install_path>"
        return 1
    fi

    local -r vs_buildtools="vs_buildtools.exe"
    # shellcheck disable=SC2064
    trap "rm -rf ${vs_buildtools}" EXIT

    local -r download_url="${base_url}/${version}/release/${vs_buildtools}"
    curl -fsSL --retry 3 "${download_url}" --output "${vs_buildtools}"
    cmd //S //C "(start /w ${vs_buildtools} --quiet --wait --norestart --nocache \
         --installPath ${install_path} \
         --includeRecommended \
         --add Microsoft.VisualStudio.Workload.VCTools \
         --add Microsoft.VisualStudio.Component.VC.Tools.x86.x64 \
         --add Microsoft.VisualStudio.Component.Windows11SDK.22621)"
}

"$@"
