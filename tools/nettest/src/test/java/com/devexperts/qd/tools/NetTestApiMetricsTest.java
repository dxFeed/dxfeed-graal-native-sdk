// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.Test;

/** The methods of the metrics endpoint other than GET. */
public class NetTestApiMetricsTest {
    @Test
    public void headHasTheHeadersWithoutABodyAndOtherMethodsAreNotAllowed() throws Exception {
        NetTestApiProducerSide side = new NetTestApiProducerSide(NetTestApi.parseArguments("p", ":0", "-S", "10"),
            new NetTestApi.Stop());
        NetTestApiMetrics metrics = NetTestApiMetrics.start("127.0.0.1", 0, side);
        try {
            HttpURLConnection head = open(metrics.url(), "HEAD");
            try {
                assertEquals(200, head.getResponseCode());
                assertEquals(NetTestApiMetrics.CONTENT_TYPE, head.getContentType());
                try (InputStream in = head.getInputStream()) {
                    assertEquals(0, in.readAllBytes().length);
                }
            } finally {
                head.disconnect();
            }
            HttpURLConnection post = open(metrics.url(), "POST");
            try {
                assertEquals(405, post.getResponseCode());
            } finally {
                post.disconnect();
            }
        } finally {
            metrics.stop();
        }
    }

    private static HttpURLConnection open(String url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(5_000);
        return connection;
    }
}
