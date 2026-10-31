// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CFunction.Transition;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.word.PointerBase;
import org.graalvm.word.SignedWord;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordFactory;

/**
 * The ready signals of the OS for {@link EventQueueV4Native#dxfg_v4_EventQueue_setReadySignal}: the
 * {@link EventQueue.Signaller}s that write to a file descriptor (Linux, macOS, iOS) or set a Windows event, with the
 * checks of the object at the time it is set (see {@code dxfg_v4_ready_signal_kind_t} of dxfg_v4_event_queue.h).
 *
 * <p>The calls are the SDK's own {@code @CFunction} declarations, without a {@code @CContext} and a
 * {@code @CLibrary}: native-image uses its built-in directives for them and adds no C query. The class of the OS is
 * chosen when the image is built, so an image never references the symbols of the other OS. The class is native
 * only: {@link EventQueue} sees nothing but {@link EventQueue.Signaller}, so the JUnit tests never load it.
 */
final class ReadySignalNative {

    // The values of dxfg_v4_ready_signal_kind_t of dxfg_v4_event_queue.h.
    static final int KIND_NONE = 0;
    static final int KIND_FD = 1;
    static final int KIND_WINDOWS_EVENT = 2;

    private ReadySignalNative() {
    }

    /**
     * Checks the object and makes the signal for it.
     *
     * @param kind the kind of the object, one of the {@code KIND_} values
     * @param handle the descriptor (a kind of descriptor) or the {@code HANDLE} (a Windows event); ignored for
     *               {@link #KIND_NONE}
     * @return the signal, or {@code null} for {@link #KIND_NONE} (removes the signal)
     * @throws IllegalArgumentException if the kind is unknown or belongs to another OS, or the object does not pass
     *                                  the checks; the message names the fix
     */
    static EventQueue.Signaller create(final int kind, final long handle) {
        if (kind == KIND_NONE) {
            return null;
        }
        if (kind != KIND_FD && kind != KIND_WINDOWS_EVENT) {
            throw new IllegalArgumentException("The `kind` is unknown: " + kind + "; use DXFG_V4_READY_SIGNAL_NONE, "
                    + "DXFG_V4_READY_SIGNAL_FD or DXFG_V4_READY_SIGNAL_WINDOWS_EVENT");
        }

        return Os.INSTANCE.create(kind, handle);
    }

    /** The checks and the signals of one OS family, chosen when the image is built. */
    abstract static class Os {
        static final Os INSTANCE = Platform.includedIn(Platform.WINDOWS.class) ? new WindowsOs() : new PosixOs();

        /** The kind is {@link #KIND_FD} or {@link #KIND_WINDOWS_EVENT}. */
        abstract EventQueue.Signaller create(int kind, long handle);
    }

    /** Linux, macOS and iOS (iOS is a Darwin platform): a write of 8 bytes to a non-blocking descriptor. */
    @Platforms({Platform.LINUX.class, Platform.DARWIN.class})
    static final class PosixOs extends Os {
        private static final int F_GETFL = 3;
        private static final int O_ACCMODE = 3;
        private static final int O_RDONLY = 0;
        private static final int EINTR = 4;
        private static final int O_NONBLOCK = Platform.includedIn(Platform.DARWIN.class) ? 0x4 : 0x800;
        private static final int EAGAIN = Platform.includedIn(Platform.DARWIN.class) ? 35 : 11;

        @Override
        EventQueue.Signaller create(final int kind, final long fd) {
            if (kind != KIND_FD) {
                throw new IllegalArgumentException("DXFG_V4_READY_SIGNAL_WINDOWS_EVENT is for Windows; "
                        + "pass the file descriptor with DXFG_V4_READY_SIGNAL_FD");
            }
            if (fd < 0 || fd > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("The file descriptor is out of range: " + fd
                        + "; pass an open descriptor from 0 to INT32_MAX");
            }

            final int flags = PosixCalls.fcntl((int) fd, F_GETFL);
            if (flags < 0) {
                throw new IllegalArgumentException("The file descriptor " + fd + " is not usable: fcntl(F_GETFL) "
                        + "failed, errno " + errno() + "; pass an open descriptor and keep it open while it is the "
                        + "signal");
            }
            if ((flags & O_NONBLOCK) == 0) {
                throw new IllegalArgumentException("The file descriptor " + fd + " is blocking: make the descriptor "
                        + "non-blocking (O_NONBLOCK; eventfd with EFD_NONBLOCK, or pipe2 with O_NONBLOCK)");
            }
            if ((flags & O_ACCMODE) == O_RDONLY) {
                throw new IllegalArgumentException("The file descriptor " + fd + " is open for reading only: pass the "
                        + "write end of the pipe");
            }

            return new FdSignaller((int) fd);
        }

        private static int errno() {
            // Not through a field: the platform check must fold, so that an image has the errno of its OS only.
            final CIntPointer location = Platform.includedIn(Platform.DARWIN.class)
                    ? DarwinErrno.location() : LinuxErrno.location();

            return location.read();
        }

        /** Writes the 8-byte value 1: an {@code eventfd} adds 1 to its counter, a pipe gets one token. */
        private static final class FdSignaller implements EventQueue.Signaller {
            private final int fd;

            FdSignaller(final int fd) {
                this.fd = fd;
            }

            @Override
            public String signal() {
                // Native byte order: x86-64 and arm64 are little-endian, as an eventfd expects.
                final CLongPointer value = StackValue.get(Long.BYTES);
                value.write(1L);

                while (true) {
                    if (PosixCalls.write(fd, value, WordFactory.unsigned(Long.BYTES)).greaterOrEqual(0)) {
                        return null;
                    }

                    // Right after the write, on the same thread: the TO_NATIVE transition keeps errno.
                    final int errno = errno();
                    if (errno == EINTR) {
                        continue;
                    }
                    if (errno == EAGAIN) {
                        // A full pipe or a saturated eventfd is already readable.
                        return null;
                    }

                    return "write failed, errno " + errno;
                }
            }
        }
    }

    /** Windows: {@code SetEvent} on the event of the host. */
    @Platforms(Platform.WINDOWS.class)
    static final class WindowsOs extends Os {
        @Override
        EventQueue.Signaller create(final int kind, final long handle) {
            if (kind != KIND_WINDOWS_EVENT) {
                throw new IllegalArgumentException("DXFG_V4_READY_SIGNAL_FD is for Linux, macOS and iOS; "
                        + "pass the event with DXFG_V4_READY_SIGNAL_WINDOWS_EVENT");
            }
            if (handle == 0 || handle == -1) {
                throw new IllegalArgumentException("The event handle is " + handle
                        + ": pass the HANDLE of an event, as (int64_t)(intptr_t)handle");
            }

            return new EventSignaller(handle);
        }

        private static final class EventSignaller implements EventQueue.Signaller {
            private final long handle;

            EventSignaller(final long handle) {
                this.handle = handle;
            }

            @Override
            public String signal() {
                if (WindowsCalls.SetEvent(WordFactory.pointer(handle)) != 0) {
                    return null;
                }

                // Right after the call, on the same thread: the TO_NATIVE transition keeps the last error.
                return "SetEvent failed, error " + WindowsCalls.GetLastError();
            }
        }
    }

    /** {@code write} and {@code fcntl} of libc, the same for Linux and Darwin. */
    @Platforms({Platform.LINUX.class, Platform.DARWIN.class})
    static final class PosixCalls {
        private PosixCalls() {
        }

        @CFunction(transition = Transition.TO_NATIVE)
        static native SignedWord write(int fd, PointerBase buffer, UnsignedWord size);

        // fcntl is variadic: F_GETFL takes no third argument.
        @CFunction(transition = Transition.TO_NATIVE)
        static native int fcntl(int fd, int command);
    }

    @Platforms(Platform.LINUX.class)
    static final class LinuxErrno {
        private LinuxErrno() {
        }

        @CFunction(value = "__errno_location", transition = Transition.NO_TRANSITION)
        static native CIntPointer location();
    }

    @Platforms(Platform.DARWIN.class)
    static final class DarwinErrno {
        private DarwinErrno() {
        }

        @CFunction(value = "__error", transition = Transition.NO_TRANSITION)
        static native CIntPointer location();
    }

    @Platforms(Platform.WINDOWS.class)
    static final class WindowsCalls {
        private WindowsCalls() {
        }

        @CFunction(transition = Transition.TO_NATIVE)
        static native int SetEvent(PointerBase handle);

        @CFunction(transition = Transition.NO_TRANSITION)
        static native int GetLastError();
    }
}
