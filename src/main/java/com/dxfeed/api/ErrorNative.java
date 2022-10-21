// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api;

import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

import java.util.*;

@CContext(ErrorNative.NativeDirectives.class)
public final class ErrorNative {
    public static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/api/dxfg_error_codes.h"));
        }
    }

    private ErrorNative() {
        throw new IllegalStateException("Native class");
    }

    @CEnum("dxfg_error_code_t")
    public enum ErrorCodes {
        DXFG_EC_SUCCESS,
        DXFG_EC_UNKNOWN_ERR,
        DXFG_EC_NULL_POINTER_EX,
        DXFG_EC_ILLEGAL_ARGUMENT_EX,
        DXFG_EC_SECURITY_EX,
        DXFG_EC_ILLEGAL_STATE_EX,
        DXFG_EC_INTERRUPTED_EX,
        DXFG_EC_UNKNOWN_DESCRIPTOR;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ErrorCodes fromCValue(int value);
    }
}
