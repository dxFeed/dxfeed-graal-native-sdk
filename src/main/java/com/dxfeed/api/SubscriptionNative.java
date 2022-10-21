// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api;

import com.dxfeed.api.events.BaseSymbol;
import com.dxfeed.api.events.BaseSymbol.BaseSymbolPtr;
import com.dxfeed.api.events.EventTypesNative;
import com.dxfeed.event.EventType;
import com.dxfeed.event.market.ListEventMapper;
import com.oracle.svm.core.c.CTypedef;
import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.*;

import java.util.*;

import static com.dxfeed.api.ErrorNative.ErrorCodes;
import static com.dxfeed.api.events.EventNative.BaseEventNativePtr;

@CContext(SubscriptionNative.NativeDirectives.class)
public class SubscriptionNative {
    static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/api/dxfg_subscription.h"));
        }
    }

    private static final HashMap<Long, DXFeedSubscription<EventType<?>>> SUBSCRIPTION_MAP = new HashMap<>();
    private static final HashMap<Long, DXFeedEventListener<EventType<?>>> EVENT_LISTENER_MAP = new HashMap<>();
    private static final ListEventMapper EVENT_MAPPER = new ListEventMapper();

    @CTypedef(name = "dxfg_sub_event_listener")
    interface EventListenerPtr extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(BaseEventNativePtr events, int size);
    }

    @CEntryPoint(name = "dxfg_sub_create_from_endpoint")
    public static ErrorCodes createFromEndpoint(IsolateThread ignoredThread,
                                                long endpointDescriptor,
                                                CIntPointer eventTypes, int eventTypesSize,
                                                CLongPointer subscriptionDescriptor) {
        if (eventTypes.isNull() || subscriptionDescriptor.isNull()) {
            return ErrorCodes.DXFG_EC_NULL_POINTER_EX;
        }
        var feed = EndpointNative.getFeedByDescriptor(endpointDescriptor);
        if (feed == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        var setTypes = new HashSet<Class<? extends EventType<?>>>();
        for (int i = 0; i < eventTypesSize; ++i) {
            setTypes.add(EventTypesNative.toEventType(eventTypes.read(i)));
        }

        @SuppressWarnings("unchecked")
        Class<? extends EventType<?>>[] types = setTypes.toArray(Class[]::new);
        var sub = feed.createSubscription(types);
        subscriptionDescriptor.write(putSubscriptionToMap(sub));
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_sub_close")
    private static ErrorCodes close(IsolateThread ignoredThread, long subscriptionDescriptor) {
        var sub = removeSubscriptionFromMap(subscriptionDescriptor);
        if (sub == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        sub.close();
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_sub_add_event_listener")
    public static ErrorCodes addEventListener(IsolateThread ignoreThread,
                                              long subscriptionDescriptor, EventListenerPtr listenerPtr) {
        if (listenerPtr.isNull()) {
            return ErrorCodes.DXFG_EC_NULL_POINTER_EX;
        }
        var sub = getSubscriptionByDescriptor(subscriptionDescriptor);
        if (sub == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        if (!EVENT_LISTENER_MAP.containsKey(listenerPtr.rawValue())) {
            DXFeedEventListener<EventType<?>> listener = events -> {
                var nativeEvents = EVENT_MAPPER.nativeObject(events);
                var size = events.size();
                listenerPtr.invoke(nativeEvents, size);
                EVENT_MAPPER.delete(nativeEvents, size);
            };
            sub.addEventListener(listener);
            EVENT_LISTENER_MAP.put(listenerPtr.rawValue(), listener);
        }
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_sub_remove_event_listener")
    public static ErrorCodes removeEventListener(IsolateThread ignoreThread,
                                                 long subscriptionDescriptor, EventListenerPtr listenerPtr) {
        if (listenerPtr.isNull()) {
            return ErrorCodes.DXFG_EC_NULL_POINTER_EX;
        }
        var sub = getSubscriptionByDescriptor(subscriptionDescriptor);
        if (sub == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        var listener = EVENT_LISTENER_MAP.remove(listenerPtr.rawValue());
        if (listener != null) {
            sub.removeEventListener(listener);
        }
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_sub_add_symbol")
    public static ErrorCodes addSymbol(IsolateThread ignoreThread, long subscriptionDescriptor, BaseSymbol symbol) {
        var sub = getSubscriptionByDescriptor(subscriptionDescriptor);
        if (sub == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        sub.addSymbols(CTypeConversion.toJavaString(symbol.getSymbolName()));
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_sub_add_symbols")
    public static ErrorCodes addSymbols(IsolateThread ignoreThread,
                                        long subscriptionDescriptor, BaseSymbolPtr symbols, int symbolsSize) {
        var sub = getSubscriptionByDescriptor(subscriptionDescriptor);
        if (sub == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        var listSymbols = new ArrayList<String>();
        for (int i = 0; i < symbolsSize; ++i) {
            listSymbols.add(CTypeConversion.toJavaString(symbols.addressOf(i).read().getSymbolName()));
        }
        sub.addSymbols(listSymbols);
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_sub_remove_symbol")
    public static ErrorCodes removeSymbol(IsolateThread ignoreThread,
                                          long subscriptionDescriptor, BaseSymbol symbol) {
        var sub = getSubscriptionByDescriptor(subscriptionDescriptor);
        if (sub == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        sub.removeSymbols(CTypeConversion.toJavaString(symbol.getSymbolName()));
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_sub_remove_symbols")
    public static ErrorCodes removeSymbols(IsolateThread ignoreThread,
                                           long subscriptionDescriptor, BaseSymbolPtr symbols, int symbolSize) {
        var sub = getSubscriptionByDescriptor(subscriptionDescriptor);
        if (sub == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        var listSymbols = new ArrayList<String>();
        for (int i = 0; i < symbolSize; ++i) {
            listSymbols.add(CTypeConversion.toJavaString(symbols.addressOf(i).read().getSymbolName()));
        }
        sub.removeSymbols(listSymbols);
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    @CEntryPoint(name = "dxfg_sub_clear")
    public static ErrorCodes clear(IsolateThread ignoreThread, long subscriptionDescriptor) {
        var sub = getSubscriptionByDescriptor(subscriptionDescriptor);
        if (sub == null) {
            return ErrorCodes.DXFG_EC_UNKNOWN_DESCRIPTOR;
        }
        sub.clear();
        return ErrorCodes.DXFG_EC_SUCCESS;
    }

    /**
     * Gets subscription object from subscription map by descriptor.
     *
     * @param subscriptionDescriptor The descriptor.
     * @return Returns DXFeedSubscription or null if this descriptor not exist.
     */
    private static DXFeedSubscription<EventType<?>> getSubscriptionByDescriptor(long subscriptionDescriptor) {
        return SUBSCRIPTION_MAP.get(subscriptionDescriptor);
    }

    /**
     * Puts subscription object to map for avoid GC
     * and returns descriptor (primitive value) associated with this subscription object.
     *
     * @param subscription The DXFeedSubscription.
     * @return Returns descriptor.
     */
    private static long putSubscriptionToMap(DXFeedSubscription<EventType<?>> subscription) {
        long descriptor = SUBSCRIPTION_MAP.size();
        var oldSub = SUBSCRIPTION_MAP.put(descriptor, subscription);
        // Shouldn't happen.
        if (oldSub != null) {
            oldSub.close();
        }
        return descriptor;
    }

    /**
     * Removes subscription object from map.
     *
     * @param subscriptionDescriptor The subscription descriptor.
     * @return Returns the removed subscription object, or null if descriptor not exist.
     */
    private static DXFeedSubscription<EventType<?>> removeSubscriptionFromMap(long subscriptionDescriptor) {
        return SUBSCRIPTION_MAP.remove(subscriptionDescriptor);
    }
}
