package com.example.model;

import org.json.JSONObject;

public class ModelReceiveMessage {

    private int messageType;
    private int fromUserID;
    private String text;
    private ModelReceiveImage dataImage;

    public ModelReceiveMessage(int messageType, int fromUserID, String text, ModelReceiveImage dataImage) {
        this.messageType = messageType;
        this.fromUserID = fromUserID;
        this.text = text;
        this.dataImage = dataImage;
    }

    public ModelReceiveMessage() {
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
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

    // ======= Thêm phương thức toJSONObject =======
    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("messageType", messageType);
        obj.put("fromUserID", fromUserID);
        obj.put("text", text);

        if (dataImage != null) {
            JSONObject img = new JSONObject();
            img.put("fileID", dataImage.getFileID());
            img.put("image", dataImage.getImage());
            img.put("width", dataImage.getWidth());
            img.put("height", dataImage.getHeight());
            obj.put("dataImage", img);
        }

        return obj;
    }
}
