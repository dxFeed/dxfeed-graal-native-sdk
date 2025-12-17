package com.dxfeed;

public class TestJmx {
    public static void main(String[] args) throws Exception {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("jmx.rmi.port", "7777");

        Class<?> clazz = Class.forName("com.devexperts.qd.monitoring.JmxRmi");
        java.lang.reflect.Method method = clazz.getDeclaredMethod("init", java.util.Properties.class);
        method.setAccessible(true);
        method.invoke(null, props);

        System.out.println("JMX initialized successfully");
    }
}
