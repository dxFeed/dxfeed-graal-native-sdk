// SPDX-License-Identifier: MPL-2.0

#include "CommandLineParser.hpp"
#include <DxfgEventFormatter/QuoteFormatter.hpp>
#include <DxfgEventFormatter/TimeAndSaleFormatter.hpp>
#include <dxfg_api.h>
#include <iostream>

using namespace dxfg;

static graal_isolate_t *isolate = nullptr;
static graal_isolatethread_t *thread = nullptr;
static std::unordered_map<std::string, std::string> properties{};
static std::string address{};
static std::vector<dxfg_event_clazz_t> eventTypes{};
static std::vector<std::string> symbols{};
static bool isQuiteMode = false;

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
        dxfg_IndexedEventSource_release(thread, source);
        printf(
            "C: ORDER{order_base.count=%lld, market_maker=%s}\n",
            event->order_base.count,
            event->market_maker
        );
    } else if (pEvent->clazz == DXFG_EVENT_ANALYTIC_ORDER) {
        auto *event = (dxfg_analytic_order_t *)pEvent;
        printf(
            "C: ANALYTIC_ORDER{order_base.count=%lld, iceberg_peak_size=%f}\n",
            event->order_base.count,
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
            "C: CANDLE{symbol=%s, index=%lld, ask_volume=%E}\n",
            event->event_symbol->symbol,
            event->index,
            event->ask_volume
        );
    } else if (pEvent->clazz == DXFG_EVENT_DAILY_CANDLE) {
        auto *event = (dxfg_daily_candle_t *)pEvent;
        printf(
            "C: DAILY_CANDLE{symbol=%s, index=%lld, ask_volume=%E}\n",
            event->candle.event_symbol->symbol,
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

int main(int argc, char *argv[]) {
    // Parses input args.
    parseArgs(argc, argv);

    // Creates a graal isolate.
    auto graal_result = graal_create_isolate(nullptr, &isolate, &thread);
    if (graal_result != 0) {
        std::cerr << "Can't create graal isolate!"
                  << "Error code: " << graal_result << std::endl;
        exit(graal_result);
    }

    // Sets system properties.
    for (const auto &property : properties) {
        if (dxfg_system_set_property(thread, property.first.c_str(), property.second.c_str()) != 0) {
            std::cout << "Can't set property: [" << property.first << ":" << property.second << "]" << std::endl;
        }
    }

    const char* d = dxfg_system_get_property(thread, "dsds");
    if (!d) {
        print_exception(thread);
    }


    // Creates an endpoint object.
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    if (endpoint == nullptr) {
        print_exception(thread);
    }


    dxfg_executor_t* executor = dxfg_Executors_newFixedThreadPool(thread, 2, "thread-processing-events");
    dxfg_DXEndpoint_executor(thread, endpoint, executor);

    dxfg_endpoint_state_change_listener_t* stateListener = dxfg_PropertyChangeListener_new(thread, endpoint_state_change_listener, nullptr);
    dxfg_DXEndpoint_addStateChangeListener(thread, endpoint, stateListener);
    dxfg_DXEndpoint_addStateChangeListener(thread, endpoint, stateListener);

    // Connects to an address.
    int res = dxfg_DXEndpoint_connect(thread, endpoint, address.c_str());
    if (res != 0) {
        print_exception(thread);
    }

    dxfg_event_clazz_list_t* event_clazz_list = dxfg_DXEndpoint_getEventTypes(thread, endpoint);
    for (int i = 0; i < event_clazz_list->size; ++i) {
        printf("%d \n", *event_clazz_list->elements[i]);
    }

    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    if (feed) {
        print_exception(thread);
    }

    int state = dxfg_DXEndpoint_getState(thread, endpoint);
    if (state == -1) {
        print_exception(thread);
    } else {
        printf("C: get_state = %d\n", state);
    }


    dxfg_subscription_t* subscriptionQuote = dxfg_DXFeedSubscription_new(thread, DXFG_EVENT_QUOTE);
    dxfg_DXFeed_attachSubscription(thread, feed, subscriptionQuote);
    dxfg_DXFeedSubscription_addEventListener(
        thread,
        subscriptionQuote,
        dxfg_DXFeedEventListener_new(thread, &c_print, nullptr)
    );
    for (const auto &symbol : symbols) {
        dxfg_symbol_t* symbol1 = dxfg_Symbol_new(thread, symbol.c_str(), STRING);
        dxfg_DXFeedSubscription_addSymbol(thread, subscriptionQuote, symbol1);
        dxfg_Symbol_release(thread, symbol1);
    }

    int containQuote = dxfg_DXFeedSubscription_containsEventType(thread, subscriptionQuote, DXFG_EVENT_QUOTE);
    int containCandle = dxfg_DXFeedSubscription_containsEventType(thread, subscriptionQuote, DXFG_EVENT_CANDLE);

    dxfg_time_series_subscription_t* subTaS = dxfg_DXFeed_createTimeSeriesSubscription2(thread, feed, event_clazz_list);
    dxfg_CList_EventClazz_release(thread, event_clazz_list);
    dxfg_DXFeedTimeSeriesSubscription_setFromTime(thread, subTaS, 0);
//    subscriptionQuote = subTaS->subscriptionQuote;


    dxfg_candle_symbol_t candleSymbol;
    candleSymbol.supper.type = CANDLE;
    candleSymbol.symbol = "AAPL";
    dxfg_promise_event_t* candlePromise = dxfg_DXFeed_getLastEventPromise(thread, feed, DXFG_EVENT_CANDLE, &candleSymbol.supper);
    dxfg_Promise_whenDone(thread, reinterpret_cast<dxfg_promise_t *>(candlePromise), &c_promise_func, nullptr);

//    dxfg_quote_t* quote = nullptr;
//    dxfg_candle_symbol_t symbol1;
//    symbol1.supper.type = STRING;
//    symbol1.symbol = "AAPL";
//    dxfg_DXFeed_getLastEventIfSubscribed(thread, feed, DXFG_EVENT_QUOTE, &symbol1, (dxfg_event_type_t**)&quote);

    // Waiting for input to exit.
//    std::cin.get();

    size_t size = symbols.size();
    dxfg_symbol_t** symbols2 = (dxfg_symbol_t**) malloc(sizeof(dxfg_symbol_t*) * size);
    for (int i = 0; i < size; ++i) {
        dxfg_string_symbol_t symbol1;
        symbol1.supper.type = STRING;
        symbol1.symbol = symbols[i].c_str();
        symbols2[i] = &symbol1.supper;
    }
    dxfg_symbol_list symbolList;
    symbolList.size = 3;
    symbolList.elements = symbols2;
//    dxfg_promise_event_t *promises[size];
    dxfg_promise_list* promises = dxfg_DXFeed_getLastEventsPromises(thread, feed, DXFG_EVENT_QUOTE, &symbolList);
    dxfg_promise_t* all = dxfg_Promises_allOf(thread, promises);
    if (!all) {
        print_exception(thread);
    }
    int e = dxfg_Promise_awaitWithoutException(thread, all, 30000);
    if (e != 1) {
        print_exception(thread);
    }
    if (dxfg_Promise_hasResult(thread, all) == 1) {
        for (int i = 0; i < size; ++i) {
            dxfg_event_type_t* event = dxfg_Promise_EventType_getResult(thread, reinterpret_cast<dxfg_promise_event_t *>(promises->list.elements[i]));
            printEvent(event);
            dxfg_EventType_release(thread, event);
        }
    }
    dxfg_CList_JavaObjectHandler_release(thread, &promises->list);
    dxfg_JavaObjectHandler_release(thread, &all->handler);


//    dxfg_symbol_t candleSymbol;
//    candleSymbol.symbol_type = CANDLE;
//    candleSymbol.candleSymbol = "IBM";
//    dxfg_promise_t candlePromise;
//    dxfg_indexed_event_source_t* source = dxfg_IndexedEventSource_new(thread, nullptr);
//    dxfg_DXFeed_getIndexedEventsPromise(thread, feed, DXFG_EVENT_CANDLE,
//                                         &candleSymbol, source, &candlePromise);
//    dxfg_JavaObjectHandler_release(thread, source);
//    dxfg_Promise_awaitWithoutException(thread, &candlePromise, 30000);
//    int hasResult = dxfg_Promise_hasResult(thread, &candlePromise);
//    int hasException = dxfg_Promise_hasException(thread, &candlePromise);
//    if (hasException == 1) {
//        dxfg_exception_t* exception = dxfg_Promise_getException(thread, &candlePromise);
//        if (exception) {
//            printf("C: %s\n", exception->stackTrace);
//            dxfg_Exception_release(thread, exception);
//        }
//    } else {
//        dxfg_event_type_t* candleEvent = dxfg_Promise_getResult(thread, &candlePromise);
//        printEvent(candleEvent);
//        dxfg_EventType_release(thread, candleEvent);
//    }
//    dxfg_JavaObjectHandler_release(thread, &candlePromise); TODO clear base (not holder)




    dxfg_string_symbol_t seriesSymbol;
    seriesSymbol.supper.type = STRING;
    seriesSymbol.symbol = "IBM";
    dxfg_indexed_event_source_t* seriesSource = dxfg_IndexedEventSource_new(thread, nullptr);
    dxfg_promise_events_t* seriesPromise = dxfg_DXFeed_getIndexedEventsPromise(thread, feed, DXFG_EVENT_SERIES,&seriesSymbol.supper, seriesSource);
    dxfg_IndexedEventSource_release(thread, seriesSource);
    dxfg_Promise_awaitWithoutException(thread, &seriesPromise->base, 30000);
    int hasResultSeries = dxfg_Promise_hasResult(thread, &seriesPromise->base);
    int hasExceptionSeries = dxfg_Promise_hasException(thread, &seriesPromise->base);
    if (hasExceptionSeries == 1) {
        dxfg_exception_t* exception = dxfg_Promise_getException(thread, &seriesPromise->base);
        if (exception) {
            printf("C: %s\n", exception->stackTrace);
            dxfg_Exception_release(thread, exception);
        }
    } else {
        dxfg_event_type_list* seriesEvent = dxfg_Promise_List_EventType_getResult(thread, seriesPromise);
        for (int i = 0; i < seriesEvent->size; ++i) {
            printEvent(reinterpret_cast<dxfg_event_type_t *>(seriesEvent->elements[0]));
        }
        dxfg_CList_EventType_release(thread, seriesEvent);
    }


//        dxfg_DXFeed_getLastEventIfSubscribed(thread, feed, DXFG_EVENT_QUOTE, &symbol1, (dxfg_event_type_t**)&quote);


    dxfg_event_type_list event_type_list;
    event_type_list.size = 2;
    event_type_list.elements = (dxfg_event_type_t**) malloc(sizeof(dxfg_event_type_t*) * 2);
    event_type_list.elements[0] = (dxfg_event_type_t*) dxfg_EventType_new(thread, "AAPL", DXFG_EVENT_CANDLE);
    event_type_list.elements[1] = (dxfg_event_type_t*) dxfg_EventType_new(thread, "IBM", DXFG_EVENT_CANDLE);
    printEvent(event_type_list.elements[0]);
    printEvent(event_type_list.elements[1]);
    dxfg_DXFeed_getLastEvents(thread, feed, &event_type_list);
    printEvent(event_type_list.elements[0]);
    printEvent(event_type_list.elements[1]);
    dxfg_DXFeed_getLastEvents(thread, feed, &event_type_list);
    printEvent(event_type_list.elements[0]);
    printEvent(event_type_list.elements[1]);
    dxfg_DXFeed_getLastEvents(thread, feed, &event_type_list);
    printEvent(event_type_list.elements[0]);
    printEvent(event_type_list.elements[1]);


    dxfg_candle_t* q = (dxfg_candle_t*) dxfg_EventType_new(thread, "AAPL", DXFG_EVENT_CANDLE);
    dxfg_DXFeed_getLastEvent(thread, feed, &q->event_type);
    printEvent(&q->event_type);
    dxfg_DXFeed_getLastEvent(thread, feed, &q->event_type);
    printEvent(&q->event_type);
    dxfg_DXFeed_getLastEvent(thread, feed, &q->event_type);
    printEvent(&q->event_type);
    dxfg_EventType_release(thread, &q->event_type);

    res = dxfg_DXEndpoint_closeAndAwaitTermination(thread, endpoint);
    if (res != 0) {
        print_exception(thread);
    }
    dxfg_JavaObjectHandler_release(thread, &executor->handler);
}
