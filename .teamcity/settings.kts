@file:Suppress("CanConvertToMultiDollarString")

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.*
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule
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
        text(
            "env.GRAALVM_VERSION", "graal-25.5.5.0.3",
            label = "GraalVM version",
            description = "The GraalVM of the builds: the tag of its Docker images (graalvm:linux-x64-<version>) and " +
                "the release on macOS and Windows agents. Examples: graal-25.5.5.0.3, jdk-25.0.1",
            allowEmpty = false,
        )
        text(
            "env.GRAALVM_VERSION_MACOS_X64", "jdk-25.0.1",
            label = "GraalVM version for macOS x64",
            description = "The GraalVM of the macOS x64 and iOS Simulator builds. Example: jdk-25.0.1",
            allowEmpty = false,
        )
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

    // The builds started by hand to make a release; they start the release chain.
    buildType(ReleaseTheNextPatch)
    buildType(ReleaseMajorMinorPatchOrRc)
    buildTypesOrder = arrayListOf(ReleaseTheNextPatch, ReleaseMajorMinorPatchOrRc)

    // The groups of the other builds (the ids of the builds do not depend on the group).
    subProject(ReleaseChain)
    subProject(ImagesAndLibraries)
    subProject(Checks)
    subProject(Tests)
    subProject(Etc)
    subProjectsOrder = arrayListOf(ReleaseChain, ImagesAndLibraries, Checks, Tests, Etc)
}

object ReleaseChain : Project({
    name = "Release chain"
    description = "Started by the release builds: the archives of the platforms, the XCFramework, NuGet, the GitHub release and the Jira release"

    buildType(BuildAndDeployForAll)
    buildType(BuildAndDeployForLinuxAarch64)
    buildType(BuildAndDeployForLinuxAarch64Release)
    buildType(BuildAndDeployForLinuxAarch64Debug)
    buildType(BuildAndDeployForWindows)
    buildType(BuildAndDeployForWindowsRelease)
    buildType(BuildAndDeployForWindowsDebug)
    buildType(BuildAndDeployForWindowsStaticRuntimeRelease)
    buildType(BuildAndDeployForWindowsStaticRuntimeDebug)
    buildType(BuildAndDeployForMacOsAndIOS)
    buildType(BuildAndDeployXCFramework)
    buildType(DeployNuget)
    buildType(SyncGitHubWithMainAndPublishRelease)
    buildType(ReleaseInJira)
    buildType(PublishCApiDocsToGitHubPages)
    buildTypesOrder = arrayListOf(
        BuildAndDeployForAll,
        BuildAndDeployForLinuxAarch64,
        BuildAndDeployForLinuxAarch64Release,
        BuildAndDeployForLinuxAarch64Debug,
        BuildAndDeployForWindows,
        BuildAndDeployForWindowsRelease,
        BuildAndDeployForWindowsDebug,
        BuildAndDeployForWindowsStaticRuntimeRelease,
        BuildAndDeployForWindowsStaticRuntimeDebug,
        BuildAndDeployForMacOsAndIOS,
        BuildAndDeployXCFramework,
        DeployNuget,
        SyncGitHubWithMainAndPublishRelease,
        ReleaseInJira,
        PublishCApiDocsToGitHubPages
    )
})

object ImagesAndLibraries : Project({
    name = "Images & Libraries"
    description = "Build and push the Docker images of the builds, build the JRE libraries for iOS"

    buildType(BuildAndPushDockerImageForLinuxX64)
    buildType(BuildAndPushDockerImageForLinuxAarch64)
    buildType(BuildAndPushDockerImageForWindowsX64)
    buildType(BuildAndPushTestDockerImageForLinuxX64)
    buildType(BuildAndPushDocsCheckDockerImageForLinuxX64)
    buildType(BuildJreLibrariesForIOS)
    buildTypesOrder = arrayListOf(
        BuildAndPushDockerImageForLinuxX64,
        BuildAndPushDockerImageForLinuxAarch64,
        BuildAndPushDockerImageForWindowsX64,
        BuildAndPushTestDockerImageForLinuxX64,
        BuildAndPushDocsCheckDockerImageForLinuxX64,
        BuildJreLibrariesForIOS
    )
})

object Checks : Project({
    name = "Checks"
    description = "The builds of the SDK for a platform without a release, and the checks of the agents"

    buildType(BuildForLinux)
    buildType(BuildForLinuxAarch64)
    buildType(BuildForWindows)
    buildType(BuildForWindowsInDocker)
    buildType(BuildForMacOSAndIOS)
    buildType(DetectVisualStudioVersion)
    buildType(ListDockerImages)
    buildTypesOrder = arrayListOf(
        BuildForLinux,
        BuildForLinuxAarch64,
        BuildForWindows,
        BuildForWindowsInDocker,
        BuildForMacOSAndIOS,
        DetectVisualStudioVersion,
        ListDockerImages
    )
})

object Tests : Project({
    name = "Tests"
    description = "Started for the pull requests to main and for main"

    buildType(TestForLinux)
    buildType(TestForMacOS)
    buildType(TestForWindows)
})

object Etc : Project({
    name = "Etc."

    buildType(CxxApiGitGubJiraSync)
})

object ReleaseTheNextPatch : BuildType({
    name = "Release The Next Patch\\RC: Prepare The Release & Build & Deploy [Linux, x64][Release][Debug]"
    description = "Releases the version of pom.xml without -SNAPSHOT (3.13.1-SNAPSHOT: v3.13.1), deploys the " +
        "Linux x64 archives; then Build & Deploy [All] builds the other platforms"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        linuxReleaseSteps(
                // With -q, Maven prints its errors to stdout, which the substitution would swallow: they go to stderr.
                tagCommand = """
                if ! VERSION=$(mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} --batch-mode help:evaluate \
                    -Dexpression=project.version \
                    -q \
                    -DforceStdout); then
                    echo "${'$'}VERSION" >&2
                    exit 1
                fi
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

object ReleaseMajorMinorPatchOrRc : BuildType({
    name = "Release (Major.Minor.Patch[-rcN]): Prepare The Release & Build & Deploy [Linux, x64][Release][Debug]"
    description = "Releases the given version (tag vX.Y.Z[-rcN]), deploys the Linux x64 archives; then " +
        "Build & Deploy [All] builds the other platforms"
    artifactRules = "*.zip"

    params {
        text(
            "env.RELEASE_VERSION", "",
            label = "Release version",
            description = "The version to release, without v: X.Y.Z or X.Y.Z-rcN. Examples: 3.14.0, 3.14.0-rc1",
            allowEmpty = false,
        )
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
    description = "Deploys the Linux aarch64 archives of the latest tag to Nexus"
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
    description = "Deploys the Linux aarch64 debug archives (-P buildDebug) of the latest tag to Nexus"
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
    description = "The Release and Debug deploys of the Linux aarch64 archives of the latest tag"
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
    description = "Deploys the Windows x64 archives of the latest tag to Nexus"
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
    description = "Deploys the Windows x64 debug archives (-P buildDebug) of the latest tag to Nexus"
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

/**
 * The archives of the static C runtime, without the Visual C++ Redistributable: the DLL linked with it
 * (amd64-windows-static-crt) and the static library of the SDK for any C runtime of the application
 * (amd64-windows-static-lib), from one build of the image. Both are checked before the deploy, the DLL also in a
 * servercore container (scripts/windows-static-runtime).
 */
object BuildAndDeployForWindowsStaticRuntimeRelease : BuildType({
    name = "Build & Deploy [Windows, x64][Release][Static CRT]"
    description = "Deploys the Windows x64 archives without the Visual C++ Redistributable (amd64-windows-static-crt, " +
        "amd64-windows-static-lib) of the latest tag to Nexus"
    artifactRules = "target/*.zip"

    mainRepository()

    steps {
        checkoutLatestTagWithPowerShell()
        mavenWithPowerShell("Deploy", "${Mvn.POWERSHELL_NEXUS} ${Mvn.POWERSHELL_REPO} ${Mvn.POWERSHELL_BITBUCKET} clean deploy ${Mvn.POWERSHELL_STATIC_RUNTIME}")
    }

    requirements {
        windowsAgent()
    }
})

/** The debug archives of the static C runtime (amd64-windows-debug-static-crt, amd64-windows-debug-static-lib). */
object BuildAndDeployForWindowsStaticRuntimeDebug : BuildType({
    name = "Build & Deploy [Windows, x64][Debug][Static CRT]"
    description = "Deploys the Windows x64 debug archives without the Visual C++ Redistributable " +
        "(amd64-windows-debug-static-crt, amd64-windows-debug-static-lib) of the latest tag to Nexus"
    artifactRules = "target/*.zip"

    mainRepository()

    steps {
        checkoutLatestTagWithPowerShell()
        mavenWithPowerShell("Deploy", "${Mvn.POWERSHELL_NEXUS} ${Mvn.POWERSHELL_REPO} ${Mvn.POWERSHELL_BITBUCKET} clean deploy -P buildDebug ${Mvn.POWERSHELL_STATIC_RUNTIME}")
    }

    requirements {
        windowsAgent()
    }
})

object BuildAndDeployForWindows : BuildType({
    name = "Build & Deploy [Windows]"
    description = "The Release, Debug and Static CRT deploys of the Windows x64 archives of the latest tag"
    type = Type.COMPOSITE
    artifactRules = "*.zip"

    dependencies {
        snapshot(BuildAndDeployForWindowsRelease) {
            onDependencyFailure = FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForWindowsDebug) {
            onDependencyFailure = FailureAction.IGNORE
        }
        snapshot(BuildAndDeployForWindowsStaticRuntimeRelease) {
            onDependencyFailure = FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForWindowsStaticRuntimeDebug) {
            onDependencyFailure = FailureAction.IGNORE
        }
    }
})

object BuildAndDeployForMacOsAndIOS : BuildType({
    name = "Build & Deploy [macOS, iOS]"
    description = "Deploys the macOS (arm64, x86_64), iOS and iOS Simulator archives of the latest tag to Nexus"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        checkoutLatestTag()

        script {
            name = "Deploy"
            // set -e: a failed build of one platform fails the step, not only the last one.
            scriptContent = "set -e\n" + Util.prepareMacOS() + """
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
                TAG=$(git describe --tags --abbrev=0)
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
                TAG=$(git describe --tags --abbrev=0)
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
    description = "After a release build: the deploys of Linux aarch64, Windows, macOS, iOS and the XCFramework of " +
        "the latest tag; then Pack NuGet"
    type = Type.COMPOSITE
    allowExternalStatus = true
    artifactRules = "*.zip"

    triggers {
        finishBuildTrigger {
            buildType = "${ReleaseTheNextPatch.id}"
            successfulOnly = true
        }
        finishBuildTrigger {
            buildType = "${ReleaseMajorMinorPatchOrRc.id}"
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

// The package is published by the workflow .github/workflows/nuget-publish.yml that "Publish GitHub Release" runs:
// nuget.org gives Trusted Publishing only to GitHub Actions and GitLab, and its API keys live 30 days at most. The id
// DeployNuget keeps the history of the build.
object DeployNuget : BuildType({
    name = "Pack NuGet"
    description = "After Build & Deploy [All]: packs DxFeed.Graal.Native.X.Y.Z.nupkg of the latest tag from its " +
        "archives in Nexus (linux-x64, linux-arm64, osx-arm64, osx-x64, win-x64)"
    artifactRules = "*.nupkg"

    mainRepository()

    steps {
        script {
            name = "Download Artifacts"
            scriptContent = """
                download_file() {
                  version=$1
                  path_to_save=$2
                  file_name=$3
                  os=$4
                  platform=$5
                  extension="zip"

                  base_url="https://maven.in.devexperts.com/repository/qd/com/dxfeed/graal-native-sdk"
                  archive_name="graal-native-sdk-${'$'}{version}-${'$'}{platform}-${'$'}{os}.${'$'}{extension}"
                  url="${'$'}{base_url}/${'$'}{version}/${'$'}{archive_name}"

                  mkdir -p "${'$'}path_to_save"
                  tmp_dir=$(mktemp -d)

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

                version=$(git describe --abbrev=0)
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
            name = "NuGet Pack"
            scriptContent = """
                git config --global safe.directory '*'
                VERSION=$(git describe --abbrev=0)
                VERSION=${'$'}{VERSION#"v"}
                nuget pack NuGet/DxFeed.Graal.Native.nuspec -Version ${'$'}VERSION
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
    }

    requirements {
        linuxAgent()
    }
})

object SyncGitHubWithMainAndPublishRelease : BuildType({
    name = "Publish GitHub Release"
    description = "Pushes main and the release tag to GitHub, publishes the GitHub release with the archives and the " +
        "NuGet package, and publishes the package to nuget.org with the workflow nuget-publish.yml"

    artifactRules = """
        release-artifacts/*.zip
        release-notes.md
    """.trimIndent()

    mainRepository()

    dependencies {
        // The package that "Pack NuGet" has just built from the release tag (the step "Prepare GitHub release" checks
        // its version).
        artifacts(DeployNuget) {
            buildRule = lastSuccessful()
            cleanDestination = true
            artifactRules = "DxFeed.Graal.Native.*.nupkg => nuget"
        }
    }

    steps {
        script {
            name = "Push main and release tag to GitHub"
            scriptContent = """
                set -eu

                ${GitHub.SSH_COMMAND}
                REMOTE="git@github.com:dxFeed/dxfeed-graal-native-sdk.git"
                TAG=$(git describe --tags --abbrev=0)

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

                TAG=$(git describe --tags --abbrev=0)
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

                PACKAGE="nuget/DxFeed.Graal.Native.${'$'}VERSION.nupkg"
                if [ ! -f "${'$'}PACKAGE" ]; then
                    echo "No NuGet package of version ${'$'}VERSION from Pack NuGet:"
                    ls -l nuget || true
                    exit 1
                fi
                cp "${'$'}PACKAGE" release-artifacts/

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
                        $0 == "## " tag {
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

                TAG=$(git describe --tags --abbrev=0)

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
                        release-artifacts/*.nupkg \
                        --clobber
                else
                    echo "Creating release ${'$'}TAG"

                    gh release create "${'$'}TAG" \
                        release-artifacts/*.zip \
                        release-artifacts/*.nupkg \
                        --verify-tag \
                        --title "${'$'}TAG" \
                        --prerelease="${'$'}PRE_RELEASE" \
                        --notes-file release-notes.md
                fi
            """.trimIndent()
        }

        script {
            name = "Publish NuGet with GitHub Actions"
            scriptContent = """
                set -eu

                TAG=$(git describe --tags --abbrev=0)
                WORKFLOW=nuget-publish.yml
                REQUEST="teamcity-%teamcity.build.id%"

                export GH_REPO="dxFeed/dxfeed-graal-native-sdk"
                # gh installed by the step "Publish GitHub release" (if env.PATH has not reached this step).
                export PATH="${'$'}PATH:${'$'}HOME/.local/bin"

                # GitHub may not know a workflow pushed with main a moment ago yet.
                echo "Running ${'$'}WORKFLOW for ${'$'}TAG (${'$'}REQUEST)"
                attempt=1
                until gh workflow run "${'$'}WORKFLOW" --ref main -f tag="${'$'}TAG" -f request="${'$'}REQUEST"; do
                    if [ "${'$'}attempt" -ge 5 ]; then
                        echo "Cannot run ${'$'}WORKFLOW"
                        exit 1
                    fi
                    attempt=$((attempt + 1))
                    sleep 15
                done

                # The run is found by the request in its name (run-name of the workflow). An error of gh fails the step.
                RUN_ID=""
                for attempt in $(seq 1 30); do
                    RUNS=$(
                        gh run list \
                            --workflow "${'$'}WORKFLOW" \
                            --event workflow_dispatch \
                            --limit 20 \
                            --json databaseId,displayTitle \
                            --jq ".[] | select(.displayTitle | endswith(\"(${'$'}REQUEST)\")) | .databaseId"
                    )
                    RUN_ID=$(echo "${'$'}RUNS" | head -n1)
                    if [ -n "${'$'}RUN_ID" ]; then
                        break
                    fi
                    sleep 10
                done

                if [ -z "${'$'}RUN_ID" ]; then
                    echo "No run of ${'$'}WORKFLOW for ${'$'}REQUEST has appeared"
                    exit 1
                fi

                echo "Run: https://github.com/${'$'}GH_REPO/actions/runs/${'$'}RUN_ID"

                # Not gh run watch: it does not work with a fine-grained token. The build timeout limits the wait; five
                # errors of gh in a row fail the step.
                errors=0
                while :; do
                    if STATE=$(
                        gh run view "${'$'}RUN_ID" --json status,conclusion --jq '.status + " " + .conclusion'
                    ); then
                        errors=0
                    else
                        errors=$((errors + 1))
                        if [ "${'$'}errors" -ge 5 ]; then
                            echo "Cannot get the state of the run ${'$'}RUN_ID"
                            exit 1
                        fi
                        STATE="unknown"
                    fi
                    echo "Run ${'$'}RUN_ID: ${'$'}STATE"
                    case "${'$'}STATE" in
                        "completed success") break ;;
                        completed*) echo "The run of ${'$'}WORKFLOW has failed: ${'$'}STATE"; exit 1 ;;
                    esac
                    sleep 30
                done
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
        // The release is complete when the package is on nuget.org.
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

    failureConditions {
        // The upload of the package and the run of the workflow take minutes; a hung run must not hold the agent.
        executionTimeoutMin = 60
    }

    requirements {
        linuxAgent()
    }
})

object ReleaseInJira : BuildType({
    name = "Release in Jira"
    description = "After the GitHub release vX.Y.Z: the SDK tickets resolved, the release mail, the Jira version " +
        "graal-native-sdk vX.Y.Z released"

    maxRunningBuilds = 1

    artifactRules = "release-mail.html"

    // The descriptions are shown in the Run dialog (Run with the "..." button), where the values can be changed for a run.
    params {
        select(
            "env.DRY_RUN", "0",
            label = "Dry run",
            description = "1: only read GitHub and Jira, log the changes that the build would make and write the " +
                "mail to the artifact release-mail.html; 0: make them",
            options = listOf("Make the changes" to "0", "Only log the changes" to "1"),
        )
        text(
            "env.TAG", "",
            label = "Release",
            description = "The tag of the GitHub release; empty: the latest one (an rc: nothing to do). Example: v3.9.0",
            allowEmpty = true,
        )
        text(
            "env.MAIL_TO", "anatoly.kalin@devexperts.com",
            label = "Release mail: To",
            description = "The comma-separated recipients of the release mail and of the mail about a failed " +
                "release; empty: no mail. Example: dxfeed-team@devexperts.com, someone@devexperts.com",
            allowEmpty = true,
        )
        text(
            "env.MAIL_CC", "",
            label = "Release mail: Cc",
            description = "The comma-separated copies of the release mail. Example: someone@devexperts.com",
            allowEmpty = true,
        )
        text(
            "env.RESEND_MAIL", "",
            label = "Resend the release mail",
            description = "A tag to send the mail of its GitHub release again (to To and Cc above), changing " +
                "nothing else. Example: v3.9.0",
            allowEmpty = true,
        )
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags, "+:.teamcity/jira-sync")
    }

    steps {
        script {
            name = "Release"
            // GH_TOKEN (env.GH_TOKEN of the project) raises the rate limit of the GitHub API.
            scriptContent = """
                export JIRA_URL="%jira.url%"
                export JIRA_TOKEN="%dxcity.token.jira%"
                python3 .teamcity/jira-sync/sdk_jira_release.py
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${SyncGitHubWithMainAndPublishRelease.id}"
            successfulOnly = true
        }
    }

    failureConditions {
        executionTimeoutMin = 5
    }

    requirements {
        linuxAgent()
    }
})

object PublishCApiDocsToGitHubPages : BuildType({
    name = "Publish C API Docs to GitHub Pages"
    description = "After the GitHub release vX.Y.Z[-rcN]: its C API reference in gh-pages of the GitHub mirror, " +
        "https://dxfeed.github.io/dxfeed-graal-native-sdk/"

    maxRunningBuilds = 1

    // The descriptions are shown in the Run dialog (Run with the "..." button), where the values can be changed for a run.
    params {
        select(
            "env.DRY_RUN", "0",
            label = "Dry run",
            description = "1: lay out the pages and log the changes, push nothing; 0: push them to gh-pages",
            options = listOf("Push the pages" to "0", "Only log the changes" to "1"),
        )
        text(
            "env.TAG", "",
            label = "Release",
            description = "The release tag; empty: the latest tag of main (the release just published). " +
                "Example: v3.9.0",
            allowEmpty = true,
        )
        select(
            "env.C_API_DOCS_SOURCE", "nexus",
            label = "Reference",
            description = "nexus: the c-api-docs-html zip of the release in Nexus; tag: generate the reference of " +
                "the tag with the Doxygen of the docs-check image (to publish a release again, e.g. one built by an " +
                "older Doxygen)",
            options = listOf("The zip in Nexus" to "nexus", "Generate from the tag" to "tag"),
        )
    }

    // The whole repository: an empty env.TAG is resolved by git describe.
    mainRepository()

    steps {
        cApiReferenceSteps(
            """if [ -n "${'$'}{TAG:-}" ]; then echo "${'$'}TAG"; else git describe --tags --abbrev=0; fi"""
        ) {
            equals("env.C_API_DOCS_SOURCE", "tag")
        }

        script {
            name = "Publish"
            scriptContent = """
                export GIT_AUTHOR_NAME=dxcity GIT_COMMITTER_NAME=dxcity
                export GIT_AUTHOR_EMAIL=dxcity@bots.devexperts.com GIT_COMMITTER_EMAIL=dxcity@bots.devexperts.com
                ${GitHub.SSH_COMMAND}
                if [ "${'$'}C_API_DOCS_SOURCE" = tag ]; then
                    export C_API_DOCS_HTML="${CApiReference.HTML}"
                fi
                python3 .teamcity/gh-pages/publish_c_api_docs.py
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${SyncGitHubWithMainAndPublishRelease.id}"
            successfulOnly = true
        }
    }

    // The key that pushes main and the tags to the mirror in Publish GitHub Release; the login for the image of
    // docs-check.
    features {
        sshAgent {
            teamcitySshKey = "sshkey_ed25519"
        }
        nexusDockerLogin()
    }

    failureConditions {
        executionTimeoutMin = 10
    }

    requirements {
        linuxAgent()
    }
})

object BuildForLinux : BuildType({
    name = "Build [Linux, x64]"
    description = "mvn clean package in the GraalVM image, without a deploy"
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
    description = "Builds the library, runs the C tests (src/main/c, CTest) and checks the C API documentation for the pull requests to main and for main"

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
                cmake --build --preset=build-release --config=Release -j$(nproc)
                ctest --test-dir build/conf-release -C Release --output-on-failure --output-junit ctest-junit.xml
            """.trimIndent()
            inDocker(Images.CPP_TEST_LINUX_X64, "--rm -m 4g")
        }

        script {
            // Fails the build on any Doxygen warning (MDAPI-443). It needs only the headers, so it runs after a failed
            // step too: a documentation warning does not hide the results of the tests, nor a failed build the warnings.
            name = "Check the C API documentation"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            scriptContent = """
                set -u
                log=target/c-api-docs-check/warnings.log
                doxygen --version
                rm -rf target/c-api-docs-check
                mkdir -p target/c-api-docs-check
                doxygen docs/Doxyfile.check || { echo "Doxygen failed with exit code ${'$'}?"; exit 1; }
                if [ ! -f "${'$'}log" ]; then
                    echo "Doxygen wrote no warnings log"
                    exit 1
                fi
                echo "Warnings: ${'$'}(grep -c -E ': (warning|error):' "${'$'}log")"
                if [ -s "${'$'}log" ]; then
                    cat "${'$'}log"
                    echo "Describe the declarations above: the build fails until docs/Doxyfile.check reports no warning"
                    exit 1
                fi
            """.trimIndent()
            inDocker(Images.DOCS_CHECK_LINUX_X64, "--rm")
        }
    }

    triggers {
        mainAndPullRequests()
    }

    features {
        pullRequestsToMain()
        javaTestsReport()
        cTestsReport()
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
})

object TestForMacOS : BuildType({
    name = "Test [macOS, arm64]"
    description = "Builds the library and runs the C tests (src/main/c, CTest) on the macOS host, without Docker, for the pull requests to main and for main"

    mainRepository()

    steps {
        script {
            name = "Build the library"
            scriptContent = Util.prepareMacOS() + """
                export JAVA_HOME=${'$'}{graalvm_arm64_path}/Contents/Home
                arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} clean package
            """.trimIndent()
            formatStderrAsError = true
        }

        script {
            // The build installs CMake into ~/.graal (the version is CMake in Common.kt); the compilers come from the Xcode
            // Command Line Tools.
            name = "Build and run the C tests"
            // The whole step runs as arm64, as the library does: CMakeLists.txt picks the architecture by the host
            // processor, so under Rosetta (x86_64) the tests would be built for x86_64.
            scriptContent = """
                set -eu
                if [ "$(uname -m)" != arm64 ] && [ -z "${'$'}{TEST_ARM64_REEXEC:-}" ]; then
                    TEST_ARM64_REEXEC=1 exec arch -arm64 /bin/bash "$0"
                fi
                if [ "$(uname -m)" != arm64 ]; then
                    echo "The C tests must run as arm64, but uname -m is $(uname -m)"
                    exit 1
                fi
                cmake_install_path=~/.graal/cmake-${CMake.VERSION}
                if [ ! -x "${'$'}{cmake_install_path}/CMake.app/Contents/bin/cmake" ]; then
                    rm -rf "${'$'}{cmake_install_path}"
                    .teamcity/scripts/install.sh cmake_macos ${CMake.VERSION} ${CMake.MACOS_SHA256} "${'$'}{cmake_install_path}"
                fi
                export PATH="${'$'}{cmake_install_path}/CMake.app/Contents/bin:${'$'}{PATH}"
                cmake --version
                cd src/main/c
                rm -rf build bin
                cmake --preset=conf-release -DCMAKE_BUILD_TYPE=Release .
                cmake --build --preset=build-release --config=Release -j$(sysctl -n hw.ncpu)
                ctest --test-dir build/conf-release -C Release --timeout 300 --output-on-failure --output-junit ctest-junit.xml
            """.trimIndent()
        }
    }

    triggers {
        mainAndPullRequests()
    }

    features {
        pullRequestsToMain()
        javaTestsReport()
        cTestsReport()
    }

    failureConditions {
        // The mac agents are shared with the releases: a hung build must not hold one.
        executionTimeoutMin = 30
    }

    requirements {
        macAgent()
    }
})

object TestForWindows : BuildType({
    name = "Test [Windows, x64]"
    description = "Builds the library and runs the C tests (src/main/c, CTest) on the Windows agent, without Docker, for the pull requests to main and for main"

    mainRepository()

    steps {
        mavenWithPowerShell("Build the library", "${Mvn.POWERSHELL_NEXUS} ${Mvn.POWERSHELL_BITBUCKET} clean package")

        // The build installs CMake into C:\BuildCache (the version is CMake in Common.kt); the compilers are the VS Build
        // Tools that prepareWinLocal sets up. The Visual Studio generator is multi-config, hence --config and -C Release,
        // and the instance is pinned to those Build Tools in case the agent has another Visual Studio.
        powerShellWithWinLocal("Build and run the C tests", """

            ${'$'}cmakeInstallPath = "C:\BuildCache\cmake-${CMake.VERSION}"
            if (-not (Test-Path "${'$'}cmakeInstallPath\bin\cmake.exe")) {
                Install-CMake -Version "${CMake.VERSION}" -Sha256 "${CMake.WINDOWS_X64_SHA256}" -InstallPath ${'$'}cmakeInstallPath
            }
            ${'$'}env:PATH = "${'$'}cmakeInstallPath\bin;${'$'}env:PATH"
            cmake --version
            if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }

            Set-Location src\main\c
            foreach (${'$'}dir in "build", "bin") {
                if (Test-Path ${'$'}dir) { Remove-Item -LiteralPath ${'$'}dir -Recurse -Force }
            }
            cmake --preset=conf-release -G "Visual Studio 17 2022" "-DCMAKE_GENERATOR_INSTANCE=${'$'}vsInstallPath" -DCMAKE_BUILD_TYPE=Release .
            if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
            cmake --build --preset=build-release --config=Release --parallel
            if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
            ctest --test-dir build/conf-release -C Release --timeout 300 --output-on-failure --output-junit ctest-junit.xml
            if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
        """.trimIndent())
    }

    triggers {
        mainAndPullRequests()
    }

    features {
        pullRequestsToMain()
        javaTestsReport()
        cTestsReport()
    }

    failureConditions {
        // The Windows agents are shared with the releases: a hung build must not hold one. The first run on an agent
        // without the cache also installs the VS Build Tools.
        executionTimeoutMin = 60
    }

    requirements {
        windowsAgent()
    }
})

object BuildAndPushDockerImageForLinuxX64 : BuildType({
    name = "Build & Push a Docker Image [Linux, x64]"
    description = "The GraalVM image of the Linux x64 builds, graalvm:linux-x64-<GraalVM version> " +
        "(docker/graalvm-linux-x64.Dockerfile); run it after a change of env.GRAALVM_VERSION"

    mainRepository()

    steps {
        buildAndPushDockerImage(Images.GRAALVM_LINUX_X64, "--build-arg GRAALVM_VERSION=\"%env.GRAALVM_VERSION%\" -f docker/graalvm-linux-x64.Dockerfile .")
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
    description = "The image with the C/C++ tools for the tests (docker/cpp-test-linux-x64.Dockerfile)"

    mainRepository()

    steps {
        buildAndPushDockerImage(Images.CPP_TEST_LINUX_X64, "-f docker/cpp-test-linux-x64.Dockerfile .")
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
})

object BuildAndPushDocsCheckDockerImageForLinuxX64 : BuildType({
    name = "Build & Push a Docs Check Docker Image [Linux, x64]"
    description = "The image with Doxygen for the check of the C API documentation (docker/docs-check-linux-x64.Dockerfile)"

    mainRepository()

    steps {
        buildAndPushDockerImage(
            Images.DOCS_CHECK_LINUX_X64,
            "--build-arg DOXYGEN_VERSION=${Doxygen.VERSION} --build-arg DOXYGEN_SHA256=${Doxygen.SHA256} -f docker/docs-check-linux-x64.Dockerfile ."
        )
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
    description = "The GraalVM image of the Linux aarch64 builds, graalvm:linux-aarch64-<GraalVM version> " +
        "(docker/graalvm-linux-aarch64.Dockerfile); run it after a change of env.GRAALVM_VERSION"

    mainRepository()

    steps {
        buildAndPushDockerImage(Images.GRAALVM_LINUX_AARCH64, "--build-arg GRAALVM_VERSION=\"%env.GRAALVM_VERSION%\" -f docker/graalvm-linux-aarch64.Dockerfile .")
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
    description = "mvn clean package in the GraalVM image, without a deploy"
    artifactRules = "target/*.zip"

    mainRepository()

    params {
        text("maven.arguments", "", label = "Maven arguments",
            description = "Added to mvn clean package. Example: -P buildDebug",
            display = ParameterDisplay.NORMAL, allowEmpty = true)
    }

    steps {
        script {
            name = "Build"
            scriptContent = """
                mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.BITBUCKET} clean package %maven.arguments%
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
    name = "Build & Push a Docker Image [Win, x64]"
    description = "The GraalVM image of the Windows x64 builds in Docker, graalvm:win-x64-<GraalVM version> " +
        "(docker/graalvm-win-x64-v2.Dockerfile); run it after a change of env.GRAALVM_VERSION"

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
                    docker build --pull -t $image --build-arg GRAALVM_VERSION="%env.GRAALVM_VERSION%" -f docker/graalvm-win-x64-v2.Dockerfile .
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
    name = "Build [Win, x64]"
    description = "mvn clean integration-test on the Windows agent, without a deploy"
    // With -DwindowsStaticRuntime=true, also the generated CRT shim and its list of functions, to look at. The archives
    // are not checked (verify).
    artifactRules = """
        target/*.zip
        target/windows-static-runtime/crt_shim.asm => windows-static-runtime
        target/windows-static-runtime/crt-functions.txt => windows-static-runtime
    """.trimIndent()

    mainRepository()

    params {
        // -DwindowsStaticRuntime=true: the archives without the Visual C++ Redistributable, -static-crt and
        // -static-lib, which is packed at integration-test.
        text("maven.arguments", "", label = "Maven arguments",
            description = "Added to mvn clean integration-test. Examples: -P buildDebug, " +
                "-DwindowsStaticRuntime=true (the archives without the Visual C++ Redistributable)",
            display = ParameterDisplay.NORMAL, allowEmpty = true)
    }

    steps {
        mavenWithPowerShell("Build", "${Mvn.POWERSHELL_NEXUS} ${Mvn.POWERSHELL_BITBUCKET} clean integration-test %maven.arguments%")
    }

    requirements {
        windowsAgent()
    }
})

object BuildForWindowsInDocker : BuildType({
    name = "Build [Win, x64] in Docker"
    description = "mvn clean package in the GraalVM image for Windows, without a deploy"
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
    description = "The macOS (arm64, x86_64), iOS and iOS Simulator archives, without a deploy"
    artifactRules = "*.zip"

    mainRepository()

    steps {
        script {
            name = "Build"
            // set -e: a failed build of one platform fails the step, not only the last one.
            scriptContent = "set -e\n" + Util.prepareMacOS() + """
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
        // The tags of oracle/graal: the GraalVM Community versions of the iOS build, for the arm64 slices and for the
        // x86_64 slice of the simulator, as in prepareMacOS.
        text(
            "jre.ios.graal.tag", "%env.GRAALVM_VERSION%",
            label = "GraalVM tag (arm64)",
            description = "The tag of oracle/graal for the iOS and iOS Simulator arm64 libraries. " +
                "Examples: graal-25.5.5.0.3, jdk-25.0.1",
            allowEmpty = false,
        )
        text(
            "jre.ios.x64.graal.tag", "%env.GRAALVM_VERSION_MACOS_X64%",
            label = "GraalVM tag (x86_64)",
            description = "The tag of oracle/graal for the iOS Simulator x86_64 libraries. Example: jdk-25.0.1",
            allowEmpty = false,
        )
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
                    directory=out/jre-ios/check/$1
                    rm -rf "${'$'}{directory}"
                    mkdir -p "${'$'}{directory}"
                    unzip -q out/jre-ios/sdk/*-$2.zip -d "${'$'}{directory}"
                    arch -arm64 /usr/bin/python3 jre-ios/build.py check --dir "${'$'}{directory}" --platform $1 || status=1
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
        text(
            "env.DETECTED_VS_VERSION", "",
            label = "Detected VS version",
            description = "Set by the build: the version of the first Visual Studio that vswhere lists. " +
                "Example: 17.14.36310.24",
            allowEmpty = true,
        )
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
                        Write-Host "Name:    $(${'$'}vs.displayName)"
                        Write-Host "Version: $(${'$'}vs.installationVersion)"
                        Write-Host "Path:    $(${'$'}vs.installationPath)"
                        Write-Host "---"
                    }

                    ${'$'}primary = ${'$'}installations | Select-Object -First 1
                    Write-Host "##teamcity[setParameter name='env.DETECTED_VS_VERSION' value='$(${'$'}primary.installationVersion)']"
                """.trimIndent()
            }
            formatStderrAsError = true
        }
    }

    requirements {
        windowsAgent()
    }
})

/**
 * Moves the MDAPI tickets of the C++ API (dxfeed-graal-cxx-api on GitHub) through the Jira workflow by its pull
 * requests, as the Jira triggers of Bitbucket do for the internal repositories, and releases the Jira versions of its
 * GitHub releases (see .teamcity/jira-sync/cxx_api_jira_sync.py).
 */
object CxxApiGitGubJiraSync : BuildType({
    name = "[DXFCXX]: GitHub -> Jira Sync & EMail On Release"
    description = "The MDAPI tickets of the pull requests and the Jira versions of the releases of dxfeed-graal-cxx-api"

    maxRunningBuilds = 1

    // The descriptions are shown in the Run dialog (Run with the "..." button), where the values can be changed for a run.
    params {
        select(
            "env.DRY_RUN", "0",
            label = "Dry run",
            description = "1: only read GitHub and Jira and log the changes that the build would make; 0: make them",
            options = listOf("Make the changes" to "0", "Only log the changes" to "1"),
        )
        text(
            "env.MAIL_TO", "anatoly.kalin@devexperts.com",
            label = "Release mail: To",
            description = "The comma-separated recipients of the release mail; empty: no mail. " +
                "Example: dxfeed-team@devexperts.com, someone@devexperts.com",
            allowEmpty = true,
        )
        text(
            "env.MAIL_CC", "",
            label = "Release mail: Cc",
            description = "The comma-separated copies of the release mail. Example: someone@devexperts.com",
            allowEmpty = true,
        )
        text(
            "env.RESEND_MAIL", "",
            label = "Resend the release mail",
            description = "A tag to send the mail of its GitHub release again (to To and Cc above). Example: v8.1.0",
            allowEmpty = true,
        )
        text(
            "env.RELINK", "",
            label = "Relink tickets",
            description = "The comma-separated tickets to link their PRs and branches again, into the groups of the " +
                "branches, with the status of the checks. Example: MDAPI-427, MDAPI-428",
            allowEmpty = true,
        )
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags, "+:.teamcity/jira-sync")
    }

    steps {
        script {
            name = "Sync"
            // GH_TOKEN (env.GH_TOKEN of the project) raises the rate limit of the GitHub API.
            scriptContent = """
                export JIRA_URL="%jira.url%"
                export JIRA_TOKEN="%dxcity.token.jira%"
                python3 .teamcity/jira-sync/cxx_api_jira_sync.py
            """.trimIndent()
        }
    }

    triggers {
        schedule {
            schedulingPolicy = cron {
                seconds = "0"
                minutes = "0/5"
                hours = "*"
                dayOfMonth = "*"
                month = "*"
                dayOfWeek = "?"
            }
            branchFilter = "+:<default>"
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    failureConditions {
        executionTimeoutMin = 5
    }

    requirements {
        linuxAgent()
    }
})

object ListDockerImages : BuildType({
    name = "List Docker Images"
    description = "Lists the tags of the graalvm and nuget images in the Docker registry"

    params {
        text(
            "env.target.repo", "nexus-docker-graalvm.in.devexperts.com",
            label = "Docker registry",
            description = "The registry whose images to list. Example: nexus-docker-graalvm.in.devexperts.com",
            allowEmpty = false,
        )
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
