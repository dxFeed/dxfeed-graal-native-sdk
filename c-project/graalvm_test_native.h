#ifndef __GRAALVMTESTNATIVE_H
#define __GRAALVMTESTNATIVE_H

#include <graal_isolate.h>

#if defined(__cplusplus)
extern "C" {
#endif

#include "dxf_types.h"

int run_main(int argc, char** argv);

graal_isolatethread_t* native_test_api_create_isolate();

void native_create_events_and_call_listener(graal_isolatethread_t*, dxf_subscription_event_listener);

int native_test_api_add(graal_isolatethread_t*, int, int);

void native_test_set_system_property(graal_isolatethread_t*, const char*, const char*);

int native_test_check_system_property(graal_isolatethread_t*, const char*, const char*);

void vmLocatorSymbol(graal_isolatethread_t* thread);

#if defined(__cplusplus)
}
#endif

#endif // __GRAALVMTESTNATIVE_H
