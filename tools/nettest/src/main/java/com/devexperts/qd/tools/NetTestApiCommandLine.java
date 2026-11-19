// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.util.TimePeriod;
import com.dxfeed.api.DXEndpoint;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The command line of {@link NetTestApi} as the NetTest specification defines it (sections "Command line" and
 * "Contracts and roles"): the mode, the address and the options in any order, {@code --option value} and
 * {@code --option=value} for the long names, the last value of a repeated option, and the error texts of the
 * conformance kit ({@code cli-vectors.json}). The first error found wins.
 */
final class NetTestApiCommandLine {
    static final int MAX_SYMBOLS = 100_000_000;
    static final int MAX_BATCH = 1 << 22; // a batch shares one time: the 22-bit sequence must not wrap inside it
    static final int MAX_INSTANCES = 1_000;
    static final double MAX_RATE = 1e12;
    static final int DEFAULT_BATCH = 100;
    static final TimePeriod DEFAULT_STAT_PERIOD = TimePeriod.valueOf("10s");
    static final String DEFAULT_NAME = "NetTest";
    static final String DEFAULT_METRICS_HOST = "127.0.0.1";
    static final int MAX_PORT = 65_535;

    // The options with a value: every name maps to the key of its value.
    private static final Map<String, String> VALUE_OPTIONS = new HashMap<>();
    // The options of the mode p.
    private static final Set<String> PRODUCER_OPTIONS = Set.of("-r", "--rate-limit", "--batch", "--reuse");

    static {
        for (String[] names : new String[][] {
            {"-S", "--symbols"}, {"-C", "--connections"}, {"-r", "--rate-limit"}, {"-s", "--stat"},
            {"-n", "--name"}, {"-c", "--collector"}, {"--event"}, {"--batch"}, {"--role"}, {"--duration"},
            {"--warmup"}, {"--summary"}, {"--metrics-port"}})
        {
            for (String name : names) {
                VALUE_OPTIONS.put(name, names[0]);
            }
        }
    }

    private final List<String> args;
    private final List<String> positional = new ArrayList<>();
    private final Map<String, Given> values = new HashMap<>();
    private final List<String[]> properties = new ArrayList<>();
    private String firstProducerOption;
    private boolean reuse;

    /** An option value with the name of the option as the command line gave it. */
    private static final class Given {
        final String name;
        final String value;

        Given(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private NetTestApiCommandLine(String[] args) {
        this.args = List.of(args);
    }

    /** Whether the arguments ask for the usage: {@code --help} anywhere outside the value of an option. */
    static boolean isHelp(String... args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--help")) {
                return true;
            }
            if (VALUE_OPTIONS.containsKey(arg)) {
                i++;
            }
        }
        return false;
    }

    /**
     * Parses the arguments into the settings of a run and then sets the {@code -D} properties in the order of the
     * command line.
     *
     * @throws BadToolParametersException with the text of the specification for the first invalid argument.
     */
    static NetTestApi.Settings parse(String... args) {
        NetTestApiCommandLine commandLine = new NetTestApiCommandLine(args);
        commandLine.tokenize(args);
        NetTestApi.Settings settings = commandLine.validate();
        for (String[] property : commandLine.properties) {
            System.setProperty(property[0], property[1]);
        }
        return settings;
    }

    private void tokenize(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String name;
            String inline = null;
            if (arg.startsWith("--") && arg.length() > 2) {
                int equals = arg.indexOf('=');
                name = equals < 0 ? arg : arg.substring(0, equals);
                inline = equals < 0 ? null : arg.substring(equals + 1);
            } else if (arg.startsWith("-D")) {
                int equals = arg.indexOf('=');
                if (equals <= 2) {
                    throw error("-D needs <key>=<value>: '" + arg + "'");
                }
                properties.add(new String[] {arg.substring(2, equals), arg.substring(equals + 1)});
                continue;
            } else if (arg.startsWith("-X")) {
                throw error("unsupported option: " + arg + " (a JVM option: put it on the java command line before " +
                    "-jar)");
            } else if (arg.startsWith("-") && arg.length() > 1) {
                name = arg;
            } else {
                positional.add(arg);
                continue;
            }
            if (PRODUCER_OPTIONS.contains(name) && firstProducerOption == null) {
                firstProducerOption = name;
            }
            if (VALUE_OPTIONS.containsKey(name)) {
                String value = inline;
                if (value == null) {
                    if (i + 1 >= args.length) {
                        throw error("missing value of " + name);
                    }
                    value = args[++i];
                }
                values.put(VALUE_OPTIONS.get(name), new Given(name, value));
                continue;
            }
            switch (name) {
                case "--reuse":
                    if (inline != null) {
                        throw error("--reuse takes no value: '" + arg + "'");
                    }
                    reuse = true;
                    break;
                case "--help":
                    break; // NetTestApi prints the usage before parsing
                case "-w":
                case "--wildcard":
                    throw error("unsupported QD flag: " + name + " (the wildcard subscription)");
                case "--legacy":
                    throw error("unsupported option: --legacy (the deprecated publishing function of the C API)");
                default:
                    throw error("unknown flag " + name);
            }
        }
    }

    private NetTestApi.Settings validate() {
        if (positional.isEmpty()) {
            throw error("no mode: the first argument is p or c");
        }
        String modeText = positional.get(0);
        if (!modeText.equals("p") && !modeText.equals("c")) {
            throw error("unknown mode '" + modeText + "'; the mode is p or c");
        }
        NetTestApi.Mode mode = modeText.equals("p") ? NetTestApi.Mode.PRODUCER : NetTestApi.Mode.CONSUMER;
        if (positional.size() < 2 || positional.get(1).isEmpty()) {
            throw error("the address is missing");
        }
        String address = positional.get(1);
        if (positional.size() > 2) {
            throw error("unexpected argument '" + positional.get(2) + "' after the address '" + address + "'");
        }
        if (mode == NetTestApi.Mode.CONSUMER && firstProducerOption != null) {
            throw error(firstProducerOption + " is an option of the mode p");
        }

        NetTestConfig config = new NetTestConfig();
        Given symbols = values.get("-S");
        if (symbols == null) {
            throw error("-S is missing");
        }
        parseSymbols(symbols.value, config);
        config.symbolsPerEntity = config.totalSymbols;

        Given instances = values.get("-C");
        config.instanceCount = instances == null ? 1 : parseCount(instances, MAX_INSTANCES);
        if (config.instanceCount > 1 && address.startsWith(":")) {
            throw error("-C " + config.instanceCount + " cannot be used with the server address '" + address +
                "': every instance would bind the same port");
        }
        config.addresses = new String[config.instanceCount];
        Arrays.fill(config.addresses, address);

        Given rateGiven = values.get("-r");
        double rate = 0;
        if (rateGiven != null) {
            rate = parseRate(rateGiven);
        }
        Given statGiven = values.get("-s");
        TimePeriod statPeriod = statGiven == null ? DEFAULT_STAT_PERIOD : TimePeriod.valueOf(parseTime(statGiven));
        Given nameGiven = values.get("-n");
        config.name = nameGiven == null ? DEFAULT_NAME : nameGiven.value;
        if (config.name.isEmpty()) {
            throw error(nameGiven.name + " needs a non-empty name");
        }
        if (config.name.indexOf('*') >= 0) {
            throw error(nameGiven.name + " cannot contain '*': '" + config.name + "'");
        }
        Given batchGiven = values.get("--batch");
        int batch = batchGiven == null ? DEFAULT_BATCH : parseCount(batchGiven, MAX_BATCH);
        NetTestApi.EventKind event = parseEvent(values.get("--event"));
        Given durationGiven = values.get("--duration");
        long durationMillis = durationGiven == null ? 0 : parseTime(durationGiven);
        Given warmupGiven = values.get("--warmup");
        long warmupMillis = warmupGiven == null ? 0 : parseTime(warmupGiven);
        Given summaryGiven = values.get("--summary");
        Path summaryPath = summaryGiven == null ? null : parseSummaryPath(summaryGiven);
        Given metricsGiven = values.get("--metrics-port");
        String metricsHost = DEFAULT_METRICS_HOST;
        int metricsPort = -1;
        if (metricsGiven != null) {
            String value = metricsGiven.value;
            int colon = value.lastIndexOf(':');
            String host = colon < 0 ? "" : value.substring(0, colon);
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }
            metricsHost = host.isEmpty() ? DEFAULT_METRICS_HOST : host;
            metricsPort = parsePort(metricsGiven, value.substring(colon + 1));
        }

        String collector = parseCollector(values.get("-c"));
        DXEndpoint.Role role = parseRole(values.get("--role"), mode, collector, event);
        return new NetTestApi.Settings(mode, config, address, event, collector, role, batch, reuse,
            rateGiven == null ? null : rate, durationMillis, statPeriod, warmupMillis, summaryPath, metricsHost,
            metricsPort, args);
    }

    private static void parseSymbols(String value, NetTestConfig config) {
        int at = value.indexOf('@');
        String head = at < 0 ? value : value.substring(0, at);
        if (head.indexOf('{') >= 0) {
            throw error("unsupported QD flag: the symbol length '{len,max}' in '-S " + value + "'");
        }
        if (head.indexOf('/') >= 0) {
            throw error("unsupported QD flag: the symbol split '/r' or '/s' in '-S " + value + "'");
        }
        if (!isDigits(head) || head.length() > 9 || Integer.parseInt(head) < 1 ||
            Integer.parseInt(head) > MAX_SYMBOLS)
        {
            throw error("-S needs <total> or <total>@<file>, where <total> is a number from 1 to " + MAX_SYMBOLS +
                ": '" + value + "'");
        }
        if (at >= 0 && at == value.length() - 1) {
            throw error("-S has no file name after '@': '" + value + "'");
        }
        config.totalSymbols = Integer.parseInt(head);
        config.ipfPath = at < 0 ? null : value.substring(at + 1);
    }

    private static int parseCount(Given given, int max) {
        String value = given.value;
        if (!isDigits(value) || value.length() > 9 || Integer.parseInt(value) < 1 || Integer.parseInt(value) > max) {
            throw error(given.name + " needs a number from 1 to " + max + ": '" + value + "'");
        }
        return Integer.parseInt(value);
    }

    // "*/<rate>": a positive decimal number with at most one dot and an optional k, M or G, from 1 to 1e12.
    private static double parseRate(Given given) {
        String value = given.value;
        if (value.indexOf(';') >= 0) {
            throw error("unsupported QD flag: the rate limiter bucket ';<time>' in '" + given.name + " " + value + "'");
        }
        if (value.length() < 3 || !value.startsWith("*/")) {
            throw error(given.name + " needs */<rate> (for example */500k): '" + value + "'");
        }
        String number = value.substring(2);
        double multiplier = 1;
        switch (number.charAt(number.length() - 1)) {
            case 'k':
                multiplier = 1e3;
                break;
            case 'M':
                multiplier = 1e6;
                break;
            case 'G':
                multiplier = 1e9;
                break;
            default:
                break;
        }
        if (multiplier != 1) {
            number = number.substring(0, number.length() - 1);
        }
        double rate = isDecimal(number) ? Double.parseDouble(number) * multiplier : Double.NaN;
        if (!(rate >= 1) || rate > MAX_RATE) {
            throw error(given.name + " has a bad rate (a positive number with an optional k, M or G): '" + value +
                "'");
        }
        return rate;
    }

    // A positive integer with the unit ms, s or m. Returns milliseconds.
    static long parseTime(Given given) {
        String value = given.value;
        long multiplier;
        int suffixLength;
        if (value.length() > 2 && value.endsWith("ms")) {
            multiplier = 1;
            suffixLength = 2;
        } else if (value.length() > 1 && value.endsWith("s")) {
            multiplier = 1_000;
            suffixLength = 1;
        } else if (value.length() > 1 && value.endsWith("m")) {
            multiplier = 60_000;
            suffixLength = 1;
        } else {
            multiplier = 0;
            suffixLength = 0;
        }
        String number = value.substring(0, value.length() - suffixLength);
        long time = multiplier > 0 && isDigits(number) && number.length() <= 15 ? Long.parseLong(number) : 0;
        if (time < 1 || time > Long.MAX_VALUE / multiplier) {
            throw error(given.name + " needs a positive time with ms, s or m (for example 10s): '" + value + "'");
        }
        return time * multiplier;
    }

    // The directory of the file must exist and be writable; the file itself is written after the run.
    private static Path parseSummaryPath(Given given) {
        Path path;
        try {
            path = given.value.isEmpty() ? null : Paths.get(given.value).toAbsolutePath();
        } catch (InvalidPathException e) {
            path = null;
        }
        Path directory = path == null ? null : path.getParent();
        if (directory == null || !Files.isDirectory(directory) || !Files.isWritable(directory) ||
            Files.isDirectory(path))
        {
            throw error(given.name + " cannot write to the directory of '" + given.value + "'");
        }
        return path;
    }

    private static int parsePort(Given given, String port) {
        if (!isDigits(port) || port.length() > 5 || Integer.parseInt(port) > MAX_PORT) {
            throw error(given.name + " needs [<host>:]<port>, where <port> is a number from 0 to " + MAX_PORT +
                ": '" + given.value + "'");
        }
        return Integer.parseInt(port);
    }

    private static NetTestApi.EventKind parseEvent(Given given) {
        if (given == null || given.value.equals("Trade")) {
            return NetTestApi.EventKind.TRADE;
        }
        if (given.value.equals("TimeAndSale")) {
            return NetTestApi.EventKind.TIME_AND_SALE;
        }
        throw error(given.name + " is Trade or TimeAndSale: '" + given.value + "'");
    }

    private static String parseCollector(Given given) {
        if (given == null) {
            return null;
        }
        switch (given.value) {
            case "stream":
            case "ticker":
                return given.value;
            case "history":
                throw error(given.name + " history is reserved");
            default:
                throw error(given.name + " is stream or ticker: '" + given.value + "'");
        }
    }

    /**
     * The role of the endpoints: the default of the mode and the contract, or {@code --role} when the consumer still
     * receives the contract of {@code -c} with it.
     */
    private static DXEndpoint.Role parseRole(Given given, NetTestApi.Mode mode, String collector,
        NetTestApi.EventKind event)
    {
        boolean producer = mode == NetTestApi.Mode.PRODUCER;
        DXEndpoint.Role role;
        if (given == null) {
            if (producer) {
                role = "stream".equals(collector) ? DXEndpoint.Role.STREAM_PUBLISHER : DXEndpoint.Role.PUBLISHER;
            } else {
                role = "ticker".equals(collector) ? DXEndpoint.Role.FEED : DXEndpoint.Role.STREAM_FEED;
            }
        } else {
            DXEndpoint.Role[] allowed = producer ?
                new DXEndpoint.Role[] {DXEndpoint.Role.PUBLISHER, DXEndpoint.Role.STREAM_PUBLISHER} :
                new DXEndpoint.Role[] {DXEndpoint.Role.FEED, DXEndpoint.Role.STREAM_FEED};
            role = null;
            for (DXEndpoint.Role candidate : allowed) {
                if (candidate.name().equals(given.value)) {
                    role = candidate;
                }
            }
            if (role == null) {
                throw error(given.name + " is " + allowed[0] + " or " + allowed[1] + ": '" + given.value + "'");
            }
        }
        if ("ticker".equals(collector)) {
            if (event != NetTestApi.EventKind.TRADE) {
                throw error("-c ticker needs --event Trade: TimeAndSale has no ticker contract");
            }
            if (role == DXEndpoint.Role.STREAM_PUBLISHER || role == DXEndpoint.Role.STREAM_FEED) {
                throw error("--role " + role + " conflicts with -c ticker for Trade");
            }
        }
        if ("stream".equals(collector) && role == DXEndpoint.Role.FEED && event == NetTestApi.EventKind.TRADE) {
            throw error("--role FEED conflicts with -c stream for Trade");
        }
        return role;
    }

    private static BadToolParametersException error(String text) {
        return new BadToolParametersException(text);
    }

    private static boolean isDigits(String text) {
        return !text.isEmpty() && text.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    // Digits with at most one inner dot: no sign, no exponent, no spaces.
    private static boolean isDecimal(String text) {
        int dot = text.indexOf('.');
        if (dot < 0) {
            return isDigits(text);
        }
        return dot > 0 && dot < text.length() - 1 && text.indexOf('.', dot + 1) < 0 &&
            isDigits(text.substring(0, dot)) && isDigits(text.substring(dot + 1));
    }
}
