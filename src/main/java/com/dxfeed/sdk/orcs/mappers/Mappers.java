package com.dxfeed.sdk.orcs.mappers;

import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.orcs.api.AuthOrderSource;
import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.orcs.DxfgAuthOrderSourceHandle;
import com.dxfeed.sdk.orcs.DxfgPriceLevelServiceHandle;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceIdMapEntryListPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceIdMapEntryPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceIdMapEntryPointerPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceMapEntryListPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceMapEntryPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceMapEntryPointerPointer;
import com.dxfeed.sdk.orcs.PriceLevelServiceHolder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class Mappers {

    public static final Mapper<PriceLevelServiceHolder, DxfgPriceLevelServiceHandle> PRICE_LEVEL_SERVICE_MAPPER = new PriceLevelServiceMapper();
    public static final Mapper<AuthOrderSource, DxfgAuthOrderSourceHandle> AUTH_ORDER_SOURCE_MAPPER = new AuthOrderSourceMapper();

    public static final Mapper<Map.Entry<Integer, Set<String>>, DxfgSymbolsByOrderSourceIdMapEntryPointer> SYMBOLS_BY_ORDER_SOURCE_ID_MAP_ENTRY_MAPPER = new SymbolsByOrderSourceIdMapEntryMapper();

    public static final Mapper<Map.Entry<? extends IndexedEventSource, Set<String>>, DxfgSymbolsByOrderSourceMapEntryPointer> SYMBOLS_BY_ORDER_SOURCE_MAP_ENTRY_MAPPER = new SymbolsByOrderSourceMapEntryMapper();
    public static final ListMapper<Entry<Integer, Set<String>>, DxfgSymbolsByOrderSourceIdMapEntryPointer, DxfgSymbolsByOrderSourceIdMapEntryPointerPointer,
            DxfgSymbolsByOrderSourceIdMapEntryListPointer> SYMBOLS_BY_ORDER_SOURCE_ID_MAP_ENTRY_LIST_MAPPER = new SymbolsByOrderSourceIdMapEntryListMapper();

    public static final ListMapper<Map.Entry<? extends IndexedEventSource, Set<String>>, DxfgSymbolsByOrderSourceMapEntryPointer, DxfgSymbolsByOrderSourceMapEntryPointerPointer,
            DxfgSymbolsByOrderSourceMapEntryListPointer> SYMBOLS_BY_ORDER_SOURCE_MAP_ENTRY_LIST_MAPPER = new SymbolsByOrderSourceMapEntryListMapper();
}
