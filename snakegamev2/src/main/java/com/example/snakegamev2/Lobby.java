package com.example.snakegamev2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Objects;

public class Lobby extends Application {

    private static final int SERVER_PORT = 12345; // default port number
    private String fullAddress; // To hold the IP and port
    private Button createRoomButton;
    private Button joinRoomButton;
    private TextField playerNameField;
    private ColorPicker colorPicker;

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        Image backgroundImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/raoni-dorim-mountains-day-highress-01.jpg")));
        BackgroundImage background = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
        root.setBackground(new Background(background));

        Font customFont = Font.loadFont(getClass().getResourceAsStream("/ARCADECLASSIC.TTF"), 20);

        // Load snake image
        Image snakeImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/snake1.png")));
        ImageView imageView = new ImageView(snakeImage);

        playerNameField = new TextField();
        playerNameField.setPromptText("Enter your name");

        colorPicker = new ColorPicker(Color.GREEN); // Default color

        createRoomButton = new Button("Create  Room");
        createRoomButton.setFont(customFont);
        Label roomInfoLabel = new Label();

        createRoomButton.setOnAction(event -> {
            String playerName = playerNameField.getText().trim();
            Color selectedColor = colorPicker.getValue();
            if (playerName.isEmpty()) {
                roomInfoLabel.setText("Please enter your name to create a room.");
            } else {
                createRoom(roomInfoLabel, playerName, selectedColor);
            }
        });

        TextField roomAddressField = new TextField();
        roomAddressField.setPromptText("Enter Room IP:Port");

        joinRoomButton = new Button("Join  Room");
        joinRoomButton.setFont(customFont);
        joinRoomButton.setOnAction(event -> {
            String playerName = playerNameField.getText().trim();
            Color selectedColor = colorPicker.getValue();
            if (playerName.isEmpty()) {
                roomInfoLabel.setText("Please enter your name to join a room.");
            } else {
                joinRoom(roomAddressField.getText(), playerName, selectedColor);
            }
        });
        joinRoomButton.setDisable(true); // Initially disabled

        roomAddressField.textProperty().addListener((observable, oldValue, newValue) -> {
            joinRoomButton.setDisable(newValue.trim().isEmpty());
        });

        Button copyRoomButton = new Button("Copy Room Details");
        copyRoomButton.setFont(customFont);
        copyRoomButton.setOnAction(event -> copyToClipboard(fullAddress));

        root.getChildren().addAll(imageView, playerNameField, colorPicker, createRoomButton, roomInfoLabel, roomAddressField, joinRoomButton, copyRoomButton);

        primaryStage.setTitle("Snake Game Lobby");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Creates a new game room and starts the server.
     * @param roomInfoLabel Label to display room information.
     * @param playerName The player's name.
     * @param snakeColor The color of the player's snake.
     */
    private void createRoom(Label roomInfoLabel, String playerName, Color snakeColor) {
        new Thread(() -> {
            SnakeServer server = new SnakeServer(SERVER_PORT);
            server.startServer();
            SnakeGame game = new SnakeGame();
            game.setPlayerName(playerName);
            game.setSnakeColor(snakeColor);
            Platform.runLater(() -> game.start(new Stage())); // Start the game as the host
        }).start();

        String localIP = getLocalIPAddress();
        fullAddress = localIP + ":" + SERVER_PORT;
        roomInfoLabel.setText("Room created at " + fullAddress);
        createRoomButton.setDisable(true); // Disable the button to prevent multiple rooms
    }

    /**
     * Joins an existing game room.
     * @param address The address of the room to join.
     * @param playerName The player's name.
     * @param snakeColor The color of the player's snake.
     */
    private void joinRoom(String address, String playerName, Color snakeColor) {
        SnakeGame game = new SnakeGame();
        game.setPlayerName(playerName);
        game.setSnakeColor(snakeColor);
        game.setServerAddress(address);
        Platform.runLater(() -> game.start(new Stage()));
    }

    /**
     * Copies the specified text to the system clipboard.
     * @param text The text to copy.
     */
    private void copyToClipboard(String text) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    /**
     * Gets the local IP address of the machine.
     * @return The local IP address, or a message if unable to determine the IP address.
     */
    private String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "Unable to determine local IP address";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
