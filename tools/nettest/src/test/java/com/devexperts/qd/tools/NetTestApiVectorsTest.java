// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.dxfeed.event.market.Trade;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.HdrHistogram.Histogram;
import org.junit.Test;

/**
 * The vectors of the conformance kit: the event pool ({@code pool-vectors.json}), the generated symbols
 * ({@code symbols-generated-10.txt}) and the percentiles of the e2e lag histogram ({@code histogram-vectors.json}).
 */
public class NetTestApiVectorsTest {
    private static final double PRICE_TOLERANCE = 1e-9;

    @Test
    public void thePoolOfEachInstanceGivesTheDrawsTheTableAndTheEventsOfTheKit() {
        for (JsonNode vector : NetTestKit.read("pool-vectors.json").get("instances")) {
            int instance = vector.get("instance").asInt();
            String about = "instance " + instance;
            assertEquals(about, hex(vector.get("seed")), NetTestApiPool.seed(instance));
            assertEquals(about, hex(vector.get("initial_state")), NetTestApiPool.initialState(instance));

            int poolSize = vector.get("events").get("pool_size").asInt();
            NetTestApiPool pool = new NetTestApiPool(instance, poolSize);
            JsonNode draws = vector.get("first_draws");
            for (int i = 0; i + 1 < draws.size(); i += 2) {
                int row = i / 2;
                assertEquals(about + " row " + row, (100 + Long.remainderUnsigned(hex(draws.get(i)), 100_000)) / 100.0,
                    pool.price(row), PRICE_TOLERANCE);
                assertEquals(about + " row " + row, 1 + Long.remainderUnsigned(hex(draws.get(i + 1)), 1_000),
                    pool.size(row), 0);
            }
            for (JsonNode row : vector.get("table_rows")) {
                int index = row.get("row").asInt();
                assertEquals(about + " row " + index, row.get("price").asDouble(), pool.price(index),
                    PRICE_TOLERANCE);
                assertEquals(about + " row " + index, row.get("size").asDouble(), pool.size(index), 0);
            }
            for (JsonNode event : vector.get("events").get("first_events")) {
                long draw = pool.next();
                assertEquals(about, hex(event.get("draw")), draw);
                assertEquals(about, event.get("symbol_index").asInt(), pool.symbolIndex(draw));
                int row = NetTestApiPool.row(draw);
                assertEquals(about, event.get("row").asInt(), row);
                assertEquals(about, event.get("price").asDouble(), pool.price(row), PRICE_TOLERANCE);
                assertEquals(about, event.get("size").asDouble(), pool.size(row), 0);
            }
        }
    }

    @Test
    public void theGeneratorPublishesTheEventsOfThePool() {
        JsonNode vector = NetTestKit.read("pool-vectors.json").get("instances").get(0);
        int poolSize = vector.get("events").get("pool_size").asInt();
        NetTestApiProducerSide side = new NetTestApiProducerSide(
            NetTestApi.parseArguments("p", ":1", "-S", Integer.toString(poolSize)), new NetTestApi.Stop());
        JsonNode expected = vector.get("events").get("first_events");

        List<com.dxfeed.event.market.MarketEvent> batch =
            new NetTestApiGeneratorThread(1, side, null).fill(expected.size(), 1_700_000_000_000_000_000L);

        for (int i = 0; i < expected.size(); i++) {
            Trade trade = (Trade) batch.get(i);
            JsonNode event = expected.get(i);
            assertEquals(side.getSymbols()[event.get("symbol_index").asInt()], trade.getEventSymbol());
            assertEquals(event.get("price").asDouble(), trade.getPrice(), PRICE_TOLERANCE);
            assertEquals(event.get("size").asDouble(), trade.getSizeAsDouble(), 0);
        }
    }

    @Test
    public void theTimeAndTheSequenceArePackedAsTheKitSays() {
        NetTestApiProducerSide side = new NetTestApiProducerSide(
            NetTestApi.parseArguments("p", ":1", "-S", "1"), new NetTestApi.Stop());
        NetTestApiGeneratorThread generator = new NetTestApiGeneratorThread(1, side, null);
        for (JsonNode vector : NetTestKit.read("pool-vectors.json").get("pack_time_sequence")) {
            generator.setNextSequence(vector.get("sequence").asInt());

            Trade trade = (Trade) generator.fill(1, vector.get("epoch_ns").asLong()).get(0);

            String about = vector.toString();
            assertEquals(about, vector.get("time_ms").asLong(), trade.getTime());
            assertEquals(about, vector.get("nano_part").asInt(), trade.getTimeNanoPart());
            assertEquals(about, hex(vector.get("packed")), trade.getTimeSequence());
        }
    }

    @Test
    public void theGeneratedSymbolsAreTheOnesOfTheKitInTheirOrder() throws IOException {
        List<String> lines = Files.readAllLines(NetTestKit.directory().resolve("symbols-generated-10.txt"),
            StandardCharsets.UTF_8);
        String[] expected = lines.stream().map(String::trim).filter(line -> !line.isEmpty()).toArray(String[]::new);

        assertArrayEquals(expected, NetTestApiSymbols.generated(expected.length));
    }

    @Test
    public void thePercentilesOfTheHistogramAreTheOnesOfTheKit() {
        JsonNode kit = NetTestKit.read("histogram-vectors.json");
        JsonNode settings = kit.get("histogram");
        assertEquals(NetTestApiStatistics.HISTOGRAM_LOWEST, settings.get("lowest").asLong());
        assertEquals(NetTestApiStatistics.HISTOGRAM_HIGHEST, settings.get("highest").asLong());
        assertEquals(NetTestApiStatistics.HISTOGRAM_DIGITS, settings.get("digits").asInt());
        String[] keys = {"p50_ns", "p90_ns", "p99_ns", "p999_ns", "p9999_ns"};
        double[] percentiles = {50, 90, 99, 99.9, 99.99};
        for (JsonNode vector : kit.get("vectors")) {
            Histogram histogram = NetTestApiStatistics.newLagHistogram();
            if (vector.has("recorded_range")) {
                JsonNode range = vector.get("recorded_range");
                for (long value = range.get("from").asLong(); value <= range.get("to").asLong(); value++) {
                    histogram.recordValueWithCount(NetTestApiStatistics.histogramValue(value),
                        range.get("count_each").asLong());
                }
            }
            if (vector.has("recorded")) {
                for (JsonNode pair : vector.get("recorded")) {
                    histogram.recordValueWithCount(NetTestApiStatistics.histogramValue(pair.get(0).asLong()),
                        pair.get(1).asLong());
                }
            }
            String about = vector.get("about").asText();
            assertEquals(about, vector.get("count").asLong(), histogram.getTotalCount());
            for (int i = 0; i < keys.length; i++) {
                assertEquals(about + " " + keys[i], vector.get(keys[i]).asLong(),
                    histogram.getValueAtPercentile(percentiles[i]));
            }
        }
    }

    private static long hex(JsonNode value) {
        return Long.parseUnsignedLong(value.asText().substring(2), 16);
    }
}
