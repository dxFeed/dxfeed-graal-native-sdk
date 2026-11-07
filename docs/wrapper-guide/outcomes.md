# Return codes and outcomes {#wrapper_outcomes}

A wrapper turns the return codes of the C API into the error model of its language: exceptions, error values or
results. The rules are in @ref V4_Conventions and @ref V4_Exceptions; this page states what a wrapper must tell apart.

## Status and exception {#wrapper_outcomes_status}

- A `dxfg_v4_` function returns `int32_t`: #DXFG_EXECUTE_SUCCESSFULLY (0) on success, #DXFG_EXECUTE_FAIL (-1) on
  error. The status is the only error signal: a NULL or 0 written to the out parameter is an ordinary value ("no
  session", "no event"), never an error.
- After #DXFG_EXECUTE_FAIL, dxfg_v4_get_and_clear_thread_exception() gives the Java exception (its class, message,
  stack trace and cause) and clears it; release it with dxfg_v4_Exception_release(). The exception belongs to the
  calling thread, so take it on that thread right after the failed call: a later successful call does not clear an
  exception that was not taken, and it would be reported for the wrong call.
- Map the exception by its class name: `java.lang.IllegalArgumentException` (a wrong argument, a NULL out pointer)
  and `java.lang.NullPointerException` are errors of the caller, `java.lang.IllegalStateException` a call in a state
  that does not allow it, the others failures of the operation. Keep the message and the class in the wrapper's error
  so that a report names the cause.
- The older functions signal errors in their own ways (a NULL pointer, -1, an exception without a status); a wrapper
  binds the `dxfg_v4_` twins and does not need them.

## Outcomes that are not errors {#wrapper_outcomes_waiting}

The functions that wait return documented positive codes for the outcomes that are not errors. A wrapper must not
treat them as failures, and takes an exception only after #DXFG_EXECUTE_FAIL:

- dxfg_v4_EventQueue_poll() and dxfg_v4_EventQueue_pollTagged() (@ref V4_EventQueue):
  - #DXFG_EXECUTE_SUCCESSFULLY: a list of events, which the caller releases;
  - #DXFG_EVENT_QUEUE_NO_EVENTS: no events: the timeout passed, dxfg_v4_EventQueue_wakeUp() was called, or the poll
    ran the work of the models on the queue; poll again;
  - #DXFG_EVENT_QUEUE_CLOSED: the queue is closed (dxfg_v4_EventQueue_close(), the release of its handle, the close of
    its subscription, dxfg_v4_system_shutdown()); every later poll returns it too, so stop polling;
  - #DXFG_EXECUTE_FAIL: the call broke the contract (e.g. a negative limit, or a poll from a listener during a poll of
    the same queue); the exception tells why, and the queue stays usable.
- dxfg_v4_Promise_awaitWithTimeout() (@ref V4_Feed): #DXFG_PROMISE_AWAIT_TIMED_OUT when the promise has not completed
  in time.

A natural mapping makes each outcome its own case: an iterator that ends on #DXFG_EVENT_QUEUE_CLOSED, an empty batch
for #DXFG_EVENT_QUEUE_NO_EVENTS, an exception or an error value for #DXFG_EXECUTE_FAIL.

## Lost events {#wrapper_outcomes_dropped}

Losing events is not an error either: a poll succeeds even when events were dropped before the ones it returns. After
a poll, compare dxfg_v4_EventQueue_getDroppedEvents() with its previous value; a larger one means a gap, which the
wrapper reports to the application (a counter, a callback, a flag on the batch) as its language allows.
