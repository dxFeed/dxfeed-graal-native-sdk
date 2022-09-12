package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.mapper.ListEventMapper;
import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;

import java.util.*;

@CContext(NativeLibApi.NativeLibApiDirectives.class)
public class NativeLibApi {
    private static final ListEventMapper LIST_EVENT_MAPPER = new ListEventMapper();

    static class NativeLibApiDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            /*
             * The header file with the C declarations that are imported. We use a helper class that
             * locates the file in our project structure.
             */
            return Collections.singletonList(ProjectHeaderFile.resolve("com.dxfeed", "src/main/c/dxf_types.h"));
        }
    }

    @CEnum("dxf_event_type_t")
    public enum EventsTypes {
        DXF_EVENT_TYPE_QUOTE,
        DXF_EVENT_TYPE_TIME_AND_SALE,
        DXF_EVENT_TYPE_CANDLES;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native EventsTypes fromCValue(int value);
    }

    /**
     * Base type of all events.
     */
    @CStruct("dxf_event_t")
    public interface EventNative extends PointerBase {
        @CField("event_type")
        int getEventType();

        @CField("event_type")
        void setEventType(int eventType);

        @CField("symbol_name")
        CCharPointer getSymbolName();

        @CField("symbol_name")
        void setSymbolName(CCharPointer symbolName);
    }

    /**
     * Pointer to EventNative.
     */
    @CPointerTo(EventNative.class)
    public interface EventNativePtr extends PointerBase {
        /**
         * Provides access to an array element by index.
         *
         * @param index The index of array.
         * @return Returns pointer.
         */
        EventNativePtr addressOf(int index);

        EventNative read();

        void write(EventNative data);
    }

    /**
     * The Quote.
     */
    @CStruct("dxf_event_quote_t")
    public interface QuoteNative extends EventNative {
        @CField("bid_price")
        double getBidPrice();

        @CField("bid_price")
        void setBidPrice(double bidPrice);

        @CField("ask_price")
        double getAskPrice();

        @CField("ask_price")
        void setAskPrice(double askPrice);
    }

    /**
     * The TimeAndSale.
     */
    @CStruct("dxf_event_time_and_sale_t")
    public interface TimeAndSaleNative extends EventNative {
        @CField("event_flag")
        int getEventFlag();

        @CField("event_flag")
        void setEventFlag(int eventFlag);

        @CField("index")
        long getIndex();

        @CField("index")
        void setIndex(long index);
    }

    interface EventListenerFunctionPtr extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(EventNativePtr events, int count);
    }

    @CEntryPoint(name = "native_create_events_and_call_listener")
    public static void createEventsAndCallListener(
        final IsolateThread ignoreIsolate,
        final EventListenerFunctionPtr listener
    ) {
        final String token = System.getProperty("token");
        final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
                .withRole(DXEndpoint.Role.FEED)
                .withProperties(System.getProperties())
                .build();
        final DXFeedSubscription<MarketEvent> subscription = dxEndpoint.getFeed().createSubscription(Quote.class, TimeAndSale.class);
        dxEndpoint.connect("lessona.dxfeed.com:7905[login=entitle:" + token + "]");
        subscription.addEventListener(qdEvents -> {
            final EventNativePtr nativeEvents = LIST_EVENT_MAPPER.nativeObject(qdEvents);
            try {
                listener.invoke(nativeEvents, qdEvents.size());
            } finally {
                LIST_EVENT_MAPPER.delete(nativeEvents, qdEvents.size());
            }
        });
        subscription.addSymbols(Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD", "BTC/USDT:CXBINA"));
    }

    /**
     * Creates graalvm native thread.
     *
     * @return The graalvm native thread.
     */
    @CEntryPoint(name = "native_test_api_create_isolate", builtin = CEntryPoint.Builtin.CREATE_ISOLATE)
    static native IsolateThread createIsolate();

    /**
     * Sets Java System Property as key-value pair.
     *
     * @param thread The graalvm native thread.
     * @param key    The key.
     * @param value  The value.
     */
    @CEntryPoint(name = "native_test_set_system_property")
    static void setSystemProperty(final IsolateThread thread, final CCharPointer key, final CCharPointer value) {
        System.setProperty(CTypeConversion.toJavaString(key), CTypeConversion.toJavaString(value));
    }

    /**
     * Checks Java System Property by key.
     *
     * @param thread The graalvm native thread.
     * @param key    The key.
     * @param value  The value.
     * @return {@code true} if the passing key-value equivalent to the corresponding Java System Property,
     * {@code false} otherwise.
     */
    @CEntryPoint(name = "native_test_check_system_property")
    static boolean checkSystemProperty(final IsolateThread thread, final CCharPointer key, final CCharPointer value) {
        final String property = System.getProperty(CTypeConversion.toJavaString(key));
        return property.equals(CTypeConversion.toJavaString(value));
    }
}
