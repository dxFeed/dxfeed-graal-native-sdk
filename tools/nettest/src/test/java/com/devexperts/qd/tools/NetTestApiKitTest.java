// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static com.devexperts.qd.tools.NetTestApiRuntime.ABSENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * The conformance kit of the NetTest specification: the command lines of {@code cli-vectors.json}, the lines of
 * {@code lines.json} and the expressions of {@code regexes.json}.
 */
public class NetTestApiKitTest {
    /** The sections of {@code cli-vectors.json} that the tool implements. */
    private static final Set<String> SECTIONS = Set.of("cli", "roles", "consumer", "summary", "metrics");
    /** The optional features of the specification that the tool has: none, so no -X and no --legacy. */
    private static final Set<String> FEATURES = Set.of();

    @Test
    public void everyCommandLineOfTheImplementedSectionsGivesItsResult() {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (JsonNode vector : NetTestKit.read("cli-vectors.json").get("vectors")) {
            if (!SECTIONS.contains(vector.get("section").asText()) || !hasFeatures(vector)) {
                continue;
            }
            checked++;
            String failure = check(vector);
            if (failure != null) {
                failures.add(failure);
            }
        }
        assertTrue(String.join("\n", failures), failures.isEmpty());
        assertTrue("vectors checked: " + checked, checked >= 65);
    }

    private static boolean hasFeatures(JsonNode vector) {
        if (!vector.has("requires")) {
            return true;
        }
        for (JsonNode feature : vector.get("requires")) {
            String name = feature.asText();
            boolean absent = name.startsWith("!");
            if (FEATURES.contains(absent ? name.substring(1) : name) == absent) {
                return false;
            }
        }
        return true;
    }

    private static String check(JsonNode vector) {
        String[] args = new String[vector.get("args").size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = vector.get("args").get(i).asText();
        }
        String line = String.join(" ", args);
        int exit = vector.get("exit").asInt();
        if (vector.has("stdout_contains")) {
            if (!NetTestApiCommandLine.isHelp(args)) {
                return line + ": not a request for the usage";
            }
            String usage = new NetTestApi().usage();
            return usage.contains(vector.get("stdout_contains").asText()) ? null : line + ": usage without " +
                vector.get("stdout_contains") + "\n" + usage;
        }
        Map<String, Object> settings;
        try {
            settings = withProperties(args).describe();
        } catch (BadToolParametersException e) {
            if (exit != 2) {
                return line + ": rejected with '" + e.getMessage() + "'";
            }
            if (vector.has("error") && !e.getMessage().equals(vector.get("error").asText())) {
                return line + ": '" + e.getMessage() + "' instead of '" + vector.get("error").asText() + "'";
            }
            if (vector.has("error_prefix") && !e.getMessage().startsWith(vector.get("error_prefix").asText())) {
                return line + ": '" + e.getMessage() + "' does not start with '" +
                    vector.get("error_prefix").asText() + "'";
            }
            return null;
        }
        if (exit != 0) {
            return line + ": accepted, expected exit " + exit;
        }
        if (!vector.has("settings")) {
            return null;
        }
        List<String> differences = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = vector.get("settings").fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!settings.containsKey(field.getKey())) {
                differences.add(field.getKey() + " is not described");
                continue;
            }
            Object actual = settings.get(field.getKey());
            if (!sameValue(field.getValue(), actual)) {
                differences.add(field.getKey() + " is " + actual + " instead of " + field.getValue());
            }
        }
        return differences.isEmpty() ? null : line + ": " + String.join(", ", differences);
    }

    // Parses with the -D properties of the vector restored afterwards.
    private static NetTestApi.Settings withProperties(String[] args) {
        Map<Object, Object> before = new java.util.HashMap<>(System.getProperties());
        try {
            return NetTestApi.parseArguments(args);
        } finally {
            System.getProperties().keySet().removeIf(key -> !before.containsKey(key));
            System.getProperties().putAll(before);
        }
    }

    private static boolean sameValue(JsonNode expected, Object actual) {
        if (expected.isNull()) {
            return actual == null;
        }
        if (actual == null) {
            return false;
        }
        if (expected.isNumber()) {
            return actual instanceof Number && expected.asDouble() == ((Number) actual).doubleValue();
        }
        if (expected.isBoolean()) {
            return Boolean.valueOf(expected.asBoolean()).equals(actual);
        }
        if (expected.isArray()) {
            List<String> values = new ArrayList<>();
            expected.forEach(value -> values.add(value.asText()));
            return values.equals(actual);
        }
        return expected.asText().equals(actual);
    }

    @Test
    public void digitsAreGroupedByThousands() {
        for (JsonNode sample : NetTestKit.read("lines.json").get("grouping")) {
            assertEquals(sample.get("text").asText(), NetTestApiStatistics.groupDigits(sample.get("value").asLong()));
        }
    }

    @Test
    public void statisticsBlocksAreTheBytesOfTheKit() {
        int checked = 0;
        for (JsonNode sample : NetTestKit.read("lines.json").get("statistics")) {
            boolean producer = sample.get("side").asText().equals("producer");
            JsonNode instances = sample.get("instances");
            long[] records = new long[instances.size()];
            long[] nanos = new long[instances.size()];
            for (int i = 0; i < records.length; i++) {
                records[i] = instances.get(i).get("records").asLong();
                nanos[i] = instances.get(i).get(producer ? "weighted_ns" : "lag_sum_ns").asLong();
            }
            String text = NetTestApiStatistics.format(
                producer ? NetTestApiStatistics.PRODUCER_LABEL : NetTestApiStatistics.CONSUMER_LABEL,
                sample.get("name").asText(), sample.get("interval_ns").asLong(), sample.get("connections").asInt(),
                records, nanos);
            assertEquals(sample.get("text").asText(), text);
            checked++;
        }
        assertTrue(checked >= 6);
    }

    @Test
    public void runtimeLinesAreTheBytesOfTheKit() {
        for (JsonNode sample : NetTestKit.read("lines.json").get("runtime")) {
            assertEquals(sample.get("text").asText(), NetTestApiRuntime.format(sample.get("name").asText(),
                runtimeSample(sample.get("previous")), runtimeSample(sample.get("current")),
                sample.get("interval_ns").asLong()));
        }
    }

    private static NetTestApiRuntime.Sample runtimeSample(JsonNode node) {
        return new NetTestApiRuntime.Sample(value(node, "heap_used_bytes"), value(node, "heap_committed_bytes"),
            value(node, "gc_count"), value(node, "gc_time_ms"), value(node, "cpu_ns"));
    }

    private static long value(JsonNode node, String key) {
        return node.get(key).isNull() ? ABSENT : node.get(key).asLong();
    }

    @Test
    public void theLinesOfTheKitMatchTheExpressions() {
        Map<String, Pattern> regexes = NetTestKit.regexes();
        for (JsonNode sample : NetTestKit.read("lines.json").get("statistics")) {
            String[] lines = sample.get("text").asText().split("\n");
            assertTrue(lines[0], NetTestKit.matches(regexes.get("summary"), lines[0]));
            for (int i = 1; i < lines.length; i++) {
                assertTrue(lines[i], NetTestKit.matches(regexes.get("instance"), lines[i]));
            }
        }
        for (JsonNode sample : NetTestKit.read("lines.json").get("runtime")) {
            assertTrue(NetTestKit.matches(regexes.get("runtime"), sample.get("text").asText()));
        }
        for (JsonNode captured : NetTestKit.read("lines.json").get("captured")) {
            String line = captured.asText();
            if (regexes.values().stream().noneMatch(regex -> NetTestKit.matches(regex, line))) {
                fail("no expression matches '" + line + "'");
            }
        }
    }
}
