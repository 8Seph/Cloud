package com.ec.client;

import com.ec.common.FilesList;
import io.netty.channel.ChannelFutureListener;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ResourceBundle;

public class MainController implements Initializable {

    protected final String FILES_PATH = "storage/client/";
    private int selectedIndex_SERVER = -1;
    private int selectedIndex_CLIENT = -1;
    private Network network = Network.getInstance();
    private ChannelFutureListener finishListener;


    @FXML
    private ListView<String> filesList_CLIENT, filesList_SERVER;

    @FXML
    private TextField IP_ADDRESS;

    @FXML
    private Label isOnline;

    @FXML
    protected TextArea logArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Настройка модели выборки для списксов файлов в интефейсе, код повторяется, но в один метод не получилось пока убрать
        filesList_SERVER.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                selectedIndex_SERVER = newValue.intValue();
            }
        });

        filesList_CLIENT.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                selectedIndex_CLIENT = newValue.intValue();
            }
        });

        finishListener = future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл успешно передан");
                Network.getInstance().stop();
            }
        };

        //Если папки на клиенте не созданы
        if (!Files.exists(Paths.get(FILES_PATH))) {
            try {
                Files.createDirectories(Paths.get(FILES_PATH));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void connect() throws InterruptedException {
        new Thread(() -> network.start()).start();
    }

    @FXML
    public void connectBtn() throws InterruptedException {
        // Network.IP_ADDRESS = IP_ADDRESS.getText();
        connect();
        refreshLocalFilesList();
    }

    @FXML
    public void delete() {
        if (filesList_CLIENT.isFocused() && selectedIndex_CLIENT != -1) {
            String fileName = filesList_CLIENT.getItems().get(selectedIndex_CLIENT);
            try {
                Files.delete(Paths.get(FILES_PATH + fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            refreshLocalFilesList();
        }
//        if (filesList_SERVER.isFocused() && selectedIndex_SERVER != -1) {
//            FileRequest fm = new FileRequest("/delete " + filesList_SERVER.getItems().get(selectedIndex_SERVER));
//            Network.sendMsg(fm);
//        }
    }

    @FXML
    public void search(ActionEvent event) throws IOException {
        FIleSendler.getServerFilesList(network.getCurrentChannel());
    }


    @FXML
    public void downloadBtn(ActionEvent actionEvent) {
//        if (selectedIndex_SERVER != -1) {
//            Network.sendMsg(new FileRequest(filesList_SERVER.getItems().get(selectedIndex_SERVER)));
//        }
    }

    @FXML
    public void sendFileBtn(ActionEvent event) throws IOException {
        FIleSendler.sendFile(Paths.get(FILES_PATH + filesList_CLIENT.getItems().get(selectedIndex_CLIENT)), network.getCurrentChannel(), finishListener);
    }


    public void getFilesListOnServer() {

    }

    public void refreshServerFilesList(FilesList fm) {
        updateUI(() -> {
            try {
                filesList_SERVER.getItems().clear();
                for (String str : fm.getFilesList()) {
                    filesList_SERVER.getItems().add(str);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                filesList_CLIENT.getItems().clear();
                Files.list(Paths.get(FILES_PATH)).map(p -> p.getFileName().toString()).forEach(o -> filesList_CLIENT.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
