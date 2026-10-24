// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt16Pointer;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.common.DxfgStringToDoubleMapEntryPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.DxfgRoundingMode;
import java.util.Arrays;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.word.WordFactory;

/**
 * The v4 twins of the functions of {@link GlossaryNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(GlossaryDirectives.class)
public final class GlossaryV4Native {

    private GlossaryV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_AdditionalUnderlyings_EMPTY",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AdditionalUnderlyings_EMPTY(
            final IsolateThread thread,
            @DxfgOut final DxfgAdditionalUnderlyingsHandlePointer emptyAdditionalUnderlyings
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_AdditionalUnderlyings_EMPTY(thread, emptyAdditionalUnderlyings);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_AdditionalUnderlyings_valueOf",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AdditionalUnderlyings_valueOf(
            final IsolateThread thread,
            @CConst final CCharPointer text,
            @DxfgOut final DxfgAdditionalUnderlyingsHandlePointer additionalUnderlyings
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_AdditionalUnderlyings_valueOf(thread, text, additionalUnderlyings);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_AdditionalUnderlyings_valueOfMap",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AdditionalUnderlyings_valueOfMap(
            final IsolateThread thread,
            @CConst final DxfgStringToDoubleMapEntryList map,
            @DxfgOut final DxfgAdditionalUnderlyingsHandlePointer additionalUnderlyings
    ) {
        final boolean empty = map.isNull() || map.getSize() <= 0;

        // The older function takes such an array as an empty map.
        if (!empty && map.getElements().isNull()) {
            throw new IllegalArgumentException(
                    "The `map` list of size " + map.getSize() + " has a null elements pointer");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_AdditionalUnderlyings_valueOf(
                        thread,
                        empty ? WordFactory.nullPointer() : map.getElements(),
                        empty ? 0 : map.getSize(),
                        additionalUnderlyings);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_AdditionalUnderlyings_getSPCFromText",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AdditionalUnderlyings_getSPCFromText(
            final IsolateThread thread,
            @CConst final CCharPointer text,
            @CConst final CCharPointer symbol,
            @DxfgOut final CDoublePointer spc
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_AdditionalUnderlyings_getSPC(thread, text, symbol, spc);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_AdditionalUnderlyings_getText",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AdditionalUnderlyings_getText(
            final IsolateThread thread,
            final DxfgAdditionalUnderlyingsHandle additionalUnderlyings,
            @DxfgOut final CCharPointerPointer text
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_AdditionalUnderlyings_getText(thread, additionalUnderlyings, text);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_AdditionalUnderlyings_getMap",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AdditionalUnderlyings_getMap(
            final IsolateThread thread,
            final DxfgAdditionalUnderlyingsHandle additionalUnderlyings,
            @DxfgOut final WordPointer map
    ) {
        if (map.isNull()) {
            throw new IllegalArgumentException("The `map` pointer is null");
        }

        // The older function writes the array and its size to the fields of the list.
        final DxfgStringToDoubleMapEntryList list = UnmanagedMemory.calloc(
                SizeOf.get(DxfgStringToDoubleMapEntryList.class));
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_AdditionalUnderlyings_getMap(
                        thread,
                        additionalUnderlyings,
                        list.addressOfElements(),
                        list.addressOfSize());

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            UnmanagedMemory.free(list);

            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        map.write(list);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_AdditionalUnderlyings_getSPC",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AdditionalUnderlyings_getSPC(
            final IsolateThread thread,
            final DxfgAdditionalUnderlyingsHandle additionalUnderlyings,
            @CConst final CCharPointer symbol,
            @DxfgOut final CDoublePointer spc
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_AdditionalUnderlyings_getSPC(
                        thread,
                        additionalUnderlyings,
                        symbol,
                        spc);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_EMPTY",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_EMPTY(
            final IsolateThread thread,
            @DxfgOut final DxfgCFIHandlePointer emptyCfi
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_EMPTY(thread, emptyCfi);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_valueOf",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_valueOf(
            final IsolateThread thread,
            @CConst final CCharPointer code,
            @DxfgOut final DxfgCFIHandlePointer cfi
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_valueOf(thread, code, cfi);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_valueOfIntCode",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_valueOfIntCode(
            final IsolateThread thread,
            final int intCode,
            @DxfgOut final DxfgCFIHandlePointer cfi
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_valueOf(thread, intCode, cfi);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_getCode",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_getCode(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CCharPointerPointer code
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_getCode(thread, cfi, code);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_getIntCode",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_getIntCode(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt32Pointer intCode
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_getIntCode(thread, cfi, intCode);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_getCategory",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_getCategory(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt16Pointer category
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_getCategory(thread, cfi, category);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_getGroup",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_getGroup(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt16Pointer group
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_getGroup(thread, cfi, group);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_isEquity",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_isEquity(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt32Pointer isEquity
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_isEquity(thread, cfi, isEquity);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_isDebtInstrument",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_isDebtInstrument(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt32Pointer isDebtInstrument
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_isDebtInstrument(thread, cfi, isDebtInstrument);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_isEntitlement",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_isEntitlement(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt32Pointer isEntitlement
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_isEntitlement(thread, cfi, isEntitlement);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_isOption",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_isOption(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt32Pointer isOption
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_isOption(thread, cfi, isOption);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_isFuture",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_isFuture(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt32Pointer isFuture
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_isFuture(thread, cfi, isFuture);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_isOther",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_isOther(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CInt32Pointer isOther
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_isOther(thread, cfi, isOther);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_decipher",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_decipher(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final WordPointer values
    ) {
        if (values.isNull()) {
            throw new IllegalArgumentException("The `values` pointer is null");
        }

        // The older function returns an array of handles; the list holds a pointer to a handle of each value.
        //noinspection DataFlowIssue
        values.write(NativeUtils.MAPPER_CFI_VALUES.toNativeList(
                Arrays.asList(NativeUtils.MAPPER_CFI.toJava(cfi).decipher())));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_describe",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_describe(
            final IsolateThread thread,
            final DxfgCFIHandle cfi,
            @DxfgOut final CCharPointerPointer description
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_describe(thread, cfi, description);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_Attribute_getName",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_Attribute_getName(
            final IsolateThread thread,
            final DxfgCFIAttributeHandle cfiAttribute,
            @DxfgOut final CCharPointerPointer name
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_Attribute_getName(thread, cfiAttribute, name);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_Attribute_getDescription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_Attribute_getDescription(
            final IsolateThread thread,
            final DxfgCFIAttributeHandle cfiAttribute,
            @DxfgOut final CCharPointerPointer description
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_Attribute_getDescription(thread, cfiAttribute, description);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_Attribute_getValues",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_Attribute_getValues(
            final IsolateThread thread,
            final DxfgCFIAttributeHandle cfiAttribute,
            @DxfgOut final WordPointer values
    ) {
        if (values.isNull()) {
            throw new IllegalArgumentException("The `values` pointer is null");
        }

        // The older function returns an array of handles; the list holds a pointer to a handle of each value.
        //noinspection DataFlowIssue
        values.write(NativeUtils.MAPPER_CFI_VALUES.toNativeList(
                Arrays.asList(NativeUtils.MAPPER_CFI_ATTRIBUTE.toJava(cfiAttribute).getValues())));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_Value_getAttribute",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_Value_getAttribute(
            final IsolateThread thread,
            final DxfgCFIValueHandle cfiValue,
            @DxfgOut final DxfgCFIAttributeHandlePointer attribute
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_Value_getAttribute(thread, cfiValue, attribute);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_Value_getCode",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_Value_getCode(
            final IsolateThread thread,
            final DxfgCFIValueHandle cfiValue,
            @DxfgOut final CInt16Pointer code
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_Value_getCode(thread, cfiValue, code);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_Value_getName",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_Value_getName(
            final IsolateThread thread,
            final DxfgCFIValueHandle cfiValue,
            @DxfgOut final CCharPointerPointer name
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_Value_getName(thread, cfiValue, name);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_CFI_Value_getDescription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_CFI_Value_getDescription(
            final IsolateThread thread,
            final DxfgCFIValueHandle cfiValue,
            @DxfgOut final CCharPointerPointer description
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_CFI_Value_getDescription(thread, cfiValue, description);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_EMPTY",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_EMPTY(
            final IsolateThread thread,
            @DxfgOut final DxfgPriceIncrementsHandlePointer emptyPriceIncrements
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_EMPTY(thread, emptyPriceIncrements);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_valueOf",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_valueOf(
            final IsolateThread thread,
            @CConst final CCharPointer text,
            @DxfgOut final DxfgPriceIncrementsHandlePointer priceIncrements
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_valueOf(thread, text, priceIncrements);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_valueOfIncrement",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_valueOfIncrement(
            final IsolateThread thread,
            final double increment,
            @DxfgOut final DxfgPriceIncrementsHandlePointer priceIncrements
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_valueOf(thread, increment, priceIncrements);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_valueOfIncrements",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_valueOfIncrements(
            final IsolateThread thread,
            @CConst final DxfgDoubleList increments,
            @DxfgOut final DxfgPriceIncrementsHandlePointer priceIncrements
    ) {
        final boolean empty = increments.isNull() || increments.getSize() <= 0;

        // The older function takes such an array as EMPTY.
        if (!empty && increments.getElements().isNull()) {
            throw new IllegalArgumentException(
                    "The `increments` list of size " + increments.getSize() + " has a null elements pointer");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_valueOf(
                        thread,
                        empty ? WordFactory.nullPointer() : increments.getElements(),
                        empty ? 0 : increments.getSize(),
                        priceIncrements);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_getText",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_getText(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            @DxfgOut final CCharPointerPointer text
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_getText(thread, priceIncrements, text);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_getPriceIncrements",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_getPriceIncrements(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            @DxfgOut final WordPointer increments
    ) {
        if (increments.isNull()) {
            throw new IllegalArgumentException("The `increments` pointer is null");
        }

        // The older function writes the array and its size to the fields of the list.
        final DxfgDoubleList list = UnmanagedMemory.calloc(SizeOf.get(DxfgDoubleList.class));
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_getPriceIncrements(
                        thread,
                        priceIncrements,
                        list.addressOfElements(),
                        list.addressOfSize());

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            UnmanagedMemory.free(list);

            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        increments.write(list);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_getPriceIncrement",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_getPriceIncrement(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            @DxfgOut final CDoublePointer priceIncrement
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_getPriceIncrement(
                        thread,
                        priceIncrements,
                        priceIncrement);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_getPriceIncrementForPrice",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_getPriceIncrementForPrice(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            final double price,
            @DxfgOut final CDoublePointer priceIncrement
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_getPriceIncrement(
                        thread,
                        priceIncrements,
                        price,
                        priceIncrement);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_getPriceIncrementForPriceAndDirection",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_getPriceIncrementForPriceAndDirection(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            final double price,
            final int direction,
            @DxfgOut final CDoublePointer priceIncrement
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_getPriceIncrement(
                        thread,
                        priceIncrements,
                        price,
                        direction,
                        priceIncrement);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_getPricePrecision",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_getPricePrecision(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            @DxfgOut final CInt32Pointer pricePrecision
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_getPricePrecision(
                        thread,
                        priceIncrements,
                        pricePrecision);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_getPricePrecisionForPrice",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_getPricePrecisionForPrice(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            final double price,
            @DxfgOut final CInt32Pointer pricePrecision
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_getPricePrecision(
                        thread,
                        priceIncrements,
                        price,
                        pricePrecision);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_roundPrice",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_roundPrice(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            final double price,
            @DxfgOut final CDoublePointer roundedPrice
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_roundPrice(
                        thread,
                        priceIncrements,
                        price,
                        roundedPrice);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_roundPriceWithDirection",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_roundPriceWithDirection(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            final double price,
            final int direction,
            @DxfgOut final CDoublePointer roundedPrice
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_roundPrice(
                        thread,
                        priceIncrements,
                        price,
                        direction,
                        roundedPrice);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_roundPriceWithRoundingMode",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_roundPriceWithRoundingMode(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            final double price,
            final DxfgRoundingMode roundingMode,
            @DxfgOut final CDoublePointer roundedPrice
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_roundPrice(
                        thread,
                        priceIncrements,
                        price,
                        roundingMode,
                        roundedPrice);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_incrementPrice",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_incrementPrice(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            final double price,
            final int direction,
            @DxfgOut final CDoublePointer incrementedPrice
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_incrementPrice(
                        thread,
                        priceIncrements,
                        price,
                        direction,
                        incrementedPrice);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceIncrements_incrementPriceWithStep",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceIncrements_incrementPriceWithStep(
            final IsolateThread thread,
            final DxfgPriceIncrementsHandle priceIncrements,
            final double price,
            final int direction,
            final double step,
            @DxfgOut final CDoublePointer incrementedPrice
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = GlossaryNative.dxfg_PriceIncrements_incrementPrice(
                        thread,
                        priceIncrements,
                        price,
                        direction,
                        step,
                        incrementedPrice);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_double_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_double_list_release(
            final IsolateThread thread,
            final DxfgDoubleList list
    ) {
        if (list.isNull()) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
        }

        if (list.getElements().isNonNull()) {
            UnmanagedMemory.free(list.getElements());
        }

        UnmanagedMemory.free(list);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_string_to_double_map_entry_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_string_to_double_map_entry_list_release(
            final IsolateThread thread,
            final DxfgStringToDoubleMapEntryList list
    ) {
        if (list.isNull()) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
        }

        final DxfgStringToDoubleMapEntryPointer elements = list.getElements();

        // An empty map may come with a non-NULL array of size 0: it is freed too.
        if (elements.isNonNull()) {
            for (int i = 0; i < list.getSize(); i++) {
                NativeUtils.MAPPER_STRING.release(elements.addressOf(i).getKey());
            }

            UnmanagedMemory.free(elements);
        }

        UnmanagedMemory.free(list);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_cfi_value_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_cfi_value_list_release(
            final IsolateThread thread,
            final DxfgCFIValueList values
    ) {
        NativeUtils.MAPPER_CFI_VALUES.release(values);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
