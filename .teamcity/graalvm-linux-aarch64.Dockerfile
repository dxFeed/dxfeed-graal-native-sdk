#
# Dockerfile for GraalVM on Linux
#
# This Dockerfile is designed to create a containerized Linux based development environment
# with the pre-installed GraalVM and Apache Maven.
# It's tailored for building and deploying Java projects.
# This image can be run from Linux or Windows (using containerd and nerdctl).
#
# BUILD
#
# How to build an image:
# docker build -t <name:tag> -f <Dockerfile> .
#
# For example:
# docker build --build-arg GRAALVM_VERSION="java11-22.3.1" -t graalvm:linux-aarch64-java11-22.3.1 -f graalvm-linux-aarch64.Dockerfile .
# docker build --build-arg GRAALVM_VERSION="jdk-22.0.1" -t graalvm:linux-aarch64-jdk-22.0.1 -f graalvm-linux-aarch64.Dockerfile .
#
# MOUNTING DIRECTORIES
#
# To mount the current directory into a container, use ${PWD} in PowerShell, %cd% in cmd,
# or $(pwd) if you are using Linux with bash (sh, zsh, fish, etc).
# It is expected that the project root will be mounted into container.
#
# NOTES
#
# If you're using Windows PowerShell is very weird about handling arguments that contain a dot,
# so it's probably better to use cmd.
#
# EXAMPLES
#
# How to build a mvn project (run from project root, quotation marks required):
# docker run -m 8GB -v $(pwd):/mnt/ --rm <name:tag> "mvn clean package"
#
# How to deploy a mvn project to the JFrog artifactory (run from project root, quotation marks required):
# docker run -m 8GB -v $(pwd):/mnt/ --rm <name:tag> "mvn --settings .teamcity/settings.xml -Djfrog.user=<user> -Djfrog.password=<pass> deploy"
#
# How to run an interactive:
# docker run -m 8GB --rm -it <name:tag> bash

# Use Oracle Linux 7 (binary compatible with RHEL 7) for glibc 2.17.
FROM oraclelinux:7-slim

ARG TARGETPLATFORM="linux-aarch64"

# Update and install dependencies.
RUN yum update -y oraclelinux-release-el7 && \
    yum install -y oraclelinux-developer-release-el7 oracle-softwarecollection-release-el7 && \
    yum-config-manager --enable ol7_developer && \
    yum-config-manager --enable ol7_developer_EPEL && \
    yum-config-manager --enable ol7_optional_latest && \
    yum install -y bzip2-devel bsdtar ed gcc gcc-c++ gcc-gfortran git grep gzip file fontconfig less libcurl-devel make openssl openssl-devel readline-devel tar vi which xz-devel zlib-devel && \
    yum install -y glibc-static libcxx libcxx-devel libstdc++-static zlib-static && \
    rm -rf /var/cache/yum

# Update font cache.
RUN fc-cache -f -v

# Copy the helper script to the PATH directory.
COPY install.sh "/usr/local/bin/"

# Download and install Apache Maven.
ARG MVN_VERSION="3.8.9"
ARG MVN_INSTALL_PATH="/opt/mvn/"
ENV PATH="${MVN_INSTALL_PATH}/bin:${PATH}"
RUN install.sh maven "${MVN_VERSION}" "${MVN_INSTALL_PATH}"

# Download and install GraalVM.
ARG GRAALVM_VERSION="java11-22.3.1"
ARG GRAALVM_INSTALL_PATH="/opt/graalvm/"
ENV JAVA_HOME="${GRAALVM_INSTALL_PATH}"
ENV PATH="${GRAALVM_INSTALL_PATH}/bin:${PATH}"
RUN install.sh graalvm "${GRAALVM_VERSION}" "${TARGETPLATFORM}" "${GRAALVM_INSTALL_PATH}"

# Define the entry point for the docker container.
# Use bash directly instead of mvn.
ENTRYPOINT ["/bin/bash", "-lc"]

# Default working directory.
# It is expected that the project root will be mounted in this directory.
WORKDIR /mnt/
