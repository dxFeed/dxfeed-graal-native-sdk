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
inline Command additionalUnderlyingsCase{
    "AdditionalUnderlyingsCase",
    {"au"},
    "",
    "au [<properties>]",
    {},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> & /*args*/,
       const dxfg::CommandsContext & /*context*/) {
        puts("== AdditionalUnderlyings ==");

        {
            puts("-- dxfg_AdditionalUnderlyings_EMPTY(..., !NULL) --");

            dxfg_additional_underlyings_t *additionalUnderlyings{};
            auto result = dxfg_AdditionalUnderlyings_EMPTY(isolateThread, &additionalUnderlyings);

            printf("  dxfg_AdditionalUnderlyings_EMPTY() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", additionalUnderlyings->handler.java_object_handle);
                dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        }

        {
            puts("-- dxfg_AdditionalUnderlyings_EMPTY(..., NULL) --");

            auto result = dxfg_AdditionalUnderlyings_EMPTY(isolateThread, nullptr);

            printf("  dxfg_AdditionalUnderlyings_EMPTY() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                puts(", The result is successful, but there must be an error.");
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        }

        auto run_dxfg_AdditionalUnderlyings_valueOf = [](graal_isolatethread_t *isolateThread, const char *text) {
            dxfg_additional_underlyings_t *additionalUnderlyings{};
            auto result = dxfg_AdditionalUnderlyings_valueOf(isolateThread, text, &additionalUnderlyings);

            printf("  dxfg_AdditionalUnderlyings_valueOf() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", additionalUnderlyings->handler.java_object_handle);
                dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- dxfg_AdditionalUnderlyings_valueOf(..., "FIS 53; US$ 45.46", !NULL) --)");
            run_dxfg_AdditionalUnderlyings_valueOf(isolateThread, "FIS 53; US$ 45.46");
        }

        {
            puts(R"(-- dxfg_AdditionalUnderlyings_valueOf(..., "", !NULL) --)");
            run_dxfg_AdditionalUnderlyings_valueOf(isolateThread, "");
        }

        {
            puts("-- dxfg_AdditionalUnderlyings_valueOf(..., NULL, !NULL) --");
            run_dxfg_AdditionalUnderlyings_valueOf(isolateThread, nullptr);
        }

        auto run_dxfg_AdditionalUnderlyings_valueOf2 = [](graal_isolatethread_t *isolateThread,
                                                          const dxfg_string_to_double_map_entry_t *mapEntries,
                                                          std::int32_t size) {
            dxfg_additional_underlyings_t *additionalUnderlyings{};
            auto result = dxfg_AdditionalUnderlyings_valueOf2(isolateThread, mapEntries, size, &additionalUnderlyings);

            printf("  dxfg_AdditionalUnderlyings_valueOf2(%p, %d) -> %d", mapEntries, size, result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", additionalUnderlyings->handler.java_object_handle);
                dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };
        {
            std::vector<dxfg_string_to_double_map_entry_t> mapEntries{{"FIS", 53}, {"US$", 45.46}, {"ABC", 123}};

            {
                puts(
                    R"(-- dxfg_AdditionalUnderlyings_valueOf2(..., {{"FIS", 53}, {"US$", 45.46}, {"ABC", 123}}, ...)--)");
                run_dxfg_AdditionalUnderlyings_valueOf2(isolateThread, mapEntries.data(),
                                                        static_cast<std::int32_t>(mapEntries.size()));
            }

            {
                std::vector<dxfg_string_to_double_map_entry_t> mapEntries2{{"FIS", 53}, {"US$", 45.46}, {"", 0}};

                puts(R"(-- dxfg_AdditionalUnderlyings_valueOf2(..., {{"FIS", 53}, {"US$", 45.46}, {"", 0}}, ...) --)");
                run_dxfg_AdditionalUnderlyings_valueOf2(isolateThread, mapEntries2.data(),
                                                        static_cast<std::int32_t>(mapEntries2.size()));
            }

            {
                puts(
                    R"(-- dxfg_AdditionalUnderlyings_valueOf2(..., {{"FIS", 53}, {"US$", 45.46}, {"ABC", 123}}, 0, ...) --)");
                run_dxfg_AdditionalUnderlyings_valueOf2(isolateThread, mapEntries.data(), 0);
            }

            {
                puts("-- dxfg_AdditionalUnderlyings_valueOf2(..., NULL, 0, ...) --");
                run_dxfg_AdditionalUnderlyings_valueOf2(isolateThread, nullptr, 0);
            }
        }

        auto run_dxfg_AdditionalUnderlyings_getSPC = [](graal_isolatethread_t *isolateThread, const char *text,
                                                        const char *symbol) {
            double spc = 0.0;
            int32_t result = dxfg_AdditionalUnderlyings_getSPC(isolateThread, text, symbol, &spc);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  spc = %g\n", spc);
            } else {
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., "FIS 53; US$ 45.46", "US$", ...) --)");
            run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, "FIS 53; US$ 45.46", "US$");
        }

        {
            puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., "FIS 53; USB 45.46", "US$", ...) --)");
            run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, "FIS 53; USB 45.46", "US$");
        }

        {
            puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., "FIS 53; US$ 45.46", "", ...) --)");
            run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, "FIS 53; US$ 45.46", "");
        }

        {
            puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., "FIS 53; US$ 45.46", nullptr, ...) --)");
            run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, "FIS 53; US$ 45.46", nullptr);
        }

        {
            puts(R"(-- dxfg_AdditionalUnderlyings_getSPC(..., nullptr, nullptr, ...) --)");
            run_dxfg_AdditionalUnderlyings_getSPC(isolateThread, nullptr, nullptr);
        }

        do {
            puts(R"(-- valueOf("FIS 53; US$ 45.46") + getText() --)");
            dxfg_additional_underlyings_t *additionalUnderlyings{};
            auto result =
                dxfg_AdditionalUnderlyings_valueOf(isolateThread, "FIS 53; US$ 45.46", &additionalUnderlyings);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", additionalUnderlyings->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            char *text = {};
            result = dxfg_AdditionalUnderlyings_getText(isolateThread, additionalUnderlyings, &text);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  text = %s\n", text);

                dxfg_String_release(isolateThread, text);
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
        } while (false);

        do {
            puts(R"(-- valueOf("FIS 53; US$ 45.46") + getMap() --)");
            dxfg_additional_underlyings_t *additionalUnderlyings{};
            auto result =
                dxfg_AdditionalUnderlyings_valueOf(isolateThread, "FIS 53; US$ 45.46", &additionalUnderlyings);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", additionalUnderlyings->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            dxfg_string_to_double_map_entry_t *mapEntries = {};
            std::int32_t size = {};

            result = dxfg_AdditionalUnderlyings_getMap(isolateThread, additionalUnderlyings, &mapEntries, &size);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                puts("  map entries:");
                for (int i = 0; i < size; i++) {
                    printf("  [%d]: key = '%s', value = %g\n", i, mapEntries[i].key, mapEntries[i].value);
                }

                dxfg_free_string_to_double_map_entries(isolateThread, mapEntries, size);
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);
        } while (false);

        do {
            puts(R"(-- valueOf("FIS 53; US$ 45.46") + getSPC2("US$") --)");
            dxfg_additional_underlyings_t *additionalUnderlyings{};
            auto result =
                dxfg_AdditionalUnderlyings_valueOf(isolateThread, "FIS 53; US$ 45.46", &additionalUnderlyings);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", additionalUnderlyings->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            double spc = 0.0;

            result = dxfg_AdditionalUnderlyings_getSPC2(isolateThread, additionalUnderlyings, "US$", &spc);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  spc = %g\n", spc);
            } else {
                getException(isolateThread);
            }

            dxfg_java_object_handler *clone = {};

            dxfg_JavaObjectHandler_clone(isolateThread,
                                         reinterpret_cast<dxfg_java_object_handler *>(additionalUnderlyings), &clone);
            dxfg_JavaObjectHandler_release(isolateThread, &additionalUnderlyings->handler);

            result = dxfg_AdditionalUnderlyings_getSPC2(
                isolateThread, reinterpret_cast<dxfg_additional_underlyings_t *>(clone), "FIS", &spc);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  spc = %g\n", spc);
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, clone);
        } while (false);
    }};
} // namespace dxfg