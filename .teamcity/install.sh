#!/bin/bash

maven() {
    set -eux
    set -o pipefail

    local -r version="$1"
    # Strip the trailing slash, otherwise the temporary path below ends up inside the install path.
    local -r install_path="${2%/}"
    # archive.apache.org keeps every released version forever, unlike dlcdn.apache.org,
    # which only hosts the single latest patch release of each minor line and 404s
    # as soon as a newer patch is published.
    local -r base_url="https://archive.apache.org/dist/maven"

    if [[ -z "${version}" || -z "${install_path}" ]]; then
        echo "Usage: install maven <version> <install_path>"
        return 1
    fi

    local -r tmp_path="${install_path}.tmp.$$"
    trap 'rm -rf "${tmp_path}"' RETURN

    mkdir -p "${tmp_path}"
    local -r download_url="${base_url}/maven-${version:0:1}/${version}/binaries/apache-maven-${version}-bin.tar.gz"
    curl -fsSL --retry 3 "${download_url}" | bsdtar -xzf - -C "${tmp_path}" --strip-components=1

    rm -rf "${install_path}"
    mv "${tmp_path}" "${install_path}"
}

graalvm() {
    set -eux
    set -o pipefail

    local -r version="$1"
    local -r platform="$2"
    # Strip the trailing slash, otherwise the temporary path below ends up inside the install path.
    local -r install_path="${3%/}"
    local base_url="https://github.com/graalvm/graalvm-ce-builds/releases/download/"

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
        release_tag="$(perl -pe '$_ = $1 if /(\d+\.\d+\.\d+)$/o' <<< "${version}")"
        version_tag="vm-${release_tag}"
        distribution_tag="graalvm-ce-java$(perl -pe '$_ = $1 if /java(\d+)/o' <<< "${version}")"
        case "$platform_os" in
            "linux") os_tag="-linux" ;;
            "macos") os_tag="-darwin" ;;
            "osx") os_tag="-darwin" ;;
            "win") os_tag="-windows"; file_extension="zip";  ;;
        esac
        case "$platform_arch" in
            "x64") arch_tag="-amd64" ;;
            "amd64") arch_tag="-amd64" ;;
            "arm64") arch_tag="-aarch64" ;;
            "aarch64") arch_tag="-aarch64" ;;
        esac
        suffix="-${release_tag}.${file_extension}"
    # Since GraalVM 25, in addition to the JDK-aligned releases (jdk-25.0.2), there are
    # Innovation releases identified by the graal version, for example, graal-25.4.4.1.1,
    # which is GraalVM 25 Innovation 4 based on JDK 25.0.4.1.1 (artifacts are named jdk-25i4-25.0.4.1.1).
    # The "oracle-" prefix selects Oracle GraalVM (GFTC license) instead of GraalVM Community,
    # for example, oracle-jdk-25.0.4 or oracle-graal-25.4.4.1.1.
    elif [[ "${version}" =~ ^(oracle-)?(jdk|graal)-(.+)$ ]]; then
        local -r vendor="${BASH_REMATCH[1]:+oracle}"
        local -r kind="${BASH_REMATCH[2]}"
        local -r number="${BASH_REMATCH[3]}"
        local jdk_tag major
        if [[ "${kind}" == "graal" ]]; then
            if [[ ! "${number}" =~ ^([0-9]+)\.([0-9]+)\.(.+)$ ]]; then
                echo "Invalid GraalVM version format: '${version}'. Expected graal-<major>.<innovation>.<jdk_update>"
                return 1
            fi
            major="${BASH_REMATCH[1]}"
            jdk_tag="${major}i${BASH_REMATCH[2]}-${major}.0.${BASH_REMATCH[3]}"
        else
            major="${number%%.*}"
            jdk_tag="${number}"
        fi
        case "$platform_os" in
            "linux") os_tag="_linux" ;;
            "osx") os_tag="_macos" ;;
            "win") os_tag="_windows"; file_extension="zip"; ;;
        esac
        case "$platform_arch" in
            "x64") arch_tag="-x64" ;;
            "amd64") arch_tag="-x64" ;;
            "arm64") arch_tag="-aarch64" ;;
            "aarch64") arch_tag="-aarch64" ;;
        esac
        suffix="_bin.${file_extension}"
        if [[ -z "${vendor}" ]]; then
            version_tag="${kind}-${number}"
            distribution_tag="graalvm-community-jdk-${jdk_tag}"
        elif [[ "${kind}" == "graal" ]]; then
            base_url="https://gds.oracle.com/download/graal"
            version_tag="${jdk_tag%%-*}/archive"
            distribution_tag="graalvm-jdk-${jdk_tag}"
        else
            base_url="https://download.oracle.com/graalvm"
            version_tag="${major}/archive"
            distribution_tag="graalvm-jdk-${jdk_tag}"
        fi
    else
        echo "Invalid GraalVM version format: '${version}'. Allowed formats:"
        echo "    java<java_version>-<version> (legacy format)"
        echo "    jdk-<jdk_version>"
        echo "    graal-<graal_version> (innovation releases)"
        echo "    oracle-jdk-<jdk_version> or oracle-graal-<graal_version> (Oracle GraalVM)"
        return 1
    fi

    local -r tmp_path="${install_path}.tmp.$$"
    trap 'rm -rf "${tmp_path}"' RETURN

    mkdir -p "${tmp_path}"
    local -r download_url="${base_url}/${version_tag}/${distribution_tag}${os_tag}${arch_tag}${suffix}"
    curl -fsSL --retry 3 "${download_url}" | bsdtar -xzf - -C "${tmp_path}" --strip-components=1

    # gu does not exist in GraalVM versions above 22.3.3
    # and native-image installation is not required.
    local -r gu=$(find "${tmp_path}" -name "gu*" -print -quit)
    if [ -n "${gu}" ]; then
        "${gu}" install native-image
    fi

    rm -rf "${install_path}"
    mv "${tmp_path}" "${install_path}"
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
