// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A live IPF server on a loopback port for one client: it answers the request with a chunked live stream (one profile
 * and the complete marker), then sends a heartbeat chunk every period and the profiles queued by {@link #send}. It
 * notes when the client closes the connection. All its threads are daemons.
 */
final class IpfLiveServer implements AutoCloseable {

    private static final String HEADER = "#STOCK::=TYPE,SYMBOL,DESCRIPTION\r\n";

    private final ServerSocket serverSocket;
    private final long heartbeatPeriodMillis;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final CountDownLatch clientClosed = new CountDownLatch(1);
    private volatile Socket socket;

    IpfLiveServer(final long heartbeatPeriodMillis) throws IOException {
        this.heartbeatPeriodMillis = heartbeatPeriodMillis;
        serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        final Thread thread = new Thread(this::serve, "IpfLiveServer");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * @return the address of the server for {@code InstrumentProfileConnection}, {@code host:port}
     */
    String address() {
        return "127.0.0.1:" + serverSocket.getLocalPort();
    }

    /**
     * Queues a profile with the symbol; it is sent with the next chunk.
     */
    void send(final String symbol) {
        queue.add(symbol);
    }

    /**
     * @return {@code true} if the client closed the connection within the time
     */
    boolean awaitClientClosed(final long timeoutMillis) throws InterruptedException {
        return clientClosed.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();

        final Socket s = socket;

        if (s != null) {
            s.close();
        }
    }

    private void serve() {
        try (Socket s = serverSocket.accept()) {
            socket = s;

            final InputStream in = s.getInputStream();
            final OutputStream out = s.getOutputStream();

            readRequest(in);
            startCloseWatcher(in);
            out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=UTF-8\r\nTransfer-Encoding: chunked\r\n"
                    + "X-Live: provided\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            writeChunk(out, HEADER + "STOCK,AAA,A\r\n##COMPLETE\r\n\r\n");

            while (clientClosed.getCount() > 0) {
                final String symbol = queue.poll(heartbeatPeriodMillis, TimeUnit.MILLISECONDS);

                writeChunk(out, symbol == null ? "\r\n" : HEADER + "STOCK," + symbol + "," + symbol + "\r\n##\r\n\r\n");
            }
        } catch (final IOException | InterruptedException e) {
            // The client or close() closed the connection.
        } finally {
            clientClosed.countDown();
        }
    }

    private static void readRequest(final InputStream in) throws IOException {
        int matched = 0;

        // Reads up to the blank line that ends the request headers.
        while (matched < 4) {
            final int b = in.read();

            if (b < 0) {
                throw new IOException("EOF in the request");
            }

            matched = (b == '\r' && (matched == 0 || matched == 2)) || (b == '\n' && (matched == 1 || matched == 3))
                    ? matched + 1 : 0;
        }
    }

    // The client sends nothing after the request: a read returns -1 (or fails) when it closes the connection.
    private void startCloseWatcher(final InputStream in) {
        final Thread thread = new Thread(() -> {
            try {
                while (in.read() >= 0) {
                    // Nothing is expected.
                }
            } catch (final IOException e) {
                // Closed.
            }

            clientClosed.countDown();
        }, "IpfLiveServer-close-watcher");

        thread.setDaemon(true);
        thread.start();
    }

    private static void writeChunk(final OutputStream out, final String data) throws IOException {
        final byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        out.write((Integer.toHexString(bytes.length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }
}
