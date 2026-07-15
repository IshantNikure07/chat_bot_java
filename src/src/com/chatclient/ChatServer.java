package com.chatclient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        int port = ChatConfig.SERVER_PORT;
        System.out.println("Starting server on port " + port + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Waiting for clients...");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                    
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    threadPool.submit(handler);
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server socket error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    private static void removeClient(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("Client disconnected. Active clients: " + clients.size());
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader input;
        private PrintWriter output;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = input.readLine()) != null) {
                    System.out.println("Received from [" + socket.getPort() + "]: " + message);
                    
                    if (message.equalsIgnoreCase("exit")) {
                        break;
                    }
                    
                    // Broadcast message to all other clients
                    broadcastToOthers("Client [" + socket.getPort() + "]: " + message, this);
                    
                    // Reply to the sender acknowledging receipt (maintaining original behavior)
                    sendMessage("Server received: " + message);
                }
            } catch (IOException e) {
                // Connection error or client disconnected
            } finally {
                cleanup();
            }
        }

        private void sendMessage(String message) {
            if (output != null) {
                output.println(message);
            }
        }

        private void broadcastToOthers(String message, ClientHandler sender) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }

        private void cleanup() {
            removeClient(this);
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                // Ignore
            }
            if (output != null) {
                output.close();
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}