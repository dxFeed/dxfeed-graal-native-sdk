// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.mapper;

import org.graalvm.word.PointerBase;

public interface NativeMapper<V, T extends PointerBase> {
    T nativeObject(final V jObject);

    void delete(final T cObject);
}
