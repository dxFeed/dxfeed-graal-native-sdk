// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary;

import com.dxfeed.glossary.AdditionalUnderlyings;
import com.dxfeed.glossary.CFI;
import com.dxfeed.glossary.CFI.Value;
import com.dxfeed.glossary.PriceIncrements;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CDoublePointerPointer;
import com.dxfeed.sdk.common.CInt16Pointer;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.common.DxfgStringToDoubleMapEntryPointer;
import com.dxfeed.sdk.common.DxfgStringToDoubleMapEntryPointerPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.javac.DxfgRoundingMode;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CDoublePointer;

@CContext(Directives.class)
public class GlossaryNative {

  @CEntryPoint(name = "dxfg_AdditionalUnderlyings_EMPTY", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_AdditionalUnderlyings_EMPTY(final IsolateThread ignoredThread,
      @DxfgOut final DxfgAdditionalUnderlyingsHandlePointer emptyAdditionalUnderlyings) {
    if (emptyAdditionalUnderlyings.isNull()) {
      throw new IllegalArgumentException("The `emptyAdditionalUnderlyings` pointer is null");
    }

    emptyAdditionalUnderlyings.write(
        NativeUtils.MAPPER_ADDITIONAL_UNDERLYINGS.toNative(AdditionalUnderlyings.EMPTY));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_AdditionalUnderlyings_valueOf", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_AdditionalUnderlyings_valueOf(final IsolateThread ignoredThread,
      @CConst final CCharPointer text,
      @DxfgOut final DxfgAdditionalUnderlyingsHandlePointer additionalUnderlyings) {
    if (additionalUnderlyings.isNull()) {
      throw new IllegalArgumentException("The `additionalUnderlyings` pointer is null");
    }

    additionalUnderlyings.write(NativeUtils.MAPPER_ADDITIONAL_UNDERLYINGS.toNative(
        AdditionalUnderlyings.valueOf(NativeUtils.MAPPER_STRING.toJava(text))));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_AdditionalUnderlyings_valueOf2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_AdditionalUnderlyings_valueOf(final IsolateThread ignoredThread,
      @CConst final DxfgStringToDoubleMapEntryPointer mapEntries, int size,
      @DxfgOut final DxfgAdditionalUnderlyingsHandlePointer additionalUnderlyings) {

    if (additionalUnderlyings.isNull()) {
      throw new IllegalArgumentException("The `additionalUnderlyings` pointer is null");
    }

    Map<String, Double> map = Map.of();

    if (mapEntries.isNonNull() && size > 0) {
      map = new HashMap<>();

      for (int i = 0; i < size; i++) {
        var mapEntry = mapEntries.addressOf(i);

        map.put(NativeUtils.MAPPER_STRING.toJava(mapEntry.getKey()), mapEntry.getValue());
      }
    }

    additionalUnderlyings.write(
        NativeUtils.MAPPER_ADDITIONAL_UNDERLYINGS.toNative(AdditionalUnderlyings.valueOf(map)));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_AdditionalUnderlyings_getSPC", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_AdditionalUnderlyings_getSPC(final IsolateThread ignoredThread,
      @CConst final CCharPointer text, @CConst final CCharPointer symbol,
      @DxfgOut final CDoublePointer spc) {
    if (spc.isNull()) {
      throw new IllegalArgumentException("The `spc` pointer is null");
    }

    double result = 0;

    if (text.isNonNull() && symbol.isNonNull()) {
      result = AdditionalUnderlyings.getSPC(NativeUtils.MAPPER_STRING.toJava(text),
          NativeUtils.MAPPER_STRING.toJava(symbol));
    }

    spc.write(result);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_AdditionalUnderlyings_getText", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_AdditionalUnderlyings_getText(final IsolateThread ignoredThread,
      final DxfgAdditionalUnderlyingsHandle additionalUnderlyings,
      @DxfgOut final CCharPointerPointer text) {
    if (text.isNull()) {
      throw new IllegalArgumentException("The `text` pointer is null");
    }

    //noinspection DataFlowIssue
    text.write(NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_ADDITIONAL_UNDERLYINGS.toJava(additionalUnderlyings).getText()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_AdditionalUnderlyings_getMap", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_AdditionalUnderlyings_getMap(final IsolateThread ignoredThread,
      final DxfgAdditionalUnderlyingsHandle additionalUnderlyings,
      @DxfgOut final DxfgStringToDoubleMapEntryPointerPointer mapEntries,
      @DxfgOut final CInt32Pointer size) {
    if (mapEntries.isNull()) {
      throw new IllegalArgumentException("The `mapEntries` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var javaAdditionalUnderlyings = NativeUtils.MAPPER_ADDITIONAL_UNDERLYINGS.toJava(
        additionalUnderlyings);
    //noinspection DataFlowIssue
    var map = javaAdditionalUnderlyings.getMap();

    DxfgStringToDoubleMapEntryPointer resultMapEntries = UnmanagedMemory.calloc(
        map.size() * SizeOf.get(DxfgStringToDoubleMapEntryPointer.class));

    int counter = 0;
    for (var mapEntry : map.entrySet()) {
      DxfgStringToDoubleMapEntryPointer resultEntry = resultMapEntries.addressOf(counter++);

      resultEntry.setKey(NativeUtils.MAPPER_STRING.toNative(mapEntry.getKey()));
      resultEntry.setValue(mapEntry.getValue());
    }

    mapEntries.write(resultMapEntries);
    size.write(counter);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_AdditionalUnderlyings_getSPC2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_AdditionalUnderlyings_getSPC(final IsolateThread ignoredThread,
      final DxfgAdditionalUnderlyingsHandle additionalUnderlyings,
      @CConst final CCharPointer symbol, @DxfgOut final CDoublePointer spc) {
    if (spc.isNull()) {
      throw new IllegalArgumentException("The `spc` pointer is null");
    }

    //noinspection DataFlowIssue
    spc.write(NativeUtils.MAPPER_ADDITIONAL_UNDERLYINGS.toJava(additionalUnderlyings)
        .getSPC(NativeUtils.MAPPER_STRING.toJava(symbol)));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_EMPTY", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_EMPTY(final IsolateThread ignoredThread,
      @DxfgOut final DxfgCFIHandlePointer emptyCfi) {
    if (emptyCfi.isNull()) {
      throw new IllegalArgumentException("The `emptyCfi` pointer is null");
    }

    emptyCfi.write(NativeUtils.MAPPER_CFI.toNative(CFI.EMPTY));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_valueOf", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_valueOf(final IsolateThread ignoredThread,
      @CConst final CCharPointer code, @DxfgOut final DxfgCFIHandlePointer cfi) {
    if (cfi.isNull()) {
      throw new IllegalArgumentException("The `cfi` pointer is null");
    }

    cfi.write(NativeUtils.MAPPER_CFI.toNative(CFI.valueOf(NativeUtils.MAPPER_STRING.toJava(code))));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_valueOf2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_valueOf(final IsolateThread ignoredThread, final int intCode,
      @DxfgOut final DxfgCFIHandlePointer cfi) {
    if (cfi.isNull()) {
      throw new IllegalArgumentException("The `cfi` pointer is null");
    }

    cfi.write(NativeUtils.MAPPER_CFI.toNative(CFI.valueOf(intCode)));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_getCode", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_getCode(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final CCharPointerPointer code) {
    if (code.isNull()) {
      throw new IllegalArgumentException("The `code` pointer is null");
    }

    //noinspection DataFlowIssue
    code.write(NativeUtils.MAPPER_STRING.toNative(NativeUtils.MAPPER_CFI.toJava(cfi).getCode()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_getIntCode", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_getIntCode(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final CInt32Pointer intCode) {
    if (intCode.isNull()) {
      throw new IllegalArgumentException("The `intCode` pointer is null");
    }

    //noinspection DataFlowIssue
    intCode.write(NativeUtils.MAPPER_CFI.toJava(cfi).getIntCode());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_getCategory", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_getCategory(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final CInt16Pointer category) {
    if (category.isNull()) {
      throw new IllegalArgumentException("The `category` pointer is null");
    }

    //noinspection DataFlowIssue
    category.write(NativeUtils.MAPPER_CFI.toJava(cfi).getCategory());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_getGroup", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_getGroup(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final CInt16Pointer group) {
    if (group.isNull()) {
      throw new IllegalArgumentException("The `group` pointer is null");
    }

    //noinspection DataFlowIssue
    group.write(NativeUtils.MAPPER_CFI.toJava(cfi).getGroup());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_isEquity", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_isEquity(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final CInt32Pointer isEquity) {
    if (isEquity.isNull()) {
      throw new IllegalArgumentException("The `isEquity` pointer is null");
    }

    //noinspection DataFlowIssue
    isEquity.write(NativeUtils.MAPPER_CFI.toJava(cfi).isEquity() ? 1 : 0);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_isDebtInstrument", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_isDebtInstrument(final IsolateThread ignoredThread,
      final DxfgCFIHandle cfi, @DxfgOut final CInt32Pointer isDebtInstrument) {
    if (isDebtInstrument.isNull()) {
      throw new IllegalArgumentException("The `isDebtInstrument` pointer is null");
    }

    //noinspection DataFlowIssue
    isDebtInstrument.write(NativeUtils.MAPPER_CFI.toJava(cfi).isDebtInstrument() ? 1 : 0);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_isEntitlement", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_isEntitlement(final IsolateThread ignoredThread,
      final DxfgCFIHandle cfi, @DxfgOut final CInt32Pointer isEntitlement) {
    if (isEntitlement.isNull()) {
      throw new IllegalArgumentException("The `isEntitlement` pointer is null");
    }

    //noinspection DataFlowIssue
    isEntitlement.write(NativeUtils.MAPPER_CFI.toJava(cfi).isEntitlement() ? 1 : 0);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_isOption", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_isOption(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final CInt32Pointer isOption) {
    if (isOption.isNull()) {
      throw new IllegalArgumentException("The `isOption` pointer is null");
    }

    //noinspection DataFlowIssue
    isOption.write(NativeUtils.MAPPER_CFI.toJava(cfi).isOption() ? 1 : 0);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_isFuture", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_isFuture(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final CInt32Pointer isFuture) {
    if (isFuture.isNull()) {
      throw new IllegalArgumentException("The `isFuture` pointer is null");
    }

    //noinspection DataFlowIssue
    isFuture.write(NativeUtils.MAPPER_CFI.toJava(cfi).isFuture() ? 1 : 0);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_isOther", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_isOther(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final CInt32Pointer isOther) {
    if (isOther.isNull()) {
      throw new IllegalArgumentException("The `isOther` pointer is null");
    }

    //noinspection DataFlowIssue
    isOther.write(NativeUtils.MAPPER_CFI.toJava(cfi).isOther() ? 1 : 0);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_decipher", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_decipher(final IsolateThread ignoredThread, final DxfgCFIHandle cfi,
      @DxfgOut final DxfgCFIValueHandlePointer values, @DxfgOut final CInt32Pointer size) {
    if (values.isNull()) {
      throw new IllegalArgumentException("The `values` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var cfiObject = NativeUtils.MAPPER_CFI.toJava(cfi);
    //noinspection DataFlowIssue
    var decipherResult = cfiObject.decipher();
    var mapper = (JavaObjectHandlerMapper<Value, DxfgCFIValueHandle>) NativeUtils.MAPPER_CFI_VALUE;

    values.write(mapper.toNativeArray(decipherResult));
    size.write(decipherResult.length);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_describe", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_describe(final IsolateThread ignoredThread,
      final DxfgCFIHandle cfi,
      @DxfgOut final CCharPointerPointer description) {
    if (description.isNull()) {
      throw new IllegalArgumentException("The `description` pointer is null");
    }

    //noinspection DataFlowIssue
    description.write(NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_CFI.toJava(cfi).describe()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_Attribute_getName", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_Attribute_getName(final IsolateThread ignoredThread,
      final DxfgCFIAttributeHandle cfiAttribute,
      @DxfgOut final CCharPointerPointer name) {
    if (name.isNull()) {
      throw new IllegalArgumentException("The `name` pointer is null");
    }

    //noinspection DataFlowIssue
    name.write(NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_CFI_ATTRIBUTE.toJava(cfiAttribute).getName()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_Attribute_getDescription", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_Attribute_getDescription(final IsolateThread ignoredThread,
      final DxfgCFIAttributeHandle cfiAttribute,
      @DxfgOut final CCharPointerPointer description) {
    if (description.isNull()) {
      throw new IllegalArgumentException("The `description` pointer is null");
    }

    //noinspection DataFlowIssue
    description.write(NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_CFI_ATTRIBUTE.toJava(cfiAttribute).getDescription()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_Attribute_getValues", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_Attribute_getValues(final IsolateThread ignoredThread,
      final DxfgCFIAttributeHandle cfiAttribute,
      @DxfgOut final DxfgCFIValueHandlePointer values, @DxfgOut final CInt32Pointer size) {
    if (values.isNull()) {
      throw new IllegalArgumentException("The `values` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var cfiAttributeObject = NativeUtils.MAPPER_CFI_ATTRIBUTE.toJava(cfiAttribute);
    //noinspection DataFlowIssue
    var getValuesResult = cfiAttributeObject.getValues();
    var mapper = (JavaObjectHandlerMapper<Value, DxfgCFIValueHandle>) NativeUtils.MAPPER_CFI_VALUE;

    values.write(mapper.toNativeArray(getValuesResult));
    size.write(getValuesResult.length);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_Value_getAttribute", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_Value_getAttribute(final IsolateThread ignoredThread,
      final DxfgCFIValueHandle cfiValue,
      @DxfgOut final DxfgCFIAttributeHandlePointer attribute) {
    if (attribute.isNull()) {
      throw new IllegalArgumentException("The `attribute` pointer is null");
    }

    //noinspection DataFlowIssue
    attribute.write(NativeUtils.MAPPER_CFI_ATTRIBUTE.toNative(
        NativeUtils.MAPPER_CFI_VALUE.toJava(cfiValue).getAttribute()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_Value_getCode", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_Value_getCode(final IsolateThread ignoredThread,
      final DxfgCFIValueHandle cfiValue,
      @DxfgOut final CInt16Pointer code) {
    if (code.isNull()) {
      throw new IllegalArgumentException("The `code` pointer is null");
    }

    //noinspection DataFlowIssue
    code.write(NativeUtils.MAPPER_CFI_VALUE.toJava(cfiValue).getCode());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_Value_getName", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_Value_getName(final IsolateThread ignoredThread,
      final DxfgCFIValueHandle cfiValue,
      @DxfgOut final CCharPointerPointer name) {
    if (name.isNull()) {
      throw new IllegalArgumentException("The `name` pointer is null");
    }

    //noinspection DataFlowIssue
    name.write(NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_CFI_VALUE.toJava(cfiValue).getName()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_CFI_Value_getDescription", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_CFI_Value_getDescription(final IsolateThread ignoredThread,
      final DxfgCFIValueHandle cfiValue,
      @DxfgOut final CCharPointerPointer description) {
    if (description.isNull()) {
      throw new IllegalArgumentException("The `description` pointer is null");
    }

    //noinspection DataFlowIssue
    description.write(NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_CFI_VALUE.toJava(cfiValue).getDescription()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_EMPTY", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_EMPTY(final IsolateThread ignoredThread,
      @DxfgOut final DxfgPriceIncrementsHandlePointer emptyPriceIncrements) {
    if (emptyPriceIncrements.isNull()) {
      throw new IllegalArgumentException("The `emptyPriceIncrements` pointer is null");
    }

    emptyPriceIncrements.write(NativeUtils.MAPPER_PRICE_INCREMENTS.toNative(PriceIncrements.EMPTY));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_valueOf", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_valueOf(final IsolateThread ignoredThread,
      @CConst final CCharPointer text,
      @DxfgOut final DxfgPriceIncrementsHandlePointer priceIncrements) {
    if (priceIncrements.isNull()) {
      throw new IllegalArgumentException("The `priceIncrements` pointer is null");
    }

    priceIncrements.write(NativeUtils.MAPPER_PRICE_INCREMENTS.toNative(
        PriceIncrements.valueOf(NativeUtils.MAPPER_STRING.toJava(text))));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_valueOf2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_valueOf(final IsolateThread ignoredThread,
      double increment,
      @DxfgOut final DxfgPriceIncrementsHandlePointer priceIncrements) {
    if (priceIncrements.isNull()) {
      throw new IllegalArgumentException("The `priceIncrements` pointer is null");
    }

    priceIncrements.write(NativeUtils.MAPPER_PRICE_INCREMENTS.toNative(
        PriceIncrements.valueOf(increment)));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_valueOf3", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_valueOf(final IsolateThread ignoredThread,
      @CConst final CDoublePointer increments,
      int size,
      @DxfgOut final DxfgPriceIncrementsHandlePointer priceIncrements) {
    if (priceIncrements.isNull()) {
      throw new IllegalArgumentException("The `priceIncrements` pointer is null");
    }

    var priceIncrementsResult = PriceIncrements.valueOf("");

    if (increments.isNonNull() && size > 0) {
      double[] incrementsArray = new double[size];

      for (int i = 0; i < size; i++) {
        incrementsArray[i] = increments.addressOf(i).read();
      }

      priceIncrementsResult = PriceIncrements.valueOf(incrementsArray);
    }

    priceIncrements.write(NativeUtils.MAPPER_PRICE_INCREMENTS.toNative(
        priceIncrementsResult));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_getText", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_getText(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      @DxfgOut final CCharPointerPointer text) {
    if (text.isNull()) {
      throw new IllegalArgumentException("The `text` pointer is null");
    }

    //noinspection DataFlowIssue
    text.write(NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements).getText()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_getPriceIncrements", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_getPriceIncrements(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      @DxfgOut final CDoublePointerPointer increments,
      @DxfgOut final CInt32Pointer size) {
    if (increments.isNull()) {
      throw new IllegalArgumentException("The `increments` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    //noinspection DataFlowIssue
    var result = NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements).getPriceIncrements();

    if (result.length > 0) {
      CDoublePointer resultIncrements = UnmanagedMemory.calloc(
          result.length * SizeOf.get(CDoublePointer.class));

      for (int i = 0; i < result.length; i++) {
        resultIncrements.addressOf(i).write(result[i]);
      }

      increments.write(resultIncrements);
    }

    size.write(result.length);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_getPriceIncrement", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_getPriceIncrement(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      @DxfgOut final CDoublePointer priceIncrement) {
    if (priceIncrement.isNull()) {
      throw new IllegalArgumentException("The `priceIncrement` pointer is null");
    }

    //noinspection DataFlowIssue
    priceIncrement.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements).getPriceIncrement());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_getPriceIncrement2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_getPriceIncrement(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      double price,
      @DxfgOut final CDoublePointer priceIncrement) {
    if (priceIncrement.isNull()) {
      throw new IllegalArgumentException("The `priceIncrement` pointer is null");
    }

    //noinspection DataFlowIssue
    priceIncrement.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements).getPriceIncrement(price));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_getPriceIncrement3", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_getPriceIncrement(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      double price,
      int direction,
      @DxfgOut final CDoublePointer priceIncrement) {
    if (priceIncrement.isNull()) {
      throw new IllegalArgumentException("The `priceIncrement` pointer is null");
    }

    //noinspection DataFlowIssue
    priceIncrement.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements)
            .getPriceIncrement(price, direction));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_getPricePrecision", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_getPricePrecision(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      @DxfgOut final CInt32Pointer pricePrecision) {
    if (pricePrecision.isNull()) {
      throw new IllegalArgumentException("The `pricePrecision` pointer is null");
    }

    //noinspection DataFlowIssue
    pricePrecision.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements)
            .getPricePrecision());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_getPricePrecision2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_getPricePrecision(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      double price,
      @DxfgOut final CInt32Pointer pricePrecision) {
    if (pricePrecision.isNull()) {
      throw new IllegalArgumentException("The `pricePrecision` pointer is null");
    }

    //noinspection DataFlowIssue
    pricePrecision.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements)
            .getPricePrecision(price));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_roundPrice", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_roundPrice(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      double price,
      @DxfgOut final CDoublePointer roundedPrice) {
    if (roundedPrice.isNull()) {
      throw new IllegalArgumentException("The `roundedPrice` pointer is null");
    }

    //noinspection DataFlowIssue
    roundedPrice.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements).roundPrice(price));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_roundPrice2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_roundPrice(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      double price,
      int direction,
      @DxfgOut final CDoublePointer roundedPrice) {
    if (roundedPrice.isNull()) {
      throw new IllegalArgumentException("The `roundedPrice` pointer is null");
    }

    //noinspection DataFlowIssue
    roundedPrice.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements).roundPrice(price, direction));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_roundPrice3", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_roundPrice(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      double price,
      DxfgRoundingMode roundingMode,
      @DxfgOut final CDoublePointer roundedPrice) {
    if (roundedPrice.isNull()) {
      throw new IllegalArgumentException("The `roundedPrice` pointer is null");
    }

    //noinspection DataFlowIssue
    roundedPrice.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements)
            .roundPrice(price, roundingMode.getRoundingMode()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_incrementPrice", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_incrementPrice(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      double price,
      int direction,
      @DxfgOut final CDoublePointer incrementedPrice) {
    if (incrementedPrice.isNull()) {
      throw new IllegalArgumentException("The `incrementedPrice` pointer is null");
    }

    //noinspection DataFlowIssue
    incrementedPrice.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements)
            .incrementPrice(price, direction));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(name = "dxfg_PriceIncrements_incrementPrice2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
  public static int dxfg_PriceIncrements_incrementPrice(final IsolateThread ignoredThread,
      final DxfgPriceIncrementsHandle priceIncrements,
      double price,
      int direction,
      double step,
      @DxfgOut final CDoublePointer incrementedPrice) {
    if (incrementedPrice.isNull()) {
      throw new IllegalArgumentException("The `incrementedPrice` pointer is null");
    }

    //noinspection DataFlowIssue
    incrementedPrice.write(
        NativeUtils.MAPPER_PRICE_INCREMENTS.toJava(priceIncrements)
            .incrementPrice(price, direction, step));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }
}
