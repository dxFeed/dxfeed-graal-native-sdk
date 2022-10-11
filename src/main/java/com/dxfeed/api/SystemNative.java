// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api;

import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.WordFactory;

import static com.dxfeed.api.ErrorNative.ErrorCodes;

import java.util.*;

@CContext(SystemNative.SystemNativeDirectives.class)
public final class SystemNative {
    static class SystemNativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/dxf_graal_system.h"));
        }
    }

    private SystemNative() {
        throw new IllegalStateException("Native class");
    }

    @CEntryPoint(name = "dxf_graal_system_set_property")
    static ErrorCodes setSystemProperty(IsolateThread ignoredThread, CCharPointer key, CCharPointer value) {
        try {
            System.setProperty(CTypeConversion.toJavaString(key), CTypeConversion.toJavaString(value));
            return ErrorCodes.DX_EC_SUCCESS;
        } catch (NullPointerException e) {
            return ErrorCodes.DX_EC_NULL_POINTER_EX;
        } catch (IllegalArgumentException e) {
            return ErrorCodes.DX_EC_ILLEGAL_ARGUMENT_EX;
        } catch (SecurityException e) {
            return ErrorCodes.DX_EC_SECURITY_EX;
        } catch (Exception e) {
            return ErrorCodes.DX_EC_UNKNOWN_ERR;
        }
    }

    @CEntryPoint(name = "dxf_graal_system_get_property")
    static CCharPointer getSystemProperty(IsolateThread ignoredThread, CCharPointer key) {
        try {
            return UtilsNative.createCString(System.getProperty(CTypeConversion.toJavaString(key)));
        } catch (Exception e) {
            return WordFactory.nullPointer();
        }
    }
}
