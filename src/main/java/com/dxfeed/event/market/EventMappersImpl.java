package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgAnalyticOrder;
import com.dxfeed.api.events.DxfgCandle;
import com.dxfeed.api.events.DxfgConfiguration;
import com.dxfeed.api.events.DxfgDailyCandle;
import com.dxfeed.api.events.DxfgEventKind;
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

public class EventMappersImpl implements EventMappers {

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

  public EventMappersImpl(
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
  public DxfgEventType nativeObject(final EventType<?> jEvent) {
    if (jEvent instanceof Quote) {
      return quoteMapper.nativeObject((Quote) jEvent);
    } else if (jEvent instanceof Series) {
      return seriesMapper.nativeObject((Series) jEvent);
    } else if (jEvent instanceof TimeAndSale) {
      return timeAndSaleMapper.nativeObject((TimeAndSale) jEvent);
    } else if (jEvent instanceof SpreadOrder) {
      return spreadOrderMapper.nativeObject((SpreadOrder) jEvent);
    } else if (jEvent instanceof AnalyticOrder) {
      return analyticOrderMapper.nativeObject((AnalyticOrder) jEvent);
    } else if (jEvent instanceof Message) {
      return messageMapper.nativeObject((Message) jEvent);
    } else if (jEvent instanceof Order) {
      return orderMapper.nativeObject((Order) jEvent);
    } else if (jEvent instanceof OrderBase) {
      return orderBaseMapper.nativeObject((OrderBase) jEvent);
    } else if (jEvent instanceof Configuration) {
      return configurationMapper.nativeObject((Configuration) jEvent);
    } else if (jEvent instanceof Trade) {
      return tradeMapper.nativeObject((Trade) jEvent);
    } else if (jEvent instanceof TradeETH) {
      return tradeETHMapper.nativeObject((TradeETH) jEvent);
    } else if (jEvent instanceof TheoPrice) {
      return theoPriceMapper.nativeObject((TheoPrice) jEvent);
    } else if (jEvent instanceof Underlying) {
      return underlyingMapper.nativeObject((Underlying) jEvent);
    } else if (jEvent instanceof Greeks) {
      return greeksMapper.nativeObject((Greeks) jEvent);
    } else if (jEvent instanceof Summary) {
      return summaryMapper.nativeObject((Summary) jEvent);
    } else if (jEvent instanceof Profile) {
      return profileMapper.nativeObject((Profile) jEvent);
    } else if (jEvent instanceof DailyCandle) {
      return dailyCandleMapper.nativeObject((DailyCandle) jEvent);
    } else if (jEvent instanceof Candle) {
      return candleMapper.nativeObject((Candle) jEvent);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void delete(final DxfgEventType nEvent) {
    switch (DxfgEventKind.fromCValue(nEvent.getKind())) {
      case DXFG_EVENT_TYPE_QUOTE:
        quoteMapper.delete((DxfgQuote) nEvent);
        break;
      case DXFG_EVENT_TYPE_SERIES:
        seriesMapper.delete((DxfgSeries) nEvent);
        break;
      case DXFG_EVENT_TYPE_TIME_AND_SALE:
        timeAndSaleMapper.delete((DxfgTimeAndSale) nEvent);
        break;
      case DXFG_EVENT_TYPE_SPREAD_ORDER:
        spreadOrderMapper.delete((DxfgSpreadOrder) nEvent);
        break;
      case DXFG_EVENT_TYPE_ORDER:
        orderMapper.delete((DxfgOrder) nEvent);
        break;
      case DXFG_EVENT_TYPE_ANALYTIC_ORDER:
        analyticOrderMapper.delete((DxfgAnalyticOrder) nEvent);
        break;
      case DXFG_EVENT_TYPE_MESSAGE:
        messageMapper.delete((DxfgMessage) nEvent);
        break;
      case DXFG_EVENT_TYPE_ORDER_BASE:
        orderBaseMapper.delete((DxfgOrderBase) nEvent);
        break;
      case DXFG_EVENT_TYPE_CONFIGURATION:
        configurationMapper.delete((DxfgConfiguration) nEvent);
        break;
      case DXFG_EVENT_TYPE_TRADE:
        tradeMapper.delete((DxfgTrade) nEvent);
        break;
      case DXFG_EVENT_TYPE_TRADE_ETH:
        tradeETHMapper.delete((DxfgTradeETH) nEvent);
        break;
      case DXFG_EVENT_TYPE_THEO_PRICE:
        theoPriceMapper.delete((DxfgTheoPrice) nEvent);
        break;
      case DXFG_EVENT_TYPE_UNDERLYING:
        underlyingMapper.delete((DxfgUnderlying) nEvent);
        break;
      case DXFG_EVENT_TYPE_GREEKS:
        greeksMapper.delete((DxfgGreeks) nEvent);
        break;
      case DXFG_EVENT_TYPE_SUMMARY:
        summaryMapper.delete((DxfgSummary) nEvent);
        break;
      case DXFG_EVENT_TYPE_PROFILE:
        profileMapper.delete((DxfgProfile) nEvent);
        break;
      case DXFG_EVENT_TYPE_DAILY_CANDLE:
        dailyCandleMapper.delete((DxfgDailyCandle) nEvent);
        break;
      case DXFG_EVENT_TYPE_CANDLE:
        candleMapper.delete((DxfgCandle) nEvent);
        break;
      default:
        throw new IllegalStateException();
    }
  }
}
