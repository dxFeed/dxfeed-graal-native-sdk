// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.StringJoiner;

/**
 * How the library was built and what runs it, which {@code dxfg_v4_system_get_build_info} reports (see
 * dxfg_v4_system.h): the QDS version and the target machine come from the build, the vendor and the garbage collectors
 * from the runtime.
 *
 * <p>The class is initialized when the native image is built, so the QDS version and the target machine are the ones
 * of the build; the vendor and the garbage collectors are read on each call.
 */
public final class SdkBuildInfo {

    /**
     * The system property with the QDS version of the SDK: a build argument of native-image (and of the tests) set to
     * {@code qd.version}.
     */
    public static final String QD_VERSION_PROPERTY = "dxfg.sdk.qd.version";

    /**
     * The system property with the target machine of native-image ({@code -march}): a build argument set to
     * {@code march}, absent from the builds without {@code -march} (iOS) and from the tests.
     */
    public static final String MARCH_PROPERTY = "dxfg.sdk.march";

    /** {@code DXFG_V4_BUILD_INFO_QD_VERSION}: the QDS version, e.g. {@code 3.358}. */
    public static final String QD_VERSION = "qd_version";

    /** {@code DXFG_V4_BUILD_INFO_VENDOR}: the vendor and the version of the runtime. */
    public static final String VENDOR = "vendor";

    /** {@code DXFG_V4_BUILD_INFO_MARCH}: the target machine of native-image, e.g. {@code compatibility}. */
    public static final String MARCH = "march";

    /** {@code DXFG_V4_BUILD_INFO_GC}: the garbage collectors of the runtime. */
    public static final String GC = "gc";

    private static final String QD_VERSION_VALUE = emptyToNull(System.getProperty(QD_VERSION_PROPERTY));
    private static final String MARCH_VALUE = emptyToNull(System.getProperty(MARCH_PROPERTY));

    private SdkBuildInfo() {
    }

    /**
     * Returns the value of a key: {@link #QD_VERSION}, {@link #VENDOR}, {@link #MARCH} or {@link #GC}.
     *
     * @param key the key
     * @return the value, or {@code null} if the key is unknown (also an empty one) or the value is not known
     * @throws IllegalArgumentException if the key is {@code null}
     */
    public static String get(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("The key is null");
        }

        switch (key) {
            case QD_VERSION:
                return QD_VERSION_VALUE;
            case VENDOR:
                return vendor();
            case MARCH:
                return MARCH_VALUE;
            case GC:
                return garbageCollectors();
            default:
                return null;
        }
    }

    /** The vendor version of the runtime (e.g. {@code GraalVM CE 25.0.1+8.1}), else its vendor. */
    private static String vendor() {
        final String vendorVersion = emptyToNull(System.getProperty("java.vendor.version"));

        return vendorVersion != null ? vendorVersion : emptyToNull(System.getProperty("java.vm.vendor"));
    }

    /**
     * The names of the garbage collectors, separated by ", "; null when the runtime has none or fails to give them (as
     * {@link RuntimeStatistics}, a failure of the management beans means an unknown value, except a
     * {@link VirtualMachineError}).
     */
    private static String garbageCollectors() {
        final StringJoiner names = new StringJoiner(", ");

        try {
            for (final GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
                names.add(collector.getName());
            }
        } catch (final VirtualMachineError error) {
            throw error;
        } catch (final RuntimeException | Error ignored) {
            return null;
        }

        return names.length() == 0 ? null : names.toString();
    }

    private static String emptyToNull(final String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
