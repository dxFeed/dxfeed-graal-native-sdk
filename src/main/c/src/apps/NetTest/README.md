# NetTest: a publisher over the C API

`NetTest p` publishes generated Trade or TimeAndSale events through `dxfg_v4_DXPublisher_publishEvents()` at a fixed
rate and prints statistics in the format of the QD tool `NetTest p` (qds-tools), so that publishing through the C API
can be measured with the same mux, consumer and parsers as the QD API. It is written in C++17 and calls only the
`dxfg_` functions of the SDK headers.

The NetTests of all language APIs follow one contract, the [NetTest specification](../../../../../../docs/wrapper-guide/nettest.md)
(`nettest/1`). Its table of the SDK tools shows the sections this tool follows, and
[Differences from the specification](#differences-from-the-specification) lists what it does not do yet. This README
describes the options the tool has.

The tool is built with the C tests and samples and is not part of the SDK archive.

## Build

```shell
cd src/main/c
cmake --preset=conf-release -DCMAKE_BUILD_TYPE=Release .
cmake --build --preset=build-release --config=Release --target NetTest
```

The executable and the SDK library are in `src/main/c/bin/Release` (Windows) or the build output directory of your
platform.

### Build against an SDK archive

The tool also builds without the build of the library: against an unpacked SDK archive of a release (or the archive
that `mvn package` puts into `target/`), with this directory as the top-level CMake project. `DXFG_SDK_DIR` names the
directory with the archive's `CMakeLists.txt` and headers; CMake 3.21 or later and a C++17 compiler are needed.

```shell
cmake -S src/main/c/src/apps/NetTest -B build-nettest -DDXFG_SDK_DIR=<unpacked archive> -DCMAKE_BUILD_TYPE=Release
cmake --build build-nettest --config Release
```

The build copies the SDK library next to the executable (`build-nettest/Release` with a multi-config generator such as
Visual Studio, `build-nettest` otherwise), and the rpath on Linux and macOS is `$ORIGIN` or `@loader_path`, so the
directory can be copied to another machine as is. The compiler flags are those of the build in the tree
(`src/main/c/cmake/PlatformOptions.cmake` and `CompileOptions.cmake`).

The C test `NetTestStandalone` builds the tool this way against the archive in `target/` and runs it once; it fails
when `target/` has no archive of the platform, so run `mvn package` before the C tests.

## Command line

```text
NetTest p <address> -S <symbols> [-C <n>] [-r */<rate>] [-s <period>] [-n <name>] [--batch <n>]
        [--event Trade|TimeAndSale] [--reuse] [--legacy] [--duration <time>] [-D<key>=<value>...]
        [-Xmx<size>] [-Xms<size>] [-Xmn<size>] [-Xss<size>] [-XX:<option>...]
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
| `-D<key>=<value>`        | set a system property before the first endpoint, for example `-Dmonitoring.stat=10s`; `-Ddxscheme.nanoTime` also goes to the builder of every endpoint (see below) |
| `-Xmx<size>`, `-Xms<size>`, `-Xmn<size>`, `-Xss<size>`, `-XX:<option>` | an option of the Graal isolate, as `java` takes it, for example `-Xmx512m` or `-XX:+PrintGC` (see [GC of the isolate](#gc-of-the-isolate)) |

Every event carries the current time: Trade in `time_sequence`, TimeAndSale in `index` (seconds, milliseconds and a
22-bit sequence per instance), the rest of the nanoseconds in `time_nano_part`. A consumer computes the business lag
from it as for QD NetTest's own records.

Every endpoint is built with `dxscheme.nanoTime=true` on its builder, so the nanosecond part goes on the wire without
an option; `-Ddxscheme.nanoTime=false` turns it off for a run against peers without it. The nanosecond part reaches a
consumer only when every hop has the property: a multiplexor needs `-Ddxscheme.nanoTime=true` on its command line, and
a STREAM_FEED consumer needs it on its own builder (the system property reaches only PUBLISHER and FEED endpoints). A
hop without it still passes the events, with the nanosecond part 0.

Exit codes: 0 after `--duration` or Ctrl+C, 2 for invalid arguments (also an option that the isolate rejects), 1 for a
failed start (unreadable IPF file, failed endpoint, malformed address, a `:port` that does not start listening within
3 s, for example because it is in use) or a failed publishing call. Connecting is asynchronous: a `host:port` without a
listener is not an error, the tool runs and reports `connections 0` until a peer is there.

## Statistics

At every multiple of the period on the wall clock (the first tick only starts the count) the tool prints to stdout:

```text
{*NetTestProducerSide-NetTest*} data 1,000,012 rps, lag 41 us, instances 1, connections 1
    NetTest.1 data 1,000,012 rps, lag 41 us
{*NetTestRuntime-NetTest*} heap 120 MB of 256 MB, gc 3 (15 ms), cpu 85%
```

- `data`: records handed to the publishing function per second.
- `lag`: the time to build a batch and publish it, weighted by its records; the wait for the rate limiter is not
  included.
- `connections`: open connections of all instances' connectors.

The last line of the block shows the Graal isolate, as `dxfg_v4_system_get_runtime_statistics()` reads it:

- `heap`: the used and the committed heap now, in MB (MiB, rounded down).
- `gc`: the young and full collections of the period and their total time.
- `cpu`: the CPU time of the whole process in the period as a share of the period's length, so it goes above 100% when
  several cores are busy.

A value that the isolate does not give is `n/a`: with the G1 GC (the oracle-g1 variant of the SDK) the line shows
`gc n/a`. The summary line keeps the QD NetTest format; parsers that read it are not affected by the new line, which
has its own prefix.

When the run stops, a last block covers the time since the last tick, scaled by its real length; a last block shorter
than one second is not printed.

The SDK's log, including the QDS monitoring, goes to stderr.

## Running against QD NetTest

The consumer must subscribe to the same record and symbols. With the default `--event Trade`:

```shell
NetTest p :6666 -S 50000@symbols.ipf -r */1M -s 10s --duration 7m
java -Dcom.devexperts.qd.tools.NetTest.record=Trade -jar qds-tools.jar NetTest c localhost:6666 -S 50000@symbols.ipf
```

For `--event TimeAndSale` drop the property (TimeAndSale is QD NetTest's default record) and give the consumer
`-c stream`: the publisher feeds TimeAndSale to the stream and the history contracts only, so the default ticker
consumer gets nothing and reports `data 0 rps`. Against a mux, run the publisher with `:6666` and point the mux's
distributor connector at it.

The same benchmark in Java, through the dxFeed API without the SDK, is [NetTestApi](../../../../../../tools/nettest/README.md).

## Uplink monitoring

`-Dmonitoring.stat=10s` turns on the QDS monitoring of every embedded endpoint. Each period it logs to stderr a line
per endpoint and per connector, for example:

```text
I 261110 181108.014 {NetTest.1} Subscription: 0; Sticky: 0; Storage: 0; Buffer: 0; Dropped: 0; Read: 0 Bps; Write: 0 Bps; CPU: 1.94%
    ServerSocket-Agent *:47123 [0] Read: 0 Bps; Write: 0 Bps
```

## GC of the isolate

The SDK library runs in a Graal isolate with the Serial GC, and its heap may grow to 80% of the RAM by default. The
`-Xmx`, `-Xms`, `-Xmn`, `-Xss` and `-XX:` arguments go to the isolate when it is created, in the command line order.
The isolate would ignore other `-X` arguments, so the tool rejects them (a typo such as `-Xnm2m`) with exit code 2 and
its usage text. The useful options:

| Option                   | Effect                                                                                        |
|--------------------------|-----------------------------------------------------------------------------------------------|
| `-Xmx<size>`, `-Xms<size>`, `-Xmn<size>` | the maximum, the initial and the young generation sizes of the heap (`512m`, `1g`) |
| `-Xss<size>`             | the stack size of the threads that the isolate starts (`1m`)                                  |
| `-XX:+PrintGC`           | a line per collection: its number, kind, heap before and after, pause                        |
| `-XX:+PrintGCSummary`    | the counts, times and allocated bytes of all collections, at the end of the run              |
| `-XX:+VerboseGC`, `-XX:+PrintGCTimes` | the generations and the phase times of every collection (verbose)               |
| `-XX:PrintFlags=`        | lists the options of the isolate and exits with 0 without a run                               |

The GC output goes to stderr, together with the SDK's log:

```text
[0.167s] GC(0) Pause Incremental GC (Collect on allocation) 4.00M->2.00M 22.029ms
...
GC summary
  Incremental GC count: 16
  Incremental GC time: 0.331s
  Complete GC count: 0
  ...
  GC load: 18%
```

At the end of the run the tool closes all endpoints and tears the isolate down, which prints the summary.

The `gc` value of the runtime line in [Statistics](#statistics) needs no option: it counts the same collections as
`-XX:+PrintGC` (`Incremental GC` as young, `Complete GC` as full) per statistics period, with their collection time
in milliseconds as the GC beans of the isolate report it.

### A rejected option

An unknown `-XX:` option or a wrong value (`-Xmx64q`) fails the creation of the isolate. GraalVM prints its reason to
stderr, and the tool then prints one line that names the options it passed to the isolate, exactly as given, in the
command line order, separated by one space, and exits with 2 without a usage text; stdout stays empty:

```text
Error: Could not find option 'NetTestNoSuchOption'. Use -XX:PrintFlags= to list all available options.
NetTest: the isolate rejected one of the options: -Xmx64m -XX:+NetTestNoSuchOption
```

The tool's line is the same on every platform and in the NetTest of every language API; a parser matches it with the
expression `isolate_rejection` of the specification:

```text
^NetTest: the isolate rejected one of the options: ([^ \t\r\n]+( [^ \t\r\n]+)*)$
```

## Differences from the specification

What the tool does not do yet of the [NetTest specification](../../../../../../docs/wrapper-guide/nettest.md):

- Only the mode `p`: no consumer, so no e2e lag, `--warmup`, `--summary` or `--metrics-port`.
- No `-c` and no `--role`: the endpoints are always PUBLISHER, which writes Trade to the ticker and the stream
  contracts and TimeAndSale to the stream contract.
- No long names of the short options (`--rate-limit`, `--stat`, `--name`), no `--option=value` and no `--help`, so
  no `spec_version` line.
- `-w` prints `unknown flag -w` instead of the text of the specification.
- The rate limiter is a token bucket (capacity the larger of the rate and the batch, a spin below 500 us) instead of a
  schedule from the first grant with catch-up and no busy wait, so the tool reports no schedule slip.

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
- Each block ends with the runtime line of the isolate (`{*NetTestRuntime-<name>*}`), which QD NetTest does not print.
- `-C` above 1 with a `:port` address is rejected: the instances would compete for one port.
