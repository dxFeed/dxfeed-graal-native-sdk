// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api;

import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

import java.util.Collections;
import java.util.List;

@CContext(ErrorNative.ErrorNativeDirectives.class)
public class ErrorNative {
    static class ErrorNativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            /*
             * The header file with the C declarations that are imported. We use a helper class that
             * locates the file in our project structure.
             */
            return Collections.singletonList(ProjectHeaderFile.resolve("com.dxfeed", "src/main/c/dxf_graal_error_codes.h"));
        }
    }

    @CEnum("dxf_graal_error_code_t")
    public enum ErrorCodes {
        DX_EC_SUCCESS,
        DX_EC_UNKNOWN_ERR,
        DX_EC_NULL_POINTER_EX,
        DX_EC_ILLEGAL_ARGUMENT_EX,
        DX_EC_SECURITY_EX;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ErrorCodes fromCValue(int value);
    }
}
