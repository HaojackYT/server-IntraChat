package com.example.model;

public class ModelRequestFile {
    
    int fileID;
    long currentLength;

    public ModelRequestFile(int fileID, long currentLength) {
        this.fileID = fileID;
        this.currentLength = currentLength;
    }

    public ModelRequestFile() {
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public long getCurrentLength() {
        return currentLength;
    }

    public void setCurrentLength(long currentLength) {
        this.currentLength = currentLength;
    }
}
