package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_analytic_order_t")
public interface DxfgAnalyticOrder extends DxfgOrderBase {

  @CField("iceberg_peak_size")
  double getIcebergPeakSize();

  @CField("iceberg_peak_size")
  void setIcebergPeakSize(double value);

  @CField("iceberg_hidden_size")
  double getIcebergHiddenSize();

  @CField("iceberg_hidden_size")
  void setIcebergHiddenSize(double value);

  @CField("iceberg_executed_size")
  double getIcebergExecutedSize();

  @CField("iceberg_executed_size")
  void setIcebergExecutedSize(double value);

  @CField("iceberg_flags")
  int getIcebergFlags();

  @CField("iceberg_flags")
  void setIcebergFlags(int value);
}
