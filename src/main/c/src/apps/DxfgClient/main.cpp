// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "CommandLineParser.hpp"
#include <DxfgEventFormatter/QuoteFormatter.hpp>
#include <DxfgEventFormatter/TimeAndSaleFormatter.hpp>
#include <chrono>
#include <dxfg_api.h>
#include <iostream>
#include <thread>

using namespace dxfg;
using namespace std::literals;

static graal_isolate_t *maintIsolate = nullptr;
static graal_isolatethread_t *mainIsolateThread = nullptr;

namespace Args {
static std::unordered_map<std::string, std::string> properties{};
static std::string address{};
static std::vector<dxfg_event_clazz_t> eventTypes{};
static std::vector<std::string> symbols{};
static bool isQuiteMode = false;
} // namespace Args

static void printUsage() {
    // clang-format off
    std::cout << "usage: DxfgClient <properties> <address> <types> <symbols> <options>" << std::endl;
    std::cout << "where: <properties> is optional list space-separated properties and starting with -D (e.g. -Ddxfeed.aggregationPeriod=5s)." << std::endl;
    std::cout << "       <address>    is address to connect for data (remote host or local file):" << std::endl;
    std::cout << "                        <tcp:hostname:port> - for remote host.\r\n"
                 "                        <file:path_to_file> - for local file.\r\n"
                 "                        If you don't specify a prefix (tcp: or file:),\r\n"
                 "                        it will try to automatically determine the connector (by the presence of a colon char)." << std::endl;
    std::cout << "       <types>      is comma-separated list of dxfeed event type (Quote,TimeAndSale)." << std::endl;
    std::cout << "       <symbols>    is comma-separated list of event_symbol names to get events for (e.g. \"IBM,AAPL,MSFT\")." << std::endl;
    std::cout << "options:" << std::endl;
    std::cout << "       -q           is quiet mode, event printing disabled." << std::endl;
    std::cout << "examples:" << std::endl;
    std::cout << "       DxfgClient demo.dxfeed.com:7300 Quote,TimeAndSale AAPL,IBM,/BTCUSD:CXBINA" << std::endl;
    std::cout << "       DxfgClient -Ddxfeed.aggregationPeriod=1s demo.dxfeed.com:7300 Quote,TimeAndSale AAPL,IBM,/BTCUSD:CXBINA" << std::endl;
    // clang-format on
}

static void parseArgs(int argc, char *argv[]) {
    if (argc < 4) {
        std::cerr << "Argument parsing error!" << std::endl;
        printUsage();
        std::exit(-1);
    }

    try {
        int currentArg = 1;

        Args::properties = CommandLineParser::parseSystemProperties(argv, currentArg);
        Args::address = CommandLineParser::parseAddress(argv, currentArg);
        Args::eventTypes = CommandLineParser::parseEventTypes(argv, currentArg);
        Args::symbols = CommandLineParser::parseSymbols(argv, currentArg);

        while (currentArg != argc) {
            if (std::string(argv[currentArg]) == "-q") {
                Args::isQuiteMode = true;
            }

            ++currentArg;
        }
    } catch (std::exception &ex) {
        std::cerr << "Argument parsing error!" << std::endl;
        std::cerr << ex.what() << std::endl;
        printUsage();
        std::exit(-1);
    }
}

void print_exception(dxfg_exception_t *exception) {
    if (!exception) {
        return;
    }

    printf("C: exception %s %s\n", exception->class_name, exception->message);
    printf("C: PrintStackTrace %s\n", exception->print_stack_trace);

    for (int i = 0; i < exception->stack_trace->size; ++i) {
        dxfg_stack_trace_element_t *pStackTraceElement = exception->stack_trace->elements[i];
        printf("C: %s %d %s\n", pStackTraceElement->class_name, pStackTraceElement->line_number,
               pStackTraceElement->method_name);
    }

    print_exception(exception->cause);
}

void get_exception(graal_isolatethread_t *isolateThread) {
    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(isolateThread);

    print_exception(exception);
    dxfg_Exception_release(isolateThread, exception);
}

void printEvent(graal_isolatethread_t *isolateThread, const dxfg_event_type_t *pEvent) {
    if (!pEvent) {
        return;
    }

    switch (pEvent->clazz) {
    case DXFG_EVENT_QUOTE: {
        auto *quote = (dxfg_quote_t *)pEvent;

        printf("C: QUOTE{event_symbol=%s, event_time=%lld, time_millis_sequence=%d, time_nano_part=%d, bid_time=%lld, "
               "bid_exchange_code=%d, bid_price=%E, bid_size=%E, ask_time=%lld, ask_exchange_code=%hd, ask_price=%f, "
               "ask_size=%E}\n",
               quote->market_event.event_symbol, quote->market_event.event_time, quote->time_millis_sequence,
               quote->time_nano_part, quote->bid_time, quote->bid_exchange_code, quote->bid_price, quote->bid_size,
               quote->ask_time, quote->ask_exchange_code, quote->ask_price, quote->ask_size);
    } break;
    case DXFG_EVENT_PROFILE: {
        auto *profile = (dxfg_profile_t *)pEvent;

        printf("C: PROFILE{description=%s, status_reason=%s, high_52_week_price=%f}\n", profile->description,
               profile->status_reason, profile->high_52_week_price);
    } break;
    case DXFG_EVENT_SUMMARY: {
        auto *summary = (dxfg_summary_t *)pEvent;

        printf("C: SUMMARY{day_id=%d, day_open_price=%f}\n", summary->day_id, summary->day_open_price);
    } break;
    case DXFG_EVENT_GREEKS: {
        auto *greeks = (dxfg_greeks_t *)pEvent;

        printf("C: GREEKS{index=%lld, volatility=%f}\n", greeks->index, greeks->volatility);
    } break;
    case DXFG_EVENT_CANDLE: {
        auto *candle = (dxfg_candle_t *)pEvent;

        printf("C: CANDLE{symbol=%s, index=%lld, ask_volume=%E, volume=%f, event_time=%lld}\n", candle->event_symbol,
               candle->index, candle->ask_volume, candle->volume, candle->event_time);
    } break;
    case DXFG_EVENT_DAILY_CANDLE: {
        auto *daily_candle = (dxfg_daily_candle_t *)pEvent;

        printf("C: DAILY_CANDLE{symbol=%s, index=%lld, ask_volume=%E}\n", daily_candle->candle.event_symbol,
               daily_candle->candle.index, daily_candle->candle.ask_volume);
    } break;
    case DXFG_EVENT_UNDERLYING: {
        auto *underlying = (dxfg_underlying_t *)pEvent;

        printf("C: UNDERLYING{index=%lld, volatility=%f}\n", underlying->index, underlying->volatility);
    } break;
    case DXFG_EVENT_THEO_PRICE: {
        auto *theo_price = (dxfg_theo_price_t *)pEvent;

        printf("C: THEO_PRICE{index=%lld, price=%f}\n", theo_price->index, theo_price->price);
    } break;
    case DXFG_EVENT_TRADE: {
        auto *trade = (dxfg_trade_t *)pEvent;

        printf("C: TRADE{trade_base.size=%f, trade_base.price=%f}\n", trade->trade_base.size, trade->trade_base.price);
    } break;
    case DXFG_EVENT_TRADE_ETH: {
        auto *trade_eth = (dxfg_trade_eth_t *)pEvent;

        printf("C: TRADE_ETH{trade_base.size=%f, trade_base.price=%f}\n", trade_eth->trade_base.size,
               trade_eth->trade_base.price);
    } break;
    case DXFG_EVENT_CONFIGURATION: {
        auto *configuration = (dxfg_configuration_t *)pEvent;

        printf("C: CONFIGURATION{event_symbol=%s, version=%d}\n", configuration->event_symbol, configuration->version);
    } break;
    case DXFG_EVENT_MESSAGE: {
        auto *message = (dxfg_message_t *)pEvent;

        printf("C: MESSAGE{event_symbol=%s, event_time=%lld}\n", message->event_symbol, message->event_time);
    } break;
    case DXFG_EVENT_TIME_AND_SALE: {
        auto *time_and_sale = (dxfg_time_and_sale_t *)pEvent;

        printf("C: TIME_AND_SALE{event_symbol=%s, bid_price=%f, exchange_sale_conditions=%s, buyer=%s, seller=%s}\n",
               time_and_sale->market_event.event_symbol, time_and_sale->bid_price,
               time_and_sale->exchange_sale_conditions, time_and_sale->buyer, time_and_sale->seller);
    } break;
    // case DXFG_EVENT_ORDER_BASE:
    //     break;
    case DXFG_EVENT_ORDER: {
        auto *order = (dxfg_order_t *)pEvent;
        dxfg_indexed_event_source_t *source =
            dxfg_IndexedEvent_getSource(isolateThread, &order->order_base.market_event.event_type);

        printf("C: Order{%s, source=%s, price=%f, size=%f, marketMaker='%s'}\n",
               order->order_base.market_event.event_symbol, source->name, order->order_base.price,
               order->order_base.size, order->market_maker);
        dxfg_IndexedEventSource_release(isolateThread, source);
    } break;
    case DXFG_EVENT_ANALYTIC_ORDER: {
        auto *analytic_order = (dxfg_analytic_order_t *)pEvent;

        printf("C: ANALYTIC_ORDER{order_base.count=%lld, iceberg_peak_size=%f}\n",
               analytic_order->order_base.order_base.count, analytic_order->iceberg_peak_size);
    } break;
    case DXFG_EVENT_OTC_MARKETS_ORDER: {
        auto *otc_markets_order = (dxfg_otc_markets_order_t *)pEvent;

        printf("C: OTC_MARKETS_ORDER{order_base.count=%lld, quote_access_payment=%d}\n",
               otc_markets_order->order_base.order_base.count, otc_markets_order->quote_access_payment);
    } break;
    case DXFG_EVENT_SPREAD_ORDER: {
        auto *spread_order = (dxfg_spread_order_t *)pEvent;

        printf("C: SPREAD_ORDER{order_base.count=%lld, spread_symbol=%s}\n", spread_order->order_base.count,
               spread_order->spread_symbol);
    } break;
    case DXFG_EVENT_SERIES: {
        auto *series = (dxfg_series_t *)pEvent;

        printf("C: SERIES{event_symbol=%s, index=%lld, volatility=%f}\n", series->market_event.event_symbol,
               series->index, series->volatility);
    } break;
    case DXFG_EVENT_OPTION_SALE: {
        auto *option_sale = (dxfg_option_sale_t *)pEvent;

        printf("C: OPTION_SALE{event_symbol=%s, index=%lld, volatility=%f, option_symbol=%s}\n",
               option_sale->market_event.event_symbol, option_sale->index, option_sale->volatility,
               option_sale->option_symbol);
    } break;

    default:
        printf("C: %u{}\n", pEvent->clazz);
    }
}

void c_print(graal_isolatethread_t *isolateThread, dxfg_event_type_list *events, void * /* user_data */) {
    for (int i = 0; i < events->size; ++i) {
        printEvent(isolateThread, events->elements[i]);
    }
}

void c_promise_func(graal_isolatethread_t *isolateThread, dxfg_promise_t *promise, void * /* user_data */) {
    dxfg_event_type_t *event =
        dxfg_Promise_EventType_getResult(isolateThread, reinterpret_cast<dxfg_promise_event_t *>(promise));
    printf("********************* -> c_promise_func\n");
    printEvent(isolateThread, event);
    printf("********************* -> c_promise_func\n");
}

void c_listener_func(graal_isolatethread_t *isolateThread, dxfg_order_book_model_t *model, void * /* user_data */) {
    printf("Buy orders:\n");
    dxfg_observable_list_model_t *buyObservableListOrders = dxfg_OrderBookModel_getBuyOrders(isolateThread, model);
    dxfg_event_type_list *buyOrders = dxfg_ObservableListModel_toArray(isolateThread, buyObservableListOrders);
    for (int i = 0; i < buyOrders->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(buyOrders->elements[0]));
    }
    dxfg_CList_EventType_release(isolateThread, buyOrders);
    dxfg_JavaObjectHandler_release(isolateThread, &buyObservableListOrders->handler);

    printf("Sell orders:\n");
    dxfg_observable_list_model_t *sellObservableListOrders = dxfg_OrderBookModel_getSellOrders(isolateThread, model);
    dxfg_event_type_list *sellOrders = dxfg_ObservableListModel_toArray(isolateThread, sellObservableListOrders);
    for (int i = 0; i < sellOrders->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(sellOrders->elements[0]));
    }
    dxfg_CList_EventType_release(isolateThread, sellOrders);
    dxfg_JavaObjectHandler_release(isolateThread, &sellObservableListOrders->handler);

    printf("\n");
}

void c_observable_list_listener_func(graal_isolatethread_t *isolateThread, dxfg_event_type_list *orders,
                                     void * /* user_data */) {
    printf("c_observable_list_listener_func:\n");
    for (int i = 0; i < orders->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(orders->elements[0]));
    }
}

void endpoint_state_change_listener(graal_isolatethread_t * /* thread */, dxfg_endpoint_state_t old_state,
                                    dxfg_endpoint_state_t new_state, void * /* user_data */) {
    printf("C: state %d -> %d\n", old_state, new_state);
}

void updateListener(graal_isolatethread_t *thread, dxfg_iterable_ip_t *profiles, void * /* user_data */) {
    while (dxfg_Iterable_InstrumentProfile_hasNext(thread, profiles) == 1) {
        dxfg_instrument_profile_t *profile = dxfg_Iterable_InstrumentProfile_next(thread, profiles);
        if (!profile) {
            get_exception(thread);
        }
        printf("C: profile %s\n", dxfg_InstrumentProfile_getSymbol(thread, profile));
        dxfg_InstrumentProfile_release(thread, profile);
    }
}

void stateChangeListener(graal_isolatethread_t * /* thread */, dxfg_ipf_connection_state_t old_state,
                         dxfg_ipf_connection_state_t new_state, void * /* user_data */) {
    printf("C: IPF stateChangeListener %d -> %d\n", old_state, new_state);
}

void finalize(graal_isolatethread_t * /* thread */, void * /* user_data */) {
    printf("C: finalize\n");
}

void liveIpf(graal_isolatethread_t *isolateThread, const char *address) {
    printf("C: liveIpf BEGIN\n");
    dxfg_ipf_collector_t *collector = dxfg_InstrumentProfileCollector_new(isolateThread);
    dxfg_ipf_connection_t *connection =
        dxfg_InstrumentProfileConnection_createConnection(isolateThread, address, collector);
    printf("C: InstrumentProfileCollector state %d\n",
           dxfg_InstrumentProfileConnection_getState(isolateThread, connection));
    dxfg_ipf_update_listener_t *listener =
        dxfg_InstrumentProfileUpdateListener_new(isolateThread, &updateListener, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), finalize, nullptr);
    dxfg_InstrumentProfileCollector_addUpdateListener(isolateThread, collector, listener);
    dxfg_ipf_connection_state_change_listener_t *listenerState =
        dxfg_IpfPropertyChangeListener_new(isolateThread, &stateChangeListener, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listenerState), finalize, nullptr);
    dxfg_InstrumentProfileConnection_addStateChangeListener(isolateThread, connection, listenerState);
    dxfg_InstrumentProfileConnection_start(isolateThread, connection);
    printf("C: InstrumentProfileCollector state %d\n",
           dxfg_InstrumentProfileConnection_getState(isolateThread, connection));
    dxfg_InstrumentProfileConnection_waitUntilCompleted(isolateThread, connection, 30000);
    printf("C: InstrumentProfileCollector state %d\n",
           dxfg_InstrumentProfileConnection_getState(isolateThread, connection));
    dxfg_InstrumentProfileConnection_close(isolateThread, connection);
    printf("C: InstrumentProfileCollector state %d\n",
           dxfg_InstrumentProfileConnection_getState(isolateThread, connection));
    dxfg_JavaObjectHandler_release(isolateThread, &collector->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &connection->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listenerState->handler);
    printf("C: liveIpf END\n");
}

void readerIpf(graal_isolatethread_t *isolateThread, const char *address) {
    printf("C: readerIpf BEGIN\n");
    dxfg_instrument_profile_reader_t *reader = dxfg_InstrumentProfileReader_new(isolateThread);
    dxfg_instrument_profile_list *profiles = dxfg_InstrumentProfileReader_readFromFile(isolateThread, reader, address);
    for (int i = 0; i < profiles->size; ++i) {
        printf("C: profile %s\n", dxfg_InstrumentProfile_getSymbol(isolateThread, profiles->elements[i]));
    }
    dxfg_CList_InstrumentProfile_release(isolateThread, profiles);
    dxfg_JavaObjectHandler_release(isolateThread, &reader->handler);
    printf("C: readerIpf END\n");
}

void finalizeListener(graal_isolatethread_t *isolateThread, const char *address) {
    printf("C: finalizeListener BEGIN\n");
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_endpoint_state_change_listener_t *stateListener =
        dxfg_PropertyChangeListener_new(isolateThread, endpoint_state_change_listener, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(stateListener), finalize, nullptr);
    dxfg_DXEndpoint_addStateChangeListener(isolateThread, endpoint, stateListener);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &c_print, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(isolateThread, subscriptionQuote, listener);
    printf("C: get_state = %d\n", dxfg_DXEndpoint_getState(isolateThread, endpoint));
    dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &stateListener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    dxfg_gc(isolateThread);

    std::this_thread::sleep_for(2s);

    dxfg_gc(isolateThread);

    std::this_thread::sleep_for(2s);

    printf("C: finalizeListener END\n");
}

void executorBaseOnConcurrentLinkedQueue(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_executor_t *executor = dxfg_ExecutorBaseOnConcurrentLinkedQueue_new(isolateThread);
    dxfg_DXEndpoint_executor(isolateThread, endpoint, executor);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &c_print, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(isolateThread, subscriptionQuote, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbolAAPL.supper);

    std::this_thread::sleep_for(2s);

    dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(isolateThread, executor);

    std::this_thread::sleep_for(2s);

    dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(isolateThread, executor);

    std::this_thread::sleep_for(2s);

    dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(isolateThread, executor);

    std::this_thread::sleep_for(2s);

    dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &executor->handler);
}

void dxLink(graal_isolatethread_t *isolateThread, const char *address) {
    printf("C: dxLink BEGIN\n");
    dxfg_system_set_property(isolateThread, "dxfeed.experimental.dxlink.enable", "true");
    dxfg_system_set_property(isolateThread, "scheme", "ext:resource:dxlink.xml");
    get_exception(isolateThread);
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    get_exception(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &c_print, nullptr);
    dxfg_DXFeedSubscription_addEventListener(isolateThread, subscriptionQuote, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbolAAPL.supper);

    std::this_thread::sleep_for(2s);

    dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    printf("C: dxLink END\n");
}

void onDemandService(graal_isolatethread_t *isolateThread, const char *user, const char *password,
                     const char *address) {
    printf("C: onDemandService BEGIN\n");
    dxfg_on_demand_service_t *service = dxfg_OnDemandService_getInstance(isolateThread);
    dxfg_endpoint_t *endpoint = dxfg_OnDemandService_getEndpoint(isolateThread, service);
    dxfg_DXEndpoint_user(isolateThread, endpoint, user);
    dxfg_DXEndpoint_password(isolateThread, endpoint, password);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &c_print, nullptr);
    dxfg_DXFeedSubscription_addEventListener(isolateThread, subscriptionQuote, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbolAAPL.supper);
    // 1273171668000 - 2010-05-06 14:47:48.000 EST
    dxfg_OnDemandService_replay(isolateThread, service, 1273171668000);

    std::this_thread::sleep_for(2s);

    dxfg_DXEndpoint_closeAndAwaitTermination(isolateThread, endpoint);
    dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
    dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &service->handler);
    printf("C: onDemandService END\n");
}

void dxEndpointSubscription(graal_isolatethread_t *isolateThread, const char *address) {
    printf("C: dxEndpointSubscription BEGIN\n");
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &c_print, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(isolateThread, subscriptionQuote, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbolAAPL.supper);
    int containQuote = dxfg_DXFeedSubscription_containsEventType(isolateThread, subscriptionQuote, DXFG_EVENT_QUOTE);
    int containCandle = dxfg_DXFeedSubscription_containsEventType(isolateThread, subscriptionQuote, DXFG_EVENT_CANDLE);

    std::this_thread::sleep_for(2s);

    dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    printf("C: dxEndpointSubscription END\n");
}

void dxEndpointMonitoring(graal_isolatethread_t *isolateThread, const char *address) {
    printf("C: dxEndpointMonitoring BEGIN\n");
    dxfg_endpoint_builder_t *builder = dxfg_DXEndpoint_newBuilder(isolateThread);
    dxfg_DXEndpoint_Builder_withProperty(isolateThread, builder, "monitoring.stat", "1s");
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_Builder_build(isolateThread, builder);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &c_print, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(isolateThread, subscriptionQuote, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbolAAPL.supper);

    std::this_thread::sleep_for(2s);

    dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &builder->handler);
    printf("C: dxEndpointMonitoring END\n");
}

void dxEndpointTimeSeriesSubscription(graal_isolatethread_t *isolateThread, const char *address) {
    printf("C: dxEndpointTimeSeriesSubscription BEGIN\n");
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_event_clazz_list_t *event_clazz_list = dxfg_DXEndpoint_getEventTypes(isolateThread, endpoint);
    dxfg_time_series_subscription_t *subscriptionTaS =
        dxfg_DXFeed_createTimeSeriesSubscription2(isolateThread, feed, event_clazz_list);
    dxfg_CList_EventClazz_release(isolateThread, event_clazz_list);
    dxfg_DXFeedTimeSeriesSubscription_setFromTime(isolateThread, subscriptionTaS, 0);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &c_print, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(isolateThread, &subscriptionTaS->sub, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(isolateThread, &subscriptionTaS->sub, &symbolAAPL.supper);

    std::this_thread::sleep_for(2s);

    dxfg_DXFeedSubscription_close(isolateThread, reinterpret_cast<dxfg_subscription_t *>(subscriptionTaS));
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &subscriptionTaS->sub.handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    printf("C: dxEndpointTimeSeriesSubscription END\n");
}

void systemProperties(graal_isolatethread_t *isolateThread) {
    const char *propertyName = "property-name";
    const char *string = dxfg_system_get_property(isolateThread, propertyName);
    printf("\"%s\" must not be equal to \"property-value\"\n", string);
    dxfg_String_release(isolateThread, string);
    dxfg_system_set_property(isolateThread, "property-name", "property-value");
    const char *value = dxfg_system_get_property(isolateThread, propertyName);
    printf("\"%s\" must be equal to \"property-value\"\n", value);
    dxfg_String_release(isolateThread, value);
}

void exception(graal_isolatethread_t *isolateThread) {
    dxfg_java_object_handler *object = dxfg_throw_exception(isolateThread);
    if (!object) {
        get_exception(isolateThread);
    }
    dxfg_JavaObjectHandler_release(isolateThread, object);
}

void orderBookModel(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_order_book_model_t *order_book_model = dxfg_OrderBookModel_new(isolateThread);
    dxfg_OrderBookModel_setFilter(isolateThread, order_book_model, ALL);
    dxfg_OrderBookModel_setSymbol(isolateThread, order_book_model, "AAPL");
    dxfg_observable_list_model_t *buyOrders = dxfg_OrderBookModel_getBuyOrders(isolateThread, order_book_model);
    dxfg_observable_list_model_listener_t *buyOrdersListener =
        dxfg_ObservableListModelListener_new(isolateThread, &c_observable_list_listener_func, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(buyOrdersListener), &finalize,
                         nullptr);
    dxfg_ObservableListModel_addListener(isolateThread, buyOrders, buyOrdersListener);
    dxfg_observable_list_model_t *sellOrders = dxfg_OrderBookModel_getSellOrders(isolateThread, order_book_model);
    dxfg_observable_list_model_listener_t *sellOrdersListener =
        dxfg_ObservableListModelListener_new(isolateThread, &c_observable_list_listener_func, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(sellOrdersListener), &finalize,
                         nullptr);
    dxfg_ObservableListModel_addListener(isolateThread, sellOrders, sellOrdersListener);
    dxfg_order_book_model_listener_t *listener =
        dxfg_OrderBookModelListener_new(isolateThread, &c_listener_func, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), &finalize, nullptr);
    dxfg_OrderBookModel_addListener(isolateThread, order_book_model, listener);
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_OrderBookModel_attach(isolateThread, order_book_model, feed);

    std::this_thread::sleep_for(2s);

    dxfg_OrderBookModel_close(isolateThread, order_book_model);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &order_book_model->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &buyOrders->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &buyOrdersListener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &sellOrders->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &sellOrdersListener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
}

void indexedEventModel(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_indexed_event_model_t *indexed_event_model =
        dxfg_IndexedEventModel_new(isolateThread, DXFG_EVENT_TIME_AND_SALE);
    dxfg_IndexedEventModel_setSizeLimit(isolateThread, indexed_event_model, 30);
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_IndexedEventModel_attach(isolateThread, indexed_event_model, feed);
    dxfg_observable_list_model_t *observable_list_model =
        dxfg_IndexedEventModel_getEventsList(isolateThread, indexed_event_model);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_IndexedEventModel_setSymbol(isolateThread, indexed_event_model, &symbolAAPL.supper);
    dxfg_observable_list_model_listener_t *observable_list_model_listener =
        dxfg_ObservableListModelListener_new(isolateThread, &c_observable_list_listener_func, nullptr);
    dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(observable_list_model_listener),
                         &finalize, nullptr);
    dxfg_ObservableListModel_addListener(isolateThread, observable_list_model, observable_list_model_listener);

    std::this_thread::sleep_for(2s);

    dxfg_IndexedEventModel_close(isolateThread, indexed_event_model);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &indexed_event_model->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &observable_list_model->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &observable_list_model_listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
}

void promise(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_candle_symbol_t candleSymbol;
    candleSymbol.supper.type = CANDLE;
    candleSymbol.symbol = "AAPL";
    dxfg_promise_event_t *candlePromise =
        dxfg_DXFeed_getLastEventPromise(isolateThread, feed, DXFG_EVENT_CANDLE, &candleSymbol.supper);
    dxfg_Promise_whenDone(isolateThread, reinterpret_cast<dxfg_promise_t *>(candlePromise), &c_promise_func, nullptr);

    std::this_thread::sleep_for(2s);

    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &candlePromise->handler.handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
}

void lastEventIfSubscribed(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_candle_symbol_t symbol;
    symbol.supper.type = STRING;
    symbol.symbol = "AAPL";
    dxfg_event_type_t *event = dxfg_DXFeed_getLastEventIfSubscribed(isolateThread, feed, DXFG_EVENT_QUOTE,
                                                                    reinterpret_cast<dxfg_symbol_t *>(&symbol));
    dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
    dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbol.supper);
    event = dxfg_DXFeed_getLastEventIfSubscribed(isolateThread, feed, DXFG_EVENT_QUOTE,
                                                 reinterpret_cast<dxfg_symbol_t *>(&symbol));
    printEvent(isolateThread, event);
    dxfg_EventType_release(isolateThread, event);

    std::this_thread::sleep_for(2s);

    dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
}

void promisesAllOf(graal_isolatethread_t *isolateThread, const char *address, const std::vector<std::string> &symbols) {
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    size_t size = symbols.size();
    dxfg_symbol_list symbolList;

    symbolList.size = size;
    symbolList.elements = (dxfg_symbol_t **)malloc(sizeof(dxfg_symbol_t *) * size);

    for (int i = 0; i < size; i++) {
        auto symbol1 = new dxfg_string_symbol_t;
        symbol1->supper.type = STRING;
        symbol1->symbol = symbols[i].c_str();
        symbolList.elements[i] = &symbol1->supper;
    }
    dxfg_promise_list *promises = dxfg_DXFeed_getLastEventsPromises(isolateThread, feed, DXFG_EVENT_QUOTE, &symbolList);
    dxfg_promise_t *all = dxfg_Promises_allOf(isolateThread, promises);
    dxfg_Promise_awaitWithoutException(isolateThread, all, 30000);
    if (dxfg_Promise_hasResult(isolateThread, all) == 1) {
        for (int i = 0; i < size; ++i) {
            dxfg_event_type_t *event = dxfg_Promise_EventType_getResult(
                isolateThread, reinterpret_cast<dxfg_promise_event_t *>(promises->list.elements[i]));
            printEvent(isolateThread, event);
            dxfg_EventType_release(isolateThread, event);
        }
    }
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    for (int i = 0; i < size; i++) {
        free(symbolList.elements[i]);
    }
    free(symbolList.elements);
    dxfg_JavaObjectHandler_release(isolateThread, &all->handler);
    dxfg_CList_JavaObjectHandler_release(isolateThread, &promises->list);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
}

void indexedEventsPromise(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_string_symbol_t symbol;
    symbol.supper.type = STRING;
    symbol.symbol = "IBM";
    dxfg_indexed_event_source_t *source = dxfg_IndexedEventSource_new(isolateThread, nullptr);
    dxfg_promise_events_t *eventsPromise =
        dxfg_DXFeed_getIndexedEventsPromise(isolateThread, feed, DXFG_EVENT_SERIES, &symbol.supper, source);
    dxfg_IndexedEventSource_release(isolateThread, source);
    dxfg_Promise_awaitWithoutException(isolateThread, &eventsPromise->base, 30000);
    int hasResult = dxfg_Promise_hasResult(isolateThread, &eventsPromise->base);
    int hasException = dxfg_Promise_hasException(isolateThread, &eventsPromise->base);
    if (hasException == -1) {
        get_exception(isolateThread);
    } else if (hasException == 1) {
        dxfg_exception_t *exception = dxfg_Promise_getException(isolateThread, &eventsPromise->base);
        if (exception) {
            for (int i = 0; i < exception->stack_trace->size; ++i) {
                printf("C: %s\n", exception->stack_trace->elements[i]->class_name);
            }
        }
        dxfg_Exception_release(isolateThread, exception);
    } else {
        dxfg_event_type_list *events = dxfg_Promise_List_EventType_getResult(isolateThread, eventsPromise);
        c_print(isolateThread, events, nullptr);
        dxfg_CList_EventType_release(isolateThread, events);
    }
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &eventsPromise->base.handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
}

void getLastEvent(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_candle_symbol_t symbol;
    symbol.supper.type = CANDLE;
    symbol.symbol = "AAPL";
    dxfg_subscription_t *subscription = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_CANDLE);
    dxfg_DXFeedSubscription_setSymbol(isolateThread, subscription, &symbol.supper);
    dxfg_candle_t *candle = (dxfg_candle_t *)dxfg_EventType_new(isolateThread, symbol.symbol, DXFG_EVENT_CANDLE);
    dxfg_DXFeed_getLastEvent(isolateThread, feed, &candle->event_type);
    printEvent(isolateThread, &candle->event_type);
    dxfg_event_type_list event_type_list;
    event_type_list.size = 2;
    event_type_list.elements = (dxfg_event_type_t **)malloc(sizeof(dxfg_event_type_t *) * 2);
    event_type_list.elements[0] = (dxfg_event_type_t *)dxfg_EventType_new(isolateThread, "AAPL", DXFG_EVENT_CANDLE);
    event_type_list.elements[1] = (dxfg_event_type_t *)dxfg_EventType_new(isolateThread, "IBM", DXFG_EVENT_CANDLE);
    printEvent(isolateThread, event_type_list.elements[0]);
    printEvent(isolateThread, event_type_list.elements[1]);

    std::this_thread::sleep_for(2s);

    dxfg_DXFeed_getLastEvent(isolateThread, feed, &candle->event_type);
    printEvent(isolateThread, &candle->event_type);
    dxfg_DXFeed_getLastEvents(isolateThread, feed, &event_type_list);
    printEvent(isolateThread, event_type_list.elements[0]);
    printEvent(isolateThread, event_type_list.elements[1]);

    std::this_thread::sleep_for(2s);

    dxfg_DXFeed_getLastEvent(isolateThread, feed, &candle->event_type);
    printEvent(isolateThread, &candle->event_type);
    dxfg_DXFeed_getLastEvents(isolateThread, feed, &event_type_list);
    printEvent(isolateThread, event_type_list.elements[0]);
    printEvent(isolateThread, event_type_list.elements[1]);
    dxfg_DXFeedSubscription_close(isolateThread, subscription);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_EventType_release(isolateThread, &candle->event_type);
    dxfg_JavaObjectHandler_release(isolateThread, &subscription->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
}

void schedule(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_system_set_property(isolateThread, "com.dxfeed.schedule.download", "auto");

    dxfg_instrument_profile_reader_t *reader = dxfg_InstrumentProfileReader_new(isolateThread);
    dxfg_instrument_profile_list *profiles = dxfg_InstrumentProfileReader_readFromFile(isolateThread, reader, address);

    for (int i = 0; i < profiles->size; ++i) {
        dxfg_instrument_profile_t *profile = profiles->elements[i];
        dxfg_schedule_t *schedule = dxfg_Schedule_getInstance(isolateThread, profile);
        const char *name = dxfg_Schedule_getName(isolateThread, schedule);
        printf("C: schedule %s\n", name);
        dxfg_String_release(isolateThread, name);
        dxfg_JavaObjectHandler_release(isolateThread, &schedule->handler);
        dxfg_string_list *venues = dxfg_Schedule_getTradingVenues(isolateThread, profile);

        for (int j = 0; j < venues->size; ++j) {
            dxfg_schedule_t *schedule2 = dxfg_Schedule_getInstance3(isolateThread, profile, venues->elements[j]);
            const char *name2 = dxfg_Schedule_getName(isolateThread, schedule2);
            printf("C: schedule %s\n", name2);
            dxfg_String_release(isolateThread, name2);
            dxfg_JavaObjectHandler_release(isolateThread, &schedule2->handler);
        }

        dxfg_CList_String_release(isolateThread, venues);
        printf("C: schedule %s\n", dxfg_InstrumentProfile_getSymbol(isolateThread, profiles->elements[i]));
    }

    dxfg_CList_InstrumentProfile_release(isolateThread, profiles);
    dxfg_JavaObjectHandler_release(isolateThread, &reader->handler);
}

void schedule2(graal_isolatethread_t *isolateThread) {
    dxfg_system_set_property(isolateThread, "com.dxfeed.schedule.download", "auto");

    dxfg_schedule_t *schedule = dxfg_Schedule_getInstance2(isolateThread, "(tz=GMT;de=2300;0=)");
    dxfg_day_t *day = dxfg_Schedule_getDayById(isolateThread, schedule, 42);
    int32_t dayId = dxfg_Day_getDayId(isolateThread, day);

    printf("C: dayId %d should be 42\n", dayId);

    const char *timeZoneGetId = dxfg_Schedule_getTimeZone_getID(isolateThread, schedule);

    printf("C: timeZoneGetId = %s\n", timeZoneGetId);

    const char *name = dxfg_Schedule_getName(isolateThread, schedule);

    printf("C: schedule %s\n", name);
    dxfg_String_release(isolateThread, name);
    dxfg_String_release(isolateThread, timeZoneGetId);
    dxfg_JavaObjectHandler_release(isolateThread, &day->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &schedule->handler);
}

void c_tx_model_listener_func(graal_isolatethread_t *isolateThread, dxfg_indexed_event_source_t * /* source */,
                              dxfg_event_type_list *events, int32_t /* isSnapshot */, void * /* user_data */) {
    printf("c_tx_model_listener_func:\n");

    for (int i = 0; i < events->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(events->elements[i]));
    }
}

void txIndexedEventModel(graal_isolatethread_t *isolateThread, const char *address) {
    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
    dxfg_DXEndpoint_connect(isolateThread, endpoint, address);
    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
    dxfg_string_symbol_t symbol;
    symbol.supper.type = STRING;
    symbol.symbol = "IBM";

    dxfg_indexed_tx_model_builder_t *builder = dxfg_IndexedTxModel_newBuilder(isolateThread, DXFG_EVENT_ORDER);
    dxfg_IndexedTxModel_Builder_withBatchProcessing(isolateThread, builder, 1);
    dxfg_IndexedTxModel_Builder_withSnapshotProcessing(isolateThread, builder, 1);
    dxfg_IndexedTxModel_Builder_withFeed(isolateThread, builder, feed);
    dxfg_IndexedTxModel_Builder_withSymbol(isolateThread, builder, &symbol.supper);
    dxfg_tx_model_listener_t *listener = dxfg_TxModelListener_new(isolateThread, c_tx_model_listener_func, nullptr);
    dxfg_IndexedTxModel_Builder_withListener(isolateThread, builder, listener);
    dxfg_indexed_tx_model_t *model = dxfg_IndexedTxModel_Builder_build(isolateThread, builder);

    std::this_thread::sleep_for(2s);

    dxfg_indexed_event_source_list sourceList;
    sourceList.size = 1;
    sourceList.elements = (dxfg_indexed_event_source_t **)malloc(sizeof(dxfg_indexed_event_source_t *) * 1);
    for (int i = 0; i < sourceList.size; i++) {
        dxfg_indexed_event_source_t pSource;
        pSource.id = 6579576;
        pSource.name = "dex";
        pSource.type = INDEXED_EVENT_SOURCE;
        sourceList.elements[i] = &pSource;
    }

    dxfg_IndexedTxModel_setSources(isolateThread, model, &sourceList);

    std::this_thread::sleep_for(2s);

    dxfg_IndexedTxModel_close(isolateThread, model);
    dxfg_DXEndpoint_close(isolateThread, endpoint);
    dxfg_JavaObjectHandler_release(isolateThread, &builder->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &model->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
    dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
}

void additionalUnderlyingsCase(graal_isolatethread_t *isolateThread) {
    puts("== Additional Underlyings ==");

    {
        puts("-- dxfg_AdditionalUnderlyings_EMPTY(..., !NULL) --");

        dxfg_additional_underlyings_t *additionalUnderlyings{};
        auto result = dxfg_AdditionalUnderlyings_EMPTY(isolateThread, &additionalUnderlyings);

        printf("  dxfg_AdditionalUnderlyings_EMPTY() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", additionalUnderlyings->handler.java_object_handle);
            dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    }

    {
        puts("-- dxfg_AdditionalUnderlyings_EMPTY(..., NULL) --");

        auto result = dxfg_AdditionalUnderlyings_EMPTY(isolateThread, nullptr);

        printf("  dxfg_AdditionalUnderlyings_EMPTY() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            puts(", The result is successful, but there must be an error.");
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    }

    auto run_dxfg_AdditionalUnderlyings_valueOf = [](graal_isolatethread_t *isolateThread, const char *text) {
        dxfg_additional_underlyings_t *additionalUnderlyings{};
        auto result = dxfg_AdditionalUnderlyings_valueOf(isolateThread, text, &additionalUnderlyings);

        printf("  dxfg_AdditionalUnderlyings_valueOf() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", additionalUnderlyings->handler.java_object_handle);
            dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- dxfg_AdditionalUnderlyings_valueOf(..., "FIS 53; US$ 45.46", !NULL) --)");
        run_dxfg_AdditionalUnderlyings_valueOf(isolateThread, "FIS 53; US$ 45.46");
    }

    {
        puts(R"(-- dxfg_AdditionalUnderlyings_valueOf(..., "", !NULL) --)");
        run_dxfg_AdditionalUnderlyings_valueOf(isolateThread, "");
    }

    {
        puts("-- dxfg_AdditionalUnderlyings_valueOf(..., NULL, !NULL) --");
        run_dxfg_AdditionalUnderlyings_valueOf(isolateThread, nullptr);
    }

    auto run_dxfg_AdditionalUnderlyings_valueOf2 = [](graal_isolatethread_t *isolateThread,
                                                      const dxfg_string_to_double_map_entry_t *mapEntries,
                                                      std::int32_t size) {
        dxfg_additional_underlyings_t *additionalUnderlyings{};
        auto result = dxfg_AdditionalUnderlyings_valueOf2(isolateThread, mapEntries, size, &additionalUnderlyings);

        printf("  dxfg_AdditionalUnderlyings_valueOf2(%p, %d) -> %d", mapEntries, size, result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", additionalUnderlyings->handler.java_object_handle);
            dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };
    {
        std::vector<dxfg_string_to_double_map_entry_t> mapEntries{{"FIS", 53}, {"US$", 45.46}, {"ABC", 123}};

        {
            puts(R"(-- dxfg_AdditionalUnderlyings_valueOf2(..., {{"FIS", 53}, {"US$", 45.46}, {"ABC", 123}}, ...)--)");
            run_dxfg_AdditionalUnderlyings_valueOf2(isolateThread, mapEntries.data(),
                                                    static_cast<std::int32_t>(mapEntries.size()));
        }

        {
            std::vector<dxfg_string_to_double_map_entry_t> mapEntries2{{"FIS", 53}, {"US$", 45.46}, {"", 0}};

            puts(R"(-- dxfg_AdditionalUnderlyings_valueOf2(..., {{"FIS", 53}, {"US$", 45.46}, {"", 0}}, ...) --)");
            run_dxfg_AdditionalUnderlyings_valueOf2(isolateThread, mapEntries2.data(),
                                                    static_cast<std::int32_t>(mapEntries2.size()));
        }

        {
            puts(
                R"(-- dxfg_AdditionalUnderlyings_valueOf2(..., {{"FIS", 53}, {"US$", 45.46}, {"ABC", 123}}, 0, ...) --)");
            run_dxfg_AdditionalUnderlyings_valueOf2(isolateThread, mapEntries.data(), 0);
        }

        {
            puts("-- dxfg_AdditionalUnderlyings_valueOf2(..., NULL, 0, ...) --");
            run_dxfg_AdditionalUnderlyings_valueOf2(isolateThread, nullptr, 0);
        }
    }

    auto run_dxfg_AdditionalUnderlyings_getSPC = [](graal_isolatethread_t *isolateThread, const char *text,
                                                    const char *symbol) {
        double spc = 0.0;
        int32_t result = dxfg_AdditionalUnderlyings_getSPC(isolateThread, text, symbol, &spc);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  spc = %g\n", spc);
        } else {
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., "FIS 53; US$ 45.46", "US$", ...) --)");
        run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, "FIS 53; US$ 45.46", "US$");
    }

    {
        puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., "FIS 53; USB 45.46", "US$", ...) --)");
        run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, "FIS 53; USB 45.46", "US$");
    }

    {
        puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., "FIS 53; US$ 45.46", "", ...) --)");
        run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, "FIS 53; US$ 45.46", "");
    }

    {
        puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., "FIS 53; US$ 45.46", nullptr, ...) --)");
        run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, "FIS 53; US$ 45.46", nullptr);
    }

    {
        puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., nullptr, nullptr, ...) --)");
        run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, nullptr, nullptr);
    }

    do {
        puts(R"(-- valueOf("FIS 53; US$ 45.46") + getText() --)");
        dxfg_additional_underlyings_t *additionalUnderlyings{};
        auto result = dxfg_AdditionalUnderlyings_valueOf(isolateThread, "FIS 53; US$ 45.46", &additionalUnderlyings);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", additionalUnderlyings->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        char *text = {};
        result = dxfg_AdditionalUnderlyings_getText(isolateThread, additionalUnderlyings, &text);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  text = %s\n", text);

            dxfg_String_release(isolateThread, text);
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
    } while (false);

    do {
        puts(R"(-- valueOf("FIS 53; US$ 45.46") + getMap() --)");
        dxfg_additional_underlyings_t *additionalUnderlyings{};
        auto result = dxfg_AdditionalUnderlyings_valueOf(isolateThread, "FIS 53; US$ 45.46", &additionalUnderlyings);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", additionalUnderlyings->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        dxfg_string_to_double_map_entry_t *mapEntries = {};
        std::int32_t size = {};

        result = dxfg_AdditionalUnderlyings_getMap(isolateThread, additionalUnderlyings, &mapEntries, &size);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            puts("  map entries:");
            for (int i = 0; i < size; i++) {
                printf("  [%d]: key = '%s', value = %g\n", i, mapEntries[i].key, mapEntries[i].value);
            }

            dxfg_free_string_to_double_map_entries(isolateThread, mapEntries, size);
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
    } while (false);

    do {
        puts(R"(-- valueOf("FIS 53; US$ 45.46") + getSPC2("US$") --)");
        dxfg_additional_underlyings_t *additionalUnderlyings{};
        auto result = dxfg_AdditionalUnderlyings_valueOf(isolateThread, "FIS 53; US$ 45.46", &additionalUnderlyings);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", additionalUnderlyings->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        double spc = 0.0;

        result = dxfg_AdditionalUnderlyings_getSPC2(isolateThread, additionalUnderlyings, "US$", &spc);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  spc = %g\n", spc);
        } else {
            get_exception(isolateThread);
        }

        dxfg_java_object_handler *clone = {};

        dxfg_JavaObjectHandler_clone(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(additionalUnderlyings),
                                     &clone);
        dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);

        result = dxfg_AdditionalUnderlyings_getSPC2(
            isolateThread, reinterpret_cast<dxfg_additional_underlyings_t *>(clone), "FIS", &spc);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  spc = %g\n", spc);
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, clone);
    } while (false);
}

void cfiCase(graal_isolatethread_t *isolateThread) {
    puts("== CFI + Value + Attribute ==");

    {
        puts("-- dxfg_CFI_EMPTY(..., !NULL) --");

        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_EMPTY(isolateThread, &cfi);

        printf("  dxfg_CFI_EMPTY() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", cfi->handler.java_object_handle);
            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    }

    {
        puts("-- dxfg_CFI_EMPTY(..., NULL) --");

        auto result = dxfg_CFI_EMPTY(isolateThread, nullptr);

        printf("  dxfg_CFI_EMPTY() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            puts(", The result is successful, but there must be an error.");
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    }

    auto run_dxfg_CFI_valueOf = [](graal_isolatethread_t *isolateThread, const char *text) {
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf(isolateThread, text, &cfi);

        printf("  dxfg_CFI_valueOf() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", cfi->handler.java_object_handle);

            char *code = {};
            dxfg_CFI_getCode(isolateThread, cfi, &code);

            printf("  code = %s\n", code);

            dxfg_String_release(isolateThread, code);

            std::int32_t intCode = 0;

            dxfg_CFI_getIntCode(isolateThread, cfi, &intCode);

            printf("  intCode = %d\n", intCode);

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- dxfg_CFI_valueOf(..., "OPEFPS", !NULL) --)");
        run_dxfg_CFI_valueOf(isolateThread, "OPEFPS");
    }

    {
        puts(R"(-- dxfg_CFI_valueOf(..., "", !NULL) --)");
        run_dxfg_CFI_valueOf(isolateThread, "");
    }

    {
        puts("-- dxfg_CFI_valueOf(..., NULL, !NULL) --");
        run_dxfg_CFI_valueOf(isolateThread, nullptr);
    }

    auto run_dxfg_CFI_valueOf2 = [](graal_isolatethread_t *isolateThread, std::int32_t intCode) {
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf2(isolateThread, intCode, &cfi);

        printf("  dxfg_CFI_valueOf2() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", cfi->handler.java_object_handle);

            char *code = {};
            dxfg_CFI_getCode(isolateThread, cfi, &code);

            printf("  code = %s\n", code);

            dxfg_String_release(isolateThread, code);

            std::int32_t intCode2 = 0;

            dxfg_CFI_getIntCode(isolateThread, cfi, &intCode2);

            printf("  intCode = %d\n", intCode2);

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- dxfg_CFI_valueOf2(..., 520264211, ...) --)");
        run_dxfg_CFI_valueOf2(isolateThread, 520264211);
    }

    {
        puts(R"(-- dxfg_CFI_valueOf2(..., 831283992, ...) --)");
        run_dxfg_CFI_valueOf2(isolateThread, 831283992);
    }

    {
        puts(R"(-- dxfg_CFI_valueOf2(..., 42, ...) --)");
        run_dxfg_CFI_valueOf2(isolateThread, 42);
    }

    {
        puts(R"(-- dxfg_CFI_valueOf2(..., 0, ...) --)");
        run_dxfg_CFI_valueOf2(isolateThread, 0);
    }

    {
        puts(R"(-- dxfg_CFI_valueOf2(..., -42, ...) --)");
        run_dxfg_CFI_valueOf2(isolateThread, -42);
    }

    do {
        puts(R"(-- valueOf2(520264211) + getCode() --)");
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf2(isolateThread, 520264211, &cfi);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", cfi->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        char *code = {};
        result = dxfg_CFI_getCode(isolateThread, cfi, &code);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  code = %s\n", code);

            dxfg_String_release(isolateThread, code);
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
    } while (false);

    do {
        puts(R"(-- valueOf("OPEFPS") + getIntCode() --)");
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", cfi->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        int32_t intCode = 0;
        result = dxfg_CFI_getIntCode(isolateThread, cfi, &intCode);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  intCode = %d\n", intCode);
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
    } while (false);

    do {
        puts(R"(-- valueOf("OPEFPS") + getCategory() --)");
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", cfi->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        int16_t category = 0;
        result = dxfg_CFI_getCategory(isolateThread, cfi, &category);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  category = 0x%x\n", category);
            printf("  category = '%c'\n", static_cast<char>(static_cast<uint16_t>(category) & 0xFF));
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
    } while (false);

    do {
        puts(R"(-- valueOf("OPEFPS") + getGroup() --)");
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", cfi->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        int16_t group = 0;
        result = dxfg_CFI_getGroup(isolateThread, cfi, &group);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  group = 0x%x\n", group);
            printf("  group = '%c'\n", static_cast<char>(static_cast<uint16_t>(group) & 0xFF));
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
    } while (false);

    do {
        puts(R"(-- valueOf("OPEFPS") + isEquity() .. isOther() --)");
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", cfi->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        int32_t isEquity = 0;
        int32_t isDebtInstrument = 0;
        int32_t isEntitlement = 0;
        int32_t isOption = 0;
        int32_t isFuture = 0;
        int32_t isOther = 0;

        result = dxfg_CFI_isEquity(isolateThread, cfi, &isEquity);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  isEquity = %d\n", isEquity);
        } else {
            get_exception(isolateThread);
        }

        result = dxfg_CFI_isDebtInstrument(isolateThread, cfi, &isDebtInstrument);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  isDebtInstrument = %d\n", isDebtInstrument);
        } else {
            get_exception(isolateThread);
        }

        result = dxfg_CFI_isEntitlement(isolateThread, cfi, &isEntitlement);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  isEntitlement = %d\n", isEntitlement);
        } else {
            get_exception(isolateThread);
        }

        result = dxfg_CFI_isOption(isolateThread, cfi, &isOption);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  isOption = %d\n", isOption);
        } else {
            get_exception(isolateThread);
        }

        result = dxfg_CFI_isFuture(isolateThread, cfi, &isFuture);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  isFuture = %d\n", isFuture);
        } else {
            get_exception(isolateThread);
        }

        result = dxfg_CFI_isOther(isolateThread, cfi, &isOther);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  isOther = %d\n", isOther);
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
    } while (false);

    do {
        puts(R"(-- valueOf("OPEFPS") + decipher() + Value_getCode() .. + getAttribute() --)");
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", cfi->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        dxfg_cfi_value_t *values = {};
        int32_t size = 0;
        result = dxfg_CFI_decipher(isolateThread, cfi, &values, &size);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            if (values != nullptr && size > 0) {
                printf("  values:\n");

                dxfg_cfi_value_t *cloneOfOptionsCfiValue = {};

                for (int32_t i = 0; i < size; i++) {
                    auto cfiValue = reinterpret_cast<dxfg_cfi_value_t *>(&values[i].handler);

                    int16_t code = {};
                    char *name = {};
                    char *description = {};
                    dxfg_CFI_Value_getCode(isolateThread, cfiValue, &code);
                    dxfg_CFI_Value_getName(isolateThread, cfiValue, &name);
                    dxfg_CFI_Value_getDescription(isolateThread, cfiValue, &description);

                    char charCode = static_cast<char>(static_cast<uint16_t>(code) & 0xFF);

                    if (charCode == 'O') {
                        dxfg_JavaObjectHandler_clone(
                            isolateThread, reinterpret_cast<dxfg_java_object_handler *>(cfiValue),
                            reinterpret_cast<dxfg_java_object_handler **>(&cloneOfOptionsCfiValue));
                    }

                    printf("    [%d] = {code = '%c', name = '%s', desc = '%s'}\n", i, charCode, name, description);

                    dxfg_String_release(isolateThread, name);
                    dxfg_String_release(isolateThread, description);
                }

                dxfg_JavaObjectHandler_array_release(isolateThread,
                                                     reinterpret_cast<const dxfg_java_object_handler **>(values), size);

                if (cloneOfOptionsCfiValue != nullptr) {
                    int16_t code = {};
                    char *name = {};
                    char *description = {};
                    dxfg_CFI_Value_getCode(isolateThread, cloneOfOptionsCfiValue, &code);
                    dxfg_CFI_Value_getName(isolateThread, cloneOfOptionsCfiValue, &name);
                    dxfg_CFI_Value_getDescription(isolateThread, cloneOfOptionsCfiValue, &description);

                    char charCode = static_cast<char>(static_cast<uint16_t>(code) & 0xFF);

                    printf("\n  CloneOfOptionsCfiValue{code = '%c', name = '%s', desc = '%s'}\n", charCode, name,
                           description);

                    dxfg_String_release(isolateThread, name);
                    dxfg_String_release(isolateThread, description);

                    dxfg_cfi_attribute_t *attribute = {};

                    dxfg_CFI_Value_getAttribute(isolateThread, cloneOfOptionsCfiValue, &attribute);

                    char *attributeName = {};
                    char *attributeDescription = {};

                    dxfg_cfi_value_t *attributeValues = {};
                    int32_t attributeValuesSize = 0;

                    dxfg_CFI_Attribute_getName(isolateThread, attribute, &attributeName);
                    dxfg_CFI_Attribute_getDescription(isolateThread, attribute, &attributeDescription);
                    dxfg_CFI_Attribute_getValues(isolateThread, attribute, &attributeValues, &attributeValuesSize);

                    printf("    attribute = {name = '%s', desc = '%s', values[%d]}\n", attributeName,
                           attributeDescription, attributeValuesSize);

                    dxfg_String_release(isolateThread, attributeName);
                    dxfg_String_release(isolateThread, attributeDescription);
                    dxfg_JavaObjectHandler_array_release(
                        isolateThread, reinterpret_cast<const dxfg_java_object_handler **>(attributeValues), size);
                    dxfg_JavaObjectHandler_release(isolateThread, &attribute->handler);
                    dxfg_JavaObjectHandler_release(isolateThread, &cloneOfOptionsCfiValue->handler);
                }
            }

        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
    } while (false);

    do {
        puts(R"(-- valueOf("OPEFPS") + describe() --)");
        dxfg_cfi_t *cfi{};
        auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  handle = %p\n", cfi->handler.java_object_handle);
        } else {
            get_exception(isolateThread);

            break;
        }

        char *description = {};
        result = dxfg_CFI_describe(isolateThread, cfi, &description);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf("  description = '%s'\n", description);

            dxfg_String_release(isolateThread, description);
        } else {
            get_exception(isolateThread);
        }

        dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
    } while (false);
}

void priceIncrementsCase(graal_isolatethread_t *isolateThread) {
    puts("== PriceIncrements ==");

    {
        puts("-- dxfg_PriceIncrements_EMPTY(..., !NULL) --");

        dxfg_price_increments_t *emptyPriceIncrements{};
        auto result = dxfg_PriceIncrements_EMPTY(isolateThread, &emptyPriceIncrements);

        printf("  dxfg_PriceIncrements_EMPTY() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", emptyPriceIncrements->handler.java_object_handle);
            dxfg_JavaObjectHandler_release(isolateThread, &emptyPriceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    }

    {
        puts("-- dxfg_PriceIncrements_EMPTY(..., NULL) --");

        auto result = dxfg_PriceIncrements_EMPTY(isolateThread, nullptr);

        printf("  dxfg_PriceIncrements_EMPTY() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            puts(", The result is successful, but there must be an error.");
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    }

    auto run_dxfg_PriceIncrements_valueOf = [](graal_isolatethread_t *isolateThread, const char *text) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_valueOf() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            char *text2 = {};
            dxfg_PriceIncrements_getText(isolateThread, priceIncrements, &text2);

            printf("  text = '%s'\n", text2);

            dxfg_String_release(isolateThread, text2);

            puts("  price increments:");

            double *increments = {};
            std::int32_t size = 0;

            dxfg_PriceIncrements_getPriceIncrements(isolateThread, priceIncrements, &increments, &size);

            for (std::int32_t i = 0; i < size; i++) {
                printf("    [%d] = %g\n", i, increments[i]);
            }

            dxfg_free(isolateThread, increments);
            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(
            R"(-- dxfg_PriceIncrements_valueOf(..., "0.01 20; 0.02 50; 0.05 100; 0.1 250; 0.25 500; 0.5 1000; 1 2500; 2.5", !NULL) --)");
        run_dxfg_PriceIncrements_valueOf(isolateThread,
                                         "0.01 20; 0.02 50; 0.05 100; 0.1 250; 0.25 500; 0.5 1000; 1 2500; 2.5");
    }

    {
        puts(R"(-- dxfg_PriceIncrements_valueOf(..., "0.0001 1; 0.01", !NULL) --)");
        run_dxfg_PriceIncrements_valueOf(isolateThread, "0.0001 1; 0.01");
    }

    {
        puts(R"(-- dxfg_PriceIncrements_valueOf(..., "0.01", !NULL) --)");
        run_dxfg_PriceIncrements_valueOf(isolateThread, "0.01");
    }

    {
        puts(R"(-- dxfg_PriceIncrements_valueOf(..., "", !NULL) --)");
        run_dxfg_PriceIncrements_valueOf(isolateThread, "");
    }

    {
        puts("-- dxfg_PriceIncrements_valueOf(..., NULL, !NULL) --");
        run_dxfg_PriceIncrements_valueOf(isolateThread, nullptr);
    }

    auto run_dxfg_PriceIncrements_valueOf2 = [](graal_isolatethread_t *isolateThread, double increment) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf2(isolateThread, increment, &priceIncrements);

        printf("  dxfg_PriceIncrements_valueOf2() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            char *text = {};
            dxfg_PriceIncrements_getText(isolateThread, priceIncrements, &text);

            printf("  text = '%s'\n", text);

            dxfg_String_release(isolateThread, text);

            puts("  price increments:");

            double *increments = {};
            std::int32_t size = 0;

            dxfg_PriceIncrements_getPriceIncrements(isolateThread, priceIncrements, &increments, &size);

            for (std::int32_t i = 0; i < size; i++) {
                printf("    [%d] = %g\n", i, increments[i]);
            }

            dxfg_free(isolateThread, increments);
            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- dxfg_PriceIncrements_valueOf2(..., 0.01, ...) --)");
        run_dxfg_PriceIncrements_valueOf2(isolateThread, 0.01);
    }

    {
        puts(R"(-- dxfg_PriceIncrements_valueOf2(..., -0.01, ...) --)");
        run_dxfg_PriceIncrements_valueOf2(isolateThread, -0.01);
    }

    {
        puts(R"(-- dxfg_PriceIncrements_valueOf2(..., 0.0, ...) --)");
        run_dxfg_PriceIncrements_valueOf2(isolateThread, 0.0);
    }

    {
        puts(R"(-- dxfg_PriceIncrements_valueOf2(..., NAN, ...) --)");
        run_dxfg_PriceIncrements_valueOf2(isolateThread, NAN);
    }

    {
        puts(R"(-- dxfg_PriceIncrements_valueOf2(..., INFINITY, ...) --)");
        run_dxfg_PriceIncrements_valueOf2(isolateThread, INFINITY);
    }

    auto run_dxfg_PriceIncrements_valueOf3 = [](graal_isolatethread_t *isolateThread, const double *increments,
                                                int32_t size) {
        if (increments == nullptr) {
            return;
        }

        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf3(isolateThread, increments, size, &priceIncrements);

        printf("  dxfg_PriceIncrements_valueOf3() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            char *text = {};
            dxfg_PriceIncrements_getText(isolateThread, priceIncrements, &text);

            printf("  text = '%s'\n", text);

            dxfg_String_release(isolateThread, text);

            puts("  price increments:");

            double *increments2 = {};
            std::int32_t size2 = 0;

            dxfg_PriceIncrements_getPriceIncrements(isolateThread, priceIncrements, &increments2, &size2);

            for (std::int32_t i = 0; i < size; i++) {
                printf("    [%d] = %g\n", i, increments[i]);
            }

            dxfg_free(isolateThread, increments2);
            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        std::vector<double> increments = {0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5};

        {
            puts(
                R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], ...) --)");
            run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(),
                                              static_cast<int32_t>(increments.size()));
        }

        {
            puts(
                R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], 1 ...) --)");
            run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(), 1);
        }

        {
            puts(
                R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], 2 ...) --)");
            run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(), 2);
        }

        {
            puts(
                R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], 3 ...) --)");
            run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(), 3);
        }

        {
            puts(
                R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], 0 ...) --)");
            run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(), 0);
        }

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf3(..., NULL, 0 ...) --)");
            run_dxfg_PriceIncrements_valueOf3(isolateThread, nullptr, 0);
        }
    }

    auto run_dxfg_PriceIncrements_getPriceIncrement = [](graal_isolatethread_t *isolateThread, const char *text) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_getPriceIncrement() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            double priceIncrement = 0.0;

            dxfg_PriceIncrements_getPriceIncrement(isolateThread, priceIncrements, &priceIncrement);

            printf("  priceIncrement = %g\n", priceIncrement);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement() --)");
        run_dxfg_PriceIncrements_getPriceIncrement(isolateThread, "0.0001 1; 0.01");
    }

    {
        puts(R"(-- valueOf(..., "", ...).getPriceIncrement() --)");
        run_dxfg_PriceIncrements_getPriceIncrement(isolateThread, "");
    }

    auto run_dxfg_PriceIncrements_getPriceIncrement2 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                          double price) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_getPriceIncrement2() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            double priceIncrement = 0.0;

            dxfg_PriceIncrements_getPriceIncrement2(isolateThread, priceIncrements, price, &priceIncrement);

            printf("  priceIncrement = %g\n", priceIncrement);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement2(23.42) --)");
        run_dxfg_PriceIncrements_getPriceIncrement2(isolateThread, "0.0001 1; 0.01", 23.42);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement2(1.01) --)");
        run_dxfg_PriceIncrements_getPriceIncrement2(isolateThread, "0.0001 1; 0.01", 1.01);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement2(1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement2(isolateThread, "0.0001 1; 0.01", 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement2(0.005) --)");
        run_dxfg_PriceIncrements_getPriceIncrement2(isolateThread, "0.0001 1; 0.01", 0.005);
    }

    auto run_dxfg_PriceIncrements_getPriceIncrement3 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                          double price, int32_t direction) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_getPriceIncrement3() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            double priceIncrement = 0.0;

            dxfg_PriceIncrements_getPriceIncrement3(isolateThread, priceIncrements, price, direction, &priceIncrement);

            printf("  priceIncrement = %g\n", priceIncrement);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(23.42, 1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 23.42, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(1.01, 1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 1.01, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(1, 1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 1, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(0.005, 1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 0.005, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(23.42, -1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 23.42, -1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(1.01, -1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 1.01, -1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(1, -1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 1, -1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(0.005, -1) --)");
        run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 0.005, -1);
    }

    auto run_dxfg_PriceIncrements_getPricePrecision = [](graal_isolatethread_t *isolateThread, const char *text) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_getPricePrecision() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            int32_t pricePrecision = 0;

            dxfg_PriceIncrements_getPricePrecision(isolateThread, priceIncrements, &pricePrecision);

            printf("  pricePrecision = %d\n", pricePrecision);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPricePrecision() --)");
        run_dxfg_PriceIncrements_getPricePrecision(isolateThread, "0.0001 1; 0.01");
    }

    {
        puts(R"(-- valueOf(..., "0.01", ...).getPricePrecision() --)");
        run_dxfg_PriceIncrements_getPricePrecision(isolateThread, "0.01");
    }

    {
        puts(R"(-- valueOf(..., "", ...).getPricePrecision() --)");
        run_dxfg_PriceIncrements_getPricePrecision(isolateThread, "");
    }

    auto run_dxfg_PriceIncrements_getPricePrecision2 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                          double price) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_getPricePrecision2() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            int32_t pricePrecision = 0;

            dxfg_PriceIncrements_getPricePrecision2(isolateThread, priceIncrements, price, &pricePrecision);

            printf("  pricePrecision = %d\n", pricePrecision);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPricePrecision2(0.1) --)");
        run_dxfg_PriceIncrements_getPricePrecision2(isolateThread, "0.0001 1; 0.01", 0.1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPricePrecision2(2) --)");
        run_dxfg_PriceIncrements_getPricePrecision2(isolateThread, "0.0001 1; 0.01", 2);
    }

    auto run_dxfg_PriceIncrements_roundPrice = [](graal_isolatethread_t *isolateThread, const char *text,
                                                  double price) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_roundPrice() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            double roundedPrice = 0.0;

            dxfg_PriceIncrements_roundPrice(isolateThread, priceIncrements, price, &roundedPrice);

            printf("  roundedPrice = %g\n", roundedPrice);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice(23.426) --)");
        run_dxfg_PriceIncrements_roundPrice(isolateThread, "0.0001 1; 0.01", 23.426);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice(1.0178) --)");
        run_dxfg_PriceIncrements_roundPrice(isolateThread, "0.0001 1; 0.01", 1.0178);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice(0.013321) --)");
        run_dxfg_PriceIncrements_roundPrice(isolateThread, "0.0001 1; 0.01", 0.013321);
    }

    auto run_dxfg_PriceIncrements_roundPrice2 = [](graal_isolatethread_t *isolateThread, const char *text, double price,
                                                   int32_t direction) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_roundPrice2() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            double roundedPrice = 0.0;

            dxfg_PriceIncrements_roundPrice2(isolateThread, priceIncrements, price, direction, &roundedPrice);

            printf("  roundedPrice = %g\n", roundedPrice);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(23.426, 1) --)");
        run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 23.426, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(1.0178, 1) --)");
        run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 1.0178, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(0.013321, 1) --)");
        run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 0.013321, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(23.426, -1) --)");
        run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 23.426, -1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(1.0178, -1) --)");
        run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 1.0178, -1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(0.013321, -1) --)");
        run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 0.013321, -1);
    }

    auto run_dxfg_PriceIncrements_roundPrice3 = [](graal_isolatethread_t *isolateThread, const char *text, double price,
                                                   dxfg_rounding_mode_t roundingMode) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_roundPrice3() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            double roundedPrice = 0.0;

            dxfg_PriceIncrements_roundPrice3(isolateThread, priceIncrements, price, roundingMode, &roundedPrice);

            printf("  roundedPrice = %g\n", roundedPrice);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(23.426, DXFG_ROUNDING_MODE_HALF_UP) --)");
        run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 23.426, DXFG_ROUNDING_MODE_HALF_UP);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(1.0178, DXFG_ROUNDING_MODE_DOWN) --)");
        run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 1.0178, DXFG_ROUNDING_MODE_DOWN);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(0.013321, DXFG_ROUNDING_MODE_UP) --)");
        run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 0.013321, DXFG_ROUNDING_MODE_UP);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(23.426, DXFG_ROUNDING_MODE_FLOOR) --)");
        run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 23.426, DXFG_ROUNDING_MODE_FLOOR);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(1.0178, DXFG_ROUNDING_MODE_CEILING) --)");
        run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 1.0178, DXFG_ROUNDING_MODE_CEILING);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(0.013321, DXFG_ROUNDING_MODE_HALF_EVEN) --)");
        run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 0.013321, DXFG_ROUNDING_MODE_HALF_EVEN);
    }

    auto run_dxfg_PriceIncrements_incrementPrice = [](graal_isolatethread_t *isolateThread, const char *text,
                                                      double price, int32_t direction) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_incrementPrice() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            double incrementedPrice = 0.0;

            dxfg_PriceIncrements_incrementPrice(isolateThread, priceIncrements, price, direction, &incrementedPrice);

            printf("  incrementedPrice = %g\n", incrementedPrice);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(23.426, 1) --)");
        run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 23.426, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(1.0178, 1) --)");
        run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 1.0178, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(0.013321, 1) --)");
        run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 0.013321, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(23.426, -1) --)");
        run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 23.426, -1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(1.0178, -1) --)");
        run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 1.0178, -1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(0.013321, -1) --)");
        run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 0.013321, -1);
    }

    auto run_dxfg_PriceIncrements_incrementPrice2 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                       double price, int32_t direction, double step) {
        dxfg_price_increments_t *priceIncrements{};
        auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

        printf("  dxfg_PriceIncrements_incrementPrice2() -> %d", result);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            printf(", %p\n", priceIncrements->handler.java_object_handle);

            double incrementedPrice = 0.0;

            dxfg_PriceIncrements_incrementPrice2(isolateThread, priceIncrements, price, direction, step,
                                                 &incrementedPrice);

            printf("  incrementedPrice = %g\n", incrementedPrice);

            dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
        } else {
            putchar('\n');
            get_exception(isolateThread);
        }
    };

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(23.426, 1, 0.5) --)");
        run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 23.426, 1, 0.5);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(1.0178, 1, 1) --)");
        run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 1.0178, 1, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(0.013321, 1, 2) --)");
        run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 0.013321, 1, 2);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(23.426, -1, 1) --)");
        run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 23.426, -1, 1);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(1.0178, -1, 2) --)");
        run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 1.0178, -1, 2);
    }

    {
        puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(0.013321, -1, 10) --)");
        run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 0.013321, -1, 10);
    }
}

int main(int argc, char *argv[]) {
    parseArgs(argc, argv);

    if (graal_create_isolate(nullptr, &maintIsolate, &mainIsolateThread) != 0) {
        get_exception(mainIsolateThread);
        exit(-1);
    }

    get_exception(mainIsolateThread); // to init com.dxfeed.sdk.NativeUtils

    auto defaultHost = "demo.dxfeed.com";
    auto defaultPort = 7300;
    auto defaultAddress = defaultHost + ":"s + std::to_string(defaultPort);
    auto defaultWsAddress = "dxlink:wss://"s + defaultHost + "/dxlink-ws"s;
    auto defaultOnDemandPort = 7680;
    auto defaultOnDemandAddress = "(ondemand:"s + defaultHost + ":"s + std::to_string(defaultOnDemandPort) + ")"s;
    auto demoUser = "demo";
    auto demoPassword = "demo";
    auto defaultIpfAddress = "https://"s + demoUser + ":" + demoPassword + "@" + defaultHost + "/ipf"s;
    auto defaultIpfFilePath = "../../../../../ipf.txt";

    txIndexedEventModel(mainIsolateThread, defaultAddress.c_str());

    const char *data[] = {"Connect", defaultAddress.c_str(), "Quote", "AAPL"};
    dxfg_string_list args{};
    args.size = 4;
    args.elements = data;

    // TODO: timeout
    dxfg_Tools_main(mainIsolateThread, &args);

    dxfg_string_list *symbols = dxfg_Tools_parseSymbols(mainIsolateThread, "Quote,Trade");

    dxfg_CList_String_release(mainIsolateThread, symbols);
    liveIpf(mainIsolateThread, defaultIpfAddress.c_str());
    readerIpf(mainIsolateThread, defaultIpfFilePath);
    finalizeListener(mainIsolateThread, defaultAddress.c_str());
    executorBaseOnConcurrentLinkedQueue(mainIsolateThread, defaultAddress.c_str());
    dxEndpointSubscription(mainIsolateThread, defaultAddress.c_str());
    dxEndpointMonitoring(mainIsolateThread, defaultAddress.c_str());
    dxEndpointTimeSeriesSubscription(mainIsolateThread, defaultAddress.c_str());
    systemProperties(mainIsolateThread);
    exception(mainIsolateThread);
    orderBookModel(mainIsolateThread, defaultAddress.c_str());
    indexedEventModel(mainIsolateThread, defaultAddress.c_str());
    promise(mainIsolateThread, defaultAddress.c_str());
    lastEventIfSubscribed(mainIsolateThread, defaultAddress.c_str());
    promisesAllOf(mainIsolateThread, defaultAddress.c_str(), Args::symbols);
    indexedEventsPromise(mainIsolateThread, defaultAddress.c_str());
    getLastEvent(mainIsolateThread, defaultAddress.c_str());
    schedule(mainIsolateThread, defaultIpfFilePath);
    schedule2(mainIsolateThread);
    dxLink(mainIsolateThread, defaultWsAddress.c_str());
    onDemandService(mainIsolateThread, demoUser, demoPassword, defaultOnDemandAddress.c_str());
    additionalUnderlyingsCase(mainIsolateThread);
    cfiCase(mainIsolateThread);
    priceIncrementsCase(mainIsolateThread);
}
