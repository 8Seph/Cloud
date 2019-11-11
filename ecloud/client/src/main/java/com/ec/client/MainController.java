package com.ec.client;


import com.ec.common.AbstractMessage;
import com.ec.common.FileMessage;
import com.ec.common.FileRequest;
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
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private final String FILES_PATH = "storage/client/";
    private int selectedIndex_SERVER = -1;
    private int selectedIndex_CLIENT = -1;
    private boolean isConnected = false;

    @FXML
    ListView<String> filesList_CLIENT;

    @FXML
    ListView<String> filesList_SERVER;

    @FXML
    TextField IP_ADDRESS;

    @FXML
    Label isOnline; //Временное решение

    @FXML
    private TextArea logArea; //Временный вариант, сюда будут передаваться логи с Log4j

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
        connect();
    }

    public void connect() {
        Network.start();
        Thread t = new Thread(() -> {
            try {
                getFilesListOnServer();

                updateUI(() -> {
                    isOnline.setText("true");
                });

                while (true) {
                    AbstractMessage am = Network.readObject(); // in внутри

                    // если прилетает фаил
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;

                        if (fm.getFilename().startsWith("/filesList") && fm.getFileList() != null) {
                            refreshServerFilesList(fm);

                        } else {
                            Files.write(Paths.get(FILES_PATH + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                            refreshLocalFilesList();
                        }
                    }

                    // если прилетает команда
                    if (am instanceof FileRequest) {
                        FileRequest fm = (FileRequest) am;
                        logArea.appendText("Команда от сервера получена.\n");
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                updateUI(() -> {
                    isOnline.setText("false");
                    logArea.appendText("Соединение разорвано.\n");
                    filesList_SERVER.getItems().clear();
                });
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
    }

    @FXML
    public void reconnect() {

        //Рефактор
        Network.IP_ADDRESS = IP_ADDRESS.getText();
        connect();
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
        if (filesList_SERVER.isFocused() && selectedIndex_SERVER != -1) {
            FileRequest fm = new FileRequest("/delete " + filesList_SERVER.getItems().get(selectedIndex_SERVER));
            Network.sendMsg(fm);
        }
    }


    @FXML
    public void downloadBtn(ActionEvent actionEvent) {
        if (selectedIndex_SERVER != -1) {
            Network.sendMsg(new FileRequest(filesList_SERVER.getItems().get(selectedIndex_SERVER)));
        }
    }

    @FXML
    public void sendFileBtn(ActionEvent event) throws IOException {
        if (selectedIndex_CLIENT != -1) {
            FileMessage fm = new FileMessage(Paths.get(FILES_PATH + filesList_CLIENT.getItems().get(selectedIndex_CLIENT)));
            Network.sendMsg(fm);
        }
    }


    public void getFilesListOnServer() {
        FileRequest filesList = new FileRequest("/getFilesList");
        Network.sendMsg(filesList);
    }

    public void refreshServerFilesList(FileMessage fm) {
        updateUI(() -> {
            try {
                filesList_SERVER.getItems().clear();
                for (String str : fm.getFileList()) {
                    filesList_SERVER.getItems().add(str);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        refreshLocalFilesList();
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
