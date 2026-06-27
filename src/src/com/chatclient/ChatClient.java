package com.chatclient;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

private JTextArea chatArea;
private JTextField messageField;

private JButton sendButton;
private JButton connectButton;
private JButton disconnectButton;

private JLabel statusLabel;

private Socket socket;
private BufferedReader input;
private PrintWriter output;

public ChatClient() {
        initializeUI();
}

    // Step 1: Design UI
private void initializeUI() {

        setTitle("Simple Chat Client");
        setSize(600,500);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane =
                new JScrollPane(chatArea);

        messageField = new JTextField();

        sendButton =
                new JButton("Send");

        connectButton =
                new JButton("Connect");

        disconnectButton =
                new JButton("Disconnect");

        statusLabel =
                new JLabel("Disconnected");

        JPanel bottomPanel =
                new JPanel(new BorderLayout());

        bottomPanel.add(messageField,
                BorderLayout.CENTER);

        bottomPanel.add(sendButton,
                BorderLayout.EAST);

        JPanel topPanel = new JPanel();

        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        add(topPanel,
                BorderLayout.NORTH);

        add(scrollPane,
                BorderLayout.CENTER);

        add(bottomPanel,
                BorderLayout.SOUTH);

        add(statusLabel,
                BorderLayout.PAGE_END);

        connectButton.addActionListener(
                e -> connect()
        );

        disconnectButton.addActionListener(
                e -> disconnect()
        );

        sendButton.addActionListener(
                e -> sendMessage()
        );

        setVisible(true);
}

    // Step 3: Connection management
private void connect() {

        try {

        socket =
                new Socket("localhost",5000);

        input =
                new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()
                        ));

        output =
                new PrintWriter(
                        socket.getOutputStream(),
                        true
                );

        statusLabel.setText(
                "Connected"
        );

chatArea.append(
                "Connected to server\n"
        );

        receiveMessages();

        }

        catch(Exception e){

        JOptionPane.showMessageDialog(
                this,
                "Connection failed"
        );
        }
}

    // Step 2: Send message
private void sendMessage() {

        try {

        String message =
                messageField.getText();

        if(message.isEmpty()) {
                return;
        }

        output.println(message);

        chatArea.append(
                "You: " +
                message + "\n"
        );

        messageField.setText("");

        }

        catch(Exception e){

        JOptionPane.showMessageDialog(
                this,
                "Message sending failed"
        );
        }
}

    // Step 2: Receive messages
private void receiveMessages() {

        Thread thread = new Thread(() -> {

        try {

                String msg;

                while(
                        (msg=input.readLine())
                                != null
                ) {

                chatArea.append(
                        msg + "\n"
                );
                }

        }

        catch(Exception e){

                chatArea.append(
                        "Disconnected\n"
                );
        }

        });

        thread.start();
}

    // Step 3: Disconnect
private void disconnect() {

        try {

        if(socket!=null){

                socket.close();

                statusLabel.setText(
                        "Disconnected"
                );

                chatArea.append(
                        "Disconnected\n"
                );
        }

        }

        catch(Exception e){
        e.printStackTrace();
        }
}

public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
        new ChatClient();
        });
}
}
