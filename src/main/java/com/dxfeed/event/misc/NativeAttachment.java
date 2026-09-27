// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.misc;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The attachment of {@link Message} and {@link Configuration} on the native side: a UTF-8 string or NULL
 * (see dxfg_message_t.attachment).
 */
final class NativeAttachment {

    private static final Logger logger = Logger.getLogger(NativeAttachment.class.getName());

    private NativeAttachment() {
    }

    /**
     * Returns a string attachment as is, the string representation of other objects (for display only: they come back
     * to Java as strings) and null when there is no attachment.
     *
     * <p>The attachment is deserialized on the first access. When it fails (e.g. the class of the attachment is not
     * registered for the native image), the attachment is null, so that the other events are still delivered.
     */
    static String toNative(final Supplier<Object> attachment, final String eventName, final String eventSymbol) {
        try {
            final Object object = attachment.get();

            return object == null ? null : object.toString();
        } catch (final RuntimeException e) {
            logger.log(Level.WARNING, "Cannot get the attachment of " + eventName + " " + eventSymbol, e);

            return null;
        }
    }
}
