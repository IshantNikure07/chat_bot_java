package com.chatclient;

import java.io.*;
import java.net.*;

public class ChatServer {

    private static final int PORT = 5000;

    public static void main(String[] args)
    {
        try (
                ServerSocket serverSocket =
                        new ServerSocket(PORT)
        ) 
        
        {
            System.out.println("Server started...");
            System.out.println("Waiting for client...");

            Socket socket = serverSocket.accept();

            System.out.println("Client connected!");

            BufferedReader input =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            ));

            PrintWriter output =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            String message;

            while ((message = input.readLine()) != null) {

                System.out.println("Client: " + message);

                output.println("Server received: " + message);

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
            }

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}