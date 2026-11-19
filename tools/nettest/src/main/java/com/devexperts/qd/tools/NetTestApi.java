// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.util.TimePeriod;
import com.dxfeed.api.DXEndpoint;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.misc.Signal;

/**
 * The NetTest of the dxFeed Java API: publishes generated Trade or TimeAndSale events through
 * {@link com.dxfeed.api.DXPublisher} (mode {@code p}) or receives them through a {@link com.dxfeed.api.DXFeed}
 * subscription (mode {@code c}), prints the statistics lines of the NetTest specification to stdout and, on request,
 * writes the summary file and serves the Prometheus metrics.
 *
 * <p>The tool lives in QD's package to reuse the package-private parts of QD NetTest: its configuration and the
 * symbol generator. It must run from one classpath with qds-tools (the all-in-one jar), never as a separate jar next
 * to {@code java -jar qds-tools.jar}.
 *
 * <p>Exit codes: 0 after {@code --duration}, Ctrl+C or {@code --help}, 2 for invalid arguments and an unwritable
 * summary path, 1 for a failure at run time (a failed start or publishing, a metrics port in use, no data, no
 * nanosecond part, the summary file). The tool calls {@link System#exit} itself
 * for 1 and 2: QD's {@link Tools#main} would log a stack trace first.
 */
@ToolSummary(
    info = "Publishes or receives Trade or TimeAndSale events through the dxFeed API and prints NetTest statistics.",
    argString = "p|c <address>",
    arguments = {
        "p -- the producer mode: publishes through DXPublisher",
        "c -- the consumer mode: receives through a DXFeed subscription and measures the end-to-end lag",
        "<address> -- :port to listen on, host:port to connect to"
    }
)
public class NetTestApi extends AbstractTool {
    static final int EXIT_FAILURE = 1;
    static final int EXIT_USAGE = 2;

    /** The version of the NetTest specification that the tool follows. */
    static final String SPEC_VERSION = "nettest/1";

    /** The side of a run. */
    enum Mode {
        PRODUCER, CONSUMER
    }

    /** The kind of the events. */
    enum EventKind {
        TRADE("Trade"), TIME_AND_SALE("TimeAndSale");

        final String text;

        EventKind(String text) {
            this.text = text;
        }
    }

    // The options for the usage only: NetTestApiCommandLine parses the arguments.
    private final Option symbols = new OptionString('S', "symbols", "<total>[@<file>]",
        "The symbols: <total> generated ones as in QD NetTest, or the first <total> of an IPF file (at most " +
            NetTestApiCommandLine.MAX_SYMBOLS + "). Every producer instance publishes all of them; the consumer " +
            "instances share them.");
    private final Option connections = new OptionString('C', "connections", "<n>",
        "Number of instances: endpoints <name>.1 ... <name>.<n> (default 1, at most " +
            NetTestApiCommandLine.MAX_INSTANCES + "; not above 1 with a :port address).");
    private final Option rateLimit = new OptionString('r', "rate-limit", "*/<rate>",
        "p only: the total rate of all instances in records per second, with k, M or G (for example */500k). " +
            "Without it the generators run flat out.");
    private final Option stat = new OptionString('s', "stat", "<period>",
        "The statistics period with ms, s or m (default 10s).");
    private final Option name = new OptionString('n', "name", "<name>",
        "The name in the statistics lines (default NetTest).");
    private final Option collector = new OptionString('c', "collector", "stream|ticker",
        "The contract that the consumer receives; it selects the roles (p: STREAM_PUBLISHER for stream, PUBLISHER " +
            "for ticker; c: STREAM_FEED for stream, FEED for ticker). Without it p uses PUBLISHER, c STREAM_FEED.");
    private final Option event = new OptionString(Option.EMPTY_SHORT_NAME, "event", "Trade|TimeAndSale",
        "The events: Trade (default) or TimeAndSale.");
    private final Option batch = new OptionString(Option.EMPTY_SHORT_NAME, "batch", "<n>",
        "p only: events per publishing call (default " + NetTestApiCommandLine.DEFAULT_BATCH + ", at most " +
            NetTestApiCommandLine.MAX_BATCH + ").");
    private final Option reuse = new Option(Option.EMPTY_SHORT_NAME, "reuse",
        "p only: republish the pre-built events in place; without it every batch publishes new event objects.");
    private final Option role = new OptionString(Option.EMPTY_SHORT_NAME, "role",
        "PUBLISHER|STREAM_PUBLISHER|FEED|STREAM_FEED",
        "The role of the endpoints instead of the one of -c: PUBLISHER or STREAM_PUBLISHER on p, FEED or " +
            "STREAM_FEED on c, as long as the consumer still receives the contract of -c.");
    private final Option duration = new OptionString(Option.EMPTY_SHORT_NAME, "duration", "<time>",
        "End the run after the time with ms, s or m (for example 30s or 7m); Ctrl+C ends it at once.");
    private final Option warmup = new OptionString(Option.EMPTY_SHORT_NAME, "warmup", "<time>",
        "The warm-up with ms, s or m: its intervals are left out of the totals of the summary. It starts at the " +
            "first publishing on p and at the first received record on c.");
    private final Option summary = new OptionString(Option.EMPTY_SHORT_NAME, "summary", "<file>",
        "Write the JSON summary of the run (schema " + NetTestApiSummary.SCHEMA + ") to the file after the stop.");
    private final Option metricsPort = new OptionString(Option.EMPTY_SHORT_NAME, "metrics-port", "[<host>:]<port>",
        "Serve the Prometheus metrics at http://<host>:<port>/metrics; the host defaults to " +
            NetTestApiCommandLine.DEFAULT_METRICS_HOST + ", port 0 takes a free port.");

    private Settings settings;

    @Override
    protected Option[] getOptions() {
        return new Option[] {symbols, connections, rateLimit, stat, name, collector, event, batch, reuse, role,
            duration, warmup, summary, metricsPort};
    }

    /** The usage with the line of the specification version. */
    String usage() {
        return generateHelpSummary(Help.DEFAULT_WIDTH) + System.lineSeparator() +
            "-D<key>=<value> sets a system property before the first endpoint; -Ddxscheme.nanoTime=false turns off " +
            "the nanosecond part of the event time." + System.lineSeparator() +
            "spec_version " + SPEC_VERSION;
    }

    /**
     * Parses and validates the arguments. Invalid arguments end the process with {@link #EXIT_USAGE}, since QD's
     * {@link Tools#main} would exit with 1 for every failure; {@code --help} prints the usage and exits with 0.
     */
    @Override
    public void parse(String[] args) {
        if (args.length == 0) {
            System.err.println(getToolName() + ": no mode: the first argument is p or c");
            System.err.println(usage());
            System.exit(EXIT_USAGE);
        }
        if (NetTestApiCommandLine.isHelp(args)) {
            System.out.println(usage());
            System.out.flush();
            System.exit(0);
        }
        try {
            settings = parseArguments(args);
        } catch (BadToolParametersException e) {
            System.err.println(getToolName() + ": " + e.getMessage());
            System.err.println("Use \"" + getToolName() + " --help\" for usage info.");
            System.exit(EXIT_USAGE);
        }
    }

    @Override
    protected void executeImpl(String[] ignored) {
        Stop stop = new Stop();
        installSignalHandlers(stop);
        NetTestApiSide side = settings.mode == Mode.PRODUCER ?
            new NetTestApiProducerSide(settings, stop) : new NetTestApiConsumerSide(settings, stop);
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
     * @throws BadToolParametersException for invalid arguments, with the text of the NetTest specification.
     */
    static Settings parseArguments(String... args) {
        return NetTestApiCommandLine.parse(args);
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
                    stop.requestBySignal();
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
        final Mode mode;
        final NetTestConfig config;
        final String address;
        final EventKind event;
        final String collector; // stream, ticker or null
        final DXEndpoint.Role role;
        final int batch; // p only
        final boolean reuse; // p only
        final Double rate; // p only: the total events per second, null without -r
        final long durationMillis; // 0: until Ctrl+C
        final TimePeriod statPeriod;
        final long warmupMillis; // 0 without --warmup
        final Path summaryPath; // null without --summary
        final String metricsHost;
        final int metricsPort; // -1 without --metrics-port
        final List<String> args; // the arguments after the tool name

        Settings(Mode mode, NetTestConfig config, String address, EventKind event, String collector,
            DXEndpoint.Role role, int batch, boolean reuse, Double rate, long durationMillis, TimePeriod statPeriod,
            long warmupMillis, Path summaryPath, String metricsHost, int metricsPort, List<String> args)
        {
            this.mode = mode;
            this.config = config;
            this.address = address;
            this.event = event;
            this.collector = collector;
            this.role = role;
            this.batch = batch;
            this.reuse = reuse;
            this.rate = rate;
            this.durationMillis = durationMillis;
            this.statPeriod = statPeriod;
            this.warmupMillis = warmupMillis;
            this.summaryPath = summaryPath;
            this.metricsHost = metricsHost;
            this.metricsPort = metricsPort;
            this.args = args;
        }

        boolean isProducer() {
            return mode == Mode.PRODUCER;
        }

        /** Whether the consumer receives conflated events: FEED takes Trade through the ticker contract. */
        boolean isConflated() {
            return mode == Mode.CONSUMER && role == DXEndpoint.Role.FEED && event == EventKind.TRADE;
        }

        /**
         * The settings under the names of the summary file of the specification ({@code side}, {@code name} and the
         * keys of its {@code config}), as {@code cli-vectors.json} names them; an absent value is {@code null}.
         */
        Map<String, Object> describe() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("side", isProducer() ? "producer" : "consumer");
            map.put("name", config.name);
            map.put("address", address);
            map.put("symbols", config.totalSymbols);
            map.put("symbols_source", config.ipfPath == null ? "generated" : "file");
            map.put("instances", config.instanceCount);
            map.put("rate_limit", rate == null ? null : Math.round(rate));
            map.put("batch", isProducer() ? batch : null);
            map.put("event", event.text);
            map.put("collector", collector);
            map.put("role", role.name());
            map.put("reuse", isProducer() ? reuse : null);
            // The publisher converts each event to a record: the events of the pool are republished in place.
            map.put("reuse_effective", isProducer() ? reuse : null);
            map.put("conflated", isConflated());
            map.put("period_ms", statPeriod.getTime());
            map.put("warmup_ms", warmupMillis == 0 ? null : warmupMillis);
            map.put("duration_ms", durationMillis == 0 ? null : durationMillis);
            map.put("nano_time", NetTestApiSide.isNanoTime());
            map.put("isolate_arguments", Collections.emptyList());
            return map;
        }
    }

    /** The request to end the run: set once by the duration, a signal or a failure. */
    static final class Stop {
        private final CountDownLatch requested = new CountDownLatch(1);
        private volatile boolean bySignal;

        void request() {
            requested.countDown();
        }

        /** Requests the stop for SIGINT or SIGTERM; a stop requested before keeps its cause. */
        void requestBySignal() {
            if (!isRequested()) {
                bySignal = true;
            }
            requested.countDown();
        }

        /** Whether a signal requested the stop. */
        boolean isBySignal() {
            return bySignal;
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
