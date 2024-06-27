package com.example.snakegamev2;

import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;

public class Food {
    private final Rectangle food;

    /**
     * Constructs a new Food object, initializes its size and image, and sets a random position.
     */
    public Food() {
        food = new Rectangle(33, 33);
        String imagePath = "/apple.png";
        Image foodImage = new Image(imagePath);
        food.setFill(new ImagePattern(foodImage));
        randomizePosition();
    }

    /**
     * Returns the Rectangle object representing the food.
     * @return The Rectangle object for the food.
     */
    public Rectangle getFood() {
        return food;
    }

    /**
     * Sets the food to a random position within a defined area.
     * Assumes the pane width is 600 and height is 400, with offsets for positioning.
     */
    public void randomizePosition() {
        // assuming the pane width is 600, -50 for the offset
        double x = Math.random() * 550;
        // assuming the pane height is 400, -50 for the offset
        double y = Math.random() * 350;
        food.setTranslateX(x);
        food.setTranslateY(y);
    }
}
