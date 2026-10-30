// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Writes the JSON description of the C API ({@code dxfg-api.json}) from the headers and checks it against the
 * committed description of the previous release (the baseline): a wrapper generates its bindings from the JSON, and the
 * build keeps the promise of the stable tier.
 *
 * <p>The description has the version of the SDK, the advanced revision ({@code DXFG_V4_ADVANCED_REVISION}), the
 * features ({@code DXFG_V4_FEATURE_*}), the headers with their tier ("basic" or "advanced"), and the functions, types
 * and macros, each with its tier ("stable" or "experimental") and the version that added it ({@code since}): the one of
 * the baseline for a declaration the baseline has, the {@code @since} tag for a new one. A function has its return
 * type, its parameters with their types and {@code DXFG_OUT}, the message and the replacements of
 * {@code DXFG_DEPRECATED}; an out parameter that returns a pointer ({@code **}) and a returned pointer name the
 * function that releases it, the one its description names or by the {@code <type>_release} convention, or "unknown".
 * The thread rules have no machine source: "unknown". The output is sorted and has LF line endings, so it is the same
 * on every platform.
 *
 * <p>The rules of the baseline: a stable declaration does not change and is not removed (a deprecated function may be
 * removed from 4.0.0 on); an experimental one changes or is removed only with a higher advanced revision; names of
 * parameters do not count, and an enum may get new enumerators while every enumerator keeps its value; a declaration
 * that the baseline does not have needs {@code @since} with a version later than the baseline. A baseline older than
 * the last release in ReleaseNotes.md is a warning: it is refreshed after a release with {@code --update-baseline}.
 *
 * <p>Usage:
 * <ul>
 *   <li>{@code java GenerateApiJson.java <directory of the API headers> <SDK version> <baseline> <ReleaseNotes.md>
 *   <output>}</li>
 *   <li>{@code java GenerateApiJson.java --self-test <directory of fixtures>}: each subdirectory has {@code api/} with
 *   the headers, optional {@code advanced.txt} (the advanced headers, one per line), {@code version.txt} (the SDK
 *   version), optional {@code baseline/} (the headers of the previous release and its {@code version.txt}: the
 *   baseline is their description), {@code expected.txt} (a part of each error, one per line; empty when the headers
 *   pass) and optional {@code expected.json} (the expected output, byte for byte, also from the headers with CRLF line
 *   endings)</li>
 *   <li>{@code java GenerateApiJson.java --update-baseline <release tag> <baseline>}, in the repository: replaces the
 *   baseline with the description of the headers of the tag (e.g. {@code v3.13.0})</li>
 * </ul>
 */
public final class GenerateApiJson {

    private static final int SCHEMA_VERSION = 1;
    private static final String API_DIRECTORY = "src/main/c/api";
    private static final String REVISION_MACRO = "DXFG_V4_ADVANCED_REVISION";
    private static final String FEATURE_PREFIX = "DXFG_V4_FEATURE_";
    private static final String UNKNOWN = "unknown";
    private static final List<String> SECTIONS = List.of("functions", "types", "macros");

    private static final Pattern RELEASE_HEADING = Pattern.compile("^## v(\\d+\\.\\d+\\.\\d+)$", Pattern.MULTILINE);
    private static final Pattern RELEASE_FUNCTION = Pattern.compile("\\bdxfg_\\w+_release\\b");
    private static final Pattern PARAM_DOC = Pattern.compile("@param(?:\\[[^\\]]*])?\\s+(\\w+)(.*?)(?=@\\w|\\z)",
            Pattern.DOTALL);
    private static final Pattern RETURN_DOC = Pattern.compile("@returns?\\b(.*?)(?=@\\w|\\z)", Pattern.DOTALL);
    private static final Pattern MAJOR = Pattern.compile("^(\\d+)\\.");
    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
    // A number, maybe cast: "1", "(1)" or "((int32_t)1)".
    private static final Pattern INTEGER = Pattern.compile("\\(*(?:\\w+\\)\\s*)?(-?\\d+)\\)*");

    private GenerateApiJson() {
    }

    public static void main(final String[] args) throws IOException, InterruptedException {
        if (args.length == 2 && args[0].equals("--self-test")) {
            System.exit(selfTest(Path.of(args[1])) ? 0 : 1);
        }

        if (args.length == 3 && args[0].equals("--update-baseline")) {
            updateBaseline(args[1], Path.of(args[2]));
            return;
        }

        if (args.length != 5) {
            System.err.println("Usage: java GenerateApiJson.java <directory of the API headers> <SDK version>"
                    + " <baseline> <ReleaseNotes.md> <output>");
            System.err.println("       java GenerateApiJson.java --self-test <directory of fixtures>");
            System.err.println("       java GenerateApiJson.java --update-baseline <release tag> <baseline>");
            System.exit(2);
        }

        final Path baselinePath = Path.of(args[2]);
        final Map<String, Object> baseline = object(Json.parse(Files.readString(baselinePath, StandardCharsets.UTF_8)));
        final List<String> errors = new ArrayList<>();
        final Map<String, Object> description = describe(Path.of(args[0]), ApiModel.ADVANCED_HEADERS, args[1],
                baseline, errors);

        errors.addAll(compare(baseline, description));

        if (!errors.isEmpty()) {
            errors.forEach(error -> System.err.println("ERROR: " + error));
            System.err.println(errors.size() + " problems in " + args[0] + " (checked against " + baselinePath + ")");
            System.exit(1);
        }

        final String lastRelease = lastRelease(Path.of(args[3]), args[1]);

        if (lastRelease != null && !lastRelease.equals(baseline.get("sdk_version"))) {
            System.out.println("WARNING: the baseline " + baselinePath + " is the description of "
                    + baseline.get("sdk_version") + ", but the last release in " + args[3] + " is " + lastRelease
                    + "; refresh it: java scripts/check-api-headers/GenerateApiJson.java --update-baseline v"
                    + lastRelease + " " + baselinePath);
        }

        final Path output = Path.of(args[4]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, Json.write(description), StandardCharsets.UTF_8);
        System.out.println("The description of the C API is in " + output + ": " + summary(description)
                + "; the baseline " + baseline.get("sdk_version") + " holds");
    }

    // Runs every fixture and compares its errors (and its output) with the expected ones.
    private static boolean selfTest(final Path fixtures) throws IOException {
        final List<Path> directories;

        try (Stream<Path> paths = Files.list(fixtures)) {
            directories = paths.filter(Files::isDirectory).sorted().toList();
        }

        if (directories.isEmpty()) {
            System.err.println("ERROR: no fixtures in " + fixtures);
            return false;
        }

        boolean passed = true;

        for (final Path fixture : directories) {
            final String name = fixture.getFileName().toString();
            final Set<String> advanced = new TreeSet<>(lines(fixture.resolve("advanced.txt")));
            final String version = Files.readString(fixture.resolve("version.txt"), StandardCharsets.UTF_8).strip();
            final Path baselineHeaders = fixture.resolve("baseline");
            final List<String> errors = new ArrayList<>();
            Map<String, Object> baseline = null;
            String output = null;

            try {
                if (Files.isDirectory(baselineHeaders)) {
                    final String baselineVersion = Files.readString(baselineHeaders.resolve("version.txt"),
                            StandardCharsets.UTF_8).strip();
                    // Through the text, as the build reads the committed baseline.
                    baseline = object(Json.parse(Json.write(describe(baselineHeaders, advanced, baselineVersion, null,
                            errors))));
                }

                final Map<String, Object> description = describe(fixture.resolve("api"), advanced, version, baseline,
                        errors);

                if (baseline != null) {
                    errors.addAll(compare(baseline, description));
                }

                output = Json.write(description);
            } catch (final IllegalStateException e) {
                errors.add(e.getMessage());
            }

            final List<String> expected = lines(fixture.resolve("expected.txt"));
            final List<String> unexpected = new ArrayList<>(errors);
            final List<String> problems = new ArrayList<>();

            for (final String part : expected) {
                if (!unexpected.removeIf(error -> error.contains(part))) {
                    problems.add("does not report an error with \"" + part + "\"");
                }
            }

            unexpected.forEach(error -> problems.add("reports an unexpected error: " + error));

            final Path expectedJson = fixture.resolve("expected.json");

            if (output != null && Files.exists(expectedJson)) {
                if (!output.equals(Files.readString(expectedJson, StandardCharsets.UTF_8))) {
                    problems.add("writes another description than expected.json");
                }

                if (!output.equals(describeWithCrlf(fixture, advanced, version, baseline))) {
                    problems.add("writes another description from the headers with CRLF line endings");
                }
            }

            if (problems.isEmpty()) {
                System.out.println("Fixture " + name + ": " + (expected.isEmpty() ? "passes" : "reports " + expected));
            } else {
                passed = false;
                problems.forEach(problem -> System.err.println("ERROR: fixture " + name + " " + problem));
            }
        }

        if (passed) {
            System.out.println("The self-test of the C API description passed: " + directories.size() + " fixtures");
        }

        return passed;
    }

    // The description of a copy of the fixture's headers with CRLF line endings.
    private static String describeWithCrlf(
            final Path fixture,
            final Set<String> advanced,
            final String version,
            final Map<String, Object> baseline
    ) throws IOException {
        final Path copy = Files.createTempDirectory("dxfg-api-crlf");

        try (Stream<Path> headers = Files.list(fixture.resolve("api"))) {
            for (final Path header : headers.toList()) {
                final String text = Files.readString(header, StandardCharsets.UTF_8).replace("\r\n", "\n");
                Files.writeString(copy.resolve(header.getFileName()), text.replace("\n", "\r\n"),
                        StandardCharsets.UTF_8);
            }

            return Json.write(describe(copy, advanced, version, baseline, new ArrayList<>()));
        } finally {
            deleteDirectory(copy);
        }
    }

    // Replaces the baseline with the description of the headers of the release tag, read with git.
    private static void updateBaseline(final String tag, final Path baselinePath)
            throws IOException, InterruptedException {
        final String version = tag.startsWith("v") ? tag.substring(1) : tag;
        final Map<String, Object> previous = Files.exists(baselinePath)
                ? object(Json.parse(Files.readString(baselinePath, StandardCharsets.UTF_8))) : null;
        final Path directory = Files.createTempDirectory("dxfg-api-" + version);

        try {
            for (final String path : git("ls-tree", "--name-only", tag, API_DIRECTORY + "/").lines().toList()) {
                final Path file = Path.of(path).getFileName();

                if (ApiModel.isApiHeader(file)) {
                    Files.writeString(directory.resolve(file), git("show", tag + ":" + path), StandardCharsets.UTF_8);
                }
            }

            final List<String> errors = new ArrayList<>();
            final Map<String, Object> description = describe(directory, ApiModel.ADVANCED_HEADERS, version, previous,
                    errors);

            if (!errors.isEmpty()) {
                errors.forEach(error -> System.err.println("ERROR: " + error));
                System.exit(1);
            }

            Files.createDirectories(baselinePath.toAbsolutePath().getParent());
            Files.writeString(baselinePath, Json.write(description), StandardCharsets.UTF_8);
            System.out.println("The baseline " + baselinePath + " is the description of " + tag + ": "
                    + summary(description));
        } finally {
            deleteDirectory(directory);
        }
    }

    private static String git(final String... arguments) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(arguments));

        final Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (process.waitFor() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed");
        }

        return output;
    }

    private static void deleteDirectory(final Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    /**
     * The description of the headers of the directory. The {@code since} of a declaration is the one of the baseline,
     * or its {@code @since} tag; without both it is an error, or, when there is no baseline, the version for a v4 name
     * and "before" the version for an older one (the first baseline, made from the headers of 3.12.0).
     */
    static Map<String, Object> describe(
            final Path directory,
            final Set<String> advancedHeaders,
            final String version,
            final Map<String, Object> baseline,
            final List<String> errors
    ) throws IOException {
        final List<ApiModel.Header> headers = new ArrayList<>();

        try (Stream<Path> paths = Files.list(directory)) {
            // By the names as strings: a Windows path compares ignoring case.
            for (final Path header : paths.filter(ApiModel::isApiHeader)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList()) {
                headers.add(ApiModel.parse(header));
            }
        }

        final Map<String, Map<String, Map<String, Object>>> sinceOf = new TreeMap<>();

        for (final String section : SECTIONS) {
            sinceOf.put(section, records(baseline, section));
        }

        final Map<String, ApiModel.Function> functions = new TreeMap<>();
        final Map<String, ApiModel.Type> types = new TreeMap<>();
        final Map<String, ApiModel.Macro> macros = new TreeMap<>();

        for (final ApiModel.Header header : headers) {
            header.functions().forEach(function -> unique(functions, function.name(), function, header.name()));
            header.types().forEach(type -> type(types, type));
            header.macros().forEach(macro -> unique(macros, macro.name(), macro, header.name()));
        }

        final Since since = (section, name, doc) -> {
            final Map<String, Object> record = sinceOf.get(section).get(name);

            if (record != null) {
                return (String) record.get("since");
            }

            final String tag = ApiModel.since(doc);

            if (tag != null) {
                // A new declaration: the baseline would keep a wrong version for good.
                if (!VERSION.matcher(tag).matches()) {
                    errors.add(name + " has @since " + tag + ", which is not a version x.y.z");
                } else if (baseline != null && compareVersions(tag, (String) baseline.get("sdk_version")) <= 0) {
                    errors.add(name + " has @since " + tag + ", but is not in the baseline "
                            + baseline.get("sdk_version") + ": a new declaration has a later version");
                }

                return tag;
            }

            if (baseline == null) {
                return name.regionMatches(true, 0, ApiModel.V4_PREFIX, 0, ApiModel.V4_PREFIX.length())
                        ? version : "before " + version;
            }

            errors.add(name + " is not in the baseline " + baseline.get("sdk_version") + " and has no @since");
            return null;
        };

        final Map<String, Object> description = new LinkedHashMap<>();
        description.put("schema_version", SCHEMA_VERSION);
        description.put("sdk_version", version);
        description.put("advanced_revision", revision(macros.get(REVISION_MACRO)));
        description.put("features", macros.values().stream().filter(macro -> macro.name().startsWith(FEATURE_PREFIX))
                .map(macro -> macro.value().replaceAll("^\"|\"$", "")).sorted().toList());
        description.put("headers", headers.stream().map(header -> (Object) ordered(
                "name", header.name(),
                "tier", advancedHeaders.contains(header.name()) ? "advanced" : "basic")).toList());
        description.put("functions", functions.values().stream()
                .map(function -> function(function, advancedHeaders, functions.keySet(), since)).toList());
        description.put("types", types.values().stream().map(type -> type(type, advancedHeaders, since)).toList());
        description.put("macros", macros.values().stream().map(macro -> (Object) ordered(
                "name", macro.name(),
                "header", macro.header(),
                "tier", tier(macro.header(), macro.doc(), advancedHeaders),
                "since", since.of("macros", macro.name(), macro.doc()),
                "value", macro.value())).toList());

        return description;
    }

    @FunctionalInterface
    private interface Since {
        String of(String section, String name, String doc);
    }

    private static <T> void unique(final Map<String, T> map, final String name, final T value, final String header) {
        if (map.putIfAbsent(name, value) != null) {
            throw new IllegalStateException(name + " is declared twice (the second time in " + header + ")");
        }
    }

    // A type may be declared again: "typedef struct X X;" before its definition, or the same typedef in two headers.
    private static void type(final Map<String, ApiModel.Type> types, final ApiModel.Type type) {
        final ApiModel.Type previous = types.get(type.name());

        if (previous == null || isForward(previous) && !isForward(type)) {
            types.put(type.name(), type);
        } else if (!isForward(type) && !sameType(previous, type)) {
            throw new IllegalStateException(type.name() + " is declared twice in another way (the second time in "
                    + type.header() + ")");
        }
    }

    // "typedef struct X X;": the name of an opaque struct, or of one that is defined later.
    private static boolean isForward(final ApiModel.Type type) {
        return type.kind().equals("alias") && type.type().matches("(?:struct|union|enum) " + type.name());
    }

    private static boolean sameType(final ApiModel.Type first, final ApiModel.Type second) {
        return first.kind().equals(second.kind()) && first.type().equals(second.type())
                && first.fields().equals(second.fields()) && first.enumerators().equals(second.enumerators());
    }

    private static Object function(
            final ApiModel.Function function,
            final Set<String> advancedHeaders,
            final Set<String> declared,
            final Since since
    ) {
        final boolean experimental = function.marked() || advancedHeaders.contains(function.header());
        final Object deprecated = function.deprecation() == null ? null : ordered(
                "message", function.deprecation(),
                "replacements", ApiModel.V4_FUNCTION.matcher(function.deprecation()).results().map(MatchResult::group)
                        .distinct().toList());
        final Map<String, Object> returns = new LinkedHashMap<>();
        returns.put("type", function.returnType());

        if (function.returnType().endsWith("*")) {
            final Matcher doc = RETURN_DOC.matcher(function.doc());
            returns.put("release", release(function.returnType(), doc.find() ? doc.group(1) : "", declared));
        }

        final Map<String, String> parameterDocs = new TreeMap<>();
        PARAM_DOC.matcher(function.doc()).results().forEach(doc -> parameterDocs.put(doc.group(1), doc.group(2)));
        final List<Object> parameters = new ArrayList<>();

        for (final ApiModel.Variable parameter : function.parameters()) {
            final Map<String, Object> record = ordered(
                    "name", parameter.name(),
                    "type", parameter.type(),
                    "out", parameter.out());

            if (parameter.out() && parameter.type().endsWith("**")) {
                record.put("release", release(parameter.type().substring(0, parameter.type().length() - 1).strip(),
                        parameterDocs.getOrDefault(parameter.name(), ""), declared));
            }

            parameters.add(record);
        }

        return ordered(
                "name", function.name(),
                "header", function.header(),
                "tier", experimental ? "experimental" : "stable",
                "since", since.of("functions", function.name(), function.doc()),
                "deprecated", deprecated,
                "returns", returns,
                "parameters", parameters,
                "threads", UNKNOWN);
    }

    // The function that releases a pointer of the type: the first declared *_release function that its documentation
    // names, or <type>_release when it is declared.
    private static String release(final String pointerType, final String doc, final Set<String> declared) {
        final String named = RELEASE_FUNCTION.matcher(doc).results().map(MatchResult::group).filter(declared::contains)
                .findFirst().orElse(null);

        if (named != null) {
            return named;
        }

        final String base = pointerType.replaceAll("\\bconst\\b", " ").replace("*", " ").strip();
        final String release = base + "_release";

        return base.contains(" ") || !declared.contains(release) ? UNKNOWN : release;
    }

    private static Object type(final ApiModel.Type type, final Set<String> advancedHeaders, final Since since) {
        final Map<String, Object> record = ordered(
                "name", type.name(),
                "header", type.header(),
                "tier", tier(type.header(), type.doc(), advancedHeaders),
                "since", since.of("types", type.name(), type.doc()),
                "kind", type.kind());

        if (!type.type().isEmpty()) {
            record.put("type", type.type());
        }

        if (type.kind().equals("enum")) {
            record.put("enumerators", type.enumerators().stream().map(enumerator -> (Object) ordered(
                    "name", enumerator.name(),
                    "value", enumerator.value().isEmpty() ? null : enumerator.value())).toList());
        } else if (!type.kind().equals("alias")) {
            record.put(type.kind().equals("function_pointer") ? "parameters" : "fields", type.fields().stream()
                    .map(field -> (Object) ordered("name", field.name(), "type", field.type())).toList());
        }

        return record;
    }

    private static String tier(final String header, final String doc, final Set<String> advancedHeaders) {
        return advancedHeaders.contains(header) || ApiModel.experimentalTag(doc) ? "experimental" : "stable";
    }

    private static long revision(final ApiModel.Macro macro) {
        if (macro == null) {
            return 0;
        }

        final Matcher matcher = INTEGER.matcher(macro.value());

        if (!matcher.matches()) {
            throw new IllegalStateException(REVISION_MACRO + " is not a number: " + macro.value());
        }

        return Long.parseLong(matcher.group(1));
    }

    /** The errors of the description against the baseline. */
    static List<String> compare(final Map<String, Object> baseline, final Map<String, Object> description) {
        final List<String> errors = new ArrayList<>();
        final long baseRevision = number(baseline.get("advanced_revision"));
        final long revision = number(description.get("advanced_revision"));
        final boolean bumped = revision > baseRevision;
        final Matcher major = MAJOR.matcher((String) description.get("sdk_version"));
        final boolean removalAllowed = major.find() && Integer.parseInt(major.group(1)) >= 4;

        if (revision < baseRevision) {
            errors.add("the advanced revision " + revision + " is lower than the one of the baseline, " + baseRevision);
        }

        for (final String section : SECTIONS) {
            final Map<String, Map<String, Object>> current = records(description, section);

            for (final Map<String, Object> base : records(baseline, section).values()) {
                final String name = (String) base.get("name");
                final Map<String, Object> record = current.get(name);

                if (name.equals(REVISION_MACRO)) {
                    // Checked above: it may only grow.
                    continue;
                }

                final boolean stable = "stable".equals(base.get("tier"));

                if (record == null) {
                    if (stable && !(section.equals("functions") && base.get("deprecated") != null && removalAllowed)) {
                        errors.add("the stable " + name + " is removed");
                    } else if (!stable && !bumped) {
                        errors.add("the experimental " + name + " is removed without an advanced revision bump");
                    }

                    continue;
                }

                final String change = change(section, base, record);

                if (stable && "experimental".equals(record.get("tier"))) {
                    errors.add("the stable " + name + " is marked experimental");
                } else if (change != null && stable) {
                    errors.add("the stable " + name + " is changed: " + change);
                } else if (change != null && !bumped) {
                    errors.add("the experimental " + name + " is changed without an advanced revision bump: " + change);
                }
            }
        }

        return errors;
    }

    // How the declaration differs from the one of the baseline, or null: names of parameters do not count, an enum may
    // get new enumerators as long as every enumerator keeps its value.
    private static String change(
            final String section,
            final Map<String, Object> base,
            final Map<String, Object> record
    ) {
        final String before;
        final String after;

        switch (section) {
            case "functions" -> {
                before = signature(base);
                after = signature(record);
            }
            case "macros" -> {
                before = (String) base.get("value");
                after = (String) record.get("value");
            }
            default -> {
                if ("enum".equals(base.get("kind")) && "enum".equals(record.get("kind"))) {
                    final Map<String, String> values = enumeratorValues(record);

                    for (final Map.Entry<String, String> enumerator : enumeratorValues(base).entrySet()) {
                        final String value = values.get(enumerator.getKey());

                        if (!enumerator.getValue().equals(value)) {
                            return "the enumerator " + enumerator.getKey() + " = " + enumerator.getValue()
                                    + (value == null ? " is removed" : " becomes " + value);
                        }
                    }

                    return null;
                }

                final Map<String, Object> beforeType = new LinkedHashMap<>(base);
                final Map<String, Object> afterType = new LinkedHashMap<>(record);

                for (final Map<String, Object> type : List.of(beforeType, afterType)) {
                    for (final String key : List.of("name", "header", "tier", "since")) {
                        type.remove(key);
                    }

                    if ("function_pointer".equals(type.get("kind"))) {
                        type.put("parameters", list(type.get("parameters")).stream()
                                .map(parameter -> object(parameter).get("type")).toList());
                    }
                }

                before = Json.write(beforeType).replaceAll("\\s+", " ").strip();
                after = Json.write(afterType).replaceAll("\\s+", " ").strip();
            }
        }

        return Objects.equals(before, after) ? null : before + " -> " + after;
    }

    // The value of each enumerator of the enum: the number, written or the previous one plus 1 (0 for the first), or
    // the text of a value that is not a number, plus the count of the enumerators after it.
    private static Map<String, String> enumeratorValues(final Map<String, Object> type) {
        final Map<String, String> values = new LinkedHashMap<>();
        String text = null;
        long number = -1;

        for (final Object enumerator : list(type.get("enumerators"))) {
            final String value = (String) object(enumerator).get("value");
            final Matcher integer = INTEGER.matcher(value == null ? "" : value);

            if (value == null) {
                number++;
            } else if (integer.matches() && integer.group(1).length() <= 18) {
                text = null;
                number = Long.parseLong(integer.group(1));
            } else {
                text = value;
                number = 0;
            }

            values.put((String) object(enumerator).get("name"), text == null ? Long.toString(number)
                    : number == 0 ? text : text + " + " + number);
        }

        return values;
    }

    // Compares two versions x.y.z by their numbers; a suffix ("-SNAPSHOT") does not count.
    private static int compareVersions(final String first, final String second) {
        final Matcher a = VERSION.matcher(first);
        final Matcher b = VERSION.matcher(second);

        if (!a.lookingAt() || !b.lookingAt()) {
            return first.compareTo(second);
        }

        for (int group = 1; group <= 3; group++) {
            final int result = Long.compare(Long.parseLong(a.group(group)), Long.parseLong(b.group(group)));

            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    private static String signature(final Map<String, Object> function) {
        final List<String> parameters = new ArrayList<>();

        for (final Object parameter : list(function.get("parameters"))) {
            final Map<String, Object> record = object(parameter);
            final String marker = Boolean.TRUE.equals(record.get("out")) ? ApiModel.OUT_MARKER + " " : "";
            parameters.add(marker + record.get("type"));
        }

        return object(function.get("returns")).get("type") + " " + function.get("name") + "("
                + String.join(", ", parameters) + ")";
    }

    // The records of a section of a description by name; none for a missing description.
    private static Map<String, Map<String, Object>> records(
            final Map<String, Object> description,
            final String section
    ) {
        final Map<String, Map<String, Object>> records = new TreeMap<>();

        if (description != null) {
            for (final Object record : list(description.get(section))) {
                records.put((String) object(record).get("name"), object(record));
            }
        }

        return records;
    }

    // The last release (not a pre-release) in the release notes other than the version being built, or null.
    private static String lastRelease(final Path releaseNotes, final String version) throws IOException {
        if (!Files.exists(releaseNotes)) {
            return null;
        }

        final String text = Files.readString(releaseNotes, StandardCharsets.UTF_8).replace("\r\n", "\n");

        return RELEASE_HEADING.matcher(text).results()
                .map(result -> result.group(1)).filter(release -> !release.equals(version)).findFirst().orElse(null);
    }

    private static String summary(final Map<String, Object> description) {
        final long experimental = SECTIONS.stream().flatMap(section -> list(description.get(section)).stream())
                .filter(record -> "experimental".equals(object(record).get("tier"))).count();

        return list(description.get("functions")).size() + " functions, " + list(description.get("types")).size()
                + " types, " + list(description.get("macros")).size() + " macros (" + experimental + " experimental)";
    }

    private static List<String> lines(final Path file) throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }

        return Files.readAllLines(file, StandardCharsets.UTF_8).stream().map(String::strip)
                .filter(line -> !line.isEmpty()).toList();
    }

    private static Map<String, Object> ordered(final Object... keysAndValues) {
        final Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(final Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(final Object value) {
        return value == null ? List.of() : (List<Object>) value;
    }

    private static long number(final Object value) {
        return ((Number) value).longValue();
    }

    /** A JSON writer with two-space indentation and LF line endings, and a parser of what it writes. */
    static final class Json {

        private final String text;
        private int position;

        private Json(final String text) {
            this.text = text;
        }

        static String write(final Object value) {
            final StringBuilder builder = new StringBuilder();
            write(builder, value, "");

            return builder.append('\n').toString();
        }

        private static void write(final StringBuilder builder, final Object value, final String indent) {
            if (value instanceof Map<?, ?> map) {
                if (map.isEmpty()) {
                    builder.append("{}");
                    return;
                }

                builder.append("{\n");
                int i = 0;

                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    builder.append(indent).append("  ");
                    string(builder, (String) entry.getKey());
                    builder.append(": ");
                    write(builder, entry.getValue(), indent + "  ");
                    builder.append(++i < map.size() ? ",\n" : "\n");
                }

                builder.append(indent).append('}');
            } else if (value instanceof List<?> list) {
                if (list.isEmpty()) {
                    builder.append("[]");
                    return;
                }

                builder.append("[\n");

                for (int i = 0; i < list.size(); i++) {
                    builder.append(indent).append("  ");
                    write(builder, list.get(i), indent + "  ");
                    builder.append(i + 1 < list.size() ? ",\n" : "\n");
                }

                builder.append(indent).append(']');
            } else if (value instanceof String string) {
                string(builder, string);
            } else {
                builder.append(value);
            }
        }

        private static void string(final StringBuilder builder, final String value) {
            builder.append('"');

            for (final char c : value.toCharArray()) {
                switch (c) {
                    case '"' -> builder.append("\\\"");
                    case '\\' -> builder.append("\\\\");
                    case '\n' -> builder.append("\\n");
                    case '\t' -> builder.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            builder.append(String.format("\\u%04x", (int) c));
                        } else {
                            builder.append(c);
                        }
                    }
                }
            }

            builder.append('"');
        }

        static Object parse(final String text) {
            final Json parser = new Json(text);
            final Object value = parser.value();
            parser.skipWhitespace();

            if (parser.position != text.length()) {
                throw parser.error("the end of the text");
            }

            return value;
        }

        private Object value() {
            skipWhitespace();

            if (position >= text.length()) {
                throw error("a value");
            }

            final char c = text.charAt(position);

            if (c == '{') {
                final Map<String, Object> map = new LinkedHashMap<>();
                position++;
                skipWhitespace();

                if (peek('}')) {
                    return map;
                }

                do {
                    skipWhitespace();
                    final String key = string();
                    skipWhitespace();
                    expect(':');
                    map.put(key, value());
                    skipWhitespace();
                } while (peek(','));

                expect('}');
                return map;
            }

            if (c == '[') {
                final List<Object> list = new ArrayList<>();
                position++;
                skipWhitespace();

                if (peek(']')) {
                    return list;
                }

                do {
                    list.add(value());
                    skipWhitespace();
                } while (peek(','));

                expect(']');
                return list;
            }

            if (c == '"') {
                return string();
            }

            for (final Map.Entry<String, Object> literal : literals().entrySet()) {
                if (text.startsWith(literal.getKey(), position)) {
                    position += literal.getKey().length();
                    return literal.getValue();
                }
            }

            final int start = position;

            while (position < text.length() && "+-0123456789.eE".indexOf(text.charAt(position)) >= 0) {
                position++;
            }

            if (start == position) {
                throw error("a value");
            }

            final String number = text.substring(start, position);

            return number.matches("-?\\d+") ? (Object) Long.parseLong(number) : (Object) Double.parseDouble(number);
        }

        private static Map<String, Object> literals() {
            final Map<String, Object> literals = new LinkedHashMap<>();
            literals.put("true", true);
            literals.put("false", false);
            literals.put("null", null);

            return literals;
        }

        private String string() {
            expect('"');
            final StringBuilder builder = new StringBuilder();

            while (position < text.length() && text.charAt(position) != '"') {
                char c = text.charAt(position++);

                if (c == '\\') {
                    c = text.charAt(position++);

                    switch (c) {
                        case 'n' -> builder.append('\n');
                        case 't' -> builder.append('\t');
                        case 'r' -> builder.append('\r');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'u' -> {
                            builder.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
                            position += 4;
                        }
                        default -> builder.append(c);
                    }
                } else {
                    builder.append(c);
                }
            }

            expect('"');
            return builder.toString();
        }

        private void skipWhitespace() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }

        private boolean peek(final char c) {
            if (position < text.length() && text.charAt(position) == c) {
                position++;
                return true;
            }

            return false;
        }

        private void expect(final char c) {
            if (!peek(c)) {
                throw error("'" + c + "'");
            }
        }

        private IllegalStateException error(final String expected) {
            return new IllegalStateException("Invalid JSON at " + position + ": expected " + expected);
        }
    }
}
