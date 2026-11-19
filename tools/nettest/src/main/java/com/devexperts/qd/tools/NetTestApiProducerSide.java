// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import com.devexperts.logging.Logging;
import com.devexperts.qd.util.RateLimiter;
import com.dxfeed.api.DXEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The producer side of {@link NetTestApi}: one {@link DXEndpoint} and one {@link NetTestApiGeneratorThread} per
 * instance.
 */
class NetTestApiProducerSide extends NetTestApiSide {
    private static final Logging log = Logging.getLogging(NetTestApiProducerSide.class);

    private static final long JOIN_TIMEOUT_MILLIS = 5_000;

    final RateLimiter rateLimiter; // shared by all generators, guarded by itself; null without -r
    private final List<NetTestApiGeneratorThread> generators = new ArrayList<>();

    NetTestApiProducerSide(NetTestApi.Settings settings, NetTestApi.Stop stop) {
        super(settings, stop);
        this.rateLimiter = settings.config.rateLimiters == null ? null : settings.config.rateLimiters.get("*");
    }

    @Override
    String statisticsLabel() {
        return NetTestApiStatistics.PRODUCER_LABEL;
    }

    /** Creates, connects and checks the endpoints, then starts the generators. Throws when an endpoint fails. */
    @Override
    public void start() {
        log.info("Publishing " + settings.event.text + " events through " + settings.role + " endpoints, batch " +
            settings.batch + (settings.reuse ? ", reused events" : ", new events"));
        for (int i = 1; i <= config.instanceCount; i++) {
            DXEndpoint endpoint = createEndpoint(i);
            connect(i, endpoint);
            NetTestApiGeneratorThread generator = createGenerator(i, endpoint);
            generators.add(generator);
            threads.add(generator);
        }
        startStatistics();
        for (NetTestApiGeneratorThread generator : generators) {
            generator.start();
        }
    }

    NetTestApiGeneratorThread createGenerator(int index, DXEndpoint endpoint) {
        return new NetTestApiGeneratorThread(index, this, qdEndpoint(endpoint), endpoint.getPublisher());
    }

    /** Records the first failure of a generator and requests the stop. */
    void fail(Throwable t) {
        fail(STATUS_ERROR, "publishing failed: " + t, t);
    }

    @Override
    void stopWork() {
        for (NetTestApiGeneratorThread generator : generators) {
            generator.interrupt();
        }
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(JOIN_TIMEOUT_MILLIS);
        for (NetTestApiGeneratorThread generator : generators) {
            long left = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            try {
                if (left > 0) {
                    generator.join(left);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (generator.isAlive()) {
                log.warn(generator.getName() + " did not stop in " + JOIN_TIMEOUT_MILLIS + " ms");
            }
        }
    }
}
