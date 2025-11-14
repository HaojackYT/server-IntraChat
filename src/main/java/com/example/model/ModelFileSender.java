package com.example.model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ModelFileSender {
    
    ModelFile data;
    File file;
    RandomAccessFile accessFile;
    long fileSize;

    public ModelFileSender(ModelFile data, File file) throws IOException {
        this.data = data;
        this.file = file;
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new IOException("Cannot read file: " + file.getAbsolutePath());
        }
        this.accessFile = new RandomAccessFile(file, "r");
        this.fileSize = accessFile.length();
        System.out.println("[ModelFileSender] Created for file: " + file.getAbsolutePath() + ", Size: " + fileSize);
    }

    public ModelFileSender() {
    }
    
    public byte[] read(long currentLength) throws IOException {
        accessFile.seek(currentLength);
        if (currentLength != fileSize) {
            int max = 2000;
            long length;
            if (currentLength + max >= fileSize) {
                length = fileSize - currentLength;
            } else {
                length = max;
            }
            byte[] b = new byte[(int) length];
            int bytesRead = accessFile.read(b);
            System.out.println("[ModelFileSender] Read " + bytesRead + " bytes from position " + currentLength + " of " + fileSize);
            return b;
        } else {
            System.out.println("[ModelFileSender] Reached end of file, currentLength: " + currentLength + ", fileSize: " + fileSize);
            return null;
        }
    }

    public ModelFile getData() {
        return data;
    }

    public void setData(ModelFile data) {
        this.data = data;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public RandomAccessFile getAccessFile() {
        return accessFile;
    }

    public void setAccessFile(RandomAccessFile accessFile) {
        this.accessFile = accessFile;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
