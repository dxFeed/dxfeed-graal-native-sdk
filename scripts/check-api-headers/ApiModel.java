// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The model of the C API headers that {@code CheckApiHeaders.java} and {@code GenerateApiJson.java} share: the text
 * helpers, the list of the advanced headers and a parser of the top-level declarations of a header (functions, types
 * and macros, each with its documentation comment).
 *
 * <p>The parser knows the shapes of the declarations that the headers use, not the whole of C: a declaration it cannot
 * read fails the build instead of being skipped.
 */
final class ApiModel {

    /**
     * The advanced headers: {@code dxfg_v4_*.h} headers with experimental functions only, which {@code dxfg_v4_api.h}
     * does not include.
     */
    static final Set<String> ADVANCED_HEADERS = Set.of();

    static final String V4_PREFIX = "dxfg_v4_";
    static final String EXPERIMENTAL_MARKER = "DXFG_EXPERIMENTAL";
    static final String OUT_MARKER = "DXFG_OUT";

    // A dxfg_v4_* name, such as a replacement in a deprecation message.
    static final Pattern V4_FUNCTION = Pattern.compile("\\b" + V4_PREFIX + "\\w+");
    static final Pattern COMMENTS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    static final Pattern STRINGS = Pattern.compile("\"(?:[^\"\\\\\\n]|\\\\.)*\"");
    static final Pattern PREPROCESSOR = Pattern.compile("^\\s*#[^\\n]*(?:\\\\\\n[^\\n]*)*", Pattern.MULTILINE);

    private static final Pattern DEFINE = Pattern.compile("#\\s*define\\s+(\\w+)(\\(?)(.*)", Pattern.DOTALL);
    private static final Pattern DIRECTIVE = Pattern.compile("#\\s*(\\w+)\\s*(.*)", Pattern.DOTALL);
    private static final Pattern CPLUSPLUS = Pattern.compile("__cplusplus|defined\\s*\\(?\\s*__cplusplus\\s*\\)?");
    private static final Pattern SINCE = Pattern.compile("@since\\s+(\\S+)");
    private static final Pattern EXPERIMENTAL_TAG = Pattern.compile("^\\s*\\*?\\s*@experimental\\b", Pattern.MULTILINE);
    private static final Pattern DEPRECATION = Pattern.compile(
            "\\bDXFG_DEPRECATED\\s*\\(\\s*(\"(?:[^\"\\\\]|\\\\.)*\")\\s*\\)");
    private static final Pattern FUNCTION = Pattern.compile("(.*?)\\b(dxfg_\\w+)\\s*\\((.*)\\)");
    private static final Pattern AGGREGATE = Pattern.compile(
            "(typedef\\s+)?(struct|union|enum)\\b\\s*(\\w+)?\\s*\\{(.*)}(.*)");
    private static final Pattern FUNCTION_POINTER = Pattern.compile("(.+?)\\(\\s*\\*\\s*(\\w*)\\s*\\)\\s*\\((.*)\\)");
    private static final Pattern TOKEN = Pattern.compile("\\w+|\\[[^\\]]*]|\\S");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");
    private static final Set<String> TYPE_WORDS = Set.of(
            "void", "char", "short", "int", "long", "float", "double", "signed", "unsigned", "const", "volatile",
            "struct", "union", "enum", "_Bool", "bool");

    /** A parameter of a function or a function pointer, or a field of a struct. */
    record Variable(String name, String type, boolean out) {
    }

    /** An enumerator and the text of its value (empty when the enumerator has none). */
    record Enumerator(String name, String value) {
    }

    /**
     * A function declaration: the marker, the message of {@code DXFG_DEPRECATED} (null when not deprecated) and the
     * documentation comment (empty when there is none).
     */
    record Function(
            String name,
            String header,
            String returnType,
            List<Variable> parameters,
            boolean marked,
            String deprecation,
            String doc
    ) {
    }

    /**
     * A type: {@code kind} is "struct", "union", "enum", "alias" or "function_pointer"; {@code type} is the aliased
     * type or the return type of the function pointer; {@code fields} are the fields of a struct or a union or the
     * parameters of a function pointer.
     */
    record Type(
            String name,
            String header,
            String kind,
            String type,
            List<Variable> fields,
            List<Enumerator> enumerators,
            String doc
    ) {
    }

    /** An object-like macro with a value. */
    record Macro(String name, String header, String value, String doc) {
    }

    /** The declarations of one header with {@code dxfg_} and {@code DXFG_} names. */
    record Header(String name, List<Function> functions, List<Type> types, List<Macro> macros) {
    }

    private ApiModel() {
    }

    static boolean isApiHeader(final Path path) {
        final String name = path.getFileName().toString();

        return name.startsWith("dxfg_") && name.endsWith(".h");
    }

    static boolean isApiName(final String name) {
        return name.regionMatches(true, 0, "dxfg_", 0, "dxfg_".length());
    }

    static String read(final Path header) throws IOException {
        return Files.readString(header, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    // The text of the header without comments and string literals: the preprocessor lines are kept.
    static String code(final Path header) throws IOException {
        return STRINGS.matcher(COMMENTS.matcher(read(header)).replaceAll(" ")).replaceAll("\"\"");
    }

    /** The version of the {@code @since} tag of the documentation comment, or null. */
    static String since(final String doc) {
        final Matcher matcher = SINCE.matcher(doc);

        return matcher.find() ? matcher.group(1) : null;
    }

    /** Whether the documentation comment has the {@code @experimental} tag. */
    static boolean experimentalTag(final String doc) {
        return EXPERIMENTAL_TAG.matcher(doc).find();
    }

    /**
     * The top-level declarations of the header. A declaration inside a conditional block fails: the description cannot
     * tell when it exists. Only the include guard and the {@code __cplusplus} blocks may hold declarations.
     */
    static Header parse(final Path path) throws IOException {
        final String header = path.getFileName().toString();
        final String text = read(path);
        final List<Function> functions = new ArrayList<>();
        final List<Type> types = new ArrayList<>();
        final List<Macro> macros = new ArrayList<>();
        final StringBuilder statement = new StringBuilder();
        // The open conditional blocks: whether each may hold declarations.
        final List<Boolean> conditions = new ArrayList<>();
        // The name of the #ifndef just read, if it is the first directive of the header: the include guard when the
        // next directive defines it.
        String guard = null;
        boolean first = true;
        // The documentation comment before the next declaration, and the one of the declaration being read.
        String doc = "";
        String statementDoc = "";
        boolean lineStart = true;
        int depth = 0;
        int i = 0;

        while (i < text.length()) {
            final char c = text.charAt(i);

            if (lineStart && c == '#') {
                if (!statement.isEmpty()) {
                    throw new IllegalStateException("A preprocessor directive inside a declaration in " + path + ": "
                            + statement.toString().strip());
                }

                final int end = directiveEnd(text, i);
                final String directive = COMMENTS.matcher(text.substring(i, end).replace("\\\n", " "))
                        .replaceAll(" ").strip();
                final Matcher matcher = DIRECTIVE.matcher(directive);
                final String keyword = matcher.matches() ? matcher.group(1) : "";
                final String argument = matcher.matches() ? matcher.group(2).strip() : "";
                final Macro macro = macro(header, directive, doc);

                switch (keyword) {
                    case "if", "ifdef", "ifndef" -> conditions.add(!keyword.equals("ifndef")
                            && CPLUSPLUS.matcher(argument).matches());
                    case "endif" -> {
                        if (conditions.isEmpty()) {
                            throw new IllegalStateException("#endif without #if in " + path);
                        }

                        conditions.remove(conditions.size() - 1);
                    }
                    // Another branch exists only when the condition of the first one does not hold.
                    case "else", "elif", "elifdef", "elifndef" -> {
                        if (conditions.isEmpty()) {
                            throw new IllegalStateException("#" + keyword + " without #if in " + path);
                        }

                        conditions.set(conditions.size() - 1, false);
                    }
                    case "define" -> {
                        if (guard != null && directive.matches("#\\s*define\\s+" + Pattern.quote(guard))) {
                            conditions.set(conditions.size() - 1, true);
                        }
                    }
                    default -> {
                    }
                }

                guard = first && keyword.equals("ifndef") ? argument : null;
                first = false;

                if (macro != null) {
                    if (conditions.contains(false)) {
                        throw conditional(macro.name(), path);
                    }

                    macros.add(macro);
                }

                doc = "";
                i = end;
                continue;
            }

            if (text.startsWith("/*", i)) {
                final int end = text.indexOf("*/", i + 2);

                if (end < 0) {
                    throw new IllegalStateException("An unterminated comment in " + path);
                }

                if (text.startsWith("/**", i) && end > i + 2 && statement.isEmpty()) {
                    doc = text.substring(i + 3, end);
                }

                statement.append(statement.isEmpty() ? "" : " ");
                i = end + 2;
                continue;
            }

            if (text.startsWith("//", i)) {
                final int end = text.indexOf('\n', i);
                i = end < 0 ? text.length() : end;
                continue;
            }

            if (c == '"') {
                final int end = stringEnd(text, i);
                statement.append(text, i, end);
                lineStart = false;
                i = end;
                continue;
            }

            i++;

            if (Character.isWhitespace(c)) {
                lineStart |= c == '\n';

                if (!statement.isEmpty()) {
                    statement.append(' ');
                }

                continue;
            }

            lineStart = false;

            if (c == '{' && statement.toString().strip().equals("extern \"C\"")) {
                statement.setLength(0);
                continue;
            }

            if (c == '}' && depth == 0) {
                // The end of extern "C".
                continue;
            }

            if (c == ';' && depth == 0) {
                final int functionCount = functions.size();
                final int typeCount = types.size();
                declaration(header, statement.toString(), statementDoc, functions, types);

                if (conditions.contains(false) && functions.size() > functionCount) {
                    throw conditional(functions.get(functions.size() - 1).name(), path);
                }

                if (conditions.contains(false) && types.size() > typeCount) {
                    throw conditional(types.get(types.size() - 1).name(), path);
                }

                statement.setLength(0);
                doc = "";
                continue;
            }

            if (statement.isEmpty()) {
                statementDoc = doc;
            }

            depth += c == '{' ? 1 : c == '}' ? -1 : 0;
            statement.append(c);
        }

        if (!statement.toString().isBlank()) {
            throw new IllegalStateException("An unterminated declaration in " + path + ": "
                    + statement.toString().strip());
        }

        if (!conditions.isEmpty()) {
            throw new IllegalStateException("An unterminated #if in " + path);
        }

        return new Header(header, functions, types, macros);
    }

    private static IllegalStateException conditional(final String name, final Path path) {
        return new IllegalStateException(name + " is declared inside a conditional block in " + path
                + ": only the include guard and the __cplusplus blocks may hold declarations");
    }

    // The end of the preprocessor directive at the position: the end of its last line, continuations included.
    private static int directiveEnd(final String text, final int start) {
        int end = start;

        while (true) {
            final int lineEnd = text.indexOf('\n', end);

            if (lineEnd < 0) {
                return text.length();
            }

            if (text.charAt(lineEnd - 1) != '\\') {
                return lineEnd;
            }

            end = lineEnd + 1;
        }
    }

    private static int stringEnd(final String text, final int start) {
        int i = start + 1;

        while (i < text.length() && text.charAt(i) != '"') {
            i += text.charAt(i) == '\\' ? 2 : 1;
        }

        return i + 1;
    }

    // An object-like dxfg_ or DXFG_ macro with a value, or null.
    private static Macro macro(final String header, final String directive, final String doc) {
        final String text = COMMENTS.matcher(directive.replace("\\\n", " ")).replaceAll(" ").strip();
        final Matcher matcher = DEFINE.matcher(text);

        if (!matcher.matches() || !matcher.group(2).isEmpty() || !isApiName(matcher.group(1))) {
            return null;
        }

        final String value = matcher.group(3).strip().replaceAll("\\s+", " ");

        return value.isEmpty() ? null : new Macro(matcher.group(1), header, value, doc);
    }

    private static void declaration(
            final String header,
            final String statement,
            final String doc,
            final List<Function> functions,
            final List<Type> types
    ) {
        final String text = statement.strip().replaceAll("\\s+", " ");
        final Matcher aggregate = AGGREGATE.matcher(text);

        if (aggregate.matches()) {
            aggregate(header, aggregate, doc, types);
            return;
        }

        if (text.startsWith("typedef ")) {
            final Variable alias = declarator(text.substring("typedef ".length()));

            if (isApiName(alias.name())) {
                final Matcher pointer = FUNCTION_POINTER.matcher(text.substring("typedef ".length()));

                if (pointer.matches()) {
                    types.add(new Type(alias.name(), header, "function_pointer", type(pointer.group(1)),
                            parameters(pointer.group(3)), List.of(), doc));
                } else {
                    types.add(new Type(alias.name(), header, "alias", alias.type(), List.of(), List.of(), doc));
                }
            }

            return;
        }

        // The message of DXFG_DEPRECATED may name functions: it is removed before the name is looked for.
        final Matcher deprecation = DEPRECATION.matcher(text);
        final String message = deprecation.find() ? unquote(deprecation.group(1)) : null;
        final String code = DEPRECATION.matcher(text).replaceAll(" ");

        if (code.contains("{")) {
            // A function body: the declaration after it would be read as a part of it.
            throw new IllegalStateException("A function body or another unknown declaration in " + header + ": "
                    + text);
        }

        final Matcher function = FUNCTION.matcher(code);

        if (function.matches()) {
            final String prefix = function.group(1);
            final boolean marked = prefix.matches("(?:.*\\s)?" + EXPERIMENTAL_MARKER + "\\b.*");
            final String returnType = prefix.replaceAll("\\b(?:" + EXPERIMENTAL_MARKER + "|extern)\\b", " ");

            functions.add(new Function(function.group(2), header, type(returnType), parameters(function.group(3)),
                    marked, message, doc));
            return;
        }

        if (text.matches("(?:struct|union|enum) \\w+")) {
            // A forward declaration.
            return;
        }

        throw new IllegalStateException("Cannot read the declaration in " + header + ": " + text);
    }

    private static void aggregate(
            final String header,
            final Matcher matcher,
            final String doc,
            final List<Type> types
    ) {
        final boolean typedef = matcher.group(1) != null;
        final String kind = matcher.group(2);
        final String tag = matcher.group(3);
        final String body = matcher.group(4);
        final List<String> names = new ArrayList<>();

        if (typedef) {
            for (final String declarator : split(matcher.group(5), ',')) {
                names.add(declarator.strip());
            }
        } else if (tag != null && matcher.group(5).isBlank()) {
            names.add(tag);
        } else {
            throw new IllegalStateException("Cannot read the declaration of " + kind + " " + tag + " in " + header);
        }

        final List<Variable> fields = new ArrayList<>();
        final List<Enumerator> enumerators = new ArrayList<>();

        if (kind.equals("enum")) {
            for (final String enumerator : split(body, ',')) {
                final String[] parts = enumerator.split("=", 2);

                if (!enumerator.isBlank()) {
                    enumerators.add(new Enumerator(parts[0].strip(), parts.length > 1 ? parts[1].strip() : ""));
                }
            }
        } else {
            if (body.contains("{")) {
                throw new IllegalStateException("A nested struct or union in " + kind + " " + tag + " of " + header);
            }

            for (final String field : split(body, ';')) {
                if (!field.isBlank()) {
                    fields.addAll(fields(field));
                }
            }
        }

        for (final String name : names) {
            if (!isApiName(name.replace("*", "").strip())) {
                continue;
            }

            if (name.contains("*")) {
                final Variable pointer = declarator((tag == null ? kind : kind + " " + tag) + " " + name);
                types.add(new Type(pointer.name(), header, "alias", pointer.type(), List.of(), List.of(), doc));
            } else {
                types.add(new Type(name, header, kind, "", fields, enumerators, doc));
            }
        }
    }

    // The fields of one field declaration: "int32_t a, *b" declares two.
    private static List<Variable> fields(final String declaration) {
        final List<String> declarators = split(declaration, ',');
        final Variable first = declarator(declarators.get(0));
        final List<Variable> fields = new ArrayList<>(List.of(first));
        final String base = first.type().replaceAll("[\\s*]+$", "").replaceAll("\\[.*$", "");

        for (final String declarator : declarators.subList(1, declarators.size())) {
            fields.add(declarator(base + " " + declarator.strip()));
        }

        return fields;
    }

    private static List<Variable> parameters(final String parameters) {
        final String text = parameters.strip();

        if (text.isEmpty() || text.equals("void")) {
            return List.of();
        }

        return split(text, ',').stream().map(ApiModel::declarator).toList();
    }

    // The name and the type of a declarator: "DXFG_OUT const char **name", "void (*f)(int)" or an unnamed "void *".
    private static Variable declarator(final String declarator) {
        String text = declarator.strip();
        final boolean out = text.matches("(?:.*\\s)?" + OUT_MARKER + "\\b.*");

        text = text.replaceAll("\\b" + OUT_MARKER + "\\b", " ").strip();

        final Matcher pointer = FUNCTION_POINTER.matcher(text);

        if (pointer.matches()) {
            final String parameters = String.join(", ",
                    parameters(pointer.group(3)).stream().map(Variable::type).toList());

            return new Variable(pointer.group(2), type(pointer.group(1)) + " (*)(" + parameters + ")", out);
        }

        final List<String> tokens = TOKEN.matcher(text).results().map(MatchResult::group).toList();
        int end = tokens.size();

        while (end > 0 && tokens.get(end - 1).startsWith("[")) {
            end--;
        }

        final String last = end > 0 ? tokens.get(end - 1) : "";
        final boolean named = end > 1 && IDENTIFIER.matcher(last).matches() && !TYPE_WORDS.contains(last)
                && !List.of("struct", "union", "enum").contains(tokens.get(end - 2));

        if (!named) {
            return new Variable("", type(text), out);
        }

        final List<String> type = new ArrayList<>(tokens.subList(0, end - 1));
        type.addAll(tokens.subList(end, tokens.size()));

        return new Variable(last, type(String.join(" ", type)), out);
    }

    // The type in one spelling: words separated by one space, "*" after a space, "**" together, "[N]" attached.
    static String type(final String type) {
        final StringBuilder result = new StringBuilder();

        for (final String token : TOKEN.matcher(type).results().map(MatchResult::group).toList()) {
            final boolean afterStar = result.length() > 0 && result.charAt(result.length() - 1) == '*';

            if (result.length() > 0 && !(token.equals("*") && afterStar) && !token.startsWith("[")) {
                result.append(' ');
            }

            result.append(token.startsWith("[") ? token.replaceAll("\\s+", "") : token);
        }

        return result.toString();
    }

    // The parts of the text between the separators outside parentheses and brackets.
    private static List<String> split(final String text, final char separator) {
        final List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;

        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);

            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (c == separator && depth == 0) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }

        parts.add(text.substring(start));

        return parts.stream().map(String::strip).filter(part -> !part.isEmpty()).toList();
    }

    // The text of a C string literal: the escaped quotes and backslashes unescaped.
    private static String unquote(final String literal) {
        return literal.substring(1, literal.length() - 1).replaceAll("\\\\([\"\\\\])", "$1");
    }
}
