package com.ec.client;

import com.ec.common.AbstractMessage;
import com.ec.common.FileMessage;
import com.ec.common.FileRequest;
import com.ec.common.FilesList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MsgHandler {

    MainController controller;
    private String FILES_PATH;


    public MsgHandler(MainController controller) {
        this.controller = controller;
        this.FILES_PATH = controller.FILES_PATH;
    }

    public void checkMsg(AbstractMessage msg) throws IOException {

        // File
        if (msg instanceof FileMessage) {
            FileMessage fm = (FileMessage) msg;
            Files.write(Paths.get(FILES_PATH + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
            controller.refreshLocalFilesList();
        }

        // FilesList
        if (msg instanceof FilesList) {
            controller.refreshServerFilesList((FilesList) msg);
        }

        // Request
        if (msg instanceof FileRequest) {
            FileRequest fm = (FileRequest) msg;

        }
    }

}
