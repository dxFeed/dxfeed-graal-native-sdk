// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feedfilter;

import com.dxfeed.api.DXFeedFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds the categories of the C API to a {@link DXFeedFilter.Builder}: checks the arguments first, so that a failed call
 * leaves the builder unchanged and fails with {@link IllegalArgumentException} (Java throws
 * {@link NullPointerException} for a null name or expression).
 */
public final class FeedFilterCategories {

    private FeedFilterCategories() {
    }

    /**
     * Adds a category: {@code DXFeedFilter.Builder.withCategory(String, String)}.
     *
     * @throws IllegalArgumentException if the name or the expression is null or empty, or the builder already has
     *                                  a category of the name
     */
    public static void add(final DXFeedFilter.Builder builder, final String categoryName,
            final String filterExpression) {
        check(categoryName, filterExpression, 0);

        if (builder.getCategories().containsKey(categoryName)) {
            throw new IllegalArgumentException("Duplicate category name: " + categoryName);
        }

        builder.withCategory(categoryName, filterExpression);
    }

    /**
     * Adds the categories of two parallel lists in their order; checks both lists as a whole before the builder
     * changes.
     *
     * @throws IllegalArgumentException if a list is null, the lists have different sizes, a name or an expression is
     *                                  null or empty, or a name repeats (in the list or in the builder)
     */
    public static void addAll(final DXFeedFilter.Builder builder, final List<String> categoryNames,
            final List<String> filterExpressions) {
        if (categoryNames == null) {
            throw new IllegalArgumentException("The `categoryNames` list is null");
        }
        if (filterExpressions == null) {
            throw new IllegalArgumentException("The `filterExpressions` list is null");
        }
        if (categoryNames.size() != filterExpressions.size()) {
            throw new IllegalArgumentException("The lists have different sizes: " + categoryNames.size()
                    + " names and " + filterExpressions.size() + " expressions");
        }

        final Set<String> names = new HashSet<>(builder.getCategories().keySet());

        for (int i = 0; i < categoryNames.size(); i++) {
            final String categoryName = categoryNames.get(i);
            check(categoryName, filterExpressions.get(i), i);

            if (!names.add(categoryName)) {
                throw new IllegalArgumentException("Duplicate category name: " + categoryName);
            }
        }

        for (int i = 0; i < categoryNames.size(); i++) {
            builder.withCategory(categoryNames.get(i), filterExpressions.get(i));
        }
    }

    private static void check(final String categoryName, final String filterExpression, final int index) {
        if (categoryName == null || categoryName.isEmpty()) {
            throw new IllegalArgumentException("The category name #" + index + " is null or empty");
        }
        if (filterExpression == null || filterExpression.isEmpty()) {
            throw new IllegalArgumentException(
                    "The filter expression of the category " + categoryName + " is null or empty");
        }
    }
}
