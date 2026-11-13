// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.qd.util.RateLimiter;
import com.devexperts.util.TimePeriod;
import com.dxfeed.api.DXEndpoint;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.misc.Signal;

/**
 * Publishes generated Trade or TimeAndSale events through {@link com.dxfeed.api.DXPublisher} of a {@code PUBLISHER} or
 * {@code STREAM_PUBLISHER} endpoint and prints the statistics of QD NetTest, so that the dxFeed event layer can be
 * measured with the same command line as the C-API NetTest and against the same consumer as QD NetTest.
 *
 * <p>The tool lives in QD's package to reuse the package-private parts of QD NetTest: the symbol generation of
 * {@link NetTestSide}, the statistics collector and the working thread. It must run from one classpath with qds-tools
 * (the all-in-one jar), never as a separate jar next to {@code java -jar qds-tools.jar}.
 *
 * <p>Exit codes: 0 after {@code --duration} or Ctrl+C, 2 for invalid arguments, 1 for a failed start or publishing.
 * The tool calls {@link System#exit} itself for 1 and 2: QD's {@link Tools#main} would log a stack trace first.
 */
@ToolSummary(
    info = "Publishes Trade or TimeAndSale events through the dxFeed API (DXPublisher) and prints NetTest statistics.",
    argString = "p <address>",
    arguments = {
        "p -- the producer mode, the only one",
        "<address> -- :port to listen on, host:port to connect to"
    }
)
public class NetTestApi extends AbstractTool {
    static final int EXIT_FAILURE = 1;
    static final int EXIT_USAGE = 2;

    // The ranges of the C-API NetTest (src/main/c/src/apps/NetTest/src/Args.cpp). A batch shares one time, so it holds
    // at most 2^22 events: the 22-bit sequence must not wrap inside one batch.
    static final int MAX_SYMBOLS = 100_000_000;
    static final int MAX_BATCH = 1 << 22;
    static final int MAX_INSTANCES = 1_000;
    static final double MAX_RATE = 1e12;

    static final int DEFAULT_BATCH = 100;
    static final TimePeriod DEFAULT_STAT_PERIOD = TimePeriod.valueOf("10s");

    /** The kind of the published events. */
    enum EventKind {
        TRADE, TIME_AND_SALE
    }

    // QD's symbol option without the parts the C tool lacks: the split "/<n>" and the length "{<len>}".
    private final NetTest.OptionSymbols symbols = new NetTest.OptionSymbols() {
        @Override
        public String getDescription() {
            return "The symbols: <total> generated ones as in QD NetTest, or the first <total> of an IPF file with " +
                "<total>@<file> (at most " + MAX_SYMBOLS + "). Every instance publishes all of them.";
        }

        @Override
        public String toString() {
            return "-S|--symbols <total>[@<file>]";
        }

        @Override
        public int parse(int i, String[] args) throws OptionParseException {
            if (i + 1 < args.length) {
                checkSymbols(args[i + 1]);
            }
            try {
                return super.parse(i, args);
            } catch (OptionParseException e) {
                throw new OptionParseException("-S: " + e.getMessage(), e);
            }
        }
    };
    private final OptionInteger connections = new OptionInteger('C', "connections", "<n>",
        "Number of instances: endpoints <name>.1 ... <name>.<n>, one generator thread each (default 1, at most " +
            MAX_INSTANCES + "; not above 1 with a :port address).");
    private final OptionMultiString rateLimit = new OptionMultiString('r', "rate-limit", "*/<rate>",
        "The total rate of all instances in records per second, with k, M or G (for example */500k). Without it " +
            "the generators run flat out.");
    private final OptionString stat = new OptionString('s', "stat", "<period>",
        "The statistics period with ms, s or m (default 10s).");
    private final OptionName name = new OptionName("NetTest");
    private final OptionInteger batch = new OptionInteger(Option.EMPTY_SHORT_NAME, "batch", "<n>",
        "Events per publishing call (default " + DEFAULT_BATCH + ", at most " + MAX_BATCH + ").");
    private final OptionString event = new OptionString(Option.EMPTY_SHORT_NAME, "event", "Trade|TimeAndSale",
        "The published events: Trade (default) or TimeAndSale.");
    private final Option reuse = new Option(Option.EMPTY_SHORT_NAME, "reuse",
        "Republish the pre-built events in place; without it every batch publishes new event objects.");
    private final OptionString role = new OptionString(Option.EMPTY_SHORT_NAME, "role",
        "PUBLISHER|STREAM_PUBLISHER",
        "The role of the endpoints: PUBLISHER (default; Trade feeds ticker and stream, TimeAndSale stream and " +
            "history) or STREAM_PUBLISHER (stream only).");
    private final OptionString duration = new OptionString(Option.EMPTY_SHORT_NAME, "duration", "<time>",
        "End the run after the time with ms, s or m (for example 30s or 7m); Ctrl+C ends it at once.");

    private Settings settings;

    @Override
    protected Option[] getOptions() {
        return new Option[] {symbols, connections, rateLimit, stat, name, batch, event, reuse, role, duration};
    }

    /**
     * Parses and validates the arguments. Invalid arguments end the process with {@link #EXIT_USAGE}, since QD's
     * {@link Tools#main} would exit with 1 for every failure.
     */
    @Override
    public void parse(String[] args) {
        if (args.length == 0) {
            System.err.println(getToolName() + ": " + generateHelpSummary(Help.DEFAULT_WIDTH));
            System.exit(EXIT_USAGE);
        }
        try {
            settings = parseArguments(args);
        } catch (BadToolParametersException e) {
            System.err.println(getToolName() + ": " + e.getMessage());
            System.err.println("Use \"Help " + getToolName() + "\" for usage info.");
            System.exit(EXIT_USAGE);
        }
    }

    @Override
    protected void executeImpl(String[] ignored) {
        Stop stop = new Stop();
        installSignalHandlers(stop);
        NetTestApiProducerSide side = new NetTestApiProducerSide(settings, stop);
        Thread closer = new Thread(side::close, "NetTestApi-close");
        Runtime.getRuntime().addShutdownHook(closer);
        String failure = side.run();
        try {
            Runtime.getRuntime().removeShutdownHook(closer);
        } catch (IllegalStateException e) {
            // The JVM is already shutting down: the hook runs anyway.
        }
        if (failure != null) {
            // The side has logged the cause; Tools would log the exception once more with a stack trace.
            System.err.println(getToolName() + ": " + failure);
            System.exit(EXIT_FAILURE);
        }
    }

    /**
     * Parses the arguments of the tool into its settings. The {@code -D} properties are set as a side effect.
     *
     * @throws BadToolParametersException for invalid arguments; the message names the argument.
     */
    static Settings parseArguments(String... args) {
        return new NetTestApi().parseSettings(args);
    }

    private Settings parseSettings(String[] args) {
        rejectForeignOptions(args);
        String[] rest = new Options(getOptions()).parse(args);
        if (rest.length == 0) {
            throw new BadToolParametersException("no mode: the first argument must be 'p'");
        }
        if (rest[0].equals("c")) {
            throw new BadToolParametersException(
                "unsupported mode 'c' (the consumer mode of QD NetTest); only the mode 'p' exists");
        }
        if (!rest[0].equals("p")) {
            throw new BadToolParametersException("unknown mode '" + rest[0] + "'; only the mode 'p' exists");
        }
        if (rest.length < 2 || rest[1].isEmpty()) {
            throw new BadToolParametersException("the address is missing");
        }
        if (rest.length > 2) {
            throw new BadToolParametersException(
                "unexpected argument '" + rest[2] + "' after the address '" + rest[1] + "'");
        }
        String address = rest[1];
        if (!symbols.isSet()) {
            throw new BadToolParametersException("-S is missing");
        }

        NetTestConfig config = new NetTestConfig();
        config.name = name.isSet() ? name.getValue() : "NetTest";
        if (config.name.isEmpty()) {
            throw new BadToolParametersException("-n needs a non-empty name");
        }
        config.instanceCount = connections.isSet() ? checkRange("-C", connections.getValue(), MAX_INSTANCES) : 1;
        if (config.instanceCount > 1 && address.startsWith(":")) {
            throw new BadToolParametersException("-C " + config.instanceCount + " cannot be used with the server " +
                "address '" + address + "': every instance would bind the same port");
        }
        config.addresses = new String[config.instanceCount];
        java.util.Arrays.fill(config.addresses, address);
        config.totalSymbols = symbols.getTotal();
        config.symbolsPerEntity = symbols.getTotal();
        config.ipfPath = symbols.getIpfPath();
        if (rateLimit.isSet()) {
            String last = rateLimit.getValues().get(rateLimit.getValues().size() - 1);
            config.rateLimiters = Map.of("*", RateLimiter.of(parseRateOption(last)));
        }

        TimePeriod statPeriod = stat.isSet() ? TimePeriod.valueOf(parseTime("-s", stat.getValue())) :
            DEFAULT_STAT_PERIOD;
        long durationMillis = duration.isSet() ? parseTime("--duration", duration.getValue()) : 0;
        int batchSize = batch.isSet() ? checkRange("--batch", batch.getValue(), MAX_BATCH) : DEFAULT_BATCH;

        EventKind eventKind = EventKind.TRADE;
        if (event.isSet()) {
            switch (event.getValue()) {
                case "Trade":
                    break;
                case "TimeAndSale":
                    eventKind = EventKind.TIME_AND_SALE;
                    break;
                default:
                    throw new BadToolParametersException("--event is Trade or TimeAndSale: '" + event.getValue() + "'");
            }
        }
        DXEndpoint.Role endpointRole = DXEndpoint.Role.PUBLISHER;
        if (role.isSet()) {
            switch (role.getValue()) {
                case "PUBLISHER":
                    break;
                case "STREAM_PUBLISHER":
                    endpointRole = DXEndpoint.Role.STREAM_PUBLISHER;
                    break;
                default:
                    throw new BadToolParametersException(
                        "--role is PUBLISHER or STREAM_PUBLISHER: '" + role.getValue() + "'");
            }
        }
        return new Settings(config, address, eventKind, endpointRole, batchSize, reuse.isSet(), durationMillis,
            statPeriod);
    }

    // The options of QD NetTest, of the C tool and of the JVM that this tool does not take, with a precise message
    // instead of QD's "Unrecognized option".
    private static void rejectForeignOptions(String[] args) {
        for (String arg : args) {
            if (arg.equals("-c") || arg.equals("--collector")) {
                throw new BadToolParametersException("unsupported QD flag: " + arg + " (the collector of QD NetTest)");
            }
            if (arg.equals("-w") || arg.equals("--wildcard")) {
                throw new BadToolParametersException("unsupported QD flag: " + arg + " (the wildcard subscription)");
            }
            if (arg.equals("--legacy")) {
                throw new BadToolParametersException("--legacy is an option of the C tool only (the deprecated C " +
                    "publishing function)");
            }
            if (arg.startsWith("-X")) {
                throw new BadToolParametersException(arg + " is a JVM option: put it on the java command line " +
                    "before -jar");
            }
        }
    }

    private static void checkSymbols(String value) throws OptionParseException {
        int at = value.indexOf('@');
        String head = at < 0 ? value : value.substring(0, at);
        if (head.indexOf('{') >= 0) {
            throw new OptionParseException("unsupported QD flag: the symbol length '{len,max}' in '-S " + value + "'");
        }
        if (head.indexOf('/') >= 0) {
            throw new OptionParseException("unsupported QD flag: the symbol split '/r' or '/s' in '-S " + value + "'");
        }
        if (!isDigits(head) || head.length() > 9 || Integer.parseInt(head) < 1 || Integer.parseInt(head) > MAX_SYMBOLS) {
            throw new OptionParseException("-S needs <total> or <total>@<file>, where <total> is a number from 1 to " +
                MAX_SYMBOLS + ": '" + value + "'");
        }
        if (at >= 0 && at == value.length() - 1) {
            throw new OptionParseException("-S has no file name after '@': '" + value + "'");
        }
    }

    // Parses "*/<rate>" as the C tool does: a positive number with an optional k, M or G, at most 1e12.
    static double parseRateOption(String value) {
        if (value.indexOf(';') >= 0) {
            throw new BadToolParametersException(
                "unsupported QD flag: the rate limiter bucket ';<time>' in '-r " + value + "'");
        }
        if (value.length() < 3 || !value.startsWith("*/")) {
            throw new BadToolParametersException("-r needs */<rate> (for example */500k): '" + value + "'");
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
            throw new BadToolParametersException(
                "-r has a bad rate (a number from 1 to 1e12 with an optional k, M or G): '" + value + "'");
        }
        return rate;
    }

    private static int checkRange(String flag, int value, int max) {
        if (value < 1 || value > max) {
            throw new BadToolParametersException(flag + " needs a number from 1 to " + max + ": " + value);
        }
        return value;
    }

    // Parses a time as the C tool does: positive digits with the unit ms, s or m. Returns milliseconds.
    static long parseTime(String flag, String value) {
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
            throw new BadToolParametersException(flag + " needs a positive time with ms, s or m (for example 10s): '" +
                value + "'");
        }
        return time * multiplier;
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

    // Ctrl+C (SIGINT) and SIGTERM only request the stop: the main thread closes the endpoints and the tool exits with 0.
    // Without the handler the JVM would begin its shutdown with the status 130. A second signal halts at once.
    private static void installSignalHandlers(Stop stop) {
        AtomicBoolean received = new AtomicBoolean();
        for (String signal : new String[] {"INT", "TERM"}) {
            try {
                Signal.handle(new Signal(signal), s -> {
                    if (received.getAndSet(true)) {
                        Runtime.getRuntime().halt(128 + s.getNumber());
                    }
                    stop.request();
                });
            } catch (IllegalArgumentException e) {
                // The signal does not exist on this platform or is used by the JVM (-Xrs): the default stays.
            }
        }
    }

    public static void main(String[] args) {
        Tools.executeSingleTool(NetTestApi.class, args);
    }

    /** The validated arguments of a run. */
    static final class Settings {
        final NetTestConfig config;
        final String address;
        final EventKind event;
        final DXEndpoint.Role role;
        final int batch;
        final boolean reuse;
        final long durationMillis; // 0: until Ctrl+C
        final TimePeriod statPeriod;

        Settings(NetTestConfig config, String address, EventKind event, DXEndpoint.Role role, int batch,
            boolean reuse, long durationMillis, TimePeriod statPeriod)
        {
            this.config = config;
            this.address = address;
            this.event = event;
            this.role = role;
            this.batch = batch;
            this.reuse = reuse;
            this.durationMillis = durationMillis;
            this.statPeriod = statPeriod;
        }
    }

    /** The request to end the run: set once by the duration, a signal or a failed generator. */
    static final class Stop {
        private final CountDownLatch requested = new CountDownLatch(1);

        void request() {
            requested.countDown();
        }

        boolean isRequested() {
            return requested.getCount() == 0;
        }

        /** Waits for the request, at most {@code millis} when it is positive. */
        void await(long millis) throws InterruptedException {
            if (millis > 0) {
                requested.await(millis, TimeUnit.MILLISECONDS);
            } else {
                requested.await();
            }
        }
    }
}
