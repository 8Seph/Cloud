package com.ec.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileMessage extends AbstractMessage {

    private String filename;
    private byte[] data;
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
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }

    public FileMessage(List<String> filesListOnServer) {
        this.filename = "/filesList";
        this.fileList = filesListOnServer;
    }
}

