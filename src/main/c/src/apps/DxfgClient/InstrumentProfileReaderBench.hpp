// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "Common.hpp"

#include <dxfg_api.h>

#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <chrono>
#include <cstdio>
#include <deque>
#include <functional>
#include <future>
#include <string>
#include <vector>

namespace dxfg {

inline std::size_t getHash(graal_isolatethread_t *isolateThread, dxfg_instrument_profile_t *profile) {
    std::size_t result = 0;

    auto type = dxfg_InstrumentProfile_getType(isolateThread, profile);
    auto symbol = dxfg_InstrumentProfile_getSymbol(isolateThread, profile);
    auto description = dxfg_InstrumentProfile_getDescription(isolateThread, profile);
    auto local_symbol = dxfg_InstrumentProfile_getLocalSymbol(isolateThread, profile);
    auto local_description = dxfg_InstrumentProfile_getLocalDescription(isolateThread, profile);
    auto country = dxfg_InstrumentProfile_getCountry(isolateThread, profile);
    auto opol = dxfg_InstrumentProfile_getOPOL(isolateThread, profile);
    auto exchange_data = dxfg_InstrumentProfile_getExchangeData(isolateThread, profile);
    auto exchanges = dxfg_InstrumentProfile_getExchanges(isolateThread, profile);
    auto currency = dxfg_InstrumentProfile_getCurrency(isolateThread, profile);
    auto base_currency = dxfg_InstrumentProfile_getBaseCurrency(isolateThread, profile);
    auto cfi = dxfg_InstrumentProfile_getCFI(isolateThread, profile);
    auto isin = dxfg_InstrumentProfile_getISIN(isolateThread, profile);
    auto sedol = dxfg_InstrumentProfile_getSEDOL(isolateThread, profile);
    auto cusip = dxfg_InstrumentProfile_getCUSIP(isolateThread, profile);
    auto icb = dxfg_InstrumentProfile_getICB(isolateThread, profile);
    auto sic = dxfg_InstrumentProfile_getSIC(isolateThread, profile);
    auto multiplier = dxfg_InstrumentProfile_getMultiplier(isolateThread, profile);
    auto product = dxfg_InstrumentProfile_getProduct(isolateThread, profile);
    auto underlying = dxfg_InstrumentProfile_getUnderlying(isolateThread, profile);
    auto spc = dxfg_InstrumentProfile_getSPC(isolateThread, profile);
    auto additional_underlyings = dxfg_InstrumentProfile_getAdditionalUnderlyings(isolateThread, profile);
    auto mmy = dxfg_InstrumentProfile_getMMY(isolateThread, profile);
    auto expiration = dxfg_InstrumentProfile_getExpiration(isolateThread, profile);
    auto last_trade = dxfg_InstrumentProfile_getLastTrade(isolateThread, profile);
    auto strike = dxfg_InstrumentProfile_getStrike(isolateThread, profile);
    auto option_type = dxfg_InstrumentProfile_getOptionType(isolateThread, profile);
    auto expiration_style = dxfg_InstrumentProfile_getExpirationStyle(isolateThread, profile);
    auto settlement_style = dxfg_InstrumentProfile_getSettlementStyle(isolateThread, profile);
    auto price_increments = dxfg_InstrumentProfile_getPriceIncrements(isolateThread, profile);
    auto trading_hours = dxfg_InstrumentProfile_getTradingHours(isolateThread, profile);

    hashCombine(result, type);
    hashCombine(result, symbol);
    hashCombine(result, description);
    hashCombine(result, local_symbol);
    hashCombine(result, local_description);
    hashCombine(result, country);
    hashCombine(result, opol);
    hashCombine(result, exchange_data);
    hashCombine(result, exchanges);
    hashCombine(result, currency);
    hashCombine(result, base_currency);
    hashCombine(result, cfi);
    hashCombine(result, isin);
    hashCombine(result, sedol);
    hashCombine(result, cusip);
    hashCombine(result, icb);
    hashCombine(result, sic);
    hashCombine(result, multiplier);
    hashCombine(result, product);
    hashCombine(result, underlying);
    hashCombine(result, spc);
    hashCombine(result, additional_underlyings);
    hashCombine(result, mmy);
    hashCombine(result, expiration);
    hashCombine(result, last_trade);
    hashCombine(result, strike);
    hashCombine(result, option_type);
    hashCombine(result, expiration_style);
    hashCombine(result, settlement_style);
    hashCombine(result, price_increments);
    hashCombine(result, trading_hours);

    dxfg_String_release(isolateThread, trading_hours);
    dxfg_String_release(isolateThread, price_increments);
    dxfg_String_release(isolateThread, settlement_style);
    dxfg_String_release(isolateThread, expiration_style);
    dxfg_String_release(isolateThread, option_type);
    dxfg_String_release(isolateThread, mmy);
    dxfg_String_release(isolateThread, additional_underlyings);
    dxfg_String_release(isolateThread, underlying);
    dxfg_String_release(isolateThread, product);
    dxfg_String_release(isolateThread, cusip);
    dxfg_String_release(isolateThread, sedol);
    dxfg_String_release(isolateThread, isin);
    dxfg_String_release(isolateThread, cfi);
    dxfg_String_release(isolateThread, base_currency);
    dxfg_String_release(isolateThread, currency);
    dxfg_String_release(isolateThread, exchanges);
    dxfg_String_release(isolateThread, exchange_data);
    dxfg_String_release(isolateThread, opol);
    dxfg_String_release(isolateThread, country);
    dxfg_String_release(isolateThread, local_description);
    dxfg_String_release(isolateThread, local_symbol);
    dxfg_String_release(isolateThread, description);
    dxfg_String_release(isolateThread, symbol);
    dxfg_String_release(isolateThread, type);

    return result;
}

inline std::size_t getHash(const dxfg_instrument_profile2_t &profile) {
    std::size_t result = 0;

    hashCombine(result, profile.type);
    hashCombine(result, profile.symbol);
    hashCombine(result, profile.description);
    hashCombine(result, profile.local_symbol);
    hashCombine(result, profile.local_description);
    hashCombine(result, profile.country);
    hashCombine(result, profile.opol);
    hashCombine(result, profile.exchange_data);
    hashCombine(result, profile.exchanges);
    hashCombine(result, profile.currency);
    hashCombine(result, profile.base_currency);
    hashCombine(result, profile.cfi);
    hashCombine(result, profile.isin);
    hashCombine(result, profile.sedol);
    hashCombine(result, profile.cusip);
    hashCombine(result, profile.icb);
    hashCombine(result, profile.sic);
    hashCombine(result, profile.multiplier);
    hashCombine(result, profile.product);
    hashCombine(result, profile.underlying);
    hashCombine(result, profile.spc);
    hashCombine(result, profile.additional_underlyings);
    hashCombine(result, profile.mmy);
    hashCombine(result, profile.expiration);
    hashCombine(result, profile.last_trade);
    hashCombine(result, profile.strike);
    hashCombine(result, profile.option_type);
    hashCombine(result, profile.expiration_style);
    hashCombine(result, profile.settlement_style);
    hashCombine(result, profile.price_increments);
    hashCombine(result, profile.trading_hours);
    hashCombine(result, profile.instrument_profile_custom_fields_hash);

    return result;
}

inline Command instrumentProfileReaderBench{
    "InstrumentProfileReaderBench",
    {"ipf"},
    "",
    "ipf [<properties>] <version> <pathToIpf>",
    {"ipf 1 ./options.ipf", "ipf 2 ./options.ipf", "ipf 2c ./options.ipf", "ipf 3 ./options.ipf"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &) {
        using namespace std::chrono_literals;

        puts("== InstrumentProfileReaderBench ==");

        if (args.size() >= 2) {
            auto reader = dxfg_InstrumentProfileReader_new(isolateThread);

            if (reader == nullptr) {
                getException(isolateThread);

                return;
            }

            StopWatch stopWatch{};

            if (args[0] == "1") {
                stopWatch.start();
                auto profileList = dxfg_InstrumentProfileReader_readFromFile(isolateThread, reader, args[1].c_str());
                stopWatch.stop();

                if (profileList == nullptr) {
                    getException(isolateThread);
                    dxfg_JavaObjectHandler_release(isolateThread, &reader->handler);

                    return;
                }

                printf("IPF loading in %lldms\n", stopWatch.elapsed().count());
                printf("Instrument count: %d\n", profileList->size);

                for (int t = 0; t < 15; ++t) {
                    printf("Rehashing...\n");
                    stopWatch.restart();

                    std::size_t hash = 0;
                    for (std::int32_t i = 0; i < profileList->size; i++) {
                        hashCombine(hash, getHash(isolateThread, profileList->elements[i]));
                    }

                    stopWatch.stop();
                    printf("hash = %zu\n", hash);
                    printf("elapsed = %lldms\n", stopWatch.elapsed().count());
                }

                dxfg_CList_InstrumentProfile_release(isolateThread, profileList);

            } else if (args[0] == "2") {
                dxfg_instrument_profile2_t *profiles{};
                std::int32_t size{};

                stopWatch.start();
                auto r = dxfg_InstrumentProfileReader_readFromFile4(isolateThread, reader, args[1].c_str(), &profiles,
                                                                      &size);
                stopWatch.stop();

                if (r == DXFG_EXECUTE_SUCCESSFULLY) {
                    printf("IPF loading in %lldms\n", stopWatch.elapsed().count());
                    printf("Instrument count: %d\n", size);

                    for (int t = 0; t < 15; ++t) {
                        printf("Rehashing...\n");
                        stopWatch.restart();

                        std::size_t hash = 0;
                        for (std::int32_t i = 0; i < size; i++) {
                            hashCombine(hash, getHash(profiles[i]));
                        }

                        stopWatch.stop();
                        printf("hash = %zu\n", hash);
                        printf("elapsed = %lldms\n", stopWatch.elapsed().count());
                    }

                    dxfg_instrument_profiles_array_free(isolateThread, profiles, size);
                }

            } else if (args[0] == "3") {
                dxfg_instrument_profile2_list_t *profiles{};

                stopWatch.start();
                auto r = dxfg_InstrumentProfileReader_readFromFile7(isolateThread, reader, args[1].c_str(), &profiles);
                stopWatch.stop();

                if (r == DXFG_EXECUTE_SUCCESSFULLY) {
                    printf("IPF loading in %lldms\n", stopWatch.elapsed().count());
                    printf("Instrument count: %d\n", profiles->size);

                    for (int t = 0; t < 15; ++t) {
                        printf("Rehashing...\n");
                        stopWatch.restart();

                        std::size_t hash = 0;
                        for (std::int32_t i = 0; i < profiles->size; i++) {
                            hashCombine(hash, getHash(*profiles->elements[i]));
                        }

                        stopWatch.stop();
                        printf("hash = %zu\n", hash);
                        printf("elapsed = %lldms\n", stopWatch.elapsed().count());
                    }

                    dxfg_instrument_profile2_list_free(isolateThread, profiles);
                }
            }

            dxfg_JavaObjectHandler_release(isolateThread, &reader->handler);
        }
    }};
} // namespace dxfg