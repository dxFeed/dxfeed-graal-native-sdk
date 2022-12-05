package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.events.DxfgAnalyticOrder;
import com.dxfeed.api.events.DxfgEventClazz;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class AnalyticOrderMapper extends OrderAbstractMapper<AnalyticOrder, DxfgAnalyticOrder> {

  public AnalyticOrderMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgAnalyticOrder createNativeObject() {
    final DxfgAnalyticOrder nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgAnalyticOrder.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_ANALYTIC_ORDER.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final AnalyticOrder jObject, final DxfgAnalyticOrder nObject) {
    super.fillNative(jObject, nObject);
    nObject.setIcebergPeakSize(jObject.getIcebergPeakSize());
    nObject.setIcebergHiddenSize(jObject.getIcebergHiddenSize());
    nObject.setIcebergExecutedSize(jObject.getIcebergExecutedSize());
    nObject.setIcebergFlags(jObject.getIcebergFlags());
  }

  @Override
  public void cleanNative(final DxfgAnalyticOrder nObject) {
    super.cleanNative(nObject);
  }

  @Override
  public AnalyticOrder toJava(final DxfgAnalyticOrder nObject) {
    final AnalyticOrder jObject = new AnalyticOrder();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgAnalyticOrder nObject, final AnalyticOrder jObject) {
    super.fillJava(nObject, jObject);
    jObject.setIcebergPeakSize(nObject.getIcebergPeakSize());
    jObject.setIcebergHiddenSize(nObject.getIcebergHiddenSize());
    jObject.setIcebergExecutedSize(nObject.getIcebergExecutedSize());
    jObject.setIcebergFlags(nObject.getIcebergFlags());
  }
}
