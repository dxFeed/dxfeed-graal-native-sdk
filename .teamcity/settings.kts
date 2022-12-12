import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
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

version = "2022.10"

project {

    buildType(CreateRelease)
    buildType(DeployRelease)
}

object CreateRelease : BuildType({
    name = "create a release (set the version in the property before running the build)"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:dfbcc5d5-7f92-4eac-8b5d-f8ac38019c50", display = ParameterDisplay.PROMPT)
        text("env.RELEASE_VERSION", "", allowEmpty = false)
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "prepare"
            scriptContent = """
                call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
                cd dxfeed-graal-native-api
                C:\ENV\apache-maven-3.8.6\bin\mvn --batch-mode -DreleaseVersion=%env.RELEASE_VERSION% release:clean release:prepare
            """.trimIndent()
        }
        script {
            name = "release"
            scriptContent = """
                call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
                cd dxfeed-graal-native-api
                C:\ENV\apache-maven-3.8.6\bin\mvn -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% release:perform
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            branchFilter = "+:<default>"
        }
    }

    requirements {
        equals("system.agent.name", "winAgent4450")
    }
})

object DeployRelease : BuildType({
    name = "create a release (automatically increase the version of the path)"

    params {
        text("env.JFROG_USER", "asheifler", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        password("env.JFROG_PASSWORD", "credentialsJSON:dfbcc5d5-7f92-4eac-8b5d-f8ac38019c50", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "prepare"
            scriptContent = """
                call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
                cd dxfeed-graal-native-api
                C:\ENV\apache-maven-3.8.6\bin\mvn -B release:clean release:prepare
            """.trimIndent()
        }
        script {
            name = "release"
            scriptContent = """
                call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
                cd dxfeed-graal-native-api
                C:\ENV\apache-maven-3.8.6\bin\mvn -Djfrog.user=%env.JFROG_USER% -Djfrog.password=%env.JFROG_PASSWORD% release:perform
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            branchFilter = "+:<default>"
        }
    }

    requirements {
        equals("system.agent.name", "winAgent4450")
    }
})
