// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api;

import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

@CContext(UtilsNative.NativeDirectives.class)
public final class UtilsNative {
    public static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/dxf_graal_utils.h"));
        }
    }

    private UtilsNative() {
        throw new IllegalStateException("Native class");
    }

    @CEntryPoint(name = "dxf_graal_utils_free")
    private static void free(IsolateThread ignoredThread, PointerBase pointer) {
        if (pointer.isNonNull()) {
            UnmanagedMemory.free(pointer);
        }
    }

    /**
     * Creates a null-terminated C-string in UTF-8 format from Java string.
     * The created string must be freed by {@link UnmanagedMemory#free(PointerBase) UnmanagedMemory.free}.
     * The native code frees this string with dxf_graal_utils_free().
     *
     * @param string The Java string.
     * @return Returns unmanaged pointer to C-string in UTF-8 format or null pointer if Java string is null.
     */
    public static CCharPointer createCString(String string) {
        if (string == null) {
            return WordFactory.nullPointer();
        }
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        // Alloc +1 byte for null-terminate.
        CCharPointer pointer = UnmanagedMemory.malloc(bytes.length + 1);
        for (int i = 0; i < bytes.length; ++i) {
            pointer.write(i, bytes[i]);
        }
        // Writes null-terminate.
        pointer.write(bytes.length, (byte) 0);
        return pointer;
    }
}
