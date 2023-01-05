package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgOrder;
import com.dxfeed.api.events.DxfgOrderList;
import com.dxfeed.api.events.DxfgOrderPointer;
import com.dxfeed.api.maper.ListMapper;
import com.dxfeed.api.maper.Mapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListOrderMapper extends
    ListMapper<Order, DxfgOrder, DxfgOrderPointer, DxfgOrderList> {

  private final Mapper<Order, DxfgOrder> orderMapper;

  public ListOrderMapper(final Mapper<Order, DxfgOrder> orderMapper) {
    this.orderMapper = orderMapper;
  }

  @Override
  protected Order toJava(final DxfgOrder nObject) {
    return orderMapper.toJava(nObject);
  }

  @Override
  protected void releaseNative(final DxfgOrder nObject) {
    orderMapper.release(nObject);
  }

  @Override
  protected DxfgOrder toNative(final Order jObject) {
    return orderMapper.toNative(jObject);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgOrderList.class);
  }
}
