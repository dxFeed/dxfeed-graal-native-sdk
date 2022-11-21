package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgMessage;
import com.dxfeed.event.misc.Message;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class MessageMapper extends EventMapper<Message, DxfgMessage> {

  protected final Mapper<String, CCharPointer> stringMapper;

  public MessageMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  public final void fillNativeObject(final Message jObject, final DxfgMessage nObject) {
    cleanNativeObject(nObject);
    nObject.setEventSymbol(this.stringMapper.toNativeObject(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
    nObject.setAttachment(this.stringMapper.toNativeObject(jObject.getAttachment().toString()));
  }

  @Override
  public DxfgMessage createNativeObject() {
    final DxfgMessage nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgMessage.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_MESSAGE.getCValue());
    return nObject;
  }

  @Override
  protected final void cleanNativeObject(final DxfgMessage nObject) {
    this.stringMapper.release(nObject.getEventSymbol());
    this.stringMapper.release(nObject.getAttachment());
  }

  @Override
  public Message toJavaObject(final DxfgMessage nObject) {
    final Message jObject = new Message();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgMessage nObject, final Message jObject) {
    jObject.setEventSymbol(this.stringMapper.toJavaObject(nObject.getEventSymbol()));
    jObject.setEventTime(nObject.getEventTime());
    jObject.setAttachment(this.stringMapper.toJavaObject(nObject.getAttachment())); //TODO
  }

  @Override
  public DxfgMessage createNativeObject(final String symbol) {
    final DxfgMessage nObject = createNativeObject();
    nObject.setEventSymbol(this.stringMapper.toNativeObject(symbol));
    return nObject;
  }
}
