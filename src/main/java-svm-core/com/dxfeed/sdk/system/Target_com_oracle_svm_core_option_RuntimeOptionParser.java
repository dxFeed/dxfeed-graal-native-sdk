// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import com.oracle.svm.core.IsolateArgumentParser;
import com.oracle.svm.core.SubstrateOptions;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.option.RuntimeOptionParser;
import java.io.PrintStream;

/**
 * Prints why an isolate option was rejected (MDAPI-541).
 *
 * <p>When {@code graal_create_isolate()} gets an option that it cannot parse, GraalVM 25.0.1 returns
 * {@code ARGUMENT_PARSING_FAILED} (22) without a message if the caller does not ask it to exit, and the SDK callers do
 * not. Newer GraalVM versions print {@code Error: <reason>} to stderr before they return 22. This substitution prints
 * the same line in the libraries built with GraalVM 25.0.1 (macOS x64, iOS Simulator x64): it is the method of 25.0.1
 * with the print added. It is the only caller of the option parser at isolate creation.
 */
@TargetClass(RuntimeOptionParser.class)
final class Target_com_oracle_svm_core_option_RuntimeOptionParser {

    @Substitute
    public static String[] parseAndConsumeAllOptions(String[] initialArgs, boolean ignoreUnrecognized) {
        // The option prefixes of RuntimeOptionParser in GraalVM 25.0.1 (a substitution class may not declare fields).
        final String normalPrefix = "-XX:";
        final String graalPrefix = "-Djdk.graal.";
        final String legacyGraalPrefix = "-Dgraal.";
        final String xPrefix = "-X";

        try {
            String[] args = initialArgs;

            if (SubstrateOptions.ParseRuntimeOptions.getValue()) {
                // JDK code may access and cache system properties, so parse them early.
                args = Target_com_oracle_svm_core_properties_RuntimeSystemPropertyParser.parse(args,
                        graalPrefix, legacyGraalPrefix);
                args = RuntimeOptionParser.singleton().parse(args, normalPrefix, graalPrefix,
                        legacyGraalPrefix, xPrefix, ignoreUnrecognized);
            } else if (Target_com_oracle_svm_core_graal_RuntimeCompilation.isEnabled() &&
                    SubstrateOptions.supportCompileInIsolates() && IsolateArgumentParser.isCompilationIsolate()) {
                // Compilation isolates always need to parse the Native Image options that the main isolate passes.
                args = RuntimeOptionParser.singleton().parse(args, normalPrefix, null, null, xPrefix,
                        ignoreUnrecognized);
            }

            return args;
        } catch (IllegalArgumentException e) {
            // The line of newer GraalVM versions. A caller that asks to exit prints its own "error: " line after it.
            Target_com_oracle_svm_core_log_Log.logStream().println("Error: " + e.getMessage());

            throw e;
        }
    }
}

/** The system property parser of GraalVM 25.0.1 (its package is not exported to the SDK). */
@TargetClass(className = "com.oracle.svm.core.properties.RuntimeSystemPropertyParser")
final class Target_com_oracle_svm_core_properties_RuntimeSystemPropertyParser {

    @Alias
    static native String[] parse(String[] args, String graalOptionPrefix, String legacyGraalOptionPrefix);
}

/** Whether the image compiles at run time; folded at build time. */
@TargetClass(className = "com.oracle.svm.core.graal.RuntimeCompilation")
final class Target_com_oracle_svm_core_graal_RuntimeCompilation {

    @Alias
    static native boolean isEnabled();
}

/** The log of the VM: stderr unless the image redirects it. */
@TargetClass(className = "com.oracle.svm.core.log.Log")
final class Target_com_oracle_svm_core_log_Log {

    @Alias
    static native PrintStream logStream();
}
