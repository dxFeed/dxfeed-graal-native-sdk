// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_JAVAC_H_
#define DXFEED_GRAAL_NATIVE_API_JAVAC_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "graal_isolate.h"

/** @defgroup Javac
 *  @{
 */

typedef struct dxfg_java_object_handler {
    void *java_object_handle;
} dxfg_java_object_handler;

typedef struct dxfg_list {
    int32_t size;
    void **elements;
} dxfg_list;

typedef struct dxfg_java_object_handler_list {
    int32_t size;
    dxfg_java_object_handler **elements;
} dxfg_java_object_handler_list;

typedef struct dxfg_string_list {
    int32_t size;
    const char **elements;
} dxfg_string_list;

typedef struct dxfg_executor_t {
    dxfg_java_object_handler handler;
} dxfg_executor_t;

typedef struct dxfg_input_stream_t {
    dxfg_java_object_handler handler;
} dxfg_input_stream_t;

// free the memory occupied by the с data structure and release the reference to the java object
int32_t dxfg_JavaObjectHandler_release(graal_isolatethread_t *thread, dxfg_java_object_handler*);

// free the memory occupied by the с data structure (list and all elements) and release the reference to the java object for all elements
int32_t dxfg_CList_JavaObjectHandler_release(graal_isolatethread_t *thread, dxfg_java_object_handler_list*);

int32_t dxfg_String_release(graal_isolatethread_t *thread, const char* string);

// read the "Threads and locks" sections at https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedSubscription.html
dxfg_executor_t*  dxfg_Executors_newFixedThreadPool(graal_isolatethread_t *thread, int nThreads, const char* nameThreads);
dxfg_executor_t*  dxfg_Executors_newScheduledThreadPool(graal_isolatethread_t *thread, int nThreads, const char* nameThreads);
dxfg_executor_t*  dxfg_ExecutorBaseOnConcurrentLinkedQueue_new(graal_isolatethread_t *thread);
int32_t           dxfg_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(graal_isolatethread_t *thread, dxfg_executor_t *executor);

dxfg_input_stream_t*   dxfg_ByteArrayInputStream_new(graal_isolatethread_t *thread, const char* bytes, int32_t size);

/** @} */ // end of Javac

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_JAVAC_H_




