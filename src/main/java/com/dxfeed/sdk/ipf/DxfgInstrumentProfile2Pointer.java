package com.dxfeed.sdk.ipf;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;
import org.graalvm.word.SignedWord;

@CContext(Directives.class)
@CStruct("dxfg_instrument_profile2_t")
public interface DxfgInstrumentProfile2Pointer extends PointerBase {

  @CField("type")
  CCharPointer getType();

  @CField("type")
  void setType(CCharPointer value);

  @CField("symbol")
  CCharPointer getSymbol();

  @CField("symbol")
  void setSymbol(CCharPointer value);

  @CField("description")
  CCharPointer getDescription();

  @CField("description")
  void setDescription(CCharPointer value);

  @CField("local_symbol")
  CCharPointer getLocalSymbol();

  @CField("local_symbol")
  void setLocalSymbol(CCharPointer value);

  @CField("local_description")
  CCharPointer getLocalDescription();

  @CField("local_description")
  void setLocalDescription(CCharPointer value);

  @CField("country")
  CCharPointer getCountry();

  @CField("country")
  void setCountry(CCharPointer value);

  @CField("opol")
  CCharPointer getOpol();

  @CField("opol")
  void setOpol(CCharPointer value);

  @CField("exchange_data")
  CCharPointer getExchangeData();

  @CField("exchange_data")
  void setExchangeData(CCharPointer value);

  @CField("exchanges")
  CCharPointer getExchanges();

  @CField("exchanges")
  void setExchanges(CCharPointer value);

  @CField("currency")
  CCharPointer getCurrency();

  @CField("currency")
  void setCurrency(CCharPointer value);

  @CField("base_currency")
  CCharPointer getBaseCurrency();

  @CField("base_currency")
  void setBaseCurrency(CCharPointer value);

  @CField("cfi")
  CCharPointer getCfi();

  @CField("cfi")
  void setCfi(CCharPointer value);

  @CField("isin")
  CCharPointer getIsin();

  @CField("isin")
  void setIsin(CCharPointer value);

  @CField("sedol")
  CCharPointer getSedol();

  @CField("sedol")
  void setSedol(CCharPointer value);

  @CField("cusip")
  CCharPointer getCusip();

  @CField("cusip")
  void setCusip(CCharPointer value);

  @CField("icb")
  int getIcb();

  @CField("icb")
  void setIcb(int value);

  @CField("sic")
  int getSic();

  @CField("sic")
  void setSic(int value);

  @CField("multiplier")
  double getMultiplier();

  @CField("multiplier")
  void setMultiplier(double value);

  @CField("product")
  CCharPointer getProduct();

  @CField("product")
  void setProduct(CCharPointer value);

  @CField("underlying")
  CCharPointer getUnderlying();

  @CField("underlying")
  void setUnderlying(CCharPointer value);

  @CField("spc")
  double getSpc();

  @CField("spc")
  void setSpc(double value);

  @CField("additional_underlyings")
  CCharPointer getAdditionalUnderlyings();

  @CField("additional_underlyings")
  void setAdditionalUnderlyings(CCharPointer value);

  @CField("mmy")
  CCharPointer getMmy();

  @CField("mmy")
  void setMmy(CCharPointer value);

  @CField("expiration")
  int getExpiration();

  @CField("expiration")
  void setExpiration(int value);

  @CField("last_trade")
  int getLastTrade();

  @CField("last_trade")
  void setLastTrade(int value);

  @CField("strike")
  double getStrike();

  @CField("strike")
  void setStrike(double value);

  @CField("option_type")
  CCharPointer getOptionType();

  @CField("option_type")
  void setOptionType(CCharPointer value);

  @CField("expiration_style")
  CCharPointer getExpirationStyle();

  @CField("expiration_style")
  void setExpirationStyle(CCharPointer value);

  @CField("settlement_style")
  CCharPointer getSettlementStyle();

  @CField("settlement_style")
  void setSettlementStyle(CCharPointer value);

  @CField("price_increments")
  CCharPointer getPriceIncrements();

  @CField("price_increments")
  void setPriceIncrements(CCharPointer value);

  @CField("trading_hours")
  CCharPointer getTradingHours();

  @CField("trading_hours")
  void setTradingHours(CCharPointer value);

  @CField("instrument_profile_custom_fields")
  DxfgInstrumentProfileCustomFieldsHandle getInstrumentProfileCustomFields();

  @CField("instrument_profile_custom_fields")
  void setInstrumentProfileCustomFields(DxfgInstrumentProfileCustomFieldsHandle value);

  @CField("instrument_profile_custom_fields_hash")
  int getInstrumentProfileCustomFieldsHash();

  @CField("instrument_profile_custom_fields_hash")
  void setInstrumentProfileCustomFieldsHash(int value);

  DxfgInstrumentProfile2Pointer addressOf(int index);

  DxfgInstrumentProfile2Pointer addressOf(SignedWord index);
}
