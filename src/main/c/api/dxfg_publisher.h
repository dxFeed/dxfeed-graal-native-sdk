// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_PUBLISHER_H_
#define DXFEED_GRAAL_NATIVE_API_PUBLISHER_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "graal_isolate.h"

/** @defgroup Publisher
 *  @{
 */

/**
 * @brief The DXPublisher.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXPublisher.html">Javadoc</a>
 */
typedef struct dxfg_publisher_t {
    void *java_object_handle;
} dxfg_publisher_t;

/** @} */ // end of Publisher

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_PUBLISHER_H_
