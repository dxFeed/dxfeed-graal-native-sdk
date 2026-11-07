# The conformance kit {#wrapper_kit}

The conformance kit checks a wrapper against the contract of this guide by running it, not by reading its code. It
ships in the archives with a dynamic library and `CMakeLists.txt` (3.14.0 and newer), in `kit/` next to
`CMakeLists.txt`, and is versioned with the SDK:

- `kit/runner/`: the runner, C++17 source with its own `CMakeLists.txt`. It needs neither the SDK nor the wrapper.
- `kit/protocol.md`: the line protocol between the runner and an adapter, the values of the events, the scenario
  format and the verdicts.
- `kit/scenarios/`: the scenarios. Those in `shared/` run one after another in one adapter process, each one in
  `own-process/` in a process of its own.
- `kit/adapter/`: the reference adapter, the protocol on the C API directly: an example, and the adapter the SDK's own
  tests run.

The wrapper author writes an adapter in the wrapper's language: a program that reads requests on its stdin, calls the
wrapper, and writes responses on its stdout. The runner starts it, sends the requests of each scenario and checks the
responses:

```sh
cmake -S kit/runner -B build/kit-runner && cmake --build build/kit-runner --config Release
kit-runner --adapter ./my-adapter --strict kit/scenarios/shared
kit-runner --adapter ./my-adapter --strict kit/scenarios/own-process
```

Each scenario ends as `PASS`, `FAIL`, `SKIP` with a reason, or `ERROR` (the adapter exited, stopped answering or wrote
garbage); the runner never waits past its deadlines. With `--strict`, a skip fails unless `--allow-skip <reason>`
allows it, and a run in which no scenario passed fails. Pass `--expected-advanced-revision` the revision the wrapper is
built for (@ref wrapper_versioning).

## What the kit proves {#wrapper_kit_proves}

The data scenarios publish events and receive them through an event queue (@ref V4_EventQueue) of an endpoint with
the role #DXFG_ENDPOINT_ROLE_LOCAL_HUB: the endpoint's feed receives what its publisher publishes, inside the adapter's
process, with no network. So **every wrapper must bind the publisher** (@ref V4_Publisher) to run them; an adapter
without the capability `publisher` skips them.

The table names the scenarios by their files; a scenario's name, in the verdicts and for `--only`, is the file name
with the prefix `kit-` (`kit-roundtrip-quote`).

| Area | Scenarios | Proven |
|------|-----------|--------|
| Events | `roundtrip-quote`, `roundtrip-profile`, `roundtrip-time-and-sale`, `roundtrip-order`, `roundtrip-candle`, `roundtrip-message` | The wrapper maps both ways every field of Quote, Profile, TimeAndSale, Order (with the members of `order_base`), Candle (subscribed as a time series) and Message that the LOCAL_HUB keeps (@ref wrapper_kit_hub): integer bounds, non-ASCII, empty and NULL strings, NaN and infinities |
| Ownership | `list-ownership`, `list-release-worker` | A polled list stays valid until it is released, lists are released one by one in any order, and another attached thread may release one (@ref wrapper_ownership) |
| Outcomes | `poll-outcomes`, `wakeup-blocked`, `close-blocked` | The wrapper tells apart each outcome of a poll: events, #DXFG_EVENT_QUEUE_NO_EVENTS (a timeout, a wake-up), #DXFG_EVENT_QUEUE_CLOSED and #DXFG_EXECUTE_FAIL, and the queue stays usable after a failed poll (@ref wrapper_outcomes) |
| Delivery | `conflation-quote`, `stream-order`, `drops` | Lasting events are conflated with nothing dropped; stream events arrive all, in order, once; beyond the buffer the oldest are dropped and the dropped count accounts for them (@ref wrapper_delivery) |
| Threads | `worker-poll`, `wakeup-blocked`, `close-blocked` | A queue created on one attached thread is polled on another, and a poll that waits without a limit is ended from another thread (@ref wrapper_threads) |
| Ready signal | `ready-signal` | The SDK makes the adapter's OS object ready when the queue has work, and the host polls it with the timeout 0 |
| Isolation | `isolation-1-leave`, `isolation-2-check` | What a scenario leaves (a held list, a waiting poll) is released before the next one, which sees only its own events |
| Shutdown | `shutdown`, `teardown` | dxfg_v4_system_shutdown() ends a waiting poll with #DXFG_EVENT_QUEUE_CLOSED and closes every later poll; after it the worker detaches, the isolate is torn down, and the process exits with 0 |
| Versioning | every scenario that `uses experimental` | A wrapper built for another advanced revision binds no experimental function: those scenarios are skipped as `advanced-disabled`, the others pass |

A scenario that needs something a wrapper may lack is skipped with a reason: `capability-absent` (the adapter does not
offer the capability, e.g. a worker thread or an OS object for the ready signal), `advanced-disabled` (the revisions
differ), `feature-absent` (dxfg_v4_system_is_feature_supported() says no). A feature that the SDK does not know is
reported as not supported, so a scenario that requires it is skipped as `feature-absent`, not failed.

## What the kit cannot prove {#wrapper_kit_limits}

- **The other event types.** The kit covers Quote, Profile, TimeAndSale, Order, Candle and Message; the mapping of the
  other types (@ref V4_Events) is not checked.
- **The fields the LOCAL_HUB drops.** A field that the hub zeroes or nulls (`event_time`, `time_nano_part` and the
  others listed below) comes back the same whatever the wrapper wrote, so its mapping is not checked.
- **Other deliveries.** Listeners, the models, the instrument profile collector, the IPF connections and the message
  connectors (@ref V4_EventModel, @ref V4_InstrumentProfile, @ref V4_MessageConnector) are not exercised: the
  scenarios receive events only through event queues.
- **Real connections and faults.** There is no network, no reconnection, no server that fails; a LOCAL_HUB is not a
  feed.
- **Leaks and performance.** A scenario passes whether or not the wrapper leaks handles, and however slow it is.
- **A real SDK of another advanced revision.** The revision mismatch is simulated: the adapter is told to expect
  another revision of the same library.
- **Foreign event loops.** The ready signal is checked with the adapter's own OS object (an `eventfd` or a pipe on
  Linux and macOS, an event on Windows), not with the loop of the wrapper's language (asyncio, libuv, a .NET
  synchronization context).
- **Behaviour outside a scenario.** A wrapper that passes on one platform may still break on another: run the kit on
  every platform the wrapper supports.

## What the LOCAL_HUB does not keep {#wrapper_kit_hub}

The hub stores events as the records of a QD feed, not as the C structures, so some fields come back changed. The
scenarios publish only values that survive; a wrapper's own tests on a LOCAL_HUB must expect the same:

- All types: `event_time` is 0. Doubles are stored as decimals: `-0` comes back as `0`, magnitudes below about `1e-9`
  as `0` and above about `1e18` as infinity.
- Quote: `time_millis_sequence` and `time_nano_part` are 0; `bid_time` and `ask_time` are kept in whole seconds of the
  `int32` range.
- Profile: `halt_start_time` and `halt_end_time` are kept in whole seconds of the `int32` range.
- TimeAndSale: `time_nano_part` is 0, `buyer` and `seller` are NULL, an empty `exchange_sale_conditions` is NULL.
- Order: `time_nano_part`, `action_time`, `order_id`, `aux_order_id` and `trade_id` are 0; `executed_size`,
  `trade_price` and `trade_size` are NaN; `count` is kept as an `int32`, an empty `market_maker` is NULL. An order with
  a size other than 0 needs a side in `flags`. A Quote of a symbol also gives composite orders to an Order
  subscription of that symbol.
- Candle: a symbol without a period (`AAPL`) is a trade and keeps only its close; with one (`AAPL{=m}`) all the fields
  are kept, and the symbol comes back in its normal form (`{=1m}` becomes `{=m}`). An `index` of the maximum `int64`
  is refused.
- Message: the attachment is kept, empty and NULL included.
