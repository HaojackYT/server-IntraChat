package com.example.model;

import java.sql.Timestamp; 

public class ChatMessage {
    private long id;
    private int senderId;
    private int receiverId; 
    private String content;
    private Timestamp sentTime;

    public ChatMessage(int senderId, int receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sentTime = new Timestamp(System.currentTimeMillis()); 
    }
    
    public ChatMessage(long id, int senderId, int receiverId, String content, Timestamp sentTime) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sentTime = sentTime;
    }
    
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public Timestamp getSentTime() { return sentTime; }
    public void setId(long id) { this.id = id; }
    public long getId() { return id; }
}