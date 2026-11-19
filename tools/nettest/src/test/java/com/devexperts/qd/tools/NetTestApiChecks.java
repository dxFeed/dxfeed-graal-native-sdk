// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** The checks of the summary file and of the metrics against the conformance kit. */
final class NetTestApiChecks {
    private NetTestApiChecks() {}

    /** Reads the summary file and checks it against {@link #checkSummary}. */
    static JsonNode readSummary(Path file) throws IOException {
        return checkSummary(Files.readString(file, StandardCharsets.UTF_8));
    }

    /**
     * Checks a summary against {@code nettest-summary.schema.json} of the kit and the sums of the specification: the
     * records of the intervals add up to {@code records}, and the totals of {@code e2e_lag} or {@code publish_call}
     * to the records of the measured intervals.
     */
    static JsonNode checkSummary(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode summary = mapper.readTree(json);
        String schemaFile = "nettest-summary.schema.json";
        // The relative $id of the schema resolves against the location of its file.
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(
            SchemaLocation.of(NetTestKit.directory().resolve(schemaFile).toAbsolutePath().toUri().toString()),
            NetTestKit.read(schemaFile));
        Set<ValidationMessage> errors = schema.validate(summary);
        assertTrue(errors + "\n" + json, errors.isEmpty());

        long records = 0;
        long measured = 0;
        for (JsonNode interval : summary.get("intervals")) {
            records += interval.get("records").asLong();
            if (interval.get("phase").asText().equals("measure")) {
                measured += interval.get("records").asLong();
            }
        }
        assertEquals(json, summary.get("records").asLong(), records);
        if (summary.get("side").asText().equals("consumer")) {
            assertEquals(json, measured, summary.get("e2e_lag").get("count").asLong());
        } else {
            assertEquals(json, measured, summary.get("publish_call").get("events").asLong());
        }
        return summary;
    }

    /** The families of a scrape with their types, from the {@code # TYPE} lines. */
    static Map<String, String> types(String scrape) {
        Map<String, String> types = new LinkedHashMap<>();
        // The samples of the kit may come with CRLF on a Windows checkout.
        for (String line : scrape.split("\r?\n")) {
            if (line.startsWith("# TYPE ")) {
                String[] parts = line.split(" ");
                types.put(parts[2], parts[3]);
            }
        }
        return types;
    }

    /**
     * Checks a scrape against the sample of the kit for the side: the same families with the same types, a sample
     * of each family, and the labels of the tool.
     */
    static void checkScrape(String scrape, String side, String name) throws IOException {
        String sample = Files.readString(NetTestKit.directory().resolve("metrics-sample-" + side + ".txt"),
            StandardCharsets.UTF_8);
        Map<String, String> types = types(scrape);
        assertEquals(scrape, types(sample), types);
        for (String family : types.keySet()) {
            assertTrue(family + "\n" + scrape, scrape.contains("\n" + family + "{tool=\"java-api\",side=\"" + side +
                "\",name=\"" + name + "\"") || scrape.contains("\n" + family + "_bucket{tool=\"java-api\",side=\"" +
                side + "\",name=\"" + name + "\""));
        }
    }

    /** A scrape of the URL: the content type of the specification and the body. */
    static String scrape(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            assertEquals(200, connection.getResponseCode());
            assertEquals(NetTestApiMetrics.CONTENT_TYPE, connection.getContentType());
            try (InputStream in = connection.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
