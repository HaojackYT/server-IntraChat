CREATE DATABASE chat_application_intrachat;
CREATE TABLE `user` (
    UserID INT NOT NULL AUTO_INCREMENT,
    UserName VARCHAR(255),
    Password VARCHAR(255),
    PRIMARY KEY (UserID)
);