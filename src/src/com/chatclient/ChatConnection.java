package com.chatclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

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

    private SocketChannel socketChannel;
    private Selector selector;
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
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.configureBlocking(false);
            
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_READ);
            
            isRunning = true;
            receiveThread = new Thread(this::receiveLoop, "ChatConnection-NIO-ReceiveThread");
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
        if (socketChannel != null) {
            String payload = message + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                int written = socketChannel.write(buffer);
                if (written == 0) {
                    Thread.sleep(10);
                }
            }
        }
    }

    public synchronized void disconnect() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        if (selector != null) {
            selector.wakeup();
        }
        cleanup();
        if (listener != null) {
            listener.onDisconnected();
        }
    }

    public synchronized boolean isConnected() {
        return socketChannel != null && socketChannel.isOpen() && socketChannel.isConnected();
    }

    private void receiveLoop() {
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        StringBuilder accumulator = new StringBuilder();

        try {
            while (isRunning) {
                int selected = selector.select(1000);
                if (!isRunning) {
                    break;
                }
                if (selected == 0) {
                    continue;
                }

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (key.isReadable()) {
                        buffer.clear();
                        int bytesRead = socketChannel.read(buffer);
                        if (bytesRead == -1) {
                            disconnect();
                            return;
                        }

                        buffer.flip();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        accumulator.append(new String(bytes, StandardCharsets.UTF_8));

                        int newlineIdx;
                        while ((newlineIdx = accumulator.indexOf("\n")) != -1) {
                            String line = accumulator.substring(0, newlineIdx);
                            if (line.endsWith("\r")) {
                                line = line.substring(0, line.length() - 1);
                            }
                            accumulator.delete(0, newlineIdx + 1);

                            if (listener != null) {
                                listener.onMessageReceived(line);
                            }
                        }
                    }
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

        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                // Ignore
            } finally {
                selector = null;
            }
        }
        if (socketChannel != null) {
            try {
                if (socketChannel.isOpen()) {
                    socketChannel.close();
                }
            } catch (IOException e) {
                // Ignore
            } finally {
                socketChannel = null;
            }
        }

        if (receiveThread != null && receiveThread != Thread.currentThread()) {
            receiveThread.interrupt();
            receiveThread = null;
        }
    }
}
