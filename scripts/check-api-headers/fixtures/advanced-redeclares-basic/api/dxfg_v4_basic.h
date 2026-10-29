// A fixture of CheckApiHeaders.java --self-test.
#define DXFG_V4_BASIC_LIMIT 1

typedef struct dxfg_v4_basic_t {
    int value;
} dxfg_v4_basic_t;

/**
 * Gets the value.
 */
int dxfg_v4_basic_get(void *thread, dxfg_v4_basic_t *basic, int *value);
