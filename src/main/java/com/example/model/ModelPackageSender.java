package com.example.model;

public class ModelPackageSender {
    
    int fileID;
    String data;
    boolean finish;

    public ModelPackageSender(int fileID, String data, boolean finish) {
        this.fileID = fileID;
        this.data = data;
        this.finish = finish;
    }

    public ModelPackageSender() { }
    
    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }
}
