# Delivery of data {#wrapper_delivery}

The SDK can hand data to the application in three ways. This page is guidance, not a rule of conformance: a wrapper
picks what fits its language, and may offer more than one.

| Data | Delivery | When |
|------|----------|------|
| The events of subscriptions | An event queue (@ref V4_EventQueue) | The default for a wrapper: the application's own thread polls, and no thread of the SDK calls into the wrapper's runtime |
| The models, the instrument profile collector, other callbacks | The executor of a shared queue (dxfg_v4_EventQueue_getExecutor()) | Their listeners run inside the polls, on the polling thread, between the events of the subscriptions |
| Events and callbacks of a language that takes calls from foreign threads | Listeners (@ref V4_Subscription, @ref V4_EventModel) | The SDK calls the listener on its own thread; the lowest latency, but the wrapper's runtime must accept the call |
| An event queue in a host with an event loop | The ready signal (dxfg_v4_EventQueue_setReadySignal(), @ref experimental) | The loop waits for an OS object of its own and polls with the timeout 0 when it is ready; no reader thread |

## Event queues {#wrapper_delivery_queue}

- A queue bound to one subscription (dxfg_v4_EventQueue_new()) suits a wrapper object per subscription. A shared queue
  (dxfg_v4_EventQueue_create(), dxfg_v4_EventQueue_attach()) lets one thread serve many subscriptions and the models,
  each subscription with a tag that dxfg_v4_EventQueue_pollTagged() returns with its events.
- The SDK retrieves the events only when the queue is polled. Until then the lasting events (Quote, Profile, ...) are
  conflated, and the stream events (TimeAndSale, Order on a plain symbol) wait in a buffer of the subscription; beyond
  it the oldest are dropped and counted (@ref wrapper_outcomes_dropped). A wrapper that polls rarely sees the latest
  values of the lasting events and gaps in the streams, never stale data.
- Poll a queue from one thread at a time. The thread need not be the one that created the queue (@ref wrapper_threads).
- Create the queue before adding the symbols to a subscription attached to a feed.

## Executors {#wrapper_delivery_executors}

A model or the collector given the executor of a shared queue calls its listener in a poll of that queue, so the
wrapper keeps one polling thread for all its data. Keep a handle of the queue or of its executor until the models are
closed: releasing the last ones closes the queue and stops their listeners.

## Event loops {#wrapper_delivery_ready}

A host with an event loop (Python asyncio, an `epoll` loop, libuv) gives the queue an OS object it owns: a
non-blocking `eventfd` or the write end of a pipe on Linux and macOS, an auto-reset event on Windows. Check
#DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL first. The SDK makes the object ready whenever the queue has work; the loop
resets it, then polls once with the timeout 0: a poll that leaves work signals again. A signal without work
is possible, work without a signal is not. Loops that cannot wait for a foreign object (the asyncio proactor loop and
libuv on Windows) keep a reader thread that polls with a timeout.

## Listeners {#wrapper_delivery_listeners}

A listener is called by a thread of the SDK, with that thread's `graal_isolatethread_t *`. A wrapper whose runtime
can take such a call (C++, Rust, Java through FFM) may use listeners directly; one whose runtime cannot, or must
enter a lock first (CPython's GIL, the Go scheduler), pays for each call and should prefer the queue. A listener may
be called once more after the subscription or the endpoint is closed: remove it first, then release it.
