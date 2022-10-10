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
import java.util.Collections;
import java.util.List;

@CContext(UtilsNative.UtilsNativeDirectives.class)

public class UtilsNative {
    static class UtilsNativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            /*
             * The header file with the C declarations that are imported. We use a helper class that
             * locates the file in our project structure.
             */
            return Collections.singletonList(ProjectHeaderFile.resolve("com.dxfeed", "src/main/c/dxf_graal_utils.h"));
        }
    }

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

    @CEntryPoint(name = "dxf_graal_utils_free")
    public static void free(IsolateThread ignoredThread, PointerBase pointer) {
        if (pointer.isNonNull()) {
            UnmanagedMemory.free(pointer);
        }
    }
}
