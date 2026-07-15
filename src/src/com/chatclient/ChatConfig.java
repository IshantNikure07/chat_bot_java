package com.chatclient;

public class ChatConfig {
    public static final String SERVER_HOST = System.getProperty("chat.host", "localhost");
    public static final int SERVER_PORT = Integer.getInteger("chat.port", 5000);
    
    // Private constructor to prevent instantiation
    private ChatConfig() {}
}
