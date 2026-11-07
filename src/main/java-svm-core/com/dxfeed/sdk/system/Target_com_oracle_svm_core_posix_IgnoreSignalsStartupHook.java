// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.posix.headers.Signal.SignalEnum;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

/**
 * The methods of GraalVM 25.0.1 that install the no-op handlers of SIGPIPE and SIGXFSZ (see
 * {@link IgnoredSignalsFeature}).
 */
@Platforms({Platform.LINUX.class, Platform.DARWIN.class})
@TargetClass(className = "com.oracle.svm.core.posix.IgnoreSignalsStartupHook")
final class Target_com_oracle_svm_core_posix_IgnoreSignalsStartupHook {

    /** Returns {@code true} once per process: the handlers are installed by the first caller only. */
    @Alias
    static native boolean isFirst();

    /** Installs the no-op handler of the signal if its action is {@code SIG_DFL}; a handler or SIG_IGN is kept. */
    @Alias
    static native void installNoopHandler(SignalEnum signal, boolean isSignalHandlingAllowed);
}
