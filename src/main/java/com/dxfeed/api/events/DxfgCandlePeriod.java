package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_candle_period_t")
public interface DxfgCandlePeriod extends PointerBase {

  @CField("period_interval_millis")
  long getPeriodIntervalMillis();

  @CField("period_interval_millis")
  void setPeriodIntervalMillis(long periodIntervalMillis);

  @CField("value")
  double getValueCandlePeriod();

  @CField("value")
  void setValueCandlePeriod(double value);

  @CField("type")
  int getType();

  @CField("type")
  void setType(int value);

  @CField("string")
  CCharPointer getString();

  @CField("string")
  void setString(CCharPointer string);
}
