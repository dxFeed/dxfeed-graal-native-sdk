package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.event.EventType;

public interface EventMappers {

  DxfgEventType nativeObject(EventType<?> jEvent);

  void delete(DxfgEventType nEvent);
}
