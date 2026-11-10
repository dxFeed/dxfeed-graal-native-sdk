# NetTest: a publisher over the C API

`NetTest p` publishes generated Trade or TimeAndSale events through `dxfg_v4_DXPublisher_publishEvents()` at a fixed
rate and prints statistics in the format of the QD tool `NetTest p` (qds-tools), so that publishing through the C API
can be measured with the same mux, consumer and parsers as the QD API. It is written in C++17 and calls only the
`dxfg_` functions of the SDK headers.

The tool is built with the C tests and samples and is not part of the SDK archive.

## Build

```shell
cd src/main/c
cmake --preset=conf-release -DCMAKE_BUILD_TYPE=Release .
cmake --build --preset=build-release --config=Release --target NetTest
```

The executable and the SDK library are in `src/main/c/bin/Release` (Windows) or the build output directory of your
platform.

## Command line

```text
NetTest p <address> -S <symbols> [-C <n>] [-r */<rate>] [-s <period>] [-n <name>] [--batch <n>]
        [--event Trade|TimeAndSale] [--reuse] [--legacy] [--duration <time>] [-D<key>=<value>...]
```

| Option                   | Meaning                                                                                       |
|--------------------------|-----------------------------------------------------------------------------------------------|
| `<address>`              | `:port` listens (the mux connects to it), `host:port` connects to a listening peer           |
| `-S <total>@<file>`      | the first `<total>` symbols of an IPF file, in file order; fewer give a warning, none an error |
| `-S <total>`             | `<total>` distinct symbols of 1-6 capital letters from a fixed seed                          |
| `-C <n>`                 | instances: endpoints `NetTest.1` ... `NetTest.<n>`, one generator thread each, the whole pool each; not above 1 with `:port` |
| `-r */<rate>`            | the total rate of all instances in records per second, with `k`, `M` or `G`; without it the generators run flat out |
| `-s <period>`            | the statistics period, default `10s` (`ms`, `s`, `m`)                                         |
| `-n <name>`              | the name in the statistics line, default `NetTest`                                            |
| `--batch <n>`            | events per publishing call, default 100                                                       |
| `--event`                | `Trade` (default) or `TimeAndSale`                                                            |
| `--reuse`                | republish the pre-built events in place; without it every batch is copied into new events, as an application that creates its events would do |
| `--legacy`               | publish through the deprecated `dxfg_DXPublisher_publishEvents()`                             |
| `--duration <time>`      | end the run after this time; Ctrl+C (SIGINT, SIGTERM) ends it at once                         |
| `-D<key>=<value>`        | set a system property before the first endpoint, for example `-Ddxscheme.nanoTime=true`       |

Every event carries the current time: Trade in `time_sequence`, TimeAndSale in `index` (seconds, milliseconds and a
22-bit sequence per instance), the rest of the nanoseconds in `time_nano_part`. A consumer computes the business lag
from it as for QD NetTest's own records.

Exit codes: 0 after `--duration` or Ctrl+C, 2 for invalid arguments, 1 for a failed start (unreadable IPF file, failed
endpoint, malformed address, a `:port` that does not start listening within 3 s, for example because it is in use) or
a failed publishing call. Connecting is asynchronous: a `host:port` without a listener is not an error, the tool runs
and reports `connections 0` until a peer is there.

## Statistics

At every multiple of the period on the wall clock (the first tick only starts the count) the tool prints to stdout:

```text
{*NetTestProducerSide-NetTest*} data 1,000,012 rps, lag 41 us, instances 1, connections 1
    NetTest.1 data 1,000,012 rps, lag 41 us
```

- `data`: records handed to the publishing function per second.
- `lag`: the time to build a batch and publish it, weighted by its records; the wait for the rate limiter is not
  included.
- `connections`: open connections of all instances' connectors.

When the run stops, a last block covers the time since the last tick, scaled by its real length; a last block shorter
than one second is not printed.

The SDK's log, including the QDS monitoring, goes to stderr.

## Running against QD NetTest

The consumer must subscribe to the same record and symbols. With the default `--event Trade`:

```shell
NetTest p :6666 -S 50000@symbols.ipf -r */1M -s 10s --duration 7m
java -Dcom.devexperts.qd.tools.NetTest.record=Trade -jar qds-tools.jar NetTest c localhost:6666 -S 50000@symbols.ipf
```

For `--event TimeAndSale` drop the property (TimeAndSale is QD NetTest's default record); the consumer may need
`-c stream`, since a ticker collector keeps only the last TimeAndSale per symbol. Against a mux, run the publisher with
`:6666` and point the mux's distributor connector at it.

## Uplink monitoring

`-Dmonitoring.stat=10s` turns on the QDS monitoring of every embedded endpoint. Each period it logs to stderr a line
per endpoint and per connector, for example:

```text
I 261110 181108.014 {NetTest.1} Subscription: 0; Sticky: 0; Storage: 0; Buffer: 0; Dropped: 0; Read: 0 Bps; Write: 0 Bps; CPU: 1.94%
    ServerSocket-Agent *:47123 [0] Read: 0 Bps; Write: 0 Bps
```

## Differences from QD NetTest

- Only the publisher mode `p`. The consumer mode `c`, `-c`, symbol splits `/r` and `/s`, symbol lengths `{len,max}`
  and the limiter bucket `;<time>` are rejected.
- The default event is Trade; QD NetTest's default record is TimeAndSale.
- `-r` is one budget for all instances, as in QD NetTest, but the limiter takes a whole batch before the batch is
  built and never goes into debt.
- `lag` excludes the limiter wait; QD NetTest's includes it.
- Generated symbols differ from QD NetTest's: runs that mix the two tools must use `-S <total>@<file>`.
- The statistics line has no QD log prefix (`I yymmdd hhmmss.mmm `); parsers match from `{*`.
- A last, partial block of one second or more is printed at stop.
- `-C` above 1 with a `:port` address is rejected: the instances would compete for one port.
