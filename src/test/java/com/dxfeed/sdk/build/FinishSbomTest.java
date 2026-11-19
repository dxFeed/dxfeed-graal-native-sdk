// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * src/build/FinishSbom.java, run as the pom runs it (a single-file program with Jackson on the classpath): the GraalVM
 * and JDK components, the removal of the GraalVM jars and of the external references, and the failures.
 */
public class FinishSbomTest {
    private static final Path PROGRAM = Path.of("src", "build", "FinishSbom.java");
    private static final Path JRE_IOS_BUILD_INFO = Path.of("jre-ios", "build-info.txt");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String MAVEN_SBOM = """
        {
          "bomFormat": "CycloneDX",
          "specVersion": "1.6",
          "metadata": {
            "component": {
              "bom-ref": "pkg:maven/com.dxfeed/graal-native-sdk@1.0?type=jar",
              "purl": "pkg:maven/com.dxfeed/graal-native-sdk@1.0?type=jar",
              "externalReferences": [{ "type": "vcs", "url": "https://devexperts.com/sdk.git" }]
            }
          },
          "components": [
            {
              "bom-ref": "pkg:maven/com.example/lib@2.0?type=jar",
              "purl": "pkg:maven/com.example/lib@2.0?type=jar",
              "externalReferences": [{ "type": "distribution", "url": "https://devexperts.com/maven" }]
            },
            {
              "bom-ref": "pkg:maven/org.graalvm.sdk/nativeimage@25.0.1?type=jar",
              "purl": "pkg:maven/org.graalvm.sdk/nativeimage@25.0.1?type=jar"
            }
          ],
          "dependencies": [
            {
              "ref": "pkg:maven/com.dxfeed/graal-native-sdk@1.0?type=jar",
              "dependsOn": ["pkg:maven/com.example/lib@2.0?type=jar", "pkg:maven/org.graalvm.sdk/nativeimage@25.0.1?type=jar"]
            },
            { "ref": "pkg:maven/com.example/lib@2.0?type=jar", "dependsOn": [] },
            { "ref": "pkg:maven/org.graalvm.sdk/nativeimage@25.0.1?type=jar", "dependsOn": [] }
          ]
        }
        """;

    private static final String BUILD_INFO = """
        target=amd64-windows
        variant=default
        graalvm.implementor=GraalVM Community
        graalvm.version=25.5.5.0.3
        graalvm.java-version=25.0.5.0.3+1
        """;

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void releaseReplacesGraalVmJarsWithGraalVmAndJdk() throws Exception {
        JsonNode bom = runOk("release", BUILD_INFO);

        assertEquals(List.of("pkg:maven/com.example/lib@2.0?type=jar", "graalvm", "jdk"), refs(bom.path("components")));
        assertTrue(bom.path("components").get(0).path("externalReferences").isMissingNode());
        JsonNode graalVm = bom.path("components").get(1);
        assertEquals("GraalVM Community", graalVm.path("name").asText());
        assertEquals("25.5.5.0.3", graalVm.path("version").asText());
        JsonNode jdk = bom.path("components").get(2);
        assertEquals("OpenJDK", jdk.path("name").asText());
        assertEquals("25.0.5", jdk.path("version").asText());
        assertEquals("cpe:2.3:a:oracle:openjdk:25.0.5:*:*:*:*:*:*:*", jdk.path("cpe").asText());

        JsonNode root = bom.path("metadata").path("component");
        assertEquals("pkg:maven/com.dxfeed/graal-native-sdk@1.0?type=zip", root.path("purl").asText());
        for (JsonNode reference : root.path("externalReferences")) {
            assertTrue(reference.path("url").asText().startsWith("https://github.com/dxFeed/dxfeed-graal-native-sdk"));
        }

        JsonNode dependencies = bom.path("dependencies");
        assertEquals(List.of("pkg:maven/com.dxfeed/graal-native-sdk@1.0?type=jar",
            "pkg:maven/com.example/lib@2.0?type=jar", "graalvm", "jdk"), refs(dependencies));
        List<String> rootDependsOn = new ArrayList<>();
        dependencies.get(0).path("dependsOn").forEach(ref -> rootDependsOn.add(ref.asText()));
        assertEquals(List.of("pkg:maven/com.example/lib@2.0?type=jar", "graalvm", "jdk"), rootDependsOn);
    }

    @Test
    public void releaseOfOracleGraalVm() throws Exception {
        JsonNode bom = runOk("release", BUILD_INFO
            .replace("graalvm.implementor=GraalVM Community", "graalvm.implementor=Oracle Corporation")
            .replace("graalvm.java-version=25.0.5.0.3+1", "graalvm.java-version=25+37"));

        assertEquals("Oracle GraalVM", bom.path("components").get(1).path("name").asText());
        JsonNode jdk = bom.path("components").get(2);
        assertEquals("Oracle JDK", jdk.path("name").asText());
        assertEquals("25.0.0", jdk.path("version").asText());
        assertEquals("cpe:2.3:a:oracle:jdk:25.0.0:*:*:*:*:*:*:*", jdk.path("cpe").asText());
    }

    @Test
    public void releaseWithoutGraalVmValuesFails() throws Exception {
        assertFails("release", BUILD_INFO.replace("graalvm.version=25.5.5.0.3", ""), "No graalvm.* values");
    }

    // The checked-in jre-ios/build-info.txt: the iOS Simulator slices come from two GraalVM versions.
    @Test
    public void jreIosSimulatorHasTwoGraalVms() throws Exception {
        JsonNode bom = runOk(jreIos("simulator-"), BUILD_INFO);

        assertEquals(List.of("pkg:maven/com.example/lib@2.0?type=jar", "graalvm-simulator-arm64", "jdk-simulator-arm64",
            "graalvm-simulator-x86_64", "jdk-simulator-x86_64"), refs(bom.path("components")));
        assertEquals("25.5.5.0.3", bom.path("components").get(1).path("version").asText());
        assertEquals("25.0.5", bom.path("components").get(2).path("version").asText());
        assertEquals("25.0.1", bom.path("components").get(3).path("version").asText());
        assertEquals("25.0.1", bom.path("components").get(4).path("version").asText());
    }

    @Test
    public void jreIosDeviceHasOneGraalVm() throws Exception {
        JsonNode bom = runOk(jreIos("ios-"), BUILD_INFO);

        assertEquals(List.of("pkg:maven/com.example/lib@2.0?type=jar", "graalvm-ios-arm64", "jdk-ios-arm64"),
            refs(bom.path("components")));
    }

    @Test
    public void jreIosUnknownSliceFails() throws Exception {
        assertFails(jreIos("tvos-"), BUILD_INFO, "No slice tvos-*");
    }

    // A company host in a field that the program keeps.
    @Test
    public void companyHostFailsAndWritesNothing() throws Exception {
        assertFails("release", BUILD_INFO.replace("variant=default", "variant=https://devexperts.com/x"),
            "names a host of the company network");
    }

    private String jreIos(String prefix) {
        // An absolute path: on Windows it has a drive colon before the prefix colon.
        return "jre-ios:" + JRE_IOS_BUILD_INFO.toAbsolutePath() + ":" + prefix;
    }

    private JsonNode runOk(String source, String buildInfo) throws Exception {
        Run run = run(source, buildInfo);
        assertEquals(run.log, 0, run.exitCode);
        return MAPPER.readTree(run.output.toFile());
    }

    private void assertFails(String source, String buildInfo, String message) throws Exception {
        Run run = run(source, buildInfo);
        assertNotEquals(run.log, 0, run.exitCode);
        assertTrue(run.log, run.log.contains(message));
        assertFalse(Files.exists(run.output));
    }

    private Run run(String source, String buildInfo) throws Exception {
        Path mavenSbom = folder.newFile("maven-sbom.json").toPath();
        Files.writeString(mavenSbom, MAVEN_SBOM, StandardCharsets.UTF_8);
        Path buildInfoFile = folder.newFile("build-info.properties").toPath();
        Files.writeString(buildInfoFile, buildInfo, StandardCharsets.UTF_8);
        Path output = folder.getRoot().toPath().resolve("sbom.cdx.json");
        Path log = folder.getRoot().toPath().resolve("log.txt");

        Process process = new ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", jacksonClasspath(),
            PROGRAM.toString(),
            mavenSbom.toString(), buildInfoFile.toString(), source, output.toString())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
        assertTrue("FinishSbom did not finish", process.waitFor(2, TimeUnit.MINUTES));
        return new Run(process.exitValue(), Files.readString(log), output);
    }

    private static String jacksonClasspath() throws URISyntaxException {
        List<String> jars = new ArrayList<>();
        for (Class<?> type : List.of(ObjectMapper.class, JsonFactory.class, JsonAutoDetect.class)) {
            jars.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        }
        return String.join(File.pathSeparator, jars);
    }

    private static List<String> refs(JsonNode array) {
        List<String> refs = new ArrayList<>();
        for (JsonNode node : array) {
            refs.add(node.has("bom-ref") ? node.path("bom-ref").asText() : node.path("ref").asText());
        }
        return refs;
    }

    private record Run(int exitCode, String log, Path output) {
    }
}
