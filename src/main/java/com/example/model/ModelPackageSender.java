package com.example.model;

public class ModelPackageSender {
    
    int fileID;
    byte[] data;
    boolean finish;

    public ModelPackageSender(int fileID, byte[] data, boolean finish) {
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

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }
}
