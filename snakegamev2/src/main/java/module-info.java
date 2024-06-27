module com.example.demomusic {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
 
    requires javafx.media;

    opens com.example.snakegamev2 to javafx.fxml;
    exports com.example.snakegamev2;
}