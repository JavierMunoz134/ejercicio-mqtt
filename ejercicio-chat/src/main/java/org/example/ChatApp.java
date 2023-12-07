package org.example;

import org.eclipse.paho.client.mqttv3.*;

import java.io.*;
import java.util.Scanner;

public class ChatApp {
    private static final String BROKER_URL = "tcp://192.168.1.120:1883";

    private static final String TOPIC_ALL = "/chat/todos";
    private static final String TOPIC_FORMAT = "/chat/%s/%s";

    private MqttClient mqttClient;

    public ChatApp() {
    }

    public void start() {
        try {
            mqttClient = new MqttClient(BROKER_URL, MqttClient.generateClientId());
            mqttClient.connect();

            subscribeToGeneralTopic();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Ingrese un comando (send [destinatario] [mensaje] / chat [destinatario]):");
                String command = scanner.nextLine();

                if (command.startsWith("send")) {
                    handleSendMessage(command);
                } else if (command.startsWith("chat")) {
                    handleViewChat(command);
                }
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeToGeneralTopic() throws MqttException {
        mqttClient.subscribe(TOPIC_ALL, (topic, msg) -> handleMessage(topic, msg));
    }

    private void handleMessage(String topic, MqttMessage message) {
        String chatFileName = topic.replace("/", "_").substring(1) + ".txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFileName, true))) {
            String messageContent = new String(message.getPayload());
            writer.println(messageContent);
            System.out.println("Mensaje recibido en " + topic + ": " + messageContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSendMessage(String command) throws MqttException {
        String[] parts = command.split(" ");
        if (parts.length >= 4) {
            String destination = parts[1];
            String messageContent = command.substring(command.indexOf(parts[2]));
            String topic;

            if (destination.equals("todos")) {
                topic = TOPIC_ALL;
            } else {
                topic = String.format(TOPIC_FORMAT, parts[1], parts[2]);
            }

            MqttMessage message = new MqttMessage(messageContent.getBytes());
            mqttClient.publish(topic, message);
        } else {
            System.out.println("Comando inválido. Use el formato: send [destinatario] [mensaje]");
        }
    }

    private void handleViewChat(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            String chatFileName = parts[1] + ".txt";
            try {
                BufferedReader reader = new BufferedReader(new FileReader(chatFileName));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                reader.close();
            } catch (IOException e) {
                System.out.println("El chat no existe o está vacío.");
            }
        } else {
            System.out.println("Comando inválido. Use el formato: chat [destinatario]");
        }
    }

    public static void main(String[] args) {
        ChatApp chatApp = new ChatApp();
        chatApp.start();
    }
}
