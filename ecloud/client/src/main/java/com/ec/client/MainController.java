package com.ec.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    protected static final String FILES_PATH = "D:/storage/client/01/";
    private Network network = Network.getInstance();

    double xOffset;
    double yOffset;

    @FXML
    private VBox rootNode;

    @FXML
    private VBox tableFolders;

    @FXML
    private ListView<String> filesList_SERVER;

    @FXML
    private Label isOnline;

    @FXML
    private Button dynamic;

    @FXML
    private ListView<String> filesList_CLIENT;

    @FXML
    private ListView<String> folders;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initDoubleClick(filesList_CLIENT);
        getFoldersOnClient();
        checkExistDirectories();
        network.setController(this);
        refreshLocalFilesList();
        connect();
    }



    @FXML
    void dynamicBtn(ActionEvent event) throws InterruptedException, IOException {


        if (filesList_CLIENT.isFocused()) {
            System.out.println("Dynamic - send");
            String fileName = filesList_CLIENT.getSelectionModel().getSelectedItem();
            try {
                ClientCommandManager.sendFile(Paths.get(FILES_PATH + fileName), network.getCurrentChannel());
            } catch (IOException e) {
                e.printStackTrace();
            }
            refreshLocalFilesList();
        }
        if (filesList_SERVER.isFocused()) {
            System.out.println("Dynamic - get");
            String fileName = filesList_SERVER.getSelectionModel().getSelectedItem();
            ClientCommandManager.requestFile(network.getCurrentChannel(), Paths.get(FILES_PATH + fileName));
        }




//        updateUI(() -> {
//
//        });

    }

    @FXML
    public void connectBtn() {
        //  Network.IP_ADDRESS = IP_ADDRESS.getText();
        Network.IP_ADDRESS = "localhost";
        connect();
        refreshLocalFilesList();
    }

    @FXML
    public void disconnectBtn() throws IOException {
        //   network.stop();
        openFile();
    }

    @FXML
    public void refreshBtn(){
        refreshLocalFilesList();
        getFilesListOnServer();
    }

    @FXML
    public void deleteBtn() {
        if (filesList_CLIENT.isFocused()) {
            String fileName = filesList_CLIENT.getSelectionModel().getSelectedItem();
            try {
                Files.delete(Paths.get(FILES_PATH + fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            refreshLocalFilesList();
        }
        if (filesList_SERVER.isFocused()) {
            String fileName = filesList_SERVER.getSelectionModel().getSelectedItem();
            ClientCommandManager.deleteFileOnServer(network.getCurrentChannel(), Paths.get(FILES_PATH + fileName));
        }
    }

    @FXML
    public void search(ActionEvent event) throws IOException {
        ClientCommandManager.getServerFilesList(network.getCurrentChannel());
    }


    private void getFoldersOnClient() {
        List<String> tmp = new LinkedList<>();
        updateUI(() -> {
            try {
                //Только папки в корневом каталоге (2!)
                Files.walkFileTree(Paths.get(FILES_PATH), new HashSet<>(), 2, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        tmp.add(dir.getFileName().toString());
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < tmp.size(); i++) {
                folders.getItems().add(tmp.get(i));
            }
        });

    }

    private void initDoubleClick(ListView<String> filesList) {
        filesList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    try {
                        openFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void openFile() throws IOException {
        Desktop desktop = Desktop.getDesktop();
        desktop.open(new File(FILES_PATH + filesList_CLIENT.getSelectionModel().getSelectedItem()));
    }

    private void checkExistDirectories() {
        if (!Files.exists(Paths.get(FILES_PATH))) {
            try {
                Files.createDirectories(Paths.get(FILES_PATH));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connect() {
        new Thread(() -> network.start()).start();
    }


    public void getFilesListOnServer() {
        ClientCommandManager.getServerFilesList(network.getCurrentChannel());
    }

    // todo Вызывается из ClientRequests.updateServerFileList()
    public void refreshServerFilesList(List<String> filesList) {
        updateUI(() -> {
            try {
                filesList_SERVER.getItems().clear();
                for (String str : filesList) {
                    filesList_SERVER.getItems().add(str);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    protected void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                filesList_CLIENT.getItems().clear();
                Files.list(Paths.get(FILES_PATH)).map(p -> p.getFileName().toString()).forEach(o -> filesList_CLIENT.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    protected void refreshConnectionState(String text) {
        updateUI(() -> {
            try {
                isOnline.setText(text);
            } catch (Exception e) {
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
