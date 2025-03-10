// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.promise.Promise;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.feed.DxfgPromise;
import com.dxfeed.sdk.feed.DxfgPromiseList;
import com.dxfeed.sdk.feed.DxfgPromisePointer;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListPromiseMapper
        extends ListMapper<Promise<?>, DxfgPromise, DxfgPromisePointer, DxfgPromiseList> {

    protected final Mapper<Promise<?>, DxfgPromise> promiseMapper;

    public ListPromiseMapper(final Mapper<Promise<?>, DxfgPromise> promiseMapper) {
        this.promiseMapper = promiseMapper;
    }

    @Override
    protected Promise<?> toJava(final DxfgPromise nativeObject) {
        return NativeUtils.MAPPER_PROMISE.toJava(nativeObject);
    }

    @Override
    protected DxfgPromise toNative(final Promise<?> javaObject) {
        return NativeUtils.MAPPER_PROMISE.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final DxfgPromise nativeObject) {
        NativeUtils.MAPPER_PROMISE.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgPromiseList.class);
    }
}
