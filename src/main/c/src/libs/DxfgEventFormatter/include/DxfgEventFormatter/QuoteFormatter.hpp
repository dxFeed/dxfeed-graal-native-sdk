#pragma once
#include <cstdint>
#include <dxfg_events.h>
#include <string>

namespace dxfg {

class QuoteFormatter {
  private:
    static const int32_t MAX_SEQUENCE = (1 << 22) - 1;

  public:
    static int64_t getTime(const dxfg_event_quote_t *e);
    static int32_t getSequence(const dxfg_event_quote_t *e);
    static std::string toString(const dxfg_event_quote_t *e);
};

} // namespace dxfg