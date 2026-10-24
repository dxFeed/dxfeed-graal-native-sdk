// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link InstrumentProfileNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(IpfDirectives.class)
public final class InstrumentProfileV4Native {

    private InstrumentProfileV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_new(
            final IsolateThread thread,
            @DxfgOut final WordPointer instrumentProfile
    ) {
        if (instrumentProfile.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfile` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfile result = InstrumentProfileNative.dxfg_InstrumentProfileReader_new(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfile.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_newCopy",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_newCopy(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer instrumentProfile
    ) {
        if (instrumentProfile.isNull()) {
            throw new IllegalArgumentException("The `instrumentProfile` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgInstrumentProfile result = InstrumentProfileNative.dxfg_InstrumentProfileReader_new(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        instrumentProfile.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getType",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getType(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer type
    ) {
        if (type.isNull()) {
            throw new IllegalArgumentException("The `type` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getType(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        type.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setType",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setType(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setType(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getSymbol(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer symbol
    ) {
        if (symbol.isNull()) {
            throw new IllegalArgumentException("The `symbol` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getSymbol(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        symbol.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setSymbol(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setSymbol(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getDescription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getDescription(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer description
    ) {
        if (description.isNull()) {
            throw new IllegalArgumentException("The `description` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getDescription(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        description.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setDescription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setDescription(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setDescription(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getLocalSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getLocalSymbol(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer localSymbol
    ) {
        if (localSymbol.isNull()) {
            throw new IllegalArgumentException("The `localSymbol` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getLocalSymbol(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        localSymbol.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setLocalSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setLocalSymbol(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setLocalSymbol(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getLocalDescription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getLocalDescription(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer localDescription
    ) {
        if (localDescription.isNull()) {
            throw new IllegalArgumentException("The `localDescription` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getLocalDescription(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        localDescription.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setLocalDescription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setLocalDescription(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setLocalDescription(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getCountry",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getCountry(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer country
    ) {
        if (country.isNull()) {
            throw new IllegalArgumentException("The `country` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getCountry(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        country.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setCountry",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setCountry(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setCountry(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getOPOL",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getOPOL(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer opol
    ) {
        if (opol.isNull()) {
            throw new IllegalArgumentException("The `opol` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getOPOL(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        opol.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setOPOL",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setOPOL(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setOPOL(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getExchangeData",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getExchangeData(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer exchangeData
    ) {
        if (exchangeData.isNull()) {
            throw new IllegalArgumentException("The `exchangeData` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getExchangeData(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        exchangeData.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setExchangeData",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setExchangeData(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setExchangeData(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getExchanges",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getExchanges(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer exchanges
    ) {
        if (exchanges.isNull()) {
            throw new IllegalArgumentException("The `exchanges` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getExchanges(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        exchanges.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setExchanges",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setExchanges(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setExchanges(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getCurrency",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getCurrency(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer currency
    ) {
        if (currency.isNull()) {
            throw new IllegalArgumentException("The `currency` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getCurrency(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        currency.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setCurrency",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setCurrency(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setCurrency(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getBaseCurrency",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getBaseCurrency(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer baseCurrency
    ) {
        if (baseCurrency.isNull()) {
            throw new IllegalArgumentException("The `baseCurrency` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getBaseCurrency(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        baseCurrency.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setBaseCurrency",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setBaseCurrency(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setBaseCurrency(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getCFI",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getCFI(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer cfi
    ) {
        if (cfi.isNull()) {
            throw new IllegalArgumentException("The `cfi` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getCFI(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        cfi.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setCFI",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setCFI(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setCFI(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getISIN",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getISIN(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer isin
    ) {
        if (isin.isNull()) {
            throw new IllegalArgumentException("The `isin` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getISIN(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        isin.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setISIN",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setISIN(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setISIN(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getSEDOL",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getSEDOL(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer sedol
    ) {
        if (sedol.isNull()) {
            throw new IllegalArgumentException("The `sedol` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getSEDOL(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sedol.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setSEDOL",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setSEDOL(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setSEDOL(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getCUSIP",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getCUSIP(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer cusip
    ) {
        if (cusip.isNull()) {
            throw new IllegalArgumentException("The `cusip` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getCUSIP(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        cusip.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setCUSIP",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setCUSIP(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setCUSIP(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getICB",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getICB(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final CInt32Pointer icb
    ) {
        if (icb.isNull()) {
            throw new IllegalArgumentException("The `icb` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_getICB(thread, ip);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        icb.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setICB",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setICB(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final int value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setICB(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getSIC",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getSIC(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final CInt32Pointer sic
    ) {
        if (sic.isNull()) {
            throw new IllegalArgumentException("The `sic` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_getSIC(thread, ip);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sic.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setSIC",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setSIC(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final int value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setSIC(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getMultiplier",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getMultiplier(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final CDoublePointer multiplier
    ) {
        if (multiplier.isNull()) {
            throw new IllegalArgumentException("The `multiplier` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final double result = InstrumentProfileNative.dxfg_InstrumentProfile_getMultiplier(thread, ip);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_NEGATIVE_INFINITY)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        multiplier.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setMultiplier",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setMultiplier(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final double value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setMultiplier(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getProduct",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getProduct(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer product
    ) {
        if (product.isNull()) {
            throw new IllegalArgumentException("The `product` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getProduct(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        product.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setProduct",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setProduct(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setProduct(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getUnderlying",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getUnderlying(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer underlying
    ) {
        if (underlying.isNull()) {
            throw new IllegalArgumentException("The `underlying` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getUnderlying(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        underlying.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setUnderlying",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setUnderlying(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setUnderlying(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getSPC",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getSPC(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final CDoublePointer spc
    ) {
        if (spc.isNull()) {
            throw new IllegalArgumentException("The `spc` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final double result = InstrumentProfileNative.dxfg_InstrumentProfile_getSPC(thread, ip);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_NEGATIVE_INFINITY)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        spc.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setSPC",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setSPC(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final double value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setSPC(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getAdditionalUnderlyings",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getAdditionalUnderlyings(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer additionalUnderlyings
    ) {
        if (additionalUnderlyings.isNull()) {
            throw new IllegalArgumentException("The `additionalUnderlyings` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getAdditionalUnderlyings(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        additionalUnderlyings.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setAdditionalUnderlyings",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setAdditionalUnderlyings(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setAdditionalUnderlyings(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getMMY",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getMMY(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer mmy
    ) {
        if (mmy.isNull()) {
            throw new IllegalArgumentException("The `mmy` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getMMY(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        mmy.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setMMY",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setMMY(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setMMY(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getExpiration",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getExpiration(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final CInt32Pointer expiration
    ) {
        if (expiration.isNull()) {
            throw new IllegalArgumentException("The `expiration` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_getExpiration(thread, ip);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        expiration.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setExpiration",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setExpiration(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final int value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setExpiration(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getLastTrade",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getLastTrade(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final CInt32Pointer lastTrade
    ) {
        if (lastTrade.isNull()) {
            throw new IllegalArgumentException("The `lastTrade` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_getLastTrade(thread, ip);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        lastTrade.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setLastTrade",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setLastTrade(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final int value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setLastTrade(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getStrike",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getStrike(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final CDoublePointer strike
    ) {
        if (strike.isNull()) {
            throw new IllegalArgumentException("The `strike` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final double result = InstrumentProfileNative.dxfg_InstrumentProfile_getStrike(thread, ip);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_NEGATIVE_INFINITY)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        strike.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setStrike",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setStrike(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final double value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setStrike(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getOptionType",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getOptionType(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer optionType
    ) {
        if (optionType.isNull()) {
            throw new IllegalArgumentException("The `optionType` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getOptionType(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        optionType.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setOptionType",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setOptionType(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setOptionType(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getExpirationStyle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getExpirationStyle(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer expirationStyle
    ) {
        if (expirationStyle.isNull()) {
            throw new IllegalArgumentException("The `expirationStyle` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getExpirationStyle(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        expirationStyle.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setExpirationStyle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setExpirationStyle(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setExpirationStyle(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getSettlementStyle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getSettlementStyle(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer settlementStyle
    ) {
        if (settlementStyle.isNull()) {
            throw new IllegalArgumentException("The `settlementStyle` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getSettlementStyle(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        settlementStyle.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setSettlementStyle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setSettlementStyle(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setSettlementStyle(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getPriceIncrements",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getPriceIncrements(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer priceIncrements
    ) {
        if (priceIncrements.isNull()) {
            throw new IllegalArgumentException("The `priceIncrements` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getPriceIncrements(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        priceIncrements.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setPriceIncrements",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setPriceIncrements(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setPriceIncrements(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getTradingHours",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getTradingHours(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer tradingHours
    ) {
        if (tradingHours.isNull()) {
            throw new IllegalArgumentException("The `tradingHours` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getTradingHours(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        tradingHours.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setTradingHours",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setTradingHours(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setTradingHours(thread, ip, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getField(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer name,
            @DxfgOut final WordPointer field
    ) {
        if (field.isNull()) {
            throw new IllegalArgumentException("The `field` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getField(thread, ip, name);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        field.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setField(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer name,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setField(thread, ip, name, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getNumericField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getNumericField(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer name,
            @DxfgOut final CDoublePointer numericField
    ) {
        if (numericField.isNull()) {
            throw new IllegalArgumentException("The `numericField` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final double result = InstrumentProfileNative.dxfg_InstrumentProfile_getNumericField(thread, ip, name);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_NEGATIVE_INFINITY)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        numericField.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setNumericField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setNumericField(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer name,
            final double value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setNumericField(thread, ip, name, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getDateField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getDateField(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer name,
            @DxfgOut final CInt32Pointer dateField
    ) {
        if (dateField.isNull()) {
            throw new IllegalArgumentException("The `dateField` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_getDateField(thread, ip, name);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        dateField.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_setDateField",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_setDateField(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            final CCharPointer name,
            final int value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_setDateField(thread, ip, name, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_getNonEmptyCustomFieldNames",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_getNonEmptyCustomFieldNames(
            final IsolateThread thread,
            final DxfgInstrumentProfile ip,
            @DxfgOut final WordPointer stringList
    ) {
        if (stringList.isNull()) {
            throw new IllegalArgumentException("The `stringList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgCStringListPointer result = InstrumentProfileNative.dxfg_InstrumentProfile_getNonEmptyCustomFieldNames(thread, ip);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        stringList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_InstrumentProfile_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_InstrumentProfile_release(
            final IsolateThread thread,
            final DxfgInstrumentProfile dxfgInstrumentProfile
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = InstrumentProfileNative.dxfg_InstrumentProfile_release(thread, dxfgInstrumentProfile);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
