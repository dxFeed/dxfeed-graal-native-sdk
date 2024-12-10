import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.ui.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry

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

version = "2024.03"

project {
    vcsRoot(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)

    params {
        param("env.GRAALVM_VERSION", "jdk-22.0.1")
    }

    features {
        dockerRegistry {
            id = "PROJECT_EXT_153"
            name = "dxFeed jFrog Docker Registry"
            url = "https://dxfeed-docker.jfrog.io"
            userName = "graal"
            password = "credentialsJSON:0de10768-dff5-49c2-8610-1ee72c8fdb09"
        }
    }

    buildType(BuildPatchAndDeployLinux)
    buildType(BuildMajorMinorPatchAndDeployLinux)
    // buildType(DeployForLinuxAarch64)
    buildType(DeployWindows)
    buildType(DeployMacOsAndIOS)
    buildType(DeployNuget)
    buildType(SyncGitHubWithMain)
    buildType(BuildForLinux)
    buildType(BuildForWindows)
    buildType(BuildForMacOSAndIOS)
    buildType(BuildAndPushDockerImageForLinuxAarch64)
    buildType(BuildForLinuxAarch64)
}

object BuildPatchAndDeployLinux : BuildType({
    name = "Build PATCH and Deploy Linux"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "release:prepare in docker"
            id = "release_prepare_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn release:clean release:prepare -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket%
                """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8GB"
        }
        script {
            name = "release:perform in docker"
            id = "release_perform_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% release:perform
                """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8GB"
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_153"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object BuildMajorMinorPatchAndDeployLinux : BuildType({
    name = "Build MAJOR.MINOR.PATCH and Deploy Linux"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
        text("env.RELEASE_VERSION", "", allowEmpty = false)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "release:prepare in docker"
            id = "release_prepare_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn release:clean release:prepare --batch-mode -DreleaseVersion=%env.RELEASE_VERSION%  -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket%
                """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8GB"
        }
        script {
            name = "release:perform in docker"
            id = "release_perform_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% release:perform
                """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8GB"
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_153"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object DeployForLinuxAarch64 : BuildType({
    name = "Deploy for Linux Aarch64"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
        param("env.DOCKER_MEMORY_SIZE", "4G")
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Checkout Latest Tag"
            scriptContent = "git checkout ${'$'}(git describe --abbrev=0)"
        }

        script {
            name = "Deploy"
            scriptContent = """
                mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% deploy
            """.trimIndent()
            formatStderrAsError = true

            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-aarch64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m %env.DOCKER_MEMORY_SIZE%"
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${BuildPatchAndDeployLinux.id}"
            successfulOnly = true
        }
        finishBuildTrigger {
            buildType = "${BuildMajorMinorPatchAndDeployLinux.id}"
            successfulOnly = true
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_153"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
        equals("teamcity.agent.hostname", "macbuilder20")
    }
})

object DeployWindows : BuildType({
    name = "Deploy Windows"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
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
        script {
            name = "Deploy"
            scriptContent = Util.prepareWin() + """
                mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% deploy
            """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Windows
            dockerRunParameters = "--rm -m 4GB"
        }
    }
    //DeployForLinuxAarch64

    triggers {
        finishBuildTrigger {
            buildType = "${DeployForLinuxAarch64.id}"
            successfulOnly = true
        }
//        finishBuildTrigger {
//            buildType = "${BuildPatchAndDeployLinux.id}"
//            successfulOnly = true
//        }
//        finishBuildTrigger {
//            buildType = "${BuildMajorMinorPatchAndDeployLinux.id}"
//            successfulOnly = true
//        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_153"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Windows 11")
    }
})

object DeployMacOsAndIOS : BuildType({
    name = "Deploy macOS and iOS"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
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
                arch -arm64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% clean deploy
                arch -arm64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -DmacIos=true clean deploy
                export JAVA_HOME=${'$'}{graalvm_install_path}-osx-x64/Contents/Home
                arch -x86_64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -DmacIosSimulator=true deploy
                arch -x86_64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% clean deploy
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${DeployWindows.id}"
            successfulOnly = true
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
    }
})

object DeployNuget : BuildType({
    name = "Deploy NuGet"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
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

                  base_url="https://dxfeed.jfrog.io/artifactory/maven-open/com/dxfeed/graal-native-sdk"
                  file_path="${'$'}{version}/graal-native-sdk-${'$'}{version}-${'$'}{platform}-${'$'}{os}.${'$'}{extension}"
                  url="${'$'}{base_url}/${'$'}{file_path}!/${'$'}{file_name}"

                  mkdir -p "${'$'}path_to_save"
                  if ! (cd "${'$'}path_to_save" && curl -LO -f "${'$'}url"); then
                    echo "Failed to download: ${'$'}url"
                    exit 1
                  fi
                }

                version=${'$'}(git describe --abbrev=0)
                version=${'$'}{version#"v"}

                download_file "${'$'}version" "NuGet/runtimes/linux-x64/native" "libDxFeedGraalNativeSdk.so" "linux" "amd64"
                download_file "${'$'}version" "NuGet/runtimes/linux-arm64/native" "libDxFeedGraalNativeSdk.so" "linux" "aarch64"
                download_file "${'$'}version" "NuGet/runtimes/osx-arm64/native" "libDxFeedGraalNativeSdk.dylib" "osx" "aarch64"
                download_file "${'$'}version" "NuGet/runtimes/osx-x64/native" "libDxFeedGraalNativeSdk.dylib" "osx" "x86_64"
                download_file "${'$'}version" "NuGet/runtimes/win-x64/native" "DxFeedGraalNativeSdk.dll" "windows" "amd64"
            """.trimIndent()
        }
        script {
            name = "NuGet Pack and Deploy"
            scriptContent = """
                git config --global safe.directory '*'
                VERSION=${'$'}(git describe --abbrev=0)
                VERSION=${'$'}{VERSION#"v"}
                nuget pack NuGet/DxFeed.Graal.Native.nuspec -Version ${'$'}VERSION
                nuget push DxFeed.Graal.Native.${'$'}VERSION.nupkg -Source https://dxfeed.jfrog.io/artifactory/api/nuget/nuget-open/com/dxfeed/graal-native/${'$'}VERSION -ApiKey %env.JFROG_USER%:%env.JFROG_PASSWORD%
            """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/nuget:6.9.1"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8GB"
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${DeployMacOsAndIOS.id}"
            successfulOnly = true
            enforceCleanCheckout = true
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_153"
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

object SyncGitHubWithMain : BuildType({
    name = "Sync GitHub with 'main'"

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            scriptContent = "git push --follow-tags git@github.com:dxFeed/dxfeed-graal-native-sdk.git main"
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
            teamcitySshKey = "id_ed25519"
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object BuildForLinux : BuildType({
    name = "Build for Linux"

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Build"
            scriptContent = """
                mvn clean package
            """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m 8GB"
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_153"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Linux")
    }
})

object BuildAndPushDockerImageForLinuxAarch64 : BuildType({
    name = "Build and push a docker image for Linux Aarch64"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Build"
            scriptContent = """
                cd .teamcity
                docker images --all
                docker rmi -f ${'$'}(docker images -aq)
                docker images --all
                docker login dxfeed-docker.jfrog.io --username %env.JFROG_USER% --password %env.JFROG_PASSWORD%
                docker build -t dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-aarch64-%env.GRAALVM_VERSION% --build-arg GRAALVM_VERSION="%env.GRAALVM_VERSION%" -f graalvm-linux-aarch64.Dockerfile .
                docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-aarch64-%env.GRAALVM_VERSION%
                docker images --all
                docker rmi -f ${'$'}(docker images -aq)
                docker logout
            """.trimIndent()
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
    }
})

object BuildForLinuxAarch64 : BuildType({
    name = "Build for Linux Aarch64"

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    params {
        param("env.DOCKER_MEMORY_SIZE", "4G")
    }

    steps {
        script {
            name = "Build"
            scriptContent = """
                mvn clean package
            """.trimIndent()
            formatStderrAsError = true

            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-aarch64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "--rm -m %env.DOCKER_MEMORY_SIZE%"
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_153"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
        equals("teamcity.agent.hostname", "macbuilder20")
    }
})

object BuildForWindows : BuildType({
    name = "Build for Windows"

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Build"
            scriptContent = Util.prepareWin() + """
                mvn clean package
            """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-%env.GRAALVM_VERSION%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Windows
            dockerRunParameters = "--rm -m 4GB"
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_153"
            }
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Windows 11")
    }
})

object BuildForMacOSAndIOS : BuildType({
    name = "Build for macOS and iOS"

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "Build"
            scriptContent = Util.prepareMacOS() + """
                export JAVA_HOME=${'$'}{graalvm_install_path}-osx-arm64/Contents/Home
                arch -arm64 ${'$'}{mvn} --settings ".teamcity/settings.xml" clean package
                arch -arm64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -DmacIos=true clean package
                export JAVA_HOME=${'$'}{graalvm_install_path}-osx-x64/Contents/Home
                arch -x86_64 ${'$'}{mvn} --settings ".teamcity/settings.xml" -DmacIosSimulator=true package
                arch -x86_64 ${'$'}{mvn} --settings ".teamcity/settings.xml" clean package
            """.trimIndent()
        }
    }

    requirements {
        equals("teamcity.agent.jvm.os.name", "Mac OS X")
    }
})

object Util {
    fun prepareWin(): String {
        return """
            set TMP=C:\Users\ContainerAdministrator\AppData\Local\Temp
            set TEMP=C:\Users\ContainerAdministrator\AppData\Local\Temp
            call C:\BuildTools\Common7\Tools\VsDevCmd.bat -arch=amd64
        """
    }

    fun prepareMacOS(): String {
        return """
            mvn_version=3.8.8
            mvn_install_path=~/.graal/maven-${'$'}{mvn_version}
            if [ ! -d "${'$'}{mvn_install_path}" ]; then
                .teamcity/install.sh maven "${'$'}{mvn_version}" "${'$'}{mvn_install_path}"
            fi
            mvn=${'$'}{mvn_install_path}/bin/mvn

            graalvm_version=%env.GRAALVM_VERSION%
            graalvm_install_path=~/.graal/${'$'}{graalvm_version}
            declare -a platforms=("osx-x64" "osx-arm64")
            for platform in "${'$'}{platforms[@]}"
            do
                graalvm_full_install_path="${'$'}{graalvm_install_path}-${'$'}{platform}"
                if [ ! -d "${'$'}{graalvm_full_install_path}" ]; then
                    .teamcity/install.sh graalvm "${'$'}{graalvm_version}" "${'$'}{platform}" "${'$'}{graalvm_full_install_path}"
                fi
            done
        """
    }
}

object SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags : GitVcsRoot({
    name = "ssh://git@stash.in.devexperts.com:7999/mdapi/dxfeed-graal-native-sdk.git#refs/heads/main tags"
    url = "ssh://git@stash.in.devexperts.com:7999/mdapi/dxfeed-graal-native-sdk.git"
    branch = "refs/heads/main"
    branchSpec = "+:refs/tags/*"
    useTagsAsBranches = true
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "dxcity for GIT"
    }
    param("secure:password", "")
})
