// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes the SBOM of an SDK archive (docs/graal-native-sdk-sbom.cdx.json) from the SBOM of the Maven dependencies that
 * cyclonedx-maven-plugin writes. Run by the pom (create-sbom-file) as a single-file program with the test classpath
 * (Jackson):
 *
 * <pre>
 * java FinishSbom.java &lt;maven SBOM&gt; &lt;build-info.properties&gt; &lt;GraalVM source&gt; &lt;output&gt;
 * </pre>
 *
 * <ul>
 *     <li>The library contains the JDK and Substrate VM of the GraalVM that built it, not the GraalVM jars of the Maven
 *     dependencies (org.graalvm.*), whose version is that of the pom: they are replaced by a component for the GraalVM
 *     and one for its JDK. The GraalVM source is {@code release} (the release file of the GraalVM that ran native-image,
 *     the graalvm.* values of build-info) or {@code jre-ios:<file>:<slice prefix>} (the iOS archives: the slices of
 *     jre-ios/build-info.txt whose names start with the prefix, since the slices of the simulator come from two
 *     GraalVM versions).</li>
 *     <li>The external references come from the POMs of the dependencies and name the hosts where they were built;
 *     they are removed. The root component gets the public references of the SDK. The program fails if the result
 *     still names a host of the company network.</li>
 * </ul>
 */
public final class FinishSbom {
    private static final String GITHUB = "https://github.com/dxFeed/dxfeed-graal-native-sdk";
    private static final Pattern INTERNAL = Pattern.compile("(?i)devexperts\\.com");
    // ios-arm64: graal graal-25.5.5.0.3 (sha), labs JDK 25.0.5.0.3+1-jvmci-25.5-b26, labs-openjdk ...
    private static final Pattern SLICE = Pattern.compile(
        "^([\\w-]+): graal (\\S+) \\(([0-9a-f]+)\\), labs JDK (\\S+),.*$");

    private record GraalVm(String implementor, String version, String javaVersion, String slice, String commit) {
        boolean oracle() {
            return implementor.startsWith("Oracle");
        }

        String name() {
            return oracle() ? "Oracle GraalVM" : "GraalVM Community";
        }

        String suffix() {
            return slice.isEmpty() ? "" : "-" + slice;
        }

        // 25.0.5.0.3+1-jvmci-25.5-b26 -> 25.0.5: the feature, interim and update numbers that the JDK advisories use.
        String jdkVersion() {
            Matcher matcher = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?").matcher(javaVersion);
            if (!matcher.find()) {
                throw new IllegalArgumentException("Unexpected Java version: " + javaVersion);
            }
            return matcher.group(1) + "." + (matcher.group(2) == null ? "0" : matcher.group(2)) + "."
                + (matcher.group(3) == null ? "0" : matcher.group(3));
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                "Usage: FinishSbom <maven SBOM> <build-info.properties> <release|jre-ios:<file>:<prefix>> <output>");
        }
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode bom = (ObjectNode) mapper.readTree(Path.of(args[0]).toFile());
        Properties buildInfo = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of(args[1]), StandardCharsets.UTF_8)) {
            buildInfo.load(reader);
        }
        List<GraalVm> graalVms = graalVms(args[2], buildInfo);

        ArrayNode components = (ArrayNode) bom.withArray("components");
        Set<String> removed = new HashSet<>();
        for (Iterator<JsonNode> iterator = components.elements(); iterator.hasNext(); ) {
            JsonNode component = iterator.next();
            if (component.path("purl").asText().startsWith("pkg:maven/org.graalvm.")) {
                removed.add(component.path("bom-ref").asText());
                iterator.remove();
            } else {
                ((ObjectNode) component).remove("externalReferences");
            }
        }

        ObjectNode metadata = bom.with("metadata");
        ObjectNode root = metadata.with("component");
        String rootRef = root.path("bom-ref").asText();
        root.remove("externalReferences");
        root.put("description", "dxFeed Graal Native SDK: the native library and its C API");
        ArrayNode rootReferences = root.putArray("externalReferences");
        rootReferences.addObject().put("type", "website").put("url", GITHUB);
        rootReferences.addObject().put("type", "vcs").put("url", GITHUB + ".git");
        rootReferences.addObject().put("type", "issue-tracker").put("url", GITHUB + "/issues");
        // The archive, not the jar of the Java code.
        root.put("purl", root.path("purl").asText().replace("type=jar", "type=zip"));
        ArrayNode properties = (ArrayNode) metadata.withArray("properties");
        addProperty(properties, "dxfeed:sdk:target", buildInfo.getProperty("target", ""));
        addProperty(properties, "dxfeed:sdk:variant", buildInfo.getProperty("variant", ""));
        addProperty(properties, "dxfeed:native-image:vendor-version",
            buildInfo.getProperty("native-image.vendor-version", ""));
        addProperty(properties, "dxfeed:native-image:march", buildInfo.getProperty("native-image.march", ""));
        addProperty(properties, "dxfeed:native-image:garbage-collector",
            buildInfo.getProperty("native-image.garbage-collector", ""));

        List<String> added = new ArrayList<>();
        for (GraalVm graalVm : graalVms) {
            String graalRef = "graalvm" + graalVm.suffix();
            String jdkRef = "jdk" + graalVm.suffix();
            ObjectNode graal = components.addObject();
            graal.put("type", "framework").put("bom-ref", graalRef).put("publisher", "Oracle")
                .put("name", graalVm.name()).put("version", graalVm.version())
                .put("description", "The GraalVM that built the library: its Substrate VM is compiled into the library");
            graal.putArray("externalReferences").addObject().put("type", "website").put("url", "https://www.graalvm.org");
            ArrayNode graalProperties = graal.putArray("properties");
            if (!graalVm.slice().isEmpty()) {
                addProperty(graalProperties, "dxfeed:slice", graalVm.slice());
            }
            if (!graalVm.commit().isEmpty()) {
                addProperty(graalProperties, "dxfeed:oracle-graal-commit", graalVm.commit());
            }
            if (graalProperties.isEmpty()) {
                graal.remove("properties");
            }
            ObjectNode jdk = components.addObject();
            String jdkProduct = graalVm.oracle() ? "jdk" : "openjdk";
            jdk.put("type", "framework").put("bom-ref", jdkRef).put("publisher", "Oracle")
                .put("name", graalVm.oracle() ? "Oracle JDK" : "OpenJDK").put("version", graalVm.jdkVersion())
                .put("description", "The JDK of " + graalVm.name() + " " + graalVm.version()
                    + ": its class library is compiled into the library (Java version " + graalVm.javaVersion() + ")")
                .put("cpe", "cpe:2.3:a:oracle:" + jdkProduct + ":" + graalVm.jdkVersion() + ":*:*:*:*:*:*:*");
            if (!graalVm.slice().isEmpty()) {
                addProperty(jdk.putArray("properties"), "dxfeed:slice", graalVm.slice());
            }
            added.add(graalRef);
            added.add(jdkRef);
        }

        ArrayNode dependencies = (ArrayNode) bom.withArray("dependencies");
        for (Iterator<JsonNode> iterator = dependencies.elements(); iterator.hasNext(); ) {
            ObjectNode dependency = (ObjectNode) iterator.next();
            if (removed.contains(dependency.path("ref").asText())) {
                iterator.remove();
                continue;
            }
            JsonNode dependsOn = dependency.path("dependsOn");
            if (dependsOn.isArray()) {
                for (Iterator<JsonNode> refs = dependsOn.elements(); refs.hasNext(); ) {
                    if (removed.contains(refs.next().asText())) {
                        refs.remove();
                    }
                }
            }
            if (dependency.path("ref").asText().equals(rootRef)) {
                ArrayNode rootDependsOn = dependency.withArray("dependsOn");
                added.forEach(rootDependsOn::add);
            }
        }
        for (String ref : added) {
            dependencies.addObject().put("ref", ref).putArray("dependsOn");
        }

        String json = mapper.writeValueAsString(bom);
        Matcher internal = INTERNAL.matcher(json);
        if (internal.find()) {
            int start = Math.max(0, json.lastIndexOf('\n', internal.start()) + 1);
            int end = json.indexOf('\n', internal.end());
            throw new IllegalStateException("The SBOM names a host of the company network: "
                + json.substring(start, end < 0 ? json.length() : end).trim());
        }
        Files.writeString(Path.of(args[3]), json + "\n", StandardCharsets.UTF_8);
    }

    private static List<GraalVm> graalVms(String source, Properties buildInfo) throws IOException {
        if (source.equals("release")) {
            GraalVm graalVm = new GraalVm(buildInfo.getProperty("graalvm.implementor", ""),
                buildInfo.getProperty("graalvm.version", ""), buildInfo.getProperty("graalvm.java-version", ""), "",
                "");
            if (graalVm.implementor().isEmpty() || graalVm.version().isEmpty() || graalVm.javaVersion().isEmpty()) {
                throw new IllegalArgumentException("No graalvm.* values in build-info: " + buildInfo);
            }
            return List.of(graalVm);
        }
        String[] parts = source.split(":", 3);
        if (parts.length != 3 || !parts[0].equals("jre-ios")) {
            throw new IllegalArgumentException("Unexpected GraalVM source: " + source);
        }
        // A Windows path has a colon: the prefix is after the last one.
        int last = source.lastIndexOf(':');
        Path file = Path.of(source.substring("jre-ios:".length(), last));
        String prefix = source.substring(last + 1);
        List<GraalVm> result = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            Matcher matcher = SLICE.matcher(line.trim());
            if (matcher.matches() && matcher.group(1).startsWith(prefix)) {
                String tag = matcher.group(2);
                String version = tag.replaceFirst("^(graal|jdk)-", "");
                result.add(new GraalVm("GraalVM Community", version, matcher.group(4), matcher.group(1),
                    matcher.group(3)));
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No slice " + prefix + "* in " + file);
        }
        return result;
    }

    private static void addProperty(ArrayNode properties, String name, String value) {
        if (!value.isEmpty()) {
            properties.addObject().put("name", name).put("value", value);
        }
    }
}
