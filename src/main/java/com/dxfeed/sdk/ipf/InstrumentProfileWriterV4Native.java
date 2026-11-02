// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.ipf.InstrumentProfileWriter;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.javac.ByteListOutputStream;
import com.dxfeed.sdk.javac.DxfgByteList;
import com.dxfeed.sdk.javac.JavacV4Native;
import com.dxfeed.sdk.mappers.ListMapper;
import java.io.IOException;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The functions of {@code InstrumentProfileWriter}. They are v4 functions without an older twin: each one writes a list
 * of profile structures or of profile handles to a file or to a {@code dxfg_v4_byte_list} (see dxfg_v4_ipf.h).
 * Every argument is checked before anything is written.
 */
@CContext(IpfDirectives.class)
public final class InstrumentProfileWriterV4Native {

    private InstrumentProfileWriterV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileWriter_writeToFile",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileWriter_writeToFile(
            final IsolateThread ignoredThread,
            @CConst final CCharPointer file,
            @CConst final DxfgInstrumentProfile2ListPointer instrumentProfiles
    ) throws IOException {
        final String javaFile = requireString(file, "file");
        final List<InstrumentProfile> profiles = toJavaProfiles(instrumentProfiles);

        new InstrumentProfileWriter().writeToFile(javaFile, profiles);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileWriter_writeHandlesToFile",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileWriter_writeHandlesToFile(
            final IsolateThread ignoredThread,
            @CConst final CCharPointer file,
            @CConst final DxfgInstrumentProfileList instrumentProfiles
    ) throws IOException {
        final String javaFile = requireString(file, "file");
        final List<InstrumentProfile> profiles = toJavaProfiles(instrumentProfiles);

        new InstrumentProfileWriter().writeToFile(javaFile, profiles);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileWriter_writeToBytes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileWriter_writeToBytes(
            final IsolateThread ignoredThread,
            @CConst final CCharPointer name,
            @CConst final DxfgInstrumentProfile2ListPointer instrumentProfiles,
            @DxfgOut final WordPointer bytes
    ) throws IOException {
        if (bytes.isNull()) {
            throw new IllegalArgumentException("The `bytes` pointer is null");
        }

        final String javaName = requireString(name, "name");
        final List<InstrumentProfile> profiles = toJavaProfiles(instrumentProfiles);

        bytes.write(writeToByteList(javaName, profiles));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileWriter_writeHandlesToBytes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileWriter_writeHandlesToBytes(
            final IsolateThread ignoredThread,
            @CConst final CCharPointer name,
            @CConst final DxfgInstrumentProfileList instrumentProfiles,
            @DxfgOut final WordPointer bytes
    ) throws IOException {
        if (bytes.isNull()) {
            throw new IllegalArgumentException("The `bytes` pointer is null");
        }

        final String javaName = requireString(name, "name");
        final List<InstrumentProfile> profiles = toJavaProfiles(instrumentProfiles);

        bytes.write(writeToByteList(javaName, profiles));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    private static String requireString(final CCharPointer string, final String name) {
        if (string.isNull()) {
            throw new IllegalArgumentException("The `" + name + "` string is null");
        }

        return NativeUtils.MAPPER_STRING.toJava(string);
    }

    private static List<InstrumentProfile> toJavaProfiles(final DxfgInstrumentProfile2ListPointer instrumentProfiles) {
        if (instrumentProfiles.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfiles` list is null");
        }

        ListMapper.requireNoNullElements(instrumentProfiles, "instrumentProfiles");

        return NativeUtils.MAPPER_INSTRUMENT_PROFILES_2.toJavaList(instrumentProfiles);
    }

    private static List<InstrumentProfile> toJavaProfiles(final DxfgInstrumentProfileList instrumentProfiles) {
        if (instrumentProfiles.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfiles` list is null");
        }

        ListMapper.requireNoNullElements(instrumentProfiles, "instrumentProfiles");

        return NativeUtils.MAPPER_INSTRUMENT_PROFILES.toJavaList(instrumentProfiles);
    }

    private static DxfgByteList writeToByteList(
            final String name,
            final List<InstrumentProfile> profiles
    ) throws IOException {
        final ByteListOutputStream stream = new ByteListOutputStream();

        // The name selects plain text, gzip or zip, as for a file; the stream is not a file and is not closed.
        new InstrumentProfileWriter().write(stream, name, profiles);

        return JavacV4Native.toNativeByteList(stream);
    }
}
