package com.dxfeed.api.maper;

import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.NativeUtils.MAPPER_STRINGS;

import com.dxfeed.api.ipf.DxfgInstrumentProfile;
import com.dxfeed.ipf.InstrumentProfile;
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
    nObject.setType(MAPPER_STRING.toNative(jObject.getType()));
    nObject.setSymbol(MAPPER_STRING.toNative(jObject.getSymbol()));
    nObject.setDescription(MAPPER_STRING.toNative(jObject.getDescription()));
    nObject.setLocalSymbol(MAPPER_STRING.toNative(jObject.getLocalSymbol()));
    nObject.setLocalDescription(MAPPER_STRING.toNative(jObject.getLocalDescription()));
    nObject.setCountry(MAPPER_STRING.toNative(jObject.getCountry()));
    nObject.setOpol(MAPPER_STRING.toNative(jObject.getOPOL()));
    nObject.setExchangeData(MAPPER_STRING.toNative(jObject.getExchangeData()));
    nObject.setExchanges(MAPPER_STRING.toNative(jObject.getExchanges()));
    nObject.setCurrency(MAPPER_STRING.toNative(jObject.getCurrency()));
    nObject.setBaseCurrency(MAPPER_STRING.toNative(jObject.getBaseCurrency()));
    nObject.setCfi(MAPPER_STRING.toNative(jObject.getCFI()));
    nObject.setIsin(MAPPER_STRING.toNative(jObject.getISIN()));
    nObject.setSedol(MAPPER_STRING.toNative(jObject.getSEDOL()));
    nObject.setCusip(MAPPER_STRING.toNative(jObject.getCUSIP()));
    nObject.setIcb(jObject.getICB());
    nObject.setSic(jObject.getSIC());
    nObject.setMultiplier(jObject.getMultiplier());
    nObject.setProduct(MAPPER_STRING.toNative(jObject.getProduct()));
    nObject.setUnderlying(MAPPER_STRING.toNative(jObject.getUnderlying()));
    nObject.setSpc(jObject.getSPC());
    nObject.setAdditionalUnderlyings(MAPPER_STRING.toNative(jObject.getAdditionalUnderlyings()));
    nObject.setMmy(MAPPER_STRING.toNative(jObject.getMMY()));
    nObject.setExpiration(jObject.getExpiration());
    nObject.setLastTrade(jObject.getLastTrade());
    nObject.setStrike(jObject.getStrike());
    nObject.setOptionType(MAPPER_STRING.toNative(jObject.getOptionType()));
    nObject.setExpirationStyle(MAPPER_STRING.toNative(jObject.getExpirationStyle()));
    nObject.setSettlementStyle(MAPPER_STRING.toNative(jObject.getSettlementStyle()));
    nObject.setPriceIncrements(MAPPER_STRING.toNative(jObject.getPriceIncrements()));
    nObject.setTradingHours(MAPPER_STRING.toNative(jObject.getTradingHours()));
    final String[] customFields = getCustomFields(jObject);
    nObject.setCustomFields(MAPPER_STRINGS.toNativeList(
        customFields == null
            ? null
            : Arrays.asList(customFields)
    ));
  }

  @Override
  public void cleanNative(final DxfgInstrumentProfile nObject) {
    MAPPER_STRING.release(nObject.getType());
    MAPPER_STRING.release(nObject.getSymbol());
    MAPPER_STRING.release(nObject.getDescription());
    MAPPER_STRING.release(nObject.getLocalSymbol());
    MAPPER_STRING.release(nObject.getLocalDescription());
    MAPPER_STRING.release(nObject.getCountry());
    MAPPER_STRING.release(nObject.getOpol());
    MAPPER_STRING.release(nObject.getExchangeData());
    MAPPER_STRING.release(nObject.getExchanges());
    MAPPER_STRING.release(nObject.getCurrency());
    MAPPER_STRING.release(nObject.getBaseCurrency());
    MAPPER_STRING.release(nObject.getCfi());
    MAPPER_STRING.release(nObject.getIsin());
    MAPPER_STRING.release(nObject.getSedol());
    MAPPER_STRING.release(nObject.getCusip());
    MAPPER_STRING.release(nObject.getProduct());
    MAPPER_STRING.release(nObject.getUnderlying());
    MAPPER_STRING.release(nObject.getAdditionalUnderlyings());
    MAPPER_STRING.release(nObject.getMmy());
    MAPPER_STRING.release(nObject.getOptionType());
    MAPPER_STRING.release(nObject.getExpirationStyle());
    MAPPER_STRING.release(nObject.getSettlementStyle());
    MAPPER_STRING.release(nObject.getPriceIncrements());
    MAPPER_STRING.release(nObject.getTradingHours());
    MAPPER_STRINGS.release(nObject.getCustomFields());
  }

  @Override
  protected InstrumentProfile doToJava(final DxfgInstrumentProfile nObject) {
    final InstrumentProfile jObject = new InstrumentProfile();
    fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgInstrumentProfile nObject, final InstrumentProfile jObject) {
    jObject.setType(MAPPER_STRING.toJava(nObject.getType()));
    jObject.setType(MAPPER_STRING.toJava(nObject.getType()));
    jObject.setSymbol(MAPPER_STRING.toJava(nObject.getSymbol()));
    jObject.setDescription(MAPPER_STRING.toJava(nObject.getDescription()));
    jObject.setLocalSymbol(MAPPER_STRING.toJava(nObject.getLocalSymbol()));
    jObject.setLocalDescription(MAPPER_STRING.toJava(nObject.getLocalDescription()));
    jObject.setCountry(MAPPER_STRING.toJava(nObject.getCountry()));
    jObject.setOPOL(MAPPER_STRING.toJava(nObject.getOpol()));
    jObject.setExchangeData(MAPPER_STRING.toJava(nObject.getExchangeData()));
    jObject.setExchanges(MAPPER_STRING.toJava(nObject.getExchanges()));
    jObject.setCurrency(MAPPER_STRING.toJava(nObject.getCurrency()));
    jObject.setBaseCurrency(MAPPER_STRING.toJava(nObject.getBaseCurrency()));
    jObject.setCFI(MAPPER_STRING.toJava(nObject.getCfi()));
    jObject.setISIN(MAPPER_STRING.toJava(nObject.getIsin()));
    jObject.setSEDOL(MAPPER_STRING.toJava(nObject.getSedol()));
    jObject.setCUSIP(MAPPER_STRING.toJava(nObject.getCusip()));
    jObject.setICB(nObject.getIcb());
    jObject.setSIC(nObject.getSic());
    jObject.setMultiplier(nObject.getMultiplier());
    jObject.setProduct(MAPPER_STRING.toJava(nObject.getProduct()));
    jObject.setUnderlying(MAPPER_STRING.toJava(nObject.getUnderlying()));
    jObject.setSPC(nObject.getSpc());
    jObject.setAdditionalUnderlyings(MAPPER_STRING.toJava(nObject.getAdditionalUnderlyings()));
    jObject.setMMY(MAPPER_STRING.toJava(nObject.getMmy()));
    jObject.setExpiration(nObject.getExpiration());
    jObject.setLastTrade(nObject.getLastTrade());
    jObject.setStrike(nObject.getStrike());
    jObject.setOptionType(MAPPER_STRING.toJava(nObject.getOptionType()));
    jObject.setExpirationStyle(MAPPER_STRING.toJava(nObject.getExpirationStyle()));
    jObject.setSettlementStyle(MAPPER_STRING.toJava(nObject.getSettlementStyle()));
    jObject.setPriceIncrements(MAPPER_STRING.toJava(nObject.getPriceIncrements()));
    jObject.setTradingHours(MAPPER_STRING.toJava(nObject.getTradingHours()));
    setCustomFields(jObject, MAPPER_STRINGS.toJavaList(nObject.getCustomFields()).toArray(new String[0]));
  }
}
