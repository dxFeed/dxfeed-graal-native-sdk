// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import org.graalvm.word.PointerBase;

import java.util.List;

public interface ListNativeMapper<V, T extends PointerBase> {
    T nativeObject(final List<V> jObjects);

    void delete(final T cObject, final int size);
}
