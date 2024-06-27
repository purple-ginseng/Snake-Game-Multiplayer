package com.example.snakegamev2;

import javafx.application.Platform;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.layout.Pane;
import java.util.ArrayList;
import java.util.List;

public class Snake {
    private final List<Circle> body;
    private final Image snakeImage;
    private final Color bodyColor;
    private final Pane root;  // Root container for adding snake parts
    private static final double HEAD_SIZE = 20;
    private static final double PART_SIZE = 11;
    private boolean hasMoved; // Flag to track if the snake has moved

    public Snake(String playerName, Pane root, Color bodyColor) {
        this.root = root;
        this.bodyColor = bodyColor;
        body = new ArrayList<>();
        hasMoved = false; // Initialize the flag to false

        // Initialize snake image
        String imagePath = "/snake_head.png"; // Ensure this image exists in your resources
        snakeImage = new Image(imagePath);

        // Create the head initially and add to the root
        Circle head = createSnakeHead(0, 0);
        body.add(head);

        Platform.runLater(() -> {
            if (!root.getChildren().contains(head)) {
                root.getChildren().add(head);
            }
        });
    }

    /**
     * Creates the snake's head with the specified coordinates.
     * @param x The x-coordinate of the snake's head.
     * @param y The y-coordinate of the snake's head.
     * @return The created head as a Circle object.
     */
    private Circle createSnakeHead(double x, double y) {
        Circle head = new Circle(HEAD_SIZE);
        head.setTranslateX(x);
        head.setTranslateY(y);
        head.setFill(new ImagePattern(snakeImage));
        return head;
    }

    /**
     * Creates a part of the snake's body with the specified coordinates.
     * @param x The x-coordinate of the snake's body part.
     * @param y The y-coordinate of the snake's body part.
     * @return The created body part as a Circle object.
     */
    private Circle createSnakeBody(double x, double y) {
        Circle part = new Circle(PART_SIZE);
        part.setTranslateX(x);
        part.setTranslateY(y);
        part.setFill(bodyColor);
        return part;
    }

    /**
     * Gets the body of the snake.
     * @return The list of Circle objects representing the snake's body.
     */
    public List<Circle> getBody() {
        return body;
    }

    /**
     * Moves the snake towards the given mouse coordinates.
     * @param mouseX The x-coordinate of the mouse.
     * @param mouseY The y-coordinate of the mouse.
     */
    public void move(double mouseX, double mouseY) {
        if (body.isEmpty()) return;

        Circle head = body.get(0);
        double dx = mouseX - head.getTranslateX();
        double dy = mouseY - head.getTranslateY();

        double distance = Math.sqrt(dx * dx + dy * dy);
        // Adjust this value for minimum distance
        double minDistance = PART_SIZE * 0.5;

        if (distance > minDistance) {
            double speed = PART_SIZE;
            dx = speed * dx / distance;
            dy = speed * dy / distance;

            double newX = head.getTranslateX() + dx;
            double newY = head.getTranslateY() + dy;

            if (newX != head.getTranslateX() || newY != head.getTranslateY()) {
                hasMoved = true;
            }

            // Adjust this value to control the interpolation strength
            double lerpFactor = 0.8;
            double interpolatedX = head.getTranslateX() + (newX - head.getTranslateX()) * lerpFactor;
            double interpolatedY = head.getTranslateY() + (newY - head.getTranslateY()) * lerpFactor;

            // Update the position of the head immediately
            head.setTranslateX(interpolatedX);
            head.setTranslateY(interpolatedY);

            // Move each part of the body
            for (int i = body.size() - 1; i > 0; i--) {
                Circle part = body.get(i);
                Circle prevPart = body.get(i - 1);
                part.setTranslateX(prevPart.getTranslateX());
                part.setTranslateY(prevPart.getTranslateY());
            }
        }
    }

    /**
     * Grows the snake by adding a new body part at the tail.
     */
    public void grow() {
        if (!body.isEmpty()) {
            Circle tail = body.get(body.size() - 1);
            double tailX = tail.getTranslateX();
            double tailY = tail.getTranslateY();
            double newX = tailX;
            double newY = tailY;

            // Determine the direction of the tail and place the new part correctly
            if (body.size() > 1) {
                Circle secondToLast = body.get(body.size() - 2);
                double deltaX = tailX - secondToLast.getTranslateX();
                double deltaY = tailY - secondToLast.getTranslateY();
                newX = tailX - deltaX;
                newY = tailY - deltaY;
            }

            Circle newPart = createSnakeBody(newX, newY);
            body.add(newPart);

            Platform.runLater(() -> {
                if (!root.getChildren().contains(newPart)) {
                    root.getChildren().add(newPart);
                }
            });
        }
    }

    /**
     * Gets the score of the snake, which is the size of the body minus one (the head).
     * @return The score of the snake.
     */
    public int getScore() {
        return body.size() - 1; // Score is body length minus the head
    }

    /**
     * Sets the score of the snake, growing it to the desired size.
     * @param newScore The new score to set.
     */
    public void setScore(int newScore) {
        int currentBodySize = body.size() - 1;  // -1 to account for the head
        int difference = newScore - currentBodySize;

        for (int i = 0; i < difference; i++) {
            grow();
        }
    }

    /**
     * Checks if the snake has moved.
     * @return True if the snake has moved, false otherwise.
     */
    public boolean hasMoved() {
        return hasMoved;
    }

    /**
     * Resets the move flag to false.
     */
    public void resetMoveFlag() {
        hasMoved = false;
    }
}
