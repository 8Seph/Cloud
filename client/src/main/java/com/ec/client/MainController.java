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
    Label isOnline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Настройка индекса для списка файлов на сервере
        filesList_SERVER.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                selectedIndex_SERVER = newValue.intValue();
            }
        });

        // Настройка индексов для списка файлов на клиенте,
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
        //если делаем в потоке JavaFX, то интерфейс не прогрузится, отдельный поток для прослушивания сообщений с сервера.
        Thread t = new Thread(() -> {
            try {
                getFilesListOnServer();

                updateUI(() -> {
                    isOnline.setText("true");
                });

                while (true) {
                    AbstractMessage am = Network.readObject(); //  in внутри

                    // если к нам прилетает фаил
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;

                        //если приходит служебный фаил
                        if (fm.getFilename().startsWith("/") && fm.getFileList() != null) {
                            refreshServerFilesList(fm);

                        } else {
                            // читаем и сохраняем фаил на клиент, затем обновляем файловый лист в интерфейсе
                            Files.write(Paths.get(FILES_PATH + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                            refreshLocalFilesList();
                        }
                    }

                    // если прилетает команда
                    if (am instanceof FileRequest) {
                        FileRequest fm = (FileRequest) am;
                        System.out.println("Команда от сервера получена");
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                updateUI(() -> {
                    isOnline.setText("false");
                    filesList_SERVER.getItems().clear();
                });
                e.printStackTrace();
            } finally {
                //   Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
    }

    @FXML
    public void reconnect() {
        connect();
        Network.IP_ADDRESS = IP_ADDRESS.getText();
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
