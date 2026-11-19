// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * The generated symbols of the NetTest kit (docs/wrapper-guide/nettest) against QD's own SymbolGenerator: the same set
 * as {@code SymbolGenerator.generateSymbols(total)}, and the order of first appearance of the symbols its
 * {@code generateSymbol} draws from {@code java.util.Random} with the QD seed. A QD change in the generator fails here.
 */
public class NetTestKitSymbolsTest {
    private static final long QD_SEED = 1416948710541751L;
    private static final Pattern CASE = Pattern.compile("\"total\":\\s*(\\d+),\\s*\"draws\":\\s*(\\d+),\\s*" +
        "\"fnv1a64\":\\s*\"0x([0-9A-Fa-f]{16})\",\\s*\"samples\":\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern SAMPLE = Pattern.compile("\"index\":\\s*(\\d+),\\s*\"symbol\":\\s*\"([^\"]*)\"");

    private static Path kitDir() {
        return Paths.get(System.getProperty("nettest.kitDir", "../../docs/wrapper-guide/nettest"));
    }

    /** QD's generator in the order of first appearance, with the number of draws behind each new symbol. */
    private static final class FirstAppearance {
        final List<String> symbols = new ArrayList<>();
        final List<Long> draws = new ArrayList<>();

        FirstAppearance(int total) {
            Random random = new Random(QD_SEED);
            Set<String> seen = new LinkedHashSet<>();
            long drawn = 0;

            while (symbols.size() < total) {
                String symbol = SymbolGenerator.generateSymbol(random,
                    SymbolGenerator.DEFAULT_LENGTH_GENERATOR.apply(random));

                drawn++;

                if (seen.add(symbol)) {
                    symbols.add(symbol);
                    draws.add(drawn);
                }
            }
        }
    }

    private static long fnv1a64(List<String> symbols) {
        long hash = 0xCBF29CE484222325L;

        for (String symbol : symbols) {
            for (byte value : (symbol + "\n").getBytes(StandardCharsets.US_ASCII)) {
                hash = (hash ^ (value & 0xFF)) * 0x100000001B3L;
            }
        }

        return hash;
    }

    @Test
    public void vectorsAreThoseOfQdSymbolGenerator() throws IOException {
        String json = Files.readString(kitDir().resolve("symbols-generated-vectors.json"));
        Matcher cases = CASE.matcher(json);
        Set<Integer> totals = new TreeSet<>();
        FirstAppearance generated = null;

        while (cases.find()) {
            int total = Integer.parseInt(cases.group(1));

            if (generated == null || generated.symbols.size() < total) {
                generated = new FirstAppearance(Math.max(total, 100_000));
            }

            List<String> symbols = generated.symbols.subList(0, total);
            String label = "total " + total;

            totals.add(total);
            assertEquals(label + ": the set of SymbolGenerator.generateSymbols", new HashSet<>(symbols),
                new HashSet<>(SymbolGenerator.generateSymbols(total)));
            assertEquals(label + ": draws", Long.parseLong(cases.group(2)), (long) generated.draws.get(total - 1));
            assertEquals(label + ": fnv1a64", Long.parseUnsignedLong(cases.group(3), 16), fnv1a64(symbols));

            Matcher samples = SAMPLE.matcher(cases.group(4));
            int sampleCount = 0;

            while (samples.find()) {
                int index = Integer.parseInt(samples.group(1));

                assertEquals(label + ": symbol " + index, samples.group(2), symbols.get(index));
                sampleCount++;
            }

            // Every sample object must parse: a malformed one would otherwise be skipped silently.
            assertTrue(label + ": no samples", sampleCount > 0);
            assertEquals(label + ": samples parsed", cases.group(4).split("\\{", -1).length - 1, sampleCount);
        }

        assertEquals("the totals of symbols-generated-vectors.json", Set.of(10, 1000, 100000), totals);
    }

    @Test
    public void firstTenAreThoseOfQdSymbolGenerator() throws IOException {
        List<String> expected = new ArrayList<>();

        for (String line : Files.readAllLines(kitDir().resolve("symbols-generated-10.txt"))) {
            if (!line.isBlank()) {
                expected.add(line.strip());
            }
        }

        assertEquals(new FirstAppearance(10).symbols, expected);
    }
}
