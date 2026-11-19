// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** The conformance kit of the NetTest specification, {@code docs/wrapper-guide/nettest} of the SDK sources. */
final class NetTestKit {
    private NetTestKit() {}

    static Path directory() {
        return Paths.get(System.getProperty("nettest.kit", "../../docs/wrapper-guide/nettest"));
    }

    static JsonNode read(String file) {
        try {
            return new ObjectMapper().readTree(directory().resolve(file).toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The expressions of {@code regexes.json} by their names. */
    static Map<String, Pattern> regexes() {
        Map<String, Pattern> regexes = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = read("regexes.json").get("regexes").fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            regexes.put(field.getKey(), Pattern.compile(field.getValue().asText()));
        }
        return regexes;
    }

    /**
     * Whether a line of the output matches the expression of the kit: the anchored ones ({@code ^}) from the start of
     * the line, the others anywhere in it.
     */
    static boolean matches(Pattern regex, String line) {
        return regex.matcher(line).find();
    }
}
