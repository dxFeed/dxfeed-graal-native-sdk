// A fixture of GenerateApiJson.java --self-test.
#define DXFG_EXPERIMENTAL
#define DXFG_OUT
#define DXFG_DEPRECATED(message)

/** The revision of the advanced API. */
#define DXFG_V4_ADVANCED_REVISION ((int32_t)1)

/** The limit. */
#define DXFG_V4_POINT_LIMIT 10

typedef struct graal_isolatethread_t graal_isolatethread_t;

/** A point. */
typedef struct dxfg_v4_point_t {
    int32_t x;
} dxfg_v4_point_t;

/** A side. */
typedef enum dxfg_v4_side_t {
    DXFG_V4_SIDE_BUY = 1,
    DXFG_V4_SIDE_SELL = 2
} dxfg_v4_side_t;

/** Gets the point. */
int32_t dxfg_v4_point_get(graal_isolatethread_t *thread, dxfg_v4_point_t *point, DXFG_OUT int32_t *x);

/**
 * Moves the point.
 * @experimental
 */
DXFG_EXPERIMENTAL
int32_t dxfg_v4_point_move(graal_isolatethread_t *thread, dxfg_v4_point_t *point, int32_t dx);

/** Gets the point. */
DXFG_DEPRECATED("use dxfg_v4_point_get()")
int32_t dxfg_point_get(graal_isolatethread_t *thread, dxfg_v4_point_t *point);
