/*
 * Checks DxFeedGraalNativeSdk.dll without the network: a local hub publishes a profile, a listener receives it, the
 * last event is read back, a system property is set and read, the isolate is torn down. Prints OK and exits with 0, or
 * prints the failed step and exits with 1.
 */
#include <stdio.h>
#include <string.h>
#include <windows.h>

#include "dxfg_api.h"

static const char *SYMBOL = "STATIC-RUNTIME-CHECK";
static const char *DESCRIPTION = "published by static-runtime-check";
static volatile LONG receivedEvents = 0;

static void countEvents(graal_isolatethread_t *thread, dxfg_event_type_list *events, void *userData) {
    (void)thread;
    (void)userData;
    InterlockedExchangeAdd(&receivedEvents, events->size);
}

static int fail(graal_isolatethread_t *thread, const char *step) {
    dxfg_exception_t *exception = thread != NULL ? dxfg_get_and_clear_thread_exception_t(thread) : NULL;

    fprintf(stderr, "FAILED: %s", step);
    if (exception != NULL) {
        fprintf(stderr, ": %s: %s", exception->class_name, exception->message != NULL ? exception->message : "");
        dxfg_Exception_release(thread, exception);
    }
    fprintf(stderr, "\n");

    return 1;
}

int main(void) {
    graal_isolate_t *isolate = NULL;
    graal_isolatethread_t *thread = NULL;

    if (graal_create_isolate(NULL, &isolate, &thread) != 0) {
        return fail(NULL, "graal_create_isolate");
    }

    if (dxfg_system_set_property(thread, "static.runtime.check", "yes") != DXFG_EXECUTE_SUCCESSFULLY) {
        return fail(thread, "dxfg_system_set_property");
    }
    const char *property = dxfg_system_get_property(thread, "static.runtime.check");
    if (property == NULL || strcmp(property, "yes") != 0) {
        return fail(thread, "dxfg_system_get_property");
    }
    dxfg_system_release_property(thread, property);

    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create2(thread, DXFG_ENDPOINT_ROLE_LOCAL_HUB);
    dxfg_feed_t *feed = endpoint != NULL ? dxfg_DXEndpoint_getFeed(thread, endpoint) : NULL;
    dxfg_subscription_t *subscription = feed != NULL ? dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_PROFILE) : NULL;
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &countEvents, NULL);
    if (subscription == NULL || listener == NULL ||
        dxfg_DXFeedSubscription_addEventListener(thread, subscription, listener) != DXFG_EXECUTE_SUCCESSFULLY) {
        return fail(thread, "the subscription");
    }

    dxfg_string_symbol_t symbol = {{STRING}, SYMBOL};
    if (dxfg_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) != DXFG_EXECUTE_SUCCESSFULLY) {
        return fail(thread, "dxfg_DXFeedSubscription_addSymbol");
    }

    dxfg_profile_t profile;
    memset(&profile, 0, sizeof(profile));
    profile.market_event.event_type.clazz = DXFG_EVENT_PROFILE;
    profile.market_event.event_symbol = SYMBOL;
    profile.description = DESCRIPTION;
    dxfg_event_type_t *events[] = {&profile.market_event.event_type};
    dxfg_event_type_list list = {1, events};
    dxfg_publisher_t *publisher = dxfg_DXEndpoint_getPublisher(thread, endpoint);
    if (publisher == NULL || dxfg_DXPublisher_publishEvents(thread, publisher, &list) != DXFG_EXECUTE_SUCCESSFULLY) {
        return fail(thread, "dxfg_DXPublisher_publishEvents");
    }
    dxfg_JavaObjectHandler_release(thread, &publisher->handler);

    for (int i = 0; i < 500 && receivedEvents == 0; i++) {
        Sleep(10);
    }
    if (receivedEvents == 0) {
        return fail(thread, "the listener received no events");
    }

    dxfg_profile_t *last = (dxfg_profile_t *)dxfg_DXFeed_getLastEventIfSubscribed(thread, feed, DXFG_EVENT_PROFILE,
                                                                                  &symbol.supper);
    if (last == NULL || last->description == NULL || strcmp(last->description, DESCRIPTION) != 0) {
        return fail(thread, "dxfg_DXFeed_getLastEventIfSubscribed");
    }
    dxfg_EventType_release(thread, &last->market_event.event_type);

    dxfg_DXFeedSubscription_close(thread, subscription);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &subscription->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);

    if (dxfg_system_close_all_and_await_termination(thread) != DXFG_EXECUTE_SUCCESSFULLY) {
        return fail(thread, "dxfg_system_close_all_and_await_termination");
    }
    if (graal_tear_down_isolate(thread) != 0) {
        return fail(NULL, "graal_tear_down_isolate");
    }

    printf("OK: %ld events received\n", (long)receivedEvents);

    return 0;
}
