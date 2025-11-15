package com.example.model;

public class ModelReceiveMessage {
    
    int fromUserID;
    String text;
    int toUserID;

    public ModelReceiveMessage(int fromUserID, int toUserID, String text) {
        this.fromUserID = fromUserID;
        this.text = text;
        this.toUserID = toUserID;
    }

    public ModelReceiveMessage() {
        
    }

    public int getToUserID(){
        return toUserID;
    }

    public void setToUserID(){
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
}
