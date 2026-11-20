#ifndef API_H
#define API_H

/**
 * Outside the blocks a header names deprecated functions anyway: dxfg_free_strings(), dxfg_Schedule_getInstance().
 *
 * <!-- dxfg-renamed:begin -->
 *  - dxfg_Schedule_getInstance() ->
 *    dxfg_v4_Schedule_getInstanceByProfileHandle(); dxfg_v4_Schedule_getInstance() replaces
 *    dxfg_Schedule_getInstance4().
 * <!-- dxfg-renamed:end -->
 *
 * <!-- dxfg-alternatives:begin -->
 *  - dxfg_free_strings() -> dxfg_v4_string_list_release(), not dxfg_v4_free_string_list().
 * <!-- dxfg-alternatives:end -->
 */
void dxfg_free_strings(void);

#endif
