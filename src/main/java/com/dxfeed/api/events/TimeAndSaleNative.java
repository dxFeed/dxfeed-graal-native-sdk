// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api.events;

import com.dxfeed.event.market.TimeAndSaleType;
import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

import java.util.Collections;
import java.util.List;

import static com.dxfeed.api.events.EventNative.BaseEventNative;

@CContext(TimeAndSaleNative.NativeDirectives.class)
@CStruct("dxfg_event_time_and_sale_t")
public interface TimeAndSaleNative extends BaseEventNative {
    class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/api/dxfg_events.h"));
        }
    }

    @CEnum("dxfg_event_time_and_sale_type_t")
    public enum TimeAndSaleTypeNative {
        DXFG_EVENT_TIME_AND_SALE_TYPE_NEW,
        DXFG_EVENT_TIME_AND_SALE_TYPE_CORRECTION,
        DXFG_EVENT_TIME_AND_SALE_TYPE_CANCEL,
        DXFG_EVENT_TIME_AND_SALE_TYPE_UNKNOWN;

        public static TimeAndSaleTypeNative fromTimeAndSaleType(TimeAndSaleType type) {
            switch (type) {
                case NEW:
                    return DXFG_EVENT_TIME_AND_SALE_TYPE_NEW;
                case CORRECTION:
                    return DXFG_EVENT_TIME_AND_SALE_TYPE_CORRECTION;
                case CANCEL:
                    return DXFG_EVENT_TIME_AND_SALE_TYPE_CANCEL;
                default:
                    return DXFG_EVENT_TIME_AND_SALE_TYPE_UNKNOWN;
            }
        }

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native TimeAndSaleTypeNative fromCValue(int value);
    }

    @CField("event_flags")
    int getEventFlags();

    @CField("event_flags")
    void setEventFlags(int eventFlag);

    @CField("index")
    long getIndex();

    @CField("index")
    void setIndex(long index);

    @CField("time_nano_part")
    int getTimeNanoPart();

    @CField("time_nano_part")
    void setTimeNanoPart(int timeNanoPart);

    @CField("exchange_code")
    char getExchangeCode();

    @CField("exchange_code")
    void setExchangeCode(char exchangeCode);

    @CField("price")
    double getPrice();

    @CField("price")
    void setPrice(double price);

    @CField("size")
    double getSize();

    @CField("size")
    void setSize(double size);

    @CField("bid_price")
    double getBidPrice();

    @CField("bid_price")
    void setBidPrice(double bidPrice);

    @CField("ask_price")
    double getAskPrice();

    @CField("ask_price")
    void setAskPrice(double askPrice);

    @CField("exchange_sale_condition")
    CCharPointer getExchangeSaleCondition();

    @CField("exchange_sale_condition")
    void setExchangeSaleCondition(CCharPointer exchangeSaleCondition);

    @CField("flags")
    int getFlags();

    @CField("flags")
    void setFlags(int askPrice);

    @CField("buyer")
    CCharPointer getBuyer();

    @CField("buyer")
    void setBuyer(CCharPointer buyer);

    @CField("seller")
    CCharPointer getSeller();

    @CField("seller")
    void setSeller(CCharPointer seller);
}
