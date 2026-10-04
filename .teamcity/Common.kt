// The parts that the build configurations of settings.kts share. They are in a separate file: the functions declared in
// settings.kts belong to the script class, and the objects of the configurations cannot use them.

import jetbrains.buildServer.configs.kotlin.BuildFeatures
import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.Requirements
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerRegistryConnections
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/** The Maven options of the builds: the settings with the repositories, and the credentials of the dxcity bot. */
object Mvn {
    const val SETTINGS = "--settings \".teamcity/settings.xml\""

    // The Nexus repositories (dependencies and deploy).
    const val NEXUS = "-Dnexus.user=%dxcity.namecode.nexus% -Dnexus.password=%dxcity.passcode.nexus%"

    // The public repository of the releases (mdd-maven, see deploy-mdd-maven of the pom).
    const val REPO = "-Drepo.user=%dxcity.namecode.nexus% -Drepo.password=%dxcity.passcode.nexus%"

    // The SCM credentials of Maven (Bitbucket), for the release plugin.
    const val BITBUCKET = "-Dusername=dxcity -Dpassword=%dxcity.token.bitbucket%"

    // The same for PowerShell, which needs the arguments with "=" in quotes.
    const val POWERSHELL_NEXUS = "\"-Dnexus.user=%dxcity.namecode.nexus%\" \"-Dnexus.password=%dxcity.passcode.nexus%\""
    const val POWERSHELL_REPO = "\"-Drepo.user=%dxcity.namecode.nexus%\" \"-Drepo.password=%dxcity.passcode.nexus%\""
    const val POWERSHELL_BITBUCKET = "\"-Dusername=dxcity\" \"-Dpassword=%dxcity.token.bitbucket%\""
}

/** The Docker images of the builds, in the Docker registry of Nexus. */
object Images {
    private const val REGISTRY = "nexus-docker-graalvm.in.devexperts.com"
    const val GRAALVM_LINUX_X64 = "$REGISTRY/graalvm:linux-x64-%env.GRAALVM_VERSION%"
    const val GRAALVM_LINUX_AARCH64 = "$REGISTRY/graalvm:linux-aarch64-%env.GRAALVM_VERSION%"
    const val GRAALVM_WINDOWS_X64 = "$REGISTRY/graalvm:win-x64-%env.GRAALVM_VERSION%"
    const val CPP_TEST_LINUX_X64 = "$REGISTRY/cpp-test:linux-x64-ol9"
    const val NUGET = "$REGISTRY/nuget:6.9.1"
}

/** The repository of the SDK (the name of its object is its id in TeamCity). */
fun BuildType.mainRepository() {
    vcs {
        root(SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags)
    }
}

/** Runs the step in the Docker image (see Images). */
fun ScriptBuildStep.inDocker(
    image: String,
    runParameters: String,
    platform: ScriptBuildStep.ImagePlatform = ScriptBuildStep.ImagePlatform.Linux
) {
    dockerImage = image
    dockerImagePlatform = platform
    dockerRunParameters = runParameters
}

/** Logs in to the Docker registry of Nexus (the project feature NEXUS) for the images of the steps. */
fun BuildFeatures.nexusDockerLogin() {
    dockerRegistryConnections {
        loginToRegistry = on {
            dockerRegistryId = "NEXUS"
        }
    }
}

/** The release builds and the deploys check out the tag that the release has just made. */
fun BuildSteps.checkoutLatestTag() {
    script {
        name = "Checkout Latest Tag"
        scriptContent = "git checkout $(git describe --abbrev=0)"
    }
}

/** The same with PowerShell, on the Windows agents. */
fun BuildSteps.checkoutLatestTagWithPowerShell() {
    powerShell {
        name = "Checkout Latest Tag"
        scriptMode = script {
            content = """
                ${'$'}tag = git describe --abbrev=0
                git checkout ${'$'}tag
            """.trimIndent()
        }
    }
}

fun Requirements.linuxAgent() {
    equals("teamcity.agent.jvm.os.name", "Linux")
}

fun Requirements.macAgent() {
    equals("teamcity.agent.jvm.os.name", "Mac OS X")
}

fun Requirements.windowsAgent() {
    startsWith("teamcity.agent.jvm.os.name", "Windows")
}

/** The Linux aarch64 builds run in Docker on the Mac agents (colima); macbuilder23 failed them most often. */
fun Requirements.macAgentForLinuxAarch64() {
    macAgent()
    doesNotEqual("system.agent.name", "macbuilder23")
}

/** The repository of the SDK: the main branch and the tags (the name of the object is its id in TeamCity). */
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

/**
 * The steps of the release builds of Linux x64: the header of the release notes, release:prepare and release:perform
 * (which deploys the release archive), then the debug archive of the tag that the release has made.
 *
 * @param tagCommand the shell command that prints the tag of the release (v3.8.0)
 * @param prepareArguments the arguments of release:prepare before the credentials, with a trailing space if any
 */
fun BuildSteps.linuxReleaseSteps(tagCommand: String, prepareArguments: String) {
    script {
        name = "release:prepend tag header to release notes"
        id = "release_prepend_changelog_header"
        scriptContent = Util.releaseNotesHeaderScript(tagCommand)
        formatStderrAsError = true
    }

    script {
        name = "release:prepare in docker"
        id = "release_prepare_in_docker"
        scriptContent = """
            git config --global user.name dxcity
            git config --global user.email dxcity@bots.devexperts.com
            mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} release:clean release:prepare $prepareArguments${Mvn.BITBUCKET}
        """.trimIndent()
        formatStderrAsError = true
        inDocker(Images.GRAALVM_LINUX_X64, "--rm -m 8g")
    }

    script {
        name = "release:perform in docker"
        id = "release_perform_in_docker"
        scriptContent = """
            git config --global user.name dxcity
            git config --global user.email dxcity@bots.devexperts.com
            mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} ${Mvn.BITBUCKET} -Darguments=-Dc-api-docs release:perform
        """.trimIndent()
        formatStderrAsError = true
        inDocker(Images.GRAALVM_LINUX_X64, "--rm -m 8g")
    }

    script {
        name = "release:checkout latest tag"
        id = "release_checkout_latest_tag"
        scriptContent = "git checkout $(git describe --abbrev=0)"
    }

    script {
        name = "release:deploy debug"
        id = "release_deploy_debug"
        scriptContent = """
            mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} ${Mvn.BITBUCKET} clean deploy -P buildDebug
        """.trimIndent()
        formatStderrAsError = true
        inDocker(Images.GRAALVM_LINUX_X64, "--rm -m 8g")
    }
}

/** Builds a Docker image with a Dockerfile of .teamcity/docker (the build context is .teamcity) and pushes it to the Docker registry of Nexus. */
fun BuildSteps.buildAndPushDockerImage(image: String, buildArguments: String) {
    script {
        name = "Build"
        scriptContent = """
            cd .teamcity
            docker images --all
            docker build --pull -t $image $buildArguments
            docker push $image
            docker rmi -f $image
        """.trimIndent()
        formatStderrAsError = true
    }
}

/** Runs Maven with PowerShell on a Windows agent (Util.prepareWinLocal installs MSVC, Maven and GraalVM if needed). */
fun BuildSteps.mavenWithPowerShell(stepName: String, mavenArguments: String) {
    powerShell {
        name = stepName
        scriptMode = script {
            content = Util.prepareWinLocal() + """

                mvn ${Mvn.SETTINGS} $mavenArguments
                if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
            """.trimIndent()
        }
        formatStderrAsError = true
    }
}

/** The shell and PowerShell scripts that the build configurations share. */
object Util {
    fun releaseNotesHeaderScript(tagCommand: String): String {
        return """
            set -eu

            TAG=$(${tagCommand})
            echo "Release tag: ${'$'}TAG"

            if grep -qxF "## ${'$'}TAG" ReleaseNotes.md; then
                echo "Header '## ${'$'}TAG' already present in ReleaseNotes.md, skipping."
                exit 0
            fi

            # The headers of the pre-releases of the same version at the top (## v3.6.0-rc1 for v3.6.0-rc2 or v3.6.0)
            # are replaced by the new header, so the section of the release lists all the changes since the previous
            # release.
            BASE=${'$'}{TAG%%-*}

            : > ReleaseNotes.md.tmp
            awk -v base="${'$'}BASE" -v tag="${'$'}TAG" -v out=ReleaseNotes.md.tmp '
                BEGIN {
                    top = 1
                }

                top && /^## / {
                    header = substr($0, 4)

                    if (index(header, base "-") == 1 && header != tag) {
                        print "Replacing the header of the pre-release: " $0
                        skipBlank = 1
                        next
                    }

                    top = 0
                }

                skipBlank && $0 == "" {
                    skipBlank = 0
                    next
                }

                {
                    skipBlank = 0
                    print > out
                }
            ' ReleaseNotes.md

            { printf '## %s\n\n' "${'$'}TAG"; cat ReleaseNotes.md.tmp; } > ReleaseNotes.md
            rm ReleaseNotes.md.tmp

            git config user.name "dxcity"
            git config user.email "dxcity@bots.devexperts.com"

            git add ReleaseNotes.md
            git commit -m "Add release notes header for ${'$'}TAG"
        """.trimIndent()
    }

    fun prepareMacOS(): String {
        return """
            mvn_version=3.8.9
            mvn_install_path=~/.graal/maven-${'$'}{mvn_version}

            if [ ! -x "${'$'}{mvn_install_path}/bin/mvn" ]; then
                rm -rf "${'$'}{mvn_install_path}"
                .teamcity/scripts/install.sh maven "${'$'}{mvn_version}" "${'$'}{mvn_install_path}"
            fi

            mvn=${'$'}{mvn_install_path}/bin/mvn

            # GraalVM for macOS x64 is no longer published after jdk-25.0.1,
            # so the x64 (and iOS simulator) builds use their own version.
            graalvm_arm64_path=~/.graal/%env.GRAALVM_VERSION%-osx-arm64
            graalvm_x64_path=~/.graal/%env.GRAALVM_VERSION_MACOS_X64%-osx-x64
            for entry in "%env.GRAALVM_VERSION% osx-arm64 ${'$'}{graalvm_arm64_path}" "%env.GRAALVM_VERSION_MACOS_X64% osx-x64 ${'$'}{graalvm_x64_path}"
            do
                set -- ${'$'}{entry}
                if [ ! -x "$3/Contents/Home/bin/java" ]; then
                    rm -rf "$3"
                    .teamcity/scripts/install.sh graalvm "$1" "$2" "$3"
                fi
            done
        """
    }

    fun prepareWinLocal(): String {
        return """
            ${'$'}ErrorActionPreference = 'Stop'

            . .teamcity\scripts\install.ps1

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
                if (${'$'}line -match '^(?<k>[^=]+)=(?<v>.*)$') {
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
