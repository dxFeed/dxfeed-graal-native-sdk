// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks that the wrapper guide ({@code docs/wrapper-guide}) and the conformance kit ({@code src/main/kit}) name only
 * what the C API has, against its description {@code dxfg-api.json} (see {@code scripts/check-api-headers}):
 *
 * <ul>
 *   <li>every {@code dxfg_*} and {@code DXFG_*} name in the {@code *.md} and {@code *.scenario} files is a function, a
 *   type, an enumerator or a macro of the description (or one of the attribute macros {@code DXFG_OUT},
 *   {@code DXFG_DEPRECATED} and {@code DXFG_EXPERIMENTAL}), and no function is deprecated; a name followed by {@code .h}
 *   is a header. A name that ends with {@code _} is a prefix ({@code dxfg_v4_<area>.h}) and is not checked, nor are
 *   the {@code graal_*} names of GraalVM, which the description does not cover;</li>
 *   <li>the features of the {@code requires feature} lines of the scenarios are features of the description;</li>
 *   <li>the event types of the scenarios ({@code Quote} is {@code dxfg_quote_t}, {@code TimeAndSale} is
 *   {@code dxfg_time_and_sale_t}) are structs of the description, and the fields they name are fields of the struct,
 *   with the members of embedded structs flattened recursively (as the kit's protocol does). The fields of
 *   {@code last} and {@code increasing} checks must be fields of one of the types of the scenario's queues.</li>
 * </ul>
 *
 * <p>Usage: {@code java CheckDocNames.java <dxfg-api.json> <directory>...}. The errors name the file and the line.
 *
 * <p>Self-test: {@code java CheckDocNames.java --self-test <directory of fixtures>}. The directory has a
 * {@code dxfg-api.json}, and each of its subdirectories is a fixture: the files to check and {@code expected.txt}
 * with the errors the check must report (a part of each error, one per line; an empty file when the check must
 * pass). The self-test fails if a fixture reports another set of errors.
 */
public final class CheckDocNames {

    private static final Pattern NAME = Pattern.compile("\\b(?:dxfg|DXFG)_[A-Za-z0-9_]*");

    // The attribute macros of dxfg_common.h, which the description gives as the out flags, `deprecated` and the tiers.
    private static final Set<String> ATTRIBUTE_MACROS = Set.of("DXFG_OUT", "DXFG_DEPRECATED", "DXFG_EXPERIMENTAL");

    private CheckDocNames() {
    }

    public static void main(final String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("--self-test")) {
            System.exit(selfTest(Path.of(args[1])) ? 0 : 1);
        }

        if (args.length < 2) {
            System.err.println("Usage: java CheckDocNames.java <dxfg-api.json> <directory>...");
            System.err.println("       java CheckDocNames.java --self-test <directory of fixtures>");
            System.exit(2);
        }

        final Api api = Api.read(Path.of(args[0]));
        final List<String> errors = new ArrayList<>();
        int files = 0;

        for (int i = 1; i < args.length; i++) {
            files += check(api, Path.of(args[i]), errors);
        }

        if (!errors.isEmpty()) {
            errors.forEach(error -> System.err.println("ERROR: " + error));
            System.err.println(errors.size() + " problems in the names of the documentation (checked against "
                    + args[0] + ")");
            System.exit(1);
        }

        System.out.println("The names of the documentation are in the API description: " + files + " files");
    }

    // Runs the check over every fixture and compares its errors with the expected ones.
    private static boolean selfTest(final Path fixtures) throws IOException {
        final Api api = Api.read(fixtures.resolve("dxfg-api.json"));
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
            final List<String> expected = Files.readAllLines(fixture.resolve("expected.txt"), StandardCharsets.UTF_8)
                    .stream().map(String::strip).filter(line -> !line.isEmpty()).toList();
            final List<String> unexpected = new ArrayList<>();
            check(api, fixture, unexpected);
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
            System.out.println("The self-test of the documentation name check passed: " + directories.size()
                    + " fixtures");
        }

        return passed;
    }

    // Checks the *.md and *.scenario files under the directory; returns the number of the files.
    private static int check(final Api api, final Path directory, final List<String> errors) throws IOException {
        final List<Path> files;

        try (Stream<Path> paths = Files.walk(directory)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md") || path.toString().endsWith(".scenario"))
                    .sorted().toList();
        }

        for (final Path file : files) {
            final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            checkNames(api, file, lines, errors);

            if (file.toString().endsWith(".scenario")) {
                checkScenario(api, file, lines, errors);
            }
        }

        return files.size();
    }

    private static void checkNames(final Api api, final Path file, final List<String> lines,
                                   final List<String> errors) {
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final Matcher matcher = NAME.matcher(line);

            while (matcher.find()) {
                final String name = matcher.group();

                if (name.endsWith("_")) {
                    continue; // a prefix: dxfg_v4_<area>.h, DXFG_V4_FEATURE_*
                }

                final String where = file + ":" + (i + 1) + ": ";

                if (line.startsWith(".h", matcher.end())) {
                    if (!api.headers.contains(name + ".h")) {
                        errors.add(where + "unknown header " + name + ".h");
                    }
                } else if (api.deprecated.containsKey(name)) {
                    errors.add(where + name + " is deprecated: " + api.deprecated.get(name));
                } else if (!api.names.contains(name)) {
                    errors.add(where + "unknown name " + name);
                }
            }
        }
    }

    private static void checkScenario(final Api api, final Path file, final List<String> lines,
                                      final List<String> errors) {
        final Set<String> queueTypes = new LinkedHashSet<>();
        final List<String[]> laterFields = new ArrayList<>(); // {where, field} of `last` and `increasing`

        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i).strip();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            final String where = file + ":" + (i + 1) + ": ";
            final String[] tokens = line.split(" +");
            List<String> request = null;

            switch (tokens[0]) {
                case "requires" -> {
                    if (tokens.length >= 3 && tokens[1].equals("feature") && !api.features.contains(tokens[2])) {
                        errors.add(where + "unknown feature " + tokens[2]);
                    }
                }
                case "send" -> request = List.of(tokens).subList(1, tokens.length);
                case "expect" -> {
                    final int arrow = List.of(tokens).indexOf("->");
                    request = List.of(tokens).subList(1, arrow < 0 ? tokens.length : arrow);
                }
                case "event" -> {
                    if (tokens.length >= 2) {
                        checkFields(api, where, tokens[1], List.of(tokens).subList(2, tokens.length), errors);
                    }
                }
                case "last", "increasing" -> {
                    if (tokens.length >= 3) {
                        laterFields.add(new String[] {where, tokens[2].split("=", 2)[0]});
                    }
                }
                default -> {
                }
            }

            if (request == null || request.isEmpty()) {
                continue;
            }

            if (request.get(0).equals("publish") && request.size() >= 3) {
                checkFields(api, where, request.get(2), request.subList(3, request.size()), errors);
            } else if (request.get(0).equals("queue") && request.size() >= 4) {
                for (final String type : request.get(3).split(",")) {
                    if (api.eventFields(type) == null) {
                        errors.add(where + "unknown event type " + type + " (no struct " + structName(type) + ")");
                    } else {
                        queueTypes.add(type);
                    }
                }
            }
        }

        for (final String[] later : laterFields) {
            if (queueTypes.stream().noneMatch(type -> api.eventFields(type).contains(later[1]))) {
                errors.add(later[0] + "unknown field " + later[1] + " of the types " + queueTypes);
            }
        }
    }

    // Checks the `field=value` tokens of an event of the type.
    private static void checkFields(final Api api, final String where, final String type, final List<String> tokens,
                                    final List<String> errors) {
        final Set<String> fields = api.eventFields(type);

        if (fields == null) {
            errors.add(where + "unknown event type " + type + " (no struct " + structName(type) + ")");
            return;
        }

        for (final String token : tokens) {
            final String field = token.split("=", 2)[0];

            if (!fields.contains(field)) {
                errors.add(where + "unknown field " + field + " of " + type + " (" + structName(type) + ")");
            }
        }
    }

    // The struct of an event type of the kit: TimeAndSale is dxfg_time_and_sale_t.
    private static String structName(final String type) {
        return "dxfg_" + type.replaceAll("(?<=[a-z0-9])([A-Z])", "_$1").toLowerCase() + "_t";
    }

    /** What the check needs of the API description. */
    private static final class Api {

        final Set<String> names = new TreeSet<>();
        final Map<String, String> deprecated = new LinkedHashMap<>(); // the name and what to use instead
        final Set<String> headers = new TreeSet<>();
        final Set<String> features = new TreeSet<>();
        final Map<String, Map<String, Object>> types = new LinkedHashMap<>();
        final Map<String, Set<String>> eventFields = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        static Api read(final Path path) throws IOException {
            final Map<String, Object> root =
                    (Map<String, Object>) new Json(Files.readString(path, StandardCharsets.UTF_8)).parse();
            final Api api = new Api();
            api.names.addAll(ATTRIBUTE_MACROS);

            for (final Object header : list(root, "headers")) {
                api.headers.add((String) ((Map<String, Object>) header).get("name"));
            }

            for (final Object feature : list(root, "features")) {
                api.features.add((String) feature);
            }

            for (final Object item : list(root, "functions")) {
                final Map<String, Object> function = (Map<String, Object>) item;
                final String name = (String) function.get("name");
                final Map<String, Object> deprecation = (Map<String, Object>) function.get("deprecated");
                api.names.add(name);

                if (deprecation != null) {
                    final List<Object> replacements = (List<Object>) deprecation.get("replacements");
                    api.deprecated.put(name, replacements == null || replacements.isEmpty()
                            ? String.valueOf(deprecation.get("message"))
                            : "use " + String.join(", ", replacements.stream().map(String::valueOf).toList()));
                }
            }

            for (final Object item : list(root, "types")) {
                final Map<String, Object> type = (Map<String, Object>) item;
                final String name = (String) type.get("name");
                api.names.add(name);
                api.types.put(name, type);

                for (final Object enumerator : list(type, "enumerators")) {
                    api.names.add((String) ((Map<String, Object>) enumerator).get("name"));
                }
            }

            for (final Object macro : list(root, "macros")) {
                api.names.add((String) ((Map<String, Object>) macro).get("name"));
            }

            return api;
        }

        // The flattened fields of the struct of an event type of the kit, or null if there is no such struct.
        Set<String> eventFields(final String type) {
            return eventFields.computeIfAbsent(type, key -> {
                final Map<String, Object> struct = resolve(structName(key));

                if (struct == null || !"struct".equals(struct.get("kind"))) {
                    return null;
                }

                final Set<String> fields = new LinkedHashSet<>();
                flatten(struct, fields, 0);
                return fields;
            });
        }

        @SuppressWarnings("unchecked")
        private void flatten(final Map<String, Object> struct, final Set<String> fields, final int depth) {
            for (final Object item : list(struct, "fields")) {
                final Map<String, Object> field = (Map<String, Object>) item;
                final String type = String.valueOf(field.get("type")).replace("const ", "").strip();
                final Map<String, Object> member = type.contains("*") ? null : resolve(type);

                if (member != null && "struct".equals(member.get("kind")) && depth < 16) {
                    flatten(member, fields, depth + 1);
                } else {
                    fields.add((String) field.get("name"));
                }
            }
        }

        // The type of the name, through its aliases; null if the description has none.
        private Map<String, Object> resolve(final String name) {
            Map<String, Object> type = types.get(name.replaceFirst("^struct ", ""));

            for (int i = 0; type != null && "alias".equals(type.get("kind")) && i < 16; i++) {
                type = types.get(String.valueOf(type.get("type")).replaceFirst("^struct ", ""));
            }

            return type;
        }

        @SuppressWarnings("unchecked")
        private static List<Object> list(final Map<String, Object> object, final String key) {
            final Object value = object.get(key);
            return value instanceof List<?> ? (List<Object>) value : List.of();
        }
    }

    /** A JSON parser: objects become maps, arrays lists, numbers longs or doubles. */
    private static final class Json {

        private final String text;
        private int position;

        Json(final String text) {
            this.text = text;
        }

        Object parse() {
            final Object value = value();
            skipWhitespace();

            if (position != text.length()) {
                throw error("the end of the text");
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
                final Map<String, Object> object = new LinkedHashMap<>();
                position++;
                skipWhitespace();

                if (peek('}')) {
                    return object;
                }

                do {
                    skipWhitespace();
                    final String key = string();
                    skipWhitespace();
                    expect(':');
                    object.put(key, value());
                    skipWhitespace();
                } while (peek(','));

                expect('}');
                return object;
            }

            if (c == '[') {
                final List<Object> array = new ArrayList<>();
                position++;
                skipWhitespace();

                if (peek(']')) {
                    return array;
                }

                do {
                    array.add(value());
                    skipWhitespace();
                } while (peek(','));

                expect(']');
                return array;
            }

            if (c == '"') {
                return string();
            }

            for (final String literal : new String[] {"true", "false", "null"}) {
                if (text.startsWith(literal, position)) {
                    position += literal.length();
                    return literal.equals("null") ? null : Boolean.valueOf(literal);
                }
            }

            final int start = position;

            while (position < text.length() && "+-.0123456789eE".indexOf(text.charAt(position)) >= 0) {
                position++;
            }

            if (start == position) {
                throw error("a value");
            }

            final String number = text.substring(start, position);
            return number.matches("-?\\d+") ? (Object) Long.parseLong(number) : (Object) Double.parseDouble(number);
        }

        private String string() {
            expect('"');
            final StringBuilder builder = new StringBuilder();

            while (position < text.length() && text.charAt(position) != '"') {
                char c = text.charAt(position++);

                if (c == '\\') {
                    if (position >= text.length()) {
                        throw error("an escape");
                    }

                    c = text.charAt(position++);

                    switch (c) {
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            if (position + 4 > text.length()) {
                                throw error("four hex digits");
                            }

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

        private IllegalArgumentException error(final String expected) {
            return new IllegalArgumentException("JSON: expected " + expected + " at " + position);
        }
    }
}
