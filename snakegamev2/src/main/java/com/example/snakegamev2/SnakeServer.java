package com.example.snakegamev2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SnakeServer {
    private final int port;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final Map<String, String> colors = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final List<Food> foodItems = new ArrayList<>();
    private static final int NUM_FOOD_ITEMS = 1;

    public SnakeServer(int port) {
        this.port = port;
        for (int i = 0; i < NUM_FOOD_ITEMS; i++) {
            foodItems.add(new Food());
        }
    }

    /**
     * Starts the server and listens for client connections.
     */
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to all connected clients.
     * @param message The message to broadcast.
     */
    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Updates the score for a player and broadcasts the updated scores.
     * @param playerName The name of the player.
     * @param score The new score of the player.
     */
    public synchronized void updateScore(String playerName, int score) {
        scores.put(playerName, score);
        broadcastScores();
    }

    /**
     * Broadcasts the scores of all players to all clients.
     */
    public void broadcastScores() {
        StringBuilder scoreMessage = new StringBuilder("SCORES:");
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            scoreMessage.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
        }
        broadcast(scoreMessage.toString());
    }

    /**
     * Broadcasts the positions of all food items to all clients.
     */
    public void broadcastFood() {
        StringBuilder foodMessage = new StringBuilder("FOOD:");
        for (Food food : foodItems) {
            foodMessage.append(food.getFood().getTranslateX()).append(":").append(food.getFood().getTranslateY()).append(";");
        }
        broadcast(foodMessage.toString());
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String playerName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                String[] initialData = in.readLine().split(":");
                playerName = initialData[0];
                String playerColor = initialData[1];
                synchronized (scores) {
                    scores.put(playerName, 0);
                    colors.put(playerName, playerColor);
                }
                broadcastPlayerData();
                broadcastFood(); // Broadcast food positions when a new player connects

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("MOVE:")) {
                        handleMove(message);
                    } else if (message.startsWith("SCORE:")) {
                        handleScoreUpdate(message);
                    } else if (message.startsWith("EAT:")) {
                        handleEat(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cleanup();
            }
        }

        /**
         * Handles a move message from a client and broadcasts it to all clients.
         * @param message The move message from a client.
         */
        private void handleMove(String message) {
            broadcast(message);
        }

        /**
         * Handles a score update message from a client and updates the score.
         * @param message The score update message from a client.
         */
        private void handleScoreUpdate(String message) {
            String[] parts = message.split(":");
            int score = Integer.parseInt(parts[1]);
            updateScore(playerName, score);
        }

        /**
         * Handles an eat message from a client, updates the score, grows the snake, and updates food positions.
         * @param message The eat message from a client.
         */
        private void handleEat(String message) {
            String[] parts = message.split(":");
            String playerName = parts[1];
            updateScore(playerName, scores.get(playerName) + 1);

            broadcast("GROW:" + playerName); // Broadcast grow event

            for (Food food : foodItems) {
                food.randomizePosition(); // Update food positions
            }
            broadcastFood(); // Broadcast new food positions
        }

        /**
         * Cleans up resources when a client disconnects.
         */
        private void cleanup() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            clients.remove(this);
            synchronized (scores) {
                scores.remove(playerName);
                colors.remove(playerName);
            }
            broadcastPlayerData();
        }

        /**
         * Sends a message to the client.
         * @param message The message to send.
         */
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        /**
         * Broadcasts the player data (name and color) to all clients.
         */
        private void broadcastPlayerData() {
            StringBuilder dataMessage = new StringBuilder("PLAYERS:");
            for (String player : scores.keySet()) {
                dataMessage.append(player).append("=").append(colors.get(player)).append(";");
            }
            broadcast(dataMessage.toString());
            broadcastScores();
        }
    }

    // Local test running server
//    public static void main(String[] args) {
//        int port = 12345; // default port number
//        SnakeServer server = new SnakeServer(port);
//        server.startServer();
//    }
}
