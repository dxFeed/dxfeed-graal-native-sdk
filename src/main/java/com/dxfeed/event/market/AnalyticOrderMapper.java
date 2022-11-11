package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgAnalyticOrder;
import com.dxfeed.api.events.DxfgEventKind;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class AnalyticOrderMapper extends OrderAbstractMapper<AnalyticOrder, DxfgAnalyticOrder> {

  public AnalyticOrderMapper(final MarketEventMapper marketEventMapper) {
    super(marketEventMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgAnalyticOrder.class);
  }

  @Override
  protected void fillNativeObject(final DxfgAnalyticOrder nObject, final AnalyticOrder jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_ANALYTIC_ORDER.getCValue());
    super.fillNativeObject(nObject, jObject);
    nObject.setIcebergPeakSize(jObject.getIcebergPeakSize());
    nObject.setIcebergHiddenSize(jObject.getIcebergHiddenSize());
    nObject.setIcebergExecutedSize(jObject.getIcebergExecutedSize());
    nObject.setIcebergFlags(jObject.getIcebergFlags());
  }

  @Override
  protected void cleanNativeObject(final DxfgAnalyticOrder nObject) {
    super.cleanNativeObject(nObject);
  }
}
