// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordFactory;

public class StringMapper extends Mapper<String, CCharPointer> {

    @Override
    public CCharPointer toNative(final String javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }
        // TODO: Avoid creating a byte array, use the previously allocated one instead. [MDAPI-240]
        final byte[] bytes = javaObject.getBytes(StandardCharsets.UTF_8);
        // Alloc +1 byte for null-terminate.
        final CCharPointer nativeObject = UnmanagedMemory.malloc(bytes.length + 1);
        for (int i = 0; i < bytes.length; ++i) {
            nativeObject.write(i, bytes[i]);
        }
        // Writes null-terminate.
        nativeObject.write(bytes.length, (byte) 0);
        return nativeObject;
    }

    @Override
    public void fillNative(final String javaObject, final CCharPointer nativeObject, boolean clean) {
        throw new IllegalStateException();
    }

    @Override
    public void cleanNative(final CCharPointer nativeObject) {
        // nothing
    }

    @Override
    protected String doToJava(final CCharPointer nativeObject) {
        return CTypeConversion.toJavaString(
                nativeObject,
                strlen(nativeObject),
                StandardCharsets.UTF_8
        );
    }

    // Public API replacement of SubstrateUtil.strlen, which is internal and was moved in GraalVM 25.4.
    private static UnsignedWord strlen(final CCharPointer str) {
        int length = 0;
        while (str.read(length) != 0) {
            ++length;
        }
        return WordFactory.unsigned(length);
    }

    @Override
    public void fillJava(final CCharPointer nativeObject, final String javaObject) {
        throw new IllegalStateException("The Java object does not support setters.");
    }
}
