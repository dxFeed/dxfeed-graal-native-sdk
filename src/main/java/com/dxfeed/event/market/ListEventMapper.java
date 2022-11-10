package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgAnalyticOrder;
import com.dxfeed.api.events.DxfgCandle;
import com.dxfeed.api.events.DxfgConfiguration;
import com.dxfeed.api.events.DxfgDailyCandle;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgEventPointer;
import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.api.events.DxfgGreeks;
import com.dxfeed.api.events.DxfgMessage;
import com.dxfeed.api.events.DxfgOrder;
import com.dxfeed.api.events.DxfgOrderBase;
import com.dxfeed.api.events.DxfgProfile;
import com.dxfeed.api.events.DxfgQuote;
import com.dxfeed.api.events.DxfgSeries;
import com.dxfeed.api.events.DxfgSpreadOrder;
import com.dxfeed.api.events.DxfgSummary;
import com.dxfeed.api.events.DxfgTheoPrice;
import com.dxfeed.api.events.DxfgTimeAndSale;
import com.dxfeed.api.events.DxfgTrade;
import com.dxfeed.api.events.DxfgTradeETH;
import com.dxfeed.api.events.DxfgUnderlying;
import com.dxfeed.event.EventType;
import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.DailyCandle;
import com.dxfeed.event.misc.Configuration;
import com.dxfeed.event.misc.Message;
import com.dxfeed.event.option.Greeks;
import com.dxfeed.event.option.Series;
import com.dxfeed.event.option.TheoPrice;
import com.dxfeed.event.option.Underlying;
import java.util.List;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListEventMapper implements ListMapper<EventType<?>, DxfgEventPointer> {

  protected final QuoteMapper quoteMapper;
  protected final SeriesMapper seriesMapper;
  protected final TimeAndSaleMapper timeAndSaleMapper;
  protected final SpreadOrderMapper spreadOrderMapper;
  protected final OrderMapper orderMapper;
  protected final AnalyticOrderMapper analyticOrderMapper;
  protected final MessageMapper messageMapper;
  protected final OrderBaseMapper orderBaseMapper;
  protected final ConfigurationMapper configurationMapper;
  protected final TradeMapper tradeMapper;
  protected final TradeETHMapper tradeETHMapper;
  protected final TheoPriceMapper theoPriceMapper;
  protected final UnderlyingMapper underlyingMapper;
  protected final GreeksMapper greeksMapper;
  protected final SummaryMapper summaryMapper;
  protected final ProfileMapper profileMapper;
  protected final DailyCandleMapper dailyCandleMapper;
  protected final CandleMapper candleMapper;

  public ListEventMapper(
      final QuoteMapper quoteMapper,
      final SeriesMapper seriesMapper,
      final TimeAndSaleMapper timeAndSaleMapper,
      final SpreadOrderMapper spreadOrderMapper,
      final OrderMapper orderMapper,
      final AnalyticOrderMapper analyticOrderMapper,
      final MessageMapper messageMapper,
      final OrderBaseMapper orderBaseMapper,
      final ConfigurationMapper configurationMapper,
      final TradeMapper tradeMapper,
      final TradeETHMapper tradeETHMapper,
      final TheoPriceMapper theoPriceMapper,
      final UnderlyingMapper underlyingMapper,
      final GreeksMapper greeksMapper,
      final SummaryMapper summaryMapper,
      final ProfileMapper profileMapper,
      final DailyCandleMapper dailyCandleMapper,
      final CandleMapper candleMapper
  ) {
    this.quoteMapper = quoteMapper;
    this.seriesMapper = seriesMapper;
    this.timeAndSaleMapper = timeAndSaleMapper;
    this.spreadOrderMapper = spreadOrderMapper;
    this.orderMapper = orderMapper;
    this.analyticOrderMapper = analyticOrderMapper;
    this.messageMapper = messageMapper;
    this.orderBaseMapper = orderBaseMapper;
    this.configurationMapper = configurationMapper;
    this.tradeMapper = tradeMapper;
    this.tradeETHMapper = tradeETHMapper;
    this.theoPriceMapper = theoPriceMapper;
    this.underlyingMapper = underlyingMapper;
    this.greeksMapper = greeksMapper;
    this.summaryMapper = summaryMapper;
    this.profileMapper = profileMapper;
    this.dailyCandleMapper = dailyCandleMapper;
    this.candleMapper = candleMapper;
  }

  @Override
  public DxfgEventPointer nativeObject(final List<EventType<?>> events) {
    final DxfgEventPointer nativeEvents =
        UnmanagedMemory.calloc(SizeOf.get(DxfgEventPointer.class) * events.size());
    for (int i = 0; i < events.size(); ++i) {
      final EventType<?> eventType = events.get(i);
      final DxfgEventType nativeEvent;
      if (eventType instanceof Quote) {
        nativeEvent = quoteMapper.nativeObject((Quote) eventType);
      } else if (eventType instanceof Series) {
        nativeEvent = seriesMapper.nativeObject((Series) eventType);
      } else if (eventType instanceof TimeAndSale) {
        nativeEvent = timeAndSaleMapper.nativeObject((TimeAndSale) eventType);
      } else if (eventType instanceof SpreadOrder) {
        nativeEvent = spreadOrderMapper.nativeObject((SpreadOrder) eventType);
      } else if (eventType instanceof AnalyticOrder) {
        nativeEvent = analyticOrderMapper.nativeObject((AnalyticOrder) eventType);
      } else if (eventType instanceof Message) {
        nativeEvent = messageMapper.nativeObject((Message) eventType);
      } else if (eventType instanceof Order) {
        nativeEvent = orderMapper.nativeObject((Order) eventType);
      } else if (eventType instanceof OrderBase) {
        nativeEvent = orderBaseMapper.nativeObject((OrderBase) eventType);
      } else if (eventType instanceof Configuration) {
        nativeEvent = configurationMapper.nativeObject((Configuration) eventType);
      } else if (eventType instanceof Trade) {
        nativeEvent = tradeMapper.nativeObject((Trade) eventType);
      } else if (eventType instanceof TradeETH) {
        nativeEvent = tradeETHMapper.nativeObject((TradeETH) eventType);
      } else if (eventType instanceof TheoPrice) {
        nativeEvent = theoPriceMapper.nativeObject((TheoPrice) eventType);
      } else if (eventType instanceof Underlying) {
        nativeEvent = underlyingMapper.nativeObject((Underlying) eventType);
      } else if (eventType instanceof Greeks) {
        nativeEvent = greeksMapper.nativeObject((Greeks) eventType);
      } else if (eventType instanceof Summary) {
        nativeEvent = summaryMapper.nativeObject((Summary) eventType);
      } else if (eventType instanceof Profile) {
        nativeEvent = profileMapper.nativeObject((Profile) eventType);
      } else if (eventType instanceof DailyCandle) {
        nativeEvent = dailyCandleMapper.nativeObject((DailyCandle) eventType);
      } else if (eventType instanceof Candle) {
        nativeEvent = candleMapper.nativeObject((Candle) eventType);
      } else {
        throw new IllegalStateException();
      }
      nativeEvents.addressOf(i).write(nativeEvent);
    }
    return nativeEvents;
  }

  @Override
  public void delete(final DxfgEventPointer nativeEvents, final int size) {
    for (int i = 0; i < size; ++i) {
      final DxfgEventType nativeEvent = nativeEvents.addressOf(i).read();
      if (nativeEvent.isNonNull()) {
        switch (DxfgEventKind.fromCValue(nativeEvent.getKind())) {
          case DXFG_EVENT_TYPE_QUOTE:
            quoteMapper.delete((DxfgQuote) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_SERIES:
            seriesMapper.delete((DxfgSeries) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_TIME_AND_SALE:
            timeAndSaleMapper.delete((DxfgTimeAndSale) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_SPREAD_ORDER:
            spreadOrderMapper.delete((DxfgSpreadOrder) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_ORDER:
            orderMapper.delete((DxfgOrder) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_ANALYTIC_ORDER:
            analyticOrderMapper.delete((DxfgAnalyticOrder) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_MESSAGE:
            messageMapper.delete((DxfgMessage) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_ORDER_BASE:
            orderBaseMapper.delete((DxfgOrderBase) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_CONFIGURATION:
            configurationMapper.delete((DxfgConfiguration) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_TRADE:
            tradeMapper.delete((DxfgTrade) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_TRADE_ETH:
            tradeETHMapper.delete((DxfgTradeETH) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_THEO_PRICE:
            theoPriceMapper.delete((DxfgTheoPrice) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_UNDERLYING:
            underlyingMapper.delete((DxfgUnderlying) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_GREEKS:
            greeksMapper.delete((DxfgGreeks) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_SUMMARY:
            summaryMapper.delete((DxfgSummary) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_PROFILE:
            profileMapper.delete((DxfgProfile) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_DAILY_CANDLE:
            dailyCandleMapper.delete((DxfgDailyCandle) nativeEvent);
            break;
          case DXFG_EVENT_TYPE_CANDLE:
            candleMapper.delete((DxfgCandle) nativeEvent);
            break;
          default:
            throw new IllegalStateException();
        }
      }
    }
    UnmanagedMemory.free(nativeEvents);
  }
}
