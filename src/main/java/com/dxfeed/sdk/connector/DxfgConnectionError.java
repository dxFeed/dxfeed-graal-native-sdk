// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

/**
 * {@code dxfg_v4_connection_error_t}: a copy of a {@link ConnectionError}, allocated by the SDK with its strings.
 */
@CContext(MessageConnectorDirectives.class)
@CStruct("dxfg_v4_connection_error_t")
public interface DxfgConnectionError extends PointerBase {

    @CField("time")
    void setTime(long value);

    @CField("address")
    CCharPointer getAddress();

    @CField("address")
    void setAddress(CCharPointer value);

    @CField("category")
    void setCategory(int value);

    @CField("dropped")
    void setDropped(int value);

    @CField("exception_class_name")
    CCharPointer getExceptionClassName();

    @CField("exception_class_name")
    void setExceptionClassName(CCharPointer value);

    @CField("message")
    CCharPointer getMessage();

    @CField("message")
    void setMessage(CCharPointer value);

    @CField("count")
    void setCount(long value);
}
