// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feedfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link FeedFilterCategories}: the arguments are checked before the builder changes, a failed call leaves it as it was.
 */
public class FeedFilterCategoriesTest {

    private DXEndpoint endpoint;
    private DXFeedFilter.Builder builder;

    @Before
    public void createBuilder() {
        endpoint = DXEndpoint.create(DXEndpoint.Role.LOCAL_HUB);
        builder = DXFeedFilter.newBuilder(endpoint);
    }

    @After
    public void closeEndpoint() {
        endpoint.close();
    }

    @Test
    public void addKeepsTheOrder() {
        FeedFilterCategories.add(builder, "b", "B*");
        FeedFilterCategories.add(builder, "a", "A*");

        assertEquals(Arrays.asList("b", "a"), List.copyOf(builder.getCategories().keySet()));
    }

    @Test
    public void addRefusesNullEmptyAndDuplicateArguments() {
        FeedFilterCategories.add(builder, "a", "A*");

        assertThrows(IllegalArgumentException.class, () -> FeedFilterCategories.add(builder, null, "B*"));
        assertThrows(IllegalArgumentException.class, () -> FeedFilterCategories.add(builder, "", "B*"));
        assertThrows(IllegalArgumentException.class, () -> FeedFilterCategories.add(builder, "b", null));
        assertThrows(IllegalArgumentException.class, () -> FeedFilterCategories.add(builder, "b", ""));
        assertThrows(IllegalArgumentException.class, () -> FeedFilterCategories.add(builder, "a", "B*"));

        assertEquals(Map.of("a", "A*"), builder.getCategories());
    }

    @Test
    public void addAllAddsInTheOrderOfTheLists() {
        FeedFilterCategories.add(builder, "first", "Z*");
        FeedFilterCategories.addAll(builder, Arrays.asList("list", "range"), Arrays.asList("IBM,AAPL", "range-A-M-"));

        final Map<String, String> expected = new LinkedHashMap<>();
        expected.put("first", "Z*");
        expected.put("list", "IBM,AAPL");
        expected.put("range", "range-A-M-");
        assertEquals(expected, builder.getCategories());
        assertEquals(List.copyOf(expected.keySet()), List.copyOf(builder.getCategories().keySet()));
    }

    @Test
    public void addAllOfEmptyListsAddsNothing() {
        FeedFilterCategories.addAll(builder, Collections.emptyList(), Collections.emptyList());

        assertEquals(Collections.emptyMap(), builder.getCategories());
    }

    @Test
    public void addAllRefusesBadListsAndLeavesTheBuilderUnchanged() {
        FeedFilterCategories.add(builder, "a", "A*");

        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, null, List.of("B*")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, List.of("b"), null));
        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, List.of("b", "c"), List.of("B*")));
        // The bad element is the last one: the first ones must not have been added.
        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, Arrays.asList("b", null), List.of("B*", "C*")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, List.of("b", ""), List.of("B*", "C*")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, List.of("b", "c"), Arrays.asList("B*", null)));
        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, List.of("b", "c"), List.of("B*", "")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, List.of("b", "b"), List.of("B*", "C*")));
        assertThrows(IllegalArgumentException.class,
                () -> FeedFilterCategories.addAll(builder, List.of("b", "a"), List.of("B*", "C*")));

        assertEquals(Map.of("a", "A*"), builder.getCategories());
    }
}
