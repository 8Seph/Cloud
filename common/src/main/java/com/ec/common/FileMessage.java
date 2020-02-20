package com.ec.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileMessage extends AbstractMessage {

    private String filename;
    private byte[] data; // файл
    private List<String> fileList;

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public List<String> getFileList() {
        return fileList;
    }

    public FileMessage(Path path) throws IOException {
        filename = path.getFileName().toString(); //передаем путь, затем получаем имя файла
        data = Files.readAllBytes(path); //считываем все байты и записываем data для отправки
    }

    public FileMessage(List<String> filesListOnServer) {
        this.filename = "/getFilesList";
        this.fileList = filesListOnServer;
    }
}

