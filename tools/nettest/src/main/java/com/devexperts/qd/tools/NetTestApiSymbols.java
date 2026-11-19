// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.ipf.InstrumentProfileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The symbols of a run (NetTest specification, section "Time, sequence and symbols"): the generated symbols of QD
 * NetTest in the order of their first appearance, or the first symbols of an IPF file in file order.
 */
final class NetTestApiSymbols {
    /** The seed of QD NetTest's generator ({@code SymbolGenerator}). */
    static final long GENERATOR_SEED = 1416948710541751L;

    private NetTestApiSymbols() {}

    /**
     * The first {@code total} distinct symbols of QD NetTest's generator in the order of their first appearance:
     * {@link Random} with {@link #GENERATOR_SEED}, the length and the characters of {@code SymbolGenerator}. QD itself
     * returns the same set in the order of a {@code HashSet}.
     */
    static String[] generated(int total) {
        Random random = new Random(GENERATOR_SEED);
        Set<String> symbols = new LinkedHashSet<>(total + (total >> 1));
        while (symbols.size() < total) {
            symbols.add(SymbolGenerator.generateSymbol(random,
                SymbolGenerator.DEFAULT_LENGTH_GENERATOR.apply(random)));
        }
        return symbols.toArray(new String[0]);
    }

    /**
     * The first {@code total} symbols of the IPF file in file order. A file with fewer symbols gives the smaller set
     * and a warning on stderr.
     *
     * @throws IOException when the file cannot be read or has no symbols.
     */
    static String[] fromFile(String path, int total) throws IOException {
        List<String> symbols = new ArrayList<>();
        for (InstrumentProfile profile : new InstrumentProfileReader().readFromFile(path)) {
            if (symbols.size() == total) {
                break;
            }
            symbols.add(profile.getSymbol());
        }
        if (symbols.isEmpty()) {
            throw new IOException("the symbol file '" + path + "' has no symbols");
        }
        if (symbols.size() < total) {
            System.err.println("NetTestApi: warning: the symbol file '" + path + "' has " + symbols.size() +
                " symbols, fewer than " + total + "; the run takes " + symbols.size());
        }
        return symbols.toArray(new String[0]);
    }
}
