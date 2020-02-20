package com.ec.common;

public class FileRequest extends AbstractMessage {
    //запрос
    private String filename;

    public String getFilename() {
        return filename;
    }

    public FileRequest(String filename) {
        this.filename = filename;
    }
}

