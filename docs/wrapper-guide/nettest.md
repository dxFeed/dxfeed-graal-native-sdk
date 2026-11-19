# NetTest specification {#wrapper_nettest}

A NetTest is the command-line benchmark of an API: it publishes (mode `p`) or receives (mode `c`) generated market
events at a controlled rate and reports the rate and the lag. The publish benchmark runs the NetTest of every API (the
C API, Java, C++, .NET, Rust, Go) and QD's own `NetTest` on one stand, through one multiplexor, one consumer and one
parser of the output, so the tools must agree on their command line, their events and their output. This page is that
contract, version `nettest/1`, from SDK 3.18.0. It is part of the wrapper guide because the tools it binds are written
on top of the wrappers, and it is versioned with the SDK; the stand, its runner and its analyzer are not part of the
SDK.

"Must" is required for conformance, "should" is a recommendation that the kit does not test, "may" is optional. The
SDK's own tools, the C API `NetTest` and the Java `NetTestApi`, follow the sections from the versions in
@ref wrapper_nettest_sdk_tools.

## Terms {#wrapper_nettest_terms}

- **Instance**: one endpoint of the tool with its own thread or threads, numbered `1..C`.
- **Batch**: the events passed to one publishing call.
- **Interval**: one statistics period (`-s`).
- **e2e lag**: the time a consumer receives an event minus the event's own time, in nanoseconds.
- **Contract**: how QD delivers a record: `ticker` keeps the last value per symbol and may conflate updates, `stream`
  delivers every event.

Every tool prints the line `spec_version nettest/1` in its `--help` output and writes `nettest/1` in its summary file.

## Command line {#wrapper_nettest_cli}

```text
NetTest p|c <address> [options]
```

The name of the tool is free (`NetTest`, `NetTestApi`, `net_test`, or a subcommand of a larger binary); what follows
it is the same for every tool. The mode is the first argument. `<address>` is the only other positional argument and
follows the address syntax of `DXEndpoint.connect`: `host:port` connects, `:port` listens. The options may come before
or after `<address>`.

| Option | Class | Modes | Default | Meaning |
|---|---|---|---|---|
| `-S <n>`, `-S <n>@<file>` | common | p, c | required | `n` generated symbols, or the first `n` symbols of an IPF file in file order; `n` from 1 to 100000000 |
| `-C <n>` | common | p, c | 1 | instances, from 1 to 1000; not above 1 with a `:port` address |
| `-r`, `--rate-limit */<rate>` | common | p | none: as fast as possible | the total events per second of all instances, from 1 to 1e12, with an optional `k`, `M` or `G` |
| `-s`, `--stat <period>` | common | p, c | `10s` | the statistics period |
| `-n`, `--name <name>` | common | p, c | `NetTest` | the name in the output lines; the endpoints are named `<name>.<i>` |
| `-c`, `--collector <contract>` | common | p, c | none | the contract, `stream` or `ticker` (@ref wrapper_nettest_roles) |
| `--event <type>` | common | p, c | `Trade` | `Trade` or `TimeAndSale` |
| `--batch <n>` | common | p | 100 | events per publishing call, from 1 to 4194304 |
| `--reuse` | common | p | off | republish the events of the pool instead of new events per batch |
| `--role <role>` | common | p, c | by `-c` | `PUBLISHER` or `STREAM_PUBLISHER` on `p`, `FEED` or `STREAM_FEED` on `c` |
| `--duration <time>` | common | p, c | none | end the run after the time with exit code 0 |
| `--warmup <time>` | common | p, c | none | the warm-up (@ref wrapper_nettest_consumer) |
| `--summary <file>` | common | p, c | none | write the summary file (@ref wrapper_nettest_summary) |
| `--metrics-port [<host>:]<port>` | common | p, c | none | serve `/metrics` (@ref wrapper_nettest_metrics); the host defaults to `127.0.0.1`, port 0 takes a free port |
| `-D<key>=<value>` | common | p, c | | a system property, set before the first endpoint |
| `--help` | common | | | print the usage with the line `spec_version nettest/1` to stdout and exit with 0 |
| `-X<option>`, `-XX:<option>` | optional | p, c | | an option of the runtime of a native tool, for example of a Graal isolate |
| `--legacy` | optional | p | off | publish through a deprecated entry point of the API |
| `-w`, `--wildcard` | rejected | | | |

- A time (`-s`, `--duration`, `--warmup`) is a positive integer with the unit `ms`, `s` or `m`: `500ms`, `10s`, `7m`.
- A rate is a positive decimal number with at most one dot and an optional `k`, `M` or `G`: `*/500k`, `*/1.5M`.
- A tool takes `--option value` and `--option=value` for its long options. A repeated option takes its last value,
  except `-D` and `-X`, which apply in the command-line order.
- A tool should also pass the `-D` properties to its endpoint builders where its API allows; `dxscheme.nanoTime` must
  reach them (@ref wrapper_nettest_time).
- A tool may add options of its own; their names must not start with `-S`, `-C`, `-r`, `-s`, `-n`, `-c`, `-D`, `-X`
  or `-w` and must not take a name of this table.
- Without arguments, a tool prints its usage and exits with 2.

An invalid command line exits with 2 before anything starts. The first line on stderr is `<tool>: <text>`, where
`<tool>` is the name of the tool; the usage may follow. The texts are normative: `cli-vectors.json` of the kit pins
each one for its command line, and the rest of stderr is free. The first error found wins. In particular:

- an unknown option: `unknown flag <option>`;
- a number out of its limits: `<option> needs a number from 1 to <max>: '<value>'`;
- an option of the class "optional" that the tool does not implement: a text that starts with
  `unsupported option: <option>`;
- an option of the mode `p` on `c`: `<option> is an option of the mode p`;
- `-w`, `--wildcard`: `unsupported QD flag: <option> (the wildcard subscription)`.

An option that a native runtime rejects (a wrong `-X` value) also exits with 2, after the runtime's own message, with
the line

```text
NetTest: the isolate rejected one of the options: <the options passed to the runtime, as given, separated by one space>
```

which is the same in every tool, whatever its own name; the expression `isolate_rejection` of the kit matches it.

## Contracts and roles {#wrapper_nettest_roles}

`-c` selects the contract that the consumer receives and, through it, the default roles of the endpoints:

| `-c` | Role on `p` | Role on `c` | Note |
|---|---|---|---|
| none | `PUBLISHER` | `STREAM_FEED` | PUBLISHER writes the ticker and the stream contracts, so either consumer receives |
| `stream` | `STREAM_PUBLISHER` | `STREAM_FEED` | the contract of the benchmark |
| `ticker` | `PUBLISHER` | `FEED` | the consumer receives conflated Trade events; the rate is not comparable with `stream` |
| `history` | | | reserved: exit code 2 with `-c history is reserved` |

What each role delivers, as checked against the QD of the SDK:

- PUBLISHER writes Trade to the ticker and the stream contracts, TimeAndSale to the stream contract.
- STREAM_PUBLISHER writes the stream contract only. Trade from it never reaches a FEED subscription, and nothing
  reports an error.
- FEED receives Trade through the ticker contract (conflated) and TimeAndSale through the stream contract.
- STREAM_FEED receives both through the stream contract.

`--role` replaces the role that `-c` selects only when the consumer still receives the contract of `-c`:

- `p -c stream --role PUBLISHER` is valid: PUBLISHER also writes the stream contract.
- `c -c stream --role FEED` is valid with `--event TimeAndSale` and exits with 2 with `--event Trade`, which FEED
  receives through the ticker contract: `--role FEED conflicts with -c stream for Trade`.
- `-c ticker` needs `--event Trade` (`-c ticker needs --event Trade: TimeAndSale has no ticker contract`) and the
  roles PUBLISHER and FEED: `--role STREAM_PUBLISHER conflicts with -c ticker for Trade`,
  `--role STREAM_FEED conflicts with -c ticker for Trade`.
- Without `-c`, either role of the mode is valid. The consumer receives conflated events when its role is FEED and
  the event is Trade (`conflated: true` in the summary).

A multiplexor between the tools runs with `-c stream`.

## Rate limit and schedule {#wrapper_nettest_rate}

- `-r */<rate>` is one budget for all instances together. The limiter grants whole batches.
- The limiter keeps an intended schedule from its first grant `t0`: batch `k`, counted over all instances, is due at
  `T(k) = t0 + k * batch / rate`, and is published at the later of the current time and `T(k)`. Ahead of the schedule,
  the tool sleeps until `T(k)` with the platform's sleep, without a busy wait. Behind it, the tool does not sleep and
  catches up without a bound; it never drops a batch.
- Each batch, in this order: (1) wait until the later of now and `T(k)`; (2) read the clock once: this time
  `publish_start` stamps every event of the batch (@ref wrapper_nettest_time) and is the reference of the slip; (3)
  fill the batch; (4) call the publishing function; (5) read the elapsed time. The publisher's lag, `publish_call` of
  the summary and `nettest_publish_seconds_total` cover (3) and (4); the consumer's e2e lag starts at (2). So no
  measured value includes the wait of the limiter. Without `-r`, step (1) is skipped.
- `schedule_slip = publish_start - T(k)` for each batch, 0 when on time. The tool reports the maximum and the slip at
  the end of each interval. A sustainable run has a slip that does not grow.
- Without `-r` there is no schedule and no slip.

## Events and pools {#wrapper_nettest_events}

The C API `NetTest`'s algorithm is normative for every tool, so that all tools put the same payload on the wire.

- The events are Trade or TimeAndSale.
- All arithmetic is unsigned 64-bit with wrap-around: logical right shifts and unsigned remainders (Java `>>>` and
  `Long.remainderUnsigned`, .NET `ulong`, Go `uint64`, Rust `u64` with `wrapping_*`). `pool-vectors.json` of the kit
  pins the results.
- **Pool.** Each publisher instance holds one event per symbol of the whole symbol set; publishers do not slice it.
  A pool event carries only its symbol; the other fields keep their defaults, with NaN in the price, the size and the
  other numeric fields that the event's defaults leave at NaN (Trade: `price`, `change`, `size`, `dayVolume`,
  `dayTurnover`; TimeAndSale: `price`, `size`, `bidPrice`, `askPrice`).
- **Random source.** xorshift64* per instance. Instance `i` (from 1) takes `seed = 0x9E3779B97F4A7C15 * i`; the state
  starts at `seed * 0x9E3779B97F4A7C15 + 0x2545F4914F6CDD1D`, or at `0x2545F4914F6CDD1D` when that is 0. Each draw:
  `s ^= s >> 12; s ^= s << 25; s ^= s >> 27; return s * 0x2545F4914F6CDD1D`.
- **Table.** 1024 rows, built at the start in row order with two draws per row: first
  `price = (100 + r % 100000) / 100.0` (1.00 to 1000.99), then `size = 1 + r % 1000`.
- **Each event of a batch** takes one draw `r`: the symbol `(r >> 32) % poolSize`, the row `(r >> 8) % 1024`. The
  event gets the time of the batch, the instance's next sequence, the row's price and size.
- With `--reuse`, the pool event of the drawn symbol is stamped in place and published: a symbol drawn twice appears
  twice in the batch with the later stamp. Without it, each event is a new copy of the pool event. A tool whose API
  copies the events anyway writes `reuse_effective: false` in its summary.
- So a tool puts on the wire the bytes per record of the C API `NetTest` with the same settings, within 1%. The
  nanosecond part (@ref wrapper_nettest_time) adds about 3 bytes per record.

## Time, sequence and symbols {#wrapper_nettest_time}

- One clock read per batch, after the limiter's wait and before the fill (step 2 of @ref wrapper_nettest_rate): the
  wall clock with nanosecond resolution (C and C++ `std::chrono::system_clock`, Java `Instant.now()`, .NET
  `DateTime.UtcNow` with a `Stopwatch` offset where its resolution is coarser, Rust `SystemTime::now()`, Go
  `time.Now()`).
- From that time `t` in nanoseconds since the epoch: the time in milliseconds `t / 1000000`, the nanosecond part
  `t % 1000000`, and the sequence, a 22-bit counter per instance that grows by one per event and wraps at `2^22`.
  Trade packs the time and the sequence into its time-and-sequence field, TimeAndSale into its index, as the seconds
  in bits 32 to 63, the milliseconds in bits 22 to 31 and the sequence in bits 0 to 21; both set the nanosecond part.
- Every endpoint of a tool is built with `dxscheme.nanoTime=true` through its endpoint builder, or with the value of
  `-Ddxscheme.nanoTime` when the command line sets it (`-Ddxscheme.nanoTime=false` turns it off for a run against
  peers without it). The system property alone is not enough: only the PUBLISHER and FEED endpoints read it, and an
  event that passes a STREAM_PUBLISHER or STREAM_FEED endpoint without the builder property loses the nanosecond part.
- The nanosecond part reaches the consumer only when every hop has the property: the publisher, a multiplexor
  (`-Ddxscheme.nanoTime=true` on its command line) and the consumer. A hop without it still passes the events, with
  the nanosecond part 0.
- Generated symbols: SplitMix64 with the seed `0x5EED0FD4F3ED5EED`; each draw
  `state += 0x9E3779B97F4A7C15; z = (state ^ (state >> 30)) * 0xBF58476D1CE4E5B9; z = (z ^ (z >> 27)) * 0x94D049BB133111EB; return z ^ (z >> 31)`.
  Each symbol takes the length `next % 6 + 1`, then each letter `'A' + next % 26`; a symbol already generated is
  skipped. The order is the order of first appearance; `symbols-generated-10.txt` of the kit lists the first ten.
- An IPF file gives its first `n` symbols in file order. A file with fewer symbols gives a warning on stderr and the
  smaller set; a file without symbols is a failure (exit code 1).
- The consumer instances slice the set: instance `i` (from 1) of `C` subscribes to the symbols with the index `j`
  (from 0) where `j % C == i - 1`, so the instances together subscribe to each symbol once.

## Output lines {#wrapper_nettest_lines}

All the lines below go to stdout. A tool may put anything before `{*` (QD's logging puts `I yyMMdd HHmmss.SSS `
there); a parser matches from `{*`. A line ends with the platform's line break; a parser strips `\r` and `\n`.

A statistics block, once per interval. The intervals end at the multiples of the period on the wall clock; the first
tick after the start only starts the count and prints nothing. At the stop, a last block covers the time since the last
tick at its real length, and is printed only when it is at least one second long.

```text
{*NetTestProducerSide-<name>*} data <rps> rps, lag <us> us, instances <C>, connections <M>
    <name>.1 data <rps> rps, lag <us> us
    ...
{*NetTestConsumerSide-<name>*} data <rps> rps, lag <us> us, instances <C>, connections <M>
    <name>.1 data <rps> rps, lag <us> us
```

- `rps`: the records of the interval per second of its real length, `records * 1000000000 / interval_ns` with integer
  division, 0 for an empty interval.
- `lag` on `p`: the time of the fill and the publishing call of a batch (steps 3 and 4), weighted by its records, in
  microseconds: `sum(batch_records * batch_ns) / records / 1000` with integer division, 0 without records.
- `lag` on `c`: the mean e2e lag of the interval in microseconds, `sum(lag_ns) / records / 1000`, divided toward
  zero; it may be negative (`-12`); 0 without records.
- `rps` and `lag` are grouped by thousands with `,` (`1,000,012`, `-1,234`), as Java's
  `NumberFormat.getIntegerInstance(Locale.US)` prints them; `instances` and `connections` are not grouped.
- `connections`: the open connections of the connectors of all instances.
- A line per instance follows the summary line, four spaces, the endpoint name `<name>.<i>` and the instance's own
  values, also when `C` is 1.

The runtime line follows each statistics block in the same write, so that no other output comes between them:

```text
{*NetTestRuntime-<name>*} heap <used> MB of <committed> MB, gc <count> (<time> ms), cpu <percent>%
```

- `heap`: the used and the committed heap at the end of the interval in MiB, rounded down, grouped by thousands.
- `gc`: the collections of the interval and their total time in milliseconds.
- `cpu`: the CPU time of the process in the interval, `cpu_ns * 100 / interval_ns` with integer division; 100% is one
  core, and the value goes above 100 on several cores.
- A value that the runtime does not give is `n/a`, which replaces the number with its unit: `heap n/a of 256 MB`,
  `gc n/a`, `cpu n/a`. A cumulative counter that is absent in either sample, or went back, is also `n/a`.

The metrics line, once at the start, when `--metrics-port` is given, after the port is bound:

```text
{*NetTestMetrics-<name>*} http://<host>:<port>/metrics
```

The expressions below are normative and are also in `regexes.json` of the kit. They use only what ECMAScript, Java,
.NET, RE2 and Rust's `regex` read alike: bracket expressions instead of escapes, `[0-9]` instead of `\d`,
`[^ \t\r\n]` instead of `\S`. They are matched against a line without its line break; the summary, runtime and metrics
expressions are searched anywhere in the line, the others are anchored.

```text
summary:            [{][*]NetTest(Producer|Consumer)Side-([^*]+)[*][}] data ([0-9,]+) rps, lag (-?[0-9,]+) us, instances ([0-9]+), connections ([0-9]+)$
instance:           ^    ([^*]+)[.]([0-9]+) data ([0-9,]+) rps, lag (-?[0-9,]+) us$
runtime:            [{][*]NetTestRuntime-([^*]+)[*][}] heap (([0-9,]+) MB|n/a) of (([0-9,]+) MB|n/a), gc (([0-9,]+) [(]([0-9,]+) ms[)]|n/a), cpu (([0-9,]+)%|n/a)$
metrics:            [{][*]NetTestMetrics-([^*]+)[*][}] http://([^ \t\r\n]+):([0-9]+)/metrics$
isolate_rejection:  ^NetTest: the isolate rejected one of the options: ([^ \t\r\n]+( [^ \t\r\n]+)*)$
```

`lines.json` of the kit has byte samples of each line with the values they are made of.

## Consumer {#wrapper_nettest_consumer}

- Each instance subscribes to its slice of the symbols for `--event` with the role of @ref wrapper_nettest_roles. The
  listener is added before the symbols, and the endpoint connects last.
- In each call of the listener, the tool reads the clock once; each event's lag is
  `now_ns - (time_ms * 1000000 + nano_part)`. It enters the histogram (@ref wrapper_nettest_summary) and the sum of the
  interval. A negative lag (a clock step or skew) enters the sum with its sign, counts in `negative` and enters the
  histogram as 0; it is never dropped.
- When the consumer's endpoints have `dxscheme.nanoTime=true` (the default, @ref wrapper_nettest_time) and the first
  1000 received events all have the nanosecond part 0, the consumer stops with the status `nano_missing` and exit
  code 1: a hop of the path lacks `dxscheme.nanoTime`. With `-Ddxscheme.nanoTime=false` the check is off, and the lag
  is computed with the nanosecond part 0.
- Starvation: with open connections and no records for 30 s after the first record, or for 60 s after the connection
  when no record came at all, the consumer stops with the status `no_data` and exit code 1.
- `symbols_seen`: the distinct symbols received.
- `--warmup` on `c` starts at the first received record, not at the start of the tool, since the consumer starts before
  the publisher; on `p` it starts at the first grant `t0`. An interval that overlaps the end of the warm-up belongs to
  the warm-up. The percentiles and the maximum of the e2e lag in the summary come from the records after the warm-up
  only.

## Summary file {#wrapper_nettest_summary}

With `--summary <file>`, the tool writes a JSON summary of the run (schema `nettest-summary/1`) after the stop and
before its runtime is torn down: to `<file>.tmp`, then renamed to `<file>`. The path is checked at the start: a
directory that does not exist or cannot be written exits with 2. All keys are always present; an absent value is
`null`. `nettest-summary.schema.json` of the kit is normative; the example is not. A consumer:

```json
{
  "schema": "nettest-summary/1",
  "spec_version": "nettest/1",
  "tool": {"implementation": "java-api", "name": "NetTestApi", "version": "3.18.0", "sdk_version": "3.18.0",
           "qd_version": "3.357", "build": {"vendor": null, "march": null, "gc": null}},
  "side": "consumer",
  "name": "bench",
  "args": ["c", "127.0.0.1:7002", "-S", "100000@symbols.ipf", "-c", "stream"],
  "config": {"address": "127.0.0.1:7002", "symbols": 100000, "symbols_source": "file", "instances": 1,
             "rate_limit": null, "batch": null, "event": "Trade", "collector": "stream", "role": "STREAM_FEED",
             "reuse": null, "reuse_effective": null, "conflated": false, "period_ms": 10000,
             "warmup_ms": 30000, "duration_ms": 420000, "nano_time": true, "isolate_arguments": []},
  "start_epoch_ns": 1763380800000000000,
  "end_epoch_ns": 1763381220000000000,
  "status": "ok",
  "exit_code": 0,
  "error": null,
  "records": 210000000,
  "rate": 500000,
  "time_source": "actual",
  "time_resolution": "ns",
  "intervals": [
    {"start_epoch_ms": 1763380800000, "end_epoch_ms": 1763380810000, "phase": "warmup", "records": 5000000,
     "rate": 500000, "lag_mean_ns": 412000, "lag_p50_ns": 380000, "lag_p99_ns": 1200000, "lag_max_ns": 3400000,
     "publish_ns_per_event": null, "slip_max_ns": null, "slip_end_ns": null, "connections": 1}
  ],
  "e2e_lag": {"count": 195000000, "negative": 0, "p50_ns": 381000, "p90_ns": 560000, "p99_ns": 1180000,
              "p999_ns": 2900000, "p9999_ns": 5100000, "max_ns": 9800000, "mean_ns": 405000,
              "histogram": {"lowest": 1, "highest": 3600000000000, "digits": 3}},
  "publish_call": null,
  "schedule_slip": null,
  "symbols_seen": 100000,
  "runtime": {"heap_used_mb": 310, "heap_committed_mb": 1024, "gc_count": 41, "gc_time_ms": 380,
              "cpu_percent_mean": 87},
  "connectors": [{"address": "127.0.0.1:7002", "v1": {"read_bytes": 0, "written_bytes": 0,
                  "read_data_records": 0, "written_data_records": 0, "active_connections": 1,
                  "closed_connections": 0}, "v2": null}]
}
```

- A publisher differs in `side: "producer"`, `e2e_lag: null`, `symbols_seen: null`,
  `publish_call: {"calls": n, "events": n, "ns_total": n, "ns_per_event_mean": n}` (steps 3 and 4 of
  @ref wrapper_nettest_rate, the span of the publisher's lag) and
  `schedule_slip: {"max_ns": n, "end_ns": n, "growth_ns_per_s": n}`, which is `null` without `-r`.
- `phase` is `warmup` or `measure`. The totals of `e2e_lag` leave out the warm-up intervals; each interval keeps its
  own percentiles. The records of the intervals add up to `records`.
- The percentiles come from an HdrHistogram with the lowest value 1 ns, the highest 3600 s and 3 significant digits:
  a percentile is the value that Java's HdrHistogram 2.1 gives for it (`getValueAtPercentile`, the highest value
  equivalent to its bucket); `histogram-vectors.json` of the kit has recorded values and their percentiles. A lag
  above the highest value enters the histogram as the highest value. `max_ns` and `mean_ns` are exact: the largest
  lag and the sum of the lags over their count, divided toward zero; the negative lags enter both with their sign.
- `time_source` is always `actual` in `nettest/1`: the events carry the clock read of their batch. `time_resolution`
  is `ns` when the nanosecond part is set, `ms` otherwise.
- `implementation` is one of `qd`, `c-api`, `java-api`, `cpp-api`, `net-api`, `rust-api`, `go-api`.
- `status` is one of `ok`, `stopped_by_signal`, `error`, `no_data`, `nano_missing` (@ref wrapper_nettest_exit).

## Metrics {#wrapper_nettest_metrics}

With `--metrics-port`, `GET /metrics` returns the text exposition format of Prometheus with the content type
`text/plain; version=0.0.4; charset=utf-8`. Every series has the labels `tool` (the `implementation` of the summary),
`side` (`producer` or `consumer`) and `name`. `nettest_records_total` and `nettest_connections` have a series per
instance with the label `connection` (`1..C`); the other metrics have one series per tool. The label `instance` is not
used: Prometheus reserves it. A parser reads the `le` values of the buckets as numbers, whatever their notation.

| Metric | Type | Unit | Side | Source |
|---|---|---|---|---|
| `nettest_info{spec_version,version,sdk_version,role,event,collector}` | gauge, always 1 | | both | the configuration |
| `nettest_records_total` | counter | records | both | the published or received events |
| `nettest_e2e_lag_seconds` | histogram | s | c | buckets 50e-6, 100e-6, 250e-6, 500e-6, 1e-3, 2.5e-3, 5e-3, 10e-3, 25e-3, 50e-3, 100e-3, 250e-3, 1, +Inf |
| `nettest_e2e_lag_negative_total` | counter | events | c | the negative lags |
| `nettest_publish_seconds_total` | counter | s | p | the fill and the publishing call (steps 3 and 4) |
| `nettest_publish_calls_total` | counter | calls | p | the publishing calls |
| `nettest_schedule_slip_seconds` | gauge | s | p | the slip of the last batch |
| `nettest_connections` | gauge | | both | the open connections |
| `nettest_connector_read_bytes_total`, `nettest_connector_written_bytes_total` | counter | bytes | both | connector statistics v1 |
| `nettest_connector_read_records_total`, `nettest_connector_written_records_total` | counter | records | both | connector statistics v1 |
| `nettest_connector_lag_seconds_total` | counter | s | c | connector statistics v2, absent without them |
| `nettest_connector_rtt_seconds_total`, `nettest_connector_sub_records_total` | counter | s, records | both | connector statistics v2, absent without them |
| `nettest_runtime_heap_used_bytes`, `nettest_runtime_heap_committed_bytes` | gauge | bytes | both | the runtime |
| `nettest_runtime_gc_total`, `nettest_runtime_gc_seconds_total` | counter | collections, s | both | the runtime |
| `nettest_runtime_cpu_seconds_total` | counter | s | both | the CPU time of the process |

A scrape reads counters that the tool keeps; it must not call into a native runtime from a thread that is not attached
to it. The port is bound at the start; a port in use exits with 1 before the first endpoint.
`metrics-sample-producer.txt` and `metrics-sample-consumer.txt` of the kit are scrapes of each side.

## Connector statistics {#wrapper_nettest_connectors}

- v1 (SDK 3.14.0 and newer): the bytes and the data records read and written, the open and the closed connections;
  cumulative.
- v2 (a later SDK version): adds the subscription records read and written, the sums of the data read and write lag
  and of the read and write round-trip times in microseconds; cumulative. The connector lag is the growth of the lag
  sum over the growth of the data records; the round-trip time is the growth of the read plus write sums over the
  growth of the data and subscription records of both directions; both are 0 until the first heartbeat. The publisher
  has no time marks on its path, so its lag means nothing and is not exported on `p`.
- A tool reads the connector statistics at most once per interval.

## Exit codes and status {#wrapper_nettest_exit}

| Exit code | When | `status` of the summary |
|---|---|---|
| 0 | `--duration` ended the run, or SIGINT or SIGTERM after the start | `ok`, `stopped_by_signal` |
| 1 | a failure at run time: an endpoint, a port in use, an unreadable symbol file, the summary file, the watchdog, the nanosecond check | `error`, `no_data`, `nano_missing` |
| 2 | an invalid command line, an unwritable summary path, a rejected runtime option | no summary file |

Ctrl+C (SIGINT) and SIGTERM end the run like `--duration`: the last block, then the summary, then exit code 0.

## Sustainability {#wrapper_nettest_sustainability}

The tools only report; the analyzer of the stand decides. For reference, a probe at the target rate `R` passes when,
over its measured window, every interval's publisher rate is at least `0.99 R`; the consumer's records equal the
publisher's within what was published during the last drain margin; the multiplexor drops nothing; the slip grows
less than 1 ms per minute; the slope of the consumer's per-interval `lag_p99_ns` is below 5% of its median per minute;
both sides end with the status `ok`. The thresholds are parameters of the analyzer.

## What the tests of a tool prove {#wrapper_nettest_tests}

1. The parser: every entry of `cli-vectors.json`, its exit code and its first error line.
2. The formatter: every entry of `lines.json`, byte for byte.
3. The pools: every entry of `pool-vectors.json` (the draws, the table rows, the symbol and row choices) and
   `symbols-generated-10.txt`.
4. The summary: the summary of a short run validates against `nettest-summary.schema.json`, and its interval records
   add up to `records`.
5. The metrics: a scrape has every metric of its side with its `TYPE` line, and the content type is exact.
6. The histogram: the percentiles of `histogram-vectors.json`.
7. A loopback run: `p` to `c` for at least 5 s, in two processes or in one where the API allows two endpoints. Both
   sides' lines match the expressions; the consumer's e2e lag is positive and below 1 s; `symbols_seen` equals the set
   (with a small `-S`, for example 100, so that every symbol is drawn). With `-r` and a test hook that holds the
   publisher 50 ms ahead of the schedule, the consumer's mean lag stays well below 50 ms.
8. The exit codes: an invalid option exits with 2, a port in use with 1, `--duration` with 0 and the summary written.
9. The nanosecond part: a test consumer, a STREAM_FEED endpoint with `dxscheme.nanoTime=true` on its builder, reads
   the events of the tool's publisher and sees a nonzero nanosecond part; with `-Ddxscheme.nanoTime=false` on the
   publisher it sees 0 in every event. On `c`: against a publisher with `-Ddxscheme.nanoTime=false`, the tool's
   consumer stops with `nano_missing` by default and runs to the end with `-Ddxscheme.nanoTime=false`.

## The kit {#wrapper_nettest_kit}

The kit is in `docs/wrapper-guide/nettest/` of the SDK sources, next to this page, and the tests of each tool read it
from there:

| File | What it holds |
|---|---|
| `nettest-summary.schema.json` | the JSON Schema (2020-12) of the summary file |
| `cli-vectors.json` | command lines with their exit code and their first error line, or the settings they give |
| `lines.json` | the values of statistics blocks and runtime lines with the exact text of each, and lines printed by the SDK tools |
| `regexes.json` | the expressions of @ref wrapper_nettest_lines |
| `histogram-vectors.json` | recorded values with their count and percentiles |
| `pool-vectors.json` | the first draws of instances 1 and 2, table rows, the symbol and row choices, packed times |
| `symbols-generated-10.txt` | the first ten generated symbols, one per line |
| `metrics-sample-producer.txt`, `metrics-sample-consumer.txt` | a scrape of each side |

An entry of `cli-vectors.json` may list `requires`: the optional features it needs (`isolate` for `-X`, `legacy`), or
their absence (`!isolate`); a tool skips the entries whose features it does not match. Each entry also names its
section, so that a tool which does not implement a section yet can skip it by name and list what it skips.

## Differences from QD NetTest {#wrapper_nettest_qd}

- `-c` selects dxFeed roles (@ref wrapper_nettest_roles), not QD collectors; `history` and QD's suffixes (`se`, `nwc`,
  `ts`, `filtered=`) are not taken. `-w` is rejected. One address only.
- The default event is Trade; QD's default record is TimeAndSale (`-Dcom.devexperts.qd.tools.NetTest.record`).
- `-S` is required (QD: 100000 by default). QD's symbol splits (`/r`, `/s`), symbol lengths (`{len,max}`) and limiter
  bucket (`;<time>`) are rejected. Generated symbols differ from QD's: a run that mixes a NetTest with QD NetTest uses
  `-S <n>@<file>`.
- Without `-c`, `p` uses PUBLISHER and `c` uses STREAM_FEED; QD uses `ticker` for both.
- The publisher's `lag` is the fill and the publishing call without the limiter's wait; QD's includes the wait. The
  consumer's `lag` is in real microseconds from the nanosecond time; QD's is milliseconds times 1000.
- Added: `--event`, `--batch`, `--reuse`, `--role`, `--duration`, `--warmup`, `--summary`, `--metrics-port`, `-X`,
  `--legacy`; the runtime and the metrics lines; the last block at the stop.

## The SDK tools {#wrapper_nettest_sdk_tools}

The SDK version from which each SDK tool follows a section; "not yet" sections follow in their own changes, and the
README of each tool lists how it differs from this page until then.

| Section | C API `NetTest` | Java `NetTestApi` |
|---|---|---|
| @ref wrapper_nettest_cli | 3.18.0 without the options of the consumer, summary and metrics sections | not yet: mode `p` only, other error texts |
| @ref wrapper_nettest_roles | 3.18.0 | not yet: `--role` without `-c` |
| @ref wrapper_nettest_rate | not yet: a token bucket | not yet: a token bucket |
| @ref wrapper_nettest_events | 3.16.0 | not yet: another generator |
| @ref wrapper_nettest_time | 3.18.0 (generated symbols since 3.16.0) | not yet: the time and `dxscheme.nanoTime` since 3.18.0, QD's generated symbols |
| @ref wrapper_nettest_lines | 3.17.0 for `p`, 3.18.0 for `c`; no `NetTestMetrics` line | not yet: the label `NetTestApiProducerSide` |
| @ref wrapper_nettest_consumer | 3.18.0 without `--warmup`, the histogram and `symbols_seen` | not yet |
| @ref wrapper_nettest_summary | not yet | not yet |
| @ref wrapper_nettest_metrics | not yet | not yet |
| @ref wrapper_nettest_exit | 3.17.0 without the summary file | 3.17.0 without the summary file |
