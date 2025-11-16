package com.example.model;

public class ModelReceiveMessage {
    
    private int messageType; 
    int fromUserID;
    private String text;
    int toUserID;
    ModelReceiveImage dataImage; 

    public ModelReceiveMessage(int messageType, int fromUserID, int toUserID, String text, ModelReceiveImage dataImage) {
        this.messageType = messageType;
        this.fromUserID = fromUserID;
        this.text = text;
        this.toUserID = toUserID;
        this.dataImage = dataImage;
    }
    
    public ModelReceiveMessage(int fromUserID, int toUserID, String text) {
        this.messageType = 0; 
        this.fromUserID = fromUserID;
        this.text = text;
        this.toUserID = toUserID;
        this.dataImage = null;
    }

    public ModelReceiveMessage() {
    }
    
    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }
    
    public int getToUserID(){
        return toUserID;
    }

    public void setToUserID(int toUserID){
        this.toUserID = toUserID;
    }

    public int getFromUserID() {
        return fromUserID;
    }

    public void setFromUserID(int fromUserID) {
        this.fromUserID = fromUserID;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ModelReceiveImage getDataImage() {
        return dataImage;
    }

    public void setDataImage(ModelReceiveImage dataImage) {
        this.dataImage = dataImage;
    }
}