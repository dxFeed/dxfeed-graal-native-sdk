// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import java.util.Set;

/**
 * The release version, the advanced revision and the features of the SDK, which {@code dxfg_v4_system_get_version},
 * {@code dxfg_v4_system_get_advanced_revision} and {@code dxfg_v4_system_is_feature_supported} report (see
 * dxfg_v4_system.h).
 *
 * <p>The class is initialized when the native image is built, so the version is the one of the build.
 */
public final class SdkFeatures {

    /**
     * The system property with the version of the SDK: a build argument of native-image (and of the tests) set to
     * {@code project.version}.
     */
    public static final String VERSION_PROPERTY = "dxfg.sdk.version";

    /** The version of the SDK release, e.g. {@code 3.13.0}. */
    public static final String VERSION = requireVersion();

    /** The revision of the experimental functions: {@code DXFG_V4_ADVANCED_REVISION} of dxfg_v4_system.h. */
    public static final int ADVANCED_REVISION = 1;

    /** {@code DXFG_V4_FEATURE_EVENT_QUEUE}: the event queue of one subscription (MDAPI-466). */
    public static final String EVENT_QUEUE = "event_queue";

    /** {@code DXFG_V4_FEATURE_SHARED_EVENT_QUEUE}: the shared event queue and its executor (MDAPI-468). */
    public static final String SHARED_EVENT_QUEUE = "shared_event_queue";

    /** {@code DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL}: the ready signal of an event queue (MDAPI-485). */
    public static final String EVENT_QUEUE_READY_SIGNAL = "event_queue_ready_signal";

    private static final Set<String> FEATURES = Set.of(EVENT_QUEUE, SHARED_EVENT_QUEUE, EVENT_QUEUE_READY_SIGNAL);

    private SdkFeatures() {
    }

    /**
     * Tells whether the SDK supports the feature: an unknown name, also an empty one, is not supported.
     *
     * @param name the name of the feature
     * @return {@code true} if the feature is supported
     * @throws IllegalArgumentException if the name is {@code null}
     */
    public static boolean isSupported(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("The feature name is null");
        }

        return FEATURES.contains(name);
    }

    private static String requireVersion() {
        final String version = System.getProperty(VERSION_PROPERTY);

        if (version == null || version.isEmpty()) {
            throw new IllegalStateException("The system property " + VERSION_PROPERTY + " is not set");
        }

        return version;
    }
}
