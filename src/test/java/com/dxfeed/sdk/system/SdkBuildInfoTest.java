// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SdkBuildInfoTest {

    @Test
    public void qdVersionIsTheOneOfTheBuild() {
        // The build sets the property to qd.version for the tests, as for native-image.
        final String qdVersion = SdkBuildInfo.get(SdkBuildInfo.QD_VERSION);

        assertNotNull(qdVersion);
        assertEquals(System.getProperty(SdkBuildInfo.QD_VERSION_PROPERTY), qdVersion);
    }

    @Test
    public void marchIsTheOneOfTheBuild() {
        // Only native-image gets -march and its property; the tests have none, so the value is null here.
        assertEquals(System.getProperty(SdkBuildInfo.MARCH_PROPERTY), SdkBuildInfo.get(SdkBuildInfo.MARCH));
    }

    @Test
    public void vendorAndGarbageCollectorsComeFromTheRuntime() {
        final String vendorVersion = System.getProperty("java.vendor.version");
        final String vendor = SdkBuildInfo.get(SdkBuildInfo.VENDOR);

        assertEquals(vendorVersion == null || vendorVersion.isEmpty() ? System.getProperty("java.vm.vendor")
                : vendorVersion, vendor);

        final String gc = SdkBuildInfo.get(SdkBuildInfo.GC);

        assertNotNull(gc);
        assertFalse(gc.isEmpty());
    }

    @Test
    public void unknownKeyHasNoValue() {
        assertNull(SdkBuildInfo.get("unknown"));
        assertNull(SdkBuildInfo.get(""));
        assertNull(SdkBuildInfo.get("QD_VERSION"));
    }

    @Test
    public void nullKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SdkBuildInfo.get(null));
    }

    @Test
    public void keysAreTheOnesOfTheHeader() {
        assertEquals("qd_version", SdkBuildInfo.QD_VERSION);
        assertEquals("vendor", SdkBuildInfo.VENDOR);
        assertEquals("march", SdkBuildInfo.MARCH);
        assertEquals("gc", SdkBuildInfo.GC);
    }
}
