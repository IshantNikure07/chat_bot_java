package com.chatclient;

import java.io.*;
import java.net.*;

public class ChatConnection {

    public interface ChatConnectionListener {
        void onConnected();
        void onDisconnected();
        void onMessageReceived(String message);
        void onError(String title, String message);
    }

    private final String host;
    private final int port;
    private final ChatConnectionListener listener;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread receiveThread;
    private volatile boolean isRunning = false;

    public ChatConnection(String host, int port, ChatConnectionListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public synchronized void connect() throws Exception {
        if (isConnected()) {
            return;
        }

        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            
            isRunning = true;
            receiveThread = new Thread(this::receiveLoop, "ChatConnection-ReceiveThread");
            receiveThread.start();

            if (listener != null) {
                listener.onConnected();
            }
        } catch (Exception e) {
            cleanup();
            throw e;
        }
    }

    public synchronized void sendMessage(String message) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to the server.");
        }
        if (output != null) {
            output.println(message);
            if (output.checkError()) {
                throw new IOException("Failed to send message.");
            }
        }
    }

    public synchronized void disconnect() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        cleanup();
        if (listener != null) {
            listener.onDisconnected();
        }
    }

    public synchronized boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    private void receiveLoop() {
        try {
            String message;
            while (isRunning && (message = input.readLine()) != null) {
                if (listener != null) {
                    listener.onMessageReceived(message);
                }
            }
        } catch (Exception e) {
            if (isRunning) {
                if (listener != null) {
                    listener.onError("Connection Error", "Lost connection to the server.");
                }
            }
        } finally {
            if (isRunning) {
                disconnect();
            } else {
                cleanup();
            }
        }
    }

    private synchronized void cleanup() {
        isRunning = false;
        
        if (input != null) {
            try {
                input.close();
            } catch (Exception e) {
                // Ignore
            } finally {
                input = null;
            }
        }
        if (output != null) {
            try {
                output.close();
            } catch (Exception e) {
                // Ignore
            } finally {
                output = null;
            }
        }
        if (socket != null) {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                // Ignore
            } finally {
                socket = null;
            }
        }
        
        if (receiveThread != null && receiveThread != Thread.currentThread()) {
            receiveThread.interrupt();
            receiveThread = null;
        }
    }
}
