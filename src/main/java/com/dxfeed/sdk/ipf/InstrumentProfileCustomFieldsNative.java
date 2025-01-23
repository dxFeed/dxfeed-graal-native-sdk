package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.InstrumentProfileCustomFields;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CCharPointerPointerPointer;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import java.util.ArrayList;
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
public class InstrumentProfileCustomFieldsNative {

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_new",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_new(
      final IsolateThread ignoredThread,
      @DxfgOut final DxfgInstrumentProfileCustomFieldsHandlePointer customFields
  ) {
    if (customFields.isNull()) {
      throw new IllegalArgumentException("The `customFields` pointer is null");
    }

    customFields.write(NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toNative(
        new InstrumentProfileCustomFields()));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_new2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_new2(
      final IsolateThread ignoredThread,
      @CConst final CCharPointerPointer customFieldsArray,
      int size,
      @DxfgOut final DxfgInstrumentProfileCustomFieldsHandlePointer customFields
  ) {
    if (customFields.isNull()) {
      throw new IllegalArgumentException("The `customFields` pointer is null");
    }

    if (customFieldsArray.isNull() || size <= 0) {
      customFields.write(NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toNative(
          new InstrumentProfileCustomFields()));
    } else {
      String[] array = new String[size];

      for (int i = 0; i < size; i++) {
        array[i] = NativeUtils.MAPPER_STRING.toJava(customFieldsArray.addressOf(i).read());
      }

      customFields.write(NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toNative(
          new InstrumentProfileCustomFields(array)));
    }

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_new3",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_new3(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCustomFieldsHandle source,
      @DxfgOut final DxfgInstrumentProfileCustomFieldsHandlePointer customFields
  ) {
    if (customFields.isNull()) {
      throw new IllegalArgumentException("The `customFields` pointer is null");
    }

    //noinspection DataFlowIssue
    customFields.write(NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toNative(
        new InstrumentProfileCustomFields(
            NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(source))));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_getField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_getField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCustomFieldsHandle customFields,
      @CConst final CCharPointer name,
      @DxfgOut final CCharPointerPointer result
  ) {
    if (result.isNull()) {
      throw new IllegalArgumentException("The `result` pointer is null");
    }

    //noinspection DataFlowIssue
    result.write(NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(customFields)
            .getField(NativeUtils.MAPPER_STRING.toJava(name))));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_setField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_setField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCustomFieldsHandle customFields,
      @CConst final CCharPointer name,
      @CConst final CCharPointer value
  ) {
    //noinspection DataFlowIssue
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(customFields)
        .setField(NativeUtils.MAPPER_STRING.toJava(name), NativeUtils.MAPPER_STRING.toJava(value));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }


  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_getNumericField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_getNumericField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCustomFieldsHandle customFields,
      @CConst final CCharPointer name,
      @DxfgOut final CDoublePointer result
  ) {
    if (result.isNull()) {
      throw new IllegalArgumentException("The `result` pointer is null");
    }

    //noinspection DataFlowIssue
    result.write(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(customFields)
            .getNumericField(NativeUtils.MAPPER_STRING.toJava(name)));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_setNumericField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_setNumericField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCustomFieldsHandle customFields,
      @CConst final CCharPointer name,
      double value
  ) {
    //noinspection DataFlowIssue
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(customFields)
        .setNumericField(NativeUtils.MAPPER_STRING.toJava(name), value);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_getDateField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_getDateField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCustomFieldsHandle customFields,
      @CConst final CCharPointer name,
      @DxfgOut final CInt32Pointer result
  ) {
    if (result.isNull()) {
      throw new IllegalArgumentException("The `result` pointer is null");
    }

    //noinspection DataFlowIssue
    result.write(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(customFields)
            .getDateField(NativeUtils.MAPPER_STRING.toJava(name)));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_setDateField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_setDateField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCustomFieldsHandle customFields,
      @CConst final CCharPointer name,
      int value
  ) {
    //noinspection DataFlowIssue
    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(customFields)
        .setDateField(NativeUtils.MAPPER_STRING.toJava(name), value);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfileCustomFields_addNonEmptyFieldNames",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfileCustomFields_addNonEmptyFieldNames(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfileCustomFieldsHandle customFields,
      @DxfgOut final CCharPointerPointerPointer targetFieldNames,
      @DxfgOut final CInt32Pointer size,
      @DxfgOut final CInt32Pointer updated
  ) {
    if (targetFieldNames.isNull()) {
      throw new IllegalArgumentException("The `targetFieldNames` pointer is null");
    }

    if (size.isNull()) {
      throw new IllegalArgumentException("The `size` pointer is null");
    }

    var names = new ArrayList<String>();

    //noinspection DataFlowIssue
    var result = NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(customFields)
        .addNonEmptyFieldNames(names);

    CCharPointerPointer targetFieldNamesPtr = UnmanagedMemory.calloc(
        names.size() * SizeOf.get(CCharPointer.class));

    for (int i = 0; i < names.size(); i++) {
      var namePtr = targetFieldNamesPtr.addressOf(i);

      namePtr.write(NativeUtils.MAPPER_STRING.toNative(names.get(i)));
    }

    targetFieldNames.write(targetFieldNamesPtr);
    size.write(names.size());

    if (updated.isNonNull()) {
      updated.write(result ? 1 : 0);
    }

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }
}
