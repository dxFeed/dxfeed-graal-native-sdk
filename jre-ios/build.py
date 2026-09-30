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

Needs macOS on arm64 with Xcode, git, curl, make and python3; autoconf (for the configure of the JDK) is installed
into the work directory when it is missing.
"""
import argparse
import concurrent.futures
import glob
import json
import os
import platform
import re
import shlex
import shutil
import subprocess
import sys

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

# The system libraries the native image and the JDK code may need, as an application that uses the SDK links them
# (the frameworks that the SDK does not have are skipped).
LINK_FRAMEWORKS = ['Foundation', 'CoreFoundation', 'CoreServices', 'SystemConfiguration', 'CFNetwork']
LINK_LIBRARIES = ['-lz']


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
    return flags


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


def check(arguments):
    """Links a dynamic library from the native image and the libraries of an unpacked SDK archive for iOS or for the
    simulator, for every architecture of the image: any symbol that the libraries do not define fails the link."""
    check_host()
    directory = os.path.abspath(arguments.dir)
    image = os.path.join(directory, 'libDxFeedGraalNativeSdk.o')
    libraries = sorted(glob.glob(os.path.join(directory, '*.a')))
    if not os.path.isfile(image) or not libraries:
        fail(f'{directory} must contain libDxFeedGraalNativeSdk.o and the .a libraries')
    sdk = 'iphoneos' if arguments.platform == 'ios' else 'iphonesimulator'
    architectures = run(['xcrun', 'lipo', '-archs', image], capture=True).split()
    root = sdk_path(sdk)
    frameworks = []
    for framework in LINK_FRAMEWORKS:
        if os.path.isdir(os.path.join(root, 'System', 'Library', 'Frameworks', framework + '.framework')):
            frameworks += ['-framework', framework]
    failed = []
    for arch in architectures:
        slice_ = next(s for s in SLICES.values() if s.sdk == sdk and s.arch == arch)
        output = os.path.join(directory, f'link-check-{arch}.dylib')
        command = ['xcrun', '--sdk', sdk, 'clang', '-target', slice_.triple(arguments.ios_min_version), '-isysroot',
                   root, '-dynamiclib', '-o', output, image] + libraries + frameworks + LINK_LIBRARIES
        log(f'== {arguments.platform} {arch}')
        result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True)
        log(result.stdout.rstrip() or 'linked')
        if result.returncode != 0:
            failed.append(arch)
        if os.path.exists(output):
            os.remove(output)
    if failed:
        fail(f'the image does not link with the libraries for {arguments.platform}: {", ".join(failed)}')


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

    arguments = parser.parse_args()
    arguments.function(arguments)


if __name__ == '__main__':
    main()
