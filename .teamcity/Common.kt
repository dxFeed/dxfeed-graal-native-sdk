// The parts that the build configurations of settings.kts share. They are in a separate file: the functions declared in
// settings.kts belong to the script class, and the objects of the configurations cannot use them.

import jetbrains.buildServer.configs.kotlin.BuildFeatures
import jetbrains.buildServer.configs.kotlin.BuildStep
import jetbrains.buildServer.configs.kotlin.BuildStepConditions
import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.FailureAction
import jetbrains.buildServer.configs.kotlin.Requirements
import jetbrains.buildServer.configs.kotlin.Triggers
import jetbrains.buildServer.configs.kotlin.buildFeatures.XmlReport
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerRegistryConnections
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildFeatures.xmlReport
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/**
 * The Maven options of the builds: the settings with the repositories, and the Nexus credentials of the dxcity bot.
 * Maven never logs in to Stash: the release plugin neither pushes nor clones (see linuxReleaseSteps).
 */
object Mvn {
    const val SETTINGS = "--settings \".teamcity/settings.xml\""

    // The Nexus repositories (dependencies and deploy).
    const val NEXUS = "-Dnexus.user=%dxcity.namecode.nexus% -Dnexus.password=%dxcity.passcode.nexus%"

    // The public repository of the releases (mdd-maven, see deploy-mdd-maven of the pom).
    const val REPO = "-Drepo.user=%dxcity.namecode.nexus% -Drepo.password=%dxcity.passcode.nexus%"

    // The same for PowerShell, which needs the arguments with "=" in quotes.
    const val POWERSHELL_NEXUS = "\"-Dnexus.user=%dxcity.namecode.nexus%\" \"-Dnexus.password=%dxcity.passcode.nexus%\""
    const val POWERSHELL_REPO = "\"-Drepo.user=%dxcity.namecode.nexus%\" \"-Drepo.password=%dxcity.passcode.nexus%\""

    // The Windows archive linked with the static C runtime, checked in a servercore container (the base image of the
    // Windows build image, already on the agents; Hyper-V isolation on their Windows 11).
    const val POWERSHELL_STATIC_RUNTIME = "\"-DwindowsStaticRuntime=true\" \"-Dwindows.static.runtime.check.image=mcr.microsoft.com/windows/servercore:ltsc2019\""
}

/** The Docker images of the builds, in the Docker registry of Nexus. */
object Images {
    private const val REGISTRY = "nexus-docker-graalvm.in.devexperts.com"
    const val GRAALVM_LINUX_X64 = "$REGISTRY/graalvm:linux-x64-%env.GRAALVM_VERSION%"
    const val GRAALVM_LINUX_AARCH64 = "$REGISTRY/graalvm:linux-aarch64-%env.GRAALVM_VERSION%"
    const val GRAALVM_WINDOWS_X64 = "$REGISTRY/graalvm:win-x64-%env.GRAALVM_VERSION%"
    // The images of the build variants: the same Dockerfiles with Oracle GraalVM (see OracleGraalVM).
    const val GRAALVM_ORACLE_LINUX_X64 = "$REGISTRY/graalvm:linux-x64-${OracleGraalVM.VERSION}"
    const val GRAALVM_ORACLE_LINUX_AARCH64 = "$REGISTRY/graalvm:linux-aarch64-${OracleGraalVM.VERSION}"
    const val CPP_TEST_LINUX_X64 = "$REGISTRY/cpp-test:linux-x64-ol9"
    const val DOCS_CHECK_LINUX_X64 = "$REGISTRY/docs-check:linux-x64-doxygen-${Doxygen.VERSION}"
    const val NUGET = "$REGISTRY/nuget:6.9.1"
}

/**
 * The Oracle GraalVM of the build variants (pom.xml requires Oracle GraalVM of graalvm.version for -Dsdk.variant): the
 * tag of the Linux images and the release on the macOS and Windows agents. The Linux images come from the regular
 * image builds run with env.GRAALVM_VERSION set to this value (.teamcity/README.MD, "Build variants").
 */
object OracleGraalVM {
    const val VERSION = "%env.GRAALVM_VERSION_ORACLE%"
}

/**
 * A platform of the build variants: arch.name of pom.xml, the first part of the classifier; the runtime identifier and
 * the library of the NuGet packages.
 */
enum class VariantPlatform(val archName: String, val title: String, val rid: String, val library: String) {
    LINUX_X64("amd64-linux", "Linux, x64", "linux-x64", "libDxFeedGraalNativeSdk.so"),
    LINUX_AARCH64("aarch64-linux", "Linux, aarch64", "linux-arm64", "libDxFeedGraalNativeSdk.so"),
    MACOS_ARM64("aarch64-osx", "macOS, arm64", "osx-arm64", "libDxFeedGraalNativeSdk.dylib"),
    WINDOWS_X64("amd64-windows", "Windows, x64", "win-x64", "DxFeedGraalNativeSdk.dll");

    /** The library for Pack NuGet: <arch.name>:<runtime identifier>:<file in the archive>. */
    val nugetLibrary: String get() = "$archName:$rid:$library"
}

/**
 * A build variant of the SDK (-Dsdk.variant, "Build variants" in README.md): the archive
 * graal-native-sdk-<version>-<arch.name>-<token>.zip of each of its platforms, published only as a complete set and
 * only with the switch <token>.publish on.
 */
enum class Variant(val token: String, val platforms: List<VariantPlatform>, val ctestArguments: String) {
    ORACLE(
        "oracle",
        listOf(VariantPlatform.LINUX_X64, VariantPlatform.LINUX_AARCH64, VariantPlatform.MACOS_ARM64, VariantPlatform.WINDOWS_X64),
        ""
    ),

    // G1 aborts on a second isolate and on the re-creation of one (one isolate per process): the tests labelled
    // multi-isolate do not run. Oracle documents no G1 on Windows.
    ORACLE_G1(
        "oracle-g1",
        listOf(VariantPlatform.LINUX_X64, VariantPlatform.LINUX_AARCH64, VariantPlatform.MACOS_ARM64),
        " -LE multi-isolate"
    );

    val publishParameter: String get() = "$token.publish"

    /** The arch.name of the platforms, separated by spaces (for a shell loop). */
    val platformList: String get() = platforms.joinToString(" ") { it.archName }

    /** The name of the archive of [platform] in Nexus and in the GitHub release; VERSION is a shell variable of the step. */
    fun archive(platform: String) = "graal-native-sdk-${'$'}{VERSION}-$platform-$token.zip"

    /** The name of the archive of [platform] that mvn package builds in target/: no version (finalName of the pom). */
    fun builtArchive(platform: String) = "graal-native-sdk-$platform-$token.zip"
}

/**
 * A native NuGet package (NuGet/DxFeed.Graal.Native.nuspec, the tokens id, readme and note): DxFeed.Graal.Native from
 * the regular archives, or the package of a build variant from the archives of the variant, packed only when all of
 * them are in Nexus.
 */
enum class NuGetPackage(val id: String, val readme: String, val note: String, val variant: Variant?, val libraries: List<String>) {
    REGULAR(
        "DxFeed.Graal.Native",
        "README.md",
        "",
        null,
        listOf(
            VariantPlatform.LINUX_X64.nugetLibrary,
            VariantPlatform.LINUX_AARCH64.nugetLibrary,
            VariantPlatform.MACOS_ARM64.nugetLibrary,
            // macOS x64 has no build variant.
            "x86_64-osx:osx-x64:libDxFeedGraalNativeSdk.dylib",
            VariantPlatform.WINDOWS_X64.nugetLibrary
        )
    ),
    ORACLE(
        "DxFeed.Graal.Native.Oracle",
        "README-Oracle.md",
        "The build variant oracle: Oracle GraalVM, -O3 and the Serial GC, no macOS x64.",
        Variant.ORACLE,
        Variant.ORACLE.platforms.map { it.nugetLibrary }
    ),
    ORACLE_G1(
        "DxFeed.Graal.Native.Oracle.G1",
        "README-Oracle-G1.md",
        "The build variant oracle-g1: Oracle GraalVM, -O3 and the G1 GC, one isolate per process, Linux and macOS arm64 only.",
        Variant.ORACLE_G1,
        Variant.ORACLE_G1.platforms.map { it.nugetLibrary }
    );

    init {
        // nuget pack -Properties separates the tokens with ';' and the names from the values with '='.
        require(!note.contains(';') && !note.contains('=')) { "The note of $id has ';' or '='" }
    }

    /** The suffix of the archive names after arch.name. */
    val archiveSuffix: String get() = variant?.let { "-${it.token}" } ?: ""

    /** The call of stage_package of the step "Download Artifacts" of Pack NuGet. */
    val stageCommand: String
        get() = "stage_package $id \"$archiveSuffix\" ${variant == null} $readme \"$note\" " + libraries.joinToString(" ")
}

/**
 * What a Test configuration builds and tests: the regular SDK (GraalVM CE; the pull requests and main), a build
 * variant (Oracle GraalVM; main and every night), or a variant at -O0 (every week): the reference for the failures of
 * the variant, since a test that fails at -O3 and passes at -O0 may be a miscompilation. The comparison is by hand.
 */
data class TestKind(val variant: Variant? = null, val optimizeO0: Boolean = false) {
    val nameSuffix: String
        get() = if (variant == null) "" else "[${variant.token}]" + if (optimizeO0) "[O0]" else ""

    private val mavenOptions: List<String>
        get() = listOfNotNull(variant?.let { "-Dsdk.variant=${it.token}" }, if (optimizeO0) "-Dsdk.optimize=O0" else null)

    /** The Maven options of the build, with a leading space ("" for the regular build). */
    val mavenArguments: String get() = mavenOptions.joinToString("") { " $it" }

    /** The same, quoted for PowerShell, which splits an unquoted -Da.b=c at the dot. */
    val powerShellMavenArguments: String get() = mavenOptions.joinToString("") { " \"$it\"" }

    val ctestArguments: String get() = variant?.ctestArguments ?: ""

    val graalvmVersion: String get() = if (variant == null) "%env.GRAALVM_VERSION%" else OracleGraalVM.VERSION

    fun description(regular: String): String = when {
        variant == null -> regular
        optimizeO0 -> "Builds the ${variant.token} library at -O0 (-Dsdk.variant=${variant.token} -Dsdk.optimize=O0) " +
            "and runs the C tests every week: a test that fails in the ${variant.token} tests and passes here may be " +
            "a miscompilation"
        else -> "Builds the ${variant.token} library (-Dsdk.variant=${variant.token}, Oracle GraalVM) and runs the " +
            "C tests for main and every night"
    }

    fun triggers(triggers: Triggers) = when {
        variant == null -> triggers.mainAndPullRequests()
        optimizeO0 -> triggers.weekly()
        else -> triggers.mainAndNightly()
    }
}

/** The Doxygen release of the image that checks the C API documentation (docker/docs-check-linux-x64.Dockerfile). */
object Doxygen {
    const val VERSION = "1.18.0"

    // The SHA-256 of doxygen-<VERSION>.linux.bin.tar.gz of the release on GitHub.
    const val SHA256 = "14fa81bdc34171edb5f1f02b1d60e74802f0439b77fa44e592565d517d72df90"
}

/**
 * CMake of the C tests on macOS and Windows, installed by the build into ~/.graal and C:\BuildCache (the agents have
 * no CMake).
 */
object CMake {
    const val VERSION = "4.4.4"

    // The SHA-256 of cmake-<VERSION>-macos-universal.tar.gz of the release on GitHub (cmake-<VERSION>-SHA-256.txt).
    const val MACOS_SHA256 = "4b7b73704b1db9b374e5c9ab8e17ac6148b817b6396ee75cd94a852cbac9d305"

    // The SHA-256 of cmake-<VERSION>-windows-x86_64.zip of the release on GitHub (cmake-<VERSION>-SHA-256.txt).
    const val WINDOWS_X64_SHA256 = "bace36e94b31c68ab6fa295f26dfa11219e0701cf7c94b0284a7d1cb13dac536"
}

/** git over SSH to the GitHub mirror (Publish GitHub Release, Publish C API Docs to GitHub Pages). */
object GitHub {
    /**
     * The export that makes git check github.com against the host keys of .teamcity/github_known_hosts instead of the
     * known_hosts of the agent: not every Linux agent has github.com there ("Host key verification failed").
     */
    const val SSH_COMMAND = "export GIT_SSH_COMMAND=\"ssh -o UserKnownHostsFile='%teamcity.build.checkoutDir%/.teamcity/github_known_hosts' -o StrictHostKeyChecking=yes\""
}

/**
 * git over SSH to Stash in the release builds, which push the release and the API baseline with the key of dxcity
 * (stashSshAgent): no password of dxcity, which a failed login elsewhere can lock in Bitbucket.
 */
object Stash {
    const val SSH_URL = "ssh://git@stash.in.devexperts.com:7999/mdapi/dxfeed-graal-native-sdk.git"

    /** The export that makes git check Stash against the host key of .teamcity/stash_known_hosts, as GitHub.SSH_COMMAND. */
    const val SSH_COMMAND = "export GIT_SSH_COMMAND=\"ssh -o UserKnownHostsFile='%teamcity.build.checkoutDir%/.teamcity/stash_known_hosts' -o StrictHostKeyChecking=yes\""
}

/** The SSH agent with the key of dxcity, the key of the VCS root, for the git of the steps on the agent (Stash). */
fun BuildFeatures.stashSshAgent() {
    sshAgent {
        teamcitySshKey = "dxcity for GIT"
    }
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

/** The tests run for main and for the pull requests to main. */
fun Triggers.mainAndPullRequests() {
    vcs {
        branchFilter = """
            +:<default>
            +:pull-requests/*
        """.trimIndent()
    }
}

/** The tests of the build variants run for main and every night, not for the pull requests (Oracle -O3 builds are slow). */
fun Triggers.mainAndNightly() {
    vcs {
        branchFilter = "+:<default>"
    }
    schedule {
        schedulingPolicy = daily {
            hour = 2
        }
        branchFilter = "+:<default>"
        triggerBuild = always()
        withPendingChangesOnly = false
    }
}

/** The -O0 reference tests of the build variants run every week for main. */
fun Triggers.weekly() {
    schedule {
        schedulingPolicy = weekly {
            dayOfWeek = ScheduleTrigger.DAY.Sunday
            hour = 3
        }
        branchFilter = "+:<default>"
        triggerBuild = always()
        withPendingChangesOnly = false
    }
}

/**
 * Builds the pull requests to main and reports the result of the build to the pull request in Bitbucket.
 *
 * Both use the refreshable token of the dxcity user that the release management team set up, not the password of
 * dxcity: a failed login elsewhere can lock it in Bitbucket.
 */
fun BuildFeatures.pullRequestsToMain() {
    pullRequests {
        vcsRootExtId = "${SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags.id}"
        provider = bitbucketServer {
            serverUrl = "https://stash.in.devexperts.com"
            authType = storedToken {
                tokenId = DXCITY_BITBUCKET_TOKEN
            }
            filterTargetBranch = "+:refs/heads/main"
            usePullRequestBranches = true
        }
    }

    commitStatusPublisher {
        vcsRootExtId = "${SshGitStashInDevexpertsCom7999mdapiDxfeedGraalNativeSdkGitRefsHeadsMainTags.id}"
        publisher = bitbucketServer {
            url = "%bitbucket.server.url%"
            authType = storedToken {
                tokenId = DXCITY_BITBUCKET_TOKEN
            }
        }
    }
}

/** The refreshable Bitbucket token of the dxcity user in TeamCity (a reference to the token, not its value). */
private const val DXCITY_BITBUCKET_TOKEN =
    "tc_token_id:CID_54855d913f384940c4cc970605eb96ff:60:a566fb5b-bdb7-48d9-a206-0d66eec35762"

/** The results of the C tests (CTest, `--output-junit` in src/main/c/build/conf-release). */
fun BuildFeatures.cTestsReport() {
    xmlReport {
        reportType = XmlReport.XmlReportType.JUNIT
        rules = "src/main/c/build/conf-release/ctest-junit.xml"
    }
}

/**
 * The results of the Java tests (Surefire). Needed where Maven runs inside a script, e.g. a PowerShell step: TeamCity
 * then does not pick the reports up by itself.
 */
fun BuildFeatures.javaTestsReport() {
    xmlReport {
        reportType = XmlReport.XmlReportType.JUNIT
        rules = "target/surefire-reports/*.xml"
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

/** The Linux aarch64 builds run in Docker on the Mac agents (colima). */
fun Requirements.macAgentForLinuxAarch64() {
    macAgent()
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

/** The C API reference that cApiReferenceSteps generates: the build temp directory belongs to the agent user. */
object CApiReference {
    const val WORK = "%system.teamcity.build.tempDir%/c-api-reference"

    /** The HTML of the reference (the c-api-docs.html property of pom.xml, C_API_DOCS_HTML of the Pages script). */
    const val HTML = "$WORK/html"
}

/**
 * Generates the C API reference of a tag with the Doxygen of the docs-check image (the one the pull request check
 * runs) into CApiReference.HTML: the agent takes docs and the headers of the tag with git archive (the image has no
 * git), then Doxygen runs in the image. The work directory is emptied first, so the reference has only the pages of
 * this tag.
 *
 * @param tagCommand the shell command that prints the tag (v3.9.0); the version of the reference is the tag without v
 * @param stepConditions the conditions of both steps, none by default
 */
fun BuildSteps.cApiReferenceSteps(tagCommand: String, stepConditions: BuildStepConditions.() -> Unit = {}) {
    script {
        name = "C API reference: sources of the tag"
        id = "c_api_reference_sources"
        conditions(stepConditions)
        scriptContent = """
            set -eu
            work="${CApiReference.WORK}"
            TAG=$(${tagCommand})
            echo "C API reference of ${'$'}TAG"
            rm -rf "${'$'}work"
            mkdir -p "${'$'}work/src"
            git archive -o "${'$'}work/src.tar" "${'$'}TAG" docs src/main/c/api
            tar -x -f "${'$'}work/src.tar" -C "${'$'}work/src"
            echo "${'$'}{TAG#v}" > "${'$'}work/version"
        """.trimIndent()
        formatStderrAsError = true
    }

    script {
        name = "C API reference: Doxygen in docs-check"
        id = "c_api_reference_doxygen"
        conditions(stepConditions)
        scriptContent = """
            set -eu
            work="${CApiReference.WORK}"
            # The container runs as root: the files go back to the agent user, who created the work directory, so the
            # later steps and the cleanup of the build temp directory can overwrite and delete them.
            owner="${'$'}(stat -c '%u:%g' "${'$'}work")"
            trap 'chown -R "${'$'}owner" "${'$'}work"' EXIT
            doxygen --version
            cd "${'$'}work/src"
            DXFG_DOCS_VERSION="${'$'}(cat "${'$'}work/version")" DXFG_DOCS_OUTPUT="${'$'}work" doxygen docs/Doxyfile
            test -f "${'$'}work/html/index.html"
        """.trimIndent()
        inDocker(Images.DOCS_CHECK_LINUX_X64, "--rm")
    }
}

/**
 * The steps of the release builds of Linux x64: the header of the release notes, release:prepare, the push of the
 * release to Stash, the C API reference of the new tag (cApiReferenceSteps), release:perform (which deploys the release
 * archive and packs that reference), the baseline of the API description for a release (Util.apiBaselineUpdateScript
 * and Util.apiBaselinePushScript), then the debug archive of the tag that the release has made.
 *
 * Maven never logs in to Stash: release:prepare commits and tags without pushing (pushChanges=false), the git of the
 * agent pushes over SSH with the key of dxcity (the build needs stashSshAgent), and release:perform checks the tag out
 * of the local repository (localCheckout=true). The Maven steps run in the GraalVM image, which has no SSH agent.
 *
 * @param tagCommand the shell command that prints the tag of the release (v3.8.0); it runs in the GraalVM image, where
 *        mvn needs Mvn.SETTINGS and Mvn.NEXUS
 * @param prepareArguments the arguments of release:prepare, with a trailing space if any
 */
fun BuildSteps.linuxReleaseSteps(tagCommand: String, prepareArguments: String) {
    script {
        name = "release:prepend tag header to release notes"
        id = "release_prepend_changelog_header"
        scriptContent = Util.releaseNotesHeaderScript(tagCommand)
        formatStderrAsError = true
        // In the image of release:prepare: the Maven and Java of the agent are not the ones of the build.
        inDocker(Images.GRAALVM_LINUX_X64, "--rm")
    }

    script {
        name = "release:prepare in docker"
        id = "release_prepare_in_docker"
        scriptContent = """
            git config --global user.name dxcity
            git config --global user.email dxcity@bots.devexperts.com
            mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} release:clean release:prepare ${prepareArguments}-DpushChanges=false
        """.trimIndent()
        formatStderrAsError = true
        inDocker(Images.GRAALVM_LINUX_X64, "--rm -m 8g")
    }

    script {
        name = "release:push to Stash"
        id = "release_push_to_stash"
        // On the agent, with the SSH agent of the build: the branch of the release (as release:prepare would push it)
        // gets the commits of the release notes header and of release:prepare, the tag is the release.
        scriptContent = """
            set -eu
            ${Stash.SSH_COMMAND}
            BRANCH=$(git rev-parse --abbrev-ref HEAD)
            if [ "${'$'}BRANCH" = HEAD ]; then
                echo "The release is not on a branch (detached HEAD): nothing is pushed."
                exit 1
            fi
            TAG=$(git describe --abbrev=0)
            # The branch first: if it is rejected (a pull request merged meanwhile), the tag is not published either.
            git push "${Stash.SSH_URL}" "HEAD:refs/heads/${'$'}BRANCH"
            git push "${Stash.SSH_URL}" "refs/tags/${'$'}TAG"
        """.trimIndent()
        // Not formatStderrAsError: git writes the progress of a successful push to stderr.
    }

    // The tag that release:prepare has made, as in release:checkout latest tag.
    cApiReferenceSteps("git describe --abbrev=0")

    script {
        name = "release:perform in docker"
        id = "release_perform_in_docker"
        scriptContent = """
            git config --global user.name dxcity
            git config --global user.email dxcity@bots.devexperts.com
            mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} -DlocalCheckout=true \
                -Darguments="-Dc-api-docs -Dc-api-docs.html=${CApiReference.HTML}" release:perform
        """.trimIndent()
        formatStderrAsError = true
        inDocker(Images.GRAALVM_LINUX_X64, "--rm -m 8g")
    }

    script {
        name = "release:update API baseline in docker"
        id = "release_update_api_baseline_in_docker"
        scriptContent = Util.apiBaselineUpdateScript()
        // Not formatStderrAsError: a failure is a warning.
        inDocker(Images.GRAALVM_LINUX_X64, "--rm")
    }

    script {
        name = "release:push API baseline to Stash"
        id = "release_push_api_baseline_to_stash"
        scriptContent = Util.apiBaselinePushScript()
        // Not formatStderrAsError: git writes the progress of a successful push to stderr, and a failure is a warning.
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
            mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} ${Mvn.REPO} clean deploy -P buildDebug
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

/**
 * Runs a PowerShell script on a Windows agent after Util.prepareWinLocal, which installs MSVC, Maven and GraalVM if
 * needed and sets their environment for this step only; [graalvmVersion] is the GraalVM of the step.
 */
fun BuildSteps.powerShellWithWinLocal(
    stepName: String,
    script: String,
    graalvmVersion: String = "%env.GRAALVM_VERSION%"
) {
    powerShell {
        name = stepName
        scriptMode = script {
            content = Util.prepareWinLocal(graalvmVersion) + script
        }
        formatStderrAsError = true
    }
}

/** Runs Maven with PowerShell on a Windows agent (see powerShellWithWinLocal). */
fun BuildSteps.mavenWithPowerShell(
    stepName: String,
    mavenArguments: String,
    graalvmVersion: String = "%env.GRAALVM_VERSION%"
) {
    powerShellWithWinLocal(stepName, """

        mvn ${Mvn.SETTINGS} $mavenArguments
        if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
    """.trimIndent(), graalvmVersion)
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

    /**
     * Makes the description of the API headers of the tag that release:prepare has made the baseline of the API
     * description check and commits it to main; apiBaselinePushScript pushes the commit. A pre-release (v3.13.0-rc1)
     * leaves the baseline as it is. Runs in the GraalVM image: the generator is a multi-file source program, which needs
     * Java 22 or newer.
     *
     * The release is deployed by then, so a failure here only logs a warning: the build and the release chain after it
     * go on, and the builds of main keep warning about the old baseline until it is updated by hand (README.md).
     */
    fun apiBaselineUpdateScript(): String {
        return """
            set -u

            # The container runs as root: .git goes back to the agent user, whose git pushes the commit (and may rebase
            # it) in the next step.
            owner="${'$'}(stat -c '%u:%g' .git)"
            trap 'chown -R "${'$'}owner" .git' EXIT

            (
                set -e

                BRANCH=$(git rev-parse --abbrev-ref HEAD)
                if [ "${'$'}BRANCH" != main ]; then
                    echo "The release is built from ${'$'}BRANCH, not from main: the API baseline stays as it is."
                    exit 0
                fi

                TAG=$(git describe --abbrev=0)
                case "${'$'}TAG" in
                    *-*)
                        echo "${'$'}TAG is a pre-release: the API baseline stays as it is."
                        exit 0
                        ;;
                esac

                # The container runs as root, the checkout belongs to the agent user.
                git config --global --add safe.directory "${'$'}PWD"
                git config --global user.name dxcity
                git config --global user.email dxcity@bots.devexperts.com

                BASELINE=scripts/check-api-headers/baseline/dxfg-api.json
                java scripts/check-api-headers/GenerateApiJson.java --update-baseline "${'$'}TAG" "${'$'}BASELINE"

                if git diff --quiet -- "${'$'}BASELINE"; then
                    echo "The API baseline is already the description of ${'$'}TAG."
                    exit 0
                fi

                git commit -m "Update the API baseline to ${'$'}TAG" -- "${'$'}BASELINE"
            )
            STATUS=${'$'}?

            if [ "${'$'}STATUS" -ne 0 ]; then
                echo "##teamcity[message text='The API baseline was not updated (exit code ${'$'}STATUS): update it by hand, see step 4 of To release a new version in README.md' status='WARNING']"
            fi
        """.trimIndent()
    }

    /**
     * Pushes the commit of apiBaselineUpdateScript to main in Stash, if it made one: on the agent, over SSH with the
     * key of dxcity (Stash, stashSshAgent). As there, a failure only logs a warning.
     */
    fun apiBaselinePushScript(): String {
        return """
            set -u

            (
                set -e

                TAG=$(git describe --abbrev=0)
                BASELINE=scripts/check-api-headers/baseline/dxfg-api.json
                if git diff --quiet "${'$'}TAG" HEAD -- "${'$'}BASELINE"; then
                    echo "No new API baseline since ${'$'}TAG: nothing to push."
                    exit 0
                fi

                ${Stash.SSH_COMMAND}

                # A pull request merged during the release moves main: then the commit goes on top of it.
                if ! git push "${Stash.SSH_URL}" HEAD:refs/heads/main; then
                    git fetch "${Stash.SSH_URL}" main
                    git -c user.name=dxcity -c user.email=dxcity@bots.devexperts.com rebase FETCH_HEAD
                    git push "${Stash.SSH_URL}" HEAD:refs/heads/main
                fi
            )
            STATUS=${'$'}?

            if [ "${'$'}STATUS" -ne 0 ]; then
                # A conflicting rebase would leave the checkout unusable for the next steps.
                git rebase --abort 2>/dev/null || true
                echo "##teamcity[message text='The API baseline was not pushed (exit code ${'$'}STATUS): push it by hand, see step 4 of To release a new version in README.md' status='WARNING']"
            fi
        """.trimIndent()
    }

    private fun prepareMacOSMaven(): String {
        return """
            mvn_version=3.8.9
            mvn_install_path=~/.graal/maven-${'$'}{mvn_version}

            if [ ! -x "${'$'}{mvn_install_path}/bin/mvn" ]; then
                rm -rf "${'$'}{mvn_install_path}"
                .teamcity/scripts/install.sh maven "${'$'}{mvn_version}" "${'$'}{mvn_install_path}"
            fi

            mvn=${'$'}{mvn_install_path}/bin/mvn
"""
    }

    /**
     * Maven and GraalVM [graalvmVersion] for macOS arm64 only (graalvm_arm64_path), for the builds of a variant on a
     * macOS agent: they need no GraalVM for macOS x64.
     */
    fun prepareMacOSArm64(graalvmVersion: String): String {
        return prepareMacOSMaven() + """
            graalvm_arm64_path=~/.graal/$graalvmVersion-osx-arm64
            if [ ! -x "${'$'}{graalvm_arm64_path}/Contents/Home/bin/java" ]; then
                rm -rf "${'$'}{graalvm_arm64_path}"
                .teamcity/scripts/install.sh graalvm "$graalvmVersion" osx-arm64 "${'$'}{graalvm_arm64_path}"
            fi
        """
    }

    fun prepareMacOS(): String {
        return prepareMacOSMaven() + """
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

            # Native Image embeds the trust store of the JDK that builds the image (lib/security/cacerts and
            # blocked.certs). jdk-25.0.1 predates the CA changes of later JDKs (JDK-8395690 removed the eMudhra and
            # Comodo roots, JDK-8395142 blocked the eMudhra CAs G3B), so the x64 builds take both files of the arm64
            # GraalVM; keytool of the x64 one checks that it reads that cacerts.
            for file in cacerts blocked.certs
            do
                cp -f "${'$'}{graalvm_arm64_path}/Contents/Home/lib/security/${'$'}{file}" \
                    "${'$'}{graalvm_x64_path}/Contents/Home/lib/security/${'$'}{file}"
            done
            "${'$'}{graalvm_x64_path}/Contents/Home/bin/keytool" -list -cacerts -storepass changeit > /dev/null
        """
    }

    fun prepareWinLocal(graalvmVersion: String = "%env.GRAALVM_VERSION%"): String {
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
            ${'$'}graalVersion = "$graalvmVersion"
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

/**
 * Test [Linux, x64] of [kind]: builds the library in the GraalVM image and runs the C tests in the test image; the
 * regular build also checks the C API documentation (a variant has the same headers).
 */
fun BuildType.testForLinux(kind: TestKind) {
    name = "Test [Linux, x64]${kind.nameSuffix}"
    description = kind.description("Builds the library, runs the C tests (src/main/c, CTest) and checks the C API documentation for the pull requests to main and for main")

    mainRepository()

    steps {
        script {
            name = "Build the library"
            scriptContent = """
                mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} clean package${kind.mavenArguments}
            """.trimIndent()
            formatStderrAsError = true
            inDocker(if (kind.variant == null) Images.GRAALVM_LINUX_X64 else Images.GRAALVM_ORACLE_LINUX_X64, "--rm -m 8g")
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
                ctest --test-dir build/conf-release -C Release --output-on-failure --output-junit ctest-junit.xml${kind.ctestArguments}
            """.trimIndent()
            inDocker(Images.CPP_TEST_LINUX_X64, "--rm -m 4g")
        }

        if (kind.variant == null) {
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
    }

    triggers {
        kind.triggers(this)
    }

    features {
        if (kind.variant == null) {
            pullRequestsToMain()
        }
        javaTestsReport()
        cTestsReport()
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
}

/** Test [macOS, arm64] of [kind]: builds the library and runs the C tests on the macOS host, without Docker. */
fun BuildType.testForMacOS(kind: TestKind) {
    name = "Test [macOS, arm64]${kind.nameSuffix}"
    description = kind.description("Builds the library and runs the C tests (src/main/c, CTest) on the macOS host, without Docker, for the pull requests to main and for main")

    mainRepository()

    steps {
        script {
            name = "Build the library"
            scriptContent = (if (kind.variant == null) Util.prepareMacOS() else Util.prepareMacOSArm64(kind.graalvmVersion)) + """
                export JAVA_HOME=${'$'}{graalvm_arm64_path}/Contents/Home
                arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} clean package${kind.mavenArguments}
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
                ctest --test-dir build/conf-release -C Release --timeout 300 --output-on-failure --output-junit ctest-junit.xml${kind.ctestArguments}
            """.trimIndent()
        }
    }

    triggers {
        kind.triggers(this)
    }

    features {
        if (kind.variant == null) {
            pullRequestsToMain()
        }
        javaTestsReport()
        cTestsReport()
    }

    failureConditions {
        // The mac agents are shared with the releases: a hung build must not hold one. Oracle -O3 builds take longer.
        executionTimeoutMin = if (kind.variant == null) 30 else 60
    }

    requirements {
        macAgent()
    }
}

/** Test [Windows, x64] of [kind]: builds the library and runs the C tests on the Windows agent, without Docker. */
fun BuildType.testForWindows(kind: TestKind) {
    name = "Test [Windows, x64]${kind.nameSuffix}"
    description = kind.description("Builds the library and runs the C tests (src/main/c, CTest) on the Windows agent, without Docker, for the pull requests to main and for main")

    mainRepository()

    steps {
        mavenWithPowerShell(
            "Build the library",
            "${Mvn.POWERSHELL_NEXUS} clean package${kind.powerShellMavenArguments}",
            kind.graalvmVersion
        )

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
            ctest --test-dir build/conf-release -C Release --timeout 300 --output-on-failure --output-junit ctest-junit.xml${kind.ctestArguments}
            if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
        """.trimIndent(), kind.graalvmVersion)
    }

    triggers {
        kind.triggers(this)
    }

    features {
        if (kind.variant == null) {
            pullRequestsToMain()
        }
        javaTestsReport()
        cTestsReport()
    }

    failureConditions {
        // The Windows agents are shared with the releases: a hung build must not hold one. The first run on an agent
        // without the cache also installs the VS Build Tools. Oracle -O3 builds take longer.
        executionTimeoutMin = if (kind.variant == null) 60 else 90
    }

    requirements {
        windowsAgent()
    }
}

/**
 * Build [<platform>][<variant>] of the release chain: builds the archive of [variant] for [platform] from the latest
 * tag, without a deploy (Deploy [<variant>] decides). Only a successful build copies its archive to staged/, the
 * artifact of the configuration, so a failed build gives the deploy nothing to take.
 */
fun BuildType.variantBuild(variant: Variant, platform: VariantPlatform) {
    name = "Build [${platform.title}][${variant.token}]"
    description = "Builds the ${variant.token} archive (${platform.archName}-${variant.token}) of the latest tag for " +
        "Deploy [${variant.token}]"
    artifactRules = "staged/*.zip"

    mainRepository()

    val archive = "target/${variant.builtArchive(platform.archName)}"
    steps {
        when (platform) {
            VariantPlatform.LINUX_X64, VariantPlatform.LINUX_AARCH64 -> {
                checkoutLatestTag()
                script {
                    name = "Build"
                    scriptContent = """
                        set -e
                        rm -rf staged
                        mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} clean package -Dsdk.variant=${variant.token}
                        mkdir staged
                        cp $archive staged/
                    """.trimIndent()
                    formatStderrAsError = true
                    if (platform == VariantPlatform.LINUX_X64) {
                        inDocker(Images.GRAALVM_ORACLE_LINUX_X64, "--rm -m 8g")
                    } else {
                        inDocker(Images.GRAALVM_ORACLE_LINUX_AARCH64, "--rm")
                    }
                }
            }

            VariantPlatform.MACOS_ARM64 -> {
                checkoutLatestTag()
                script {
                    name = "Build"
                    scriptContent = "set -e\n" + Util.prepareMacOSArm64(OracleGraalVM.VERSION) + """
                        rm -rf staged
                        export JAVA_HOME=${'$'}{graalvm_arm64_path}/Contents/Home
                        arch -arm64 ${'$'}{mvn} ${Mvn.SETTINGS} ${Mvn.NEXUS} clean package -Dsdk.variant=${variant.token}
                        mkdir staged
                        cp $archive staged/
                    """.trimIndent()
                    formatStderrAsError = true
                }
            }

            VariantPlatform.WINDOWS_X64 -> {
                checkoutLatestTagWithPowerShell()
                powerShellWithWinLocal("Build", """

                    if (Test-Path staged) { Remove-Item -LiteralPath staged -Recurse -Force }
                    mvn ${Mvn.SETTINGS} ${Mvn.POWERSHELL_NEXUS} clean package "-Dsdk.variant=${variant.token}"
                    if (${'$'}LASTEXITCODE -ne 0) { exit ${'$'}LASTEXITCODE }
                    New-Item -ItemType Directory staged | Out-Null
                    Copy-Item $archive staged
                """.trimIndent(), OracleGraalVM.VERSION)
            }
        }
    }

    if (platform == VariantPlatform.LINUX_X64 || platform == VariantPlatform.LINUX_AARCH64) {
        features {
            nexusDockerLogin()
        }
    }

    failureConditions {
        // The agents are shared with the regular release, which waits for the variant builds.
        executionTimeoutMin = 60
    }

    requirements {
        when (platform) {
            VariantPlatform.LINUX_X64 -> linuxAgent()
            VariantPlatform.LINUX_AARCH64 -> macAgentForLinuxAarch64()
            VariantPlatform.MACOS_ARM64 -> macAgent()
            VariantPlatform.WINDOWS_X64 -> windowsAgent()
        }
    }
}

/**
 * Deploy [<variant>] of the release chain: takes the archives of the variant [builds] (one per platform of the variant,
 * in their order) and, with the switch
 * <variant>.publish on, deploys them to Nexus under the release version (classifier <arch.name>-<variant>), where
 * Publish GitHub Release takes them. It runs after the regular deploys of the platforms ([regularDeploys]), so a variant
 * is never in Nexus without its release.
 *
 * Only a complete set is deployed. A failed variant build leaves the set incomplete: the deploy then skips the variant
 * with a warning and the release goes on without it. An archive that Nexus already has is skipped, so a rerun after a
 * failed upload deploys the rest.
 */
fun BuildType.variantDeploy(variant: Variant, builds: List<BuildType>, regularDeploys: List<BuildType>) {
    require(builds.size == variant.platforms.size) { "One build per platform of ${variant.token}, in their order" }
    name = "Deploy [${variant.token}]"
    description = "After the regular deploys: deploys the ${variant.token} archives of the latest tag to Nexus when " +
        "${variant.publishParameter} is on and all of them are built"

    mainRepository()

    steps {
        checkoutLatestTag()

        script {
            name = "Deploy"
            // git runs in the container as another user than the owner of the checkout, hence safe.directory.
            scriptContent = """
                set -eu
                TAG=$(git -c safe.directory='*' describe --abbrev=0)
                VERSION=${'$'}{TAG#v}
                BASE_URL="https://maven.in.devexperts.com/repository/qd/com/dxfeed/graal-native-sdk/${'$'}{VERSION}"

                missing=""
                for platform in ${variant.platformList}; do
                    if [ ! -f "staged/${'$'}{platform}/${variant.builtArchive("${'$'}{platform}")}" ]; then
                        missing="${'$'}{missing} ${'$'}{platform}"
                    fi
                done
                if [ -n "${'$'}{missing}" ]; then
                    echo "##teamcity[message text='No ${variant.token} archive for:${'$'}{missing}. Nothing is deployed, the release goes on without ${variant.token}' status='WARNING']"
                    echo "##teamcity[buildStatus text='${variant.token} set incomplete, nothing deployed']"
                    exit 0
                fi

                if [ "%${variant.publishParameter}%" != "true" ]; then
                    echo "${variant.publishParameter} is off: the ${variant.token} archives are not deployed"
                    echo "##teamcity[buildStatus text='${variant.publishParameter} off, nothing deployed']"
                    exit 0
                fi

                for platform in ${variant.platformList}; do
                    archive="${variant.archive("${'$'}{platform}")}"
                    if curl --head --fail --location --silent --retry 3 --max-time 60 --output /dev/null "${'$'}{BASE_URL}/${'$'}{archive}"; then
                        echo "${'$'}{archive} is already in Nexus: skipped"
                        continue
                    fi
                    # The repository of distributionManagement of the pom (qd), with the credentials of .teamcity/settings.xml.
                    mvn ${Mvn.SETTINGS} ${Mvn.NEXUS} org.apache.maven.plugins:maven-deploy-plugin:3.2.0:deploy-file -DrepositoryId=qd -Durl=https://maven.in.devexperts.com/content/repositories/qd/ -Dfile="staged/${'$'}{platform}/${variant.builtArchive("${'$'}{platform}")}" -DgroupId=com.dxfeed -DartifactId=graal-native-sdk -Dversion="${'$'}{VERSION}" -Dclassifier="${'$'}{platform}-${variant.token}" -Dpackaging=zip -DgeneratePom=false
                done
            """.trimIndent()
            formatStderrAsError = true
            inDocker(Images.GRAALVM_LINUX_X64, "--rm")
        }
    }

    dependencies {
        // A failed or cancelled variant build does not stop the deploy: it sees the incomplete set and skips the variant.
        variant.platforms.zip(builds).forEach { (platform, build) ->
            dependency(build) {
                snapshot {
                    onDependencyFailure = FailureAction.IGNORE
                    onDependencyCancel = FailureAction.IGNORE
                }
                artifacts {
                    cleanDestination = true
                    artifactRules = "?:*.zip => staged/${platform.archName}"
                }
            }
        }
        regularDeploys.forEach { deploy ->
            snapshot(deploy) {
                onDependencyFailure = FailureAction.CANCEL
            }
        }
    }

    failureConditions {
        // Build & Deploy [All] waits for this config: a hung upload must not hold the release chain.
        executionTimeoutMin = 30
    }

    features {
        nexusDockerLogin()
    }

    requirements {
        linuxAgent()
    }
}
