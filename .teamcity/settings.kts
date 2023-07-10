import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.script
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

version = "2022.10"

project {

    vcsRoot(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)

    buildType(BuildNuget)
    buildType(DeployWindows)
    buildType(TestBuildOsx)
    buildType(BuildMajorMinorPatch)
    buildType(AutomaticDeploymentOfTheOsxArtifact)
    buildType(BuildPatch)
}

object SyncGitHubWithMain : BuildType({
    name = "Sync GitHub with 'main'"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            scriptContent = "git push --follow-tags git@github.com:dxFeed/dxfeed-graal-native-sdk.git main"
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "Eugenics_DxfeedGraalNativeApi_BuildPatch"
            successfulOnly = true
        }
        finishBuildTrigger {
            buildType = "Eugenics_DxfeedGraalNativeApi_BuildMajorMinorPatch"
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
}))

object AutomaticDeploymentOfTheOsxArtifact : BuildType({
    name = "deploy osx"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:dfbcc5d5-7f92-4eac-8b5d-f8ac38019c50", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        script {
            name = "git checkout latest tag"
            scriptContent = "git checkout ${'$'}(git describe --abbrev=0)"
        }
        maven {
            name = "deploy"
            enabled = false
            goals = "deploy"
            runnerArgs = """--settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD%"""
            mavenVersion = custom {
                path = "%teamcity.tool.maven.3.8.4%"
            }
            jdkHome = "/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1-arm64/Contents/Home"
        }
        script {
            name = "deploy (1)"
            scriptContent = """
                export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1-arm64/Contents/Home
                arch -arm64 /Users/dxcity/apache-maven-3.8.8/bin/mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% clean deploy
                arch -arm64 /Users/dxcity/apache-maven-3.8.8/bin/mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -DmacIos=true clean deploy
                export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1/Contents/Home
                arch -x86_64 /Users/dxcity/apache-maven-3.8.8/bin/mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% clean deploy
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${BuildPatch.id}"
            successfulOnly = true
        }
        finishBuildTrigger {
            buildType = "${BuildMajorMinorPatch.id}"
            successfulOnly = true
        }
    }

    requirements {
        equals("system.agent.name", "macbuilder22")
    }
})

object BuildMajorMinorPatch : BuildType({
    name = "build MAJOR.MINOR.PATCH and deploy linux"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:dfbcc5d5-7f92-4eac-8b5d-f8ac38019c50", display = ParameterDisplay.HIDDEN)
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
            jdkHome = "/opt/jdks/graalvm-ce-java11-22.3.1"
        }
        maven {
            name = "release:perform"
            goals = "release:perform"
            runnerArgs = """--settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD%"""
            mavenVersion = custom {
                path = "%teamcity.tool.maven.3.8.4%"
            }
            userSettingsPath = "settings-agent.xml"
            jdkHome = "/opt/jdks/graalvm-ce-java11-22.3.1"
        }
    }

    requirements {
        equals("system.agent.name", "dxfeedAgent5919-1")
    }
})

object BuildNuget : BuildType({
    name = "deploy nuget"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:16401ff9-f8e0-4c26-b367-e828c39a01c3", display = ParameterDisplay.HIDDEN)
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
                VERSION=${'$'}(git describe --abbrev=0)
                VERSION=${'$'}{VERSION#"v"}
                echo ${'$'}VERSION
                nuget pack NuGet/DxFeed.Graal.Native.nuspec -Version ${'$'}VERSION
                jf rt upload DxFeed.Graal.Native.${'$'}VERSION.nupkg nuget-open/com/dxfeed/graal-native/${'$'}VERSION/DxFeed.Graal.Native.${'$'}VERSION.nupkg --url https://dxfeed.jfrog.io/artifactory --access-token %env.JFROG_PASSWORD%
                cp DxFeed.Graal.Native.${'$'}VERSION.nupkg /mnt/projects/mdd/DXFG/Release/${'$'}VERSION/DxFeed.Graal.Native.${'$'}VERSION.nupkg
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${AutomaticDeploymentOfTheOsxArtifact.id}"
            successfulOnly = true

            enforceCleanCheckout = true
        }
    }

    features {
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
        equals("system.agent.name", "dxfeedAgent5919-1")
    }
})

object BuildPatch : BuildType({
    name = "build PATCH and deploy linux"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:dfbcc5d5-7f92-4eac-8b5d-f8ac38019c50", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        maven {
            name = "release:prepare"
            goals = "release:clean release:prepare"
            runnerArgs = "-B"
            mavenVersion = custom {
                path = "%teamcity.tool.maven.3.8.4%"
            }
            userSettingsPath = "settings-agent.xml"
            jdkHome = "/opt/jdks/graalvm-ce-java11-22.3.1"
        }
        maven {
            name = "release:perform"
            goals = "release:perform"
            runnerArgs = """--settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD%"""
            mavenVersion = custom {
                path = "%teamcity.tool.maven.3.8.4%"
            }
            userSettingsPath = "settings-agent.xml"
            jdkHome = "/opt/jdks/graalvm-ce-java11-22.3.1"
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
        password("env.JFROG_PASSWORD", "credentialsJSON:dfbcc5d5-7f92-4eac-8b5d-f8ac38019c50", display = ParameterDisplay.HIDDEN)
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
                call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
                set JAVA_HOME=C:\Release\graalvm-ce-java11-22.3.1
                C:\ENV\apache-maven-3.8.6\bin\mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% deploy
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${BuildPatch.id}"
            successfulOnly = true
        }
        finishBuildTrigger {
            buildType = "${BuildMajorMinorPatch.id}"
            successfulOnly = true
        }
    }

    requirements {
        equals("system.agent.name", "winAgent4450")
    }
})

object TestBuildOsx : BuildType({
    name = "test build osx"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:dfbcc5d5-7f92-4eac-8b5d-f8ac38019c50", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(SshGitStashInDevexpertsCom7999enDxfeedGraalNativeApiGitRefsHeadsMainTags)
    }

    steps {
        maven {
            name = "package"
            enabled = false
            goals = "package"
            runnerArgs = """--settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD%"""
            mavenVersion = custom {
                path = "%teamcity.tool.maven.3.8.4%"
            }
            jdkHome = "/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0-arm64/Contents/Home"
        }
        script {
            name = "package (1)"
            scriptContent = """
                arch -arm64 bash
                arch
                export JAVA_HOME=`/usr/libexec/java_home -a arm64`
                echo ${'$'}JAVA_HOME
                mvn -version
                file ${'$'}JAVA_HOME/bin/java
                /usr/bin/cc -v
                arch -arm64 uname -m
                arch -arm64 id -un
                xcode-select --print-path
                export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0-arm64/Contents/Home
                arch -arm64 mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% -DmacIos=true package
                export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0-arm64/Contents/Home
                echo ${'$'}JAVA_HOME
                arch -arm64 mvn -version
                arch -arm64 mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% package
                export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0/Contents/Home
                echo ${'$'}JAVA_HOME
                arch -x86_64 mvn -version
                arch -x86_64 mvn --settings ".teamcity/settings.xml" -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% package
            """.trimIndent()
        }
    }

    requirements {
        equals("system.agent.name", "macbuilder22")
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
