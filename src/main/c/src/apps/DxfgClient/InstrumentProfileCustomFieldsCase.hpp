// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <dxfg_api.h>

#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <chrono>
#include <cstdio>
#include <string>
#include <vector>

void getException(graal_isolatethread_t *isolateThread);

namespace dxfg {
inline Command instrumentProfileCustomFieldsCase{
    "InstrumentProfileCustomFieldsCase",
    {"ipcf"},
    "",
    "ipcf [<properties>]",
    {"ipcf"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &,
       const dxfg::CommandsContext &) {
        using namespace std::chrono_literals;

        {
            puts("== InstrumentProfileCustomFields 1 ==");

            std::vector<const char *> data{{"field1", "value1", "numberField", "12345.89", "dateField", "2022-04-05"}};

            dxfg_instrument_profile_custom_fields_t *customFields{};
            auto r = dxfg_InstrumentProfileCustomFields_new2(isolateThread, data.data(),
                                                             static_cast<std::int32_t>(data.size()), &customFields);

            if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                getException(isolateThread);
            } else {
                for (const auto &f : data) {
                    char *fieldValue{};
                    r = dxfg_InstrumentProfileCustomFields_getField(isolateThread, customFields, f, &fieldValue);

                    if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                        getException(isolateThread);
                    }

                    printf("%s -> %s\n", f, fieldValue);
                    dxfg_String_release(isolateThread, fieldValue);
                }

                putchar('\n');

                for (const auto &f : data) {
                    double numField{};
                    r = dxfg_InstrumentProfileCustomFields_getNumericField(isolateThread, customFields, f, &numField);

                    if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                        getException(isolateThread);
                    }

                    printf("%s -> %f\n", f, numField);
                }

                putchar('\n');

                for (const auto &f : data) {
                    std::int32_t dateField{};
                    r = dxfg_InstrumentProfileCustomFields_getDateField(isolateThread, customFields, f, &dateField);

                    if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                        getException(isolateThread);
                    }

                    printf("%s -> %d\n", f, dateField);
                }

                r = dxfg_InstrumentProfileCustomFields_setDateField(isolateThread, customFields, "dateField2", 12345);

                if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                    getException(isolateThread);
                }

                char *fieldValue{};
                r = dxfg_InstrumentProfileCustomFields_getField(isolateThread, customFields, "dateField2", &fieldValue);

                if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                    getException(isolateThread);
                }

                printf("%s -> %s\n", "dateField2", fieldValue);
                dxfg_String_release(isolateThread, fieldValue);

                std::int32_t dateField{};
                r = dxfg_InstrumentProfileCustomFields_getDateField(isolateThread, customFields, "dateField2",
                                                                    &dateField);

                if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                    getException(isolateThread);
                }

                printf("%s -> %d\n", "dateField2", dateField);

                dxfg_string_list *fieldNames{};

                r = dxfg_InstrumentProfileCustomFields_getNonEmptyFieldNames(isolateThread, customFields, &fieldNames);

                if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                    getException(isolateThread);
                } else {
                    puts("\nField names:");

                    for (std::size_t i = 0; i < fieldNames->size; i++) {
                        printf("  %s\n", fieldNames->elements[i]);
                    }

                    dxfg_CList_String_release(isolateThread, fieldNames);
                }

                dxfg_JavaObjectHandler_release(isolateThread,
                                               reinterpret_cast<dxfg_java_object_handler *>(customFields));
            }
        }

        {
            puts("== InstrumentProfileCustomFields 2 ==");

            for (auto step = 0; step < 20; step++) {
                printf("  Step = %d\n", step);
                std::vector<const char *> data{{"field1", "value",  "field2", "value",  "field3",  "value",  "field4",
                                                "value",  "field5", "value",  "field6", "value",   "field7", "value",
                                                "field8", "value",  "field9", "value",  "field10", "value"}};

                dxfg_instrument_profile_custom_fields_t *customFields{};
                auto r = dxfg_InstrumentProfileCustomFields_new2(isolateThread, data.data(),
                                                                 static_cast<std::int32_t>(data.size()), &customFields);

                if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                    getException(isolateThread);
                } else {
                    dxfg_string_list *fieldNames{};

                    r = dxfg_InstrumentProfileCustomFields_getNonEmptyFieldNames(isolateThread, customFields,
                                                                                 &fieldNames);

                    if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                        getException(isolateThread);
                    } else {
                        puts("\n    Field names:");

                        for (std::size_t i = 0; i < fieldNames->size; i++) {
                            printf("      %s\n", fieldNames->elements[i]);
                        }

                        dxfg_CList_String_release(isolateThread, fieldNames);
                    }

                    dxfg_JavaObjectHandler_release(isolateThread,
                                                   reinterpret_cast<dxfg_java_object_handler *>(customFields));
                }
            }
        }
    }};
} // namespace dxfg