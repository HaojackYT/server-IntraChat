package com.example.model;

public class ModelReceiveImage {

    private int fileID;
    private String fileName; // tên file
    private long fileSize;   // dung lượng file
    private String image;
    private int width;
    private int height;

    public ModelReceiveImage() {
    }

    public ModelReceiveImage(int fileID, String fileName, long fileSize, String image, int width, int height) {
        this.fileID = fileID;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.image = image;
        this.width = width;
        this.height = height;
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
