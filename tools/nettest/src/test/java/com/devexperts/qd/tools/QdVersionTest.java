// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.devexperts.qd.QDFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * NetTestApi subclasses package-private classes of qds-tools, so it must be built against the QD version of the SDK.
 */
public class QdVersionTest {
    private static final Pattern QD_VERSION = Pattern.compile("<qd\\.version>\\s*([^<\\s]+)\\s*</qd\\.version>");

    @Test
    public void toolQdVersionEqualsSdkQdVersion() throws IOException {
        String sdkVersion = qdVersion(Paths.get(System.getProperty("nettest.rootPom", "../../pom.xml")));
        String toolVersion = qdVersion(Paths.get("pom.xml"));

        assertEquals("qd.version of tools/nettest/pom.xml (" + toolVersion + ") must equal qd.version of the SDK's " +
            "pom.xml (" + sdkVersion + ")", sdkVersion, toolVersion);
    }

    @Test
    public void classpathQdVersionEqualsToolQdVersion() throws IOException {
        String toolVersion = qdVersion(Paths.get("pom.xml"));
        String classpathVersion = QDFactory.class.getPackage().getImplementationVersion();

        assertEquals("the QD version on the classpath (" + classpathVersion + ") must equal qd.version of " +
            "tools/nettest/pom.xml (" + toolVersion + ")", toolVersion, classpathVersion);
    }

    private static String qdVersion(Path pom) throws IOException {
        Matcher matcher = QD_VERSION.matcher(Files.readString(pom));

        assertNotNull("no qd.version in " + pom.toAbsolutePath().normalize(), matcher.find() ? matcher : null);

        return matcher.group(1);
    }
}
