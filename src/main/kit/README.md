# The conformance kit

The conformance kit checks a wrapper of the dxFeed Graal Native SDK (a library that makes its C API usable from another
language) by running it: a runner sends the requests of scenarios to an adapter program, which calls the wrapper, and
checks the responses. What the kit proves, and what it cannot, is on the page "The conformance kit" of the wrapper
guide: `docs/wrapper-guide/kit.md` in this archive, and the "Wrapper guide" of the C API reference.

| Path | Contents |
|------|----------|
| `runner/` | The runner, C++17 source with its own `CMakeLists.txt`; it needs neither the SDK nor the wrapper |
| `protocol.md` | The line protocol between the runner and an adapter, the values of the events, the scenario format and the verdicts |
| `scenarios/shared/` | The scenarios that run one after another in one adapter process |
| `scenarios/own-process/` | The scenarios that run in an adapter process each (configuration, shutdown, tear-down) |
| `adapter/` | The reference adapter: the protocol on the C API directly, an example for adapter authors |

The kit is versioned with the SDK: run the kit of the archive whose library the wrapper loads.

## Build the runner

CMake 3.21 or newer and a C++17 compiler:

```sh
cmake -S kit/runner -B build/kit-runner
cmake --build build/kit-runner --config Release
```

## Write an adapter

An adapter is a program in the wrapper's language that reads requests on its stdin, calls the wrapper, and writes
responses on its stdout, as `protocol.md` describes. It starts with a `hello` line that names its capabilities: a
scenario that needs a capability the adapter lacks (a worker thread, an OS object for the ready signal) is skipped,
not failed. The data scenarios publish events to an endpoint with the role `DXFG_ENDPOINT_ROLE_LOCAL_HUB`, so the
wrapper must bind the publisher. Only responses may reach stdout: send logs, the SDK's included, to stderr.

The reference adapter is one way to do it; build it against the library of this archive:

```sh
cmake -S kit/adapter -B build/kit-adapter
cmake --build build/kit-adapter --config Release
```

## Run

```sh
kit-runner --adapter ./my-adapter --strict kit/scenarios/shared
kit-runner --adapter ./my-adapter --strict kit/scenarios/own-process
```

- `--adapter-arg <arg>` passes an argument to the adapter (repeatable).
- `--strict` fails a skipped scenario, unless `--allow-skip <reason>` allows the reason (`capability-absent`,
  `advanced-disabled`, `feature-absent`), and fails a run in which no scenario passed.
- `--expected-advanced-revision <n>` is the advanced revision the wrapper is built for (`DXFG_V4_ADVANCED_REVISION`
  of its headers); the scenarios with experimental functions are skipped when the library has another one.
- `--step-timeout <ms>` is the deadline of one step, 10000 by default; the handshake (`hello`, the configuration and
  `start`, which creates the isolate) shares one step deadline. `--total-timeout <ms>` is the deadline of the whole run,
  100000 by default: the scenarios not run by then end as `ERROR`. Raise both for a slow adapter or a slow machine.
- `--only <scenario>` runs one scenario, by the name its file declares: the file name with the prefix `kit-`
  (`--only kit-roundtrip-quote`). `--verbose` prints every line exchanged with the adapter.

The runner prints a verdict per scenario (`PASS`, `FAIL`, `SKIP` with a reason, `ERROR`) and a summary, and exits with
0 when everything passed or was skipped as allowed, 1 on a failure (in strict mode also when no scenario passed), 2
when the adapter misbehaved or a deadline passed, 3 on a handshake mismatch, 64 on bad usage. Run it on every platform the wrapper supports.
