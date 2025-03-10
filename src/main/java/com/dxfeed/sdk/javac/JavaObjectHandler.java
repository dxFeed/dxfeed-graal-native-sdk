// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;
import org.graalvm.word.SignedWord;

@CContext(Directives.class)
@CStruct("dxfg_java_object_handler")
public interface JavaObjectHandler<T> extends PointerBase {

    @CField("java_object_handle")
    ObjectHandle getJavaObjectHandler();

    @CField("java_object_handle")
    void setJavaObjectHandler(ObjectHandle value);

    JavaObjectHandler<T> addressOf(int index);

    JavaObjectHandler<T> addressOf(SignedWord index);
}
