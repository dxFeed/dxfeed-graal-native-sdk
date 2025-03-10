// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.schedule.Session;
import com.dxfeed.sdk.schedule.DxfgSession;
import com.dxfeed.sdk.schedule.DxfgSessionList;
import com.dxfeed.sdk.schedule.DxfgSessionPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListSessionMapper
        extends ListMapper<Session, DxfgSession, DxfgSessionPointer, DxfgSessionList> {

    private final Mapper<Session, DxfgSession> mapper;

    public ListSessionMapper(final Mapper<Session, DxfgSession> mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Session toJava(final DxfgSession nativeObject) {
        return this.mapper.toJava(nativeObject);
    }

    @Override
    protected DxfgSession toNative(final Session javaObject) {
        return this.mapper.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final DxfgSession nativeObject) {
        this.mapper.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgSessionList.class);
    }
}
