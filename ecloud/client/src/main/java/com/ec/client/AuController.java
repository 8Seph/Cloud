package com.ec.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

import java.util.Random;
import java.util.ResourceBundle;


public class AuController implements Initializable {


    @FXML
    private Label brut;

    @FXML
    private VBox mainVbox;

    @FXML
    private TextField login;

    @FXML
    private Button connect;

    @FXML
    private Button exit;

    @FXML
    private Button registration;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bruting();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        initializeWindowDragAndDropLabel();
    }

    @FXML
    void close(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    void connect() throws IOException {
        //   new Thread(() -> Network.getInstance().start(() -> changeScene())).start();
        openMainWindow();
    }

    void bruting() throws InterruptedException {

        Random random = new Random();
        int x = random.nextInt(50);
        while (true) {
            setBrut(x);
            x++;
            Thread.sleep(30);
        }
    }

    private void setBrut(int i) {
        String s = Integer.toString(i);
        updateUI(() -> {
            brut.setText(s + " files");
        });
    }

    private void changeScene() {
        if (Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        Parent mainScene = FXMLLoader.load(getClass().getResource("/main.fxml"));
                        StartClient.mainStage.setScene(new Scene(mainScene));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private void openMainWindow() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Stage filesStage = new Stage();
        Parent filesUI = fxmlLoader.load();
        filesStage.setTitle("ECloud");
        filesStage.setMinWidth(650);
        Scene scene = new Scene(filesUI);
        filesStage.setScene(scene);
        filesStage.setOnCloseRequest(e -> Network.getInstance().stop());
        filesStage.show();
        StartClient.mainStage.close();
    }


    private double dragDeltaX, dragDeltaY;

    private void initializeWindowDragAndDropLabel() {
        Platform.runLater(() -> {
            Stage stage = (Stage) mainVbox.getScene().getWindow();
            mainVbox.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    // record a delta distance for the drag and drop operation.
                    dragDeltaX = stage.getX() - mouseEvent.getScreenX();
                    dragDeltaY = stage.getY() - mouseEvent.getScreenY();
                }
            });
            mainVbox.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    stage.setX(mouseEvent.getScreenX() + dragDeltaX);
                    stage.setY(mouseEvent.getScreenY() + dragDeltaY);
                }
            });
        });
    }

}
