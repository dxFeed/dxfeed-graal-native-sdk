// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.DxfgAuthToken;
import com.dxfeed.sdk.javac.DxfgInputStream;
import java.io.IOException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link InstrumentProfileReaderNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(IpfDirectives.class)
public final class InstrumentProfileReaderV4Native {

    private InstrumentProfileReaderV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_new(
            final IsolateThread thread,
            @DxfgOut final WordPointer instrumentProfileReader
    ) {
        if (instrumentProfileReader.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfileReader` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileReader result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_new(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfileReader.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_getLastModified",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_getLastModified(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            @DxfgOut final CLongPointer lastModified
    ) {
        if (lastModified.isNull()) {
            throw new IllegalArgumentException("The `lastModified` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_getLastModified(
                        thread,
                        dxfgInstrumentProfileReader);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        lastModified.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_wasComplete",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_wasComplete(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            @DxfgOut final CInt32Pointer complete
    ) {
        if (complete.isNull()) {
            throw new IllegalArgumentException("The `complete` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_wasComplete(
                        thread,
                        dxfgInstrumentProfileReader);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        complete.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readFromFileAsHandles",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readFromFileAsHandles(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final CCharPointer address,
            @DxfgOut final WordPointer instrumentProfileList
    ) throws IOException {
        if (instrumentProfileList.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfileList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileList result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_readFromFile(
                        thread,
                        dxfgInstrumentProfileReader,
                        address);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfileList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readFromFileWithUserAndPasswordAsHandles",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readFromFileWithUserAndPasswordAsHandles(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final CCharPointer address,
            final CCharPointer user,
            final CCharPointer password,
            @DxfgOut final WordPointer instrumentProfileList
    ) throws IOException {
        if (instrumentProfileList.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfileList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileList result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_readFromFile2(
                        thread,
                        dxfgInstrumentProfileReader,
                        address,
                        user,
                        password);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfileList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readFromFileWithTokenAsHandles",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readFromFileWithTokenAsHandles(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final CCharPointer address,
            final DxfgAuthToken token,
            @DxfgOut final WordPointer instrumentProfileList
    ) throws IOException {
        if (instrumentProfileList.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfileList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileList result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_readFromFile3(
                        thread,
                        dxfgInstrumentProfileReader,
                        address,
                        token);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfileList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_resolveSourceURL",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_resolveSourceURL(
            final IsolateThread thread,
            final CCharPointer address,
            @DxfgOut final WordPointer url
    ) {
        if (url.isNull()) {
            throw new IllegalArgumentException("The `url` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_resolveSourceURL(
                        thread,
                        address);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        url.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readWithAddressAsHandles",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readWithAddressAsHandles(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final DxfgInputStream dxfgInputStream,
            final CCharPointer address,
            @DxfgOut final WordPointer instrumentProfileList
    ) throws IOException {
        if (instrumentProfileList.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfileList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileList result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_read2(
                        thread,
                        dxfgInstrumentProfileReader,
                        dxfgInputStream,
                        address);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfileList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readCompressedAsHandles",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readCompressedAsHandles(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final DxfgInputStream dxfgInputStream,
            @DxfgOut final WordPointer instrumentProfileList
    ) throws IOException {
        if (instrumentProfileList.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfileList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileList result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_readCompressed(
                        thread,
                        dxfgInstrumentProfileReader,
                        dxfgInputStream);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfileList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readAsHandles",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readAsHandles(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final DxfgInputStream dxfgInputStream,
            @DxfgOut final WordPointer instrumentProfileList
    ) throws IOException {
        if (instrumentProfileList.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfileList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfileList result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_read(
                        thread,
                        dxfgInstrumentProfileReader,
                        dxfgInputStream);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfileList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readFromFile",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readFromFile(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            @CConst final CCharPointer address,
            @DxfgOut final DxfgInstrumentProfile2ListPointerPointer instrumentProfiles
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_readFromFile7(
                        thread,
                        dxfgInstrumentProfileReader,
                        address,
                        instrumentProfiles);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readFromFileWithUserAndPassword",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readFromFileWithUserAndPassword(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            @CConst final CCharPointer address,
            @CConst final CCharPointer user,
            @CConst final CCharPointer password,
            @DxfgOut final DxfgInstrumentProfile2ListPointerPointer instrumentProfiles
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_readFromFile8(
                        thread,
                        dxfgInstrumentProfileReader,
                        address,
                        user,
                        password,
                        instrumentProfiles);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readFromFileWithToken",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readFromFileWithToken(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            @CConst final CCharPointer address,
            final DxfgAuthToken token,
            @DxfgOut final DxfgInstrumentProfile2ListPointerPointer instrumentProfiles
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_readFromFile9(
                        thread,
                        dxfgInstrumentProfileReader,
                        address,
                        token,
                        instrumentProfiles);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_read",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_read(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final DxfgInputStream dxfgInputStream,
            @DxfgOut final DxfgInstrumentProfile2ListPointerPointer instrumentProfiles
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_read5(
                        thread,
                        dxfgInstrumentProfileReader,
                        dxfgInputStream,
                        instrumentProfiles);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readWithAddress",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readWithAddress(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final DxfgInputStream dxfgInputStream,
            @CConst final CCharPointer address,
            @DxfgOut final DxfgInstrumentProfile2ListPointerPointer instrumentProfiles
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_read6(
                        thread,
                        dxfgInstrumentProfileReader,
                        dxfgInputStream,
                        address,
                        instrumentProfiles);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfileReader_readCompressed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfileReader_readCompressed(
            final IsolateThread thread,
            final DxfgInstrumentProfileReader dxfgInstrumentProfileReader,
            final DxfgInputStream dxfgInputStream,
            @DxfgOut final DxfgInstrumentProfile2ListPointerPointer instrumentProfiles
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileReaderNative.dxfg_InstrumentProfileReader_readCompressed3(
                        thread,
                        dxfgInstrumentProfileReader,
                        dxfgInputStream,
                        instrumentProfiles);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
