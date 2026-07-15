package com.chatclient;

import javax.swing.*;
import java.awt.*;

public class ChatClient extends JFrame {

    private JTextArea chatArea;
    private JTextField messageField;

    private JButton sendButton;
    private JButton connectButton;
    private JButton disconnectButton;

    private JLabel statusLabel;

    private final ChatConnection connection;

    public ChatClient() {
        this.connection = new ChatConnection(
                ChatConfig.SERVER_HOST,
                ChatConfig.SERVER_PORT,
                new ChatConnection.ChatConnectionListener() {
                    @Override
                    public void onConnected() {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Connected");
                            chatArea.append("Connected to server\n");
                        });
                    }

                    @Override
                    public void onDisconnected() {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Disconnected");
                            chatArea.append("Disconnected\n");
                        });
                    }

                    @Override
                    public void onMessageReceived(String message) {
                        SwingUtilities.invokeLater(() -> {
                            chatArea.append(message + "\n");
                        });
                    }

                    @Override
                    public void onError(String title, String message) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(ChatClient.this, message, title, JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }
        );
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Simple Chat Client");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        messageField = new JTextField();
        sendButton = new JButton("Send");
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        statusLabel = new JLabel("Disconnected");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel topPanel = new JPanel();
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.PAGE_END);

        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());
        sendButton.addActionListener(e -> sendMessage());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });

        setVisible(true);
    }

    private void connect() {
        try {
            connection.connect();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection failed");
        }
    }

    private void sendMessage() {
        try {
            String message = messageField.getText();
            if (message.isEmpty()) {
                return;
            }

            connection.sendMessage(message);
            chatArea.append("You: " + message + "\n");
            messageField.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Message sending failed");
        }
    }

    private void disconnect() {
        connection.disconnect();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient());
    }
}
