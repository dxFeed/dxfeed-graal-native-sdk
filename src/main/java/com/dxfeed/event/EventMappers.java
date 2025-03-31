// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event;

import com.dxfeed.event.candle.CandleMapper;
import com.dxfeed.event.candle.DailyCandleMapper;
import com.dxfeed.event.market.AnalyticOrderMapper;
import com.dxfeed.event.market.MarketMakerMapper;
import com.dxfeed.event.market.OptionSaleMapper;
import com.dxfeed.event.market.OrderBaseMapper;
import com.dxfeed.event.market.OrderMapper;
import com.dxfeed.event.market.OtcMarketsOrderMapper;
import com.dxfeed.event.market.ProfileMapper;
import com.dxfeed.event.market.QuoteMapper;
import com.dxfeed.event.market.SpreadOrderMapper;
import com.dxfeed.event.market.SummaryMapper;
import com.dxfeed.event.market.TimeAndSaleMapper;
import com.dxfeed.event.market.TradeETHMapper;
import com.dxfeed.event.market.TradeMapper;
import com.dxfeed.event.misc.ConfigurationMapper;
import com.dxfeed.event.misc.MessageMapper;
import com.dxfeed.event.misc.TextConfigurationMapper;
import com.dxfeed.event.misc.TextMessageMapper;
import com.dxfeed.event.option.GreeksMapper;
import com.dxfeed.event.option.SeriesMapper;
import com.dxfeed.event.option.TheoPriceMapper;
import com.dxfeed.event.option.UnderlyingMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class EventMappers extends Mapper<EventType<?>, DxfgEventType> {

    protected final Mapper<String, CCharPointer> stringMapper;
    protected final Map<Class<? extends EventType<?>>, EventMapper<?, ?>> mapperByClass;
    protected final Map<DxfgEventClazz, EventMapper<?, ?>> mapperByDxfgEventType;

    private EventMappers(
            final Mapper<String, CCharPointer> stringMapper,
            final EventMapper<?, ?>[] eventMappers
    ) {
        this.stringMapper = stringMapper;
        this.mapperByClass = new HashMap<>();
        this.mapperByDxfgEventType = new EnumMap<>(DxfgEventClazz.class);

        for (EventMapper<?, ?> mapper : eventMappers) {
            this.mapperByClass.put(mapper.getEventClazz().clazz, mapper);
            this.mapperByDxfgEventType.put(mapper.getEventClazz(), mapper);
        }
    }

    public static EventMappers createDefault(final Mapper<String, CCharPointer> stringMapper,
            Mapper<Object, DxfgSymbol> symbolMapper) {
        return new EventMappers(stringMapper, new EventMapper[]{
                //candle
                new CandleMapper<>(stringMapper, symbolMapper),
                new DailyCandleMapper(stringMapper, symbolMapper),
                //market
                new AnalyticOrderMapper(stringMapper),
                new MarketMakerMapper(stringMapper),
                new OptionSaleMapper(stringMapper),
                new OrderBaseMapper(stringMapper),
                new OrderMapper<>(stringMapper),
                new OtcMarketsOrderMapper(stringMapper),
                new ProfileMapper(stringMapper),
                new QuoteMapper(stringMapper),
                new SpreadOrderMapper(stringMapper),
                new SummaryMapper(stringMapper),
                new TimeAndSaleMapper(stringMapper),
                new TradeETHMapper(stringMapper),
                new TradeMapper(stringMapper),
                //misc
                new ConfigurationMapper(stringMapper),
                new MessageMapper(stringMapper),
                new TextConfigurationMapper(stringMapper),
                new TextMessageMapper(stringMapper),
                //option
                new GreeksMapper(stringMapper),
                new SeriesMapper(stringMapper),
                new TheoPriceMapper(stringMapper),
                new UnderlyingMapper(stringMapper),
        });
    }

    @Override
    public DxfgEventType toNative(final EventType<?> jEvent) {
        if (jEvent == null) {
            return WordFactory.nullPointer();
        }
        return this.mapperByClass.get(jEvent.getClass()).toNativeObjectWithCast(jEvent);
    }

    @Override
    public void release(final DxfgEventType nativeObject) {
        if (nativeObject.isNull()) {
            return;
        }
        final DxfgEventClazz key = DxfgEventClazz.fromCValue(nativeObject.getClazz());
        this.mapperByDxfgEventType.get(key).releaseWithCast(nativeObject);
    }

    @Override
    public void fillNative(final EventType<?> javaObject, final DxfgEventType nativeObject, boolean clean) {
        this.mapperByClass.get(javaObject.getClass()).fillNativeObjectWithCast(javaObject, nativeObject);
    }

    @Override
    public void cleanNative(final DxfgEventType nativeObject) {
        if (nativeObject.isNull()) {
            return;
        }
        final DxfgEventClazz key = DxfgEventClazz.fromCValue(nativeObject.getClazz());
        this.mapperByDxfgEventType.get(key).cleanNativeObjectWithCast(nativeObject);
    }

    @Override
    protected EventType<?> doToJava(final DxfgEventType nativeObject) {
        final DxfgEventClazz key = DxfgEventClazz.fromCValue(nativeObject.getClazz());
        return this.mapperByDxfgEventType.get(key).toJavaObjectWithCast(nativeObject);
    }

    @Override
    public void fillJava(final DxfgEventType nativeObject, final EventType<?> javaObject) {
        final DxfgEventClazz key = DxfgEventClazz.fromCValue(nativeObject.getClazz());
        this.mapperByDxfgEventType.get(key).fillJavaObjectWithCast(nativeObject, javaObject);
    }

    public DxfgEventType createNativeEvent(
            final DxfgEventClazz dxfgClazz,
            final CCharPointer symbol
    ) {
        return this.mapperByDxfgEventType.get(dxfgClazz)
                .createNativeObject(this.stringMapper.toJava(symbol));
    }
}
