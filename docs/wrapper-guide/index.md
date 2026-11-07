# Wrapper guide {#wrapper_guide}

This guide is the contract between the SDK and a wrapper: a library that makes the C API usable from another language
(Python, Go, .NET, Rust, Swift, Java through FFM, ...). It states what a wrapper may rely on and what it must do, in
one place; the groups of the C API reference hold the details, and the pages link to them instead of repeating them.
The rules of the `dxfg_v4_` functions themselves are in @ref V4_Conventions.

- @subpage wrapper_threads &ndash; attaching the threads of the wrapper's runtime, long-lived threads, finalizers.
- @subpage wrapper_ownership &ndash; who owns what a function returns, and when it is released.
- @subpage wrapper_outcomes &ndash; the return codes, the exceptions and the outcomes of the functions that wait.
- @subpage wrapper_versioning &ndash; the tiers, the start-up check, the features and the API description.
- @subpage wrapper_delivery &ndash; which delivery of data to use for what.
- @subpage wrapper_kit &ndash; what the conformance kit proves about a wrapper, and what it cannot.

A wrapper that follows these pages and passes the kit in strict mode on every platform it supports is conformant for
what the kit covers; the limits are on @ref wrapper_kit.
