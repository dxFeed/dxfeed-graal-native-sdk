package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNegativeInfinityDouble;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public class InstrumentProfileNative {

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfile dxfg_InstrumentProfileReader_new(
      final IsolateThread ignoredThread
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toNative(new InstrumentProfile());
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_new2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgInstrumentProfile dxfg_InstrumentProfileReader_new(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toNative(
        new InstrumentProfile(NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip))
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getType",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getType(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getType()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setType",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setType(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setType(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getSymbol",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getSymbol(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getSymbol()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setSymbol(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setSymbol(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getDescription",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getDescription(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getDescription()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setDescription",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setDescription(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setDescription(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getLocalSymbol",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getLocalSymbol(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getLocalSymbol()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setLocalSymbol",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setLocalSymbol(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setLocalSymbol(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getLocalDescription",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getLocalDescription(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getLocalDescription()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setLocalDescription",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setLocalDescription(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setLocalDescription(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getCountry",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getCountry(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getCountry()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setCountry",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setCountry(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setCountry(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getOPOL",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getOPOL(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getOPOL()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setOPOL",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setOPOL(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setOPOL(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getExchangeData",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getExchangeData(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getExchangeData()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setExchangeData",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setExchangeData(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setExchangeData(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getExchanges",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getExchanges(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getExchanges()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setExchanges",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setExchanges(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setExchanges(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getCurrency",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getCurrency(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getCurrency()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setCurrency",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setCurrency(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setCurrency(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getBaseCurrency",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getBaseCurrency(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getBaseCurrency()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setBaseCurrency",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setBaseCurrency(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setBaseCurrency(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getCFI",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getCFI(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getCFI()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setCFI",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setCFI(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setCFI(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getISIN",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getISIN(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getISIN()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setISIN",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setISIN(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setISIN(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getSEDOL",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getSEDOL(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getSEDOL()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setSEDOL",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setSEDOL(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setSEDOL(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getCUSIP",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getCUSIP(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getCUSIP()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setCUSIP",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setCUSIP(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setCUSIP(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getICB",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_getICB(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getICB();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setICB",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setICB(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final int value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setICB(value);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getSIC",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_getSIC(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getSIC();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setSIC",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setSIC(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final int value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setSIC(value);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getMultiplier",
      exceptionHandler = ExceptionHandlerReturnNegativeInfinityDouble.class
  )
  public static double dxfg_InstrumentProfile_getMultiplier(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getMultiplier();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setMultiplier",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setMultiplier(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final double value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setMultiplier(value);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getProduct",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getProduct(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getProduct()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setProduct",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setProduct(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setProduct(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getUnderlying",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getUnderlying(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getUnderlying()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setUnderlying",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setUnderlying(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setUnderlying(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getSPC",
      exceptionHandler = ExceptionHandlerReturnNegativeInfinityDouble.class
  )
  public static double dxfg_InstrumentProfile_getSPC(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getSPC();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setSPC",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setSPC(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final double value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setSPC(value);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getAdditionalUnderlyings",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getAdditionalUnderlyings(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getAdditionalUnderlyings()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setAdditionalUnderlyings",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setAdditionalUnderlyings(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setAdditionalUnderlyings(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getMMY",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getMMY(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getMMY()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setMMY",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setMMY(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setMMY(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getExpiration",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_getExpiration(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getExpiration();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setExpiration",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setExpiration(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final int value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setExpiration(value);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getLastTrade",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_getLastTrade(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getLastTrade();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setLastTrade",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setLastTrade(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final int value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setLastTrade(value);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getStrike",
      exceptionHandler = ExceptionHandlerReturnNegativeInfinityDouble.class
  )
  public static double dxfg_InstrumentProfile_getStrike(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getStrike();
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setStrike",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setStrike(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final double value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setStrike(value);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getOptionType",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getOptionType(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getOptionType()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setOptionType",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setOptionType(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setOptionType(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getExpirationStyle",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getExpirationStyle(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getExpirationStyle()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setExpirationStyle",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setExpirationStyle(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setExpirationStyle(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getSettlementStyle",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getSettlementStyle(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getSettlementStyle()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setSettlementStyle",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setSettlementStyle(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setSettlementStyle(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getPriceIncrements",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getPriceIncrements(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getPriceIncrements()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setPriceIncrements",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setPriceIncrements(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setPriceIncrements(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getTradingHours",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getTradingHours(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getTradingHours()
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setTradingHours",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setTradingHours(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setTradingHours(NativeUtils.MAPPER_STRING.toJava(value));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getField",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer dxfg_InstrumentProfile_getField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer name
  ) {
    return NativeUtils.MAPPER_STRING.toNative(
        NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getField(NativeUtils.MAPPER_STRING.toJava(name))
    );
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer name,
      final CCharPointer value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setField(
        NativeUtils.MAPPER_STRING.toJava(name),
        NativeUtils.MAPPER_STRING.toJava(value)
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getNumericField",
      exceptionHandler = ExceptionHandlerReturnNegativeInfinityDouble.class
  )
  public static double dxfg_InstrumentProfile_getNumericField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer name
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getNumericField(NativeUtils.MAPPER_STRING.toJava(name));
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setNumericField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setNumericField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer name,
      final double value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setNumericField(
        NativeUtils.MAPPER_STRING.toJava(name),
        value
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getDateField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_getDateField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer name
  ) {
    return NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).getDateField(NativeUtils.MAPPER_STRING.toJava(name));
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_setDateField",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_setDateField(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip,
      final CCharPointer name,
      final int value
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).setDateField(
        NativeUtils.MAPPER_STRING.toJava(name),
        value
    );
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_getNonEmptyCustomFieldNames",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgCStringListPointer dxfg_InstrumentProfile_getNonEmptyCustomFieldNames(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile ip
  ) {
    List<String> fieldNames = new ArrayList<>();
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.toJava(ip).addNonEmptyCustomFieldNames(fieldNames);
    return NativeUtils.MAPPER_STRINGS.toNativeList(fieldNames);
  }

  @CEntryPoint(
      name = "dxfg_InstrumentProfile_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_InstrumentProfile_release(
      final IsolateThread ignoredThread,
      final DxfgInstrumentProfile dxfgInstrumentProfile
  ) {
    NativeUtils.MAPPER_INSTRUMENT_PROFILE.release(dxfgInstrumentProfile);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }
}
