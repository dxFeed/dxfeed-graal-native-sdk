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

@CContext(SystemNative.NativeDirectives.class)
public final class SystemNative {
    public static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/api/dxfg_system.h"));
        }
    }

    private SystemNative() {
        throw new IllegalStateException("Native class");
    }

    @CEntryPoint(name = "dxfg_system_set_property")
    private static ErrorCodes setSystemProperty(IsolateThread ignoredThread, CCharPointer key, CCharPointer value) {
        try {
            System.setProperty(CTypeConversion.toJavaString(key), CTypeConversion.toJavaString(value));
            return ErrorCodes.DXFG_EC_SUCCESS;
        } catch (NullPointerException e) {
            return ErrorCodes.DXFG_EC_NULL_POINTER_EX;
        } catch (IllegalArgumentException e) {
            return ErrorCodes.DXFG_EC_ILLEGAL_ARGUMENT_EX;
        } catch (SecurityException e) {
            return ErrorCodes.DXFG_EC_SECURITY_EX;
        } catch (Exception e) {
            return ErrorCodes.DXFG_EC_UNKNOWN_ERR;
        }
    }

    @CEntryPoint(name = "dxfg_system_get_property")
    private static CCharPointer getSystemProperty(IsolateThread ignoredThread, CCharPointer key) {
        try {
            return UtilsNative.createCString(System.getProperty(CTypeConversion.toJavaString(key)));
        } catch (Exception e) {
            return WordFactory.nullPointer();
        }
    }
}
