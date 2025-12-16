// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.symbol;

import com.dxfeed.api.osub.IndexedEventSubscriptionSymbol;
import com.dxfeed.api.osub.TimeSeriesSubscriptionSymbol;
import com.dxfeed.api.osub.WildcardSymbol;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.candle.CandleSymbol;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class SymbolMapper extends Mapper<Object, DxfgSymbol> {

    private final Mapper<String, CCharPointer> stringMapper;
    private final Mapper<IndexedEventSource, DxfgIndexedEventSourcePointer> mapperSource;

    public SymbolMapper(
            final Mapper<String, CCharPointer> stringMapper,
            final Mapper<IndexedEventSource, DxfgIndexedEventSourcePointer> mapperSource
    ) {
        this.stringMapper = stringMapper;
        this.mapperSource = mapperSource;
    }

    @Override
    public DxfgSymbol toNative(final Object javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }
        final DxfgSymbol nativeObject;
        if (javaObject instanceof String) {
            nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgStringSymbol.class));
            nativeObject.setType(DxfgSymbolType.STRING.getCValue());
        } else if (javaObject instanceof CandleSymbol) {
            nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandleSymbol.class));
            nativeObject.setType(DxfgSymbolType.CANDLE.getCValue());
        } else if (javaObject instanceof TimeSeriesSubscriptionSymbol) {
            nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTimeSeriesSubscriptionSymbol.class));
            nativeObject.setType(DxfgSymbolType.TIME_SERIES_SUBSCRIPTION.getCValue());
        } else if (javaObject instanceof IndexedEventSubscriptionSymbol) {
            nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgIndexedEventSubscriptionSymbol.class));
            nativeObject.setType(DxfgSymbolType.INDEXED_EVENT_SUBSCRIPTION.getCValue());
        } else if (javaObject instanceof WildcardSymbol) {
            nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgWildcardSymbol.class));
            nativeObject.setType(DxfgSymbolType.WILDCARD.getCValue());
        } else {
            throw new IllegalStateException();
        }

        fillNative(javaObject, nativeObject, false);

        return nativeObject;
    }

    @Override
    public void fillNative(final Object javaObject, final DxfgSymbol nativeObject, boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        if (javaObject instanceof String) {
            final DxfgStringSymbol dxfgStringSymbol = (DxfgStringSymbol) nativeObject;
            final String stringSymbol = (String) javaObject;
            dxfgStringSymbol.setType(DxfgSymbolType.STRING.getCValue());
            dxfgStringSymbol.setSymbol(this.stringMapper.toNative(stringSymbol));
        } else if (javaObject instanceof CandleSymbol) {
            final DxfgCandleSymbol dxfgCandleSymbol = (DxfgCandleSymbol) nativeObject;
            final CandleSymbol candleSymbol = (CandleSymbol) javaObject;
            dxfgCandleSymbol.setType(DxfgSymbolType.CANDLE.getCValue());
            dxfgCandleSymbol.setSymbol(this.stringMapper.toNative(candleSymbol.toString()));
        } else if (javaObject instanceof WildcardSymbol) {
            nativeObject.setType(DxfgSymbolType.WILDCARD.getCValue());
        } else if (javaObject instanceof TimeSeriesSubscriptionSymbol) {
            final DxfgTimeSeriesSubscriptionSymbol dxfgTssSymbol = (DxfgTimeSeriesSubscriptionSymbol) nativeObject;
            final TimeSeriesSubscriptionSymbol<?> tssSymbol = (TimeSeriesSubscriptionSymbol<?>) javaObject;
            dxfgTssSymbol.setType(DxfgSymbolType.TIME_SERIES_SUBSCRIPTION.getCValue());
            dxfgTssSymbol.setSymbol(toNative(tssSymbol.getEventSymbol()));
            dxfgTssSymbol.setFromTime(tssSymbol.getFromTime());
        } else if (javaObject instanceof IndexedEventSubscriptionSymbol) {
            final DxfgIndexedEventSubscriptionSymbol dxfgIesSymbol = (DxfgIndexedEventSubscriptionSymbol) nativeObject;
            final IndexedEventSubscriptionSymbol<?> iesSymbol = (IndexedEventSubscriptionSymbol<?>) javaObject;
            dxfgIesSymbol.setType(DxfgSymbolType.INDEXED_EVENT_SUBSCRIPTION.getCValue());
            dxfgIesSymbol.setSymbol(toNative(iesSymbol.getEventSymbol()));
            dxfgIesSymbol.setSource(this.mapperSource.toNative(iesSymbol.getSource()));
        }
    }

    @Override
    public void cleanNative(final DxfgSymbol nativeObject) {
        switch (DxfgSymbolType.fromCValue(nativeObject.getType())) {
            case STRING:
                this.stringMapper.release(((DxfgStringSymbol) nativeObject).getSymbol());
                break;
            case CANDLE:
                this.stringMapper.release(((DxfgCandleSymbol) nativeObject).getSymbol());
                break;
            case TIME_SERIES_SUBSCRIPTION:
                release(((DxfgTimeSeriesSubscriptionSymbol) nativeObject).getSymbol());
                break;
            case INDEXED_EVENT_SUBSCRIPTION:
                final DxfgIndexedEventSubscriptionSymbol dxfgIesSymbol = (DxfgIndexedEventSubscriptionSymbol) nativeObject;
                release(dxfgIesSymbol.getSymbol());
                this.mapperSource.release(dxfgIesSymbol.getSource());
                break;
        }
    }

    @Override
    protected Object doToJava(final DxfgSymbol nativeObject) {
        switch (DxfgSymbolType.fromCValue(nativeObject.getType())) {
            case STRING:
                return this.stringMapper.toJava(((DxfgStringSymbol) nativeObject).getSymbol());
            case CANDLE:
                return CandleSymbol.valueOf(
                        this.stringMapper.toJava(((DxfgCandleSymbol) nativeObject).getSymbol())
                );
            case WILDCARD:
                return WildcardSymbol.ALL;
            case TIME_SERIES_SUBSCRIPTION:
                final DxfgTimeSeriesSubscriptionSymbol dxfgTssSymbol = (DxfgTimeSeriesSubscriptionSymbol) nativeObject;
                return new TimeSeriesSubscriptionSymbol<>(
                        toJava(dxfgTssSymbol.getSymbol()),
                        dxfgTssSymbol.getFromTime()
                );
            case INDEXED_EVENT_SUBSCRIPTION:
                final DxfgIndexedEventSubscriptionSymbol dxfgIesSymbol = (DxfgIndexedEventSubscriptionSymbol) nativeObject;
                return new IndexedEventSubscriptionSymbol<>(
                        toJava(dxfgIesSymbol.getSymbol()),
                        this.mapperSource.toJava(dxfgIesSymbol.getSource())
                );
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void fillJava(final DxfgSymbol nativeObject, final Object javaObject) {
        throw new IllegalStateException("The Java object does not support setters.");
    }
}
