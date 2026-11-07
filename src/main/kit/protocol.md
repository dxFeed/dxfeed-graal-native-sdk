# The protocol of the conformance kit

This is the normative description of how the runner talks to an adapter, of the values of events, of the scenario
files and of the verdicts. Protocol version: **1**.

An adapter is a program that a wrapper author writes in the wrapper's language. It reads requests on its stdin, calls
the wrapper, and writes responses on its stdout. The runner starts it, sends the requests of the scenarios and checks
the responses. The reference adapter (`adapter/`) does the same on the C API directly and is an example.

## Transport

- Messages are UTF-8 lines, one message per line, ended by LF. The runner ignores a CR before the LF.
- Tokens in a line are separated by one space.
- The exchange is strictly request–response: the runner sends one request and waits for its response before it sends
  the next one. Each request gets exactly one response: one line, or one events block (see below).
- The runner never parses stderr. It shows the last lines of stderr when a scenario fails, so an adapter may log there
  freely.
- stdout carries only responses. Whatever else writes to the process's stdout (a log of the SDK, say) breaks the
  exchange, so an adapter keeps stdout for itself: the reference adapter writes to a copy of the original stdout and
  points stdout at stderr before it creates the isolate.
- EOF on stdin means the end: the adapter releases what it holds and exits with the code 0.
- Every wait of the runner has a deadline. An adapter that exits, stops answering, or writes a line that is not a
  response fails the scenario with the verdict `ERROR`, and the runner kills it. Each step has the step deadline
  (`--step-timeout`, 10 s by default; the handshake shares one), and the whole run the total deadline
  (`--total-timeout`, 100 s by default): the scenarios not run by then end as `ERROR`.
- A line longer than 4 MiB is not a response: the runner kills the adapter.

## Handshake

1. The adapter speaks first:

   ```text
   hello proto=1 adapter=<name> caps=<capability>,<capability>,...
   ```

   `caps=` may be empty. The capabilities:

   | Capability     | The adapter can                                                          |
   |----------------|--------------------------------------------------------------------------|
   | `publisher`    | `publish`, `publish-burst` (the wrapper binds the publisher)             |
   | `async_poll`   | `poll-async`, `await`, `release ... thread=worker` (a worker thread)     |
   | `ready_signal` | `ready-arm`, `ready-wait` (the adapter owns an OS object for the signal) |
   | `teardown`     | `teardown`                                                               |

2. The runner sends `config <key>=<value>` lines, and the adapter answers each with `ok`. The keys:

   | Key                          | Meaning                                                                                  |
   |------------------------------|------------------------------------------------------------------------------------------|
   | `expected_advanced_revision` | Always sent. The advanced revision the wrapper is built for (`DXFG_V4_ADVANCED_REVISION`) |
   | `max_buffer_size`            | Set the system property `com.devexperts.qd.impl.matrix.Agent.MaxBufferSize` to the value before the first endpoint (`dxfg_v4_system_set_property`) |

   An adapter answers `error <text>` for a key it does not know; the runner then stops with the exit code 3.

3. The runner sends `start`. The adapter applies the configuration and answers:

   ```text
   ready advanced=<enabled|disabled> version=<SDK version>
   ```

   `advanced=enabled` means that the wrapper binds the experimental functions: the expected revision equals
   `dxfg_v4_system_get_advanced_revision`. Otherwise it answers `disabled`, and the runner skips the scenarios that use
   experimental functions as `advanced-disabled`. `version` is `dxfg_v4_system_get_version`.

A first line that is not `hello`, a protocol version other than 1, or a bad answer to `config` or `start` stops the run
with the exit code 3.

## Requests

Names of endpoints, queues and lists (`<ep>`, `<q>`, `<list>`) are chosen by the scenario: letters, digits, `_`, `-`
and `.`. Symbols are percent-encoded like string values (`AAPL%7B%3Dm%7D` is `AAPL{=m}`). A timeout in milliseconds
is negative for "no timeout". Unless a row says otherwise, the response is `ok`.

| Request | Effect |
|---------|--------|
| `feature <name>` | Answers `present` or `absent`, by `dxfg_v4_system_is_feature_supported` |
| `endpoint <ep> local_hub` | Creates an endpoint with the role `DXFG_ENDPOINT_ROLE_LOCAL_HUB`: its feed receives what its publisher publishes, in the adapter's process |
| `close-endpoint <ep>` | Closes the endpoint |
| `queue <q> <ep> <Type>[,<Type>...]` | Creates a subscription of the event types on the endpoint's feed and an event queue bound to it (`dxfg_v4_EventQueue_new`) |
| `subscribe <q> <symbol>... [from=<time>]` | Adds the symbols to the queue's subscription; with `from=`, as time-series subscription symbols from that time |
| `publish <ep> <Type> <field>=<value>...` | Publishes one event; the fields not listed are zero or NULL |
| `publish-burst <ep> <Type> <symbol> <n>` | Publishes `n` events of the symbol in order, generated inside the adapter: the event `i` (from 1) has its counter field set to `i` (`bid_price` of Quote, `price` of TimeAndSale) |
| `poll <q> <timeout> [limit=<n>] [hold=<list>]` | Polls on the adapter's main thread; answers an events block. The limit goes to the SDK as is (a negative one fails the poll: the outcome `fail`), and a batch may exceed it. With `hold=`, the list is not released and is kept under that name |
| `read <list>` | Reads the held list again; answers an events block with the outcome `events` |
| `release <list> [thread=worker]` | Releases the held list, on the worker thread with `thread=worker` |
| `poll-async <q> <timeout> [limit=<n>]` | Starts the poll on the worker thread and answers at once |
| `await <q> <timeout>` | Waits for the poll of `poll-async`; answers its events block, or `pending` if it has not ended in time |
| `wakeup <q>` | `dxfg_v4_EventQueue_wakeUp` |
| `close-queue <q>` | `dxfg_v4_EventQueue_close` |
| `ready-arm <q>` | Creates the adapter's ready object (an `eventfd` or a non-blocking pipe on Linux and macOS, an auto-reset event on Windows) and sets it as the ready signal of the queue |
| `ready-wait <q> <timeout>` | Waits for the ready object; answers `signaled` (after resetting the object) or `timeout` |
| `shutdown` | `dxfg_v4_system_shutdown` |
| `teardown` | Answers `ok`, then shuts the SDK down, tears the isolate down and exits with the code 0 |
| `reset` | Closes every queue, subscription and endpoint that the scenario created, waits for the worker's poll, releases the held lists and the ready objects |

A request the adapter does not implement, or a call of the wrapper that fails, is answered `error <text>`.

### Events blocks

A poll is answered with a block:

```text
events <outcome> dropped=<n>
event <Type> <field>=<value> <field>=<value> ...
...
end
```

- `<outcome>` is `events` (the poll returned a list), `no_events` (`DXFG_EVENT_QUEUE_NO_EVENTS`), `closed`
  (`DXFG_EVENT_QUEUE_CLOSED`) or `fail` (`DXFG_EXECUTE_FAIL`). Only `events` has event lines.
- `dropped` is `dxfg_v4_EventQueue_getDroppedEvents` after the poll.
- An event line lists every field the adapter maps, in any order; the runner compares only the fields a scenario
  names.

## Values

An event line is `event <Type> <field>=<value>...`.

- `<Type>` is the event type name: `Quote`, `Profile`, `TimeAndSale`, `Order`, `Candle`, `Message`.
- `<field>` is the name of the member of the C struct of the type (`dxfg_quote_t` and so on, as in the API
  description `dxfg-api.json`). The members of an embedded base struct are flattened, recursively: `market_event` gives
  `event_symbol` and `event_time`, and Order also gets the members of `order_base` (`index`, `price`, `size`, ...).
- Integers and chars: decimal, e.g. `sequence=17`, `exchange_code=81`.
- Doubles: `nan`, `inf`, `-inf`, or a decimal with up to 17 significant digits (`%.17g` in C), e.g. `bid_price=1.5`,
  `size=-0`.
- Strings: UTF-8, percent-encoded. An adapter must encode every byte that is not a letter, a digit, `-`, `.` or `_`
  (`%20` for a space, `%7E` for `~`, `%C3%A9` for `é`). `field=` is the empty string, `field=~` is NULL.

### Comparison

- NULL (`~`) equals only NULL.
- Two integers are compared as integers.
- Two numbers of which one is not an integer are compared as doubles, by their bits (so `-0` differs from `0`), and any
  NaN equals any NaN.
- Anything else is compared as decoded strings.

## Scenarios

A scenario is a `.scenario` file. Lines are trimmed; empty lines and lines starting with `#` are ignored. A `#` inside
a line is not a comment. Header lines come before the steps:

```text
scenario <name>
requires feature <feature>
requires capability <capability>
uses experimental
config <key>=<value>
```

A scenario that requires a feature is skipped as `feature-absent` if the adapter answers `absent`; one that requires a
capability is skipped as `capability-absent` if the `hello` lacks it; one that uses experimental functions is skipped
as `advanced-disabled` if the adapter answers `advanced=disabled`. `config` lines are allowed only in own-process
scenarios: the runner sends them before `start`.

Steps:

```text
send <request>
expect <request> -> <matcher>
exit <code>
```

`send` expects `ok`. `exit` is allowed only in own-process scenarios, as the last step: the adapter exits with the
code.

Matchers:

| Matcher | Holds when |
|---------|-----------|
| `ok` | the response is `ok` |
| `error` | the response starts with `error` |
| `reply <text>` | the response is exactly `<text>` (e.g. `reply signaled`, `reply pending`) |
| `outcome <outcome>` | the response is an events block with the outcome |
| `count <op> <n>` | the response is an events block with the outcome `events` or `no_events`, and its number of events compares so (`=`, `<=`, `>=`) |
| `events` ... `end` | the events listed on the following `event` lines arrived, in this order, with the listed fields equal. With a `poll` request the runner polls again until that many events have arrived |
| `until <symbol>` ... `end` | the runner polls again until an event of the symbol (the sentinel) arrives, then runs the checks on the following lines over everything received |

The checks of `until`:

| Check | Holds when |
|-------|-----------|
| `count <symbol> <op> <n>` | the number of the received events of the symbol compares so |
| `last <symbol> <field>=<value>` | the last received event of the symbol has the value |
| `increasing <symbol> <field>` | the values of the field over the events of the symbol strictly increase (order kept, no duplicates) |
| `accounted <symbol> <n>` | the received events of the symbol plus the last `dropped` equal `n` |
| `dropped <op> <n>` | the last `dropped` compares so |
| `monotonic dropped` | `dropped` never decreased from one poll to the next |

Where a file lives decides its process. The scenarios in a directory named `own-process` run in an adapter process
each, with the scenario's `config` lines. All the others run one after another in one adapter process; after each of
them the runner sends `reset`. Each scenario uses symbols that start with its own name, so nothing it publishes
reaches the next one.

## Verdicts

Each scenario ends with one line:

- `PASS <name>`;
- `FAIL <name>`: a response did not match, the adapter answered `error` where `ok` was expected, or `reset` failed;
- `SKIP <name> (<reason>)`: the reasons are `capability-absent`, `advanced-disabled`, `feature-absent`;
- `ERROR <name>`: the adapter exited, missed a deadline, or wrote a line that is not a response.

The indented lines after `FAIL` and `ERROR` name the scenario line, the request, what was expected and what came, and
show the end of the adapter's stderr. A failed scenario of a shared process taints it: the runner kills the adapter
and starts a fresh one for the next scenario.

The run ends with `summary passed=<n> failed=<n> skipped=<n> errors=<n> [skips=<reason>:<n>,...]`. With `--strict`, a
skip whose reason is not allowed by `--allow-skip <reason>` is a failure, and so is a run in which no scenario passed
(the line `FAIL no scenario passed, and strict mode needs at least one` follows the summary).

| Exit code | Meaning |
|-----------|---------|
| 0 | every scenario passed or was skipped as allowed |
| 1 | a scenario failed, no scenario was selected, or none passed in strict mode |
| 2 | the adapter misbehaved or a deadline passed (an `ERROR` verdict) |
| 3 | a handshake or protocol-version mismatch: the run stopped |
| 64 | bad usage, or a scenario file breaks the grammar |
