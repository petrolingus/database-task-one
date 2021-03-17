package me.petrolingus;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/table-view-test.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("Table View Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
