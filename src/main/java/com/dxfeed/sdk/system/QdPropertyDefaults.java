// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

/**
 * The SDK defaults of the QD system properties. They are applied before every endpoint is created
 * (QD reads them when the first endpoint starts the monitoring), and the values set by the user take precedence.
 */
public final class QdPropertyDefaults {

    /**
     * Set to {@code true} to enable the QD {@code TimeSyncTracker}, which is disabled in the SDK by default.
     */
    public static final String TIME_SYNC_TRACKER_ENABLE_PROPERTY = "com.dxfeed.sdk.TimeSyncTracker.enable";

    /*
     * The MARS plugin TimeSyncTracker sends UDP multicast packets (239.192.51.45:5145 by default) from every process
     * to compare the clocks of the hosts. An application that embeds the SDK does not expect such traffic.
     * QD skips a service (see com.devexperts.services.Services) if "<class name>.disable" is set to any value.
     */
    private static final String TIME_SYNC_TRACKER_DISABLE_PROPERTY =
            "com.devexperts.qd.tools.TimeSyncTracker$PluginFactory.disable";

    private static boolean timeSyncTrackerDisabledBySdk;

    private QdPropertyDefaults() {
    }

    /**
     * Applies the defaults. Must be called before an endpoint is created.
     */
    public static synchronized void apply() {
        final boolean enable = Boolean.parseBoolean(System.getProperty(TIME_SYNC_TRACKER_ENABLE_PROPERTY));
        if (!enable && System.getProperty(TIME_SYNC_TRACKER_DISABLE_PROPERTY) == null) {
            System.setProperty(TIME_SYNC_TRACKER_DISABLE_PROPERTY, "true");
            timeSyncTrackerDisabledBySdk = true;
        } else if (enable && timeSyncTrackerDisabledBySdk) {
            System.clearProperty(TIME_SYNC_TRACKER_DISABLE_PROPERTY);
            timeSyncTrackerDisabledBySdk = false;
        }
    }
}
