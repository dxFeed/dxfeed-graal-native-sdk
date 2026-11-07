# Ownership and lifetimes {#wrapper_ownership}

The C API never shares memory with the caller for longer than a call, except through handles and the data it returns.
The general rules are in @ref V4_Conventions; this page states what a wrapper builds on them.

## What a function returns {#wrapper_ownership_returned}

- **Handles.** The structures with a `handler` field (`dxfg_endpoint_t`, `dxfg_subscription_t`, the listeners, the
  event queues, ...) are handles of Java objects. The Java object lives at least as long as a handle of it: release
  each handle once with dxfg_v4_JavaObjectHandler_release(). dxfg_v4_JavaObjectHandler_clone() makes another handle
  of the same object, released on its own. A wrapper object that owns a handle releases it when it is closed or
  collected (@ref wrapper_threads_finalizers).
- **Data.** Events, symbols, strings, instrument profiles and lists are copies of Java data, owned by the caller until
  it releases them with the function that the description names: dxfg_v4_EventType_release() and
  dxfg_v4_event_type_list_release() for events, dxfg_v4_String_release() for strings,
  dxfg_v4_string_list_release() for lists of strings, and so on. In the API description (@ref wrapper_versioning)
  every returned pointer has its `release` function.
- **Exceptions.** The exception taken with dxfg_v4_get_and_clear_thread_exception() is the caller's; release it with
  dxfg_v4_Exception_release().
- **Out parameters.** A function writes its out parameter only when it succeeds. After an error the variable keeps
  what the caller put there, and there is nothing to release.

A returned list is one block: its elements are released with it, never one by one, and an element must not be kept
after the list is released. Copy what is needed into the wrapper's own objects, then release the list. Lists are
independent of each other: each is released on its own, in any order, and releasing one does not touch another.

Any attached thread may release what any thread obtained: a list polled on one thread can be released on another, a
handle created on one thread can be released by a finalizer (@ref wrapper_threads).

## What the caller passes {#wrapper_ownership_passed}

- What the caller passes stays the caller's: the SDK copies what it needs during the call. Strings, symbols, events
  and lists built by the wrapper can be freed as soon as the call returns, and a list the caller built is never
  passed to a release function.
- The user data of a listener or a callback is the caller's too: the SDK passes it back as it is and never frees it.
  It must stay valid until the listener is deactivated (dxfg_v4_NativeListener_deactivate() or the release of its
  last handle), which waits for the calls in progress, so it can be freed right after.

## What a callback receives {#wrapper_ownership_callbacks}

The data passed to a callback (the events of a listener, the arguments of a filter) belongs to the SDK and is released
when the callback returns. Copy what is needed later; never release it, and never keep a pointer into it.

## Lifetimes of objects {#wrapper_ownership_lifetimes}

- Closing is not releasing. dxfg_v4_DXEndpoint_close(), dxfg_v4_DXFeedSubscription_close() and
  dxfg_v4_EventQueue_close() stop the object, but its handle stays valid until it is released; releasing the last
  handle of an open object does not always close it (an endpoint stays open, an event queue closes).
- An event queue bound to a subscription (dxfg_v4_EventQueue_new()) closes with the subscription. A shared queue
  (dxfg_v4_EventQueue_create()) closes when it is closed or when the last handle of it and of its executor is released:
  keep a handle until the models that use its executor are closed (@ref V4_EventQueue).
- After dxfg_v4_system_shutdown() the handles stay valid and must still be released, but the listeners are no longer
  called and the queues are closed. After graal_tear_down_isolate() nothing of the isolate may be used or released.
