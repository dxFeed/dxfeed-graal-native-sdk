# Tiers and versioning {#wrapper_versioning}

A wrapper is built against the headers of one release and runs against the library that the application installs,
which may be newer. This page states what it may rely on and how it checks the library at start-up. The tiers are
defined in @ref V4_Conventions, the runtime queries in @ref V4_System.

## Tiers {#wrapper_versioning_tiers}

- A **stable** function keeps its name, parameters and behaviour. It changes only by deprecation, and is removed only
  in a major release. A wrapper binds the stable functions unconditionally.
- An **experimental** function is marked `DXFG_EXPERIMENTAL` and listed on the page @ref experimental. It may change or
  be removed in any minor release. An incompatible change gives it a new name and increments the advanced revision
  (#DXFG_V4_ADVANCED_REVISION), so a wrapper bound by name misses the old symbol instead of calling it with a changed
  signature.
- A **deprecated** function is marked `DXFG_DEPRECATED`, and its warning names its replacement. The deprecated
  functions are removed in 4.0.0: a wrapper binds the replacements.

## The start-up check {#wrapper_versioning_check}

Before it binds anything else, a wrapper checks the library with three functions (3.13.0 and newer; a library without
them is older than the wrapper supports):

1. dxfg_v4_system_get_version() gives the release version, to log and to compare with the minimum the wrapper needs.
2. dxfg_v4_system_get_advanced_revision() gives the revision of the experimental functions. The wrapper binds them only
   when it equals #DXFG_V4_ADVANCED_REVISION of the headers it was built with; otherwise it treats the experimental
   part as missing, and its own experimental API reports "not available" instead of calling the library.
3. dxfg_v4_system_is_feature_supported() tells whether a feature is there (#DXFG_V4_FEATURE_EVENT_QUEUE,
   #DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL, ...). A feature that the library does not know is not supported, not an
   error, so a wrapper may ask for a feature newer than the library.

A wrapper that links to the library instead of loading it at run time makes the same check at start-up, and stops
before it calls an experimental function of another revision: such a call may crash.

## The API description {#wrapper_versioning_description}

Every archive has `dxfg-api.json` next to the library: the C API in a form a wrapper generator reads instead of parsing
the headers. It lists every function, type and macro with its header, its tier, `since`, its parameters and their
`out` flags, the `release` function of what it returns, and `deprecated` with the replacements. Its `sdk_version`,
`advanced_revision` and `features` are what the three functions above report at run time. A generator that binds only
what the description lists, and only the stable tier unless the revisions match, follows this page by construction.

The headers and the description of a release keep every stable declaration of the previous one: the build checks the
headers against the description of the last release, so a wrapper built for an older release keeps working with a
newer library of the same major version.
