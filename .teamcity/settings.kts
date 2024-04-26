import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
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

version = "2023.05"

project {

    vcsRoot(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)

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

    buildType(SyncGitHubWithMain)

    buildType(DeployWindows)
    buildType(DeployMacAndIOS)
    buildType(DeployNuget)
}

object BuildPatchAndDeployLinux : BuildType({
    name = "build PATCH and deploy linux"

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
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-jdk-22.0.1"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "-m 8GB"
        }
        script {
            name = "release:perform in docker"
            id = "release_perform_in_docker"
            scriptContent = """
                    git config --global user.name %dxcity.login%
                    git config --global user.email %dxcity.login%@bots.devexperts.com
                    mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -Dusername=%dxcity.login% -Dpassword=%dxcity.token.bitbucket% release:perform
                """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-jdk-22.0.1"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "-m 8GB"
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
    name = "build MAJOR.MINOR.PATCH and deploy linux"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
        text("env.RELEASE_VERSION", "", allowEmpty = false)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        maven {
            name = "release:prepare"
            goals = "release:clean release:prepare"
            runnerArgs = "--batch-mode -DreleaseVersion=%env.RELEASE_VERSION%"
            mavenVersion = custom {
                path = "%teamcity.tool.maven.3.8.4%"
            }
            userSettingsPath = "settings-agent.xml"
            jdkHome = "/opt/jdks/graalvm-community-openjdk-22.0.1+8.1"
        }
        maven {
            name = "release:perform"
            goals = "release:perform"
            runnerArgs = """--settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD%"""
            mavenVersion = custom {
                path = "%teamcity.tool.maven.3.8.4%"
            }
            userSettingsPath = "settings-agent.xml"
            jdkHome = "/opt/jdks/graalvm-community-openjdk-22.0.1+8.1"
        }
    }

    requirements {
        equals("system.agent.name", "dxfeedAgent5919-1")
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
            buildType = "${BuildPatchAndDeployLinux.id}"
            successfulOnly = true
        }
        finishBuildTrigger {
            buildType = "${BuildMajorMinorPatchAndDeployLinux.id}"
            successfulOnly = true
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "id_ed25519"
        }
    }

    requirements {
        equals("system.agent.name", "dxfeedAgent5919-1")
    }
})

object DeployWindows : BuildType({
    name = "deploy windows"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        powerShell {
            name = "checkout latest tag"
            scriptMode = script {
                content = """
                    ${'$'}tag = git describe --abbrev=0
                    git checkout ${'$'}tag
                """.trimIndent()
            }
        }
        script {
            name = "deploy"
            scriptContent = """
set TMP=C:\Users\ContainerAdministrator\AppData\Local\Temp
set TEMP=C:\Users\ContainerAdministrator\AppData\Local\Temp
call C:\BuildTools\Common7\Tools\VsDevCmd.bat -arch=amd64
mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% deploy
            """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-jdk-22.0.1"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Windows
            dockerRunParameters = "-m 8GB"
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
        equals("system.agent.name", "winAgent5168")
    }
})

object DeployMacAndIOS : BuildType({
    name = "deploy osx"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "git checkout latest tag"
            scriptContent = "git checkout ${'$'}(git describe --abbrev=0)"
        }
        script {
            name = "deploy"
            scriptContent = """
                export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22.0.1+8.1/Contents/Home
                arch -arm64 /Users/dxcity/apache-maven-3.8.8/bin/mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% clean deploy
                arch -arm64 /Users/dxcity/apache-maven-3.8.8/bin/mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -DmacIos=true clean deploy
                export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22.0.1+8.1-amd64/Contents/Home
                arch -x86_64 /Users/dxcity/apache-maven-3.8.8/bin/mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -DmacIosSimulator=true deploy
                arch -x86_64 /Users/dxcity/apache-maven-3.8.8/bin/mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% clean deploy
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
        equals("system.agent.name", "macbuilder22")
    }
})

object DeployNuget : BuildType({
    name = "deploy nuget"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:086ca686-63eb-4b78-bc09-c11a44a41bcb", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "download artifacts"
            scriptContent = """
                VERSION=${'$'}(git describe --abbrev=0)
                VERSION=${'$'}{VERSION#"v"}

                PATH_TO_SAVE=NuGet/runtimes/linux-x64/native
                LINK=https://dxfeed.jfrog.io/artifactory/maven-open/com/dxfeed/graal-native-sdk/${'$'}VERSION/graal-native-sdk-${'$'}VERSION-amd64-linux.zip\!/libDxFeedGraalNativeSdk.so
                STATUS=${'$'}(curl -o /dev/null -s -w "%{http_code}\n" ${'$'}LINK)
                if [ ${'$'}STATUS -eq 200 ]; then (cd ${'$'}PATH_TO_SAVE && curl -LO "${'$'}LINK"); else rm -R ${'$'}PATH_TO_SAVE; fi

                PATH_TO_SAVE=NuGet/runtimes/osx-arm64/native
                LINK=https://dxfeed.jfrog.io/artifactory/maven-open/com/dxfeed/graal-native-sdk/${'$'}VERSION/graal-native-sdk-${'$'}VERSION-aarch64-osx.zip\!/libDxFeedGraalNativeSdk.dylib
                STATUS=${'$'}(curl -o /dev/null -s -w "%{http_code}\n" ${'$'}LINK)
                if [ ${'$'}STATUS -eq 200 ]; then (cd ${'$'}PATH_TO_SAVE && curl -LO "${'$'}LINK"); else rm -R ${'$'}PATH_TO_SAVE; fi

                PATH_TO_SAVE=NuGet/runtimes/osx-x64/native
                LINK=https://dxfeed.jfrog.io/artifactory/maven-open/com/dxfeed/graal-native-sdk/${'$'}VERSION/graal-native-sdk-${'$'}VERSION-x86_64-osx.zip\!/libDxFeedGraalNativeSdk.dylib
                STATUS=${'$'}(curl -o /dev/null -s -w "%{http_code}\n" ${'$'}LINK)
                if [ ${'$'}STATUS -eq 200 ]; then (cd ${'$'}PATH_TO_SAVE && curl -LO "${'$'}LINK"); else rm -R ${'$'}PATH_TO_SAVE; fi

                PATH_TO_SAVE=NuGet/runtimes/win-x64/native
                LINK=https://dxfeed.jfrog.io/artifactory/maven-open/com/dxfeed/graal-native-sdk/${'$'}VERSION/graal-native-sdk-${'$'}VERSION-amd64-windows.zip\!/DxFeedGraalNativeSdk.dll
                STATUS=${'$'}(curl -o /dev/null -s -w "%{http_code}\n" ${'$'}LINK)
                if [ ${'$'}STATUS -eq 200 ]; then (cd ${'$'}PATH_TO_SAVE && curl -LO "${'$'}LINK"); else rm -R ${'$'}PATH_TO_SAVE; fi
                LINK=https://dxfeed.jfrog.io/artifactory/maven-open/com/dxfeed/graal-native-sdk/${'$'}VERSION/graal-native-sdk-${'$'}VERSION-amd64-windows.zip\!/sunmscapi.dll
                STATUS=${'$'}(curl -o /dev/null -s -w "%{http_code}\n" ${'$'}LINK)
                if [ ${'$'}STATUS -eq 200 ]; then (cd ${'$'}PATH_TO_SAVE && curl -LO "${'$'}LINK"); else rm -R ${'$'}PATH_TO_SAVE; fi
            """.trimIndent()
        }
        script {
            name = "nuget pack and deploy"
            scriptContent = """
                git config --global safe.directory '*'
                VERSION=${'$'}(git describe --abbrev=0)
                VERSION=${'$'}{VERSION#"v"}
                nuget pack NuGet/DxFeed.Graal.Native.nuspec -Version ${'$'}VERSION
                nuget push DxFeed.Graal.Native.${'$'}VERSION.nupkg -Source https://dxfeed.jfrog.io/artifactory/api/nuget/nuget-open/com/dxfeed/graal-native/${'$'}VERSION -ApiKey %env.JFROG_USER%:%env.JFROG_PASSWORD%
            """.trimIndent()
            dockerImage = "dxfeed-docker.jfrog.io/dxfeed-api/nuget:6.9.1"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "-m 8GB"
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${DeployMacAndIOS.id}"
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

object SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags : GitVcsRoot({
    name = "ssh://git@stash.in.devexperts.com:7999/en/dxfeed-graal-native-sdk.git#refs/heads/main tags"
    url = "ssh://git@stash.in.devexperts.com:7999/en/dxfeed-graal-native-sdk.git"
    branch = "refs/heads/main"
    branchSpec = "+:refs/tags/*"
    useTagsAsBranches = true
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "dxcity for GIT"
    }
    param("secure:password", "")
})
