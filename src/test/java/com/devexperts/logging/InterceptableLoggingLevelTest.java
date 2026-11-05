// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.sdk.connector.ConnectionError;
import com.dxfeed.sdk.connector.ConnectionErrors;
import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@code log.level}, {@code err.level} and the functions that change them set the level of the JUL root logger, so
 * that the records below it are not created and the records of a more verbose level are; the SDK's connection error
 * capture keeps working when the user silences the output.
 */
public class InterceptableLoggingLevelTest {

    private static final String CONNECTOR_LOGGER = "com.devexperts.qd.qtp.MessageConnector";
    private static final String HEARTBEAT_LOGGER = "com.devexperts.qd.qtp.MessageAdapterConnection";

    private final Logger root = Logger.getLogger("");
    private Handler[] rootHandlers;
    private Level rootLevel;
    private InterceptableLoggingListener listener;
    private boolean configured;
    private File errFile;

    @Before
    public void saveLogging() {
        synchronized (InterceptableLogging.lock) {
            rootHandlers = root.getHandlers();
            rootLevel = root.getLevel();
            listener = InterceptableLogging.listener_;
            configured = InterceptableLogging.configured_;
        }
    }

    @After
    public void restoreLogging() {
        synchronized (InterceptableLogging.lock) {
            for (Handler handler : root.getHandlers()) {
                root.removeHandler(handler);

                if (!List.of(rootHandlers).contains(handler)) {
                    handler.close();
                }
            }

            for (Handler handler : rootHandlers) {
                root.addHandler(handler);
            }

            root.setLevel(rootLevel);
            InterceptableLogging.listener_ = listener;
            InterceptableLogging.configured_ = configured;
        }

        System.clearProperty(InterceptableLogging.LOG_LEVEL);
        System.clearProperty(InterceptableLogging.ERR_LEVEL);
        System.clearProperty(Logging.ERR_FILE_PROPERTY);
        Logger.getLogger(CONNECTOR_LOGGER).setLevel(null);
        ConnectionErrors.loggingLevelsChanged();

        if (errFile != null) {
            errFile.delete();
        }
    }

    @Test
    public void defaultLevelIsInfo() {
        configure();

        assertEquals(Level.INFO, root.getLevel());
        assertEquals(Level.INFO, root.getHandlers()[0].getLevel());
    }

    @Test
    public void verboseLogLevelCreatesTheRecordsBelowInfo() {
        System.setProperty(InterceptableLogging.LOG_LEVEL, "DEBUG");
        configure();

        assertEquals(Level.FINE, root.getLevel());
        assertTrue(Logger.getLogger("com.devexperts.qd.Test").isLoggable(Level.FINE));
        assertFalse(Logger.getLogger("com.devexperts.qd.Test").isLoggable(Level.FINER));
    }

    @Test
    public void quietLogLevelCreatesNoInfoRecords() {
        System.setProperty(InterceptableLogging.LOG_LEVEL, "WARN");
        configure();

        assertEquals(Level.WARNING, root.getLevel());
        assertFalse(Logger.getLogger("com.devexperts.qd.Test").isLoggable(Level.INFO));
    }

    @Test
    public void setLogLevelSetsTheRootLevel() {
        configure();

        InterceptableLogging.setLogLevel(Level.FINEST);
        assertEquals(Level.FINEST, root.getLevel());
        assertTrue(Logger.getLogger("com.devexperts.qd.Test").isLoggable(Level.FINEST));

        InterceptableLogging.setLogLevel(Level.OFF);
        assertEquals(Level.OFF, root.getLevel());
    }

    @Test
    public void moreVerboseErrLevelSetsTheRootLevel() throws Exception {
        errFile = File.createTempFile("InterceptableLoggingLevelTest", ".log");
        System.setProperty(Logging.ERR_FILE_PROPERTY, errFile.getPath());
        System.setProperty(InterceptableLogging.LOG_LEVEL, "WARN");
        System.setProperty(InterceptableLogging.ERR_LEVEL, "DEBUG");
        configure();

        assertEquals(2, root.getHandlers().length);
        assertEquals(Level.FINE, root.getLevel());
        assertEquals(Level.WARNING, root.getHandlers()[0].getLevel());

        InterceptableLogging.setErrLevel(Level.SEVERE);
        assertEquals(Level.WARNING, root.getLevel());
    }

    @Test
    public void otherLoggingImplementationsKeepTheRootLevel() {
        // QD's own logging configured the handlers: their levels are not the user's.
        InterceptableLogging.configured_ = false;
        root.setLevel(Level.INFO);

        InterceptableLogging.setLogLevel(Level.ALL);

        assertEquals(Level.INFO, root.getLevel());
    }

    @Test
    public void silencedListenerGetsNothingAndErrorsAreCaptured() throws Exception {
        System.setProperty(InterceptableLogging.LOG_LEVEL, "OFF");
        final List<String> records = new CopyOnWriteArrayList<>();
        InterceptableLogging.listener_ = null;
        InterceptableLogging.setListener((level, timestamp, threadName, threadId, loggerName, message, exception,
                formattedMessage) -> records.add(loggerName + ": " + message));

        assertEquals(Level.OFF, root.getLevel());

        final DXEndpoint endpoint = DXEndpoint.newBuilder().withRole(DXEndpoint.Role.FEED).build();

        try {
            ConnectionErrors.watch(endpoint);

            assertTrue(Logger.getLogger(CONNECTOR_LOGGER).isLoggable(Level.SEVERE));
            assertTrue(Logger.getLogger(HEARTBEAT_LOGGER).isLoggable(Level.INFO));

            endpoint.connect("127.0.0.1:" + findFreeLoopbackPort());

            final long deadline = System.currentTimeMillis() + 20_000;

            while (ConnectionErrors.getLastError(endpoint) == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }

            final ConnectionError error = ConnectionErrors.getLastError(endpoint);

            assertNotNull(error);
            assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED, error.getCategory());
            assertEquals(List.of(), records);

            // Verbose again: the SDK's level on the capture logger goes, so that its INFO records come back.
            InterceptableLogging.setLogLevel(Level.INFO);
            assertNull(Logger.getLogger(CONNECTOR_LOGGER).getLevel());
        } finally {
            endpoint.closeAndAwaitTermination();
        }
    }

    @Test
    public void userLevelOnCaptureLoggerWins() {
        configure();
        Logger.getLogger(CONNECTOR_LOGGER).setLevel(Level.OFF);

        InterceptableLogging.setLogLevel(Level.OFF);

        assertEquals(Level.OFF, Logger.getLogger(CONNECTOR_LOGGER).getLevel());
    }

    private static void configure() {
        new InterceptableLogging().configureLogFile(null);
    }

    private static int findFreeLoopbackPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
