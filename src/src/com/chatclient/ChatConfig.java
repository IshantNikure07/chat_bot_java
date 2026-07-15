package com.chatclient;

public class ChatConfig {
    public static final String PROPERTY_HOST = "chat.host";
    public static final String PROPERTY_PORT = "chat.port";
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5000;

    public static final String SERVER_HOST = System.getProperty(PROPERTY_HOST, DEFAULT_HOST);
    public static final int SERVER_PORT = Integer.getInteger(PROPERTY_PORT, DEFAULT_PORT);
    
    // Private constructor to prevent instantiation
    private ChatConfig() {}
}
