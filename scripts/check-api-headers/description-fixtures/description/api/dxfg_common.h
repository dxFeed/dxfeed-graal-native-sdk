// A fixture of GenerateApiJson.java --self-test.
#ifndef DXFG_COMMON_H
#define DXFG_COMMON_H

#include <stdint.h>

#ifndef DXFG_EXPERIMENTAL
#    define DXFG_EXPERIMENTAL
#endif

#define DXFG_OUT
#define DXFG_DEPRECATED(message)

/** The status of a call that succeeded. */
#define DXFG_EXECUTE_SUCCESSFULLY ((int32_t)0)

/**
 * The revision of the advanced API.
 * @since 1.1.0
 */
#define DXFG_V4_ADVANCED_REVISION ((int32_t)2)

/**
 * The name of the queue feature.
 * @since 1.1.0
 */
#define DXFG_V4_FEATURE_QUEUE "queue"

/** The name of the stream feature. @since 1.1.0 */
#define DXFG_V4_FEATURE_STREAM \
    "stream"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct graal_isolatethread_t graal_isolatethread_t;

/** A handle that is defined below. */
typedef struct dxfg_v4_point_t dxfg_v4_point_t;

/** An opaque handle. */
typedef struct dxfg_v4_handle_t dxfg_v4_handle_t;

/** A point. */
typedef struct dxfg_v4_point_t {
    int32_t x, *y;
    const char *name; // The name.
    int64_t values[4];
} dxfg_v4_point_t;

/** A list of points. */
typedef struct dxfg_v4_point_list {
    int32_t size;
    dxfg_v4_point_t **elements;
} dxfg_v4_point_list;

/** A side. */
typedef enum dxfg_v4_side_t {
    DXFG_V4_SIDE_BUY = 1,
    DXFG_V4_SIDE_SELL
} dxfg_v4_side_t;

/** Another name of the handle. */
typedef dxfg_v4_handle_t dxfg_v4_handle;

/** A callback. */
typedef void (*dxfg_v4_callback_f)(graal_isolatethread_t *thread, dxfg_v4_point_t *point, void *userData);

/**
 * A preview of a struct.
 * @experimental
 */
typedef struct dxfg_v4_preview_t {
    double value;
} dxfg_v4_preview_t;

/** Releases a handle. */
int32_t dxfg_v4_JavaObjectHandler_release(graal_isolatethread_t *thread, void *handle);

/** Releases a list of points. */
int32_t dxfg_v4_point_list_release(graal_isolatethread_t *thread, dxfg_v4_point_list *points);

/**
 * Creates a handle.
 *
 * @param[in] thread The thread.
 * @param[in] name The name.
 * @param[out] handle The handle (release it with dxfg_v4_JavaObjectHandler_release()).
 * @param[in] callback Called for every point.
 * @return DXFG_EXECUTE_SUCCESSFULLY or DXFG_EXECUTE_FAIL.
 */
int32_t dxfg_v4_handle_new(graal_isolatethread_t *thread, const char *name, DXFG_OUT dxfg_v4_handle_t **handle,
                           dxfg_v4_callback_f callback);

/**
 * Gets the points of the handle.
 *
 * @param[out] points The points.
 */
int32_t dxfg_v4_handle_getPoints(graal_isolatethread_t *thread, dxfg_v4_handle_t *handle,
                                 DXFG_OUT dxfg_v4_point_list **points);

/**
 * Peeks at the handle.
 *
 * @experimental
 * @param[out] side The side.
 * @since 1.1.0
 */
DXFG_EXPERIMENTAL
int32_t dxfg_v4_handle_peek(graal_isolatethread_t *thread, const dxfg_v4_handle_t *handle, DXFG_OUT dxfg_v4_side_t *side);

/** Does nothing. */
void dxfg_v4_handle_nothing(void);

/** Creates a handle. */
DXFG_DEPRECATED("use dxfg_v4_handle_new() and dxfg_v4_handle_getPoints(); the \"name\" stays")
dxfg_v4_handle_t *dxfg_handle_new(graal_isolatethread_t *thread, const char *name);

#ifdef __cplusplus
}
#endif

#endif // DXFG_COMMON_H
