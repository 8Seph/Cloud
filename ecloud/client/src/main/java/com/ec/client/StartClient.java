package com.ec.client;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class StartClient extends Application {


    protected static Stage mainStage;
    double xOffset;
    double yOffset;

    @Override
    public void start(Stage primaryStage) throws Exception {
        //   FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/AuUI.fxml"));

        Parent root = fxmlLoader.load();
        primaryStage.setTitle("ECloud");
        primaryStage.setMinWidth(400);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // Отключить стили ОС
        primaryStage.initStyle(StageStyle.UNDECORATED);

        primaryStage.show();
        mainStage = primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
