// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import com.oracle.svm.core.SubstrateOptions;
import com.oracle.svm.core.jdk.RuntimeSupport;
import com.oracle.svm.core.posix.headers.Signal.SignalEnum;

/**
 * The isolate initialization hook of {@link IgnoredSignalsFeature}: installs the no-op handlers of SIGPIPE and SIGXFSZ
 * once per process, unless {@code -XX:-EnableSignalHandling} is set. It is a class of its own because the feature,
 * which implements a build-time interface, cannot be reachable at run time.
 */
final class IgnoredSignalsHook implements RuntimeSupport.Hook {

    @Override
    public void execute(final boolean isFirstIsolate) {
        final boolean isSignalHandlingAllowed = SubstrateOptions.EnableSignalHandling.getValue();
        if (isSignalHandlingAllowed && Target_com_oracle_svm_core_posix_IgnoreSignalsStartupHook.isFirst()) {
            Target_com_oracle_svm_core_posix_IgnoreSignalsStartupHook.installNoopHandler(
                SignalEnum.SIGPIPE, isSignalHandlingAllowed);
            Target_com_oracle_svm_core_posix_IgnoreSignalsStartupHook.installNoopHandler(
                SignalEnum.SIGXFSZ, isSignalHandlingAllowed);
        }
    }
}
