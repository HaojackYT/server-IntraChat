package com.example.model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ModelFileReceiver {
    
    ModelSendMessage message;
    File file;
    RandomAccessFile accessFile;

    public ModelFileReceiver(ModelSendMessage message, File file) throws IOException {
        this.message = message;
        this.file = file;
        // Create parent directory if not exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        this.accessFile = new RandomAccessFile(file, "rw");
    }

    public ModelFileReceiver() {
    }
    
    public synchronized long writeFile(byte[] data) throws IOException {
        accessFile.seek(accessFile.length());
        accessFile.write(data);
        return accessFile.length();
    }
    
    public void close() throws IOException {
        accessFile.close();
    }
    
    public ModelSendMessage getMessage() {
        return message;
    }

    public void setMessage(ModelSendMessage message) {
        this.message = message;
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
}
