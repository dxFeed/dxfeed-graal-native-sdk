// Copyright (c) 2024 Devexperts LLC.
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
Command instrumentProfileCustomFieldsCase{
    "InstrumentProfileCustomFieldsCase",
    {"ipcf"},
    "",
    "ipcf [<properties>]",
    {"ipcf"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &,
       const dxfg::CommandsContext &) {
        using namespace std::chrono_literals;

        puts("== InstrumentProfileCustomFields ==");

        std::vector<const char *> data{{"field1", "value1", "numberField", "12345.89", "dateField", "2022-04-05"}};

        dxfg_instrument_profile_custom_fields_t *customFields{};
        auto r = dxfg_InstrumentProfileCustomFields_new2(isolateThread, data.data(), static_cast<std::int32_t>(data.size()), &customFields);

        if (r != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);
        } else {
            for (const auto& f : data) {
                char* fieldValue{};
                r = dxfg_InstrumentProfileCustomFields_getField(isolateThread, customFields, f, &fieldValue);

                if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                    getException(isolateThread);
                }

                printf("%s -> %s\n", f, fieldValue);
                dxfg_String_release(isolateThread, fieldValue);
            }

            putchar('\n');

            for (const auto& f : data) {
                double numField{};
                r = dxfg_InstrumentProfileCustomFields_getNumericField(isolateThread, customFields, f, &numField);

                if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                    getException(isolateThread);
                }

                printf("%s -> %f\n", f, numField);
            }

            putchar('\n');

            for (const auto& f : data) {
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

            char* fieldValue{};
            r = dxfg_InstrumentProfileCustomFields_getField(isolateThread, customFields, "dateField2", &fieldValue);

            if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                getException(isolateThread);
            }

            printf("%s -> %s\n", "dateField2", fieldValue);
            dxfg_String_release(isolateThread, fieldValue);

            std::int32_t dateField{};
            r = dxfg_InstrumentProfileCustomFields_getDateField(isolateThread, customFields, "dateField2", &dateField);

            if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                getException(isolateThread);
            }

            printf("%s -> %d\n", "dateField2", dateField);

            char** fieldNames{};
            std::int32_t fieldNamesLength{};

            r = dxfg_InstrumentProfileCustomFields_addNonEmptyFieldNames(isolateThread, customFields, &fieldNames, &fieldNamesLength, nullptr);

            if (r != DXFG_EXECUTE_SUCCESSFULLY) {
                getException(isolateThread);
            } else {
                puts("\nField names:");

                for (std::size_t i = 0; i < fieldNamesLength; i++) {
                    printf("  %s\n", fieldNames[i]);
                }

                dxfg_free_strings(isolateThread, fieldNames, fieldNamesLength);
            }

            dxfg_JavaObjectHandler_release(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(customFields));
        }
    }};
} // namespace dxfg