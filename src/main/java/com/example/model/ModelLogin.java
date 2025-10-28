package com.example.model;

public class ModelLogin {
    
    String userName;
    String password;

    public ModelLogin(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }
    
    public ModelLogin() { }

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
