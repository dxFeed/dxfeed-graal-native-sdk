import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.*
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2026.1"

project {
    vcsRoot(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)

    params {
        param("env.GRAALVM_VERSION", "graal-25.4.4.1.1")
        param("env.GRAALVM_VERSION_MACOS_X64", "jdk-25.0.1")
        password("env.NUGETORG_API_KEY", "credentialsJSON:4ba447c3-64f4-4a4c-8ff8-505258ddd420", display = ParameterDisplay.HIDDEN)
        password("env.GH_TOKEN", "credentialsJSON:657ea93a-c18d-414f-be39-e12fb36fb13f", display = ParameterDisplay.HIDDEN)
    }

    features {
        dockerRegistry {
            id = "NEXUS"
            name = "Nexus"
            url = "https://nexus-docker-graalvm.in.devexperts.com"
            userName = "%dxcity.namecode.nexus%"
            password = "%dxcity.passcode.nexus%"
        }
    }

    buildType(BuildPatchAndDeployForLinux)
    buildType(BuildMajorMinorPatchAndDeployLinux)
    buildType(BuildAndDeployForLinuxAarch64Release)
    buildType(BuildAndDeployForLinuxAarch64Debug)
    buildType(BuildAndDeployForLinuxAarch64)
    buildType(BuildAndDeployForWindowsRelease)
    buildType(BuildAndDeployForWindowsDebug)
    buildType(BuildAndDeployForWindows)
    buildType(BuildAndDeployForMacOsAndIOS)
    buildType(BuildAndDeployXCFramework)
    buildType(BuildAndDeployForAll)
    buildType(DeployNuget)
    buildType(SyncGitHubWithMainAndPublishRelease)
    buildType(BuildForLinux)
    buildType(TestForLinux)
    buildType(BuildForWindows)
    buildType(BuildForWindowsInDocker)
    buildType(BuildForMacOSAndIOS)
    buildType(BuildJreLibrariesForIOS)
    buildType(BuildForLinuxAarch64)
    buildType(BuildAndPushDockerImageForLinuxX64)
    buildType(BuildAndPushTestDockerImageForLinuxX64)
    buildType(BuildAndPushDockerImageForLinuxAarch64)
    buildType(BuildAndPushDockerImageForWindowsX64)

    buildType(CopyServiceImages)
    buildType(ListServiceImages)

    buildType(DetectVisualStudioVersion)
}

object BuildPatchAndDeployForLinux : BuildType({
    name = "Build PATCH & Deploy [Linux, x64]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        linuxReleaseSteps(
                tagCommand = """
                VERSION=${'$'}(mvn help:evaluate \
                    -Dexpression=project.version \
                    -q \
                    -DforceStdout)
                VERSION=${'$'}{VERSION%-SNAPSHOT}
                echo "v${'$'}VERSION"
            """.trimIndent(),
                prepareArguments = ""
        )
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
})

object BuildMajorMinorPatchAndDeployLinux : BuildType({
    name = "Build MAJOR.MINOR.PATCH & Deploy [Linux, x64]"
    artifactRules = "*.zip"

    params {
        text("env.RELEASE_VERSION", "", allowEmpty = false)
    }

    mainRepository()

    steps {
        linuxReleaseSteps(
                tagCommand = "echo v%env.RELEASE_VERSION%",
                prepareArguments = "--batch-mode -DreleaseVersion=%env.RELEASE_VERSION% "
        )
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
})

object BuildAndDeployForLinuxAarch64Release : BuildType({
    name = "Build & Deploy [Linux, aarch64][Release]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        checkoutLatestTag()

        script {
            name = "Deploy"
            scriptContent = """
                mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} ${Mvn.BITBUCKET} clean deploy
            """.trimIndent()
            formatStderrAsError = true
            inDocker(Images.GRAALVM_LINUX_AARCH64, "--rm")
        }
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        macAgentForLinuxAarch64()
    }
})

object BuildAndDeployForLinuxAarch64Debug : BuildType({
    name = "Build & Deploy [Linux, aarch64][Debug]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        checkoutLatestTag()

        script {
            name = "Deploy Debug"
            scriptContent = """
                mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} ${Mvn.BITBUCKET} clean deploy -P buildDebug
            """.trimIndent()
            formatStderrAsError = true
            inDocker(Images.GRAALVM_LINUX_AARCH64, "--rm")
        }
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        macAgentForLinuxAarch64()
    }
})

object BuildAndDeployForLinuxAarch64 : BuildType({
    name = "Build & Deploy [Linux, aarch64]"
    artifactRules = "*.zip"
    type = Type.COMPOSITE

    dependencies {
        snapshot(BuildAndDeployForLinuxAarch64Release) {
            onDependencyFailure = FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForLinuxAarch64Debug) {
            onDependencyFailure = FailureAction.IGNORE
        }
    }
})

object BuildAndDeployForWindowsRelease : BuildType({
    name = "Build & Deploy [Windows, x64][Release]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        checkoutLatestTagWithPowerShell()
        mavenWithPowerShell("Deploy", "${Mvn.POWERSHELL_NEXUS} ${Mvn.POWERSHELL_REPO} ${Mvn.POWERSHELL_BITBUCKET} clean deploy")
    }

    requirements {
        windowsAgent()
    }
})

object BuildAndDeployForWindowsDebug : BuildType({
    name = "Build & Deploy [Windows, x64][Debug]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        checkoutLatestTagWithPowerShell()
        mavenWithPowerShell("Deploy", "${Mvn.POWERSHELL_NEXUS} ${Mvn.POWERSHELL_REPO} ${Mvn.POWERSHELL_BITBUCKET} clean deploy -P buildDebug")
    }

    requirements {
        windowsAgent()
    }
})

object BuildAndDeployForWindows : BuildType({
    name = "Build & Deploy [Windows]"
    type = Type.COMPOSITE
    artifactRules = "*.zip"

    dependencies {
        snapshot(BuildAndDeployForWindowsRelease) {
            onDependencyFailure = FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForWindowsDebug) {
            onDependencyFailure = FailureAction.IGNORE
        }
    }
})

object BuildAndDeployForMacOsAndIOS : BuildType({
    name = "Build & Deploy [macOS, iOS]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        checkoutLatestTag()

        script {
            name = "Deploy"
            scriptContent = Util.prepareMacOS() + """
                export JAVA_HOME=${'$'}{graalvm_arm64_path}/Contents/Home
                arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} clean deploy
                arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} -DmacIos=true clean deploy
                export JAVA_HOME=${'$'}{graalvm_x64_path}/Contents/Home
                arch -x86_64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} -DmacIosSimulator=true deploy
                arch -x86_64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} clean deploy
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    requirements {
        macAgent()
    }
})

object BuildAndDeployXCFramework : BuildType({
    name = "Build & Deploy [XCFramework]"
    description = "Makes DxFeedGraalNativeSdk.xcframework from the iOS, iOS Simulator and macOS archives of the release in Nexus and deploys it next to them (graal-native-sdk-<version>-xcframework.zip)"
    artifactRules = "out/xcframework/graal-native-sdk-*-xcframework.zip"

    mainRepository()

    steps {
        checkoutLatestTag()

        script {
            name = "Make the XCFramework"
            scriptContent = """
                set -eu
                TAG=${'$'}(git describe --tags --abbrev=0)
                VERSION=${'$'}{TAG#v}
                BASE_URL="https://maven.in.devexperts.com/repository/qd/com/dxfeed/graal-native-sdk/${'$'}{VERSION}"
                rm -rf out/xcframework
                mkdir -p out/xcframework
                # The archives that Build & Deploy [macOS, iOS] has just deployed.
                for classifier in aarch64-ios ios-simulator aarch64-osx x86_64-osx; do
                    curl --fail --location --silent --show-error --output "out/xcframework/${'$'}{classifier}.zip" \
                        "${'$'}{BASE_URL}/graal-native-sdk-${'$'}{VERSION}-${'$'}{classifier}.zip"
                    unzip -q "out/xcframework/${'$'}{classifier}.zip" -d "out/xcframework/${'$'}{classifier}"
                done
                arch -arm64 /usr/bin/python3 jre-ios/build.py xcframework \
                    --ios out/xcframework/aarch64-ios --simulator out/xcframework/ios-simulator \
                    --macos out/xcframework/aarch64-osx out/xcframework/x86_64-osx --version "${'$'}{VERSION}" \
                    --work out/xcframework/work --zip "out/xcframework/graal-native-sdk-${'$'}{VERSION}-xcframework.zip"
            """.trimIndent()
        }

        script {
            name = "Deploy"
            scriptContent = "set -e\n" + Util.prepareMacOS() + """
                TAG=${'$'}(git describe --tags --abbrev=0)
                VERSION=${'$'}{TAG#v}
                export JAVA_HOME=${'$'}{graalvm_arm64_path}/Contents/Home
                # The repository of distributionManagement of the pom (qd), with the credentials of .teamcity/settings.xml;
                # Publish GitHub Release takes all the zips of the version from there.
                arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} org.apache.maven.plugins:maven-deploy-plugin:3.2.0:deploy-file -DrepositoryId=qd -Durl=https://maven.in.devexperts.com/content/repositories/qd/ -Dfile="out/xcframework/graal-native-sdk-${'$'}{VERSION}-xcframework.zip" -DgroupId=com.dxfeed -DartifactId=graal-native-sdk -Dversion="${'$'}{VERSION}" -Dclassifier=xcframework -Dpackaging=zip -DgeneratePom=false
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    dependencies {
        snapshot(BuildAndDeployForMacOsAndIOS) {
            onDependencyFailure = FailureAction.CANCEL
        }
    }

    requirements {
        macAgent()
    }
})

object BuildAndDeployForAll : BuildType({
    name = "Build & Deploy [All]"
    type = Type.COMPOSITE
    allowExternalStatus = true
    artifactRules = "*.zip"

    triggers {
        finishBuildTrigger {
            buildType = "${BuildPatchAndDeployForLinux.id}"
            successfulOnly = true
        }
        finishBuildTrigger {
            buildType = "${BuildMajorMinorPatchAndDeployLinux.id}"
            successfulOnly = true
        }
    }

    dependencies {
        snapshot(BuildAndDeployForLinuxAarch64) {
            onDependencyFailure = FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForWindows) {
            onDependencyFailure = FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForMacOsAndIOS) {
            onDependencyFailure = FailureAction.CANCEL
        }
        snapshot(BuildAndDeployXCFramework) {
            onDependencyFailure = FailureAction.CANCEL
        }
    }
})

object DeployNuget : BuildType({
    name = "Deploy NuGet"
    artifactRules = "*.nupkg"

    mainRepository()

    steps {
        script {
            name = "Download Artifacts"
            scriptContent = """
                download_file() {
                  version=${'$'}1
                  path_to_save=${'$'}2
                  file_name=${'$'}3
                  os=${'$'}4
                  platform=${'$'}5
                  extension="zip"

                  base_url="https://maven.in.devexperts.com/repository/qd/com/dxfeed/graal-native-sdk"
                  archive_name="graal-native-sdk-${'$'}{version}-${'$'}{platform}-${'$'}{os}.${'$'}{extension}"
                  url="${'$'}{base_url}/${'$'}{version}/${'$'}{archive_name}"

                  mkdir -p "${'$'}path_to_save"
                  tmp_dir=${'$'}(mktemp -d)

                  if ! (cd "${'$'}tmp_dir" && curl -LO -f "${'$'}url"); then
                    echo "Failed to download: ${'$'}url"
                    rm -rf "${'$'}tmp_dir"
                    exit 1
                  fi

                  if ! unzip -o "${'$'}{tmp_dir}/${'$'}{archive_name}" "${'$'}file_name" -d "${'$'}path_to_save"; then
                    echo "Failed to extract ${'$'}file_name from ${'$'}{archive_name}"
                    rm -rf "${'$'}tmp_dir"
                    exit 1
                  fi

                  rm -rf "${'$'}tmp_dir"
                }

                version=${'$'}(git describe --abbrev=0)
                version=${'$'}{version#"v"}

                download_file "${'$'}version" "NuGet/runtimes/linux-x64/native" "libDxFeedGraalNativeSdk.so" "linux" "amd64"
                download_file "${'$'}version" "NuGet/runtimes/linux-arm64/native" "libDxFeedGraalNativeSdk.so" "linux" "aarch64"
                download_file "${'$'}version" "NuGet/runtimes/osx-arm64/native" "libDxFeedGraalNativeSdk.dylib" "osx" "aarch64"
                download_file "${'$'}version" "NuGet/runtimes/osx-x64/native" "libDxFeedGraalNativeSdk.dylib" "osx" "x86_64"
                download_file "${'$'}version" "NuGet/runtimes/win-x64/native" "DxFeedGraalNativeSdk.dll" "windows" "amd64"
            """.trimIndent()
            formatStderrAsError = true
        }
        script {
            name = "NuGet Pack and Deploy"
            scriptContent = """
                git config --global safe.directory '*'
                VERSION=${'$'}(git describe --abbrev=0)
                VERSION=${'$'}{VERSION#"v"}
                nuget pack NuGet/DxFeed.Graal.Native.nuspec -Version ${'$'}VERSION
                # The package is about 110 MB: the default timeout of 300 s is not enough for a slow upload.
                # -SkipDuplicate makes a rerun safe if a timed out push has been accepted after all.
                nuget push DxFeed.Graal.Native.${'$'}VERSION.nupkg -Source https://api.nuget.org/v3/index.json -ApiKey %env.NUGETORG_API_KEY% -SkipDuplicate -Timeout 1800
            """.trimIndent()
            formatStderrAsError = true
            inDocker(Images.NUGET, "--rm -m 8g")
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${BuildAndDeployForAll.id}"
            successfulOnly = true
            enforceCleanCheckout = true
        }
    }

    features {
        nexusDockerLogin()
        notifications {
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_137"
                sendTo = "#graal-api"
                messageFormat = verboseMessageFormat {
                    addChanges = true
                    maximumNumberOfChanges = 10
                }
            }
            buildFinishedSuccessfully = true
        }
    }

    requirements {
        linuxAgent()
    }
})

object SyncGitHubWithMainAndPublishRelease : BuildType({
    name = "Publish GitHub Release"

    artifactRules = """
        release-artifacts/*.zip
        release-notes.md
    """.trimIndent()

    mainRepository()

    steps {
        script {
            name = "Push main and release tag to GitHub"
            scriptContent = """
                set -eu

                REMOTE="git@github.com:dxFeed/dxfeed-graal-native-sdk.git"
                TAG=${'$'}(git describe --tags --abbrev=0)

                echo "Pushing main..."
                git push "${'$'}REMOTE" main

                echo "Pushing release tag: ${'$'}TAG"
                git push "${'$'}REMOTE" \
                    "refs/tags/${'$'}TAG:refs/tags/${'$'}TAG"
            """.trimIndent()
        }

        script {
            name = "Prepare GitHub release"
            scriptContent = """
                set -eu

                TAG=${'$'}(git describe --tags --abbrev=0)
                VERSION=${'$'}{TAG#v}

                NEXUS="https://maven.in.devexperts.com"
                BASE_URL="${'$'}NEXUS/repository/qd/com/dxfeed/graal-native-sdk/${'$'}VERSION"
                INDEX_URL="${'$'}NEXUS/service/rest/repository/browse/qd/com/dxfeed/graal-native-sdk/${'$'}VERSION/"

                echo "Preparing release ${'$'}TAG"
                echo "Version: ${'$'}VERSION"

                rm -rf release-artifacts
                mkdir -p release-artifacts

                #
                # Obtain all ZIP artifact names from Nexus.
                #
                curl \
                    --fail \
                    --location \
                    --silent \
                    --show-error \
                    "${'$'}INDEX_URL" \
                    --output nexus-index.html

                grep -oE \
                    "graal-native-sdk-${'$'}VERSION-[^\"<>[:space:]]+\.zip" \
                    nexus-index.html \
                    | sort -u \
                    > release-artifacts.txt

                if [ ! -s release-artifacts.txt ]; then
                    echo "No ZIP artifacts found for version ${'$'}VERSION"
                    exit 1
                fi

                echo "Artifacts:"
                cat release-artifacts.txt

                #
                # Download exactly the artifacts belonging to this version.
                #
                while IFS= read -r artifact; do
                    echo "Downloading ${'$'}artifact"

                    curl \
                        --fail \
                        --location \
                        --silent \
                        --show-error \
                        "${'$'}BASE_URL/${'$'}artifact" \
                        --output "release-artifacts/${'$'}artifact"
                done < release-artifacts.txt

                #
                # Extract the section:
                #
                #   ## v3.2.11
                #   ...
                #
                # up to the next ## heading.
                #
                git show "${'$'}{TAG}:ReleaseNotes.md" | \
                    awk -v tag="${'$'}TAG" '
                        ${'$'}0 == "## " tag {
                            found = 1
                            next
                        }

                        found && /^## / {
                            exit
                        }

                        found {
                            print
                        }

                        END {
                            if (!found)
                                exit 2
                        }
                    ' > release-notes.md

                if [ ! -s release-notes.md ]; then
                    echo "Release notes for ${'$'}TAG are missing or empty"
                    exit 1
                fi

                echo
                echo "Release notes:"
                cat release-notes.md

                echo
                echo "Downloaded artifacts:"
                ls -lh release-artifacts
            """.trimIndent()
        }

        script {
            name = "Publish GitHub release"
            scriptContent = """
                set -eu

                TAG=${'$'}(git describe --tags --abbrev=0)

                export GH_REPO="dxFeed/dxfeed-graal-native-sdk"

                # A version with a qualifier (v3.6.0-rc1) is a pre-release on GitHub, so it is not marked as the latest.
                PRE_RELEASE=false
                case "${'$'}TAG" in
                    *-*) PRE_RELEASE=true ;;
                esac

                if ! command -v gh >/dev/null 2>&1; then
                    echo "GitHub CLI (gh) is not installed on this agent, installing..."

                    ARCH=$(uname -m)
                    case "${'$'}ARCH" in
                        x86_64)         GH_ARCH="amd64" ;;
                        aarch64|arm64)  GH_ARCH="arm64" ;;
                        *)
                            echo "Unsupported architecture: ${'$'}ARCH"
                            exit 1
                            ;;
                    esac

                    TMP_DIR=$(mktemp -d)
                    trap 'rm -rf "${'$'}TMP_DIR"' EXIT

                    curl \
                        --fail \
                        --location \
                        --silent \
                        --show-error \
                        --header "Accept: application/vnd.github+json" \
                        --header "Authorization: Bearer %env.GH_TOKEN%" \
                        --header "X-GitHub-Api-Version: 2026-03-10" \
                        "https://api.github.com/repos/cli/cli/releases/latest" \
                        --output "${'$'}TMP_DIR/release.json"

                    GH_VERSION=$(
                        sed -nE 's/.*"tag_name"[[:space:]]*:[[:space:]]*"v([^"]+)".*/\1/p' \
                            "${'$'}TMP_DIR/release.json" \
                            | head -n1
                    )

                    if [ -z "${'$'}GH_VERSION" ]; then
                        echo "Failed to determine latest GitHub CLI version"
                        cat "${'$'}TMP_DIR/release.json"
                        exit 1
                    fi

                    echo "Installing GitHub CLI ${'$'}GH_VERSION for ${'$'}GH_ARCH..."

                    curl \
                        --fail \
                        --location \
                        --silent \
                        --show-error \
                        "https://github.com/cli/cli/releases/download/v${'$'}{GH_VERSION}/gh_${'$'}{GH_VERSION}_linux_${'$'}{GH_ARCH}.tar.gz" \
                        --output "${'$'}TMP_DIR/gh.tar.gz"

                    tar -xzf "${'$'}TMP_DIR/gh.tar.gz" -C "${'$'}TMP_DIR"

                    INSTALL_DIR="${'$'}HOME/.local/bin"
                    mkdir -p "${'$'}INSTALL_DIR"

                    cp \
                        "${'$'}TMP_DIR/gh_${'$'}{GH_VERSION}_linux_${'$'}{GH_ARCH}/bin/gh" \
                        "${'$'}INSTALL_DIR/gh"

                    chmod +x "${'$'}INSTALL_DIR/gh"

                    export PATH="${'$'}INSTALL_DIR:${'$'}PATH"

                    # Preserve PATH for subsequent TeamCity build steps.
                    echo "##teamcity[setParameter name='env.PATH' value='${'$'}INSTALL_DIR:${'$'}PATH']"

                    gh --version
                else
                    echo "GitHub CLI is already installed:"
                    gh --version
                fi

                if gh release view "${'$'}TAG" >/dev/null 2>&1; then
                    echo "Release ${'$'}TAG already exists; updating it"

                    gh release edit "${'$'}TAG" \
                        --title "${'$'}TAG" \
                        --prerelease="${'$'}PRE_RELEASE" \
                        --notes-file release-notes.md

                    gh release upload "${'$'}TAG" \
                        release-artifacts/*.zip \
                        --clobber
                else
                    echo "Creating release ${'$'}TAG"

                    gh release create "${'$'}TAG" \
                        release-artifacts/*.zip \
                        --verify-tag \
                        --title "${'$'}TAG" \
                        --prerelease="${'$'}PRE_RELEASE" \
                        --notes-file release-notes.md
                fi
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${DeployNuget.id}"
            successfulOnly = true
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "sshkey_ed25519"
        }
    }

    requirements {
        linuxAgent()
    }
})

object BuildForLinux : BuildType({
    name = "Build [Linux, x64]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        script {
            name = "Build"
            scriptContent = """
                mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.BITBUCKET} clean package
            """.trimIndent()
            formatStderrAsError = true
            inDocker(Images.GRAALVM_LINUX_X64, "--rm -m 8g")
        }
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
})

object TestForLinux : BuildType({
    name = "Test [Linux, x64]"
    description = "Builds the library and runs the C tests (src/main/c, CTest) for the pull requests to main and for main"

    mainRepository()

    steps {
        script {
            name = "Build the library"
            scriptContent = """
                mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.BITBUCKET} clean package
            """.trimIndent()
            formatStderrAsError = true
            inDocker(Images.GRAALVM_LINUX_X64, "--rm -m 8g")
        }

        script {
            // The library is built for glibc 2.17 (Oracle Linux 7); the tests are built and run on Oracle Linux 9.
            name = "Build and run the C tests"
            scriptContent = """
                set -eu
                cd src/main/c
                rm -rf build bin
                cmake --preset=conf-release -DCMAKE_BUILD_TYPE=Release .
                cmake --build --preset=build-release --config=Release -j${'$'}(nproc)
                ctest --test-dir build/conf-release -C Release --output-on-failure --output-junit ctest-junit.xml
            """.trimIndent()
            inDocker(Images.CPP_TEST_LINUX_X64, "--rm -m 4g")
        }
    }

    triggers {
        vcs {
            branchFilter = """
                +:<default>
                +:pull-requests/*
            """.trimIndent()
        }
    }

    features {
        pullRequests {
            vcsRootExtId = "${SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags.id}"
            provider = bitbucketServer {
                serverUrl = "https://stash.in.devexperts.com"
                authType = password {
                    username = "dxcity"
                    password = "%dxcity.token.bitbucket%"
                }
                filterTargetBranch = "+:refs/heads/main"
                usePullRequestBranches = true
            }
        }

        commitStatusPublisher {
            vcsRootExtId = "${SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags.id}"
            publisher = bitbucketServer {
                url = "https://stash.in.devexperts.com"
                authType = password {
                    userName = "dxcity"
                    password = "%dxcity.token.bitbucket%"
                }
            }
        }

        xmlReport {
            reportType = XmlReport.XmlReportType.JUNIT
            rules = "src/main/c/build/conf-release/ctest-junit.xml"
        }

        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
})

object BuildAndPushDockerImageForLinuxX64 : BuildType({
    name = "Build & Push a Docker Image [Linux, x64]"

    mainRepository()

    steps {
        buildAndPushDockerImage(Images.GRAALVM_LINUX_X64, "--build-arg GRAALVM_VERSION=\"%env.GRAALVM_VERSION%\" -f graalvm-linux-x64.Dockerfile .")
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
})

object BuildAndPushTestDockerImageForLinuxX64 : BuildType({
    name = "Build & Push a Test Docker Image [Linux, x64]"
    description = "The image with the C/C++ tools for the tests (cpp-test-linux-x64.Dockerfile)"

    mainRepository()

    steps {
        buildAndPushDockerImage(Images.CPP_TEST_LINUX_X64, "-f cpp-test-linux-x64.Dockerfile .")
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
})

object BuildAndPushDockerImageForLinuxAarch64 : BuildType({
    name = "Build & Push a Docker Image [Linux, aarch64]"

    mainRepository()

    steps {
        buildAndPushDockerImage(Images.GRAALVM_LINUX_AARCH64, "--build-arg GRAALVM_VERSION=\"%env.GRAALVM_VERSION%\" -f graalvm-linux-aarch64.Dockerfile .")
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        macAgent()
    }
})

object BuildForLinuxAarch64 : BuildType({
    name = "Build [Linux, aarch64]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        script {
            name = "Build"
            scriptContent = """
                mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.BITBUCKET} clean package
            """.trimIndent()
            formatStderrAsError = true
            inDocker(Images.GRAALVM_LINUX_AARCH64, "--rm")
        }
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        macAgentForLinuxAarch64()
    }
})

object BuildAndPushDockerImageForWindowsX64 : BuildType({
    name = "Build & Push a Docker Image [Windows, x64]"

    mainRepository()

    val image = Images.GRAALVM_WINDOWS_X64

    steps {
        powerShell {
            name = "Build"
            scriptMode = script {
                content = """
                    ${'$'}ErrorActionPreference = 'Stop'
                    cd .teamcity
                    docker images --all
                    docker build --pull -t $image --build-arg GRAALVM_VERSION="%env.GRAALVM_VERSION%" -f graalvm-win-x64-v2.Dockerfile .
                    if (${'$'}LASTEXITCODE -ne 0) { throw "docker build failed" }
                    docker push $image
                    if (${'$'}LASTEXITCODE -ne 0) { throw "docker push failed" }
                    docker rmi -f $image
                """.trimIndent()
            }
        }
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        windowsAgent()
    }
})

object BuildForWindows : BuildType({
    name = "Build [Windows, x64]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        mavenWithPowerShell("Build", "${Mvn.POWERSHELL_NEXUS} ${Mvn.POWERSHELL_BITBUCKET} clean package")
    }

    requirements {
        windowsAgent()
    }
})

object BuildForWindowsInDocker : BuildType({
    name = "Build [Windows, x64] in Docker"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        script {
            name = "Build"
            scriptContent = """
            mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.BITBUCKET} clean package
        """.trimIndent()
            formatStderrAsError = true
            inDocker(Images.GRAALVM_WINDOWS_X64, "--rm -m 8g", ScriptBuildStep.ImagePlatform.Windows)
        }
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        windowsAgent()
    }
})

object BuildForMacOSAndIOS : BuildType({
    name = "Build [macOS, iOS]"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        script {
            name = "Build"
            scriptContent = Util.prepareMacOS() + """
                export JAVA_HOME=${'$'}{graalvm_arm64_path}/Contents/Home
                arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} clean package
                arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} -DmacIos=true clean package
                export JAVA_HOME=${'$'}{graalvm_x64_path}/Contents/Home
                arch -x86_64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} -DmacIosSimulator=true package
                arch -x86_64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} clean package
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    requirements {
        macAgent()
    }
})

object BuildJreLibrariesForIOS : BuildType({
    name = "Build JRE Libraries [iOS]"
    description = "Builds the jre-ios libraries from the sources of GraalVM Community and checks that the SDK for iOS and the simulator links with them"
    artifactRules = """
        out/jre-ios/libraries/** => jre-ios-libraries-%jre.ios.graal.tag%.zip
        out/jre-ios/sdk/*.zip
    """.trimIndent()

    params {
        // The tags of oracle/graal: the GraalVM Community versions of the iOS build (graal-25.4.4.1.1, jdk-25.0.1, ...),
        // for the arm64 slices and for the x86_64 slice of the simulator, as in prepareMacOS.
        param("jre.ios.graal.tag", "%env.GRAALVM_VERSION%")
        param("jre.ios.x64.graal.tag", "%env.GRAALVM_VERSION_MACOS_X64%")
    }

    mainRepository()

    steps {
        script {
            name = "Build the libraries"
            scriptContent = """
                set -e
                rm -rf out/jre-ios
                arch -arm64 /usr/bin/python3 jre-ios/build.py build --graal-tag "%jre.ios.graal.tag%" --x64-graal-tag "%jre.ios.x64.graal.tag%" --work ~/.graal/jre-ios --out out/jre-ios/libraries
                cp out/jre-ios/libraries/*.a jre-ios/
            """.trimIndent()
        }

        script {
            name = "Build the SDK for iOS and the simulator with the libraries"
            scriptContent = "set -e\n" + Util.prepareMacOS() + """
                mkdir -p out/jre-ios/sdk
                # The next step checks both archives, so one build reports all the missing symbols.
                export JAVA_HOME=${'$'}{graalvm_arm64_path}/Contents/Home
                arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} -DmacIos=true -Dios.check.skip=true clean package
                # The build for the simulator deletes the archive for iOS.
                cp target/*-aarch64-ios.zip out/jre-ios/sdk/
                export JAVA_HOME=${'$'}{graalvm_x64_path}/Contents/Home
                arch -x86_64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} -DmacIosSimulator=true -Dios.check.skip=true package
                cp target/*-ios-simulator.zip out/jre-ios/sdk/
            """.trimIndent()
            formatStderrAsError = true
        }

        script {
            name = "Check that the SDK links with the libraries"
            scriptContent = """
                set -e
                # Both archives are checked, so one build reports all the missing symbols.
                status=0
                for entry in "ios aarch64-ios" "simulator ios-simulator"; do
                    set -- ${'$'}{entry}
                    directory=out/jre-ios/check/${'$'}1
                    rm -rf "${'$'}{directory}"
                    mkdir -p "${'$'}{directory}"
                    unzip -q out/jre-ios/sdk/*-${'$'}2.zip -d "${'$'}{directory}"
                    arch -arm64 /usr/bin/python3 jre-ios/build.py check --dir "${'$'}{directory}" --platform ${'$'}1 || status=1
                done
                exit ${'$'}{status}
            """.trimIndent()
        }
    }

    requirements {
        macAgent()
    }
})

object DetectVisualStudioVersion : BuildType({
    name = "Detect Visual Studio Version"
    description = "Runs a check of the installed VS on each Windows agent separately"

    params {
        param("env.DETECTED_VS_VERSION", "")
    }

    steps {
        powerShell {
            name = "Detect Visual Studio Version"
            scriptMode = script {
                content = """
                    ${'$'}vswhere = "${'$'}{env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"

                    if (-not (Test-Path ${'$'}vswhere)) {
                        Write-Host "##teamcity[message text='vswhere.exe not found - VS Installer component missing' status='WARNING']"
                        exit 0
                    }

                    ${'$'}installations = & ${'$'}vswhere -all -products * -format json | ConvertFrom-Json

                    if (-not ${'$'}installations) {
                        Write-Host "##teamcity[message text='Visual Studio not found on this agent' status='WARNING']"
                        exit 0
                    }

                    foreach (${'$'}vs in ${'$'}installations) {
                        Write-Host "Name:    ${'$'}(${'$'}vs.displayName)"
                        Write-Host "Version: ${'$'}(${'$'}vs.installationVersion)"
                        Write-Host "Path:    ${'$'}(${'$'}vs.installationPath)"
                        Write-Host "---"
                    }

                    ${'$'}primary = ${'$'}installations | Select-Object -First 1
                    Write-Host "##teamcity[setParameter name='env.DETECTED_VS_VERSION' value='${'$'}(${'$'}primary.installationVersion)']"
                """.trimIndent()
            }
            formatStderrAsError = true
        }
    }

    requirements {
        windowsAgent()
    }
})

object CopyServiceImages : BuildType({
    name = "Copy service images"

    params {
        password("env.jfrogPass", "credentialsJSON:d288798f-47b9-4fbb-8463-a68be694481d")
        param("env.srcRepo", "dxfeed-docker.jfrog.io/dxfeed-api/nuget:6.9.1")
        param("env.target.repo", "nexus-docker-graalvm.in.devexperts.com")
    }

    mainRepository()

    steps {
        script {
            name = "test"
            id = "test"
            scriptContent = """
                echo "Installing Skopeo"
                apt-get update
                apt-get install -y skopeo
                apt-get install -y ca-certificates

                image_tag=${'$'}(echo "%env.srcRepo%" | cut -d "/" -f 3)
                echo "Image tag is ${'$'}image_tag"
                echo "Source image - %env.srcRepo%:latest"
                echo "Target image - %env.target.repo%/${'$'}image_tag"


                #skopeo copy \
                #  --src-creds amordovskii:%env.jfrogPass% \
                #  --dest-creds %dxcity.namecode.nexus%:%dxcity.passcode.nexus% \
                #  docker://%env.srcRepo% docker://%env.target.repo%/${'$'}image_tag

                echo "List images nuget:"
                skopeo list-tags \
                  --creds %dxcity.namecode.nexus%:%dxcity.passcode.nexus% \
                docker://%env.target.repo%/nuget

                echo "List images graal:"
                skopeo list-tags \
                  --creds %dxcity.namecode.nexus%:%dxcity.passcode.nexus% \
                docker://%env.target.repo%/graalvm
            """.trimIndent()
            dockerImage = "ubuntu:latest"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }

    features {
        perfmon {}
    }
})

object ListServiceImages : BuildType({
    name = "List service images"

    params {
        password("env.jfrogPass", "credentialsJSON:d288798f-47b9-4fbb-8463-a68be694481d")
        param("env.srcRepo", "dxfeed-docker.jfrog.io/dxfeed-api/nuget:6.9.1")
        param("env.target.repo", "nexus-docker-graalvm.in.devexperts.com")
    }

    mainRepository()

    steps {
        script {
            name = "test"
            id = "test"
            scriptContent = """
                echo "Installing Skopeo"
                apt-get update
                apt-get install -y skopeo
                apt-get install -y ca-certificates

                image_tag=${'$'}(echo "%env.srcRepo%" | cut -d "/" -f 3)
                echo "Image tag is ${'$'}image_tag"
                echo "Source image - %env.srcRepo%:latest"
                echo "Target image - %env.target.repo%/${'$'}image_tag"

                echo "List images nuget:"
                skopeo list-tags \
                  --creds %dxcity.namecode.nexus%:%dxcity.passcode.nexus% \
                docker://%env.target.repo%/nuget

                echo "List images graal:"
                skopeo list-tags \
                  --creds %dxcity.namecode.nexus%:%dxcity.passcode.nexus% \
                docker://%env.target.repo%/graalvm
            """.trimIndent()
            dockerImage = "ubuntu:latest"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }

    features {
        perfmon {}
    }
})
