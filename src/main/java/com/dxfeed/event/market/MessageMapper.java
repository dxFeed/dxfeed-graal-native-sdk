package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
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
  public final void fillNative(final Message jObject, final DxfgMessage nObject) {
    cleanNative(nObject);
    nObject.setEventSymbol(this.stringMapper.toNative(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
    nObject.setAttachment(this.stringMapper.toNative(jObject.getAttachment().toString()));
  }

  @Override
  public DxfgMessage createNativeObject() {
    final DxfgMessage nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgMessage.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_MESSAGE.getCValue());
    return nObject;
  }

  @Override
  public final void cleanNative(final DxfgMessage nObject) {
    this.stringMapper.release(nObject.getEventSymbol());
    this.stringMapper.release(nObject.getAttachment());
  }

  @Override
  public Message toJava(final DxfgMessage nObject) {
    final Message jObject = new Message();
    fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgMessage nObject, final Message jObject) {
    jObject.setEventSymbol(this.stringMapper.toJava(nObject.getEventSymbol()));
    jObject.setEventTime(nObject.getEventTime());
    jObject.setAttachment(this.stringMapper.toJava(nObject.getAttachment())); //TODO
  }

  @Override
  public DxfgMessage createNativeObject(final String symbol) {
    final DxfgMessage nObject = createNativeObject();
    nObject.setEventSymbol(this.stringMapper.toNative(symbol));
    return nObject;
  }
}
