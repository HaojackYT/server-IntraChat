package com.example.model;

public class ModelMessage {
    
    private boolean action;
    private String message;
    private Object data;

    public ModelMessage(boolean action, String message, Object data) {
        this.action = action;
        this.message = message;
        this.data = data;
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
