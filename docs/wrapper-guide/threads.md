# Threads {#wrapper_threads}

The SDK runs in an isolate, created by graal_create_isolate(): a GraalVM instance with its own heap and Java state.
The handles of one isolate cannot be used in another. A process normally has one isolate, created by the wrapper at
start-up.

## Attachment {#wrapper_threads_attach}

- Every thread that calls the SDK must be attached to the isolate. graal_create_isolate() attaches the calling thread,
  graal_attach_thread() any other one; attaching a thread that is already attached returns its isolate thread.
- Every function takes the `graal_isolatethread_t *` of the calling thread first. It belongs to that thread: never
  pass it to another thread or cache it beyond the thread's life. graal_get_current_thread() returns it, or NULL if
  the thread is not attached.
- A thread detaches with graal_detach_thread() before it exits, and only when no SDK function runs on it.
  graal_tear_down_isolate() waits for the other attached threads to detach.
- The exception of a failed call is stored in the calling thread (@ref wrapper_outcomes), so take it on the same
  thread, before the next call.
- A callback of the SDK (a listener, a finalizer, a filter) runs on a thread of the SDK and receives that thread's
  `graal_isolatethread_t *`: make the calls of the callback with it, and never detach it.

The cheapest scheme is to attach a thread once, at its first call, and to keep the isolate thread in thread-local
storage until the thread ends. Attaching and detaching around each call works, but costs much more than most calls.

## Long-lived threads {#wrapper_threads_long_lived}

Runtimes that move their tasks between OS threads must keep the calls of one sequence on one OS thread:

- **Go.** A goroutine may move to another OS thread between two calls, and the isolate thread of the first OS thread
  must not be used on the second one. Make the calls from a goroutine locked to its OS thread (`runtime.LockOSThread`)
  that attaches once, e.g. one worker goroutine that serves the requests of the others, or attach and detach within
  one locked section.
- **.NET.** The threads of the thread pool live long and end without notice. Attach a pool thread at its first call
  and keep the isolate thread in a `[ThreadStatic]` field; a thread that exits attached still counts for
  graal_tear_down_isolate(), so end the process with graal_detach_all_threads_and_tear_down_isolate(), once no other
  thread runs an SDK function. An `async` method may resume on another thread: take the isolate thread after each
  `await`, never before it.
- **Python, Ruby, Node.js.** The interpreter's threads are OS threads: attach each one at its first call. The worker
  threads of Node.js and the threads of `concurrent.futures` are attached like any other.

A poll that waits (dxfg_v4_EventQueue_poll() with a timeout) holds its thread for the wait. Run it on a dedicated
thread, or poll with the timeout 0 when an event loop reports the queue ready (@ref wrapper_delivery), so that the
runtime's own scheduler is not blocked. A poll that waits is ended from another thread by dxfg_v4_EventQueue_wakeUp()
or dxfg_v4_EventQueue_close().

## Finalizers {#wrapper_threads_finalizers}

A wrapper that releases handles from the finalizers or cleaners of its runtime (a .NET finalizer, a Java `Cleaner`,
Python `__del__`, a Go finalizer) releases them on a thread it does not choose:

- Attach the finalizer thread like any other (graal_attach_thread()), or hand the release over to a thread that is
  attached already.
- Releasing on another thread than the one that obtained the handle or the list is allowed: a list, a string or a
  handle can be released by any attached thread (@ref wrapper_ownership).
- A finalizer may run after the isolate is torn down, e.g. at the exit of the process. Keep a flag that the wrapper
  sets before graal_tear_down_isolate(), and skip the release once it is set: the memory goes with the isolate.
- Releasing a listener waits for its calls that are in progress (see dxfg_v4_NativeListener_deactivate()): do not
  release a listener in a finalizer that holds a lock the listener's function takes.
- dxfg_v4_Object_finalize() is the other direction: the SDK calls a function after a Java object is collected, on a
  thread of the SDK (@ref V4_Javac).

## Shutdown {#wrapper_threads_shutdown}

The threads of an open endpoint do not stop on `Thread.interrupt()`, so graal_tear_down_isolate() waits for them
forever while an endpoint is open. Before tearing down the isolate, call dxfg_v4_system_shutdown() (@ref V4_System):
it deactivates the listeners, closes the event queues (the polls in progress return #DXFG_EVENT_QUEUE_CLOSED) and
closes the endpoints. Then end the wrapper's own threads that call the SDK, release the remaining handles, and tear
the isolate down. Do not call it from an exit handler of a DLL (`DLL_PROCESS_DETACH`, a destructor of a static object
of a DLL).

What a wrapper may rely on after the call, and what it may not:

- No listener of a `dxfg_*Listener_new` function is called, and none of their calls is in progress: their user data
  can be freed. The listeners created afterwards are never called, the event queues created afterwards are closed.
- The callbacks that are not listeners are still called: the handlers of dxfg_v4_Promise_whenDone() and
  dxfg_v4_Promise_whenDoneAsync() when a promise completes, the functions of dxfg_v4_Object_finalize() when an object
  is collected (@ref wrapper_threads_finalizers), and the filters of dxfg_v4_DayFilter_new(),
  dxfg_v4_DayFilter_newWithUserData(), dxfg_v4_SessionFilter_new() and dxfg_v4_SessionFilter_newWithUserData() when a
  search uses them. Their user data lives until they have run or the isolate is torn down; a filter's until it is
  released. A wrapper that promises "no callback after shutdown" has to gate these itself.
- A second call is allowed: it closes what was created since the previous one and returns at once when nothing is
  left, so a wrapper need not track whether it has shut down already.
- The call acts on its own isolate. A new isolate created afterwards works, also after graal_tear_down_isolate() of the
  old one, except in the `oracle-g1` build variant, which allows one isolate per process: there the isolate that was
  shut down is the last one of the process, and it serves only the calls that need no listener and no event queue.

The call waits without a limit: for a listener's call in progress (a callback that blocks blocks the shutdown) and for
the termination of the endpoints. A wrapper that must stop its application in bounded time:

1. Calls dxfg_v4_system_shutdown() on a thread of its own, attached to the isolate, and waits for that thread with a
   timeout.
2. When the thread returns in time, goes on with the sequence above.
3. When the timeout expires, the call is still running. The wrapper must not free the user data of the listeners (a
   call may still be in progress), call graal_tear_down_isolate() (it waits for that thread to detach) or
   graal_detach_all_threads_and_tear_down_isolate() (that thread is in an SDK function). It may keep waiting, or
   report the hang and let the process exit with the isolate as it is: the threads of the isolate end with the
   process.
