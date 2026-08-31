import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerRegistryConnections
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

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
        param("env.GRAALVM_VERSION", "jdk-23.0.2")
        text("env.JFROG_USER", "anatoly.kalin", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:435755aa-d8b4-4841-baf2-3cf7748cbc10", display = ParameterDisplay.HIDDEN)
        password("env.NUGETORG_API_KEY", "credentialsJSON:4ba447c3-64f4-4a4c-8ff8-505258ddd420", display = ParameterDisplay.HIDDEN)
        password("env.GH_TOKEN", "credentialsJSON:657ea93a-c18d-414f-be39-e12fb36fb13f", display = ParameterDisplay.HIDDEN)
    }

    features {
        dockerRegistry {
            id = "PROJECT_EXT_153"
            name = "dxFeed jFrog Docker Registry"
            url = "https://dxfeed-docker.jfrog.io"
            userName = "graal"
            password = "credentialsJSON:0de10768-dff5-49c2-8610-1ee72c8fdb09"
        }

        dockerRegistry {
            id = "NEXUS"
            name = "Nexus"
            url = "https://nexus-docker-graalvm.in.devexperts.com"
            userName = "%dxcity.login%"
            password = "%dxcity.password%"
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
    buildType(BuildAndDeployForAll)
    buildType(DeployNuget)
    buildType(SyncGitHubWithMainAndPublishRelease)
    buildType(BuildForLinux)
    buildType(BuildForWindows)
    buildType(BuildForWindowsInDocker)
    buildType(BuildForMacOSAndIOS)
    buildType(BuildForLinuxAarch64)
    buildType(BuildAndPushDockerImageForLinuxX64)
    buildType(BuildAndPushDockerImageForLinuxAarch64)
    buildType(BuildAndPushDockerImageForWindowsX64)

    buildType(CopyServiceImages)
    buildType(ListServiceImages)

    buildType(DetectVisualStudioVersion)
}

object BuildPatchAndDeployForLinux : BuildType({
    name = "Build PATCH & Deploy [Linux, x64]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "release:prepend tag header to release notes"
            id = "release_prepend_changelog_header"
            scriptContent = Util.releaseNotesHeaderScript(
                    """
                    VERSION=${'$'}(mvn help:evaluate \
                        -Dexpression=project.version \
                        -q \
                        -DforceStdout)
                    VERSION=${'$'}{VERSION%-SNAPSHOT}
                    echo "v${'$'}VERSION"
                    """.trimIndent())
            formatStderrAsError = true
        }

        script {
            name = "release:prepare in docker"
            id = "release_prepare_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% release:clean release:prepare -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket%
                """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8g"
        }

        script {
            name = "release:perform in docker"
            id = "release_perform_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% release:perform
                """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8g"
        }

        script {
            name = "release:checkout latest tag"
            id = "release_checkout_latest_tag"
            scriptContent = "git checkout ${'$'}(git describe --abbrev=0)"
        }

        script {
            name = "release:deploy debug"
            id = "release_deploy_debug"
            scriptContent = """
                mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% clean deploy -P buildDebug
            """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8g"
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object BuildMajorMinorPatchAndDeployLinux : BuildType({
    name = "Build MAJOR.MINOR.PATCH & Deploy [Linux, x64]"
    artifactRules = "*.zip"

    params {
        text("env.RELEASE_VERSION", "", allowEmpty = false)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "release:prepend tag header to release notes"
            id = "release_prepend_changelog_header"
            scriptContent = Util.releaseNotesHeaderScript("echo v%env.RELEASE_VERSION%")
            formatStderrAsError = true
        }

        script {
            name = "release:prepare in docker"
            id = "release_prepare_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% release:clean release:prepare --batch-mode -DreleaseVersion=%env.RELEASE_VERSION%  -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket%
                """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8g"
        }

        script {
            name = "release:perform in docker"
            id = "release_perform_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% release:perform
                """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8g"
        }

        script {
            name = "release:checkout latest tag"
            id = "release_checkout_latest_tag"
            scriptContent = "git checkout ${'$'}(git describe --abbrev=0)"
        }

        script {
            name = "release:deploy debug"
            id = "release_deploy_debug"
            scriptContent = """
                mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% clean deploy -P buildDebug
            """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8g"
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object BuildAndDeployForLinuxAarch64Release : BuildType({
    name = "Build & Deploy [Linux, aarch64][Release]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Checkout Latest Tag"
            scriptContent = "git checkout ${'$'}(git describe --abbrev=0)"
        }

        script {
            name = "Deploy"
            scriptContent = """
                mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% clean deploy
            """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-aarch64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm"
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
        doesNotEqual("system.agent.name", "macbuilder23")
    }
})

object BuildAndDeployForLinuxAarch64Debug : BuildType({
    name = "Build & Deploy [Linux, aarch64][Debug]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Checkout Latest Tag"
            scriptContent = "git checkout ${'$'}(git describe --abbrev=0)"
        }

        script {
            name = "Deploy Debug"
            scriptContent = """
                mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% clean deploy -P buildDebug
            """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-aarch64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm"
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
        doesNotEqual("system.agent.name", "macbuilder23")
    }
})

object BuildAndDeployForLinuxAarch64 : BuildType({
    name = "Build & Deploy [Linux, aarch64]"
    artifactRules = "*.zip"
    type = Type.COMPOSITE

    dependencies {
        snapshot(BuildAndDeployForLinuxAarch64Release) {
            onDependencyFailure = jetbrains.buildServer.configs.kotlin.FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForLinuxAarch64Debug) {
            onDependencyFailure = jetbrains.buildServer.configs.kotlin.FailureAction.IGNORE
        }
    }
})

object BuildAndDeployForWindowsRelease : BuildType({
    name = "Build & Deploy [Windows, x64][Release]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        powerShell {
            name = "Checkout Latest Tag"
            scriptMode = script {
                content = """
                    ${'$'}tag = git describe --abbrev=0
                    git checkout ${'$'}tag
                """.trimIndent()
            }
        }
        powerShell {
            name = "Deploy"
            scriptMode = script {
                content = Util.prepareWinLocal() + """

                    mvn --settings ".teamcity/settings.xml" "-Djfrog.user=%env.JFROG_USER%" "-Djfrog.password=%env.JFROG_PASSWORD%" "-Dnexus.user=%dxcity.login%" "-Dnexus.password=%dxcity.password%" "-Dusername=%dxcity.login%" "-Dpassword=%dxcity.token.bitbucket%" clean deploy
                    if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
                """.trimIndent()
            }
            formatStderrAsError = true
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
    }
})

object BuildAndDeployForWindowsDebug : BuildType({
    name = "Build & Deploy [Windows, x64][Debug]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }
    steps {
        powerShell {
            name = "Checkout Latest Tag"
            scriptMode = script {
                content = """
                    ${'$'}tag = git describe --abbrev=0
                    git checkout ${'$'}tag
                """.trimIndent()
            }
        }
        powerShell {
            name = "Deploy"
            scriptMode = script {
                content = Util.prepareWinLocal() + """

                    mvn --settings ".teamcity/settings.xml" "-Djfrog.user=%env.JFROG_USER%" "-Djfrog.password=%env.JFROG_PASSWORD%" "-Dnexus.user=%dxcity.login%" "-Dnexus.password=%dxcity.password%" "-Dusername=%dxcity.login%" "-Dpassword=%dxcity.token.bitbucket%" clean deploy -P buildDebug
                    if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
                """.trimIndent()
            }
            formatStderrAsError = true
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
    }
})

object BuildAndDeployForWindows : BuildType({
    name = "Build & Deploy [Windows]"
    type = Type.COMPOSITE
    artifactRules = "*.zip"

    dependencies {
        snapshot(BuildAndDeployForWindowsRelease) {
            onDependencyFailure = jetbrains.buildServer.configs.kotlin.FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForWindowsDebug) {
            onDependencyFailure = jetbrains.buildServer.configs.kotlin.FailureAction.IGNORE
        }
    }
})

object BuildAndDeployForMacOsAndIOS : BuildType({
    name = "Build & Deploy [macOS, iOS]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Checkout Latest Tag"
            scriptContent = "git checkout ${'$'}(git describe --abbrev=0)"
        }

        script {
            name = "Deploy"
            scriptContent = Util.prepareMacOS() + """
                export JAVA_HOME=${'$'}{graalvm_install_path}-osx-arm64/Contents/Home
                arch -arm64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% clean deploy
                arch -arm64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -DmacIos=true clean deploy
                export JAVA_HOME=${'$'}{graalvm_install_path}-osx-x64/Contents/Home
                arch -x86_64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -DmacIosSimulator=true deploy
                arch -x86_64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% clean deploy
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
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
            onDependencyFailure = jetbrains.buildServer.configs.kotlin.FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForWindows) {
            onDependencyFailure = jetbrains.buildServer.configs.kotlin.FailureAction.CANCEL
        }
        snapshot(BuildAndDeployForMacOsAndIOS) {
            onDependencyFailure = jetbrains.buildServer.configs.kotlin.FailureAction.CANCEL
        }
    }
})

object DeployNuget : BuildType({
    name = "Deploy NuGet"
    artifactRules = "*.nupkg"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

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
                nuget push DxFeed.Graal.Native.${'$'}VERSION.nupkg -Source https://api.nuget.org/v3/index.json -ApiKey %env.NUGETORG_API_KEY% -SkipDuplicate
            """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/nuget:6.9.1"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8g"
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
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
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
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object SyncGitHubWithMainAndPublishRelease : BuildType({
    name = "Publish GitHub Release"

    artifactRules = """
        release-artifacts/*.zip
        release-notes.md
    """.trimIndent()

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

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
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object BuildForLinux : BuildType({
    name = "Build [Linux, x64]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Build"
            scriptContent = """
                mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% clean package
            """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8g"
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object BuildAndPushDockerImageForLinuxX64 : BuildType({
    name = "Build & Push a Docker Image [Linux, x64]"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    val image = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-%env.GRAALVM_VERSION%"

    steps {
        script {
            name = "Build"
            scriptContent = """
                cd .teamcity
                docker images --all
                docker build --pull -t $image --build-arg GRAALVM_VERSION="%env.GRAALVM_VERSION%" -f graalvm-linux-x64.Dockerfile .
                docker push $image
                docker rmi -f $image
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object BuildAndPushDockerImageForLinuxAarch64 : BuildType({
    name = "Build & Push a Docker Image [Linux, aarch64]"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    val image = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-aarch64-%env.GRAALVM_VERSION%"

    steps {
        script {
            name = "Build"
            scriptContent = """
                cd .teamcity
                docker images --all
                docker build --pull -t $image --build-arg GRAALVM_VERSION="%env.GRAALVM_VERSION%" -f graalvm-linux-aarch64.Dockerfile .
                docker push $image
                docker rmi -f $image
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
    }
})

object BuildForLinuxAarch64 : BuildType({
    name = "Build [Linux, aarch64]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Build"
            scriptContent = """
                mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% clean package
            """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:linux-aarch64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm"
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
        doesNotEqual("system.agent.name", "macbuilder23")
    }
})

object BuildAndPushDockerImageForWindowsX64 : BuildType({
    name = "Build & Push a Docker Image [Windows, x64]"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    val image = "nexus-docker-graalvm.in.devexperts.com/graalvm:win-x64-%env.GRAALVM_VERSION%"

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
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
    }
})

object BuildForWindows : BuildType({
    name = "Build [Windows, x64]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        powerShell {
            name = "Build"
            scriptMode = script {
                content = Util.prepareWinLocal() + """

                    mvn --settings ".teamcity/settings.xml" "-Djfrog.user=%env.JFROG_USER%" "-Djfrog.password=%env.JFROG_PASSWORD%" "-Dnexus.user=%dxcity.login%" "-Dnexus.password=%dxcity.password%" "-Dusername=%dxcity.login%" "-Dpassword=%dxcity.token.bitbucket%" clean package
                    if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
                """.trimIndent()
            }
            formatStderrAsError = true
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
    }
})

object BuildForWindowsInDocker : BuildType({
    name = "Build [Windows, x64] in Docker"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Build"
            scriptContent = """
            mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% clean package
        """.trimIndent()
            formatStderrAsError = true
            dockerImage = "nexus-docker-graalvm.in.devexperts.com/graalvm:win-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Windows
            dockerRunParameters = "--rm -m 8g"
        }
    }

    features {
        dockerRegistryConnections {
            loginToRegistry = on {
                dockerRegistryId = "NEXUS"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
    }
})

object BuildForMacOSAndIOS : BuildType({
    name = "Build [macOS, iOS]"
    artifactRules = "*.zip"

    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Build"
            scriptContent = Util.prepareMacOS() + """
                export JAVA_HOME=${'$'}{graalvm_install_path}-osx-arm64/Contents/Home
                arch -arm64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% clean package
                arch -arm64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -DmacIos=true clean package
                export JAVA_HOME=${'$'}{graalvm_install_path}-osx-x64/Contents/Home
                arch -x86_64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% -DmacIosSimulator=true package
                arch -x86_64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dnexus.user=%dxcity.login% -Dnexus.password=%dxcity.password% clean package
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
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
        startsWith("teamcity.agent.jvm.os.name", "Windows")
    }
})

object Util {
    fun releaseNotesHeaderScript(tagCommand: String): String {
        return """
            set -eu

            TAG=${'$'}(${tagCommand})
            echo "Release tag: ${'$'}TAG"

            if grep -qxF "## ${'$'}TAG" ReleaseNotes.md; then
                echo "Header '## ${'$'}TAG' already present in ReleaseNotes.md, skipping."
                exit 0
            fi

            { printf '## %s\n\n' "${'$'}TAG"; cat ReleaseNotes.md; } > ReleaseNotes.md.tmp
            mv ReleaseNotes.md.tmp ReleaseNotes.md

            git config user.name "%dxcity.login%"
            git config user.email "%dxcity.login%@bots.devexperts.com"

            git add ReleaseNotes.md
            git commit -m "Add release notes header for ${'$'}TAG"
        """.trimIndent()
    }

    fun prepareWin(): String {
        return """
            set TMP=C:\Users\ContainerAdministrator\AppData\Local\Temp
            set TEMP=C:\Users\ContainerAdministrator\AppData\Local\Temp
            call C:\BuildTools\Common7\Tools\VsDevCmd.bat -arch=amd64
        """
    }

    fun prepareMacOS(): String {
        return """
            mvn_version=3.8.9
            mvn_install_path=~/.graal/maven-${'$'}{mvn_version}

            if [ ! -x "${'$'}{mvn_install_path}/bin/mvn" ]; then
                rm -rf "${'$'}{mvn_install_path}"
                .teamcity/install.sh maven "${'$'}{mvn_version}" "${'$'}{mvn_install_path}"
            fi

            mvn=${'$'}{mvn_install_path}/bin/mvn

            graalvm_version=%env.GRAALVM_VERSION%
            graalvm_install_path=~/.graal/${'$'}{graalvm_version}
            declare -a platforms=("osx-x64" "osx-arm64")
            for platform in "${'$'}{platforms[@]}"
            do
                graalvm_full_install_path="${'$'}{graalvm_install_path}-${'$'}{platform}"
                if [ ! -x "${'$'}{graalvm_full_install_path}/Contents/Home/bin/java" ]; then
                    rm -rf "${'$'}{graalvm_full_install_path}"
                    .teamcity/install.sh graalvm "${'$'}{graalvm_version}" "${'$'}{platform}" "${'$'}{graalvm_full_install_path}"
                fi
            done
        """
    }

    fun waitForNexus(): String {
        return """
            for i in 1 2 3 4 5; do
              getent hosts nexus.in.devexperts.com && break
              echo "DNS not ready yet, retrying..."
              sleep 2
            done
        """.trimIndent()
    }

    fun prepareWinLocal(): String {
        return """
            ${'$'}ErrorActionPreference = 'Stop'

            . .teamcity\install.ps1

            # --- 1. Download and cache VS Build Tools 2022 if not already installed ---
            ${'$'}vsInstallPath = "C:\BuildCache\vs-buildtools-2022"
            ${'$'}vsDevCmd = Join-Path ${'$'}vsInstallPath "Common7\Tools\VsDevCmd.bat"

            if (-not (Test-Path ${'$'}vsDevCmd)) {
                Write-Host "VS Build Tools 2022 not found in cache, installing..."
                Install-VSBuildTools -Version "17" -InstallPath ${'$'}vsInstallPath
            } else {
                Write-Host "VS Build Tools 2022 found in cache, skipping installation"
            }

            if (-not (Test-Path ${'$'}vsDevCmd)) {
                throw "VS Build Tools installation failed - VsDevCmd.bat still not found at ${'$'}vsDevCmd"
            }

            # --- 2. Import MSVC environment variables from the installed VS 2022 ---
            ${'$'}envDump = cmd /c "`"${'$'}vsDevCmd`" -arch=amd64 && set"
            foreach (${'$'}line in ${'$'}envDump) {
                if (${'$'}line -match '^(?<k>[^=]+)=(?<v>.*)${'$'}') {
                    [System.Environment]::SetEnvironmentVariable(${'$'}Matches.k, ${'$'}Matches.v, "Process")
                }
            }

            # --- 3. Download Maven if not already cached ---
            ${'$'}mvnVersion = "3.8.9"
            ${'$'}mvnInstallPath = "C:\BuildCache\maven-${'$'}mvnVersion"
            if (-not (Test-Path "${'$'}mvnInstallPath\bin\mvn.cmd")) {
                Write-Host "Maven ${'$'}mvnVersion not found in cache, installing..."
                Install-Maven -Version ${'$'}mvnVersion -InstallPath ${'$'}mvnInstallPath
            } else {
                Write-Host "Maven ${'$'}mvnVersion found in cache, skipping installation"
            }
            ${'$'}env:Path = "${'$'}mvnInstallPath\bin;${'$'}env:Path"

            # --- 4. Download GraalVM if not already cached ---
            ${'$'}graalVersion = "%env.GRAALVM_VERSION%"
            ${'$'}graalInstallPath = "C:\BuildCache\graalvm-${'$'}graalVersion-win-x64"
            if (-not (Test-Path "${'$'}graalInstallPath\bin\java.exe")) {
                Write-Host "GraalVM ${'$'}graalVersion not found in cache, installing..."
                Install-GraalVM -Version ${'$'}graalVersion -Platform "win-x64" -InstallPath ${'$'}graalInstallPath
            } else {
                Write-Host "GraalVM ${'$'}graalVersion found in cache, skipping installation"
            }
            ${'$'}env:JAVA_HOME = ${'$'}graalInstallPath
            ${'$'}env:Path = "${'$'}graalInstallPath\bin;${'$'}env:Path"

            where.exe cl
            where.exe java
            where.exe mvn
        """.trimIndent()
    }
}

object SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags : GitVcsRoot({
    name = "ssh://git@stash.in.devexperts.com:7999/mdapi/dxfeed-graal-native-sdk.git#refs/heads/main tags"
    url = "ssh://git@stash.in.devexperts.com:7999/mdapi/dxfeed-graal-native-sdk.git"
    branch = "refs/heads/main"
    branchSpec = """
        +:refs/tags/*
        +:refs/heads/*
    """.trimIndent()
    useTagsAsBranches = true
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "dxcity for GIT"
    }
    param("secure:password", "")
})

object CopyServiceImages : BuildType({
    name = "Copy service images"

    params {
        password("env.jfrogPass", "credentialsJSON:d288798f-47b9-4fbb-8463-a68be694481d")
        param("env.srcRepo", "dxfeed-docker.jfrog.io/dxfeed-api/nuget:6.9.1")
        param("env.target.repo", "nexus-docker-graalvm.in.devexperts.com")
    }

    vcs {
        root(RelativeId("SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags"))
    }

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
                #  --dest-creds %dxcity.login%:%dxcity.password% \
                #  docker://%env.srcRepo% docker://%env.target.repo%/${'$'}image_tag

                echo "List images nuget:"
                skopeo list-tags \
                  --creds %dxcity.login%:%dxcity.password% \
                docker://%env.target.repo%/nuget

                echo "List images graal:"
                skopeo list-tags \
                  --creds %dxcity.login%:%dxcity.password% \
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

    vcs {
        root(RelativeId("SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags"))
    }

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
                  --creds %dxcity.login%:%dxcity.password% \
                docker://%env.target.repo%/nuget

                echo "List images graal:"
                skopeo list-tags \
                  --creds %dxcity.login%:%dxcity.password% \
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