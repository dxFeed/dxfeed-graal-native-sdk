// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgProfile;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class ProfileMapper extends MarketEventMapper<Profile, DxfgProfile> {

    public ProfileMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public DxfgProfile createNativeObject() {
        final DxfgProfile nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgProfile.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_PROFILE.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_PROFILE;
    }

    @Override
    public void fillNative(final Profile javaObject, final DxfgProfile nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setDescription(stringMapper.toNative(javaObject.getDescription()));
        nativeObject.setStatusReason(stringMapper.toNative(javaObject.getStatusReason()));
        nativeObject.setHaltStartTime(javaObject.getHaltStartTime());
        nativeObject.setHaltEndTime(javaObject.getHaltEndTime());
        nativeObject.setHighLimitPrice(javaObject.getHighLimitPrice());
        nativeObject.setLowLimitPrice(javaObject.getLowLimitPrice());
        nativeObject.setHigh52WeekPrice(javaObject.getHigh52WeekPrice());
        nativeObject.setLow52WeekPrice(javaObject.getLow52WeekPrice());
        nativeObject.setBeta(javaObject.getBeta());
        nativeObject.setEarningsPerShare(javaObject.getEarningsPerShare());
        nativeObject.setDividendFrequency(javaObject.getDividendFrequency());
        nativeObject.setExDividendAmount(javaObject.getExDividendAmount());
        nativeObject.setExDividendDayId(javaObject.getExDividendDayId());
        nativeObject.setShares(javaObject.getShares());
        nativeObject.setFreeFloat(javaObject.getFreeFloat());
        nativeObject.setFlags(javaObject.getFlags());
    }

    @Override
    public void cleanNative(final DxfgProfile nativeObject) {
        super.cleanNative(nativeObject);
        stringMapper.release(nativeObject.getDescription());
        stringMapper.release(nativeObject.getStatusReason());
    }

    @Override
    protected Profile doToJava(final DxfgProfile nativeObject) {
        final Profile javaObject = new Profile();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgProfile nativeObject, final Profile javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setDescription(stringMapper.toJava(nativeObject.getDescription()));
        javaObject.setStatusReason(stringMapper.toJava(nativeObject.getStatusReason()));
        javaObject.setHaltStartTime(nativeObject.getHaltStartTime());
        javaObject.setHaltEndTime(nativeObject.getHaltEndTime());
        javaObject.setHighLimitPrice(nativeObject.getHighLimitPrice());
        javaObject.setLowLimitPrice(nativeObject.getLowLimitPrice());
        javaObject.setHigh52WeekPrice(nativeObject.getHigh52WeekPrice());
        javaObject.setLow52WeekPrice(nativeObject.getLow52WeekPrice());
        javaObject.setBeta(nativeObject.getBeta());
        javaObject.setEarningsPerShare(nativeObject.getEarningsPerShare());
        javaObject.setDividendFrequency(nativeObject.getDividendFrequency());
        javaObject.setExDividendAmount(nativeObject.getExDividendAmount());
        javaObject.setExDividendDayId(nativeObject.getExDividendDayId());
        javaObject.setShares(nativeObject.getShares());
        javaObject.setFreeFloat(nativeObject.getFreeFloat());
        javaObject.setFlags(nativeObject.getFlags());
    }
}
