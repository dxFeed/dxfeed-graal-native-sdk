// SPDX-License-Identifier: MPL-2.0

#include "CommandLineParser.hpp"
#include <DxfgEventFormatter/QuoteFormatter.hpp>
#include <DxfgEventFormatter/TimeAndSaleFormatter.hpp>
#include <dxfg_api.h>
#include <iostream>
#include <unistd.h>

using namespace dxfg;

static graal_isolate_t *isolate = nullptr;
static graal_isolatethread_t *thread = nullptr;
static std::unordered_map<std::string, std::string> properties{};
static std::string address{};
static std::vector<dxfg_event_clazz_t> eventTypes{};
static std::vector<std::string> symbols{};
static bool isQuiteMode = false;

void printException(graal_isolatethread_t *thread);
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

    int currentArg = 1;
    try {
        properties = CommandLineParser::parseSystemProperties(argv, currentArg);
        address = CommandLineParser::parseAddress(argv, currentArg);
        eventTypes = CommandLineParser::parseEventTypes(argv, currentArg);
        symbols = CommandLineParser::parseSymbols(argv, currentArg);
        while (currentArg != argc) {
            if (std::string(argv[currentArg]) == "-q") {
                isQuiteMode = true;
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
void printEvent(const dxfg_event_type_t *pEvent) {
    if (pEvent->clazz == DXFG_EVENT_QUOTE) {
        auto *quote = (dxfg_quote_t *)pEvent;
        printf(
            "C: QUOTE{event_symbol=%s, event_time=%lld, time_millis_sequence=%d, time_nano_part=%d, bid_time=%lld, bid_exchange_code=%d, bid_price=%E, bid_size=%E, ask_time=%lld, ask_exchange_code=%hd, ask_price=%f, ask_size=%E}\n",
            quote->market_event.event_symbol,
            quote->market_event.event_time,
            quote->time_millis_sequence,
            quote->time_nano_part,
            quote->bid_time,
            quote->bid_exchange_code,
            quote->bid_price,
            quote->bid_size,
            quote->ask_time,
            quote->ask_exchange_code,
            quote->ask_price,
            quote->ask_size
        );
    } else if (pEvent->clazz == DXFG_EVENT_SERIES) {
        auto *event = (dxfg_series_t *)pEvent;
        printf(
            "C: SERIES{event_symbol=%s, index=%lld, volatility=%f}\n",
            event->market_event.event_symbol,
            event->index,
            event->volatility
        );
    } else if (pEvent->clazz == DXFG_EVENT_OPTION_SALE) {
        auto *event = (dxfg_option_sale_t *)pEvent;
        printf(
            "C: OPTION_SALE{event_symbol=%s, index=%lld, volatility=%f, option_symbol=%s}\n",
            event->market_event.event_symbol,
            event->index,
            event->volatility,
            event->option_symbol
        );
    } else if (pEvent->clazz == DXFG_EVENT_TIME_AND_SALE) {
        auto *time_and_sale = (dxfg_time_and_sale_t *)pEvent;
        printf(
            "C: TIME_AND_SALE{event_symbol=%s, bid_price=%f, exchange_sale_conditions=%s, buyer=%s, seller=%s}\n",
            time_and_sale->market_event.event_symbol,
            time_and_sale->bid_price,
            time_and_sale->exchange_sale_conditions,
            time_and_sale->buyer,
            time_and_sale->seller
        );
    } else if (pEvent->clazz == DXFG_EVENT_SPREAD_ORDER) {
        auto *event = (dxfg_spread_order_t *)pEvent;
        printf(
            "C: SPREAD_ORDER{order_base.count=%lld, spread_symbol=%s}\n",
            event->order_base.count,
            event->spread_symbol
        );
    } else if (pEvent->clazz == DXFG_EVENT_ORDER) {
        auto *event = (dxfg_order_t *)pEvent;
        dxfg_indexed_event_source_t* source = dxfg_IndexedEvent_getSource(thread, &event->order_base.market_event.event_type);
        printf(
            "C: Order{%s, source=%s, price=%f, size=%f, marketMaker='%s'}\n",
            event->order_base.market_event.event_symbol,
            source->name,
            event->order_base.price,
            event->order_base.size,
            event->market_maker
        );
        dxfg_IndexedEventSource_release(thread, source);
    } else if (pEvent->clazz == DXFG_EVENT_ANALYTIC_ORDER) {
        auto *event = (dxfg_analytic_order_t *)pEvent;
        printf(
            "C: ANALYTIC_ORDER{order_base.count=%lld, iceberg_peak_size=%f}\n",
            event->order_base.order_base.count,
            event->iceberg_peak_size
        );
    } else if (pEvent->clazz == DXFG_EVENT_MESSAGE) {
        auto *event = (dxfg_message_t *)pEvent;
        printf(
            "C: MESSAGE{event_symbol=%s, event_time=%lld}\n",
            event->event_symbol,
            event->event_time
        );
    } else if (pEvent->clazz == DXFG_EVENT_ORDER_BASE) {
        auto *event = (dxfg_order_base_t *)pEvent;
        printf(
            "C: ORDER_BASE{size=%f, index=%lld}\n",
            event->size,
            event->index
        );
    } else if (pEvent->clazz == DXFG_EVENT_CONFIGURATION) {
        auto *event = (dxfg_configuration_t *)pEvent;
        printf(
            "C: CONFIGURATION{event_symbol=%s, version=%d}\n",
            event->event_symbol,
            event->version
        );
    } else if (pEvent->clazz == DXFG_EVENT_TRADE) {
        auto *event = (dxfg_trade_t *)pEvent;
        printf(
            "C: TRADE{trade_base.size=%f, trade_base.price=%f}\n",
            event->trade_base.size,
            event->trade_base.price
        );
    } else if (pEvent->clazz == DXFG_EVENT_TRADE_ETH) {
        auto *event = (dxfg_trade_eth_t *)pEvent;
        printf(
            "C: TRADE_ETH{trade_base.size=%f, trade_base.price=%f}\n",
            event->trade_base.size,
            event->trade_base.price
        );
    } else if (pEvent->clazz == DXFG_EVENT_THEO_PRICE) {
        auto *event = (dxfg_theo_price_t *)pEvent;
        printf(
            "C: THEO_PRICE{index=%lld, price=%f}\n",
            event->index,
            event->price
        );
    } else if (pEvent->clazz == DXFG_EVENT_UNDERLYING) {
        auto *event = (dxfg_underlying_t *)pEvent;
        printf(
            "C: UNDERLYING{index=%lld, volatility=%f}\n",
            event->index,
            event->volatility
        );
    } else if (pEvent->clazz == DXFG_EVENT_GREEKS) {
        auto *event = (dxfg_greeks_t *)pEvent;
        printf(
            "C: GREEKS{index=%lld, volatility=%f}\n",
            event->index,
            event->volatility
        );
    } else if (pEvent->clazz == DXFG_EVENT_SUMMARY) {
        auto *event = (dxfg_summary_t *)pEvent;
        printf(
            "C: SUMMARY{day_id=%d, day_open_price=%f}\n",
            event->day_id,
            event->day_open_price
        );
    } else if (pEvent->clazz == DXFG_EVENT_PROFILE) {
        auto *event = (dxfg_profile_t *)pEvent;
        printf(
            "C: PROFILE{description=%s, status_reason=%s, high_52_week_price=%f}\n",
            event->description,
            event->status_reason,
            event->high_52_week_price
        );
    } else if (pEvent->clazz == DXFG_EVENT_CANDLE) {
        auto *event = (dxfg_candle_t *)pEvent;
        printf(
            "C: CANDLE{symbol=%s, index=%lld, ask_volume=%E, volume=%f, event_time=%lld}\n",
            event->event_symbol,
            event->index,
            event->ask_volume,
            event->volume,
            event->event_time
        );
    } else if (pEvent->clazz == DXFG_EVENT_DAILY_CANDLE) {
        auto *event = (dxfg_daily_candle_t *)pEvent;
        printf(
            "C: DAILY_CANDLE{symbol=%s, index=%lld, ask_volume=%E}\n",
            event->candle.event_symbol,
            event->candle.index,
            event->candle.ask_volume
        );
    } else {
        printf(
            "C: %u{}\n",
            pEvent->clazz
        );
    }
}

void c_print(graal_isolatethread_t *thread, dxfg_event_type_list *events, void *user_data) {
    for (int i = 0; i < events->size; ++i) {
        printEvent(events->elements[i]);
    }
}

void c_promise_func(graal_isolatethread_t *thread, dxfg_promise_t *promise, void *user_data) {
    dxfg_event_type_t* event = dxfg_Promise_EventType_getResult(thread, reinterpret_cast<dxfg_promise_event_t *>(promise));
    printf("********************* -> c_promise_func\n");
    printEvent(event);
    printf("********************* -> c_promise_func\n");
}

void c_listener_func(graal_isolatethread_t *thread, dxfg_order_book_model_t* model, void *user_data) {
    printf("Buy orders:\n");
    dxfg_observable_list_model_t* buyObservableListOrders = dxfg_OrderBookModel_getBuyOrders(thread, model);
    dxfg_event_type_list* buyOrders = dxfg_ObservableListModel_toArray(thread, buyObservableListOrders);
    for (int i = 0; i < buyOrders->size; ++i) {
        printEvent(reinterpret_cast<dxfg_event_type_t *>(buyOrders->elements[0]));
    }
    dxfg_CList_EventType_release(thread, buyOrders);
    dxfg_JavaObjectHandler_release(thread, &buyObservableListOrders->handler);

    printf("Sell orders:\n");
    dxfg_observable_list_model_t* sellObservableListOrders = dxfg_OrderBookModel_getSellOrders(thread, model);
    dxfg_event_type_list* sellOrders = dxfg_ObservableListModel_toArray(thread, sellObservableListOrders);
    for (int i = 0; i < sellOrders->size; ++i) {
        printEvent(reinterpret_cast<dxfg_event_type_t *>(sellOrders->elements[0]));
    }
    dxfg_CList_EventType_release(thread, sellOrders);
    dxfg_JavaObjectHandler_release(thread, &sellObservableListOrders->handler);

    printf("\n");
}

void c_observable_list_listener_func(graal_isolatethread_t *thread, dxfg_event_type_list* orders, void *user_data) {
    printf("c_observable_list_listener_func:\n");
    for (int i = 0; i < orders->size; ++i) {
        printEvent(reinterpret_cast<dxfg_event_type_t *>(orders->elements[0]));
    }
}

void print_exception(graal_isolatethread_t *thread) {
    dxfg_exception_t* exception = dxfg_get_and_clear_thread_exception_t(thread);
    if (exception) {
        printf("C: %s\n", exception->stackTrace);
        dxfg_Exception_release(thread, exception);
    }
}

void endpoint_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_state_t old_state,
                                    dxfg_endpoint_state_t new_state, void *user_data) {
    printf("C: state %d -> %d\n", old_state, new_state);
}

void updateListener(graal_isolatethread_t *thread, dxfg_iterable_ip_t *profiles, void *user_data) {
    while (dxfg_Iterable_InstrumentProfile_hasNext(thread, profiles) == 1) {
        dxfg_instrument_profile_t* profile = dxfg_Iterable_InstrumentProfile_next(thread, profiles);
        if (!profile) {
            print_exception(thread);
        }
        printf("C: profile %s\n", profile->symbol);
        dxfg_InstrumentProfile_release(thread, profile);
    }
}

void stateChangeListener(graal_isolatethread_t *thread,
                         dxfg_ipf_connection_state_t old_state,
                         dxfg_ipf_connection_state_t new_state,
                         void *user_data) {
    printf("C: IPF stateChangeListener %d -> %d\n", old_state, new_state);
}

void finalize(graal_isolatethread_t *thread, void *userData) {
    printf("C: finalize\n");
}

void liveIpf(graal_isolatethread_t *thread) {
    printf("C: liveIpf BEGIN\n");
    dxfg_ipf_collector_t* collector = dxfg_InstrumentProfileCollector_new(thread);
    dxfg_ipf_connection_t* connection = dxfg_InstrumentProfileConnection_createConnection(
        thread,
        "https://demo:demo@tools.dxfeed.com/ipf",
        collector
    );
    dxfg_ipf_update_listener_t* listener = dxfg_InstrumentProfileUpdateListener_new(thread, &updateListener, nullptr);
    dxfg_Object_finalize(thread, listener, finalize, nullptr);
    dxfg_InstrumentProfileCollector_addUpdateListener(thread, collector, listener);
    dxfg_ipf_connection_state_change_listener_t* listenerState = dxfg_IpfPropertyChangeListener_new(thread, &stateChangeListener, nullptr);
    dxfg_Object_finalize(thread, listenerState, finalize, nullptr);
    dxfg_InstrumentProfileConnection_addStateChangeListener(thread, connection, listenerState);
    dxfg_InstrumentProfileConnection_start(thread, connection);
    dxfg_InstrumentProfileConnection_waitUntilCompleted(thread, connection, 30000);
    dxfg_InstrumentProfileConnection_close(thread, connection);
    dxfg_JavaObjectHandler_release(thread, &collector->handler);
    dxfg_JavaObjectHandler_release(thread, &connection->handler);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &listenerState->handler);
    printf("C: liveIpf END\n");
}

void readerIpf(graal_isolatethread_t *thread) {
    printf("C: readerIpf BEGIN\n");
    dxfg_instrument_profile_reader_t* reader = dxfg_InstrumentProfileReader_new(thread);
    dxfg_instrument_profile_list* profiles = dxfg_InstrumentProfileReader_readFromFile(
        thread, reader, "../../../../../ipf.txt"
    );
    for (int i = 0; i < profiles->size; ++i) {
        printf("C: profile %s\n", profiles->elements[i]->symbol);
    }
    dxfg_CList_InstrumentProfile_release(thread, profiles);
    dxfg_JavaObjectHandler_release(thread, &reader->handler);
    printf("C: readerIpf END\n");
}

void finalizeListener(graal_isolatethread_t *thread) {
    printf("C: finalizeListener BEGIN\n");
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_endpoint_state_change_listener_t* stateListener = dxfg_PropertyChangeListener_new(thread, endpoint_state_change_listener, nullptr);
    dxfg_Object_finalize(thread, stateListener, finalize, nullptr);
    dxfg_DXEndpoint_addStateChangeListener(thread, endpoint, stateListener);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_subscription_t* subscriptionQuote = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &c_print, nullptr);
    dxfg_Object_finalize(thread, listener, finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(thread,subscriptionQuote, listener);
    printf("C: get_state = %d\n", dxfg_DXEndpoint_getState(thread, endpoint));
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &stateListener->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
    dxfg_gc(thread);
    usleep(2000000);
    dxfg_gc(thread);
    usleep(2000000);
    printf("C: finalizeListener END\n");
}

void executorBaseOnConcurrentLinkedQueue(graal_isolatethread_t *thread) {
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_executor_t* executor = dxfg_ExecutorBaseOnConcurrentLinkedQueue_new(thread);
    dxfg_DXEndpoint_executor(thread, endpoint, executor);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_subscription_t* subscriptionQuote = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &c_print, nullptr);
    dxfg_Object_finalize(thread, listener, finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(thread,subscriptionQuote, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(thread, subscriptionQuote, &symbolAAPL.supper);
    usleep(2000000);
    dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(thread, executor);
    usleep(2000000);
    dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(thread, executor);
    usleep(2000000);
    dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(thread, executor);
    usleep(2000000);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
    dxfg_JavaObjectHandler_release(thread, &executor->handler);
}

void dxEndpointSubscription(graal_isolatethread_t *thread) {
    printf("C: dxEndpointSubscription BEGIN\n");
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_subscription_t* subscriptionQuote = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &c_print, nullptr);
    dxfg_Object_finalize(thread, listener, finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(thread,subscriptionQuote, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(thread, subscriptionQuote, &symbolAAPL.supper);
    int containQuote = dxfg_DXFeedSubscription_containsEventType(thread, subscriptionQuote, DXFG_EVENT_QUOTE);
    int containCandle = dxfg_DXFeedSubscription_containsEventType(thread, subscriptionQuote, DXFG_EVENT_CANDLE);
    usleep(2000000);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
    printf("C: dxEndpointSubscription END\n");
}

void dxEndpointTimeSeriesSubscription(graal_isolatethread_t *thread) {
    printf("C: dxEndpointTimeSeriesSubscription BEGIN\n");
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_event_clazz_list_t* event_clazz_list = dxfg_DXEndpoint_getEventTypes(thread, endpoint);
    dxfg_time_series_subscription_t* subscriptionTaS = dxfg_DXFeed_createTimeSeriesSubscription2(thread, feed, event_clazz_list);
    dxfg_CList_EventClazz_release(thread, event_clazz_list);
    dxfg_DXFeedTimeSeriesSubscription_setFromTime(thread, subscriptionTaS, 0);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &c_print, nullptr);
    dxfg_Object_finalize(thread, listener, finalize, nullptr);
    dxfg_DXFeedSubscription_addEventListener(thread, &subscriptionTaS->sub, listener);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(thread, &subscriptionTaS->sub, &symbolAAPL.supper);
    usleep(2000000);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &subscriptionTaS->sub.handler);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
    printf("C: dxEndpointTimeSeriesSubscription END\n");
}

void systemProperties(graal_isolatethread_t *thread) {
    const char *propertyName = "property-name";
    const char *string = dxfg_system_get_property(thread, propertyName);
    printf("\"%s\" must not be equal to \"property-value\"\n", string);
    dxfg_String_release(thread, string);
    dxfg_system_set_property(thread, "property-name", "property-value");
    const char *value = dxfg_system_get_property(thread, propertyName);
    printf("\"%s\" must be equal to \"property-value\"\n", value);
    dxfg_String_release(thread, value);
}

void exception(graal_isolatethread_t *thread) {
    dxfg_java_object_handler* object = dxfg_throw_exception(thread);
    if (!object) {
        printException(thread);
    }
    dxfg_JavaObjectHandler_release(thread, object);
}

void printException(graal_isolatethread_t *thread) {
    dxfg_exception_t* exception = dxfg_get_and_clear_thread_exception_t(thread);
    if (exception) {
        printf("C: %s\n", exception->stackTrace);
    }
    dxfg_Exception_release(thread, exception);
}

void orderBookModel(graal_isolatethread_t *thread) {
    dxfg_order_book_model_t* order_book_model = dxfg_OrderBookModel_new(thread);
    dxfg_OrderBookModel_setFilter(thread, order_book_model, ALL);
    dxfg_OrderBookModel_setSymbol(thread, order_book_model, "AAPL");
    dxfg_observable_list_model_t* buyOrders = dxfg_OrderBookModel_getBuyOrders(thread, order_book_model);
    dxfg_observable_list_model_listener_t *buyOrdersListener = dxfg_ObservableListModelListener_new(thread, &c_observable_list_listener_func, nullptr);
    dxfg_Object_finalize(thread, buyOrdersListener, &finalize, nullptr);
    dxfg_ObservableListModel_addListener(thread, buyOrders, buyOrdersListener);
    dxfg_observable_list_model_t* sellOrders = dxfg_OrderBookModel_getSellOrders(thread, order_book_model);
    dxfg_observable_list_model_listener_t *sellOrdersListener = dxfg_ObservableListModelListener_new(thread, &c_observable_list_listener_func, nullptr);
    dxfg_Object_finalize(thread, sellOrdersListener, &finalize, nullptr);
    dxfg_ObservableListModel_addListener(thread, sellOrders, sellOrdersListener);
    dxfg_order_book_model_listener_t* listener = dxfg_OrderBookModelListener_new(thread, &c_listener_func, nullptr);
    dxfg_Object_finalize(thread, listener, &finalize, nullptr);
    dxfg_OrderBookModel_addListener(thread, order_book_model, listener);
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_OrderBookModel_attach(thread, order_book_model, feed);
    usleep(2000000);
    dxfg_OrderBookModel_close(thread, order_book_model);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &order_book_model->handler);
    dxfg_JavaObjectHandler_release(thread, &buyOrders->handler);
    dxfg_JavaObjectHandler_release(thread, &buyOrdersListener->handler);
    dxfg_JavaObjectHandler_release(thread, &sellOrders->handler);
    dxfg_JavaObjectHandler_release(thread, &sellOrdersListener->handler);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
}

void indexedEventModel(graal_isolatethread_t *thread) {
    dxfg_indexed_event_model_t* indexed_event_model = dxfg_IndexedEventModel_new(thread, DXFG_EVENT_TIME_AND_SALE);
    dxfg_IndexedEventModel_setSizeLimit(thread, indexed_event_model, 30);
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_IndexedEventModel_attach(thread, indexed_event_model, feed);
    dxfg_observable_list_model_t* observable_list_model = dxfg_IndexedEventModel_getEventsList(thread, indexed_event_model);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_IndexedEventModel_setSymbol(thread, indexed_event_model, &symbolAAPL.supper);
    dxfg_observable_list_model_listener_t *observable_list_model_listener = dxfg_ObservableListModelListener_new(thread, &c_observable_list_listener_func, nullptr);
    dxfg_Object_finalize(thread, observable_list_model_listener, &finalize, nullptr);
    dxfg_ObservableListModel_addListener(thread, observable_list_model, observable_list_model_listener);
    usleep(2000000);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &indexed_event_model->handler);
    dxfg_JavaObjectHandler_release(thread, &observable_list_model->handler);
    dxfg_JavaObjectHandler_release(thread, &observable_list_model_listener->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
}

void promise(graal_isolatethread_t *thread) {
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_candle_symbol_t candleSymbol;
    candleSymbol.supper.type = CANDLE;
    candleSymbol.symbol = "AAPL";
    dxfg_promise_event_t* candlePromise = dxfg_DXFeed_getLastEventPromise(thread, feed, DXFG_EVENT_CANDLE, &candleSymbol.supper);
    dxfg_Promise_whenDone(thread, reinterpret_cast<dxfg_promise_t *>(candlePromise), &c_promise_func, nullptr);
    usleep(2000000);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &candlePromise->handler.handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
}

void lastEventIfSubscribed(graal_isolatethread_t *thread) {
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_candle_symbol_t symbol;
    symbol.supper.type = STRING;
    symbol.symbol = "AAPL";
    dxfg_event_type_t* event = dxfg_DXFeed_getLastEventIfSubscribed(thread, feed, DXFG_EVENT_QUOTE,reinterpret_cast<dxfg_symbol_t *>(&symbol));
    dxfg_subscription_t* subscriptionQuote = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE);
    dxfg_DXFeedSubscription_setSymbol(thread, subscriptionQuote, &symbol.supper);
    event = dxfg_DXFeed_getLastEventIfSubscribed(thread, feed, DXFG_EVENT_QUOTE,reinterpret_cast<dxfg_symbol_t *>(&symbol));
    printEvent(event);
    dxfg_EventType_release(thread, event);
    usleep(2000000);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
}

void promisesAllOf(graal_isolatethread_t *thread) {
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    size_t size = symbols.size();
    dxfg_symbol_list symbolList;
    symbolList.size = size;
    symbolList.elements = (dxfg_symbol_t**) malloc(sizeof(dxfg_symbol_t*) * size);
    for (int i = 0; i < size; i++) {
        dxfg_string_symbol_t* symbol1 = new dxfg_string_symbol_t;
        symbol1->supper.type = STRING;
        symbol1->symbol = symbols[i].c_str();
        symbolList.elements[i] = &symbol1->supper;
    }
    dxfg_promise_list* promises = dxfg_DXFeed_getLastEventsPromises(thread, feed, DXFG_EVENT_QUOTE, &symbolList);
    dxfg_promise_t* all = dxfg_Promises_allOf(thread, promises);
    dxfg_Promise_awaitWithoutException(thread, all, 30000);
    if (dxfg_Promise_hasResult(thread, all) == 1) {
        for (int i = 0; i < size; ++i) {
            dxfg_event_type_t* event = dxfg_Promise_EventType_getResult(thread, reinterpret_cast<dxfg_promise_event_t *>(promises->list.elements[i]));
            printEvent(event);
            dxfg_EventType_release(thread, event);
        }
    }
    dxfg_DXEndpoint_close(thread, endpoint);
    for (int i = 0; i < size; i++) {
        free(symbolList.elements[i]);
    }
    free(symbolList.elements);
    dxfg_JavaObjectHandler_release(thread, &all->handler);
    dxfg_CList_JavaObjectHandler_release(thread, &promises->list);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
}

void indexedEventsPromise(graal_isolatethread_t *thread) {
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_string_symbol_t symbol;
    symbol.supper.type = STRING;
    symbol.symbol = "IBM";
    dxfg_indexed_event_source_t* source = dxfg_IndexedEventSource_new(thread, nullptr);
    dxfg_promise_events_t* eventsPromise = dxfg_DXFeed_getIndexedEventsPromise(thread, feed, DXFG_EVENT_SERIES, &symbol.supper, source);
    dxfg_IndexedEventSource_release(thread, source);
    dxfg_Promise_awaitWithoutException(thread, &eventsPromise->base, 30000);
    int hasResult = dxfg_Promise_hasResult(thread, &eventsPromise->base);
    int hasException = dxfg_Promise_hasException(thread, &eventsPromise->base);
    if (hasException == -1) {
        printException(thread);
    } else if (hasException == 1) {
        dxfg_exception_t* exception = dxfg_Promise_getException(thread, &eventsPromise->base);
        if (exception) {
            printf("C: %s\n", exception->stackTrace);
            dxfg_Exception_release(thread, exception);
        }
    } else {
        dxfg_event_type_list* events = dxfg_Promise_List_EventType_getResult(thread, eventsPromise);
        c_print(thread, events, nullptr);
        dxfg_CList_EventType_release(thread, events);
    }
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &eventsPromise->base.handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
}

void getLastEvent(graal_isolatethread_t *thread) {
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_candle_symbol_t symbol;
    symbol.supper.type = CANDLE;
    symbol.symbol = "AAPL";
    dxfg_subscription_t* subscription= dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_CANDLE);
    dxfg_DXFeedSubscription_setSymbol(thread, subscription, &symbol.supper);
    dxfg_candle_t* candle = (dxfg_candle_t*) dxfg_EventType_new(thread, symbol.symbol, DXFG_EVENT_CANDLE);
    dxfg_DXFeed_getLastEvent(thread, feed, &candle->event_type);
    printEvent(&candle->event_type);
    dxfg_event_type_list event_type_list;
    event_type_list.size = 2;
    event_type_list.elements = (dxfg_event_type_t**) malloc(sizeof(dxfg_event_type_t*) * 2);
    event_type_list.elements[0] = (dxfg_event_type_t*) dxfg_EventType_new(thread, "AAPL", DXFG_EVENT_CANDLE);
    event_type_list.elements[1] = (dxfg_event_type_t*) dxfg_EventType_new(thread, "IBM", DXFG_EVENT_CANDLE);
    printEvent(event_type_list.elements[0]);
    printEvent(event_type_list.elements[1]);
    usleep(2000000);
    dxfg_DXFeed_getLastEvent(thread, feed, &candle->event_type);
    printEvent(&candle->event_type);
    dxfg_DXFeed_getLastEvents(thread, feed, &event_type_list);
    printEvent(event_type_list.elements[0]);
    printEvent(event_type_list.elements[1]);
    usleep(2000000);
    dxfg_DXFeed_getLastEvent(thread, feed, &candle->event_type);
    printEvent(&candle->event_type);
    dxfg_DXFeed_getLastEvents(thread, feed, &event_type_list);
    printEvent(event_type_list.elements[0]);
    printEvent(event_type_list.elements[1]);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_EventType_release(thread, &candle->event_type);
    dxfg_JavaObjectHandler_release(thread, &subscription->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
}

void schedule(graal_isolatethread_t *thread) {
    dxfg_system_set_property(thread, "com.dxfeed.schedule.download", "auto");

    dxfg_instrument_profile_reader_t* reader = dxfg_InstrumentProfileReader_new(thread);
    dxfg_instrument_profile_list* profiles = dxfg_InstrumentProfileReader_readFromFile(
        thread, reader, "../../../../../ipf.txt"
    );
    for (int i = 0; i < profiles->size; ++i) {
        dxfg_instrument_profile_t *profile = profiles->elements[i];
        dxfg_schedule_t* schedule = dxfg_Schedule_getInstance(thread, profile);
        const char* name = dxfg_Schedule_getName(thread, schedule);
        printf("C: schedule %s\n", name);
        dxfg_String_release(thread, name);
        dxfg_JavaObjectHandler_release(thread, &schedule->handler);
        dxfg_string_list* venues = dxfg_Schedule_getTradingVenues(thread, profile);
        for (int j = 0; j < venues->size; ++j) {
            dxfg_schedule_t* schedule2 = dxfg_Schedule_getInstance3(thread, profile, venues->elements[j]);
            const char* name = dxfg_Schedule_getName(thread, schedule2);
            printf("C: schedule %s\n", name);
            dxfg_String_release(thread, name);
            dxfg_JavaObjectHandler_release(thread, &schedule2->handler);
        }
        dxfg_CList_String_release(thread, venues);
        printf("C: schedule %s\n", profiles->elements[i]->symbol);
    }
    dxfg_CList_InstrumentProfile_release(thread, profiles);
    dxfg_JavaObjectHandler_release(thread, &reader->handler);
}

int main(int argc, char *argv[]) {
    parseArgs(argc, argv);
    if (graal_create_isolate(nullptr, &isolate, &thread) != 0) {
        print_exception(thread);
        exit(-1);
    }
    liveIpf(thread);
    readerIpf(thread);
    finalizeListener(thread);
    executorBaseOnConcurrentLinkedQueue(thread);
    dxEndpointSubscription(thread);
    dxEndpointTimeSeriesSubscription(thread);
    systemProperties(thread);
    exception(thread);
    orderBookModel(thread);
    indexedEventModel(thread);
    promise(thread);
    lastEventIfSubscribed(thread);
    promisesAllOf(thread);
    indexedEventsPromise(thread);
    getLastEvent(thread);
    schedule(thread);
}
