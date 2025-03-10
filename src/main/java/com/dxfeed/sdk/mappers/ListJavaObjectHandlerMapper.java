// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerPointer;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListJavaObjectHandlerMapper
        extends ListMapper<Object, JavaObjectHandler<?>, DxfgJavaObjectHandlerPointer, DxfgJavaObjectHandlerList> {

    private final Mapper<Object, JavaObjectHandler<Object>> mapperJavaObjectHandler;

    public ListJavaObjectHandlerMapper(
            final Mapper<Object, JavaObjectHandler<Object>> mapperJavaObjectHandler
    ) {
        this.mapperJavaObjectHandler = mapperJavaObjectHandler;
    }

    @Override
    protected Object toJava(final JavaObjectHandler<?> nativeObject) {
        return mapperJavaObjectHandler.toJava((JavaObjectHandler<Object>) nativeObject);
    }

    @Override
    protected JavaObjectHandler<?> toNative(final Object javaObject) {
        return mapperJavaObjectHandler.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final JavaObjectHandler<?> nativeObject) {
        mapperJavaObjectHandler.release((JavaObjectHandler<Object>) nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgJavaObjectHandlerList.class);
    }
}
