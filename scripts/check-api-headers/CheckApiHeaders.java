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
 * that names only declared {@code dxfg_v4_*} functions, {@code dxfg_v4_api.h} includes every {@code dxfg_v4_*.h} and
 * {@code dxfg_api.h} includes {@code dxfg_v4_api.h}.
 *
 * <p>Usage: {@code java CheckApiHeaders.java <directory of the API headers> <generated header>}. The argument types
 * are not compared: native-image declares them by their Java types.
 */
public final class CheckApiHeaders {

    private static final String V4_PREFIX = "dxfg_v4_";
    private static final String V4_API_HEADER = "dxfg_v4_api.h";
    private static final String API_HEADER = "dxfg_api.h";

    private static final Pattern COMMENTS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Pattern STRINGS = Pattern.compile("\"(?:[^\"\\\\\\n]|\\\\.)*\"");
    private static final Pattern PREPROCESSOR = Pattern.compile("^\\s*#[^\\n]*(?:\\\\\\n[^\\n]*)*", Pattern.MULTILINE);
    private static final Pattern DECLARATION = Pattern.compile("\\b(dxfg_\\w+)\\s*\\(([^;{]*?)\\)\\s*;");
    private static final Pattern DEPRECATION = Pattern.compile(
            "\\bDXFG_DEPRECATED\\s*\\(\\s*\"((?:[^\"\\\\\\n]|\\\\.)*)\"\\s*\\)[^;{(]*?\\b(dxfg_\\w+)\\s*\\(");
    private static final Pattern V4_FUNCTION = Pattern.compile("\\b" + V4_PREFIX + "\\w+");
    private static final Pattern V4_INCLUDE = Pattern.compile(
            "^\\s*#\\s*include\\s*\"(" + V4_PREFIX + "\\w+\\.h)\"", Pattern.MULTILINE);

    private CheckApiHeaders() {
    }

    public static void main(final String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java CheckApiHeaders.java <directory of the API headers> <generated header>");
            System.exit(2);
        }

        final Map<String, Integer> declared = new TreeMap<>();
        final Map<String, String> deprecations = new TreeMap<>();
        final Set<String> v4Headers = new TreeSet<>();

        try (Stream<Path> headers = Files.list(Path.of(args[0]))) {
            for (final Path header : headers.filter(CheckApiHeaders::isApiHeader).sorted().toList()) {
                declared.putAll(functions(header));
                deprecations.putAll(deprecations(header));

                final String name = header.getFileName().toString();

                if (name.startsWith(V4_PREFIX) && !name.equals(V4_API_HEADER)) {
                    v4Headers.add(name);
                }
            }
        }

        final Map<String, Integer> generated = functions(Path.of(args[1]));
        final List<String> errors = new ArrayList<>();

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
            if (function.startsWith(V4_PREFIX)) {
                continue;
            }

            final String message = deprecations.get(function);

            if (message == null) {
                errors.add(function + " is not marked DXFG_DEPRECATED");
                continue;
            }

            final List<String> replacements = V4_FUNCTION.matcher(message).results().map(MatchResult::group).toList();

            if (replacements.isEmpty()) {
                errors.add(function + " is deprecated without naming a " + V4_PREFIX + " function");
            }

            for (final String replacement : replacements) {
                if (!declared.containsKey(replacement)) {
                    errors.add(function + " is deprecated in favour of " + replacement + ", which no header declares");
                }
            }

            deprecated++;
        }

        final Set<String> included = includes(Path.of(args[0], V4_API_HEADER));

        for (final String header : v4Headers) {
            if (!included.contains(header)) {
                errors.add(header + " is not included by " + V4_API_HEADER);
            }
        }

        if (!includes(Path.of(args[0], API_HEADER)).contains(V4_API_HEADER)) {
            errors.add(V4_API_HEADER + " is not included by " + API_HEADER);
        }

        if (!errors.isEmpty()) {
            errors.forEach(error -> System.err.println("ERROR: " + error));
            System.err.println(errors.size() + " problems in " + args[0] + " (checked against " + args[1] + ")");
            System.exit(1);
        }

        System.out.println("The API headers declare the " + declared.size()
                + " functions of the library with the same numbers of parameters");
        System.out.println("The " + deprecated + " older functions are deprecated in favour of declared "
                + V4_PREFIX + " functions, " + V4_API_HEADER + " includes the " + v4Headers.size() + " v4 headers, "
                + API_HEADER + " includes " + V4_API_HEADER);
    }

    private static boolean isApiHeader(final Path path) {
        final String name = path.getFileName().toString();

        return name.startsWith("dxfg_") && name.endsWith(".h");
    }

    // The dxfg_* function declarations of the header: the name and the number of parameters.
    private static Map<String, Integer> functions(final Path header) throws IOException {
        String text = Files.readString(header, StandardCharsets.UTF_8).replace("\r\n", "\n");

        text = COMMENTS.matcher(text).replaceAll(" ");
        text = STRINGS.matcher(text).replaceAll("\"\"");
        text = PREPROCESSOR.matcher(text).replaceAll(" ");

        final Map<String, Integer> functions = new TreeMap<>();
        final Matcher matcher = DECLARATION.matcher(text);

        while (matcher.find()) {
            final Integer previous = functions.put(matcher.group(1), countParameters(matcher.group(2)));

            if (previous != null) {
                throw new IllegalStateException(matcher.group(1) + " is declared twice in " + header);
            }
        }

        return functions;
    }

    // The functions of the header marked DXFG_DEPRECATED: the name and the message.
    private static Map<String, String> deprecations(final Path header) throws IOException {
        final String text = COMMENTS.matcher(Files.readString(header, StandardCharsets.UTF_8).replace("\r\n", "\n"))
                .replaceAll(" ");
        final Map<String, String> deprecations = new TreeMap<>();
        final Matcher matcher = DEPRECATION.matcher(text);

        while (matcher.find()) {
            deprecations.put(matcher.group(2), matcher.group(1));
        }

        return deprecations;
    }

    // The dxfg_v4_*.h headers that the header includes, dxfg_v4_api.h among them.
    private static Set<String> includes(final Path header) throws IOException {
        final String text = COMMENTS.matcher(Files.readString(header, StandardCharsets.UTF_8).replace("\r\n", "\n"))
                .replaceAll(" ");

        return V4_INCLUDE.matcher(text).results().map(result -> result.group(1))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static int countParameters(final String parameters) {
        final String trimmed = parameters.trim();

        if (trimmed.isEmpty() || trimmed.equals("void")) {
            return 0;
        }

        int count = 1;
        int depth = 0;

        for (final char c : trimmed.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                count++;
            }
        }

        return count;
    }
}
