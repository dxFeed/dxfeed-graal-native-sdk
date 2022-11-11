package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgMessage;
import com.dxfeed.event.misc.Message;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class MessageMapper extends Mapper<Message, DxfgMessage> {

  protected final Mapper<String, CCharPointer> stringMapper;

  public MessageMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgMessage.class);
  }

  @Override
  protected final void fillNativeObject(final DxfgMessage nObject, final Message jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_MESSAGE.getCValue());
    nObject.setEventSymbol(stringMapper.nativeObject(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
    nObject.setAttachment(stringMapper.nativeObject(jObject.getAttachment().toString()));
  }

  @Override
  protected final void cleanNativeObject(final DxfgMessage nObject) {
    stringMapper.delete(nObject.getEventSymbol());
    stringMapper.delete(nObject.getAttachment());
  }
}
