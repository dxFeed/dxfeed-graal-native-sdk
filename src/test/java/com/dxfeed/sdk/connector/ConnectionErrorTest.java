// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorState;
import com.devexperts.qd.qtp.QTPWorkerThread;
import com.dxfeed.api.DXEndpoint;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;
import org.junit.After;
import org.junit.Test;

/**
 * The connection errors that real QD paths give on loopback, their attribution to the connectors and the endpoints,
 * and the capture loggers' levels.
 */
public class ConnectionErrorTest extends ConnectorTestSupport {

    private final List<ServerSocket> servers = new ArrayList<>();
    private final List<Socket> accepted = new ArrayList<>();

    @After
    public void closeServers() throws IOException {
        for (ServerSocket server : servers) {
            server.close();
        }

        synchronized (accepted) {
            for (Socket socket : accepted) {
                socket.close();
            }
        }
    }

    @Test
    public void rejectsNullAndForeignEndpoints() {
        assertThrows(IllegalArgumentException.class, () -> ConnectionErrors.getLastError((DXEndpoint) null));
        assertThrows(NullPointerException.class, () -> ConnectionErrors.getLastError((MessageConnector) null));
        // Ignored, as the SDK registers only its own endpoints.
        ConnectionErrors.watch(null);
    }

    @Test
    public void refusedConnectIsConnectFailed() throws Exception {
        final int port = findFreeLoopbackPort();
        final long before = System.currentTimeMillis();
        final DXEndpoint endpoint = watched().connect("127.0.0.1:" + port);

        final ConnectionError error = awaitError(endpoint);

        assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED, error.getCategory());
        assertEquals("java.net.ConnectException", error.getExceptionClassName());
        assertEquals("127.0.0.1:" + port, error.getAddress());
        assertFalse(error.isDropped());
        assertTrue(error.getCount() >= 1);
        assertTrue(error.getTime() >= before - 1000 && error.getTime() <= System.currentTimeMillis());
        assertNotNull(error.getMessage());

        final MessageConnector connector = MessageConnectors.getConnectors(endpoint).getFirst();
        assertNotNull(ConnectionErrors.getLastError(connector));
        assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED, ConnectionErrors.getLastError(connector).getCategory());
    }

    @Test
    public void unknownHostIsDns() throws Exception {
        final DXEndpoint endpoint = watched().connect("nonexistent.invalid:7300");
        final ConnectionError error = awaitError(endpoint);

        // Best effort: some resolvers answer every name.
        assumeTrue("the resolver resolves .invalid", "java.net.UnknownHostException".equals(
                error.getExceptionClassName()));
        assertEquals(ConnectionError.CATEGORY_DNS, error.getCategory());
        assertFalse(error.isDropped());
    }

    @Test
    public void serverCloseIsClosedByRemote() throws Exception {
        final DXEndpoint endpoint = watched().connect("127.0.0.1:" + startServer(Mode.CLOSE));
        final ConnectionError error = awaitError(endpoint);

        assertEquals(ConnectionError.CATEGORY_CLOSED_BY_REMOTE, error.getCategory());
        assertEquals("java.io.EOFException", error.getExceptionClassName());
        assertTrue(error.isDropped());
    }

    @Test
    public void serverResetIsDropped() throws Exception {
        final DXEndpoint endpoint = watched().connect("127.0.0.1:" + startServer(Mode.RESET));
        final ConnectionError error = awaitError(endpoint);
        final String recorded = error.getExceptionClassName() + ": " + error.getMessage();

        // A reset is reported to one call on the socket only. Usually the reader gets it, an I/O error; on Linux the
        // writer of QD may take it first, and the reader then reads the end of the stream.
        if (error.getCategory() == ConnectionError.CATEGORY_CLOSED_BY_REMOTE) {
            assertEquals(recorded, "java.io.EOFException", error.getExceptionClassName());
        } else {
            assertEquals(recorded, ConnectionError.CATEGORY_IO, error.getCategory());
        }

        assertTrue(error.isDropped());
    }

    @Test
    public void silentServerIsHeartbeatTimeout() throws Exception {
        final int port = startServer(Mode.HOLD);
        final DXEndpoint endpoint = watched().connect("127.0.0.1:" + port + "[heartbeatTimeout=1s]");
        final ConnectionError error = awaitError(endpoint);

        assertEquals(ConnectionError.CATEGORY_HEARTBEAT_TIMEOUT, error.getCategory());
        assertNull(error.getExceptionClassName());
        assertTrue(error.isDropped());
        assertTrue(error.getAddress(), error.getAddress().startsWith("127.0.0.1:" + port));
    }

    @Test
    public void tlsToPlainPortIsTls() throws Exception {
        // Not a QD publisher: it may close the connection on the client's hello before it writes anything, and the
        // client then reads the end of the stream instead of a TLS failure (MDAPI-528).
        final DXEndpoint endpoint = watched().connect("tls+127.0.0.1:" + startServer(Mode.PLAIN));
        final ConnectionError error = awaitError(endpoint);

        // Pinned: QD logs the handshake failure on its TLS codec logger.
        assertEquals(ConnectionError.CATEGORY_TLS, error.getCategory());
        assertEquals("javax.net.ssl.SSLException", error.getExceptionClassName());
        assertTrue(error.isDropped());
    }

    @Test
    public void multiHostAddressGivesTheWholeListAndCounts() throws Exception {
        final String address = "127.0.0.1:" + findFreeLoopbackPort() + ",127.0.0.1:" + findFreeLoopbackPort();
        final DXEndpoint endpoint = watched().connect(address);

        await("two errors", () -> {
            final ConnectionError error = ConnectionErrors.getLastError(endpoint);
            return error != null && error.getCount() >= 2;
        });

        assertEquals(address, ConnectionErrors.getLastError(endpoint).getAddress());
    }

    @Test
    public void endpointsWithTheSameConnectorNameGetTheirOwnErrors() throws Exception {
        final DXEndpoint refused = watched().connect("127.0.0.1:" + findFreeLoopbackPort());
        final DXEndpoint closed = watched().connect("127.0.0.1:" + startServer(Mode.CLOSE));

        assertEquals(MessageConnectors.getConnectors(refused).getFirst().getName(),
                MessageConnectors.getConnectors(closed).getFirst().getName());
        assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED, awaitError(refused).getCategory());
        assertEquals(ConnectionError.CATEGORY_CLOSED_BY_REMOTE, awaitError(closed).getCategory());

        // Later errors keep their endpoints too.
        Thread.sleep(1000);
        assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED, ConnectionErrors.getLastError(refused).getCategory());
        assertEquals(ConnectionError.CATEGORY_CLOSED_BY_REMOTE, ConnectionErrors.getLastError(closed).getCategory());
    }

    @Test
    public void localReconnectDisconnectAndCloseAreNoErrors() throws Exception {
        final int port = startPublisher().port();
        final DXEndpoint endpoint = watched().connect("127.0.0.1:" + port);
        final MessageConnector connector = MessageConnectors.getConnectors(endpoint).getFirst();

        await("connected", () -> connector.getState() == MessageConnectorState.CONNECTED);
        endpoint.reconnect();
        // The local close of the connection: no error, whenever QD connects again.
        Thread.sleep(500);
        endpoint.disconnect();
        endpoint.closeAndAwaitTermination();

        assertNull(ConnectionErrors.getLastError(endpoint));
        assertNull(ConnectionErrors.getLastError(connector));
    }

    @Test
    public void endpointErrorSurvivesConnectToAnotherAddressAndSuccess() throws Exception {
        final int port = findFreeLoopbackPort();
        final DXEndpoint endpoint = watched().connect("127.0.0.1:" + port);
        final MessageConnector failed = MessageConnectors.getConnectors(endpoint).getFirst();
        final ConnectionError error = awaitError(endpoint);

        final int healthyPort = startPublisher().port();
        endpoint.connect("127.0.0.1:" + healthyPort);
        final MessageConnector healthy = MessageConnectors.getConnectors(endpoint).getFirst();
        await("connected", () -> healthy.getState() == MessageConnectorState.CONNECTED);

        assertSame(error, ConnectionErrors.getLastError(endpoint));
        assertNull(ConnectionErrors.getLastError(healthy));
        assertSame(error, ConnectionErrors.getLastError(failed));

        endpoint.closeAndAwaitTermination();
        assertSame(error, ConnectionErrors.getLastError(endpoint));
    }

    @Test
    public void staleOrForeignRecordsAreNotAttributed() throws Exception {
        final DXEndpoint endpoint = watched().connect("127.0.0.1:" + findFreeLoopbackPort());
        final MessageConnector connector = MessageConnectors.getConnectors(endpoint).getFirst();
        final String ownLogger = ConnectionErrorCapture.CONNECTOR_LOGGER + "." + connector.getName();
        final long now = System.currentTimeMillis();

        assertNull(ConnectionErrors.record(connector, pending(ownLogger,
                now - ConnectionErrors.MAX_RECORD_AGE_MILLIS - 1000)));
        assertNull(ConnectionErrors.record(connector, pending(ConnectionErrorCapture.CONNECTOR_LOGGER + ".Other",
                now)));
        assertNull(ConnectionErrors.record(connector, pending("com.example.Other", now)));
        assertNotNull(ConnectionErrors.record(connector, pending(ownLogger, now)));
    }

    @Test
    public void readTimeoutBeforeAnyConnectionIsConnectFailed() throws Exception {
        final DXEndpoint endpoint = watched().connect("127.0.0.1:" + findFreeLoopbackPort());
        final MessageConnector connector = MessageConnectors.getConnectors(endpoint).getFirst();
        final String ownLogger = ConnectionErrorCapture.CONNECTOR_LOGGER + "." + connector.getName();

        assertEquals("never connected", 0, connector.getClosedConnectionCount());

        final ConnectionError error = ConnectionErrors.record(connector, new ConnectionErrorCapture.Pending(ownLogger,
                System.currentTimeMillis(), ConnectionError.CATEGORY_READ_TIMEOUT,
                "java.net.SocketTimeoutException", "Read timed out"));

        assertNotNull(error);
        assertEquals("a read timeout of the connect", ConnectionError.CATEGORY_CONNECT_FAILED, error.getCategory());
        assertFalse(error.isDropped());
        assertEquals("java.net.SocketTimeoutException", error.getExceptionClassName());
    }

    @Test
    public void capturedRecordsKeepNoExceptionAndBoundTheMessage() throws Exception {
        watched();

        final Logger logger = Logger.getLogger(ConnectionErrorCapture.CONNECTOR_LOGGER + ".Test");
        IOException exception = new IOException("x".repeat(1000));
        final WeakReference<IOException> reference = new WeakReference<>(exception);
        final LogRecord record = new LogRecord(Level.SEVERE, "test");
        record.setLoggerName(logger.getName());
        record.setThrown(exception);
        logger.log(record);
        record.setThrown(null);
        exception = null;

        for (int i = 0; i < 50 && reference.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }

        final ConnectionErrorCapture.Pending pending = ConnectionErrorCapture.take();

        assertNull("the exception is not retained", reference.get());
        assertNotNull(pending);
        assertEquals(ConnectionError.CATEGORY_IO, pending.category());
        assertEquals("java.io.IOException", pending.exceptionClassName());
        assertEquals(ConnectionErrorCapture.MAX_MESSAGE_LENGTH, pending.message().length());
        assertNull("taken once", ConnectionErrorCapture.take());
        assertFalse(ConnectionErrorCapture.bound("host:7300[user=me,password=secret]").contains("secret"));
    }

    // QD's socket thread that closes the connection after the other one notifies no listeners: the other thread takes
    // its record (MDAPI-528).
    @Test
    public void socketThreadTakesTheFreshRecordOfTheOtherThreadOfItsConnection() throws Exception {
        watched();

        final Logger logger = Logger.getLogger(ConnectionErrorCapture.TLS_LOGGER + ".SSLConnection");
        final String address = "paired-" + System.nanoTime() + ":7300";
        final long now = System.currentTimeMillis();

        ConnectionErrorCapture.take(); // A record of this thread from an earlier test.

        try (SocketThreads connection = SocketThreads.of(address); SocketThreads other = SocketThreads.of(address)) {
            other.reader.log(logger, now, "other");
            connection.reader.log(logger, now, "own");

            assertNull("not a socket thread", ConnectionErrorCapture.take());
            assertEquals("own", connection.writer.take().message());
            assertNull("taken once", connection.writer.take());
            assertEquals("the other connection to the address", "other", other.writer.take().message());

            connection.writer.log(logger, now - ConnectionErrorCapture.PAIRED_MAX_AGE_MILLIS - 1_000, "stale");
            assertNull("too old", connection.reader.take());
            assertEquals("its own at any age", "stale", connection.writer.take().message());

            // A stopped connection may leave a record behind: a new connection to the address does not take it.
            other.reader.log(logger, now, "stopped");
            try (SocketThreads next = SocketThreads.of(address)) {
                assertNull("a new connection", next.writer.take());
                assertNull("a new connection", next.reader.take());
            }
            assertEquals("stopped", other.reader.take().message());
        }

        assertEquals("h:1-Writer", ConnectionErrorCapture.pairedName("h:1-Reader"));
        assertEquals("h:1-Reader", ConnectionErrorCapture.pairedName("h:1-Writer"));
        assertNull(ConnectionErrorCapture.pairedName("h:1-Distributor"));
    }

    // QD's socket thread may log the failure after the other thread notified the listeners of the closed connection:
    // the record goes to the waiting thread's sink at once (MDAPI-528).
    @Test
    public void recordAfterTheNotificationGoesToTheOtherThreadOfItsConnection() throws Exception {
        watched();

        final Logger logger = Logger.getLogger(ConnectionErrorCapture.TLS_LOGGER + ".SSLConnection");
        final String address = "late-" + System.nanoTime() + ":7300";
        final List<ConnectionErrorCapture.Pending> late = new CopyOnWriteArrayList<>();
        final List<ConnectionErrorCapture.Pending> otherLate = new CopyOnWriteArrayList<>();

        try (SocketThreads connection = SocketThreads.of(address); SocketThreads other = SocketThreads.of(address)) {
            assertNull(connection.writer.takeOrAwait(late::add));
            assertNull(other.writer.takeOrAwait(otherLate::add));
            assertNull("not a socket thread", ConnectionErrorCapture.takeOrAwait(late::add));

            connection.reader.log(logger, System.currentTimeMillis(), "late");

            assertEquals(1, late.size());
            assertEquals("late", late.get(0).message());
            assertTrue("the other connection to the address", otherLate.isEmpty());
            assertNull("not kept as well", connection.writer.take());

            other.reader.log(logger, System.currentTimeMillis(), "other");
            assertEquals("other", otherLate.get(0).message());

            connection.reader.log(logger, System.currentTimeMillis(), "next");

            assertEquals("the wait ends with the first record", 1, late.size());
            assertEquals("next", connection.writer.take().message());

            // A record that the other thread captured before the notification is taken instead of a wait.
            connection.reader.log(logger, System.currentTimeMillis(), "before");
            assertEquals("before", connection.writer.takeOrAwait(late::add).message());
            connection.reader.log(logger, System.currentTimeMillis(), "after");
            assertEquals("no wait", 1, late.size());
            assertEquals("after", connection.writer.take().message());
        }
    }

    // Only the thread that closed its socket connection waits for the record of the other thread: QD closes both
    // threads of the connection, and the threads of the next connection are not closed (MDAPI-528).
    @Test
    public void closedWorkerThreadIsTheOneThatClosedItsConnection() throws Exception {
        assertFalse("not a worker thread", ConnectionErrorCapture.isClosedWorkerThread());

        final boolean[] open = inWorker(false, false);
        final boolean[] closed = inWorker(true, false);
        final boolean[] interrupted = inWorker(false, true);

        assertFalse("open", open[0]);
        assertFalse("open, not interrupted", open[1]);
        assertTrue("closed", closed[0]);
        assertTrue("closed, still interrupted", closed[1]);
        assertFalse("interrupted, not closed", interrupted[0]);
        assertTrue("interrupted, still interrupted", interrupted[1]);
    }

    // Runs isClosedWorkerThread() on a QD worker thread, closed or interrupted first.
    // @return whether it is closed and whether the thread is interrupted after the call
    private static boolean[] inWorker(final boolean close, final boolean interrupt) throws InterruptedException {
        final boolean[] result = new boolean[2];
        final QTPWorkerThread worker = new QTPWorkerThread("worker-Writer") {
            @Override
            protected void doWork() {
                if (close) {
                    close();
                }

                if (interrupt) {
                    interrupt();
                }

                result[0] = ConnectionErrorCapture.isClosedWorkerThread();
                result[1] = isInterrupted();
            }

            @Override
            protected void handleShutdown() {
            }

            @Override
            protected void handleClose(final Throwable reason) {
            }
        };

        worker.start();
        worker.join();
        return result;
    }

    @Test
    public void captureNeedsNoUserLevelAndUserLevelWins() throws Exception {
        watched();

        final Logger root = Logger.getLogger("");
        final Logger connectorLogger = Logger.getLogger(ConnectionErrorCapture.CONNECTOR_LOGGER);
        final Logger heartbeatLogger = Logger.getLogger(ConnectionErrorCapture.HEARTBEAT_LOGGER);
        final Level rootLevel = root.getLevel();

        try {
            root.setLevel(Level.OFF);
            ConnectionErrors.loggingLevelsChanged();

            assertTrue(connectorLogger.isLoggable(Level.SEVERE));
            assertTrue(heartbeatLogger.isLoggable(Level.INFO));
            assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED,
                    awaitError(watched().connect("127.0.0.1:" + findFreeLoopbackPort())).getCategory());

            // The SDK's levels go when the user's levels let the records through.
            root.setLevel(Level.INFO);
            ConnectionErrors.loggingLevelsChanged();

            assertNull(connectorLogger.getLevel());
            assertNull(heartbeatLogger.getLevel());

            // A level the user sets on a capture logger wins: no error, no exception.
            connectorLogger.setLevel(Level.OFF);
            ConnectionErrors.loggingLevelsChanged();

            assertEquals(Level.OFF, connectorLogger.getLevel());

            final DXEndpoint silenced = watched().connect("127.0.0.1:" + findFreeLoopbackPort());
            Thread.sleep(1500);
            assertNull(ConnectionErrors.getLastError(silenced));
        } finally {
            connectorLogger.setLevel(null);
            root.setLevel(rootLevel);
            ConnectionErrors.loggingLevelsChanged();
        }
    }

    private DXEndpoint watched() {
        final DXEndpoint endpoint = create(DXEndpoint.Role.FEED);
        ConnectionErrors.watch(endpoint);
        return endpoint;
    }

    private static ConnectionError awaitError(final DXEndpoint endpoint) throws InterruptedException {
        await("a connection error", () -> ConnectionErrors.getLastError(endpoint) != null);
        return ConnectionErrors.getLastError(endpoint);
    }

    // The reader and the writer of a socket connection, created one after the other as QD creates them, so that the id
    // of the writer follows the id of the reader.
    private static final class SocketThreads implements AutoCloseable {
        final SocketThread reader;
        final SocketThread writer;

        private SocketThreads(final SocketThread reader, final SocketThread writer) {
            this.reader = reader;
            this.writer = writer;
            reader.thread.start();
            writer.thread.start();
        }

        static SocketThreads of(final String address) {
            // Another thread of the JVM may be created between them; then the pair is created anew.
            for (int attempt = 0; attempt < 100; attempt++) {
                final SocketThread reader = new SocketThread(address + ConnectionErrorCapture.READER_SUFFIX);
                final SocketThread writer = new SocketThread(address + ConnectionErrorCapture.WRITER_SUFFIX);

                if (writer.thread.threadId() == reader.thread.threadId() + 1) {
                    return new SocketThreads(reader, writer);
                }
            }

            throw new AssertionError("no adjacent thread ids");
        }

        @Override
        public void close() {
            reader.thread.interrupt();
            writer.thread.interrupt();
        }
    }

    // A thread of this name that runs the actions given to it. The records are kept by weak references to their
    // threads, so the test keeps the thread.
    private static final class SocketThread {
        private final BlockingQueue<FutureTask<?>> actions = new LinkedBlockingQueue<>();
        final Thread thread;

        SocketThread(final String name) {
            thread = new Thread(this::run, name);
            thread.setDaemon(true);
        }

        private void run() {
            try {
                while (true) {
                    actions.take().run();
                }
            } catch (final InterruptedException e) {
                // Closed.
            }
        }

        private <T> T call(final Callable<T> action) throws Exception {
            final FutureTask<T> task = new FutureTask<>(action);

            actions.put(task);
            return task.get(10, TimeUnit.SECONDS);
        }

        // Logs a TLS failure.
        void log(final Logger logger, final long time, final String message) throws Exception {
            call(() -> {
                final LogRecord record = new LogRecord(Level.SEVERE, "Failed to unwrap");
                record.setLoggerName(logger.getName());
                record.setInstant(Instant.ofEpochMilli(time));
                record.setThrown(new SSLException(message));
                logger.log(record);
                return null;
            });
        }

        ConnectionErrorCapture.Pending take() throws Exception {
            return call(ConnectionErrorCapture::take);
        }

        ConnectionErrorCapture.Pending takeOrAwait(final Consumer<ConnectionErrorCapture.Pending> sink)
                throws Exception {
            return call(() -> ConnectionErrorCapture.takeOrAwait(sink));
        }
    }

    private static ConnectionErrorCapture.Pending pending(final String loggerName, final long time) {
        return new ConnectionErrorCapture.Pending(loggerName, time, ConnectionError.CATEGORY_IO,
                "java.io.IOException", "test");
    }

    private enum Mode {
        CLOSE, RESET, HOLD, PLAIN
    }

    // A server on loopback that accepts connections and ends them, resets them, keeps them silent or writes plain text
    // to them.
    private int startServer(final Mode mode) throws IOException {
        final ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        servers.add(server);

        final Thread thread = new Thread(() -> {
            try {
                while (true) {
                    final Socket socket = server.accept();

                    switch (mode) {
                        // A graceful close the client reads as the end of the stream; the socket stays open, so
                        // that a write of the client does not get a reset instead.
                        case CLOSE -> {
                            socket.shutdownOutput();
                            keep(socket);
                        }
                        // A reset after the first bytes of the client: its writer is then idle, so the reset usually
                        // reaches its reader. The socket is kept, so that the tear-down closes it if the read fails.
                        case RESET -> {
                            keep(socket);
                            socket.getInputStream().read();
                            socket.setSoLinger(true, 0);
                            socket.close();
                        }
                        case HOLD -> keep(socket);
                        // Plain text at once, and no close: the client reads no TLS record, never the end of the
                        // stream.
                        case PLAIN -> {
                            keep(socket);
                            socket.getOutputStream().write("plain text\r\n".getBytes(StandardCharsets.US_ASCII));
                            socket.getOutputStream().flush();
                        }
                    }
                }
            } catch (IOException e) {
                // The server is closed.
            }
        }, "ConnectionErrorTest-server");
        thread.setDaemon(true);
        thread.start();

        return server.getLocalPort();
    }

    private void keep(final Socket socket) {
        synchronized (accepted) {
            accepted.add(socket);
        }
    }
}
