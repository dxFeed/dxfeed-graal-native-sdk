// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <cstdint>

class BitUtils {
  public:
    /**
     * @brief Gets bits form flags.
     * @tparam T The flag and mask types.
     * @param[in] flags The bit flags.
     * @param[in] mask The bit mask.
     * @param[in] shift The bit right shift.
     * @return Returns bit value.
     */
    template <class T> inline static T getBits(T flags, T mask, uint32_t shift) {
        return (flags >> shift) & mask;
    }
};
