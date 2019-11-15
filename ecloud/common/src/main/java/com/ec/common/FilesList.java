package com.ec.common;

import java.util.LinkedList;
import java.util.List;

public class FilesList extends AbstractMessage {
    private List<String> filesList = new LinkedList<>();

    public FilesList(List<String> filesList){
        this.filesList = filesList;
    }

    public List<String> getFilesList() {
        return filesList;
    }
}
