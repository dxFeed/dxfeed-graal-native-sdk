package com.dxfeed;

import com.devexperts.connector.proto.ApplicationConnectionFactory;
import com.devexperts.connector.proto.ConfigurationKey;
import com.devexperts.qd.QDContract;
import com.devexperts.qd.QDFilter;
import com.devexperts.qd.qtp.DynamicChannelShaper;
import com.devexperts.qd.qtp.file.FileReaderParams;
import com.devexperts.qd.qtp.file.FileWriterParams;
import com.devexperts.qd.util.QDConfig;
import com.devexperts.util.ConfigUtil;
import com.devexperts.util.TimePeriod;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.impl.DXEndpointImpl;
import com.dxfeed.api.osub.IndexedEventSubscriptionSymbol;
import com.dxfeed.event.market.Order;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.event.market.Side;
import com.dxfeed.ipf.filter.IPFSymbolFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * ./scripts/native-image-metadata/update-native-image-metadata.ps1
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
        cases.put("connectorPropertiesCase", NewCases::connectorPropertiesCase);

        // -Dcases=name1,name2 runs only the given cases
        var selected = System.getProperty("cases");
        if (selected != null) {
            cases.keySet().retainAll(List.of(selected.split(",")));
        }
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

    /*
     * QD sets the properties of connectors, codecs, file parameters, etc. by reflection: from the address
     * ("host:port[bindAddr=...]", "ssl[...]+host:port", "file:name[speed=max]") and from the system properties
     * (e.g. "com.devexperts.qd.qtp.socket.ClientSocketConnector.restoreTime"). The case sets every property
     * of these objects the way QD does it, so that the agent records all the setters, not only the used ones.
     */
    public static void connectorPropertiesCase() throws Exception {
        System.setProperty("dxfeed.experimental.dxlink.enable", "true");
        var tapeFile = Files.createTempFile(Path.of("."), "properties-case", ".txt").getFileName();
        var addresses = new LinkedHashMap<String, List<DXEndpoint.Role>>();
        // The message adapter factories (configured like codecs) depend on the role.
        addresses.put("localhost:1", List.of(DXEndpoint.Role.values()));
        for (var address : List.of(":0", "nio::0", "file:ConvertTapeFile.in", "tape:" + tapeFile,
                "dxlink:wss://localhost:1/realtime", "ondemand:localhost:1",
                "ssl+localhost:1", "tls+localhost:1", "shaped+localhost:1", "delayed+localhost:1")) {
            addresses.put(address, List.of(DXEndpoint.Role.FEED));
        }
        try {
            addresses.forEach((address, roles) -> roles.forEach(role -> {
                try (var endpoint = DXEndpoint.create(role)) {
                    var qdEndpoint = ((DXEndpointImpl) endpoint).getQDEndpoint();
                    qdEndpoint.initializeConnectorsForAddress(address); // creates the connectors without starting them
                    for (var connector : qdEndpoint.getConnectors()) {
                        System.out.println(address + " (" + role + "): " + connector.getClass().getName()
                                + ", " + connector.getFactory().getClass().getName());
                        setAllProperties(connector);
                        setAllDefaultProperties(connector);
                        setAllConfiguration(connector.getFactory());
                    }
                } catch (Exception e) {
                    System.out.println(address + " (" + role + ") !! " + e);
                }
            }));
            setAllProperties(new FileReaderParams.Default());
            setAllProperties(new FileWriterParams.Default());
            setAllProperties(new IPFSymbolFilter.Config());
            setAllProperties(new DynamicChannelShaper(QDContract.TICKER, Runnable::run, QDFilter.ANYTHING));
        } finally {
            Files.deleteIfExists(tapeFile);
        }
    }

    // QDConfig.setProperties is what QD calls for "[name=value, ...]".
    private static void setAllProperties(Object bean) {
        for (var property : QDConfig.getProperties(bean.getClass())) {
            var kv = property.getName() + "=" + sampleValue(property.getPropertyType(), getValue(bean, property));
            try {
                QDConfig.setProperties(bean, List.of(kv));
            } catch (Exception e) {
                printFailure(bean.getClass().getSimpleName() + "." + kv, e);
            }
        }
    }

    private static void printFailure(String what, Exception e) {
        // A setter that rejects the sample value is still reached (and recorded), but properties of unsupported
        // types (TrustManager, QDStats, etc.) cannot be set from strings at all, so they are not reported.
        if (!String.valueOf(e.getMessage()).startsWith("Unsupported property type")) {
            System.out.println("  " + what + " !! " + e);
        }
    }

    // QDConfig.setDefaultProperties is what the connectors call in their constructors for "<class name>.<property>".
    private static void setAllDefaultProperties(Object bean) {
        var prefix = bean.getClass().getName();
        for (var mbean : mbeanInterfaces(bean.getClass())) {
            for (var property : QDConfig.getProperties(mbean)) {
                var value = sampleValue(property.getPropertyType(), getValue(bean, property));
                if (!value.isEmpty()) {
                    System.setProperty(prefix + "." + property.getName(), value);
                }
            }
            try {
                QDConfig.setDefaultProperties(bean, mbean, prefix);
            } catch (Exception e) {
                printFailure(mbean.getSimpleName() + " defaults", e);
            }
        }
    }

    // ConfigurableObject.setConfiguration is what QD calls for "codec[name=value, ...]+".
    private static void setAllConfiguration(ApplicationConnectionFactory factory) {
        for (var key : factory.supportedConfiguration()) {
            var value = sampleValue(key.getType(), factory.getConfiguration(key));
            try {
                factory.setConfiguration(ConfigurationKey.create(key.getName(), String.class), value);
            } catch (Exception e) {
                printFailure(factory.getClass().getSimpleName() + "." + key.getName() + "=" + value, e);
            }
        }
    }

    private static List<Class<?>> mbeanInterfaces(Class<?> type) {
        var result = new ArrayList<Class<?>>();
        for (var c = type; c != null; c = c.getSuperclass()) {
            for (var intf : c.getInterfaces()) {
                if (intf.getSimpleName().endsWith("MBean") && !result.contains(intf)) {
                    result.add(intf);
                }
            }
        }
        return result;
    }

    private static Object getValue(Object bean, QDConfig.Property property) {
        try {
            return property.getGetterMethod() == null ? null : property.getGetterMethod().invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }

    // Returns the first value that QD can convert to the property type, so that the setter is reached
    // (the current value is preferred to keep the object valid).
    private static String sampleValue(Class<?> type, Object current) {
        var candidates = new ArrayList<String>();
        if (current instanceof String || current instanceof Number || current instanceof Boolean
                || current instanceof Enum || current instanceof TimePeriod) {
            candidates.add(current.toString());
        }
        if (type.isEnum()) {
            candidates.add(type.getEnumConstants()[0].toString());
        }
        candidates.addAll(List.of("", "0", "false", "1s", "2020-01-01", "none", "text", "all"));
        for (var candidate : candidates) {
            try {
                ConfigUtil.convertStringToObject(type, candidate);
                return candidate;
            } catch (Exception ignored) {
                // try the next one
            }
        }
        System.out.println("  no convertible value for " + type.getName());
        return "";
    }
}