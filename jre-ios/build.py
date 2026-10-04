#!/usr/bin/env python3
# Copyright (c) 2026 Devexperts LLC.
# SPDX-License-Identifier: MPL-2.0

"""Builds the static libraries of the JDK and of Substrate VM for iOS (jdk-*-r.a, graal-svm-*-r.a) that the native
image of the SDK for iOS is linked with, and checks that an image links with them.

The native image contains the Java code of the JDK and of Substrate VM, the libraries contain their native code, so
both must come from the same sources: each slice is built from the sources of the GraalVM Community version that
builds its image (graal) and of the labs JDK that it is built on (labs-openjdk, found by mx). The x86_64 slice of the
simulator has its own version, since GraalVM for macOS x64 is no longer published after jdk-25.0.1.

    build.py build --graal-tag graal-25.4.4.1.1 --x64-graal-tag jdk-25.0.1 --work ~/.graal/jre-ios --out out/jre-ios
    build.py check --dir <unpacked graal-native-sdk-*-aarch64-ios.zip> --platform ios
    build.py set-build-version --file libDxFeedGraalNativeSdk.o --platform simulator
    build.py manifest --dir <the files of an archive> --platform ios --version 3.7.1
    build.py xcframework --ios <dir> --simulator <dir> --macos <arm64 dir> <x86_64 dir> --version 3.7.1 --zip <zip>

set-build-version needs only python3 (and Xcode for the default SDK version). The others need macOS on arm64 with
Xcode, git, curl, make and python3; autoconf (for the configure of the JDK) is installed into the work directory when
it is missing.
"""
import argparse
import concurrent.futures
import glob
import json
import os
import platform
import plistlib
import re
import shlex
import shutil
import struct
import subprocess
import sys
import textwrap

HERE = os.path.dirname(os.path.abspath(__file__))

GRAAL_REPOSITORY = 'https://github.com/oracle/graal.git'
MX_REPOSITORY = 'https://github.com/graalvm/mx.git'
LABS_JDK_REPOSITORY = 'https://github.com/graalvm/labs-openjdk.git'
GNU_MIRROR = 'https://ftp.gnu.org/gnu'
M4_VERSION = '1.4.19'
AUTOCONF_VERSION = '2.72'

# The name of the configuration of the JDK build (build/<name>), it must not contain the architecture.
JDK_CONFIGURATION = 'ios-host'


class Slice:
    """One architecture of one platform: the libraries for the simulator are fat (arm64 and x86_64)."""

    def __init__(self, name, sdk, arch, triple_suffix, x64=False):
        self.name = name
        self.sdk = sdk
        self.arch = arch
        self.triple_suffix = triple_suffix
        # Built from the sources of the x64 GraalVM (--x64-graal-tag).
        self.x64 = x64

    def triple(self, minimum_version):
        return f'{self.arch}-apple-ios{minimum_version}{self.triple_suffix}'


SLICES = {
    'ios-arm64': Slice('ios-arm64', 'iphoneos', 'arm64', ''),
    'simulator-arm64': Slice('simulator-arm64', 'iphonesimulator', 'arm64', '-simulator'),
    'simulator-x86_64': Slice('simulator-x86_64', 'iphonesimulator', 'x86_64', '-simulator', x64=True),
}

# The published libraries: the name and the slices of each, as the pom and the Swift API expect them.
LIBRARIES = [
    ('jdk-arm64-ios-r.a', 'jdk', ['ios-arm64']),
    ('graal-svm-arm64-ios-r.a', 'svm', ['ios-arm64']),
    ('jdk-ios-simulator-r.a', 'jdk', ['simulator-arm64', 'simulator-x86_64']),
    ('graal-svm-ios-simulator-r.a', 'svm', ['simulator-arm64', 'simulator-x86_64']),
]

# The flags of the Substrate VM native projects (substratevm/mx.substratevm/suite.py, darwin), without the debug
# information.
SVM_PROJECT_FLAGS = {
    'com.oracle.svm.native.libchelper': ['-fPIC', '-O2', '-D_LITTLE_ENDIAN', '-fvisibility=hidden',
                                         '-D_FORTIFY_SOURCE=0'],
    'com.oracle.svm.native.darwin': ['-ObjC', '-fPIC', '-O1', '-D_LITTLE_ENDIAN', '-fvisibility=hidden',
                                     '-D_FORTIFY_SOURCE=0'],
    'com.oracle.svm.native.jvm.posix': ['-fPIC', '-O2', '-fvisibility=hidden'],
}

# The system libraries that the objects of a JDK library need, as linker options. The objects name them in
# LC_LINKER_OPTION, so the linker of the application adds them (autolinking): the application does not have to know
# them, and the libraries do not embed their own copies (zlib), which would conflict with the system ones that the
# application may use. Swift adds the frameworks of the imported modules itself (Foundation imports CFNetwork), C and
# Objective-C applications do not.
LIBRARY_DEPENDENCIES = {
    'net': [['-framework', 'CFNetwork']],  # DefaultProxySelector.c
    'zip': [['-lz']],  # Deflater.c, Inflater.c, zip_util.c, CRC32.c, Adler32.c
}

# The link check links only with what any application that uses the SDK links (Swift and Objective-C link Foundation),
# so a system library missing from LIBRARY_DEPENDENCIES fails it, as it fails the link of the application.
CHECK_LINK_FLAGS = ['-framework', 'Foundation']

# The iOS Simulator on arm64 starts with iOS 14.0: clang raises a lower minimum version of an arm64 simulator target to
# it, so the objects of the libraries declare it, and the native image declares it too.
SIMULATOR_ARM64_MIN_VERSION = '14.0'

# Mach-O (mach-o/loader.h, mach-o/fat.h).
MH_MAGIC_64 = 0xfeedfacf
FAT_MAGIC = 0xcafebabe
FAT_MAGIC_64 = 0xcafebabf
MACH_HEADER_64_SIZE = 32
CPU_TYPES = {0x01000007: 'x86_64', 0x0100000c: 'arm64'}
LC_SEGMENT_64 = 0x19
LC_SYMTAB = 0x2
LC_DYSYMTAB = 0xb
LC_BUILD_VERSION = 0x32
BUILD_VERSION_COMMAND_SIZE = 24
# LC_VERSION_MIN_MACOSX, LC_VERSION_MIN_IPHONEOS, LC_VERSION_MIN_TVOS, LC_VERSION_MIN_WATCHOS.
VERSION_MIN_COMMANDS = {0x24, 0x25, 0x2f, 0x30}
# The linkedit_data_command commands (dataoff, datasize): LC_CODE_SIGNATURE, LC_SEGMENT_SPLIT_INFO,
# LC_FUNCTION_STARTS, LC_DATA_IN_CODE, LC_DYLIB_CODE_SIGN_DRS, LC_LINKER_OPTIMIZATION_HINT, LC_DYLD_EXPORTS_TRIE,
# LC_DYLD_CHAINED_FIXUPS (with LC_REQ_DYLD), LC_ATOM_INFO.
LINKEDIT_DATA_COMMANDS = {0x1d, 0x1e, 0x26, 0x29, 0x2b, 0x2e, 0x80000033, 0x80000034, 0x36}
# The commands without file offsets: LC_LINKER_OPTION, LC_UUID, LC_SOURCE_VERSION, LC_BUILD_VERSION,
# LC_VERSION_MIN_*.
COMMANDS_WITHOUT_DATA = {0x2d, 0x1b, 0x2a, LC_BUILD_VERSION} | VERSION_MIN_COMMANDS
PLATFORMS = {1: 'macos', 2: 'ios', 7: 'iossimulator'}
PLATFORM_IDS = {name: number for number, name in PLATFORMS.items()}

IMAGE_OBJECT = 'libDxFeedGraalNativeSdk.o'
IMAGE_DYLIB = 'libDxFeedGraalNativeSdk.dylib'
# The manifest of the linker flags in the archives for iOS, the simulator and macOS (see manifest).
MANIFEST = 'link-flags.txt'
PLATFORM_TITLES = {'ios': 'iOS (iphoneos)', 'simulator': 'iOS Simulator (iphonesimulator)', 'macos': 'macOS'}
# The XCFramework: one framework (and Clang module) per platform, static for iOS and the simulator (the archive of the
# image object and of the libraries), dynamic for macOS (the library of the SDK).
FRAMEWORK = 'DxFeedGraalNativeSdk'
FRAMEWORK_IDENTIFIER = 'com.dxfeed.DxFeedGraalNativeSdk'
MACOS_MIN_VERSION = '11.0'


def log(message):
    print(message, flush=True)


def run(command, cwd=None, env=None, capture=False):
    log('+ ' + ' '.join(shlex.quote(str(part)) for part in command))
    if capture:
        return subprocess.run(command, cwd=cwd, env=env, check=True, stdout=subprocess.PIPE,
                              universal_newlines=True).stdout
    subprocess.run(command, cwd=cwd, env=env, check=True)
    return None


def fail(message):
    sys.exit('error: ' + message)


def read_list(path):
    with open(path, encoding='utf-8') as file:
        return [line.strip() for line in file if line.strip() and not line.startswith('#')]


def sdk_path(sdk):
    result = subprocess.run(['xcrun', '--sdk', sdk, '--show-sdk-path'], stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, universal_newlines=True)
    if result.returncode != 0:
        # The Command Line Tools have only the macOS SDK.
        fail(f'no {sdk} SDK: install Xcode and select it with sudo xcode-select -s /Applications/Xcode.app '
             f'({result.stdout.strip()})')
    return result.stdout.strip()


def check_host():
    if platform.system() != 'Darwin' or platform.machine() != 'arm64':
        fail(f'needs macOS on arm64 (run it with arch -arm64), not {platform.system()} {platform.machine()}')
    for sdk in ('iphoneos', 'iphonesimulator'):
        log(f'{sdk} SDK: {sdk_path(sdk)}')


def clone(repository, tag, directory):
    """A shallow clone of the tag; an existing clone of the same tag is reused (the sources are not changed except by
    the patch, which is applied idempotently)."""
    marker = os.path.join(directory, '.jre-ios-tag')
    if os.path.isfile(marker):
        with open(marker, encoding='utf-8') as file:
            if file.read().strip() == tag:
                log(f'Reusing {directory} ({tag})')
                return
    shutil.rmtree(directory, ignore_errors=True)
    run(['git', '-c', 'advice.detachedHead=false', 'clone', '--quiet', '--depth', '1', '--branch', tag, repository,
         directory])
    with open(marker, 'w', encoding='utf-8') as file:
        file.write(tag)


def ensure_autoconf(work):
    """The configure script of the JDK generates itself with autoconf. macOS has neither autoconf nor a GNU m4 recent
    enough for it, so both are built into the work directory when autoconf is missing."""
    if shutil.which('autoconf'):
        return {}
    tools = os.path.join(work, 'tools')
    bin_directory = os.path.join(tools, 'bin')
    env = dict(os.environ, PATH=bin_directory + os.pathsep + os.environ['PATH'])
    if not os.path.isfile(os.path.join(bin_directory, 'autoconf')):
        sources = os.path.join(work, 'tools-src')
        os.makedirs(sources, exist_ok=True)
        for name, version in (('m4', M4_VERSION), ('autoconf', AUTOCONF_VERSION)):
            archive = os.path.join(sources, f'{name}-{version}.tar.gz')
            run(['curl', '-fsSL', '--retry', '3', '-o', archive, f'{GNU_MIRROR}/{name}/{name}-{version}.tar.gz'])
            run(['tar', '-xzf', archive, '-C', sources])
            source = os.path.join(sources, f'{name}-{version}')
            run(['./configure', '--quiet', f'--prefix={tools}'], cwd=source, env=env)
            run(['make', f'-j{os.cpu_count()}', 'install'], cwd=source, env=env)
    return {'PATH': env['PATH']}


def metal_stub(work):
    """A stand-in for metal and metallib, which the configure of the JDK requires on macOS and which Xcode 26 no longer
    has without the Metal Toolchain component. They only compile the shaders of java.desktop, which make
    compile-commands also runs, so the stub just creates the output file."""
    stub = os.path.join(work, 'tools', 'metal-stub')
    os.makedirs(os.path.dirname(stub), exist_ok=True)
    with open(stub, 'w', encoding='utf-8') as file:
        file.write('#!/bin/sh\n'
                   '# Creates the -o file: the shaders of java.desktop are not used by the libraries for iOS.\n'
                   'while [ $# -gt 0 ]; do\n'
                   '    if [ "$1" = -o ]; then shift; mkdir -p "$(dirname "$1")"; : > "$1"; fi\n'
                   '    shift\n'
                   'done\n')
    os.chmod(stub, 0o755)
    return stub


def mx_version(graal):
    """The mx version required by the suites of graal: the highest of their mxversion."""
    versions = []
    for suite in glob.glob(os.path.join(graal, '*', 'mx.*', 'suite.py')):
        with open(suite, encoding='utf-8') as file:
            match = re.search(r'"mxversion"\s*:\s*"([^"]+)"', file.read())
        if match:
            versions.append(match.group(1))
    if not versions:
        fail(f'no mxversion in the suites of {graal}')
    return max(versions, key=lambda version: [int(part) for part in version.split('.')])


def read_release(java_home):
    values = {}
    with open(os.path.join(java_home, 'release'), encoding='utf-8') as file:
        for line in file:
            key, _, value = line.strip().partition('=')
            values[key] = value.strip('"')
    return values


def jvmci_tag(version):
    """The tag of labs-openjdk for a labs JDK version, as mx computes it for the downloads (mx_fetchjdk.py):
    25.0.4.1.1+1-jvmci-25.4-b23 -> jvmci-25.4-b23, 25.0.1+8-jvmci-b01 -> 25.0.1+8-jvmci-b01."""
    return re.sub(r'(?:.*)-(.*-jvmci-b\d+).*|.*(jvmci-\d+\.\d+-b\d+).*', r'\1\2', 'x-' + version)


def apply_patch(directory, patch):
    """Applies the patch to the clean sources, so a changed patch replaces the previous one."""
    run(['git', 'checkout', '--quiet', '--', '.'], cwd=directory)
    run(['git', 'apply', patch], cwd=directory)


def prepare_graal(directory, graal_tag):
    """Clones graal and mx, fetches the labs JDK of this graal and generates JvmFuncsFallbacks.c."""
    graal = os.path.join(directory, 'graal')
    clone(GRAAL_REPOSITORY, graal_tag, graal)
    mx_tag = mx_version(graal)
    mx_directory = os.path.join(directory, 'mx')
    clone(MX_REPOSITORY, mx_tag, mx_directory)
    mx = os.path.join(mx_directory, 'mx')
    substratevm = os.path.join(graal, 'substratevm')

    jdks = os.path.join(directory, 'jdks')
    labs_jdk = os.path.join(jdks, 'labsjdk')
    if not os.path.isfile(os.path.join(labs_jdk, 'release')):
        shutil.rmtree(jdks, ignore_errors=True)
        run([mx, 'fetch-jdk', '--jdk-id', 'labsjdk-ce-latest', '--to', jdks, '--alias', 'labsjdk',
             '--strip-contents-home'], cwd=substratevm)
    labs_jdk = os.path.realpath(labs_jdk)

    env = dict(os.environ, JAVA_HOME=labs_jdk)
    run([mx, 'build', '--dependencies', 'svm-jvmfuncs-fallback-builder'], cwd=substratevm, env=env)
    fallbacks = glob.glob(os.path.join(substratevm, 'mxbuild', '*', 'svm-jvmfuncs-fallback-builder', 'gensrc',
                                       'JvmFuncsFallbacks.c'))
    if len(fallbacks) != 1:
        fail(f'expected one generated JvmFuncsFallbacks.c, found {fallbacks}')
    return graal, labs_jdk, fallbacks[0]


def prepare_jdk(work, directory, labs_jdk, jobs):
    """Clones the labs JDK sources of the fetched labs JDK, applies the iOS patch and runs the part of the JDK build
    that the compilation needs: the generated headers and compile_commands.json with the flags of every file."""
    release = read_release(labs_jdk)
    tag = jvmci_tag(release['JAVA_RUNTIME_VERSION'])
    jdk = os.path.join(directory, 'labs-openjdk')
    clone(LABS_JDK_REPOSITORY, tag, jdk)

    # SOURCE=".:git:391a5a739cb6+ labsjdk-builder:..." names the commit the labs JDK is built from.
    source = re.search(r'\.:git:([0-9a-f]+)', release.get('SOURCE', ''))
    head = run(['git', 'rev-parse', 'HEAD'], cwd=jdk, capture=True).strip()
    if source and not head.startswith(source.group(1)):
        log(f'warning: the labs JDK is built from {source.group(1)}, the tag {tag} is {head}')

    apply_patch(jdk, os.path.join(HERE, 'jdk-ios.patch'))

    env = dict(os.environ, **ensure_autoconf(work))
    configuration = os.path.join(jdk, 'build', JDK_CONFIGURATION)
    if not os.path.isfile(os.path.join(configuration, 'spec.gmk')):
        stub = metal_stub(work)
        run(['bash', 'configure', f'--with-boot-jdk={labs_jdk}', f'--with-conf-name={JDK_CONFIGURATION}',
             '--with-debug-level=release', '--with-native-debug-symbols=none', '--disable-warnings-as-errors',
             f'METAL={stub}', f'METALLIB={stub}'],
            cwd=jdk, env=env)
    # compile-commands compiles the Java code of the modules with native code, which generates their headers.
    run(['make', f'CONF={JDK_CONFIGURATION}', f'JOBS={jobs}', 'compile-commands', 'java.base-copy'], cwd=jdk, env=env)
    return jdk, tag, head, configuration


def load_compile_commands(jdk, configuration):
    with open(os.path.join(configuration, 'compile_commands.json'), encoding='utf-8') as file:
        entries = json.load(file)
    commands = {}
    root = os.path.realpath(jdk)
    for entry in entries:
        path = os.path.relpath(os.path.realpath(os.path.join(entry['directory'], entry['file'])), root)
        arguments = entry['arguments'] if 'arguments' in entry else shlex.split(entry['command'])
        commands[path] = (entry['directory'], arguments)
    return commands


# The options of the compile commands for macOS that do not apply to iOS, with the number of their arguments.
DROPPED_OPTIONS = {'-c': 0, '-o': 1, '-arch': 1, '-isysroot': 1, '-iframework': 1, '-target': 1, '-MF': 1, '-MT': 1,
                   '-MQ': 1, '-MMD': 0, '-MD': 0, '-Werror': 0}
DROPPED_PREFIXES = ('-mmacosx-version-min=', '-mmacos-version-min=', '--target=', '-DMAC_OS_X_VERSION_')


def jdk_compile_arguments(arguments, source, slice_):
    """The flags of a compile command of the JDK for macOS (arm64), made for the slice."""
    flags = []
    library = None
    index = 1  # the compiler
    while index < len(arguments):
        argument = arguments[index]
        index += 1
        if argument == '-o':
            match = re.search(r'/native/[^/]+/lib([^/]+)/', arguments[index])
            library = match.group(1) if match else None
        if argument in DROPPED_OPTIONS:
            index += DROPPED_OPTIONS[argument]
            continue
        if argument.startswith(DROPPED_PREFIXES) or re.match(r'^-g(\d|dwarf.*|full)?$', argument):
            continue
        # The framework directories of the macOS SDK: the frameworks of the iOS SDK come with its -isysroot.
        if argument == '-F' and '.sdk' in arguments[index]:
            index += 1
            continue
        if argument.startswith('-F') and '.sdk' in argument:
            continue
        if os.path.basename(argument) == os.path.basename(source) and not argument.startswith('-'):
            continue
        flags.append(argument)

    if slice_.arch == 'x86_64':
        # The JDK defines the architecture as -D<legacy cpu> -DARCH='"<legacy cpu>"', and os.arch as ARCHPROPNAME.
        rewritten = []
        for flag in flags:
            if flag.startswith('-mbranch-protection='):
                continue
            if flag == '-Daarch64':
                flag = '-Damd64'
            elif flag.startswith('-DARCH='):
                flag = flag.replace('aarch64', 'amd64')
            elif flag.startswith('-DARCHPROPNAME='):
                flag = flag.replace('aarch64', 'x86_64')
            rewritten.append(flag)
        flags = rewritten
    left = [flag for flag in flags if flag.startswith('-D') and re.search(r'aarch64|arm64', flag)]
    if slice_.arch == 'x86_64' and left:
        fail(f'{source}: flags of arm64 left for x86_64: {left}')

    if library is None:
        match = re.search(r'/native/lib([^/]+)/', source)
        library = match.group(1)
    if not any(flag.startswith('-DLIBRARY_NAME=') for flag in flags):
        # The JDK defines them only for its static libraries: JNI_OnLoad_<library> etc.
        flags.append(f'-DLIBRARY_NAME={library}')
    if '-DSTATIC_BUILD=1' not in flags:
        flags.append('-DSTATIC_BUILD=1')
    if library in LIBRARY_DEPENDENCIES:
        flags += ['-include', linker_options_header(library)]
    return flags


def linker_options_header(library):
    """A header that adds the linker options of the library to every object that includes it: .linker_option of the
    Mach-O assembler makes one LC_LINKER_OPTION of several strings (-framework and its name), which the clang options
    (--dependent-lib, --linker-option) cannot."""
    path = os.path.join(LINKER_OPTIONS_DIRECTORY, f'{library}.h')
    if not os.path.isfile(path):
        os.makedirs(LINKER_OPTIONS_DIRECTORY, exist_ok=True)
        with open(path, 'w', encoding='utf-8') as file:
            file.write(f'// The system libraries of the JDK library {library}, generated by build.py.\n')
            for option in LIBRARY_DEPENDENCIES[library]:
                strings = ', '.join('\\"' + part + '\\"' for part in option)
                file.write(f'__asm__(".linker_option {strings}");\n')
    return path


# Set by build() to <work>/linker-options.
LINKER_OPTIONS_DIRECTORY = None


def compile_all(tasks, jobs):
    """Runs the compile commands [(command, cwd, object)] in parallel, reports all the failures."""
    def compile_one(task):
        command, cwd, output = task
        os.makedirs(os.path.dirname(output), exist_ok=True)
        result = subprocess.run(command, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                universal_newlines=True)
        return task, result

    failures = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=jobs) as executor:
        for task, result in executor.map(compile_one, tasks):
            if result.stdout.strip():
                log(result.stdout.rstrip())
            if result.returncode != 0:
                failures.append(task)
                log('FAILED: ' + ' '.join(shlex.quote(part) for part in task[0]))
    if failures:
        fail(f'{len(failures)} of {len(tasks)} files failed to compile')


def archive(objects, output):
    if os.path.exists(output):
        os.remove(output)
    run(['xcrun', 'libtool', '-static', '-no_warning_for_no_symbols', '-o', output] + sorted(objects))


class Sources:
    """The prepared sources of one GraalVM version, in <work>/<graal tag>."""

    def __init__(self, work, graal_tag, jobs):
        self.graal_tag = graal_tag
        directory = os.path.join(work, graal_tag)
        self.graal, self.labs_jdk, self.fallbacks = prepare_graal(directory, graal_tag)
        self.jdk, self.jdk_tag, self.jdk_commit, configuration = prepare_jdk(work, directory, self.labs_jdk, jobs)
        self.commands = load_compile_commands(self.jdk, configuration)
        self.graal_commit = run(['git', 'rev-parse', 'HEAD'], cwd=self.graal, capture=True).strip()

    def info(self):
        return (f'graal {self.graal_tag} ({self.graal_commit}), labs JDK '
                f'{read_release(self.labs_jdk)["JAVA_RUNTIME_VERSION"]}, labs-openjdk {self.jdk_tag} '
                f'({self.jdk_commit})')


def build_slice_libraries(work, sources, slice_, minimum_version, jobs):
    """Compiles the JDK and the Substrate VM sources for one slice, returns the paths of the two libraries."""
    jdk, commands, graal, labs_jdk, fallbacks = (sources.jdk, sources.commands, sources.graal, sources.labs_jdk,
                                                 sources.fallbacks)
    root = os.path.join(work, 'objects', slice_.name)
    shutil.rmtree(root, ignore_errors=True)
    target = ['-target', slice_.triple(minimum_version), '-isysroot', sdk_path(slice_.sdk)]
    compiler = ['xcrun', '--sdk', slice_.sdk, 'clang']

    jdk_tasks = []
    missing = []
    for source in read_list(os.path.join(HERE, 'jdk-sources.txt')):
        if source not in commands:
            missing.append(source)
            continue
        cwd, arguments = commands[source]
        output = os.path.join(root, 'jdk', source + '.o')
        flags = jdk_compile_arguments(arguments, source, slice_)
        jdk_tasks.append((compiler + target + flags + ['-c', os.path.join(jdk, source), '-o', output], cwd, output))
    if missing:
        fail('no compile command in the JDK build for: ' + ', '.join(missing))

    substratevm = os.path.join(graal, 'substratevm')
    includes = ['-I' + os.path.join(labs_jdk, 'include'), '-I' + os.path.join(labs_jdk, 'include', 'darwin'),
                '-I' + os.path.join(substratevm, 'src', 'com.oracle.svm.native.libchelper', 'include')]
    svm_tasks = []
    for source in read_list(os.path.join(HERE, 'svm-sources.txt')) + [os.path.relpath(fallbacks, substratevm)]:
        project = 'com.oracle.svm.native.jvm.posix' if source.endswith('JvmFuncsFallbacks.c') else source.split('/')[1]
        output = os.path.join(root, 'svm', os.path.basename(source) + '.o')
        command = compiler + target + SVM_PROJECT_FLAGS[project] + includes + [
            '-c', os.path.join(substratevm, source), '-o', output]
        svm_tasks.append((command, substratevm, output))

    compile_all(jdk_tasks + svm_tasks, jobs)
    libraries = {}
    for kind, tasks in (('jdk', jdk_tasks), ('svm', svm_tasks)):
        libraries[kind] = os.path.join(root, f'{kind}.a')
        archive([task[2] for task in tasks], libraries[kind])
    return libraries


def build(arguments):
    check_host()
    work = os.path.abspath(os.path.expanduser(arguments.work))
    out = os.path.abspath(arguments.out)
    os.makedirs(work, exist_ok=True)
    jobs = arguments.jobs or os.cpu_count()
    global LINKER_OPTIONS_DIRECTORY
    LINKER_OPTIONS_DIRECTORY = os.path.join(work, 'linker-options')
    shutil.rmtree(LINKER_OPTIONS_DIRECTORY, ignore_errors=True)

    # All the sources are prepared before the compilation, so a problem with any of them shows up early.
    x64_tag = arguments.x64_graal_tag or arguments.graal_tag
    sources = {}
    for tag in dict.fromkeys([arguments.graal_tag, x64_tag]):
        log(f'== sources of {tag}')
        sources[tag] = Sources(work, tag, jobs)

    def sources_of(slice_):
        return sources[x64_tag if slice_.x64 else arguments.graal_tag]

    slices = {}
    for name, slice_ in SLICES.items():
        log(f'== {name}')
        slices[name] = build_slice_libraries(work, sources_of(slice_), slice_, arguments.ios_min_version, jobs)

    os.makedirs(out, exist_ok=True)
    for file_name, kind, names in LIBRARIES:
        output = os.path.join(out, file_name)
        if os.path.exists(output):
            os.remove(output)
        run(['xcrun', 'lipo', '-create'] + [slices[name][kind] for name in names] + ['-output', output])
        run(['xcrun', 'lipo', '-info', output])

    xcode = run(['xcodebuild', '-version'], capture=True).replace('\n', ' ').strip()
    with open(os.path.join(out, 'build-info.txt'), 'w', encoding='utf-8') as file:
        for name, slice_ in SLICES.items():
            file.write(f'{name}: {sources_of(slice_).info()}\n')
        file.write(f'minimum iOS version: {arguments.ios_min_version}\n'
                   f'{xcode}\n')
    log(f'The libraries are in {out}')


def encode_version(version):
    """X.Y.Z as xxxx.yy.zz nibbles (the minos and sdk fields of LC_BUILD_VERSION)."""
    parts = [int(part) for part in version.split('.')] + [0, 0]
    return (parts[0] << 16) | (parts[1] << 8) | parts[2]


def decode_version(value):
    version = f'{value >> 16}.{(value >> 8) & 0xff}'
    return version + (f'.{value & 0xff}' if value & 0xff else '')


def macho_slices(data):
    """The (architecture, offset) of each thin Mach-O of a fat or thin file."""
    magic = struct.unpack_from('>I', data, 0)[0]
    if magic in (FAT_MAGIC, FAT_MAGIC_64):
        count = struct.unpack_from('>I', data, 4)[0]
        entry, layout = (20, '>iiIII') if magic == FAT_MAGIC else (32, '>iiQQI')
        slices = []
        for index in range(count):
            cpu_type, _, offset = struct.unpack_from(layout, data, 8 + index * entry)[:3]
            slices.append((CPU_TYPES.get(cpu_type, hex(cpu_type)), offset))
        return slices
    if struct.unpack_from('<I', data, 0)[0] != MH_MAGIC_64:
        fail('not a 64-bit Mach-O file')
    return [(CPU_TYPES.get(struct.unpack_from('<i', data, 4)[0], 'unknown'), 0)]


def load_commands(data, offset):
    """The (command, offset in the file, size) of the load commands of the thin Mach-O at the offset, with the lowest
    file offset of the data that the commands refer to (the sections, the symbols, the relocations, etc.)."""
    if struct.unpack_from('<I', data, offset)[0] != MH_MAGIC_64:
        fail(f'no 64-bit Mach-O at {offset}')
    count, size_of_commands = struct.unpack_from('<II', data, offset + 16)
    commands = []
    data_offsets = []
    position = offset + MACH_HEADER_64_SIZE
    for _ in range(count):
        command, size = struct.unpack_from('<II', data, position)
        if command == LC_SEGMENT_64:
            file_offset, file_size = struct.unpack_from('<QQ', data, position + 40)
            if file_size:
                data_offsets.append(file_offset)
            for section in range(struct.unpack_from('<I', data, position + 64)[0]):
                section_offset, _, relocations_offset, relocations = struct.unpack_from(
                    '<IIII', data, position + 72 + section * 80 + 48)
                section_size = struct.unpack_from('<Q', data, position + 72 + section * 80 + 40)[0]
                if section_offset and section_size:
                    data_offsets.append(section_offset)
                if relocations:
                    data_offsets.append(relocations_offset)
        elif command == LC_SYMTAB:
            symbols_offset, symbols, strings_offset, strings_size = struct.unpack_from('<IIII', data, position + 8)
            data_offsets += [value for value, used in ((symbols_offset, symbols), (strings_offset, strings_size))
                             if used]
        elif command == LC_DYSYMTAB:
            fields = struct.unpack_from('<18I', data, position + 8)
            # tocoff, modtaboff, extrefsymoff, indirectsymoff, extreloff, locreloff with their counts.
            data_offsets += [fields[index] for index in (6, 8, 10, 12, 14, 16) if fields[index + 1]]
        elif command in LINKEDIT_DATA_COMMANDS:
            data_offset, data_size = struct.unpack_from('<II', data, position + 8)
            if data_size:
                data_offsets.append(data_offset)
        elif command not in COMMANDS_WITHOUT_DATA:
            # The free space after the load commands cannot be found without knowing what the command refers to.
            fail(f'unsupported load command {command:#x} at {position}')
        commands.append((command, position, size))
        position += size
    if position != offset + MACH_HEADER_64_SIZE + size_of_commands:
        fail(f'the load commands at {offset} do not match sizeofcmds')
    return commands, offset + min(data_offsets) if data_offsets else len(data)


def command_positions(data, offset):
    """The (command, offset in the file, size) of the load commands of the thin Mach-O at the offset."""
    if struct.unpack_from('<I', data, offset)[0] != MH_MAGIC_64:
        fail(f'no 64-bit Mach-O at {offset}')
    count = struct.unpack_from('<I', data, offset + 16)[0]
    position = offset + MACH_HEADER_64_SIZE
    commands = []
    for _ in range(count):
        command, size = struct.unpack_from('<II', data, position)
        commands.append((command, position, size))
        position += size
    return commands


def build_versions(data, offset):
    """The (platform, minimum version, SDK version) of LC_BUILD_VERSION of the thin Mach-O at the offset."""
    versions = []
    for command, position, _ in command_positions(data, offset):
        if command == LC_BUILD_VERSION:
            platform_id, minimum, sdk = struct.unpack_from('<III', data, position + 8)
            versions.append((PLATFORMS.get(platform_id, str(platform_id)), decode_version(minimum),
                             decode_version(sdk)))
        elif command in VERSION_MIN_COMMANDS:
            versions.append((f'version-min {command:#x}', '', ''))
    return versions


def expected_build_version(platform_name, arch, ios_min_version):
    """The platform and the minimum version that an object of the platform (ios or simulator) declares on the
    architecture, as clang declares them for the libraries."""
    if platform_name == 'ios':
        return 'ios', ios_min_version
    if arch == 'arm64' and tuple(map(int, ios_min_version.split('.'))) < (14, 0):
        return 'iossimulator', SIMULATOR_ARM64_MIN_VERSION
    return 'iossimulator', ios_min_version


def sdk_version(platform_name):
    sdk = 'iphoneos' if platform_name == 'ios' else 'iphonesimulator'
    return run(['xcrun', '--sdk', sdk, '--show-sdk-version'], capture=True).strip()


def set_build_version(arguments):
    """Declares the platform of every architecture of the native image (LC_BUILD_VERSION), as clang does for an object:
    native-image writes the object of the iOS image itself and declares none, and the arm64 object of the simulator is
    the object of the device (GraalVM has no simulator platform), so the linker cannot check the platform otherwise.
    The command is written into the free space after the load commands (the data of the sections starts at a page
    boundary), so no offset changes and the size of the file stays the same; without free space the command fails."""
    with open(arguments.file, 'rb') as file:
        data = bytearray(file.read())
    size = len(data)
    sdk = arguments.sdk_version or sdk_version(arguments.platform)
    for arch, offset in macho_slices(data):
        platform_name, minimum = expected_build_version(arguments.platform, arch, arguments.ios_min_version)
        command_data = struct.pack('<IIIIII', LC_BUILD_VERSION, BUILD_VERSION_COMMAND_SIZE,
                                   PLATFORM_IDS[platform_name], encode_version(minimum), encode_version(sdk), 0)
        commands, data_start = load_commands(data, offset)
        existing = [(position, command_size) for command, position, command_size in commands
                    if command in VERSION_MIN_COMMANDS or command == LC_BUILD_VERSION]
        if existing:
            position, command_size = existing[0]
            if len(existing) > 1 or command_size != BUILD_VERSION_COMMAND_SIZE or \
                    struct.unpack_from('<I', data, position)[0] != LC_BUILD_VERSION:
                fail(f'{arch}: the object already declares its platform in another way: '
                     f'{build_versions(data, offset)}')
            data[position:position + BUILD_VERSION_COMMAND_SIZE] = command_data
        else:
            count, size_of_commands = struct.unpack_from('<II', data, offset + 16)
            end = offset + MACH_HEADER_64_SIZE + size_of_commands
            if end + BUILD_VERSION_COMMAND_SIZE > data_start or \
                    any(data[end:end + BUILD_VERSION_COMMAND_SIZE]):
                fail(f'{arch}: no free space after the load commands for LC_BUILD_VERSION')
            data[end:end + BUILD_VERSION_COMMAND_SIZE] = command_data
            struct.pack_into('<II', data, offset + 16, count + 1, size_of_commands + BUILD_VERSION_COMMAND_SIZE)
        log(f'{arguments.file} {arch}: {build_versions(data, offset)}')
    if len(data) != size:
        fail('the size of the file changed')
    with open(arguments.file, 'wb') as file:
        file.write(data)


def check_build_versions(image, platform_name, ios_min_version):
    """The image declares the platform and the minimum version of each architecture (see set_build_version)."""
    with open(image, 'rb') as file:
        data = file.read()
    errors = []
    for arch, offset in macho_slices(data):
        expected = expected_build_version(platform_name, arch, ios_min_version)
        versions = build_versions(data, offset)
        log(f'{os.path.basename(image)} {arch}: {versions or "no platform"}')
        if [version[:2] for version in versions] != [expected]:
            errors.append(f'{arch} declares {versions or "no platform"}, expected {expected}')
    return errors


def check(arguments):
    """Links a dynamic library from the native image and the libraries of an unpacked SDK archive for iOS or for the
    simulator, for every architecture of the image, as an application does (CHECK_LINK_FLAGS): any symbol that neither
    the libraries nor the system libraries that they name define fails the link. The image must declare the platform
    of each architecture (LC_BUILD_VERSION, see set-build-version), so the linker also checks the platform."""
    check_host()
    directory = os.path.abspath(arguments.dir)
    image = os.path.join(directory, 'libDxFeedGraalNativeSdk.o')
    libraries = sorted(glob.glob(os.path.join(directory, '*.a')))
    if not os.path.isfile(image) or not libraries:
        fail(f'{directory} must contain libDxFeedGraalNativeSdk.o and the .a libraries')
    sdk = 'iphoneos' if arguments.platform == 'ios' else 'iphonesimulator'
    architectures = run(['xcrun', 'lipo', '-archs', image], capture=True).split()
    root = sdk_path(sdk)
    failed = check_build_versions(image, arguments.platform, arguments.ios_min_version)
    for arch in architectures:
        slice_ = next(s for s in SLICES.values() if s.sdk == sdk and s.arch == arch)
        output = os.path.join(directory, f'link-check-{arch}.dylib')
        command = ['xcrun', '--sdk', sdk, 'clang', '-target', slice_.triple(arguments.ios_min_version), '-isysroot',
                   root, '-dynamiclib', '-o', output, image] + libraries + CHECK_LINK_FLAGS
        log(f'== {arguments.platform} {arch}')
        result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True)
        log(result.stdout.rstrip() or 'linked')
        if result.returncode != 0:
            failed.append(f'{arch} does not link')
        if os.path.exists(output):
            os.remove(output)
        # The manifest: an application that links with its flags without autolinking.
        command = ['xcrun', '--sdk', sdk, 'clang', '-target', slice_.triple(arguments.ios_min_version), '-isysroot',
                   root, '-dynamiclib', '-o', output] + read_manifest(directory) + ['-Wl,-ignore_auto_link']
        log(f'== {arguments.platform} {arch}: {MANIFEST}, without autolinking')
        result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True)
        log(result.stdout.rstrip() or 'linked')
        if result.returncode != 0:
            failed.append(f'{arch} does not link with the flags of {MANIFEST}')
        if os.path.exists(output):
            os.remove(output)
    if failed:
        fail(f'the check of the image for {arguments.platform} failed: {"; ".join(failed)}')


def linker_options(path):
    """The options that the objects of a file (an object or a library, thin or fat) name for the linker
    (LC_LINKER_OPTION), each as a list of strings ('-lz', or '-framework' and 'CFNetwork'), in their order."""
    options = []
    current = None
    for line in run(['xcrun', 'otool', '-arch', 'all', '-l', path], capture=True).splitlines() + ['cmd']:
        line = line.strip()
        if line.startswith('cmd ') or line == 'cmd' or line.startswith('Load command'):
            if current and current not in options:
                options.append(current)
            current = [] if line == 'cmd LC_LINKER_OPTION' else None
        elif current is not None and line.startswith('string #'):
            current.append(line.split(' ', 2)[2])
    return options


def file_build_versions(path):
    """The (architecture, [(platform, minimum version, SDK version)]) of each architecture of a Mach-O file."""
    with open(path, 'rb') as file:
        data = file.read()
    return [(arch, build_versions(data, offset)) for arch, offset in macho_slices(data)]


def read_manifest(directory):
    """The linker flags of the manifest of an archive, the files with their paths."""
    path = os.path.join(directory, MANIFEST)
    if not os.path.isfile(path):
        fail(f'no {MANIFEST} in {directory}')
    with open(path, encoding='utf-8') as file:
        lines = [line.strip() for line in file if line.strip() and not line.startswith('#')]
    if len(lines) != 1:
        fail(f'{path} must have one line of flags')
    return [os.path.join(directory, flag) if os.path.isfile(os.path.join(directory, flag)) else flag
            for flag in lines[0].split()]


def manifest(arguments):
    """Writes the manifest of the linker flags (link-flags.txt) of the files of an archive: the files and the system
    libraries that the application links with, made from the files themselves (the libraries that the objects name in
    LC_LINKER_OPTION, the platforms and minimum versions of LC_BUILD_VERSION), so it cannot get out of date; check links
    with these flags only."""
    directory = os.path.abspath(arguments.dir)
    lines = [f'# graal-native-sdk {arguments.version}: {PLATFORM_TITLES[arguments.platform]}']
    if arguments.platform == 'macos':
        library = os.path.join(directory, IMAGE_DYLIB)
        if not os.path.isfile(library):
            fail(f'no {IMAGE_DYLIB} in {directory}')
        flags = [IMAGE_DYLIB]
        image = library
    else:
        image = os.path.join(directory, IMAGE_OBJECT)
        libraries = sorted(glob.glob(os.path.join(directory, '*.a')))
        if not os.path.isfile(image) or not libraries:
            fail(f'{directory} must contain {IMAGE_OBJECT} and the .a libraries')
        system = []
        for path in [image] + libraries:
            for option in linker_options(path):
                if option not in system:
                    system.append(option)
        flags = [IMAGE_OBJECT] + [os.path.basename(library) for library in libraries] + CHECK_LINK_FLAGS + \
            [part for option in system for part in option]
    for arch, versions in file_build_versions(image):
        declared = ', '.join(f'{name} {minimum}' for name, minimum, _ in versions) or 'no platform'
        lines.append(f'# {arch}: {declared}')
    lines += ['#',
              '# The linker flags of an application (the last line); the files are relative to this directory.',
              '# Made from the files of the archive by jre-ios/build.py manifest.']
    if arguments.platform == 'macos':
        install_name = run(['xcrun', 'otool', '-D', image], capture=True).splitlines()[-1].strip()
        loaded = [line.strip().split(' (')[0] for line in
                  run(['xcrun', 'otool', '-L', image], capture=True).splitlines()[1:]]
        loaded = [path for path in dict.fromkeys(loaded) if path and path != install_name]
        lines += [f'# The install name of the library is {install_name}: add the directory of the library to the run',
                  '# path of the application (-Wl,-rpath,<directory>), or put the library next to it',
                  '# (-Wl,-rpath,@executable_path).',
                  *textwrap.wrap('The library loads: ' + ', '.join(loaded) + '.', width=118, initial_indent='# ',
                                 subsequent_indent='#   ', break_long_words=False, break_on_hyphens=False)]
    else:
        lines += ['# The SDK build links the files with these flags only (without autolinking) before publishing.',
                  '# The objects also name their system libraries for the linker (LC_LINKER_OPTION), so with',
                  '# autolinking (the default of clang and Xcode) the files and -framework Foundation are enough.']
    lines.append(' '.join(flags))
    path = os.path.join(directory, MANIFEST)
    with open(path, 'w', encoding='utf-8') as file:
        file.write('\n'.join(lines) + '\n')
    log(f'{path}:\n' + '\n'.join(lines))


def info_plist(path, platform_name, version, minimum):
    """The Info.plist of a framework of the XCFramework."""
    numeric = re.match(r'\d+(\.\d+)*', version).group(0)
    info = {
        'CFBundleDevelopmentRegion': 'en',
        'CFBundleExecutable': FRAMEWORK,
        'CFBundleIdentifier': FRAMEWORK_IDENTIFIER,
        'CFBundleInfoDictionaryVersion': '6.0',
        'CFBundleName': FRAMEWORK,
        'CFBundlePackageType': 'FMWK',
        'CFBundleShortVersionString': numeric,
        'CFBundleVersion': numeric,
        'CFBundleSupportedPlatforms': [{'ios': 'iPhoneOS', 'simulator': 'iPhoneSimulator',
                                        'macos': 'MacOSX'}[platform_name]],
    }
    info['LSMinimumSystemVersion' if platform_name == 'macos' else 'MinimumOSVersion'] = minimum
    with open(path, 'wb') as file:
        plistlib.dump(info, file)


def framework_contents(directory, headers):
    """The headers and the module map (import DxFeedGraalNativeSdk) of a framework."""
    os.makedirs(os.path.join(directory, 'Headers'))
    os.makedirs(os.path.join(directory, 'Modules'))
    for header in headers:
        shutil.copy2(header, os.path.join(directory, 'Headers'))
    with open(os.path.join(directory, 'Modules', 'module.modulemap'), 'w', encoding='utf-8') as file:
        file.write(f'framework module {FRAMEWORK} {{\n'
                   '    umbrella header "dxfg_api.h"\n'
                   '    export *\n'
                   '    module * { export * }\n'
                   # The objects name their other system libraries themselves (LC_LINKER_OPTION).
                   '    link framework "Foundation"\n'
                   '}\n')


def xcframework(arguments):
    """Makes DxFeedGraalNativeSdk.xcframework from the files of the archives for iOS, the simulator and macOS: a static
    framework for iOS and for the simulator (the image object and the libraries in one static library, which keeps
    their LC_BUILD_VERSION and LC_LINKER_OPTION) and a dynamic framework for macOS (the library of the SDK for arm64 and
    x86_64), each with the headers and a Clang module."""
    check_host()
    work = os.path.abspath(arguments.work)
    shutil.rmtree(work, ignore_errors=True)
    headers = sorted(glob.glob(os.path.join(arguments.ios, '*.h')))
    if not headers:
        fail(f'no headers in {arguments.ios}')
    frameworks = []
    for platform_name, directory in (('ios', arguments.ios), ('simulator', arguments.simulator)):
        image = os.path.join(directory, IMAGE_OBJECT)
        errors = check_build_versions(image, platform_name, arguments.ios_min_version)
        if errors:
            fail(f'{image}: {"; ".join(errors)} (see set-build-version)')
        framework = os.path.join(work, platform_name, FRAMEWORK + '.framework')
        framework_contents(framework, headers)
        run(['xcrun', 'libtool', '-static', '-no_warning_for_no_symbols', '-o', os.path.join(framework, FRAMEWORK),
             image] + sorted(glob.glob(os.path.join(directory, '*.a'))))
        info_plist(os.path.join(framework, 'Info.plist'), platform_name, arguments.version, arguments.ios_min_version)
        frameworks.append(framework)

    # macOS frameworks have versions: Versions/A with the content and the links to Versions/Current.
    framework = os.path.join(work, 'macos', FRAMEWORK + '.framework')
    version_directory = os.path.join(framework, 'Versions', 'A')
    framework_contents(version_directory, headers)
    os.makedirs(os.path.join(version_directory, 'Resources'))
    binary = os.path.join(version_directory, FRAMEWORK)
    run(['xcrun', 'lipo', '-create'] + [os.path.join(directory, IMAGE_DYLIB) for directory in arguments.macos] +
        ['-output', binary])
    run(['xcrun', 'install_name_tool', '-id', f'@rpath/{FRAMEWORK}.framework/Versions/A/{FRAMEWORK}', binary])
    # The new install name invalidates the ad hoc signature, without which macOS on arm64 does not load the library.
    run(['codesign', '--force', '--sign', '-', binary])
    info_plist(os.path.join(version_directory, 'Resources', 'Info.plist'), 'macos', arguments.version,
               MACOS_MIN_VERSION)
    os.symlink('A', os.path.join(framework, 'Versions', 'Current'))
    for name in (FRAMEWORK, 'Headers', 'Modules', 'Resources'):
        os.symlink(os.path.join('Versions', 'Current', name), os.path.join(framework, name))
    frameworks.append(framework)

    output = os.path.join(work, FRAMEWORK + '.xcframework')
    command = ['xcodebuild', '-create-xcframework']
    for framework in frameworks:
        command += ['-framework', framework]
    run(command + ['-output', output])
    if arguments.zip:
        zip_path = os.path.abspath(arguments.zip)
        if os.path.exists(zip_path):
            os.remove(zip_path)
        # ditto keeps the links of the macOS framework.
        run(['ditto', '-c', '-k', '--sequesterRsrc', '--keepParent', output, zip_path])
        log(f'{zip_path}: {os.path.getsize(zip_path)} bytes')
    log(f'The XCFramework is {output}')


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest='command', required=True)

    build_parser = commands.add_parser('build', help='build the libraries')
    build_parser.add_argument('--graal-tag', required=True,
                              help='the tag of oracle/graal of the GraalVM Community used for the native image, '
                                   'for example graal-25.4.4.1.1 or jdk-25.0.1')
    build_parser.add_argument('--x64-graal-tag',
                              help='the tag of the GraalVM used for the x86_64 image of the simulator '
                                   '(default: --graal-tag)')
    build_parser.add_argument('--work', required=True,
                              help='the directory of the build, with the sources of each GraalVM in <work>/<tag>')
    build_parser.add_argument('--out', required=True, help='the directory of the built libraries')
    build_parser.add_argument('--ios-min-version', default='12.0', help='the minimum iOS version (default 12.0)')
    build_parser.add_argument('--jobs', type=int, help='the number of parallel jobs (default: the number of CPUs)')
    build_parser.set_defaults(function=build)

    check_parser = commands.add_parser('check', help='check that a native image links with the libraries')
    check_parser.add_argument('--dir', required=True,
                              help='the directory with libDxFeedGraalNativeSdk.o and the .a libraries')
    check_parser.add_argument('--platform', required=True, choices=['ios', 'simulator'])
    check_parser.add_argument('--ios-min-version', default='12.0', help='the minimum iOS version (default 12.0)')
    check_parser.set_defaults(function=check)

    version_parser = commands.add_parser('set-build-version',
                                         help='declare the platform of each architecture of a native image')
    version_parser.add_argument('--file', required=True,
                                help='the native image (libDxFeedGraalNativeSdk.o), thin or fat')
    version_parser.add_argument('--platform', required=True, choices=['ios', 'simulator'])
    version_parser.add_argument('--ios-min-version', default='12.0', help='the minimum iOS version (default 12.0)')
    version_parser.add_argument('--sdk-version',
                                help='the SDK version to declare (default: the version of the SDK of Xcode)')
    version_parser.set_defaults(function=set_build_version)

    manifest_parser = commands.add_parser('manifest', help=f'write the manifest of the linker flags ({MANIFEST})')
    manifest_parser.add_argument('--dir', required=True, help='the directory with the files of an archive')
    manifest_parser.add_argument('--platform', required=True, choices=['ios', 'simulator', 'macos'])
    manifest_parser.add_argument('--version', required=True, help='the version of the SDK')
    manifest_parser.set_defaults(function=manifest)

    xcframework_parser = commands.add_parser('xcframework', help=f'make {FRAMEWORK}.xcframework')
    xcframework_parser.add_argument('--ios', required=True, help='the files of the archive for iOS')
    xcframework_parser.add_argument('--simulator', required=True, help='the files of the archive for the simulator')
    xcframework_parser.add_argument('--macos', required=True, nargs='+',
                                    help='the files of the archives for macOS (arm64 and x86_64)')
    xcframework_parser.add_argument('--version', required=True, help='the version of the SDK')
    xcframework_parser.add_argument('--work', required=True, help='the directory of the XCFramework (recreated)')
    xcframework_parser.add_argument('--zip', help='the zip of the XCFramework to make')
    xcframework_parser.add_argument('--ios-min-version', default='12.0', help='the minimum iOS version (default 12.0)')
    xcframework_parser.set_defaults(function=xcframework)

    arguments = parser.parse_args()
    arguments.function(arguments)


if __name__ == '__main__':
    main()
