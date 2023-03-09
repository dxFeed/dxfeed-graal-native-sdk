package com.dxfeed.sdk.maper;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class InstrumentProfileMapper extends Mapper<InstrumentProfile, DxfgInstrumentProfile> {

  private static final Field customFields;

  static {
    try {
      customFields = InstrumentProfile.class.getDeclaredField("customFields");
      customFields.setAccessible(true);
    } catch (final NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static String[] getCustomFields(final InstrumentProfile jObject) {
    try {
      return (String[]) InstrumentProfileMapper.customFields.get(jObject);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static void setCustomFields(final InstrumentProfile jObject, final String[] newValue) {
    try {
      InstrumentProfileMapper.customFields.set(jObject, newValue);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DxfgInstrumentProfile toNative(final InstrumentProfile jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgInstrumentProfile nObject = UnmanagedMemory.malloc(
        SizeOf.get(DxfgInstrumentProfile.class));
    fillNative(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNative(final InstrumentProfile jObject, final DxfgInstrumentProfile nObject) {
    nObject.setType(NativeUtils.MAPPER_STRING.toNative(jObject.getType()));
    nObject.setSymbol(NativeUtils.MAPPER_STRING.toNative(jObject.getSymbol()));
    nObject.setDescription(NativeUtils.MAPPER_STRING.toNative(jObject.getDescription()));
    nObject.setLocalSymbol(NativeUtils.MAPPER_STRING.toNative(jObject.getLocalSymbol()));
    nObject.setLocalDescription(NativeUtils.MAPPER_STRING.toNative(jObject.getLocalDescription()));
    nObject.setCountry(NativeUtils.MAPPER_STRING.toNative(jObject.getCountry()));
    nObject.setOpol(NativeUtils.MAPPER_STRING.toNative(jObject.getOPOL()));
    nObject.setExchangeData(NativeUtils.MAPPER_STRING.toNative(jObject.getExchangeData()));
    nObject.setExchanges(NativeUtils.MAPPER_STRING.toNative(jObject.getExchanges()));
    nObject.setCurrency(NativeUtils.MAPPER_STRING.toNative(jObject.getCurrency()));
    nObject.setBaseCurrency(NativeUtils.MAPPER_STRING.toNative(jObject.getBaseCurrency()));
    nObject.setCfi(NativeUtils.MAPPER_STRING.toNative(jObject.getCFI()));
    nObject.setIsin(NativeUtils.MAPPER_STRING.toNative(jObject.getISIN()));
    nObject.setSedol(NativeUtils.MAPPER_STRING.toNative(jObject.getSEDOL()));
    nObject.setCusip(NativeUtils.MAPPER_STRING.toNative(jObject.getCUSIP()));
    nObject.setIcb(jObject.getICB());
    nObject.setSic(jObject.getSIC());
    nObject.setMultiplier(jObject.getMultiplier());
    nObject.setProduct(NativeUtils.MAPPER_STRING.toNative(jObject.getProduct()));
    nObject.setUnderlying(NativeUtils.MAPPER_STRING.toNative(jObject.getUnderlying()));
    nObject.setSpc(jObject.getSPC());
    nObject.setAdditionalUnderlyings(NativeUtils.MAPPER_STRING.toNative(jObject.getAdditionalUnderlyings()));
    nObject.setMmy(NativeUtils.MAPPER_STRING.toNative(jObject.getMMY()));
    nObject.setExpiration(jObject.getExpiration());
    nObject.setLastTrade(jObject.getLastTrade());
    nObject.setStrike(jObject.getStrike());
    nObject.setOptionType(NativeUtils.MAPPER_STRING.toNative(jObject.getOptionType()));
    nObject.setExpirationStyle(NativeUtils.MAPPER_STRING.toNative(jObject.getExpirationStyle()));
    nObject.setSettlementStyle(NativeUtils.MAPPER_STRING.toNative(jObject.getSettlementStyle()));
    nObject.setPriceIncrements(NativeUtils.MAPPER_STRING.toNative(jObject.getPriceIncrements()));
    nObject.setTradingHours(NativeUtils.MAPPER_STRING.toNative(jObject.getTradingHours()));
    final String[] customFields = getCustomFields(jObject);
    nObject.setCustomFields(NativeUtils.MAPPER_STRINGS.toNativeList(
        customFields == null
            ? null
            : Arrays.asList(customFields)
    ));
  }

  @Override
  public void cleanNative(final DxfgInstrumentProfile nObject) {
    NativeUtils.MAPPER_STRING.release(nObject.getType());
    NativeUtils.MAPPER_STRING.release(nObject.getSymbol());
    NativeUtils.MAPPER_STRING.release(nObject.getDescription());
    NativeUtils.MAPPER_STRING.release(nObject.getLocalSymbol());
    NativeUtils.MAPPER_STRING.release(nObject.getLocalDescription());
    NativeUtils.MAPPER_STRING.release(nObject.getCountry());
    NativeUtils.MAPPER_STRING.release(nObject.getOpol());
    NativeUtils.MAPPER_STRING.release(nObject.getExchangeData());
    NativeUtils.MAPPER_STRING.release(nObject.getExchanges());
    NativeUtils.MAPPER_STRING.release(nObject.getCurrency());
    NativeUtils.MAPPER_STRING.release(nObject.getBaseCurrency());
    NativeUtils.MAPPER_STRING.release(nObject.getCfi());
    NativeUtils.MAPPER_STRING.release(nObject.getIsin());
    NativeUtils.MAPPER_STRING.release(nObject.getSedol());
    NativeUtils.MAPPER_STRING.release(nObject.getCusip());
    NativeUtils.MAPPER_STRING.release(nObject.getProduct());
    NativeUtils.MAPPER_STRING.release(nObject.getUnderlying());
    NativeUtils.MAPPER_STRING.release(nObject.getAdditionalUnderlyings());
    NativeUtils.MAPPER_STRING.release(nObject.getMmy());
    NativeUtils.MAPPER_STRING.release(nObject.getOptionType());
    NativeUtils.MAPPER_STRING.release(nObject.getExpirationStyle());
    NativeUtils.MAPPER_STRING.release(nObject.getSettlementStyle());
    NativeUtils.MAPPER_STRING.release(nObject.getPriceIncrements());
    NativeUtils.MAPPER_STRING.release(nObject.getTradingHours());
    NativeUtils.MAPPER_STRINGS.release(nObject.getCustomFields());
  }

  @Override
  protected InstrumentProfile doToJava(final DxfgInstrumentProfile nObject) {
    final InstrumentProfile jObject = new InstrumentProfile();
    fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgInstrumentProfile nObject, final InstrumentProfile jObject) {
    jObject.setType(NativeUtils.MAPPER_STRING.toJava(nObject.getType()));
    jObject.setType(NativeUtils.MAPPER_STRING.toJava(nObject.getType()));
    jObject.setSymbol(NativeUtils.MAPPER_STRING.toJava(nObject.getSymbol()));
    jObject.setDescription(NativeUtils.MAPPER_STRING.toJava(nObject.getDescription()));
    jObject.setLocalSymbol(NativeUtils.MAPPER_STRING.toJava(nObject.getLocalSymbol()));
    jObject.setLocalDescription(NativeUtils.MAPPER_STRING.toJava(nObject.getLocalDescription()));
    jObject.setCountry(NativeUtils.MAPPER_STRING.toJava(nObject.getCountry()));
    jObject.setOPOL(NativeUtils.MAPPER_STRING.toJava(nObject.getOpol()));
    jObject.setExchangeData(NativeUtils.MAPPER_STRING.toJava(nObject.getExchangeData()));
    jObject.setExchanges(NativeUtils.MAPPER_STRING.toJava(nObject.getExchanges()));
    jObject.setCurrency(NativeUtils.MAPPER_STRING.toJava(nObject.getCurrency()));
    jObject.setBaseCurrency(NativeUtils.MAPPER_STRING.toJava(nObject.getBaseCurrency()));
    jObject.setCFI(NativeUtils.MAPPER_STRING.toJava(nObject.getCfi()));
    jObject.setISIN(NativeUtils.MAPPER_STRING.toJava(nObject.getIsin()));
    jObject.setSEDOL(NativeUtils.MAPPER_STRING.toJava(nObject.getSedol()));
    jObject.setCUSIP(NativeUtils.MAPPER_STRING.toJava(nObject.getCusip()));
    jObject.setICB(nObject.getIcb());
    jObject.setSIC(nObject.getSic());
    jObject.setMultiplier(nObject.getMultiplier());
    jObject.setProduct(NativeUtils.MAPPER_STRING.toJava(nObject.getProduct()));
    jObject.setUnderlying(NativeUtils.MAPPER_STRING.toJava(nObject.getUnderlying()));
    jObject.setSPC(nObject.getSpc());
    jObject.setAdditionalUnderlyings(NativeUtils.MAPPER_STRING.toJava(nObject.getAdditionalUnderlyings()));
    jObject.setMMY(NativeUtils.MAPPER_STRING.toJava(nObject.getMmy()));
    jObject.setExpiration(nObject.getExpiration());
    jObject.setLastTrade(nObject.getLastTrade());
    jObject.setStrike(nObject.getStrike());
    jObject.setOptionType(NativeUtils.MAPPER_STRING.toJava(nObject.getOptionType()));
    jObject.setExpirationStyle(NativeUtils.MAPPER_STRING.toJava(nObject.getExpirationStyle()));
    jObject.setSettlementStyle(NativeUtils.MAPPER_STRING.toJava(nObject.getSettlementStyle()));
    jObject.setPriceIncrements(NativeUtils.MAPPER_STRING.toJava(nObject.getPriceIncrements()));
    jObject.setTradingHours(NativeUtils.MAPPER_STRING.toJava(nObject.getTradingHours()));
    setCustomFields(jObject, NativeUtils.MAPPER_STRINGS.toJavaList(nObject.getCustomFields()).toArray(new String[0]));
  }
}
