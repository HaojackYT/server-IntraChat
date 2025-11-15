package com.example.model;

public class ModelRegister {
    String userName;
    String password;

    public ModelRegister(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public ModelRegister() { }
    
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
}
