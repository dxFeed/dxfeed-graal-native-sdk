package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.event.market.Order;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * How to run:
 * ```powershell
 * .\run-with-agent.ps1 com.dxfeed.NewCases
 * .\merge-ser-json-files.ps1 C:\w3\graal\dxfeed-graal-native-sdk\src\main\resources\META-INF\native-image\serialization-config.json C:\w3\graal\dxfeed-graal-native-sdk\config\serialization-config.json .\serialization-config.json
 * .\merge-json-files.ps1 C:\w3\graal\dxfeed-graal-native-sdk\src\main\resources\META-INF\native-image\reflect-config.json C:\w3\graal\dxfeed-graal-native-sdk\config\reflect-config.json .\reflect-config.json
 * ```
 */
public class NewCases {

    @FunctionalInterface
    private interface Case {
        void run() throws Exception;
    }

    public static void main(final String[] args) {
        Map<String, Case> cases = new LinkedHashMap<>();

        cases.put("dxLinkCase", NewCases::dxLinkCase);
        cases.put("connectToLocalPublisherCase", NewCases::connectToLocalPublisherCase);
        cases.put("closeFromListenerCase", NewCases::closeFromListenerCase);

        cases.forEach(NewCases::runCase);
    }

    private static void runCase(String name, Case testCase) {
        System.out.println(" --- " + name + " --- ");
        var properties = (Properties) System.getProperties().clone();
        try {
            testCase.run();
        } catch (Exception e) {
            System.out.println(" !!! " + name + " failed: " + e);
            e.printStackTrace(System.out);
        } finally {
            System.setProperties(properties);
            System.out.println(" --- " + name + " --- ");
        }
    }

    public static void dxLinkCase() throws Exception {
        System.setProperty("dxfeed.experimental.dxlink.enable", "true");
        System.setProperty("scheme", "ext:resource:dxlink.xml");
        System.setProperty("dxscheme.fob", "true");
        System.setProperty("com.dxfeed.event.market.impl.Order.fob.suffixes", "|#GLBX");

        var address = "dxlink...";
        var token = "Z2...";

        try (DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
                .withRole(DXEndpoint.Role.FEED)
                .withProperties(System.getProperties())
                .build()) {
            dxEndpoint.connect("dxlink:wss://" + address + "/realtime[login=dxlink:" + token + "]");

            try (var sub = dxEndpoint.getFeed().createSubscription(Order.class)) {
                sub.addEventListener(events -> events.forEach(System.out::println));
                sub.addSymbols("/ES:XCME");

                Thread.sleep(7000);
            }
        }
    }

    public static void connectToLocalPublisherCase() throws Exception {
        var port = ThreadLocalRandom.current().nextInt(48658, 49150);

        try (var pub = DXEndpoint.create(DXEndpoint.Role.PUBLISHER).connect(":" + port);
                var feed = DXEndpoint.create(DXEndpoint.Role.FEED)) {

            feed.addStateChangeListener(evt -> {
                var newState = (DXEndpoint.State) evt.getNewValue();
                System.out.println("Feed state changed to: " + newState);
            });
            feed.connect("localhost:" + port);

            Thread.sleep(3000);
        }
    }

    public static void closeFromListenerCase() throws InterruptedException {
        try (var endpoint = DXEndpoint.create(DXEndpoint.Role.PUBLISHER)) {
            endpoint.addStateChangeListener(evt -> {
                var newState = (DXEndpoint.State) evt.getNewValue();
                System.out.println("Publisher state changed to: " + newState);
                if (newState == DXEndpoint.State.CONNECTED) {
                    endpoint.close();
                }
            });

            endpoint.connect(":0");
            endpoint.awaitNotConnected();

            System.out.println("Final state: " + endpoint.getState());
        }
    }
}