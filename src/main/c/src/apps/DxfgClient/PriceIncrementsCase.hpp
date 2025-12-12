// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "Common.hpp"

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <cstdint>
#include <cstdio>
#include <string>
#include <vector>

namespace dxfg {
inline Command priceIncrementsCase{
    "PriceIncrementsCase",
    {"pi"},
    "",
    "pi [<properties>]",
    {},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> & /*args*/,
       const dxfg::CommandsContext & /*context*/) {
        puts("== PriceIncrements ==");

        {
            puts("-- dxfg_PriceIncrements_EMPTY(..., !NULL) --");

            dxfg_price_increments_t *emptyPriceIncrements{};
            auto result = dxfg_PriceIncrements_EMPTY(isolateThread, &emptyPriceIncrements);

            printf("  dxfg_PriceIncrements_EMPTY() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", emptyPriceIncrements->handler.java_object_handle);
                dxfg_JavaObjectHandler_release(isolateThread, &emptyPriceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        }

        {
            puts("-- dxfg_PriceIncrements_EMPTY(..., NULL) --");

            auto result = dxfg_PriceIncrements_EMPTY(isolateThread, nullptr);

            printf("  dxfg_PriceIncrements_EMPTY() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                puts(", The result is successful, but there must be an error.");
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        }

        auto run_dxfg_PriceIncrements_valueOf = [](graal_isolatethread_t *isolateThread, const char *text) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_valueOf() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                char *text2 = {};
                dxfg_PriceIncrements_getText(isolateThread, priceIncrements, &text2);

                printf("  text = '%s'\n", text2);

                dxfg_String_release(isolateThread, text2);

                puts("  price increments:");

                double *increments = {};
                std::int32_t size = 0;

                dxfg_PriceIncrements_getPriceIncrements(isolateThread, priceIncrements, &increments, &size);

                for (std::int32_t i = 0; i < size; i++) {
                    printf("    [%d] = %g\n", i, increments[i]);
                }

                dxfg_free(isolateThread, increments);
                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(
                R"(-- dxfg_PriceIncrements_valueOf(..., "0.01 20; 0.02 50; 0.05 100; 0.1 250; 0.25 500; 0.5 1000; 1 2500; 2.5", !NULL) --)");
            run_dxfg_PriceIncrements_valueOf(isolateThread,
                                             "0.01 20; 0.02 50; 0.05 100; 0.1 250; 0.25 500; 0.5 1000; 1 2500; 2.5");
        }

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf(..., "0.0001 1; 0.01", !NULL) --)");
            run_dxfg_PriceIncrements_valueOf(isolateThread, "0.0001 1; 0.01");
        }

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf(..., "0.01", !NULL) --)");
            run_dxfg_PriceIncrements_valueOf(isolateThread, "0.01");
        }

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf(..., "", !NULL) --)");
            run_dxfg_PriceIncrements_valueOf(isolateThread, "");
        }

        {
            puts("-- dxfg_PriceIncrements_valueOf(..., NULL, !NULL) --");
            run_dxfg_PriceIncrements_valueOf(isolateThread, nullptr);
        }

        auto run_dxfg_PriceIncrements_valueOf2 = [](graal_isolatethread_t *isolateThread, double increment) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf2(isolateThread, increment, &priceIncrements);

            printf("  dxfg_PriceIncrements_valueOf2() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                char *text = {};
                dxfg_PriceIncrements_getText(isolateThread, priceIncrements, &text);

                printf("  text = '%s'\n", text);

                dxfg_String_release(isolateThread, text);

                puts("  price increments:");

                double *increments = {};
                std::int32_t size = 0;

                dxfg_PriceIncrements_getPriceIncrements(isolateThread, priceIncrements, &increments, &size);

                for (std::int32_t i = 0; i < size; i++) {
                    printf("    [%d] = %g\n", i, increments[i]);
                }

                dxfg_free(isolateThread, increments);
                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf2(..., 0.01, ...) --)");
            run_dxfg_PriceIncrements_valueOf2(isolateThread, 0.01);
        }

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf2(..., -0.01, ...) --)");
            run_dxfg_PriceIncrements_valueOf2(isolateThread, -0.01);
        }

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf2(..., 0.0, ...) --)");
            run_dxfg_PriceIncrements_valueOf2(isolateThread, 0.0);
        }

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf2(..., NAN, ...) --)");
            run_dxfg_PriceIncrements_valueOf2(isolateThread, NAN);
        }

        {
            puts(R"(-- dxfg_PriceIncrements_valueOf2(..., INFINITY, ...) --)");
            run_dxfg_PriceIncrements_valueOf2(isolateThread, INFINITY);
        }

        auto run_dxfg_PriceIncrements_valueOf3 = [](graal_isolatethread_t *isolateThread, const double *increments,
                                                    int32_t size) {
            if (increments == nullptr) {
                return;
            }

            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf3(isolateThread, increments, size, &priceIncrements);

            printf("  dxfg_PriceIncrements_valueOf3() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                char *text = {};
                dxfg_PriceIncrements_getText(isolateThread, priceIncrements, &text);

                printf("  text = '%s'\n", text);

                dxfg_String_release(isolateThread, text);

                puts("  price increments:");

                double *increments2 = {};
                std::int32_t size2 = 0;

                dxfg_PriceIncrements_getPriceIncrements(isolateThread, priceIncrements, &increments2, &size2);

                for (std::int32_t i = 0; i < size; i++) {
                    printf("    [%d] = %g\n", i, increments[i]);
                }

                dxfg_free(isolateThread, increments2);
                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            std::vector<double> increments = {0.01, 20,  0.02, 50,   0.05, 100,  0.1, 250,
                                              0.25, 500, 0.5,  1000, 1,    2500, 2.5};

            {
                puts(
                    R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], ...) --)");
                run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(),
                                                  static_cast<int32_t>(increments.size()));
            }

            {
                puts(
                    R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], 1 ...) --)");
                run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(), 1);
            }

            {
                puts(
                    R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], 2 ...) --)");
                run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(), 2);
            }

            {
                puts(
                    R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], 3 ...) --)");
                run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(), 3);
            }

            {
                puts(
                    R"(-- dxfg_PriceIncrements_valueOf3(..., [0.01, 20, 0.02, 50, 0.05, 100, 0.1, 250, 0.25, 500, 0.5, 1000, 1, 2500, 2.5], 0 ...) --)");
                run_dxfg_PriceIncrements_valueOf3(isolateThread, increments.data(), 0);
            }

            {
                puts(R"(-- dxfg_PriceIncrements_valueOf3(..., NULL, 0 ...) --)");
                run_dxfg_PriceIncrements_valueOf3(isolateThread, nullptr, 0);
            }
        }

        auto run_dxfg_PriceIncrements_getPriceIncrement = [](graal_isolatethread_t *isolateThread, const char *text) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_getPriceIncrement() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                double priceIncrement = 0.0;

                dxfg_PriceIncrements_getPriceIncrement(isolateThread, priceIncrements, &priceIncrement);

                printf("  priceIncrement = %g\n", priceIncrement);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement() --)");
            run_dxfg_PriceIncrements_getPriceIncrement(isolateThread, "0.0001 1; 0.01");
        }

        {
            puts(R"(-- valueOf(..., "", ...).getPriceIncrement() --)");
            run_dxfg_PriceIncrements_getPriceIncrement(isolateThread, "");
        }

        auto run_dxfg_PriceIncrements_getPriceIncrement2 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                              double price) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_getPriceIncrement2() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                double priceIncrement = 0.0;

                dxfg_PriceIncrements_getPriceIncrement2(isolateThread, priceIncrements, price, &priceIncrement);

                printf("  priceIncrement = %g\n", priceIncrement);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement2(23.42) --)");
            run_dxfg_PriceIncrements_getPriceIncrement2(isolateThread, "0.0001 1; 0.01", 23.42);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement2(1.01) --)");
            run_dxfg_PriceIncrements_getPriceIncrement2(isolateThread, "0.0001 1; 0.01", 1.01);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement2(1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement2(isolateThread, "0.0001 1; 0.01", 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement2(0.005) --)");
            run_dxfg_PriceIncrements_getPriceIncrement2(isolateThread, "0.0001 1; 0.01", 0.005);
        }

        auto run_dxfg_PriceIncrements_getPriceIncrement3 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                              double price, int32_t direction) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_getPriceIncrement3() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                double priceIncrement = 0.0;

                dxfg_PriceIncrements_getPriceIncrement3(isolateThread, priceIncrements, price, direction,
                                                        &priceIncrement);

                printf("  priceIncrement = %g\n", priceIncrement);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(23.42, 1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 23.42, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(1.01, 1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 1.01, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(1, 1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 1, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(0.005, 1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 0.005, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(23.42, -1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 23.42, -1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(1.01, -1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 1.01, -1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(1, -1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 1, -1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPriceIncrement3(0.005, -1) --)");
            run_dxfg_PriceIncrements_getPriceIncrement3(isolateThread, "0.0001 1; 0.01", 0.005, -1);
        }

        auto run_dxfg_PriceIncrements_getPricePrecision = [](graal_isolatethread_t *isolateThread, const char *text) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_getPricePrecision() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                int32_t pricePrecision = 0;

                dxfg_PriceIncrements_getPricePrecision(isolateThread, priceIncrements, &pricePrecision);

                printf("  pricePrecision = %d\n", pricePrecision);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPricePrecision() --)");
            run_dxfg_PriceIncrements_getPricePrecision(isolateThread, "0.0001 1; 0.01");
        }

        {
            puts(R"(-- valueOf(..., "0.01", ...).getPricePrecision() --)");
            run_dxfg_PriceIncrements_getPricePrecision(isolateThread, "0.01");
        }

        {
            puts(R"(-- valueOf(..., "", ...).getPricePrecision() --)");
            run_dxfg_PriceIncrements_getPricePrecision(isolateThread, "");
        }

        auto run_dxfg_PriceIncrements_getPricePrecision2 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                              double price) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_getPricePrecision2() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                int32_t pricePrecision = 0;

                dxfg_PriceIncrements_getPricePrecision2(isolateThread, priceIncrements, price, &pricePrecision);

                printf("  pricePrecision = %d\n", pricePrecision);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPricePrecision2(0.1) --)");
            run_dxfg_PriceIncrements_getPricePrecision2(isolateThread, "0.0001 1; 0.01", 0.1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).getPricePrecision2(2) --)");
            run_dxfg_PriceIncrements_getPricePrecision2(isolateThread, "0.0001 1; 0.01", 2);
        }

        auto run_dxfg_PriceIncrements_roundPrice = [](graal_isolatethread_t *isolateThread, const char *text,
                                                      double price) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_roundPrice() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                double roundedPrice = 0.0;

                dxfg_PriceIncrements_roundPrice(isolateThread, priceIncrements, price, &roundedPrice);

                printf("  roundedPrice = %g\n", roundedPrice);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice(23.426) --)");
            run_dxfg_PriceIncrements_roundPrice(isolateThread, "0.0001 1; 0.01", 23.426);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice(1.0178) --)");
            run_dxfg_PriceIncrements_roundPrice(isolateThread, "0.0001 1; 0.01", 1.0178);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice(0.013321) --)");
            run_dxfg_PriceIncrements_roundPrice(isolateThread, "0.0001 1; 0.01", 0.013321);
        }

        auto run_dxfg_PriceIncrements_roundPrice2 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                       double price, int32_t direction) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_roundPrice2() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                double roundedPrice = 0.0;

                dxfg_PriceIncrements_roundPrice2(isolateThread, priceIncrements, price, direction, &roundedPrice);

                printf("  roundedPrice = %g\n", roundedPrice);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(23.426, 1) --)");
            run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 23.426, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(1.0178, 1) --)");
            run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 1.0178, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(0.013321, 1) --)");
            run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 0.013321, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(23.426, -1) --)");
            run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 23.426, -1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(1.0178, -1) --)");
            run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 1.0178, -1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(0.013321, -1) --)");
            run_dxfg_PriceIncrements_roundPrice2(isolateThread, "0.0001 1; 0.01", 0.013321, -1);
        }

        auto run_dxfg_PriceIncrements_roundPrice3 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                       double price, dxfg_rounding_mode_t roundingMode) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_roundPrice3() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                double roundedPrice = 0.0;

                dxfg_PriceIncrements_roundPrice3(isolateThread, priceIncrements, price, roundingMode, &roundedPrice);

                printf("  roundedPrice = %g\n", roundedPrice);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(23.426, DXFG_ROUNDING_MODE_HALF_UP) --)");
            run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 23.426, DXFG_ROUNDING_MODE_HALF_UP);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(1.0178, DXFG_ROUNDING_MODE_DOWN) --)");
            run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 1.0178, DXFG_ROUNDING_MODE_DOWN);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(0.013321, DXFG_ROUNDING_MODE_UP) --)");
            run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 0.013321, DXFG_ROUNDING_MODE_UP);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(23.426, DXFG_ROUNDING_MODE_FLOOR) --)");
            run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 23.426, DXFG_ROUNDING_MODE_FLOOR);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(1.0178, DXFG_ROUNDING_MODE_CEILING) --)");
            run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 1.0178, DXFG_ROUNDING_MODE_CEILING);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).roundPrice2(0.013321, DXFG_ROUNDING_MODE_HALF_EVEN) --)");
            run_dxfg_PriceIncrements_roundPrice3(isolateThread, "0.0001 1; 0.01", 0.013321,
                                                 DXFG_ROUNDING_MODE_HALF_EVEN);
        }

        auto run_dxfg_PriceIncrements_incrementPrice = [](graal_isolatethread_t *isolateThread, const char *text,
                                                          double price, int32_t direction) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_incrementPrice() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                double incrementedPrice = 0.0;

                dxfg_PriceIncrements_incrementPrice(isolateThread, priceIncrements, price, direction,
                                                    &incrementedPrice);

                printf("  incrementedPrice = %g\n", incrementedPrice);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(23.426, 1) --)");
            run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 23.426, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(1.0178, 1) --)");
            run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 1.0178, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(0.013321, 1) --)");
            run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 0.013321, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(23.426, -1) --)");
            run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 23.426, -1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(1.0178, -1) --)");
            run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 1.0178, -1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice(0.013321, -1) --)");
            run_dxfg_PriceIncrements_incrementPrice(isolateThread, "0.0001 1; 0.01", 0.013321, -1);
        }

        auto run_dxfg_PriceIncrements_incrementPrice2 = [](graal_isolatethread_t *isolateThread, const char *text,
                                                           double price, int32_t direction, double step) {
            dxfg_price_increments_t *priceIncrements{};
            auto result = dxfg_PriceIncrements_valueOf(isolateThread, text, &priceIncrements);

            printf("  dxfg_PriceIncrements_incrementPrice2() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", priceIncrements->handler.java_object_handle);

                double incrementedPrice = 0.0;

                dxfg_PriceIncrements_incrementPrice2(isolateThread, priceIncrements, price, direction, step,
                                                     &incrementedPrice);

                printf("  incrementedPrice = %g\n", incrementedPrice);

                dxfg_JavaObjectHandler_release(isolateThread, &priceIncrements->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(23.426, 1, 0.5) --)");
            run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 23.426, 1, 0.5);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(1.0178, 1, 1) --)");
            run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 1.0178, 1, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(0.013321, 1, 2) --)");
            run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 0.013321, 1, 2);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(23.426, -1, 1) --)");
            run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 23.426, -1, 1);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(1.0178, -1, 2) --)");
            run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 1.0178, -1, 2);
        }

        {
            puts(R"(-- valueOf(..., "0.0001 1; 0.01", ...).incrementPrice2(0.013321, -1, 10) --)");
            run_dxfg_PriceIncrements_incrementPrice2(isolateThread, "0.0001 1; 0.01", 0.013321, -1, 10);
        }
    }};
} // namespace dxfg
