// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Checks that the C API headers declare the functions that the library exports: the same {@code dxfg_*} names with the
 * same numbers of parameters in {@code src/main/c/api/dxfg_*.h} and in the header that native-image generates. Without
 * it a declaration with no entry point (or an entry point with no declaration) fails only when a consumer links.
 *
 * <p>It also checks that the v4 API is complete: every older function is marked {@code DXFG_DEPRECATED} with a message
 * that names only declared {@code dxfg_v4_*} functions, {@code dxfg_v4_api.h} includes every {@code dxfg_v4_*.h}
 * except the advanced headers and {@code dxfg_api.h} includes {@code dxfg_v4_api.h}.
 *
 * <p>And it checks the boundary between the stable and the experimental functions (see {@code V4_Conventions} in
 * dxfg_common.h): the advanced headers ({@code ApiModel.ADVANCED_HEADERS}) are included by no basic header, every
 * function they declare is marked {@code DXFG_EXPERIMENTAL}, and they declare no name that a basic header declares; in
 * every header, a function is marked {@code DXFG_EXPERIMENTAL} exactly when its description has {@code @experimental}.
 * The text helpers and the list of the advanced headers are shared with {@code GenerateApiJson.java} in
 * {@code ApiModel.java}.
 *
 * <p>Usage: {@code java CheckApiHeaders.java <directory of the API headers> <generated header>}. The argument types
 * are not compared: native-image declares them by their Java types.
 *
 * <p>Self-test: {@code java CheckApiHeaders.java --self-test <directory of fixtures>}. Each subdirectory of it is a
 * fixture: {@code api/} with the headers, {@code generated.h}, {@code advanced.txt} with the names of its advanced
 * headers (one per line) and {@code expected.txt} with the errors the check must report (a part of each error, one per
 * line; an empty file when the check must pass). The self-test fails if a fixture reports another set of errors.
 */
public final class CheckApiHeaders {

    private static final String V4_API_HEADER = "dxfg_v4_api.h";
    private static final String API_HEADER = "dxfg_api.h";

    private static final Pattern V4_INCLUDE = Pattern.compile(
            "^\\s*#\\s*include\\s*\"(" + ApiModel.V4_PREFIX + "\\w+\\.h)\"", Pattern.MULTILINE);
    // The names a header declares besides its functions: macros, struct, union and enum tags, typedefs, enumerators.
    private static final Pattern MACRO = Pattern.compile("^\\s*#\\s*define\\s+(\\w+)", Pattern.MULTILINE);
    private static final Pattern TAG = Pattern.compile("\\b(?:struct|union|enum)\\s+(\\w+)\\s*\\{");
    private static final Pattern TYPEDEF_AFTER_BODY = Pattern.compile("}\\s*(\\w+)\\s*;");
    private static final Pattern TYPEDEF = Pattern.compile("\\btypedef\\s+[^;{}()]*?\\b(\\w+)\\s*;");
    private static final Pattern FUNCTION_POINTER_TYPEDEF = Pattern.compile(
            "\\btypedef\\b[^;{}]*?\\(\\s*\\*\\s*(\\w+)\\s*\\)");
    private static final Pattern ENUM_BODY = Pattern.compile("\\benum\\b[^{;]*\\{([^}]*)}");
    private static final Pattern ENUMERATOR = Pattern.compile("^\\s*(\\w+)");

    private CheckApiHeaders() {
    }

    public static void main(final String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("--self-test")) {
            System.exit(selfTest(Path.of(args[1])) ? 0 : 1);
        }

        if (args.length != 2) {
            System.err.println("Usage: java CheckApiHeaders.java <directory of the API headers> <generated header>");
            System.err.println("       java CheckApiHeaders.java --self-test <directory of fixtures>");
            System.exit(2);
        }

        final List<String> summary = new ArrayList<>();
        final List<String> errors = check(Path.of(args[0]), Path.of(args[1]), ApiModel.ADVANCED_HEADERS, summary);

        if (!errors.isEmpty()) {
            errors.forEach(error -> System.err.println("ERROR: " + error));
            System.err.println(errors.size() + " problems in " + args[0] + " (checked against " + args[1] + ")");
            System.exit(1);
        }

        summary.forEach(System.out::println);
    }

    // Runs the check over every fixture and compares its errors with the expected ones.
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
            final Set<String> advanced = new TreeSet<>(lines(fixture.resolve("advanced.txt")));
            final List<String> expected = lines(fixture.resolve("expected.txt"));
            final List<String> errors = check(fixture.resolve("api"), fixture.resolve("generated.h"), advanced,
                    new ArrayList<>());
            final List<String> unexpected = new ArrayList<>(errors);
            final List<String> missing = new ArrayList<>();

            for (final String part : expected) {
                if (!unexpected.removeIf(error -> error.contains(part))) {
                    missing.add(part);
                }
            }

            if (missing.isEmpty() && unexpected.isEmpty()) {
                System.out.println("Fixture " + fixture.getFileName() + ": "
                        + (expected.isEmpty() ? "passes" : "reports " + expected));
                continue;
            }

            passed = false;
            missing.forEach(part -> System.err.println(
                    "ERROR: fixture " + fixture.getFileName() + " does not report an error with \"" + part + "\""));
            unexpected.forEach(error -> System.err.println(
                    "ERROR: fixture " + fixture.getFileName() + " reports an unexpected error: " + error));
        }

        if (passed) {
            System.out.println("The self-test of the header check passed: " + directories.size() + " fixtures");
        }

        return passed;
    }

    // The errors of the headers in the directory, checked against the generated header; the summary of a passed check.
    private static List<String> check(
            final Path directory,
            final Path generatedHeader,
            final Set<String> advancedHeaders,
            final List<String> summary
    ) throws IOException {
        final Map<String, Integer> declared = new TreeMap<>();
        final Map<String, String> deprecations = new TreeMap<>();
        final Set<String> experimental = new TreeSet<>();
        final Set<String> v4Headers = new TreeSet<>();
        final Map<String, Set<String>> includes = new TreeMap<>();
        final Map<String, Set<String>> functionsByHeader = new TreeMap<>();
        final Map<String, Set<String>> namesByHeader = new TreeMap<>();
        final List<String> errors = new ArrayList<>();

        try (Stream<Path> headers = Files.list(directory)) {
            for (final Path header : headers.filter(ApiModel::isApiHeader).sorted().toList()) {
                final String name = header.getFileName().toString();
                final ApiModel.Header model = ApiModel.parse(header);
                final Map<String, Integer> functions = functions(model);

                declared.putAll(functions);

                for (final ApiModel.Function function : model.functions()) {
                    if (function.deprecation() != null) {
                        deprecations.put(function.name(), function.deprecation());
                    }

                    if (function.marked()) {
                        experimental.add(function.name());
                    }
                }

                includes.put(name, includes(header));
                functionsByHeader.put(name, functions.keySet());
                namesByHeader.put(name, names(header, functions.keySet()));
                errors.addAll(experimentalTags(model));

                if (name.startsWith(ApiModel.V4_PREFIX) && !name.equals(V4_API_HEADER)) {
                    v4Headers.add(name);
                }
            }
        }

        final Map<String, Integer> generated = functions(ApiModel.parse(generatedHeader));

        for (final Map.Entry<String, Integer> function : declared.entrySet()) {
            final Integer count = generated.get(function.getKey());

            if (count == null) {
                errors.add(function.getKey() + " is declared, but the library has no such function");
            } else if (!count.equals(function.getValue())) {
                errors.add(function.getKey() + " is declared with " + function.getValue()
                        + " parameters, the library function has " + count);
            }
        }

        for (final String function : generated.keySet()) {
            if (!declared.containsKey(function)) {
                errors.add(function + " is exported by the library, but no header declares it");
            }
        }

        int deprecated = 0;

        for (final String function : declared.keySet()) {
            if (function.startsWith(ApiModel.V4_PREFIX)) {
                continue;
            }

            final String message = deprecations.get(function);

            if (message == null) {
                errors.add(function + " is not marked DXFG_DEPRECATED");
                continue;
            }

            final List<String> replacements = ApiModel.V4_FUNCTION.matcher(message).results().map(MatchResult::group)
                    .toList();

            if (replacements.isEmpty()) {
                errors.add(function + " is deprecated without naming a " + ApiModel.V4_PREFIX + " function");
            }

            for (final String replacement : replacements) {
                if (!declared.containsKey(replacement)) {
                    errors.add(function + " is deprecated in favour of " + replacement + ", which no header declares");
                }
            }

            deprecated++;
        }

        for (final String header : advancedHeaders) {
            if (!includes.containsKey(header)) {
                errors.add("the advanced header " + header + " does not exist");
            }
        }

        final Set<String> included = includes.getOrDefault(V4_API_HEADER, Set.of());

        for (final String header : v4Headers) {
            if (!advancedHeaders.contains(header) && !included.contains(header)) {
                errors.add(header + " is not included by " + V4_API_HEADER + " and is not an advanced header");
            }
        }

        if (!includes.getOrDefault(API_HEADER, Set.of()).contains(V4_API_HEADER)) {
            errors.add(V4_API_HEADER + " is not included by " + API_HEADER);
        }

        final Map<String, String> basicNames = new TreeMap<>();

        for (final Map.Entry<String, Set<String>> header : namesByHeader.entrySet()) {
            if (!advancedHeaders.contains(header.getKey())) {
                header.getValue().forEach(name -> basicNames.putIfAbsent(name, header.getKey()));
            }
        }

        for (final String header : new TreeSet<>(advancedHeaders)) {
            for (final Map.Entry<String, Set<String>> includer : includes.entrySet()) {
                if (!advancedHeaders.contains(includer.getKey()) && includer.getValue().contains(header)) {
                    errors.add("the advanced header " + header + " must not be included by the basic header "
                            + includer.getKey());
                }
            }

            for (final String function : functionsByHeader.getOrDefault(header, Set.of())) {
                if (!experimental.contains(function)) {
                    errors.add(function + " in the advanced header " + header + " is not marked "
                            + ApiModel.EXPERIMENTAL_MARKER);
                }
            }

            for (final String name : namesByHeader.getOrDefault(header, Set.of())) {
                final String basic = basicNames.get(name);

                if (basic != null) {
                    errors.add("the advanced header " + header + " declares " + name + ", which the basic header "
                            + basic + " declares");
                }
            }
        }

        summary.add("The API headers declare the " + declared.size()
                + " functions of the library with the same numbers of parameters");
        summary.add("The " + deprecated + " older functions are deprecated in favour of declared "
                + ApiModel.V4_PREFIX + " functions, " + V4_API_HEADER + " includes the "
                + (v4Headers.size() - advancedHeaders.size()) + " basic v4 headers, " + API_HEADER + " includes "
                + V4_API_HEADER);
        summary.add("The " + experimental.size() + " functions marked " + ApiModel.EXPERIMENTAL_MARKER
                + " have @experimental in their descriptions; the " + advancedHeaders.size()
                + " advanced headers keep the boundary");

        return errors;
    }

    private static List<String> lines(final Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream().map(String::strip)
                .filter(line -> !line.isEmpty()).toList();
    }

    // The dxfg_* function declarations of the header: the name and the number of parameters.
    private static Map<String, Integer> functions(final ApiModel.Header header) {
        final Map<String, Integer> functions = new TreeMap<>();

        for (final ApiModel.Function function : header.functions()) {
            if (functions.put(function.name(), function.parameters().size()) != null) {
                throw new IllegalStateException(function.name() + " is declared twice in " + header.name());
            }
        }

        return functions;
    }

    // The functions of the header whose marker and @experimental tag disagree.
    private static List<String> experimentalTags(final ApiModel.Header header) {
        final List<String> errors = new ArrayList<>();

        for (final ApiModel.Function function : header.functions()) {
            final boolean tagged = ApiModel.experimentalTag(function.doc());

            if (tagged && !function.marked()) {
                errors.add(function.name() + " has @experimental in its description, but is not marked "
                        + ApiModel.EXPERIMENTAL_MARKER);
            } else if (!tagged && function.marked()) {
                errors.add(function.name() + " is marked " + ApiModel.EXPERIMENTAL_MARKER
                        + ", but has no @experimental in its description");
            }
        }

        return errors;
    }

    // The dxfg_v4_*.h headers that the header includes, dxfg_v4_api.h among them.
    private static Set<String> includes(final Path header) throws IOException {
        final String text = ApiModel.COMMENTS.matcher(ApiModel.read(header)).replaceAll(" ");

        return V4_INCLUDE.matcher(text).results().map(result -> result.group(1))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    // The dxfg_* and DXFG_* names that the header declares: functions, macros, tags, typedefs and enumerators.
    private static Set<String> names(final Path header, final Set<String> functions) throws IOException {
        final String withPreprocessor = ApiModel.code(header);
        final String text = ApiModel.PREPROCESSOR.matcher(withPreprocessor).replaceAll(" ");
        final Set<String> names = new TreeSet<>(functions);

        MACRO.matcher(withPreprocessor).results().forEach(result -> names.add(result.group(1)));

        for (final Pattern pattern : List.of(TAG, TYPEDEF_AFTER_BODY, TYPEDEF, FUNCTION_POINTER_TYPEDEF)) {
            pattern.matcher(text).results().forEach(result -> names.add(result.group(1)));
        }

        ENUM_BODY.matcher(text).results().forEach(body -> {
            for (final String enumerator : body.group(1).split(",")) {
                final Matcher matcher = ENUMERATOR.matcher(enumerator);

                if (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        });

        names.removeIf(name -> !name.regionMatches(true, 0, "dxfg_", 0, "dxfg_".length()));

        return names;
    }
}
