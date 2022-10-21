// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <dxfg_events.h>
#include <string>
#include <unordered_map>

namespace dxfg {

class EventFormatter {
  public:
    class Quote {
      public:
        static std::string toString(const dxfg_event_quote_t *event);
    };

    class TimeAndSale {
      private:
        static const std::unordered_map<dxfg_event_time_and_sale_type_t, std::string> typesMap_;
        static const std::unordered_map<dxfg_event_side_t, std::string> sidesMap_;

      public:
        static std::string toString(const dxfg_event_time_and_sale_t *event);
    };
};

} // namespace dxfg
