package com.example.model;

public class ModelMessage {
    
    boolean action;
    String message;

    public ModelMessage(boolean action, String message) {
        this.action = action;
        this.message = message;
    }

    public ModelMessage() { }
    
    public boolean isAction() {
        return action;
    }

    public void setAction(boolean action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    
}
