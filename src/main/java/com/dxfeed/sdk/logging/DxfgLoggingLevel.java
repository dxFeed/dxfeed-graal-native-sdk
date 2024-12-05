// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_logging_level_t")
public enum DxfgLoggingLevel {
  DXFG_LOGGING_LEVEL_ALL(Level.ALL),
  DXFG_LOGGING_LEVEL_TRACE(Level.FINEST),
  DXFG_LOGGING_LEVEL_DEBUG(Level.FINE),
  DXFG_LOGGING_LEVEL_INFO(Level.INFO),
  DXFG_LOGGING_LEVEL_WARN(Level.WARNING),
  DXFG_LOGGING_LEVEL_ERROR(Level.SEVERE),
  DXFG_LOGGING_LEVEL_OFF(Level.OFF),
  ;

  private static final Map<Level, DxfgLoggingLevel> map = new HashMap<>();

  static {
    map.put(Level.ALL, DxfgLoggingLevel.DXFG_LOGGING_LEVEL_ALL);
    map.put(Level.FINEST, DxfgLoggingLevel.DXFG_LOGGING_LEVEL_TRACE);
    map.put(Level.FINE, DxfgLoggingLevel.DXFG_LOGGING_LEVEL_DEBUG);
    map.put(Level.INFO, DxfgLoggingLevel.DXFG_LOGGING_LEVEL_INFO);
    map.put(Level.WARNING, DxfgLoggingLevel.DXFG_LOGGING_LEVEL_WARN);
    map.put(Level.SEVERE, DxfgLoggingLevel.DXFG_LOGGING_LEVEL_ERROR);
    map.put(Level.OFF, DxfgLoggingLevel.DXFG_LOGGING_LEVEL_OFF);
  }

  private final Level level;

  DxfgLoggingLevel(final Level level) {
    this.level = level;
  }

  public static DxfgLoggingLevel of(final Level level) {
    return map.get(level);
  }

  @CEnumLookup
  public static native DxfgLoggingLevel fromCValue(int value);

  @CEnumValue
  public native int getCValue();

  public Level getLevel() {
    return level;
  }
}
