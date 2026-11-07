// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import com.oracle.svm.core.jdk.RuntimeSupport;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

/**
 * Installs the no-op handlers of SIGPIPE and SIGXFSZ when the isolate is created, as GraalVM 25.4 does.
 * <p>
 * GraalVM 25.0.1, which builds the macOS x64 library (no newer GraalVM for macOS x64), installs them only in a startup
 * hook, which runs in {@code VMRuntime.initialize()}: a shared library never calls it, so SIGPIPE kept SIG_DFL and a
 * write to a socket closed by the peer could kill the process (MDAPI-501). This feature adds {@link IgnoredSignalsHook},
 * which runs the same code of GraalVM as an isolate initialization hook. {@code graal_create_isolate()} runs it after
 * it has parsed the isolate arguments, so {@code -XX:-EnableSignalHandling} still turns the handlers off. A handler or
 * SIG_IGN set by the application is kept, and the handlers are installed once per process.
 * <p>
 * Only the svm-core build (GraalVM before 25.4) has this class and registers the feature ({@code --features}).
 */
public final class IgnoredSignalsFeature implements Feature {

    @Override
    public boolean isInConfiguration(final IsInConfigurationAccess access) {
        return Platform.includedIn(Platform.LINUX.class) || Platform.includedIn(Platform.DARWIN.class);
    }

    @Override
    public String getDescription() {
        return "Installs the no-op handlers of SIGPIPE and SIGXFSZ at isolate creation (MDAPI-501)";
    }

    @Override
    public void afterRegistration(final AfterRegistrationAccess access) {
        // The hook object is created at build time and stored in the image heap.
        RuntimeClassInitialization.initializeAtBuildTime(IgnoredSignalsHook.class);
    }

    @Override
    public void beforeAnalysis(final BeforeAnalysisAccess access) {
        RuntimeSupport.getRuntimeSupport().addInitializationHook(new IgnoredSignalsHook());
    }
}
