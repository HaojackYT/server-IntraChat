package com.example.model;

import com.corundumstudio.socketio.SocketIOClient;

public class ModelClient {
    
    SocketIOClient client;
    ModelUserAccount user;

    public ModelClient(SocketIOClient client, ModelUserAccount user) {
        this.client = client;
        this.user = user;
    }
    
    public ModelClient() { }
    
    public SocketIOClient getClient() {
        return client;
    }

    public void setClient(SocketIOClient client) {
        this.client = client;
    }

    public ModelUserAccount getUser() {
        return user;
    }

    public void setUser(ModelUserAccount user) {
        this.user = user;
    }
}
