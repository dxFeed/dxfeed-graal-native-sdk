// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <cstdint>
#include <cstdio>
#include <string>
#include <vector>

void getException(graal_isolatethread_t *isolateThread);

namespace dxfg {
Command cfiCase{
    "CfiCase",
    {"cfi"},
    "",
    "cfi [<properties>]",
    {},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> & /*args*/,
       const dxfg::CommandsContext & /*context*/) {
        puts("== CFI + Value + Attribute ==");

        {
            puts("-- dxfg_CFI_EMPTY(..., !NULL) --");

            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_EMPTY(isolateThread, &cfi);

            printf("  dxfg_CFI_EMPTY() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", cfi->handler.java_object_handle);
                dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        }

        {
            puts("-- dxfg_CFI_EMPTY(..., NULL) --");

            auto result = dxfg_CFI_EMPTY(isolateThread, nullptr);

            printf("  dxfg_CFI_EMPTY() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                puts(", The result is successful, but there must be an error.");
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        }

        auto run_dxfg_CFI_valueOf = [](graal_isolatethread_t *isolateThread, const char *text) {
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf(isolateThread, text, &cfi);

            printf("  dxfg_CFI_valueOf() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", cfi->handler.java_object_handle);

                char *code = {};
                dxfg_CFI_getCode(isolateThread, cfi, &code);

                printf("  code = %s\n", code);

                dxfg_String_release(isolateThread, code);

                std::int32_t intCode = 0;

                dxfg_CFI_getIntCode(isolateThread, cfi, &intCode);

                printf("  intCode = %d\n", intCode);

                dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- dxfg_CFI_valueOf(..., "OPEFPS", !NULL) --)");
            run_dxfg_CFI_valueOf(isolateThread, "OPEFPS");
        }

        {
            puts(R"(-- dxfg_CFI_valueOf(..., "", !NULL) --)");
            run_dxfg_CFI_valueOf(isolateThread, "");
        }

        {
            puts("-- dxfg_CFI_valueOf(..., NULL, !NULL) --");
            run_dxfg_CFI_valueOf(isolateThread, nullptr);
        }

        auto run_dxfg_CFI_valueOf2 = [](graal_isolatethread_t *isolateThread, std::int32_t intCode) {
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf2(isolateThread, intCode, &cfi);

            printf("  dxfg_CFI_valueOf2() -> %d", result);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf(", %p\n", cfi->handler.java_object_handle);

                char *code = {};
                dxfg_CFI_getCode(isolateThread, cfi, &code);

                printf("  code = %s\n", code);

                dxfg_String_release(isolateThread, code);

                std::int32_t intCode2 = 0;

                dxfg_CFI_getIntCode(isolateThread, cfi, &intCode2);

                printf("  intCode = %d\n", intCode2);

                dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
            } else {
                putchar('\n');
                getException(isolateThread);
            }
        };

        {
            puts(R"(-- dxfg_CFI_valueOf2(..., 520264211, ...) --)");
            run_dxfg_CFI_valueOf2(isolateThread, 520264211);
        }

        {
            puts(R"(-- dxfg_CFI_valueOf2(..., 831283992, ...) --)");
            run_dxfg_CFI_valueOf2(isolateThread, 831283992);
        }

        {
            puts(R"(-- dxfg_CFI_valueOf2(..., 42, ...) --)");
            run_dxfg_CFI_valueOf2(isolateThread, 42);
        }

        {
            puts(R"(-- dxfg_CFI_valueOf2(..., 0, ...) --)");
            run_dxfg_CFI_valueOf2(isolateThread, 0);
        }

        {
            puts(R"(-- dxfg_CFI_valueOf2(..., -42, ...) --)");
            run_dxfg_CFI_valueOf2(isolateThread, -42);
        }

        do {
            puts(R"(-- valueOf2(520264211) + getCode() --)");
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf2(isolateThread, 520264211, &cfi);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", cfi->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            char *code = {};
            result = dxfg_CFI_getCode(isolateThread, cfi, &code);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  code = %s\n", code);

                dxfg_String_release(isolateThread, code);
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } while (false);

        do {
            puts(R"(-- valueOf("OPEFPS") + getIntCode() --)");
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", cfi->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            int32_t intCode = 0;
            result = dxfg_CFI_getIntCode(isolateThread, cfi, &intCode);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  intCode = %d\n", intCode);
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } while (false);

        do {
            puts(R"(-- valueOf("OPEFPS") + getCategory() --)");
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", cfi->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            int16_t category = 0;
            result = dxfg_CFI_getCategory(isolateThread, cfi, &category);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  category = 0x%x\n", category);
                printf("  category = '%c'\n", static_cast<char>(static_cast<uint16_t>(category) & 0xFF));
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } while (false);

        do {
            puts(R"(-- valueOf("OPEFPS") + getGroup() --)");
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", cfi->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            int16_t group = 0;
            result = dxfg_CFI_getGroup(isolateThread, cfi, &group);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  group = 0x%x\n", group);
                printf("  group = '%c'\n", static_cast<char>(static_cast<uint16_t>(group) & 0xFF));
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } while (false);

        do {
            puts(R"(-- valueOf("OPEFPS") + isEquity() .. isOther() --)");
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", cfi->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            int32_t isEquity = 0;
            int32_t isDebtInstrument = 0;
            int32_t isEntitlement = 0;
            int32_t isOption = 0;
            int32_t isFuture = 0;
            int32_t isOther = 0;

            result = dxfg_CFI_isEquity(isolateThread, cfi, &isEquity);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  isEquity = %d\n", isEquity);
            } else {
                getException(isolateThread);
            }

            result = dxfg_CFI_isDebtInstrument(isolateThread, cfi, &isDebtInstrument);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  isDebtInstrument = %d\n", isDebtInstrument);
            } else {
                getException(isolateThread);
            }

            result = dxfg_CFI_isEntitlement(isolateThread, cfi, &isEntitlement);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  isEntitlement = %d\n", isEntitlement);
            } else {
                getException(isolateThread);
            }

            result = dxfg_CFI_isOption(isolateThread, cfi, &isOption);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  isOption = %d\n", isOption);
            } else {
                getException(isolateThread);
            }

            result = dxfg_CFI_isFuture(isolateThread, cfi, &isFuture);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  isFuture = %d\n", isFuture);
            } else {
                getException(isolateThread);
            }

            result = dxfg_CFI_isOther(isolateThread, cfi, &isOther);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  isOther = %d\n", isOther);
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } while (false);

        do {
            puts(R"(-- valueOf("OPEFPS") + decipher() + Value_getCode() .. + getAttribute() --)");
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", cfi->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            dxfg_cfi_value_t *values = {};
            int32_t size = 0;
            result = dxfg_CFI_decipher(isolateThread, cfi, &values, &size);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                if (values != nullptr && size > 0) {
                    printf("  values:\n");

                    dxfg_cfi_value_t *cloneOfOptionsCfiValue = {};

                    for (int32_t i = 0; i < size; i++) {
                        auto cfiValue = reinterpret_cast<dxfg_cfi_value_t *>(&values[i].handler);

                        int16_t code = {};
                        char *name = {};
                        char *description = {};
                        dxfg_CFI_Value_getCode(isolateThread, cfiValue, &code);
                        dxfg_CFI_Value_getName(isolateThread, cfiValue, &name);
                        dxfg_CFI_Value_getDescription(isolateThread, cfiValue, &description);

                        char charCode = static_cast<char>(static_cast<uint16_t>(code) & 0xFF);

                        if (charCode == 'O') {
                            dxfg_JavaObjectHandler_clone(
                                isolateThread, reinterpret_cast<dxfg_java_object_handler *>(cfiValue),
                                reinterpret_cast<dxfg_java_object_handler **>(&cloneOfOptionsCfiValue));
                        }

                        printf("    [%d] = {code = '%c', name = '%s', desc = '%s'}\n", i, charCode, name, description);

                        dxfg_String_release(isolateThread, name);
                        dxfg_String_release(isolateThread, description);
                    }

                    dxfg_JavaObjectHandler_array_release(
                        isolateThread, reinterpret_cast<const dxfg_java_object_handler **>(values), size);

                    if (cloneOfOptionsCfiValue != nullptr) {
                        int16_t code = {};
                        char *name = {};
                        char *description = {};
                        dxfg_CFI_Value_getCode(isolateThread, cloneOfOptionsCfiValue, &code);
                        dxfg_CFI_Value_getName(isolateThread, cloneOfOptionsCfiValue, &name);
                        dxfg_CFI_Value_getDescription(isolateThread, cloneOfOptionsCfiValue, &description);

                        char charCode = static_cast<char>(static_cast<uint16_t>(code) & 0xFF);

                        printf("\n  CloneOfOptionsCfiValue{code = '%c', name = '%s', desc = '%s'}\n", charCode, name,
                               description);

                        dxfg_String_release(isolateThread, name);
                        dxfg_String_release(isolateThread, description);

                        dxfg_cfi_attribute_t *attribute = {};

                        dxfg_CFI_Value_getAttribute(isolateThread, cloneOfOptionsCfiValue, &attribute);

                        char *attributeName = {};
                        char *attributeDescription = {};

                        dxfg_cfi_value_t *attributeValues = {};
                        int32_t attributeValuesSize = 0;

                        dxfg_CFI_Attribute_getName(isolateThread, attribute, &attributeName);
                        dxfg_CFI_Attribute_getDescription(isolateThread, attribute, &attributeDescription);
                        dxfg_CFI_Attribute_getValues(isolateThread, attribute, &attributeValues, &attributeValuesSize);

                        printf("    attribute = {name = '%s', desc = '%s', values[%d]}\n", attributeName,
                               attributeDescription, attributeValuesSize);

                        dxfg_String_release(isolateThread, attributeName);
                        dxfg_String_release(isolateThread, attributeDescription);
                        dxfg_JavaObjectHandler_array_release(
                            isolateThread, reinterpret_cast<const dxfg_java_object_handler **>(attributeValues), size);
                        dxfg_JavaObjectHandler_release(isolateThread, &attribute->handler);
                        dxfg_JavaObjectHandler_release(isolateThread, &cloneOfOptionsCfiValue->handler);
                    }
                }

            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } while (false);

        do {
            puts(R"(-- valueOf("OPEFPS") + describe() --)");
            dxfg_cfi_t *cfi{};
            auto result = dxfg_CFI_valueOf(isolateThread, "OPEFPS", &cfi);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  handle = %p\n", cfi->handler.java_object_handle);
            } else {
                getException(isolateThread);

                break;
            }

            char *description = {};
            result = dxfg_CFI_describe(isolateThread, cfi, &description);

            if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                printf("  description = '%s'\n", description);

                dxfg_String_release(isolateThread, description);
            } else {
                getException(isolateThread);
            }

            dxfg_JavaObjectHandler_release(isolateThread, &cfi->handler);
        } while (false);
    }};
} // namespace dxfg
