package com.example.model;

public class ModelFile {
    
    int fileID;
    String fileExtension;

    public ModelFile(int fileID, String fileExtension) {
        this.fileID = fileID;
        this.fileExtension = fileExtension;
    }

    public ModelFile() {
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }
}
