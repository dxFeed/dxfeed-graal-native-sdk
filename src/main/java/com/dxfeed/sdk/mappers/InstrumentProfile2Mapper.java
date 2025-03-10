// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.ipf.InstrumentProfileCustomFields;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile2Pointer;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class InstrumentProfile2Mapper extends
        Mapper<InstrumentProfile, DxfgInstrumentProfile2Pointer> {

    Mapper<String, CCharPointer> stringMapper;

    public InstrumentProfile2Mapper(Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    public DxfgInstrumentProfile2Pointer toNative(final InstrumentProfile javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }

        final DxfgInstrumentProfile2Pointer nativeObject = UnmanagedMemory.calloc(
                SizeOf.get(DxfgInstrumentProfile2Pointer.class));

        fillNative(javaObject, nativeObject, false);

        return nativeObject;
    }

    @Override
    public void fillNative(final InstrumentProfile javaObject,
            final DxfgInstrumentProfile2Pointer nativeObject, boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setType(stringMapper.toNative(javaObject.getType()));
        nativeObject.setSymbol(stringMapper.toNative(javaObject.getSymbol()));
        nativeObject.setDescription(stringMapper.toNative(javaObject.getDescription()));
        nativeObject.setLocalSymbol(stringMapper.toNative(javaObject.getLocalSymbol()));
        nativeObject.setLocalDescription(
                stringMapper.toNative(javaObject.getLocalDescription()));
        nativeObject.setCountry(stringMapper.toNative(javaObject.getCountry()));
        nativeObject.setOpol(stringMapper.toNative(javaObject.getOPOL()));
        nativeObject.setExchangeData(stringMapper.toNative(javaObject.getExchangeData()));
        nativeObject.setExchanges(stringMapper.toNative(javaObject.getExchanges()));
        nativeObject.setCurrency(stringMapper.toNative(javaObject.getCurrency()));
        nativeObject.setBaseCurrency(stringMapper.toNative(javaObject.getBaseCurrency()));
        nativeObject.setCfi(stringMapper.toNative(javaObject.getCFI()));
        nativeObject.setIsin(stringMapper.toNative(javaObject.getISIN()));
        nativeObject.setSedol(stringMapper.toNative(javaObject.getSEDOL()));
        nativeObject.setCusip(stringMapper.toNative(javaObject.getCUSIP()));
        nativeObject.setIcb(javaObject.getICB());
        nativeObject.setSic(javaObject.getSIC());
        nativeObject.setMultiplier(javaObject.getMultiplier());
        nativeObject.setProduct(stringMapper.toNative(javaObject.getProduct()));
        nativeObject.setUnderlying(stringMapper.toNative(javaObject.getUnderlying()));
        nativeObject.setSpc(javaObject.getSPC());
        nativeObject.setAdditionalUnderlyings(
                stringMapper.toNative(javaObject.getAdditionalUnderlyings()));
        nativeObject.setMmy(stringMapper.toNative(javaObject.getMMY()));
        nativeObject.setExpiration(javaObject.getExpiration());
        nativeObject.setLastTrade(javaObject.getLastTrade());
        nativeObject.setStrike(javaObject.getStrike());
        nativeObject.setOptionType(stringMapper.toNative(javaObject.getOptionType()));
        nativeObject.setExpirationStyle(
                stringMapper.toNative(javaObject.getExpirationStyle()));
        nativeObject.setSettlementStyle(
                stringMapper.toNative(javaObject.getSettlementStyle()));
        nativeObject.setPriceIncrements(
                stringMapper.toNative(javaObject.getPriceIncrements()));
        nativeObject.setTradingHours(stringMapper.toNative(javaObject.getTradingHours()));

        var fields = new InstrumentProfileCustomFields(javaObject);

        if (fields.areNullOrEmpty()) {
            nativeObject.setInstrumentProfileCustomFields(WordFactory.nullPointer());
        } else {
            nativeObject.setInstrumentProfileCustomFields(
                    NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toNative(fields));
        }

        nativeObject.setInstrumentProfileCustomFieldsHash(fields.hashCode());
    }

    @Override
    public void cleanNative(final DxfgInstrumentProfile2Pointer nativeObject) {
        if (nativeObject.isNull()) {
            return;
        }

        stringMapper.release(nativeObject.getType());
        stringMapper.release(nativeObject.getSymbol());
        stringMapper.release(nativeObject.getDescription());
        stringMapper.release(nativeObject.getLocalSymbol());
        stringMapper.release(nativeObject.getLocalDescription());
        stringMapper.release(nativeObject.getCountry());
        stringMapper.release(nativeObject.getOpol());
        stringMapper.release(nativeObject.getExchangeData());
        stringMapper.release(nativeObject.getExchanges());
        stringMapper.release(nativeObject.getCurrency());
        stringMapper.release(nativeObject.getBaseCurrency());
        stringMapper.release(nativeObject.getCfi());
        stringMapper.release(nativeObject.getIsin());
        stringMapper.release(nativeObject.getSedol());
        stringMapper.release(nativeObject.getCusip());
        stringMapper.release(nativeObject.getProduct());
        stringMapper.release(nativeObject.getUnderlying());
        stringMapper.release(nativeObject.getAdditionalUnderlyings());
        stringMapper.release(nativeObject.getMmy());
        stringMapper.release(nativeObject.getOptionType());
        stringMapper.release(nativeObject.getExpirationStyle());
        stringMapper.release(nativeObject.getSettlementStyle());
        stringMapper.release(nativeObject.getPriceIncrements());
        stringMapper.release(nativeObject.getTradingHours());

        NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.release(nativeObject.getInstrumentProfileCustomFields());
    }

    @Override
    protected InstrumentProfile doToJava(final DxfgInstrumentProfile2Pointer nativeObject) {
        final InstrumentProfile javaObject = new InstrumentProfile();

        fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @Override
    public void fillJava(final DxfgInstrumentProfile2Pointer nativeObject,
            final InstrumentProfile javaObject) {
        javaObject.setType(stringMapper.toJava(nativeObject.getType()));
        javaObject.setType(stringMapper.toJava(nativeObject.getType()));
        javaObject.setSymbol(stringMapper.toJava(nativeObject.getSymbol()));
        javaObject.setDescription(stringMapper.toJava(nativeObject.getDescription()));
        javaObject.setLocalSymbol(stringMapper.toJava(nativeObject.getLocalSymbol()));
        javaObject.setLocalDescription(
                stringMapper.toJava(nativeObject.getLocalDescription()));
        javaObject.setCountry(stringMapper.toJava(nativeObject.getCountry()));
        javaObject.setOPOL(stringMapper.toJava(nativeObject.getOpol()));
        javaObject.setExchangeData(stringMapper.toJava(nativeObject.getExchangeData()));
        javaObject.setExchanges(stringMapper.toJava(nativeObject.getExchanges()));
        javaObject.setCurrency(stringMapper.toJava(nativeObject.getCurrency()));
        javaObject.setBaseCurrency(stringMapper.toJava(nativeObject.getBaseCurrency()));
        javaObject.setCFI(stringMapper.toJava(nativeObject.getCfi()));
        javaObject.setISIN(stringMapper.toJava(nativeObject.getIsin()));
        javaObject.setSEDOL(stringMapper.toJava(nativeObject.getSedol()));
        javaObject.setCUSIP(stringMapper.toJava(nativeObject.getCusip()));
        javaObject.setICB(nativeObject.getIcb());
        javaObject.setSIC(nativeObject.getSic());
        javaObject.setMultiplier(nativeObject.getMultiplier());
        javaObject.setProduct(stringMapper.toJava(nativeObject.getProduct()));
        javaObject.setUnderlying(stringMapper.toJava(nativeObject.getUnderlying()));
        javaObject.setSPC(nativeObject.getSpc());
        javaObject.setAdditionalUnderlyings(
                stringMapper.toJava(nativeObject.getAdditionalUnderlyings()));
        javaObject.setMMY(stringMapper.toJava(nativeObject.getMmy()));
        javaObject.setExpiration(nativeObject.getExpiration());
        javaObject.setLastTrade(nativeObject.getLastTrade());
        javaObject.setStrike(nativeObject.getStrike());
        javaObject.setOptionType(stringMapper.toJava(nativeObject.getOptionType()));
        javaObject.setExpirationStyle(
                stringMapper.toJava(nativeObject.getExpirationStyle()));
        javaObject.setSettlementStyle(
                stringMapper.toJava(nativeObject.getSettlementStyle()));
        javaObject.setPriceIncrements(
                stringMapper.toJava(nativeObject.getPriceIncrements()));
        javaObject.setTradingHours(stringMapper.toJava(nativeObject.getTradingHours()));

        var nativeCustomFields = nativeObject.getInstrumentProfileCustomFields();

        if (nativeCustomFields.isNonNull()) {
            var javaCustomFields = NativeUtils.MAPPER_INSTRUMENT_PROFILE_CUSTOM_FIELDS.toJava(
                    nativeCustomFields);

            if (javaCustomFields != null) {
                javaCustomFields.assignTo(javaObject);
            }
        }
    }
}
