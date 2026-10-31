// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SdkFeaturesTest {

    @Test
    public void versionIsTheOneOfTheBuild() {
        // The build sets the property to project.version for the tests, as for native-image.
        assertEquals(System.getProperty(SdkFeatures.VERSION_PROPERTY), SdkFeatures.VERSION);
        assertFalse(SdkFeatures.VERSION.isEmpty());
    }

    @Test
    public void advancedRevisionStartsAtOne() {
        assertEquals(1, SdkFeatures.ADVANCED_REVISION);
    }

    @Test
    public void eventQueueFeaturesAreSupported() {
        assertTrue(SdkFeatures.isSupported(SdkFeatures.EVENT_QUEUE));
        assertTrue(SdkFeatures.isSupported(SdkFeatures.SHARED_EVENT_QUEUE));
        assertTrue(SdkFeatures.isSupported(SdkFeatures.EVENT_QUEUE_READY_SIGNAL));
        assertTrue(SdkFeatures.isSupported("event_queue"));
        assertTrue(SdkFeatures.isSupported("shared_event_queue"));
        assertTrue(SdkFeatures.isSupported("event_queue_ready_signal"));
    }

    @Test
    public void unknownAndEmptyNamesAreNotSupported() {
        assertFalse(SdkFeatures.isSupported("no_such_feature"));
        assertFalse(SdkFeatures.isSupported(""));
        assertFalse(SdkFeatures.isSupported("EVENT_QUEUE"));
    }

    @Test
    public void nullNameIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> SdkFeatures.isSupported(null));
    }
}
