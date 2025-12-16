// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_nuam_order_t")
public interface DxfgNuamOrder extends DxfgOrder {
    @CField("actor_id")
    int getActorId();

    @CField("actor_id")
    void setActorId(int actorId);

    @CField("participant_id")
    int getParticipantId();

    @CField("participant_id")
    void setParticipantId(int participantId);

    @CField("submitter_id")
    int getSubmitterId();

    @CField("submitter_id")
    void setSubmitterId(int submitterId);

    @CField("on_behalf_of_submitter_id")
    int getOnBehalfOfSubmitterId();

    @CField("on_behalf_of_submitter_id")
    void setOnBehalfOfSubmitterId(int onBehalfOfSubmitterId);

    @CField("client_order_id")
    CCharPointer getClientOrderId();

    @CField("client_order_id")
    void setClientOrderId(CCharPointer clientOrderId);

    @CField("customer_account")
    CCharPointer getCustomerAccount();

    @CField("customer_account")
    void setCustomerAccount(CCharPointer customerAccount);

    @CField("customer_info")
    CCharPointer getCustomerInfo();

    @CField("customer_info")
    void setCustomerInfo(CCharPointer customerInfo);

    @CField("exchange_info")
    CCharPointer getExchangeInfo();

    @CField("exchange_info")
    void setExchangeInfo(CCharPointer exchangeInfo);

    @CField("time_in_force_data")
    int getTimeInForceData();

    @CField("time_in_force_data")
    void setTimeInForceData(int timeInForceData);

    @CField("trigger_order_book_id")
    int getTriggerOrderBookId();

    @CField("trigger_order_book_id")
    void setTriggerOrderBookId(int triggerOrderBookId);

    @CField("trigger_price")
    double getTriggerPrice();

    @CField("trigger_price")
    void setTriggerPrice(double triggerPrice);

    @CField("trigger_session_type")
    int getTriggerSessionType();

    @CField("trigger_session_type")
    void setTriggerSessionType(int triggerSessionType);

    @CField("order_quantity")
    double getOrderQuantity();

    @CField("order_quantity")
    void setOrderQuantity(double orderQuantity);

    @CField("display_quantity")
    double getDisplayQuantity();

    @CField("display_quantity")
    void setDisplayQuantity(double displayQuantity);

    @CField("refresh_quantity")
    double getRefreshQuantity();

    @CField("refresh_quantity")
    void setRefreshQuantity(double refreshQuantity);

    @CField("leaves_quantity")
    double getLeavesQuantity();

    @CField("leaves_quantity")
    void setLeavesQuantity(double leavesQuantity);

    @CField("matched_quantity")
    double getMatchedQuantity();

    @CField("matched_quantity")
    void setMatchedQuantity(double matchedQuantity);

    @CField("nuam_flags")
    int getNuamFlags();

    @CField("nuam_flags")
    void setNuamFlags(int nuamFlags);
}
