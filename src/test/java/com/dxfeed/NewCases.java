package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.osub.IndexedEventSubscriptionSymbol;
import com.dxfeed.event.market.Order;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.event.market.Side;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;

/**
 * Scenarios for collecting Native Image metadata with the native-image-agent.
 * How to run (JAVA_HOME must point to GraalVM 23+):
 * ```powershell
 * ./update-native-image-metadata.ps1
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
        cases.put("jmxConnectorAttributesCase", NewCases::jmxConnectorAttributesCase);
        cases.put("newOrderSourcesCase", NewCases::newOrderSourcesCase);
        cases.put("eventsSerializationCase", NewCases::eventsSerializationCase);

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

    // Reads all attributes of the connector MBeans (e.g. DisplayFilter, DisplayChannels, Role added in QDS 3.354).
    public static void jmxConnectorAttributesCase() throws Exception {
        var port = ThreadLocalRandom.current().nextInt(48658, 49150);
        System.setProperty("jmx.rmi.port", String.valueOf(port + 1));
        var tapeFile = Files.createTempFile(Path.of("."), "jmx-case", ".txt").getFileName();

        try (var pub = DXEndpoint.create(DXEndpoint.Role.PUBLISHER).connect(":" + port);
                var feed = DXEndpoint.create(DXEndpoint.Role.FEED).connect("localhost:" + port);
                var tape = DXEndpoint.create(DXEndpoint.Role.STREAM_PUBLISHER).connect("tape:" + tapeFile + "[format=text]");
                var file = DXEndpoint.create(DXEndpoint.Role.STREAM_FEED).connect("file:ConvertTapeFile.in[speed=max]")) {
            Thread.sleep(2000);

            var server = ManagementFactory.getPlatformMBeanServer();
            for (var name : server.queryNames(new ObjectName("com.devexperts.qd.qtp:*"), null)) {
                System.out.println(name);
                for (var attribute : server.getMBeanInfo(name).getAttributes()) {
                    try {
                        System.out.println("  " + attribute.getName() + " = " + server.getAttribute(name, attribute.getName()));
                    } catch (Exception e) {
                        System.out.println("  " + attribute.getName() + " !! " + e);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(tapeFile);
        }
    }

    // Publishes and receives orders with the NEO and neo sources added in QDS 3.355.
    public static void newOrderSourcesCase() throws Exception {
        var sources = List.of("NEO", "neo");
        try (var hub = DXEndpoint.create(DXEndpoint.Role.LOCAL_HUB)) {
            var received = new CountDownLatch(sources.size());
            var sub = hub.getFeed().createSubscription(Order.class);
            sub.addEventListener(events -> events.forEach(event -> {
                System.out.println("Received: " + event);
                received.countDown();
            }));
            for (var source : sources) {
                sub.addSymbols(new IndexedEventSubscriptionSymbol<>("AAPL", OrderSource.valueOf(source)));
            }

            for (var source : sources) {
                var order = new Order("AAPL");
                order.setIndex(1);
                order.setSource(OrderSource.valueOf(source));
                order.setOrderSide(Side.BUY);
                order.setPrice(100.5);
                order.setSize(10);
                hub.getPublisher().publishEvents(List.of(order));
            }
            System.out.println("All orders received: " + received.await(5, TimeUnit.SECONDS));
        }
    }

    // Serializes and deserializes all event types with Java serialization.
    public static void eventsSerializationCase() throws Exception {
        try (var endpoint = DXEndpoint.create(DXEndpoint.Role.STREAM_FEED)) {
            for (var type : endpoint.getEventTypes()) {
                try {
                    var bytes = new ByteArrayOutputStream();
                    try (var out = new ObjectOutputStream(bytes)) {
                        out.writeObject(type.getConstructor().newInstance());
                    }
                    try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                        System.out.println(type.getSimpleName() + ": " + in.readObject());
                    }
                } catch (Exception e) {
                    System.out.println(type.getSimpleName() + " !! " + e);
                }
            }
        }
    }
}