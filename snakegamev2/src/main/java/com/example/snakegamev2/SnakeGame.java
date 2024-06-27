package com.example.snakegamev2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


import java.io.*;
import java.net.*;
import java.util.*;

public class SnakeGame extends Application {
    private Snake snake;
    private List<Food> foods;
    private Pane root;
    private Map<String, Snake> otherSnakes;
    private Map<String, Integer> scores;
    private Text scoreText;
    private PrintWriter out;
    private String serverAddress;
    private String playerName;
    private Color snakeColor;
    private static final int NUM_FOOD_ITEMS = 1;
    private double lastMouseX;
    private double lastMouseY;
    private MediaPlayer mediaPlayer;

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setSnakeColor(Color snakeColor) {
        this.snakeColor = snakeColor;
    }

    @Override
    public void start(Stage primaryStage) {
        root = new Pane();
        Scene scene = new Scene(root, 600, 400);
        MediaView mediaView = new MediaView();

        // Load background image
        Image backgroundImage = new Image(getClass().getResource("/background.jpeg").toExternalForm());
        BackgroundImage bgImage = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.REPEAT, // Repeat horizontally
                BackgroundRepeat.REPEAT, // Repeat vertically
                BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT
        );
        root.setBackground(new Background(bgImage));

        // Load background music
        try {
            URL resource = getClass().getResource("/bg.mp3");
            String musicFilePath = resource != null ? resource.toExternalForm() : new File("C:/Users/user/Documents/Java/Assignment/snakegamev2/bg.mp3").toURI().toString();
            Media bgMusic = new Media(musicFilePath);
            mediaPlayer = new MediaPlayer(bgMusic);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Error loading or playing background music: " + e.getMessage());
            e.printStackTrace();
        }

        snake = new Snake(playerName, root, snakeColor);
        foods = new ArrayList<>();
        for (int i = 0; i < NUM_FOOD_ITEMS; i++) {
            Food food = new Food();
            food.randomizePosition();
            foods.add(food);
            root.getChildren().add(food.getFood());
        }
        otherSnakes = new HashMap<>();
        scores = new HashMap<>();
        scoreText = new Text(10, 20, "Scores: ");
        scoreText.setFont(Font.font("Verdana", 20));
        root.getChildren().add(scoreText);
        root.getChildren().addAll(snake.getBody());

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, this::handleMouseMovement);

        primaryStage.setTitle("Snake Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Show popup alert
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Instructions");
        alert.setHeaderText(null);
        alert.setContentText("Control the snake by moving your cursor.");
        alert.showAndWait();

        startGameLoop();

        if (serverAddress != null) {
            connectToServer(serverAddress);
        }
    }
    
    public void setServerAddress(String address) {
        this.serverAddress = address;
    }

    /**
     * Connect to the server at the given address.
     * @param address The server address in the format host:port.
     */
    public void connectToServer(String address) {
        String[] parts = address.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        try {
            Socket socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            new Thread(new PeerHandler(socket)).start();
            out.println(playerName + ":" + colorToString(snakeColor));
            sendInitialStateToServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send the initial state of the snake to the server.
     */
    private void sendInitialStateToServer() {
        for (int i = 0; i < snake.getBody().size(); i++) {
            out.println("MOVE:" + playerName + ":" + snake.getBody().get(i).getTranslateX() + ":" + snake.getBody().get(i).getTranslateY());
        }
        out.println("SCORE:" + snake.getScore());
    }

    /**
     * Handle mouse movement events to update the snake's direction.
     * @param event The MouseEvent triggered by mouse movement.
     */
    private void handleMouseMovement(MouseEvent event) {
        lastMouseX = event.getX();
        lastMouseY = event.getY();
    }

    /**
     * Send the snake's movement to the server.
     * @param x The new x-coordinate of the snake's head.
     * @param y The new y-coordinate of the snake's head.
     */
    private void sendMoveToServer(double x, double y) {
        if (out != null) {
            out.println("MOVE:" + playerName + ":" + x + ":" + y);
        }
    }

    /**
     * Check for collisions between the snake and food items.
     * If a collision is detected, grow the snake and update the score.
     */
    private void checkFoodCollision() {
        for (Food food : foods) {
            if (snake.getBody().getFirst().getBoundsInParent().intersects(food.getFood().getBoundsInParent())) {
                snake.grow();
                updateScore(snake.getScore());
                if (out != null) {
                    out.println("EAT:" + playerName);
                }
                food.randomizePosition();
            }
        }
    }

    /**
     * Update the position of food items based on server messages.
     * @param message The message from the server containing food positions.
     */
    private void updateFood(String message) {
        String[] parts = message.split(":");
        Platform.runLater(() -> {
            int index = 0;
            for (int i = 1; i < parts.length; i += 2) {
                if (i + 1 < parts.length) {
                    double x = Double.parseDouble(parts[i]);
                    double y = Double.parseDouble(parts[i + 1].replace(';', ' '));
                    if (index < foods.size()) {
                        foods.get(index).getFood().setTranslateX(x);
                        foods.get(index).getFood().setTranslateY(y);
                    } else {
                        Food food = new Food();
                        food.getFood().setTranslateX(x);
                        food.getFood().setTranslateY(y);
                        foods.add(food);
                        root.getChildren().add(food.getFood());
                    }
                    index++;
                }
            }
        });
    }

    /**
     * Start the main game loop using AnimationTimer.
     */
    private void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                snake.move(lastMouseX, lastMouseY);
                checkFoodCollision();
                if (snake.hasMoved()) {
                    sendMoveToServer(snake.getBody().get(0).getTranslateX(), snake.getBody().get(0).getTranslateY());
                    snake.resetMoveFlag();
                }
            }
        };
        timer.start();
    }

    private class PeerHandler implements Runnable {
        private BufferedReader in;

        public PeerHandler(Socket socket) {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("MOVE:")) {
                        handleMoveMessage(message);
                    } else if (message.startsWith("SCORE:")) {
                        handleScoreUpdate(message);
                    } else if (message.startsWith("SCORES:")) {
                        String finalMessage = message;
                        Platform.runLater(() -> updateScores(finalMessage));
                    } else if (message.startsWith("FOOD:")) {
                        updateFood(message);
                    } else if (message.startsWith("GROW:")) {
                        handleGrowMessage(message);
                    } else if (message.startsWith("PLAYERS:")) {
                        updatePlayers(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Handle a "GROW" message from the server to grow the corresponding snake.
         * @param message The message containing the player's name whose snake should grow.
         */
        private void handleGrowMessage(String message) {
            String[] parts = message.split(":");
            String playerName = parts[1];

            Platform.runLater(() -> {
                Snake snake = otherSnakes.get(playerName);
                if (snake != null) {
                    snake.grow();
                } else {
                    System.out.println("Snake not found for growth: " + playerName);
                }
            });
        }

        /**
         * Handle a "MOVE" message from the server to update the corresponding snake's position.
         * @param message The message containing the player's name and new coordinates.
         */
        private void handleMoveMessage(String message) {
            String[] parts = message.split(":");
            String playerName = parts[1];
            double x = Double.parseDouble(parts[2]);
            double y = Double.parseDouble(parts[3]);

            Platform.runLater(() -> {
                Snake snake = otherSnakes.get(playerName);
                if (snake == null) {
                    snake = new Snake(playerName, root, Color.GREEN); // Default color for other players
                    otherSnakes.put(playerName, snake);
                }

                // Ensure the snake's head is rendered above other parts
                if (!snake.getBody().isEmpty()) {
                    snake.move(x, y);
                    Circle head = snake.getBody().get(0);
                    root.getChildren().remove(head);
                    root.getChildren().add(head);
                }
            });
        }

        /**
         * Handle a "SCORE" message from the server to update the corresponding snake's score.
         * @param message The message containing the player's name and new score.
         */
        private void handleScoreUpdate(String message) {
            String[] parts = message.split(":");
            String playerName = parts[1];
            int score = Integer.parseInt(parts[2]);
            Platform.runLater(() -> {
                Snake snake = otherSnakes.get(playerName);
                if (snake != null) {
                    snake.setScore(score);
                }
                scores.put(playerName, score);
                displayScores();
            });
        }

        /**
         * Handle a "PLAYERS" message from the server to update the list of players and their snakes.
         * @param message The message containing the list of players and their colors.
         */
        private void updatePlayers(String message) {
            String[] parts = message.split(":")[1].split(";");
            Platform.runLater(() -> {
                for (String part : parts) {
                    String[] playerData = part.split("=");
                    if (playerData.length == 2) {
                        String playerName = playerData[0];
                        Color color = Color.web(playerData[1]);
                        if (!otherSnakes.containsKey(playerName)) {
                            Snake newSnake = new Snake(playerName, root, color);
                            otherSnakes.put(playerName, newSnake);
                        }
                    }
                }
            });
        }
    }

    /**
     * Display the current scores of all players.
     */
    private void displayScores() {
        StringBuilder scoreDisplay = new StringBuilder("Scores: \n");
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            scoreDisplay.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        scoreText.setText(scoreDisplay.toString());
    }

    /**
     * Update the scores based on a message from the server.
     * @param message The message containing the scores of all players.
     */
    private void updateScores(String message) {
        String[] parts = message.split(":");
        String[] scoreParts = parts[1].split(";");
        for (String scorePart : scoreParts) {
            String[] scoreData = scorePart.split("=");
            if (scoreData.length == 2) {
                String playerName = scoreData[0];
                int score = Integer.parseInt(scoreData[1]);
                scores.put(playerName, score);
                Snake snake = otherSnakes.get(playerName);
                if (snake != null) {
                    snake.setScore(score);
                }
            }
        }
        displayScores();
    }

    /**
     * Update the score display for the player's snake.
     * @param score The new score of the player's snake.
     */
    private void updateScore(int score) {
        scoreText.setText("Scores: " + score);
    }

    /**
     * Convert a Color object to a string in hexadecimal format.
     * @param color The Color object to convert.
     * @return The hexadecimal string representation of the color.
     */
    private String colorToString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    // Local test Snake game working or not
//    public static void main(String[] args) {
//        launch(args);
//    }
}
