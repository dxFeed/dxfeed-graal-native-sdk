// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api.events;

import com.dxfeed.api.events.EventNative.BaseEventNative;
import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

import java.util.*;

@CContext(QuoteNative.NativeDirectives.class)
@CStruct("dxfg_event_quote_t")
public interface QuoteNative extends BaseEventNative {
    class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/api/dxfg_events.h"));
        }
    }

    @CField("time_millis_sequence")
    int getTimeMillisSequence();

    @CField("time_millis_sequence")
    void setTimeMillisSequence(int timeMillisSequence);

    @CField("time_nano_part")
    int getTimeNanoPart();

    @CField("time_nano_part")
    void setTimeNanoPart(int timeNanoPart);

    @CField("bid_time")
    long getBidTime();

    @CField("bid_time")
    void setBidTime(long bidTime);

    @CField("bid_exchange_code")
    char getBidExchangeCode();

    @CField("bid_exchange_code")
    void setBidExchangeCode(char bidExchangeCode);

    @CField("bid_price")
    double getBidPrice();

    @CField("bid_price")
    void setBidPrice(double bidPrice);

    @CField("bid_size")
    double getBidSize();

    @CField("bid_size")
    void setBidSize(double bidSize);

    @CField("ask_time")
    long getAskTime();

    @CField("ask_time")
    void setAskTime(long askTime);

    @CField("ask_exchange_code")
    char getAskExchangeCode();

    @CField("ask_exchange_code")
    void setAskExchangeCode(char askExchangeCode);

    @CField("ask_price")
    double getAskPrice();

    @CField("ask_price")
    void setAskPrice(double askPrice);

    @CField("ask_size")
    double getAskSize();

    @CField("ask_size")
    void setAskSize(double askSize);
}
