package com.dxfeed.api;

import com.dxfeed.api.events.DxfgSymbol;
import com.dxfeed.api.events.DxfgSymbolType;
import com.dxfeed.event.market.AnalyticOrderMapper;
import com.dxfeed.event.market.CandleExchangeMapper;
import com.dxfeed.event.market.CandleMapper;
import com.dxfeed.event.market.CandlePeriodMapper;
import com.dxfeed.event.market.CandlePriceLevelMapper;
import com.dxfeed.event.market.CandleSymbolMapper;
import com.dxfeed.event.market.ConfigurationMapper;
import com.dxfeed.event.market.DailyCandleMapper;
import com.dxfeed.event.market.EventMappersImpl;
import com.dxfeed.event.market.GreeksMapper;
import com.dxfeed.event.market.ListEventMapper;
import com.dxfeed.event.market.Mapper;
import com.dxfeed.event.market.MarketEventMapper;
import com.dxfeed.event.market.MessageMapper;
import com.dxfeed.event.market.OrderBaseMapper;
import com.dxfeed.event.market.OrderMapper;
import com.dxfeed.event.market.ProfileMapper;
import com.dxfeed.event.market.QuoteMapper;
import com.dxfeed.event.market.SeriesMapper;
import com.dxfeed.event.market.SpreadOrderMapper;
import com.dxfeed.event.market.StringMapper;
import com.dxfeed.event.market.StringMapperCacheStore;
import com.dxfeed.event.market.StringMapperUnlimitedStore;
import com.dxfeed.event.market.SummaryMapper;
import com.dxfeed.event.market.TheoPriceMapper;
import com.dxfeed.event.market.TimeAndSaleMapper;
import com.dxfeed.event.market.TradeETHMapper;
import com.dxfeed.event.market.TradeMapper;
import com.dxfeed.event.market.UnderlyingMapper;
import com.oracle.svm.core.SubstrateUtil;
import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

public final class NativeUtils {

  public static final ListEventMapper EVENT_MAPPER;
  public static final StringMapperCacheStore STRING_MAPPER_CACHE_STORE = new StringMapperCacheStore(
      3000);

  static {
    final Mapper<String, CCharPointer> stringMapper = new StringMapper();
    final Mapper<String, CCharPointer> stringMapperUnlimitedStore = new StringMapperUnlimitedStore();

    final CandleSymbolMapper candleSymbolMapper = new CandleSymbolMapper(
        stringMapperUnlimitedStore,
        new CandlePeriodMapper(STRING_MAPPER_CACHE_STORE),
        new CandleExchangeMapper(),
        new CandlePriceLevelMapper()
    );
    final MarketEventMapper marketEventMapper = new MarketEventMapper(stringMapperUnlimitedStore);
    EVENT_MAPPER = new ListEventMapper(
        new EventMappersImpl(
            new QuoteMapper(marketEventMapper),
            new SeriesMapper(marketEventMapper),
            new TimeAndSaleMapper(marketEventMapper, STRING_MAPPER_CACHE_STORE),
            new SpreadOrderMapper(marketEventMapper, STRING_MAPPER_CACHE_STORE),
            new OrderMapper(marketEventMapper, STRING_MAPPER_CACHE_STORE),
            new AnalyticOrderMapper(marketEventMapper),
            new MessageMapper(stringMapper),
            new OrderBaseMapper(marketEventMapper),
            new ConfigurationMapper(stringMapper),
            new TradeMapper(marketEventMapper),
            new TradeETHMapper(marketEventMapper),
            new TheoPriceMapper(marketEventMapper),
            new UnderlyingMapper(marketEventMapper),
            new GreeksMapper(marketEventMapper),
            new SummaryMapper(marketEventMapper),
            new ProfileMapper(marketEventMapper, STRING_MAPPER_CACHE_STORE),
            new DailyCandleMapper(candleSymbolMapper),
            new CandleMapper(candleSymbolMapper)
        )
    );
  }


  public static ObjectHandle createHandler(final Object object) {
    return ObjectHandles.getGlobal().create(object);
  }

  public static <T> T extractHandler(final ObjectHandle objectHandle) {
    return ObjectHandles.getGlobal().get(objectHandle);
  }

  public static void destroyHandler(final ObjectHandle objectHandle) {
    ObjectHandles.getGlobal().destroy(objectHandle);
  }

  public static String toJavaString(final CCharPointer cCharPointer) {
    if (cCharPointer.isNull()) {
      return null;
    }
    return CTypeConversion.toJavaString(
        cCharPointer,
        SubstrateUtil.strlen(cCharPointer),
        StandardCharsets.UTF_8
    );
  }

  public static Object toJavaSymbol(final DxfgSymbol dxfgSymbol) {
    return DxfgSymbolType.fromCValue(dxfgSymbol.getSymbolType())
        .createSymbolFunction.apply(toJavaString(dxfgSymbol.getSymbolString()));
  }
}
