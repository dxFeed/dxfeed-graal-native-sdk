package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_text_message_t")
public interface DxfgTextMessage extends DxfgEventType {
  @CField("event_symbol")
  CCharPointer getEventSymbol();

  @CField("event_symbol")
  void setEventSymbol(CCharPointer value);

  @CField("event_time")
  long getEventTime();

  @CField("event_time")
  void setEventTime(long eventTime);

  @CField("time_sequence")
  long getTimeSequence();

  @CField("time_sequence")
  void setTimeSequence(long value);

  @CField("text")
  CCharPointer getText();

  @CField("text")
  void setText(CCharPointer value);
}
